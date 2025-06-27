/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.filament;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Factory and manager for <em>renderables</em>, which are entities that can be drawn.
 * 可渲染对象的工厂和管理器，用于管理可以被绘制的实体。
 *
 * <p>Renderables are bundles of <em>primitives</em>, each of which has its own geometry and material. All
 * primitives in a particular renderable share a set of rendering attributes, such as whether they
 * cast shadows or use vertex skinning. Kotlin usage example:</p>
 * <p>可渲染对象是图元的集合，每个图元都有自己的几何体和材质。特定可渲染对象中的所有图元
 * 共享一组渲染属性，例如是否投射阴影或使用顶点蒙皮。Kotlin使用示例：</p>
 *
 * <pre>
 * val entity = EntityManager.get().create()
 *
 * RenderableManager.Builder(1)
 *         .boundingBox(Box(0.0f, 0.0f, 0.0f, 9000.0f, 9000.0f, 9000.0f))
 *         .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib)
 *         .material(0, material)
 *         .build(engine, entity)
 *
 * scene.addEntity(renderable)
 * </pre>
 *
 * <p>To modify the state of an existing renderable, clients should first use RenderableManager
 * to get a temporary handle called an <em>instance</em>. The instance can then be used to get or set
 * the renderable's state. Please note that instances are ephemeral; clients should store entities,
 * not instances.</p>
 * <p>要修改现有可渲染对象的状态，客户端应首先使用RenderableManager获取一个称为实例的临时句柄。
 * 然后可以使用该实例来获取或设置可渲染对象的状态。请注意，实例是临时的；客户端应存储实体，而不是实例。</p>
 *
 * <ul>
 * <li>For details about constructing renderables, see {@link RenderableManager.Builder}.</li>
 * <li>有关构造可渲染对象的详细信息，请参阅 {@link RenderableManager.Builder}。</li>
 * <li>To associate a 4x4 transform with an entity, see {@link TransformManager}.</li>
 * <li>要将4x4变换与实体关联，请参阅 {@link TransformManager}。</li>
 * </ul>
 */
public class RenderableManager {
    /** 日志标签 */
    private static final String LOG_TAG = "Filament";

    /** 顶点属性值的静态数组缓存 */
    private static final VertexBuffer.VertexAttribute[] sVertexAttributeValues =
            VertexBuffer.VertexAttribute.values();

    /** 本地对象的指针 */
    private long mNativeObject;

    /**
     * 构造RenderableManager实例
     * @param nativeRenderableManager 本地RenderableManager对象的指针
     */
    RenderableManager(long nativeRenderableManager) {
        mNativeObject = nativeRenderableManager;
    }

    /**
     * Checks if the given entity already has a renderable component.
     * 检查给定的实体是否已经有可渲染组件。
     * 
     * @param entity 要检查的实体ID
     * @return 如果实体有可渲染组件则返回true，否则返回false
     */
    public boolean hasComponent(@Entity int entity) {
        return nHasComponent(mNativeObject, entity);
    }

    /**
     * Gets a temporary handle that can be used to access the renderable state.
     * 获取一个可用于访问可渲染状态的临时句柄。
     * 
     * @param entity 实体ID
     * @return 可渲染实例的句柄
     */
    @EntityInstance
    public int getInstance(@Entity int entity) {
        return nGetInstance(mNativeObject, entity);
    }

    /**
     * Destroys the renderable component in the given entity.
     * 销毁给定实体中的可渲染组件。
     * 
     * @param entity 要销毁可渲染组件的实体ID
     */
    public void destroy(@Entity int entity) {
        nDestroy(mNativeObject, entity);
    }

    /**
     * Primitive types used in {@link RenderableManager.Builder#geometry}.
     * 在 {@link RenderableManager.Builder#geometry} 中使用的图元类型。
     */
    public enum PrimitiveType {
        /** 点图元 */
        POINTS(0),
        /** 线段图元 */
        LINES(1),
        /** 线条图元 */
        LINE_STRIP(3),
        /** 三角形图元 */
        TRIANGLES(4),
        /** 三角形条带图元 */
        TRIANGLE_STRIP(5);

        /** 图元类型的内部值 */
        private final int mType;
        
        /**
         * 构造图元类型
         * @param value 图元类型的数值
         */
        PrimitiveType(int value) { mType = value; }
        
        /**
         * 获取图元类型的数值
         * @return 图元类型的内部值
         */
        int getValue() { return mType; }
    }

    /**
     * Adds renderable components to entities using a builder pattern.
     * 使用构建器模式向实体添加可渲染组件。
     */
    public static class Builder {
        @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"}) // Keep to finalize native resources
        /** 用于清理本地资源的终结器 */
        private final BuilderFinalizer mFinalizer;
        /** 本地构建器对象的指针 */
        private final long mNativeBuilder;

        /**
         * Creates a builder for renderable components.
         * 创建可渲染组件的构建器。
         *
         * @param count the number of primitives that will be supplied to the builder
         *              将提供给构建器的图元数量
         *
         * Note that builders typically do not have a long lifetime since clients should discard
         * them after calling {@link #build}. For a usage example, see {@link RenderableManager}.
         * 注意：构建器通常没有很长的生命周期，因为客户端应该在调用 {@link #build} 后丢弃它们。
         * 有关使用示例，请参阅 {@link RenderableManager}。
         */
        public Builder(@IntRange(from = 1) int count) {
            mNativeBuilder = nCreateBuilder(count);
            mFinalizer = new BuilderFinalizer(mNativeBuilder);
        }

        /**
         * Specifies the geometry data for a primitive.
         * 为图元指定几何数据。
         *
         * Filament primitives must have an associated {@link VertexBuffer} and {@link IndexBuffer}.
         * Typically, each primitive is specified with a pair of daisy-chained calls:
         * <code>geometry()</code> and <code>material()</code>.
         * Filament图元必须有关联的 {@link VertexBuffer} 和 {@link IndexBuffer}。
         * 通常，每个图元都通过一对链式调用来指定：<code>geometry()</code> 和 <code>material()</code>。
         *
         * @param index zero-based index of the primitive, must be less than the count passed to Builder constructor
         *              图元的从零开始的索引，必须小于传递给Builder构造函数的计数
         * @param type specifies the topology of the primitive (e.g., {@link PrimitiveType#TRIANGLES})
         *             指定图元的拓扑结构（例如，{@link PrimitiveType#TRIANGLES}）
         * @param vertices specifies the vertex buffer, which in turn specifies a set of attributes
         *                 指定顶点缓冲区，进而指定一组属性
         * @param indices specifies the index buffer (either u16 or u32)
         *                指定索引缓冲区（u16或u32）
         * @param offset specifies where in the index buffer to start reading (expressed as a number of indices)
         *               指定在索引缓冲区中开始读取的位置（以索引数表示）
         * @param minIndex specifies the minimum index contained in the index buffer
         *                 指定索引缓冲区中包含的最小索引
         * @param maxIndex specifies the maximum index contained in the index buffer
         *                 指定索引缓冲区中包含的最大索引
         * @param count number of indices to read (for triangles, this should be a multiple of 3)
         *              要读取的索引数（对于三角形，这应该是3的倍数）
         */
        @NonNull
        public Builder geometry(
                @IntRange(from = 0) int index,
                @NonNull PrimitiveType type,
                @NonNull VertexBuffer vertices,
                @NonNull IndexBuffer indices,
                @IntRange(from = 0) int offset,
                @IntRange(from = 0) int minIndex,
                @IntRange(from = 0) int maxIndex,
                @IntRange(from = 0) int count) {
            nBuilderGeometry(mNativeBuilder, index, type.getValue(), vertices.getNativeObject(),
                    indices.getNativeObject(), offset, minIndex, maxIndex, count);
            return this;
        }

