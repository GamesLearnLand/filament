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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * A <code>Scene</code> is a flat container of {@link RenderableManager} and {@link LightManager}
 * components.
 * <code>Scene</code> 是 {@link RenderableManager} 和 {@link LightManager} 组件的扁平容器。
 * <br>
 * <p>A <code>Scene</code> doesn't provide a hierarchy of objects, i.e.: it's not a scene-graph.
 * However, it manages the list of objects to render and the list of lights. These can
 * be added or removed from a <code>Scene</code> at any time.
 * Moreover clients can use {@link TransformManager} to create a graph of transforms.</p>
 * <p><code>Scene</code> 不提供对象的层次结构，即：它不是场景图。
 * 但是，它管理要渲染的对象列表和灯光列表。这些可以随时从 <code>Scene</code> 中添加或删除。
 * 此外，客户端可以使用 {@link TransformManager} 创建变换图。</p>
 * <br>
 * <p>A {@link RenderableManager} component <b>must</b> be added to a <code>Scene</code> in order
 * to be rendered, and the <code>Scene</code> must be provided to a {@link View}.</p>
 * <p>{@link RenderableManager} 组件<b>必须</b>添加到 <code>Scene</code> 中才能被渲染，
 * 并且 <code>Scene</code> 必须提供给 {@link View}。</p>
 *
 * <h1>Creation and Destruction</h1>
 * <h1>创建和销毁</h1>
 *
 * A <code>Scene</code> is created using {@link Engine#createScene} and destroyed using
 * {@link Engine#destroyScene(Scene)}.
 * <code>Scene</code> 使用 {@link Engine#createScene} 创建，使用 {@link Engine#destroyScene(Scene)} 销毁。
 *
 * @see View
 * @see LightManager
 * @see RenderableManager
 * @see TransformManager
 */
public class Scene {
    private long mNativeObject;
    private @Nullable Skybox mSkybox;
    private @Nullable IndirectLight mIndirectLight;

    Scene(long nativeScene) {
        mNativeObject = nativeScene;
    }

    /**
     * @return the {@link Skybox} or <code>null</code> if none is set
     *         {@link Skybox} 或如果未设置则返回 <code>null</code>
     * @see #setSkybox(Skybox)
     */
    @Nullable
    public Skybox getSkybox() {
        return mSkybox;
    }

    /**
     * Sets the {@link Skybox}.
     * 设置 {@link Skybox}。
     *
     * The {@link Skybox} is drawn last and covers all pixels not touched by geometry.
     * {@link Skybox} 最后绘制，覆盖所有未被几何体触及的像素。
     *
     * @param skybox the {@link Skybox} to use to fill untouched pixels,
     *               or <code>null</code> to unset the {@link Skybox}.
     *               用于填充未触及像素的 {@link Skybox}，
     *               或 <code>null</code> 来取消设置 {@link Skybox}。
     */
    public void setSkybox(@Nullable Skybox skybox) {
        mSkybox = skybox;
        nSetSkybox(getNativeObject(), mSkybox != null ? mSkybox.getNativeObject() : 0);
    }

    /**
     * @return the {@link IndirectLight} or <code>null</code> if none is set
     *         {@link IndirectLight} 或如果未设置则返回 <code>null</code>
     * @see #setIndirectLight(IndirectLight)
     */
    @Nullable
    public IndirectLight getIndirectLight() {
        return mIndirectLight;
    }

    /**
     * Sets the {@link IndirectLight} to use when rendering the <code>Scene</code>.
     * 设置渲染 <code>Scene</code> 时使用的 {@link IndirectLight}。
     *
     * Currently, a <code>Scene</code> may only have a single {@link IndirectLight}.
     * This call replaces the current {@link IndirectLight}.
     * 目前，<code>Scene</code> 只能有一个 {@link IndirectLight}。
     * 此调用会替换当前的 {@link IndirectLight}。
     *
     * @param ibl the {@link IndirectLight} to use when rendering the <code>Scene</code>
     *            or <code>null</code> to unset.
     *            渲染 <code>Scene</code> 时使用的 {@link IndirectLight}
     *            或 <code>null</code> 来取消设置。
     */
    public void setIndirectLight(@Nullable IndirectLight ibl) {
        mIndirectLight = ibl;
        nSetIndirectLight(getNativeObject(),
                mIndirectLight != null ? mIndirectLight.getNativeObject() : 0);
    }

    /**
     * Adds an {@link Entity} to the <code>Scene</code>.
     * 向 <code>Scene</code> 添加一个 {@link Entity}。
     *
     * @param entity the entity is ignored if it doesn't have a {@link RenderableManager} component
     *               or {@link LightManager} component.<br>
     *               A given {@link Entity} object can only be added once to a <code>Scene</code>.
     *               如果实体没有 {@link RenderableManager} 组件或 {@link LightManager} 组件，则会被忽略。<br>
     *               给定的 {@link Entity} 对象只能向 <code>Scene</code> 添加一次。
     */
    public void addEntity(@Entity int entity) {
        nAddEntity(getNativeObject(), entity);
    }

    /**
     * Adds a list of entities to the <code>Scene</code>.
     * 向 <code>Scene</code> 添加实体列表。
     *
     * @param entities array containing entities to add to the <code>Scene</code>.
     *                 包含要添加到 <code>Scene</code> 的实体的数组。
     */
    public void addEntities(@Entity int[] entities) {
        nAddEntities(getNativeObject(), entities);
    }

    /**
     * Removes an {@link Entity} from the <code>Scene</code>.
     * 从 <code>Scene</code> 中移除一个 {@link Entity}。
     *
     * @param entity the {@link Entity} to remove from the <code>Scene</code>. If the specified
     *                   <code>entity</code> doesn't exist, this call is ignored.
     *               要从 <code>Scene</code> 中移除的 {@link Entity}。如果指定的
     *               <code>entity</code> 不存在，此调用将被忽略。
     */
    public void removeEntity(@Entity int entity) {
        nRemove(getNativeObject(), entity);
    }

    /**
     * @deprecated See {@link #removeEntity(int)}
     *             请参见 {@link #removeEntity(int)}
     */
    @Deprecated
    public void remove(@Entity int entity) {
        removeEntity(entity);
    }

    /**
     * Removes a list of entities from the <code>Scene</code>.
     * 从 <code>Scene</code> 中移除实体列表。
     *
     * This is equivalent to calling remove in a loop.
     * If any of the specified entities do not exist in the scene, they are skipped.
     * 这等同于在循环中调用 remove。
     * 如果指定的任何实体在场景中不存在，它们将被跳过。
     *
     * @param entities array containing entities to remove from the <code>Scene</code>.
     *                 包含要从 <code>Scene</code> 中移除的实体的数组。
     */
    public void removeEntities(@Entity int[] entities) {
        nRemoveEntities(getNativeObject(), entities);
    }

    /**
     * Returns the total number of Entities in the <code>Scene</code>, whether alive or not.
     * 返回 <code>Scene</code> 中实体的总数，无论是否活跃。
     *
     * @return the total number of Entities in the <code>Scene</code>.
     *         <code>Scene</code> 中实体的总数。
     */
    public int getEntityCount() {
        return nGetEntityCount(getNativeObject());
    }

    /**
     * Returns the number of active (alive) {@link RenderableManager} components in the
     * <code>Scene</code>.
     * 返回 <code>Scene</code> 中活跃的 {@link RenderableManager} 组件数量。
     *
     * @return number of {@link RenderableManager} components in the <code>Scene</code>.
     *         <code>Scene</code> 中 {@link RenderableManager} 组件的数量。
     */
    public int getRenderableCount() {
        return nGetRenderableCount(getNativeObject());
    }

    /**
     * Returns the number of active (alive) {@link LightManager} components in the
     * <code>Scene</code>.
     * 返回 <code>Scene</code> 中活跃的 {@link LightManager} 组件数量。
     *
     * @return number of {@link LightManager} components in the <code>Scene</code>.
     *         <code>Scene</code> 中 {@link LightManager} 组件的数量。
     */
    public int getLightCount() {
        return nGetLightCount(getNativeObject());
    }

    /**
     * Returns true if the given entity is in the Scene.
     * 如果给定实体在 Scene 中则返回 true。
     *
     * @return Whether the given entity is in the Scene.
     *         给定实体是否在 Scene 中。
     */
    public boolean hasEntity(@Entity int entity) {
        return nHasEntity(getNativeObject(), entity);
    }

    public long getNativeObject() {
        if (mNativeObject == 0) {
            throw new IllegalStateException("Calling method on destroyed Scene");
        }
        return mNativeObject;
    }

    /**
     * Returns the list of all entities in the Scene. If outArray is provided and large enough,
     * it is used to store the list and returned, otherwise a new array is allocated and returned.
     * 返回 Scene 中所有实体的列表。如果提供了 outArray 且足够大，
     * 则使用它来存储列表并返回，否则分配并返回一个新数组。
     * @param outArray an array to store the list of entities in the scene.
     *                 用于存储场景中实体列表的数组。
     * @return outArray if it was used or a newly allocated array.
     *         如果使用了 outArray 则返回它，否则返回新分配的数组。
     * @see #getEntityCount
     */
    public int[] getEntities(@Nullable int[] outArray) {
        int c = getEntityCount();
        if (outArray == null || outArray.length < c) {
            outArray = new int[c];
        }
        boolean success = nGetEntities(getNativeObject(), outArray, outArray.length);
        if (!success) {
            throw new IllegalStateException("Error retriving Scene's entities");
        }
        return outArray;
    }

    /**
     * Returns the list of all entities in the Scene in a newly allocated array.
     * 在新分配的数组中返回 Scene 中所有实体的列表。
     * @return an array containing the list of all entities in the scene.
     *         包含场景中所有实体列表的数组。
     * @see #getEntityCount
     */
    public int[] getEntities() {
        return getEntities(null);
    }

    public interface EntityProcessor {
        void process(@Entity int entity);
    }

    /**
     * Invokes user functor on each entity in the scene.
     * 对场景中的每个实体调用用户函数。
     *
     * It is not allowed to add or remove an entity from the scene within the functor.
     * 不允许在函数内向场景添加或从场景移除实体。
     *
     * @param entityProcessor User provided functor called for each entity in the scene
     *                        用户提供的函数，对场景中的每个实体调用
     */
    public void forEach(@NonNull EntityProcessor entityProcessor) {
        int[] entities = getEntities(null);
        for (int entity : entities) {
            entityProcessor.process(entity);
        }
    }

    void clearNativeObject() {
        mNativeObject = 0;
    }

    private static native void nSetSkybox(long nativeScene, long nativeSkybox);
    private static native void nSetIndirectLight(long nativeScene, long nativeIndirectLight);
    private static native void nAddEntity(long nativeScene, int entity);
    private static native void nAddEntities(long nativeScene, int[] entities);
    private static native void nRemove(long nativeScene, int entity);
    private static native void nRemoveEntities(long nativeScene, int[] entities);
    private static native int nGetEntityCount(long nativeScene);
    private static native int nGetRenderableCount(long nativeScene);
    private static native int nGetLightCount(long nativeScene);
    private static native boolean nHasEntity(long nativeScene, int entity);
    private static native boolean nGetEntities(long nativeScene, int[] outArray, int length);
}