        /**
         * For details, see the {@link RenderableManager.Builder#geometry} primary overload.
         * 有关详细信息，请参阅 {@link RenderableManager.Builder#geometry} 主重载方法。
         */
        @NonNull
        public Builder geometry(@IntRange(from = 0) int index, @NonNull PrimitiveType type,
                @NonNull VertexBuffer vertices, @NonNull IndexBuffer indices,
                @IntRange(from = 0) int offset, @IntRange(from = 0) int count) {
            nBuilderGeometry(mNativeBuilder, index, type.getValue(), vertices.getNativeObject(),
                    indices.getNativeObject(), offset, count);
            return this;
        }

        /**
         * For details, see the {@link RenderableManager.Builder#geometry} primary overload.
         * 有关详细信息，请参阅 {@link RenderableManager.Builder#geometry} 主重载方法。
         */
        @NonNull
        public Builder geometry(@IntRange(from = 0) int index, @NonNull PrimitiveType type,
                @NonNull VertexBuffer vertices, @NonNull IndexBuffer indices) {
            nBuilderGeometry(mNativeBuilder, index, type.getValue(),
                    vertices.getNativeObject(), indices.getNativeObject());
            return this;
        }

        /**
         * Type of geometry for a Renderable
         * 可渲染对象的几何类型
         */
        public enum GeometryType {
            /** dynamic gemoetry has no restriction */
            /** 动态几何体没有限制 */
            DYNAMIC,
            /** bounds and world space transform are immutable */
            /** 边界和世界空间变换是不可变的 */
            STATIC_BOUNDS,
            /** skinning/morphing not allowed and Vertex/IndexBuffer immutables */
            /** 不允许蒙皮/变形，顶点/索引缓冲区不可变 */
            STATIC
        }

        /**
         * Specify whether this renderable has static bounds. In this context his means that
         * the renderable's bounding box cannot change and that the renderable's transform is
         * assumed immutable. Changing the renderable's transform via the TransformManager
         * can lead to corrupted graphics. Note that skinning and morphing are not forbidden.
         * Disabled by default.
         * 指定此可渲染对象是否具有静态边界。在此上下文中，这意味着可渲染对象的边界框不能更改，
         * 并且可渲染对象的变换被假定为不可变的。通过TransformManager更改可渲染对象的变换
         * 可能导致图形损坏。请注意，蒙皮和变形不被禁止。默认情况下禁用。
         * 
         * @param type whether this renderable has static bounds. false by default.
         *             此可渲染对象是否具有静态边界。默认为false。
         */
        @NonNull
        public Builder geometryType(GeometryType type) {
            nBuilderGeometryType(mNativeBuilder, type.ordinal());
            return this;
        }

        /**
         * Binds a material instance to the specified primitive.
         * 将材质实例绑定到指定的图元。
         *
         * <p>If no material is specified for a given primitive, Filament will fall back to a basic
         * default material.</p>
         * <p>如果没有为给定图元指定材质，Filament将回退到基本的默认材质。</p>
         *
         * @param index zero-based index of the primitive, must be less than the count passed to Builder constructor
         *              图元的从零开始的索引，必须小于传递给Builder构造函数的计数
         * @param material the material to bind
         *                 要绑定的材质
         */
        @NonNull
        public Builder material(@IntRange(from = 0) int index, @NonNull MaterialInstance material) {
            nBuilderMaterial(mNativeBuilder, index, material.getNativeObject());
            return this;
        }

        /**
         * Sets the drawing order for blended primitives. The drawing order is either global or
         * local (default) to this Renderable. In either case, the Renderable priority takes
         * precedence.
         * 设置混合图元的绘制顺序。绘制顺序可以是全局的或此可渲染对象的本地（默认）。
         * 在任何情况下，可渲染对象的优先级都优先。
         *
         * @param index the primitive of interest
         *              感兴趣的图元
         * @param blendOrder draw order number (0 by default). Only the lowest 15 bits are used.
         *                   绘制顺序号（默认为0）。仅使用最低15位。
         */
        @NonNull
        public Builder blendOrder(@IntRange(from = 0) int index,
                @IntRange(from = 0, to = 32767) int blendOrder) {
            nBuilderBlendOrder(mNativeBuilder, index, blendOrder);
            return this;
        }

       /**
         * Sets whether the blend order is global or local to this Renderable (by default).
         * 设置混合顺序是全局的还是此可渲染对象的本地（默认）。
         *
         * @param index the primitive of interest
         *              感兴趣的图元
         * @param enabled true for global, false for local blend ordering.
         *                true表示全局，false表示本地混合排序。
         */
        @NonNull
        public Builder globalBlendOrderEnabled(@IntRange(from = 0) int index, boolean enabled) {
            nBuilderGlobalBlendOrderEnabled(mNativeBuilder, index, enabled);
            return this;
        }

        /**
         * The axis-aligned bounding box of the renderable.
         * 可渲染对象的轴对齐边界框。
         *
         * <p>This is an object-space AABB used for frustum culling. For skinning and morphing, this
         * should encompass all possible vertex positions. It is mandatory unless culling is
         * disabled for the renderable.</p>
         * <p>这是用于视锥体剔除的对象空间AABB。对于蒙皮和变形，这应该包含所有可能的顶点位置。
         * 除非为可渲染对象禁用剔除，否则这是必需的。</p>
         */
        @NonNull
        public Builder boundingBox(@NonNull Box aabb) {
            nBuilderBoundingBox(mNativeBuilder,
                    aabb.getCenter()[0], aabb.getCenter()[1], aabb.getCenter()[2],
                    aabb.getHalfExtent()[0], aabb.getHalfExtent()[1], aabb.getHalfExtent()[2]);
            return this;
        }

        /**
         * Sets bits in a visibility mask. By default, this is 0x1.
         * 设置可见性掩码中的位。默认情况下，这是0x1。
         *
         * <p>This feature provides a simple mechanism for hiding and showing groups of renderables
         * in a Scene. See {@link View#setVisibleLayers}.</p>
         * <p>此功能提供了在场景中隐藏和显示可渲染对象组的简单机制。请参阅{@link View#setVisibleLayers}。</p>
         *
         * <p>For example, to set bit 1 and reset bits 0 and 2 while leaving all other bits
         * unaffected, do: <code>builder.layerMask(7, 2)</code>.</p>
         * <p>例如，要设置位1并重置位0和2，同时保持所有其他位不受影响，请执行：<code>builder.layerMask(7, 2)</code>。</p>
         *
         * @see RenderableManager#setLayerMask
         *
         * @param select the set of bits to affect
         *               要影响的位集
         * @param value the replacement values for the affected bits
         *              受影响位的替换值
         */
        @NonNull
        public Builder layerMask(@IntRange(from = 0, to = 255) int select,
                @IntRange(from = 0, to = 255) int value) {
            nBuilderLayerMask(mNativeBuilder, select & 0xFF, value & 0xFF);
            return this;
        }

        /**
         * Provides coarse-grained control over draw order.
         *
         * <p>In general Filament reserves the right to re-order renderables to allow for efficient
         * rendering. However clients can control ordering at a coarse level using \em priority.
         * The priority is applied separately for opaque and translucent objects, that is, opaque
         * objects are always drawn before translucent objects regardless of the priority.</p>
         *
         * <p>For example, this could be used to draw a semitransparent HUD, if a client wishes to
         * avoid using a separate View for the HUD. Note that priority is completely orthogonal to
         * {@link Builder#layerMask}, which merely controls visibility.</p>

         * <p>The Skybox always using the lowest priority, so it's drawn last, which may improve
         * performance.</p>
         *
         * <p>The priority is clamped to the range [0..7], defaults to 4; 7 is lowest priority
         * (rendered last).</p>
         *
         * @see Builder#blendOrder
         */

        /**
         * Provides coarse-grained control over draw order.
         * 提供对绘制顺序的粗粒度控制。
         *
         * <p>In general Filament reserves the right to re-order renderables to allow for efficient
         * rendering. However clients can control ordering at a coarse level using priority.
         * The priority is applied separately for opaque and translucent objects, that is, opaque
         * objects are always drawn before translucent objects regardless of the priority.</p>
         * <p>一般来说，Filament保留重新排序可渲染对象的权利以允许高效渲染。但是，客户端可以使用优先级
         * 在粗粒度级别控制排序。优先级分别应用于不透明和半透明对象，即无论优先级如何，
         * 不透明对象总是在半透明对象之前绘制。</p>
         *
         * <p>For example, this could be used to draw a semitransparent HUD, if a client wishes to
         * avoid using a separate View for the HUD. Note that priority is completely orthogonal to
         * {@link Builder#layerMask}, which merely controls visibility.</p>
         * <p>例如，如果客户端希望避免为HUD使用单独的视图，这可以用于绘制半透明HUD。
         * 请注意，优先级与{@link Builder#layerMask}完全正交，后者仅控制可见性。</p>

         * <p>The Skybox always using the lowest priority, so it's drawn last, which may improve
         * performance.</p>
         * <p>天空盒总是使用最低优先级，因此它最后绘制，这可能会提高性能。</p>
         *
         * @param priority clamped to the range [0..7], defaults to 4; 7 is lowest priority
         *                 (rendered last).
         *                 限制在范围[0..7]内，默认为4；7是最低优先级（最后渲染）。
         *
         * @return Builder reference for chaining calls.
         *         用于链式调用的Builder引用。
         *
         * @see Builder#channel
         * @see Builder#blendOrder
         * @see #setPriority
         * @see #setBlendOrderAt
         */
        @NonNull
        public Builder priority(@IntRange(from = 0, to = 7) int priority) {
            nBuilderPriority(mNativeBuilder, priority);
            return this;
        }

        /**
         * Set the channel this renderable is associated to. There can be 4 channels.
         * 设置此可渲染对象关联的通道。可以有4个通道。
         *
         * <p>All renderables in a given channel are rendered together, regardless of anything else.
         * They are sorted as usual within a channel.</p>
         * <p>给定通道中的所有可渲染对象都一起渲染，无论其他任何因素。
         * 它们在通道内按常规排序。</p>
         * <p>Channels work similarly to priorities, except that they enforce the strongest
         * ordering.</p>
         * <p>通道的工作方式类似于优先级，除了它们强制执行最强的排序。</p>
         *
         * <p>Channels 0 and 1 may not have render primitives using a material with `refractionType`
         * set to `screenspace`.</p>
         * <p>通道0和1可能没有使用将`refractionType`设置为`screenspace`的材质的渲染图元。</p>
         *
         * @param channel clamped to the range [0..3], defaults to 2.
         *                限制在范围[0..3]内，默认为2。
         *
         * @return Builder reference for chaining calls.
         *         用于链式调用的Builder引用。
         *
         * @see Builder::blendOrder()
         * @see Builder::priority()
         * @see RenderableManager::setBlendOrderAt()
         */
        @NonNull
        public Builder channel(@IntRange(from = 0, to = 3) int channel) {
            nBuilderChannel(mNativeBuilder, channel);
            return this;
        }

        /**
         * Controls frustum culling, true by default.
         * 控制视锥体剔除，默认为true。
         *
         * <p>Do not confuse frustum culling with backface culling. The latter is controlled via
         * the material.</p>
         * <p>不要将视锥体剔除与背面剔除混淆。后者通过材质控制。</p>
         */
        @NonNull
        public Builder culling(boolean enabled) {
            nBuilderCulling(mNativeBuilder, enabled);
            return this;
        }

        /**
         * Enables or disables a light channel. Light channel 0 is enabled by default.
         * 启用或禁用光照通道。光照通道0默认启用。
         *
         * @param channel Light channel to enable or disable, between 0 and 7.
         *                要启用或禁用的光照通道，介于0和7之间。
         * @param enable Whether to enable or disable the light channel.
         *               是否启用或禁用光照通道。
         */
        @NonNull
        public Builder lightChannel(@IntRange(from = 0, to = 7) int channel, boolean enable) {
            nBuilderLightChannel(mNativeBuilder, channel, enable);
            return this;
        }

        /**
         * Specifies the number of draw instance of this renderable. The default is 1 instance and
         * the maximum number of instances allowed is 32767. 0 is invalid.
         * All instances are culled using the same bounding box, so care must be taken to make
         * sure all instances render inside the specified bounding box.
         * The material can use getInstanceIndex() in the vertex shader to get the instance index and
         * possibly adjust the position or transform.
         * 指定此可渲染对象的绘制实例数量。默认为1个实例，允许的最大实例数为32767。0无效。
         * 所有实例都使用相同的边界框进行剔除，因此必须小心确保所有实例都在指定的边界框内渲染。
         * 材质可以在顶点着色器中使用getInstanceIndex()来获取实例索引，并可能调整位置或变换。
         *
         * @param instanceCount the number of instances silently clamped between 1 and 32767.
         *                      实例数量，静默限制在1和32767之间。
         */
        @NonNull
        public Builder instances(@IntRange(from = 1, to = 32767) int instanceCount) {
            nBuilderInstances(mNativeBuilder, instanceCount);
            return this;
        }

        /**
         * Controls if this renderable casts shadows, false by default.
         * 控制此可渲染对象是否投射阴影，默认为false。
         *
         * If the View's shadow type is set to {@link View.ShadowType#VSM}, castShadows should only
         * be disabled if either is true:
         * 如果视图的阴影类型设置为{@link View.ShadowType#VSM}，只有在以下任一情况为真时才应禁用castShadows：
         * <ul>
         *   <li>{@link RenderableManager#setReceiveShadows} is also disabled</li>
         *   <li>{@link RenderableManager#setReceiveShadows}也被禁用</li>
         *   <li>the object is guaranteed to not cast shadows on itself or other objects (for
         *   example, a ground plane)</li>
         *   <li>保证对象不会在自身或其他对象上投射阴影（例如，地面平面）</li>
         * </ul>
         */
        @NonNull
        public Builder castShadows(boolean enabled) {
            nBuilderCastShadows(mNativeBuilder, enabled);
            return this;
        }

        /**
         * Controls if this renderable receives shadows, true by default.
         * 控制此可渲染对象是否接收阴影，默认为true。
         */
        @NonNull
        public Builder receiveShadows(boolean enabled) {
            nBuilderReceiveShadows(mNativeBuilder, enabled);
            return this;
        }

        /**
         * Controls if this renderable uses screen-space contact shadows. This is more
         * expensive but can improve the quality of shadows, especially in large scenes.
         * (off by default).
         * 控制此可渲染对象是否使用屏幕空间接触阴影。这更昂贵，但可以提高阴影质量，
         * 特别是在大型场景中。（默认关闭）。
         */
        @NonNull
        public Builder screenSpaceContactShadows(boolean enabled) {
            nBuilderScreenSpaceContactShadows(mNativeBuilder, enabled);
            return this;
        }

        /**
         * Allows bones to be swapped out and shared using SkinningBuffer.
         * 允许使用SkinningBuffer交换和共享骨骼。
         *
         * If skinning buffer mode is enabled, clients must call #setSkinningBuffer() rather than
         * #setBonesAsQuaternions(). This allows sharing of data between renderables.
         * 如果启用蒙皮缓冲区模式，客户端必须调用#setSkinningBuffer()而不是
         * #setBonesAsQuaternions()。这允许在可渲染对象之间共享数据。
         *
         * @param enabled If true, enables buffer object mode.  False by default.
         *                如果为true，启用缓冲区对象模式。默认为false。
         */
        @NonNull
        public Builder enableSkinningBuffers(boolean enabled) {
            nBuilderEnableSkinningBuffers(mNativeBuilder, enabled);
            return this;
        }

        /**
         * Controls if this renderable is affected by the large-scale fog.
         * 控制此可渲染对象是否受大规模雾效影响。
         * @param enabled If true, enables large-scale fog on this object. Disables it otherwise.
         *                True by default.
         *                如果为true，在此对象上启用大规模雾效。否则禁用它。默认为true。
         * @return this <code>Builder</code> object for chaining calls
         *         用于链式调用的<code>Builder</code>对象
         */
         @NonNull
        public Builder fog(boolean enabled) {
            nBuilderFog(mNativeBuilder, enabled);
            return this;
        }

        /**
         * Enables GPU vertex skinning for up to 255 bones, 0 by default.
         * 启用GPU顶点蒙皮，最多支持255个骨骼，默认为0。
         *
         *<p>Skinning Buffer mode must be enabled.</p>
         *<p>必须启用蒙皮缓冲区模式。</p>
         *
         *<p>Each vertex can be affected by up to 4 bones simultaneously. The attached
         * VertexBuffer must provide data in the BONE_INDICES slot (uvec4) and the
         * BONE_WEIGHTS slot (float4).</p>
         *<p>每个顶点最多可以同时受4个骨骼影响。附加的VertexBuffer必须在BONE_INDICES槽（uvec4）
         * 和BONE_WEIGHTS槽（float4）中提供数据。</p>
         *
         *<p>See also {@link #setSkinningBuffer}, {@link SkinningBuffer#setBonesAsMatrices}
         * or  {@link SkinningBuffer#setBonesAsQuaternions},
         * which can be called on a per-frame basis to advance the animation.</p>
         *<p>另请参阅{@link #setSkinningBuffer}、{@link SkinningBuffer#setBonesAsMatrices}
         * 或{@link SkinningBuffer#setBonesAsQuaternions}，可以按帧调用以推进动画。</p>
         *
         * @see #setSkinningBuffer
         * @see SkinningBuffer#setBonesAsMatrices
         * @see SkinningBuffer#setBonesAsQuaternions
         *
         * @param skinningBuffer null to disable, otherwise the {@link SkinningBuffer} to use
         *                       null表示禁用，否则为要使用的{@link SkinningBuffer}
         * @param boneCount 0 to disable, otherwise the number of bone transforms (up to 255)
         *                  0表示禁用，否则为骨骼变换数量（最多255个）
         * @param offset offset in the {@link SkinningBuffer}
         *               {@link SkinningBuffer}中的偏移量
         * @return this <code>Builder</code> object for chaining calls
         *         用于链式调用的<code>Builder</code>对象
         */
        @NonNull
        public Builder skinning(SkinningBuffer skinningBuffer,
                @IntRange(from = 0, to = 255) int boneCount, int offset) {
            nBuilderSkinningBuffer(mNativeBuilder,
                    skinningBuffer != null ? skinningBuffer.getNativeObject() : 0, boneCount, offset);
            return this;
        }

        /**
         * Enables GPU vertex skinning for up to 255 bones, 0 by default.
         * 启用GPU顶点蒙皮，最多支持255个骨骼，默认为0。
         *
         * @param boneCount 0 to disable, otherwise the number of bone transforms (up to 255)
         *                  0表示禁用，否则为骨骼变换数量（最多255个）
         */
        @NonNull
        public Builder skinning(@IntRange(from = 0, to = 255) int boneCount) {
            nBuilderSkinning(mNativeBuilder, boneCount);
            return this;
        }

        /**
         * Enables GPU vertex skinning for up to 255 bones, 0 by default.
         * 启用GPU顶点蒙皮，最多支持255个骨骼，默认为0。
         *
         * <p>Skinning Buffer mode must be disabled.</p>
         * <p>必须禁用蒙皮缓冲区模式。</p>
         *
         * <p>Each vertex can be affected by up to 4 bones simultaneously. The attached
         * VertexBuffer must provide data in the <code>BONE_INDICES</code> slot (uvec4) and the
         * <code>BONE_WEIGHTS</code> slot (float4).</p>
         * <p>每个顶点最多可以同时受4个骨骼影响。附加的VertexBuffer必须在<code>BONE_INDICES</code>槽（uvec4）
         * 和<code>BONE_WEIGHTS</code>槽（float4）中提供数据。</p>
         *
         * <p>See also {@link RenderableManager#setBonesAsMatrices}, which can be called on a per-frame basis
         * to advance the animation.</p>
         * <p>另请参阅{@link RenderableManager#setBonesAsMatrices}，可以按帧调用以推进动画。</p>
         *
         * @see SkinningBuffer#setBonesAsMatrices
         *
         * @param boneCount Number of bones associated with this component
         *                  与此组件关联的骨骼数量
         * @param bones A FloatBuffer containing boneCount transforms. Each transform consists of 8 float.
         *              float 0 to 3 encode a unit quaternion w+ix+jy+kz stored as x,y,z,w.
         *              float 4 to 7 encode a translation stored as x,y,z,1
         *              包含boneCount个变换的FloatBuffer。每个变换由8个float组成。
         *              float 0到3编码单位四元数w+ix+jy+kz，存储为x,y,z,w。
         *              float 4到7编码平移，存储为x,y,z,1
         */
        @NonNull
        public Builder skinning(@IntRange(from = 0, to = 255) int boneCount, @NonNull Buffer bones) {
            int result = nBuilderSkinningBones(mNativeBuilder, boneCount, bones, bones.remaining());
            if (result < 0) {
                throw new BufferOverflowException();
            }
            return this;
        }

        /**
         * Controls if the renderable has legacy vertex morphing targets, zero by default.
         * 控制可渲染对象是否具有传统顶点形变目标，默认为零。
         *
         * For legacy morphing, the attached {@link VertexBuffer} must provide data in the
         * appropriate {@link VertexBuffer.VertexAttribute} slots (<code>MORPH_POSITION_0</code> etc).
         * Legacy morphing only supports up to 4 morph targets and will be deprecated in the future.
         * Legacy morphing must be enabled on the material definition: either via the
         * <code>legacyMorphing</code> material attribute or by calling
         * {@link MaterialBuilder::useLegacyMorphing}.
         * 对于传统形变，附加的{@link VertexBuffer}必须在适当的{@link VertexBuffer.VertexAttribute}
         * 槽（<code>MORPH_POSITION_0</code>等）中提供数据。传统形变仅支持最多4个形变目标，
         * 将来会被弃用。必须在材质定义上启用传统形变：通过<code>legacyMorphing</code>材质属性
         * 或调用{@link MaterialBuilder::useLegacyMorphing}。
         *
         * <p>See also {@link RenderableManager#setMorphWeights}, which can be called on a per-frame basis
         * to advance the animation.</p>
         * <p>另请参阅{@link RenderableManager#setMorphWeights}，可以按帧调用以推进动画。</p>
         */
        @NonNull
        public Builder morphing(@IntRange(from = 0, to = 255) int targetCount) {
            nBuilderMorphing(mNativeBuilder, targetCount);
            return this;
        }

        /**
         * Controls if the renderable has vertex morphing targets, zero by default.
         * 控制可渲染对象是否具有顶点形变目标，默认为零。
         *
         * <p>For standard morphing, A {@link MorphTargetBuffer} must be provided.
         * Standard morphing supports up to
         * <code>CONFIG_MAX_MORPH_TARGET_COUNT</code> morph targets.</p>
         * <p>对于标准形变，必须提供{@link MorphTargetBuffer}。标准形变支持最多
         * <code>CONFIG_MAX_MORPH_TARGET_COUNT</code>个形变目标。</p>
         *
         * <p>See also {@link RenderableManager#setMorphWeights}, which can be called on a per-frame basis
         * to advance the animation.</p>
         * <p>另请参阅{@link RenderableManager#setMorphWeights}，可以按帧调用以推进动画。</p>
         */
        @NonNull
        public Builder morphing(@NonNull MorphTargetBuffer morphTargetBuffer) {
            nBuilderMorphingStandard(mNativeBuilder, morphTargetBuffer.getNativeObject());
            return this;
        }

        /**
         * Specifies the morph target buffer for a primitive.
         * 为图元指定形变目标缓冲区。
         *
         * The morph target buffer must have an associated renderable and geometry. Two conditions
         * must be met:
         * 1. The number of morph targets in the buffer must equal the renderable's morph target
         *    count.
         * 2. The vertex count of each morph target must equal the geometry's vertex count.
         * 形变目标缓冲区必须有关联的可渲染对象和几何体。必须满足两个条件：
         * 1. 缓冲区中的形变目标数量必须等于可渲染对象的形变目标计数。
         * 2. 每个形变目标的顶点计数必须等于几何体的顶点计数。
         *
         * @param level the level of detail (lod), only 0 can be specified
         *              细节级别（lod），只能指定0
         * @param primitiveIndex zero-based index of the primitive, must be less than the count passed to Builder constructor
         *                       图元的从零开始的索引，必须小于传递给Builder构造函数的计数
         * @param offset specifies where in the morph target buffer to start reading (expressed as a number of vertices)
         *               指定在形变目标缓冲区中开始读取的位置（以顶点数表示）
         */
        @NonNull
        public Builder morphing(@IntRange(from = 0) int level,
                                @IntRange(from = 0) int primitiveIndex,
                                @IntRange(from = 0) int offset) {
            nBuilderSetMorphTargetBufferOffsetAt(mNativeBuilder, level, primitiveIndex, offset);
            return this;
        }

        /**
         * Adds the Renderable component to an entity.
         * 将可渲染组件添加到实体。
         *
         * <p>If this component already exists on the given entity and the construction is successful,
         * it is first destroyed as if {@link RenderableManager#destroy} was called.</p>
         * <p>如果此组件已存在于给定实体上且构造成功，则首先销毁它，
         * 就像调用了{@link RenderableManager#destroy}一样。</p>
         *
         * @param engine reference to the <code>Engine</code> to associate this renderable with
         *               要与此可渲染对象关联的<code>Engine</code>引用
         * @param entity entity to add the renderable component to
         *               要添加可渲染组件的实体
         */
        public void build(@NonNull Engine engine, @Entity int entity) {
            if (!nBuilderBuild(mNativeBuilder, engine.getNativeObject(), entity)) {
                throw new IllegalStateException(
                    "Couldn't create Renderable component for entity " + entity + ", see log.");
            }
        }

        private static class BuilderFinalizer {
            private final long mNativeObject;
            BuilderFinalizer(long nativeObject) { mNativeObject = nativeObject; }
            @Override
            public void finalize() {
                try {
                    super.finalize();
                } catch (Throwable t) { // Ignore
                } finally {
                    nDestroyBuilder(mNativeObject);
                }
            }
        }
    }


    /**
     * Associates a {@link SkinningBuffer} to a renderable instance
     * 将{@link SkinningBuffer}关联到可渲染实例
     * @param i Instance of the Renderable
     *          可渲染对象的实例
     * @param skinningBuffer {@link SkinningBuffer} to use
     *                       要使用的{@link SkinningBuffer}
     * @param count Numbers of bones to set
     *              要设置的骨骼数量
     * @param offset Offset in the {@link SkinningBuffer}
     *               {@link SkinningBuffer}中的偏移量
     */
    public void setSkinningBuffer(@EntityInstance int i, @NonNull SkinningBuffer skinningBuffer,
                           int count, int offset) {
        nSetSkinningBuffer(mNativeObject, i, skinningBuffer.getNativeObject(), count, offset);
    }

    /**
     * Sets the transforms associated with each bone of a Renderable.
     * 设置与可渲染对象的每个骨骼关联的变换。
     *
     * @param i Instance of the Renderable
     *          可渲染对象的实例
     * @param matrices A FloatBuffer containing boneCount 4x4 packed matrices (i.e. 16 floats each matrix and no gap between matrices)
     *                 包含boneCount个4x4紧密排列矩阵的FloatBuffer（即每个矩阵16个浮点数，矩阵之间无间隙）
     * @param boneCount Number of bones to set
     *                  要设置的骨骼数量
     * @param offset Index of the first bone to set
     *               要设置的第一个骨骼的索引
     */
    public void setBonesAsMatrices(@EntityInstance int i,
            @NonNull Buffer matrices, @IntRange(from = 0, to = 255) int boneCount,
            @IntRange(from = 0) int offset) {
        int result = nSetBonesAsMatrices(mNativeObject, i, matrices, matrices.remaining(), boneCount, offset);
        if (result < 0) {
            throw new BufferOverflowException();
        }
    }

    /**
     * Sets the transforms associated with each bone of a Renderable.
     * 设置与可渲染对象的每个骨骼关联的变换。
     *
     * @param i Instance of the Renderable
     *          可渲染对象的实例
     * @param quaternions A FloatBuffer containing boneCount transforms. Each transform consists of 8 float.
     *                    float 0 to 3 encode a unit quaternion w+ix+jy+kz stored as x,y,z,w.
     *                    float 4 to 7 encode a translation stored as x,y,z,1
     *                    包含boneCount个变换的FloatBuffer。每个变换由8个浮点数组成。
     *                    浮点数0到3编码单位四元数w+ix+jy+kz，存储为x,y,z,w。
     *                    浮点数4到7编码平移，存储为x,y,z,1
     * @param boneCount Number of bones to set
     *                  要设置的骨骼数量
     * @param offset Index of the first bone to set
     *               要设置的第一个骨骼的索引
     */
    public void setBonesAsQuaternions(@EntityInstance int i,
            @NonNull Buffer quaternions, @IntRange(from = 0, to = 255) int boneCount,
            @IntRange(from = 0) int offset) {
        int result = nSetBonesAsQuaternions(mNativeObject, i, quaternions, quaternions.remaining(), boneCount, offset);
        if (result < 0) {
            throw new BufferOverflowException();
        }
    }

    /**
     * Updates the vertex morphing weights on a renderable, all zeroes by default.
     * 更新可渲染对象上的顶点形变权重，默认全为零。
     *
     * <p>The renderable must be built with morphing enabled. In legacy morphing mode, only the
     * first 4 weights are considered.</p>
     * <p>可渲染对象必须在启用形变的情况下构建。在传统形变模式下，只考虑前4个权重。</p>
     *
     * @see Builder#morphing
     */
    public void setMorphWeights(@EntityInstance int i, @NonNull float[] weights, @IntRange(from = 0) int offset) {
        nSetMorphWeights(mNativeObject, i, weights, offset);
    }

    /**
     * Changes the morph target buffer for the given primitive.
     * 更改给定图元的形变目标缓冲区。
     *
     * <p>The renderable must be built with morphing enabled.</p>
     * <p>可渲染对象必须在启用形变的情况下构建。</p>
     *
     * @see Builder#morphing
     */
    public void setMorphTargetBufferOffsetAt(@EntityInstance int i,
                                       @IntRange(from = 0) int level,
                                       @IntRange(from = 0) int primitiveIndex,
                                       @IntRange(from = 0) int offset) {
        nSetMorphTargetBufferOffsetAt(mNativeObject, i, level, primitiveIndex, 0, offset);
    }

    /**
     * Gets the morph target count on a renderable.
     * 获取可渲染对象上的形变目标计数。
     */
    @IntRange(from = 0)
    public int getMorphTargetCount(@EntityInstance int i) {
        return nGetMorphTargetCount(mNativeObject, i);
    }

    /**
     * Changes the bounding box used for frustum culling.
     * 更改用于视锥体剔除的包围盒。
     *
     * @see Builder#boundingBox
     * @see RenderableManager#getAxisAlignedBoundingBox
     */
    public void setAxisAlignedBoundingBox(@EntityInstance int i, @NonNull Box aabb) {
        nSetAxisAlignedBoundingBox(mNativeObject, i,
                aabb.getCenter()[0], aabb.getCenter()[1], aabb.getCenter()[2],
                aabb.getHalfExtent()[0], aabb.getHalfExtent()[1], aabb.getHalfExtent()[2]);
    }

    /**
     * Changes the visibility bits.
     * 更改可见性位。
     *
     * @see Builder#layerMask
     * @see View#setVisibleLayers
     */
    public void setLayerMask(@EntityInstance int i, @IntRange(from = 0, to = 255) int select,
            @IntRange(from = 0, to = 255) int value) {
        nSetLayerMask(mNativeObject, i, select, value);
    }

    /**
     * Changes the coarse-level draw ordering.
     * 更改粗粒度绘制顺序。
     *
     * @see Builder#priority
     */
    public void setPriority(@EntityInstance int i, @IntRange(from = 0, to = 7) int priority) {
        nSetPriority(mNativeObject, i, priority);
    }

    /**
     * Changes the channel of a renderable
     * 更改可渲染对象的通道
     *
     * @see Builder#channel
     */
    public void setChannel(@EntityInstance int i, @IntRange(from = 0, to = 3) int channel) {
        nSetChannel(mNativeObject, i, channel);
    }

    /**
     * Changes whether or not frustum culling is on.
     * 更改是否启用视锥体剔除。
     *
     * @see Builder#culling
     */
    public void setCulling(@EntityInstance int i, boolean enabled) {
        nSetCulling(mNativeObject, i, enabled);
    }

    /**
     * Changes whether or not the large-scale fog is applied to this renderable
     * 更改是否将大尺度雾应用于此可渲染对象
     * @see Builder#fog
     */
    public void setFogEnabled(@EntityInstance int i, boolean enabled) {
        nSetFogEnabled(mNativeObject, i, enabled);
    }

    /**
     * Returns whether large-scale fog is enabled for this renderable.
     * 返回此可渲染对象是否启用了大尺度雾。
     * @return True if fog is enabled for this renderable.
     *         如果此可渲染对象启用了雾，则返回true。
     * @see Builder#fog
     */
    public boolean getFogEnabled(@EntityInstance int i) {
        return nGetFogEnabled(mNativeObject, i);
    }

    /**
     * Enables or disables a light channel.
     * Light channel 0 is enabled by default.
     * 启用或禁用光照通道。
     * 光照通道0默认启用。
     *
     * @param i        Instance of the component obtained from getInstance().
     *                 从getInstance()获得的组件实例。
     * @param channel  Light channel to set
     *                 要设置的光照通道
     * @param enable   true to enable, false to disable
     *                 true表示启用，false表示禁用
     *
     * @see Builder#lightChannel
     */
    public void setLightChannel(@EntityInstance int i, @IntRange(from = 0, to = 7) int channel, boolean enable) {
        nSetLightChannel(mNativeObject, i, channel, enable);
    }

    /**
     * Returns whether a light channel is enabled on a specified renderable.
     * 返回指定可渲染对象上是否启用了光照通道。
     * @param i        Instance of the component obtained from getInstance().
     *                 从getInstance()获得的组件实例。
     * @param channel  Light channel to query
     *                 要查询的光照通道
     * @return         true if the light channel is enabled, false otherwise
     *                 如果光照通道已启用则返回true，否则返回false
     */
    public boolean getLightChannel(@EntityInstance int i, @IntRange(from = 0, to = 7) int channel) {
        return nGetLightChannel(mNativeObject, i, channel);
    }

    /**
     * Changes whether or not the renderable casts shadows.
     * 更改可渲染对象是否投射阴影。
     *
     * @see Builder#castShadows
     */
    public void setCastShadows(@EntityInstance int i, boolean enabled) {
        nSetCastShadows(mNativeObject, i, enabled);
    }

    /**
     * Changes whether or not the renderable can receive shadows.
     * 更改可渲染对象是否可以接收阴影。
     *
     * @see Builder#receiveShadows
     */
    public void setReceiveShadows(@EntityInstance int i, boolean enabled) {
        nSetReceiveShadows(mNativeObject, i, enabled);
    }

    /**
     * Changes whether or not the renderable can use screen-space contact shadows.
     * 更改可渲染对象是否可以使用屏幕空间接触阴影。
     *
     * @see Builder#screenSpaceContactShadows
     */
    public void setScreenSpaceContactShadows(@EntityInstance int i, boolean enabled) {
        nSetScreenSpaceContactShadows(mNativeObject, i, enabled);
    }

    /**
     * Checks if the renderable can cast shadows.
     * 检查可渲染对象是否可以投射阴影。
     *
     * @see Builder#castShadows
     */
    public boolean isShadowCaster(@EntityInstance int i) {
        return nIsShadowCaster(mNativeObject, i);
    }

    /**
     * Checks if the renderable can receive shadows.
     * 检查可渲染对象是否可以接收阴影。
     *
     * @see Builder#receiveShadows
     */
    public boolean isShadowReceiver(@EntityInstance int i) {
        return nIsShadowReceiver(mNativeObject, i);
    }

    /**
     * Gets the bounding box used for frustum culling.
     * 获取用于视锥体剔除的包围盒。
     *
     * @see Builder#boundingBox
     * @see RenderableManager#setAxisAlignedBoundingBox
     */
    @NonNull
    public Box getAxisAlignedBoundingBox(@EntityInstance int i, @Nullable Box out) {
        if (out == null) out = new Box();
        nGetAxisAlignedBoundingBox(mNativeObject, i, out.getCenter(), out.getHalfExtent());
        return out;
    }

    /**
     * Gets the immutable number of primitives in the given renderable.
     * 获取给定可渲染对象中不可变的图元数量。
     */
    @IntRange(from = 0)
    public int getPrimitiveCount(@EntityInstance int i) {
        return nGetPrimitiveCount(mNativeObject, i);
    }

    /**
     * Changes the material instance binding for the given primitive.
     * 更改给定图元的材质实例绑定。
     *
     * @see Builder#material
     */
    public void setMaterialInstanceAt(@EntityInstance int i, @IntRange(from = 0) int primitiveIndex,
            @NonNull MaterialInstance materialInstance) {
        int required = materialInstance.getMaterial().getRequiredAttributesAsInt();
        int declared = nGetEnabledAttributesAt(mNativeObject, i, primitiveIndex);
        if ((declared & required) != required) {
            Platform.get().warn("setMaterialInstanceAt() on primitive "
                    + primitiveIndex + " of Renderable at " + i
                    + ": declared attributes " + getEnabledAttributesAt(i, primitiveIndex)
                    + " do no satisfy required attributes " + materialInstance.getMaterial().getRequiredAttributes());
        }
        nSetMaterialInstanceAt(mNativeObject, i, primitiveIndex, materialInstance.getNativeObject());
    }

    /**
     * Clears the material instance for the given primitive.
     * 清除给定图元的材质实例。
     */
    public void clearMaterialInstanceAt(@EntityInstance int i, @IntRange(from = 0) int primitiveIndex) {
        nClearMaterialInstanceAt(mNativeObject, i, primitiveIndex);
    }

    /**
     * Creates a MaterialInstance Java wrapper object for a particular material instance.
     * 为特定材质实例创建MaterialInstance Java包装器对象。
     */
    public @NonNull MaterialInstance getMaterialInstanceAt(@EntityInstance int i,
            @IntRange(from = 0) int primitiveIndex) {
        long nativeMatInstance = nGetMaterialInstanceAt(mNativeObject, i, primitiveIndex);
        return new MaterialInstance(nativeMatInstance);
    }

    /**
     * Changes the geometry for the given primitive.
     * 更改给定图元的几何体。
     *
     * @see Builder#geometry Builder.geometry
     */
    public void setGeometryAt(@EntityInstance int i, @IntRange(from = 0) int primitiveIndex,
            @NonNull PrimitiveType type, @NonNull VertexBuffer vertices,
            @NonNull IndexBuffer indices, @IntRange(from = 0) int offset,
            @IntRange(from = 0) int count) {
        nSetGeometryAt(mNativeObject, i, primitiveIndex, type.getValue(), vertices.getNativeObject(), indices.getNativeObject(), offset, count);
    }

    /**
     * Changes the geometry for the given primitive.
     * 更改给定图元的几何体。
     *
     * @see Builder#geometry Builder.geometry
     */
    public void setGeometryAt(@EntityInstance int i, @IntRange(from = 0) int primitiveIndex,
            @NonNull PrimitiveType type, @NonNull VertexBuffer vertices,
            @NonNull IndexBuffer indices) {
        nSetGeometryAt(mNativeObject, i, primitiveIndex, type.getValue(), vertices.getNativeObject(), indices.getNativeObject(),
                0, indices.getIndexCount());
    }

     /**
     * Changes the drawing order for blended primitives. The drawing order is either global or
     * local (default) to this Renderable. In either case, the Renderable priority takes precedence.
     * 更改混合图元的绘制顺序。绘制顺序可以是全局的或此可渲染对象的本地（默认）。
     * 无论哪种情况，可渲染对象优先级都优先。
     *
     * @see Builder#blendOrder
     *
     * @param instance the renderable of interest
     *                 感兴趣的可渲染对象
     * @param primitiveIndex the primitive of interest
     *                       感兴趣的图元
     * @param blendOrder draw order number (0 by default). Only the lowest 15 bits are used.
     *                   绘制顺序号（默认为0）。只使用最低15位。
     */
    public void setBlendOrderAt(@EntityInstance int instance, @IntRange(from = 0) int primitiveIndex,
            @IntRange(from = 0, to = 65535) int blendOrder) {
        nSetBlendOrderAt(mNativeObject, instance, primitiveIndex, blendOrder);
    }

    /**
     * Changes whether the blend order is global or local to this Renderable (by default).
     * 更改混合顺序是全局的还是此可渲染对象的本地（默认）。
     *
     * @see Builder#globalBlendOrderEnabled
     *
     * @param instance the renderable of interest
     *                 感兴趣的可渲染对象
     * @param primitiveIndex the primitive of interest
     *                       感兴趣的图元
     * @param enabled true for global, false for local blend ordering.
     *                true表示全局，false表示本地混合排序。
     */
    public void setGlobalBlendOrderEnabledAt(@EntityInstance int instance, @IntRange(from = 0) int primitiveIndex,
            boolean enabled) {
        nSetGlobalBlendOrderEnabledAt(mNativeObject, instance, primitiveIndex, enabled);
    }

    /**
     * Retrieves the set of enabled attribute slots in the given primitive's VertexBuffer.
     * 检索给定图元的VertexBuffer中已启用的属性槽集合。
     */
    public Set<VertexBuffer.VertexAttribute> getEnabledAttributesAt(
            @EntityInstance int i, @IntRange(from = 0) int primitiveIndex) {
        int bitSet = nGetEnabledAttributesAt(mNativeObject, i, primitiveIndex);
        Set<VertexBuffer.VertexAttribute> requiredAttributes =
                EnumSet.noneOf(VertexBuffer.VertexAttribute.class);
        VertexBuffer.VertexAttribute[] values = sVertexAttributeValues;

        for (int j = 0; j < values.length; j++) {
            if ((bitSet & (1 << j)) != 0) {
                requiredAttributes.add(values[j]);
            }
        }

        requiredAttributes = Collections.unmodifiableSet(requiredAttributes);
        return requiredAttributes;
    }

    public long getNativeObject() {
        return mNativeObject;
    }

    private static native boolean nHasComponent(long nativeRenderableManager, int entity);
    private static native int nGetInstance(long nativeRenderableManager, int entity);
    private static native void nDestroy(long nativeRenderableManager, int entity);

    private static native long nCreateBuilder(int count);
    private static native void nDestroyBuilder(long nativeBuilder);
    private static native boolean nBuilderBuild(long nativeBuilder, long nativeEngine, int entity);

    private static native void nBuilderGeometry(long nativeBuilder, int index, int value, long nativeVertexBuffer, long nativeIndexBuffer);
    private static native void nBuilderGeometry(long nativeBuilder, int index, int value, long nativeVertexBuffer, long nativeIndexBuffer, int offset, int count);
    private static native void nBuilderGeometry(long nativeBuilder, int index, int value, long nativeVertexBuffer, long nativeIndexBuffer, int offset, int minIndex, int maxIndex, int count);
    private static native void nBuilderGeometryType(long nativeBuilder, int type);
    private static native void nBuilderMaterial(long nativeBuilder, int index, long nativeMaterialInstance);
    private static native void nBuilderBlendOrder(long nativeBuilder, int index, int blendOrder);
    private static native void nBuilderGlobalBlendOrderEnabled(long nativeBuilder, int index, boolean enabled);
    private static native void nBuilderBoundingBox(long nativeBuilder, float cx, float cy, float cz, float ex, float ey, float ez);
    private static native void nBuilderLayerMask(long nativeBuilder, int select, int value);
    private static native void nBuilderPriority(long nativeBuilder, int priority);
    private static native void nBuilderChannel(long nativeBuilder, int channel);
    private static native void nBuilderCulling(long nativeBuilder, boolean enabled);
    private static native void nBuilderCastShadows(long nativeBuilder, boolean enabled);
    private static native void nBuilderReceiveShadows(long nativeBuilder, boolean enabled);
    private static native void nBuilderScreenSpaceContactShadows(long nativeBuilder, boolean enabled);
    private static native void nBuilderSkinning(long nativeBuilder, int boneCount);
    private static native int nBuilderSkinningBones(long nativeBuilder, int boneCount, Buffer bones, int remaining);
    private static native void nBuilderSkinningBuffer(long nativeBuilder, long nativeSkinningBuffer, int boneCount, int offset);
    private static native void nBuilderMorphing(long nativeBuilder, int targetCount);
    private static native void nBuilderMorphingStandard(long nativeBuilder, long nativeMorphTargetBuffer);
    private static native void nBuilderSetMorphTargetBufferOffsetAt(long nativeBuilder, int level, int primitiveIndex, int offset);
    private static native void nBuilderEnableSkinningBuffers(long nativeBuilder, boolean enabled);
    private static native void nBuilderFog(long nativeBuilder, boolean enabled);
    private static native void nBuilderLightChannel(long nativeRenderableManager, int channel, boolean enable);
    private static native void nBuilderInstances(long nativeRenderableManager, int instances);

    private static native void nSetSkinningBuffer(long nativeObject, int i, long nativeSkinningBuffer, int count, int offset);
    private static native int nSetBonesAsMatrices(long nativeObject, int i, Buffer matrices, int remaining, int boneCount, int offset);
    private static native int nSetBonesAsQuaternions(long nativeObject, int i, Buffer quaternions, int remaining, int boneCount, int offset);
    private static native void nSetMorphWeights(long nativeObject, int instance, float[] weights, int offset);
    private static native void nSetMorphTargetBufferOffsetAt(long nativeObject, int i, int level, int primitiveIndex, long nativeMorphTargetBuffer, int offset);
    private static native int nGetMorphTargetCount(long nativeObject, int i);
    private static native void nSetAxisAlignedBoundingBox(long nativeRenderableManager, int i, float cx, float cy, float cz, float ex, float ey, float ez);
    private static native void nSetLayerMask(long nativeRenderableManager, int i, int select, int value);
    private static native void nSetPriority(long nativeRenderableManager, int i, int priority);
    private static native void nSetChannel(long nativeRenderableManager, int i, int channel);
    private static native void nSetCulling(long nativeRenderableManager, int i, boolean enabled);
    private static native void nSetFogEnabled(long nativeRenderableManager, int i, boolean enabled);
    private static native boolean nGetFogEnabled(long nativeRenderableManager, int i);
    private static native void nSetLightChannel(long nativeRenderableManager, int i, int channel, boolean enable);
    private static native boolean nGetLightChannel(long nativeRenderableManager, int i, int channel);
    private static native void nSetCastShadows(long nativeRenderableManager, int i, boolean enabled);
    private static native void nSetReceiveShadows(long nativeRenderableManager, int i, boolean enabled);
    private static native void nSetScreenSpaceContactShadows(long nativeRenderableManager, int i, boolean enabled);
    private static native boolean nIsShadowCaster(long nativeRenderableManager, int i);
    private static native boolean nIsShadowReceiver(long nativeRenderableManager, int i);
    private static native void nGetAxisAlignedBoundingBox(long nativeRenderableManager, int i, float[] center, float[] halfExtent);
    private static native int nGetPrimitiveCount(long nativeRenderableManager, int i);
    private static native void nSetMaterialInstanceAt(long nativeRenderableManager, int i, int primitiveIndex, long nativeMaterialInstance);
    private static native void nClearMaterialInstanceAt(long nativeRenderableManager, int i, int primitiveIndex);
    private static native long nGetMaterialInstanceAt(long nativeRenderableManager, int i, int primitiveIndex);
    private static native void nSetGeometryAt(long nativeRenderableManager, int i, int primitiveIndex, int primitiveType, long nativeVertexBuffer, long nativeIndexBuffer, int offset, int count);
    private static native void nSetBlendOrderAt(long nativeRenderableManager, int i, int primitiveIndex, int blendOrder);
    private static native void nSetGlobalBlendOrderEnabledAt(long nativeRenderableManager, int i, int primitiveIndex, boolean enabled);
    private static native int nGetEnabledAttributesAt(long nativeRenderableManager, int i, int primitiveIndex);
}
