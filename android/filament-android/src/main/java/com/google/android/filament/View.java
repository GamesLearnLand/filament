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

import java.util.EnumSet;

import static com.google.android.filament.Asserts.assertFloat3In;
import static com.google.android.filament.Asserts.assertFloat4In;
import static com.google.android.filament.Colors.LinearColor;

import com.google.android.filament.proguard.UsedByNative;

/**
 * Encompasses all the state needed for rendering a {@link Scene}.
 * 包含渲染 {@link Scene} 所需的所有状态。
 *
 * <p>
 * {@link Renderer#render} operates on <code>View</code> objects. These <code>View</code> objects
 * specify important parameters such as:
 * {@link Renderer#render} 操作 <code>View</code> 对象。这些 <code>View</code> 对象
 * 指定重要的参数，例如：
 * </p>
 *
 * <ul>
 * <li>The Scene</li>
 * <li>场景</li>
 * <li>The Camera</li>
 * <li>相机</li>
 * <li>The Viewport</li>
 * <li>视口</li>
 * <li>Some rendering parameters</li>
 * <li>一些渲染参数</li>
 * </ul>
 *
 * <p>
 * <code>View</code> instances are heavy objects that internally cache a lot of data needed for
 * rendering. It is not advised for an application to use many View objects.
 * <code>View</code> 实例是重量级对象，内部缓存了大量渲染所需的数据。
 * 不建议应用程序使用太多 View 对象。
 * </p>
 *
 * <p>
 * For example, in a game, a <code>View</code> could be used for the main scene and another one for
 * the game's user interface. More <code>View</code> instances could be used for creating special
 * effects (e.g. a <code>View</code> is akin to a rendering pass).
 * 例如，在游戏中，一个 <code>View</code> 可以用于主场景，另一个用于
 * 游戏的用户界面。更多的 <code>View</code> 实例可以用于创建特殊
 * 效果（例如，一个 <code>View</code> 类似于一个渲染通道）。
 * </p>
 *
 * @see Renderer
 * @see Scene
 * @see Camera
 * @see RenderTarget
 */
public class View {
    private static final AntiAliasing[] sAntiAliasingValues = AntiAliasing.values();
    private static final Dithering[] sDitheringValues = Dithering.values();
    private static final AmbientOcclusion[] sAmbientOcclusionValues = AmbientOcclusion.values();

    private long mNativeObject;
    private String mName;
    private Scene mScene;
    private Camera mCamera;
    private Viewport mViewport = new Viewport(0, 0, 0, 0);
    private DynamicResolutionOptions mDynamicResolution;
    private RenderQuality mRenderQuality;
    private AmbientOcclusionOptions mAmbientOcclusionOptions;
    private BloomOptions mBloomOptions;
    private FogOptions mFogOptions;
    private StereoscopicOptions mStereoscopicOptions;
    private RenderTarget mRenderTarget;
    private BlendMode mBlendMode;
    private DepthOfFieldOptions mDepthOfFieldOptions;
    private VignetteOptions mVignetteOptions;
    private ColorGrading mColorGrading;
    private TemporalAntiAliasingOptions mTemporalAntiAliasingOptions;
    private ScreenSpaceReflectionsOptions mScreenSpaceReflectionsOptions;
    private MultiSampleAntiAliasingOptions mMultiSampleAntiAliasingOptions;
    private VsmShadowOptions mVsmShadowOptions;
    private SoftShadowOptions mSoftShadowOptions;
    private GuardBandOptions mGuardBandOptions;

    /**
     * List of available tone-mapping operators
     * 可用的色调映射操作符列表
     *
     * @deprecated Use ColorGrading instead
     * @deprecated 请使用 ColorGrading 代替
     */
    @Deprecated
    public enum ToneMapping {
        /**
         * Equivalent to disabling tone-mapping.
         * 等同于禁用色调映射。
         */
        LINEAR,

        /**
         * The Academy Color Encoding System (ACES).
         * 学院颜色编码系统 (ACES)。
         */
        ACES
    }

    /**
     * Used to select buffers.
     * 用于选择缓冲区。
     */
    public enum TargetBufferFlags {
        /**
         * Color 0 buffer selected.
         * 选择颜色0缓冲区。
         */
        COLOR0(0x1),
        /**
         * Color 1 buffer selected.
         * 选择颜色1缓冲区。
         */
        COLOR1(0x2),
        /**
         * Color 2 buffer selected.
         * 选择颜色2缓冲区。
         */
        COLOR2(0x4),
        /**
         * Color 3 buffer selected.
         * 选择颜色3缓冲区。
         */
        COLOR3(0x8),
        /**
         * Depth buffer selected.
         * 选择深度缓冲区。
         */
        DEPTH(0x10),
        /**
         * Stencil buffer selected.
         * 选择模板缓冲区。
         */
        STENCIL(0x20);

        /*
         * No buffer selected
         * 未选择缓冲区
         */
        public static EnumSet<TargetBufferFlags> NONE = EnumSet.noneOf(TargetBufferFlags.class);

        /*
         * All color buffers selected
         * 选择所有颜色缓冲区
         */
        public static EnumSet<TargetBufferFlags> ALL_COLOR =
                EnumSet.of(COLOR0, COLOR1, COLOR2, COLOR3);
        /**
         * Depth and stencil buffer selected.
         * 选择深度和模板缓冲区。
         */
        public static EnumSet<TargetBufferFlags> DEPTH_STENCIL = EnumSet.of(DEPTH, STENCIL);
        /**
         * All buffers are selected.
         * 选择所有缓冲区。
         */
        public static EnumSet<TargetBufferFlags> ALL = EnumSet.range(COLOR0, STENCIL);

        private int mFlags;

        TargetBufferFlags(int flags) {
            mFlags = flags;
        }

        static int flags(EnumSet<TargetBufferFlags> flags) {
            int result = 0;
            for (TargetBufferFlags flag : flags) {
                result |= flag.mFlags;
            }
            return result;
        }
    }

    View(long nativeView) {
        mNativeObject = nativeView;
    }

    /**
     * Sets the View's name. Only useful for debugging.
     * 设置View的名称。仅用于调试。
     */
    public void setName(@NonNull String name) {
        mName = name;
        nSetName(getNativeObject(), name);
    }

    /**
     * Returns the View's name.
     * 返回View的名称。
     */
    @Nullable
    public String getName() {
        return mName;
    }

    /**
     * Sets this View instance's Scene.
     * 设置此View实例的场景。
     *
     * <p>
     * This method associates the specified Scene with this View. Note that a particular scene can
     * be associated with several View instances. To remove an existing association, simply pass
     * null.
     * 此方法将指定的场景与此View关联。请注意，特定场景可以
     * 与多个View实例关联。要删除现有关联，只需传递null。
     * </p>
     *
     * <p>
     * The View does not take ownership of the Scene pointer. Before destroying a Scene, be sure
     * to remove it from all assoicated Views.
     * View不拥有Scene指针的所有权。在销毁Scene之前，请确保
     * 将其从所有关联的View中移除。
     * </p>
     *
     * @see #getScene
     */
    public void setScene(@Nullable Scene scene) {
        mScene = scene;
        nSetScene(getNativeObject(), scene == null ? 0 : scene.getNativeObject());
    }

    /**
     * Gets this View's associated Scene, or null if none has been assigned.
     * 获取此View关联的场景，如果未分配则返回null。
     *
     * @see #setScene
     */
    @Nullable
    public Scene getScene() {
        return mScene;
    }

    /**
     * Sets this View's Camera.
     * 设置此View的相机。
     *
     * <p>
     * This method associates the specified Camera with this View. A Camera can be associated with
     * several View instances. To remove an existing association, simply pass
     * null.
     * 此方法将指定的相机与此View关联。一个相机可以与
     * 多个View实例关联。要删除现有关联，只需传递null。
     * </p>
     *
     * <p>
     * The View does not take ownership of the Scene pointer. Before destroying a Camera, be sure
     * to remove it from all assoicated Views.
     * View不拥有Scene指针的所有权。在销毁相机之前，请确保
     * 将其从所有关联的View中移除。
     * </p>
     *
     * @see #getCamera
     */
    public void setCamera(@Nullable Camera camera) {
        mCamera = camera;
        nSetCamera(getNativeObject(), camera == null ? 0 : camera.getNativeObject());
    }

    /**
     * Query whether a camera is set.
     * 查询是否设置了相机。
     * @return true if a camera is set, false otherwise
     * @return 如果设置了相机则返回true，否则返回false
     * @see #setCamera
     */
    public boolean hasCamera() {
        return nHasCamera(getNativeObject());
    }

    /**
     * Gets this View's associated Camera, or null if none has been assigned.
     * 获取此View关联的相机，如果未分配则返回null。
     *
     * @see #setCamera
     */
    @Nullable
    public Camera getCamera() {
        return mCamera;
    }

    /**
     * Specifies the rectangular rendering area.
     * 指定矩形渲染区域。
     *
     * <p>
     * The viewport specifies where the content of the View (i.e. the Scene) is rendered in
     * the render target. The render target is automatically clipped to the Viewport.
     * 视口指定View的内容（即场景）在渲染目标中的渲染位置。
     * 渲染目标会自动裁剪到视口。
     * </p>
     *
     * <p>
     * If you wish subsequent changes to take effect please call this method again in order to
     * propagate the changes down to the native layer.
     * 如果您希望后续更改生效，请再次调用此方法以便
     * 将更改传播到本机层。
     * </p>
     *
     * @param viewport  The Viewport to render the Scene into.
     * @param viewport  要将场景渲染到的视口。
     */
    public void setViewport(@NonNull Viewport viewport) {
        mViewport = viewport;
        nSetViewport(getNativeObject(),
                mViewport.left, mViewport.bottom, mViewport.width, mViewport.height);
    }

    /**
     * Returns the rectangular rendering area.
     * 返回矩形渲染区域。
     *
     * @see #setViewport
     */
    @NonNull
    public Viewport getViewport() {
        return mViewport;
    }

    /**
     * Sets the blending mode used to draw the view into the SwapChain.
     * 设置用于将视图绘制到交换链中的混合模式。
     *
     * @param blendMode either {@link BlendMode#OPAQUE} or {@link BlendMode#TRANSLUCENT}
     * @param blendMode {@link BlendMode#OPAQUE} 或 {@link BlendMode#TRANSLUCENT}
     * @see #getBlendMode
     */
    public void setBlendMode(BlendMode blendMode) {
        mBlendMode = blendMode;
        nSetBlendMode(getNativeObject(), blendMode.ordinal());
    }

    /**
     * 获取混合模式。
     * @return blending mode set by setBlendMode
     * @return setBlendMode设置的混合模式
     * @see #setBlendMode
     */
    public BlendMode getBlendMode() {
        return mBlendMode;
    }

    /**
     * Sets which layers are visible.
     * 设置哪些层可见。
     *
     * <p>
     * Renderable objects can have one or several layers associated to them. Layers are
     * represented with an 8-bits bitmask, where each bit corresponds to a layer.
     * By default all layers are visible.
     * 可渲染对象可以有一个或多个与之关联的层。层用
     * 8位位掩码表示，其中每个位对应一个层。
     * 默认情况下所有层都可见。
     * </p>
     *
     * @see RenderableManager#setLayerMask
     *
     * @param select    a bitmask specifying which layer to set or clear using <code>values</code>.
     * @param select    指定使用<code>values</code>设置或清除哪个层的位掩码。
     * @param values    a bitmask where each bit sets the visibility of the corresponding layer
     *                  (1: visible, 0: invisible), only layers in <code>select</code> are affected.
     * @param values    位掩码，其中每个位设置相应层的可见性
     *                  （1：可见，0：不可见），只有<code>select</code>中的层受影响。
     */
    public void setVisibleLayers(
            @IntRange(from = 0, to = 255) int select,
            @IntRange(from = 0, to = 255) int values) {
        nSetVisibleLayers(getNativeObject(), select & 0xFF, values & 0xFF);
    }

    /**
     * Enables or disables shadow mapping. Enabled by default.
     * 启用或禁用阴影映射。默认启用。
     *
     * @see LightManager.Builder#castShadows
     * @see RenderableManager.Builder#receiveShadows
     * @see RenderableManager.Builder#castShadows
     */
    public void setShadowingEnabled(boolean enabled) {
        nSetShadowingEnabled(getNativeObject(), enabled);
    }

    /**
     * @return whether shadowing is enabled
     * @return 是否启用阴影
     */
    boolean isShadowingEnabled() {
        return nIsShadowingEnabled(getNativeObject());
    }

    /**
     * Enables or disables screen space refraction. Enabled by default.
     * 启用或禁用屏幕空间折射。默认启用。
     *
     * @param enabled true enables screen space refraction, false disables it.
     * @param enabled true启用屏幕空间折射，false禁用它。
     */
    public void setScreenSpaceRefractionEnabled(boolean enabled) {
        nSetScreenSpaceRefractionEnabled(getNativeObject(), enabled);
    }

    /**
     * @return whether screen space refraction is enabled
     * @return 是否启用屏幕空间折射
     */
    boolean isScreenSpaceRefractionEnabled() {
        return nIsScreenSpaceRefractionEnabled(getNativeObject());
    }

    /**
     * Specifies an offscreen render target to render into.
     * 指定要渲染到的离屏渲染目标。
     *
     * <p>
     * By default, the view's associated render target is null, which corresponds to the
     * SwapChain associated with the engine.
     * 默认情况下，视图关联的渲染目标为null，对应于
     * 与引擎关联的交换链。
     * </p>
     *
     * <p>
     * A view with a custom render target cannot rely on Renderer.ClearOptions, which only applies
     * to the SwapChain. Such view can use a Skybox instead.
     * 具有自定义渲染目标的视图不能依赖Renderer.ClearOptions，它仅适用于
     * 交换链。这样的视图可以使用天空盒代替。
     * </p>
     *
     * @param target render target associated with view, or null for the swap chain
     * @param target 与视图关联的渲染目标，或null表示交换链
     */
    public void setRenderTarget(@Nullable RenderTarget target) {
        mRenderTarget = target;
        nSetRenderTarget(getNativeObject(), target != null ? target.getNativeObject() : 0);
    }

    /**
     * Gets the offscreen render target associated with this view.
     * 获取与此视图关联的离屏渲染目标。
     *
     * Returns null if the render target is the swap chain (which is default).
     * 如果渲染目标是交换链（默认），则返回null。
     *
     * @see #setRenderTarget
     */
    @Nullable
    public RenderTarget getRenderTarget() {
        return mRenderTarget;
    }

    /**
     * Sets how many samples are to be used for MSAA in the post-process stage.
     * Default is 1 and disables MSAA.
     * 设置在后处理阶段用于MSAA的采样数量。
     * 默认值为1并禁用MSAA。
     *
     * <p>
     * Note that anti-aliasing can also be performed in the post-processing stage, generally at
     * lower cost. See the FXAA option in {@link #setAntiAliasing}.
     * 请注意，抗锯齿也可以在后处理阶段执行，通常成本较低。
     * 请参阅{@link #setAntiAliasing}中的FXAA选项。
     * </p>
     *
     * @param count number of samples to use for multi-sampled anti-aliasing.
     * @param count 用于多重采样抗锯齿的采样数量。
     *
     * @deprecated use setMultiSampleAntiAliasingOptions instead
     * @deprecated 请使用setMultiSampleAntiAliasingOptions代替
     */
    @Deprecated
    public void setSampleCount(int count) {
        nSetSampleCount(getNativeObject(), count);
    }

    /**
     * Returns the effective MSAA sample count.
     * 返回有效的MSAA采样数量。
     *
     * <p>
     * A value of 0 or 1 means MSAA is disabled.
     * 值为0或1表示MSAA被禁用。
     * </p>
     *
     * @return value set by {@link #setSampleCount}
     * @return {@link #setSampleCount}设置的值
     *
     * @deprecated use getMultiSampleAntiAliasingOptions instead
     * @deprecated 请使用getMultiSampleAntiAliasingOptions代替
     */
    @Deprecated
    public int getSampleCount() {
        return nGetSampleCount(getNativeObject());
    }

    /**
     * Enables or disables anti-aliasing in the post-processing stage. Enabled by default.
     * 在后处理阶段启用或禁用抗锯齿。默认启用。
     *
     * <p>
     * For MSAA anti-aliasing, see {@link #setSampleCount}.
     * 对于MSAA抗锯齿，请参阅{@link #setSampleCount}。
     * </p>
     *
     * @param type FXAA for enabling, NONE for disabling anti-aliasing.
     * @param type FXAA用于启用，NONE用于禁用抗锯齿。
     */
    public void setAntiAliasing(@NonNull AntiAliasing type) {
        nSetAntiAliasing(getNativeObject(), type.ordinal());
    }

    /**
     * Queries whether anti-aliasing is enabled during the post-processing stage. To query
     * whether MSAA is enabled, see {@link #getSampleCount}.
     * 查询在后处理阶段是否启用抗锯齿。要查询
     * 是否启用MSAA，请参阅{@link #getSampleCount}。
     *
     * @return The post-processing anti-aliasing method.
     * @return 后处理抗锯齿方法。
     */
    @NonNull
    public AntiAliasing getAntiAliasing() {
        return sAntiAliasingValues[nGetAntiAliasing(getNativeObject())];
    }

    /**
     * Enables or disable multi-sample anti-aliasing (MSAA). Disabled by default.
     * 启用或禁用多重采样抗锯齿（MSAA）。默认禁用。
     *
     * @param options multi-sample anti-aliasing options
     * @param options 多重采样抗锯齿选项
     */
    public void setMultiSampleAntiAliasingOptions(@NonNull MultiSampleAntiAliasingOptions options) {
        mMultiSampleAntiAliasingOptions = options;
        nSetMultiSampleAntiAliasingOptions(getNativeObject(),
                options.enabled, options.sampleCount, options.customResolve);
    }

    /**
     * Returns multi-sample anti-aliasing options.
     * 返回多重采样抗锯齿选项。
     *
     * @return multi-sample anti-aliasing options
     * @return 多重采样抗锯齿选项
     */
    @NonNull
    public MultiSampleAntiAliasingOptions getMultiSampleAntiAliasingOptions() {
        if (mMultiSampleAntiAliasingOptions == null) {
            mMultiSampleAntiAliasingOptions = new MultiSampleAntiAliasingOptions();
        }
        return mMultiSampleAntiAliasingOptions;
    }

    /**
     * Enables or disable temporal anti-aliasing (TAA). Disabled by default.
     * 启用或禁用时间抗锯齿（TAA）。默认禁用。
     *
     * @param options temporal anti-aliasing options
     * @param options 时间抗锯齿选项
     */
    public void setTemporalAntiAliasingOptions(@NonNull TemporalAntiAliasingOptions options) {
        mTemporalAntiAliasingOptions = options;
        nSetTemporalAntiAliasingOptions(getNativeObject(),
                options.feedback, options.filterWidth, options.enabled);
    }

    /**
     * Returns temporal anti-aliasing options.
     * 返回时间抗锯齿选项。
     *
     * @return temporal anti-aliasing options
     * @return 时间抗锯齿选项
     */
    @NonNull
    public TemporalAntiAliasingOptions getTemporalAntiAliasingOptions() {
        if (mTemporalAntiAliasingOptions == null) {
            mTemporalAntiAliasingOptions = new TemporalAntiAliasingOptions();
        }
        return mTemporalAntiAliasingOptions;
    }

    /**
     * Enables or disable screen-space reflections. Disabled by default.
     * 启用或禁用屏幕空间反射。默认禁用。
     *
     * @param options screen-space reflections options
     * @param options 屏幕空间反射选项
     */
    public void setScreenSpaceReflectionsOptions(@NonNull ScreenSpaceReflectionsOptions options) {
        mScreenSpaceReflectionsOptions = options;
        nSetScreenSpaceReflectionsOptions(getNativeObject(), options.thickness, options.bias,
                options.maxDistance, options.stride, options.enabled);
    }

    /**
     * Returns screen-space reflections options.
     * 返回屏幕空间反射选项。
     *
     * @return screen-space reflections options
     * @return 屏幕空间反射选项
     */
    @NonNull
    public ScreenSpaceReflectionsOptions getScreenSpaceReflectionsOptions() {
        if (mScreenSpaceReflectionsOptions == null) {
            mScreenSpaceReflectionsOptions = new ScreenSpaceReflectionsOptions();
        }
        return mScreenSpaceReflectionsOptions;
    }

    /**
     * Enables or disable screen-space guard band. Disabled by default.
     * 启用或禁用屏幕空间保护带。默认禁用。
     *
     * @param options guard band options
     * @param options 保护带选项
     */
    public void setGuardBandOptions(@NonNull GuardBandOptions options) {
        mGuardBandOptions = options;
        nSetGuardBandOptions(getNativeObject(), options.enabled);
    }

    /**
     * Returns screen-space guard band options.
     * 返回屏幕空间保护带选项。
     *
     * @return guard band options
     * @return 保护带选项
     */
    @NonNull
    public GuardBandOptions getGuardBandOptions() {
        if (mGuardBandOptions == null) {
            mGuardBandOptions = new GuardBandOptions();
        }
        return mGuardBandOptions;
    }


    /**
     * Enables or disables tone-mapping in the post-processing stage. Enabled by default.
     * 在后处理阶段启用或禁用色调映射。默认启用。
     *
     * @param type Tone-mapping function.
     * @param type 色调映射函数。
     *
     * @deprecated Use {@link #setColorGrading(com.google.android.filament.ColorGrading)}
     * @deprecated 请使用{@link #setColorGrading(com.google.android.filament.ColorGrading)}
     */
    @Deprecated
    public void setToneMapping(@NonNull ToneMapping type) {
    }

    /**
     * Returns the tone-mapping function.
     * 返回色调映射函数。
     * @return tone-mapping function.
     * @return 色调映射函数。
     *
     * @deprecated Use {@link #getColorGrading()}. This always returns {@link ToneMapping#ACES}
     * @deprecated 请使用{@link #getColorGrading()}。这总是返回{@link ToneMapping#ACES}
     */
    @Deprecated
    @NonNull
    public ToneMapping getToneMapping() {
        return ToneMapping.ACES;
    }

    /**
     * Sets this View's color grading transforms.
     * 设置此View的颜色分级变换。
     *
     * @param colorGrading Associate the specified {@link ColorGrading} to this view. A ColorGrading
     *                     can be associated to several View instances. Can be null to dissociate
     *                     the currently set ColorGrading from this View. Doing so will revert to
     *                     the use of the default color grading transforms.
     * @param colorGrading 将指定的{@link ColorGrading}与此视图关联。一个ColorGrading
     *                     可以与多个View实例关联。可以为null以取消
     *                     当前设置的ColorGrading与此View的关联。这样做将恢复到
     *                     使用默认的颜色分级变换。
     */
    public void setColorGrading(@Nullable ColorGrading colorGrading) {
        nSetColorGrading(getNativeObject(),
                colorGrading != null ? colorGrading.getNativeObject() : 0);
        mColorGrading = colorGrading;
    }

    /**
     * Returns the {@link ColorGrading} associated to this view.
     * 返回与此视图关联的{@link ColorGrading}。
     *
     * @return A {@link ColorGrading} or null if the default {@link ColorGrading} is in use
     * @return 一个{@link ColorGrading}，如果使用默认{@link ColorGrading}则返回null
     */
    public ColorGrading getColorGrading() {
        return mColorGrading;
    }

    /**
     * Enables or disables dithering in the post-processing stage. Enabled by default.
     * 在后处理阶段启用或禁用抖动。默认启用。
     *
     * @param dithering dithering type
     * @param dithering 抖动类型
     */
    public void setDithering(@NonNull Dithering dithering) {
        nSetDithering(getNativeObject(), dithering.ordinal());
    }

    /**
     * Queries whether dithering is enabled during the post-processing stage.
     * 查询在后处理阶段是否启用抖动。
     *
     * @return the current dithering type for this view.
     * @return 此视图的当前抖动类型。
     */
    @NonNull
    public Dithering getDithering() {
        return sDitheringValues[nGetDithering(getNativeObject())];
    }

    /**
     * Sets the dynamic resolution options for this view.
     * 设置此视图的动态分辨率选项。
     *
     * <p>
     * Dynamic resolution options controls whether dynamic resolution is enabled, and if it is,
     * how it behaves.
     * 动态分辨率选项控制是否启用动态分辨率，如果启用，
     * 它如何表现。
     * </p>
     *
     * <p>
     * If you wish subsequent changes to take effect please call this method again in order to
     * propagate the changes down to the native layer.
     * 如果您希望后续更改生效，请再次调用此方法以便
     * 将更改传播到本机层。
     * </p>
     *
     * @param options The dynamic resolution options to use on this view
     * @param options 在此视图上使用的动态分辨率选项
     */
    public void setDynamicResolutionOptions(@NonNull DynamicResolutionOptions options) {
        mDynamicResolution = options;
        nSetDynamicResolutionOptions(getNativeObject(),
                options.enabled,
                options.homogeneousScaling,
                options.minScale,
                options.maxScale,
                options.sharpness,
                options.quality.ordinal());
    }

    /**
     * Returns the dynamic resolution options associated with this view.
     * 返回与此视图关联的动态分辨率选项。
     * @return value set by {@link #setDynamicResolutionOptions}.
     * @return {@link #setDynamicResolutionOptions}设置的值。
     */
    @NonNull
    public DynamicResolutionOptions getDynamicResolutionOptions() {
        if (mDynamicResolution == null) {
            mDynamicResolution = new DynamicResolutionOptions();
        }
        return mDynamicResolution;
    }

    /**
     * Sets the rendering quality for this view (e.g. color precision).
     * 设置此视图的渲染质量（例如颜色精度）。
     *
     * @param renderQuality The render quality to use on this view
     * @param renderQuality 在此视图上使用的渲染质量
     */
    public void setRenderQuality(@NonNull RenderQuality renderQuality) {
        mRenderQuality = renderQuality;
        nSetRenderQuality(getNativeObject(), renderQuality.hdrColorBuffer.ordinal());
    }

    /**
     * Returns the render quality used by this view.
     * 返回此视图使用的渲染质量。
     * @return value set by {@link #setRenderQuality}.
     * @return {@link #setRenderQuality}设置的值。
     */
    @NonNull
    public RenderQuality getRenderQuality() {
        if (mRenderQuality == null) {
            mRenderQuality = new RenderQuality();
        }
        return mRenderQuality;
    }

    /**
     * Returns true if post-processing is enabled.
     * 如果启用后处理则返回true。
     *
     * @see #setPostProcessingEnabled
     */
    public boolean isPostProcessingEnabled() {
        return nIsPostProcessingEnabled(getNativeObject());
    }

    /**
     * Enables or disables post processing. Enabled by default.
     * 启用或禁用后处理。默认启用。
     *
     * <p>Post-processing includes:</p>
     * <p>后处理包括：</p>
     * <ul>
     * <li>Depth-of-field</li>
     * <li>景深</li>
     * <li>Bloom</li>
     * <li>泛光</li>
     * <li>Vignetting</li>
     * <li>暗角</li>
     * <li>Temporal Anti-aliasing (TAA)</li>
     * <li>时间抗锯齿（TAA）</li>
     * <li>Color grading & gamma encoding</li>
     * <li>色彩分级和伽马编码</li>
     * <li>Dithering</li>
     * <li>抖动</li>
     * <li>FXAA</li>
     * <li>FXAA</li>
     * <li>Dynamic scaling</li>
     * <li>动态缩放</li>
     * </ul>
     *
     * <p>
     * Disabling post-processing forgoes color correctness as well as some anti-aliasing techniques
     * and should only be used for debugging, UI overlays or when using custom render targets
     * (see RenderTarget).
     * 禁用后处理会放弃颜色正确性以及一些抗锯齿技术，
     * 应该仅用于调试、UI覆盖或使用自定义渲染目标时
     * （参见RenderTarget）。
     * </p>
     *
     * @param enabled true enables post processing, false disables it
     * @param enabled true启用后处理，false禁用它
     *
     * @see #setBloomOptions
     * @see #setColorGrading
     * @see #setAntiAliasing
     * @see #setDithering
     * @see #setSampleCount
     */
    public void setPostProcessingEnabled(boolean enabled) {
        nSetPostProcessingEnabled(getNativeObject(), enabled);
    }

    /**
     * Returns true if front face winding is inverted.
     * 如果前面朝向缠绕被反转则返回true。
     *
     * @see #setFrontFaceWindingInverted
     */
    public boolean isFrontFaceWindingInverted() {
        return nIsFrontFaceWindingInverted(getNativeObject());
    }

    /**
     * Inverts the winding order of front faces. By default front faces use a counter-clockwise
     * winding order. When the winding order is inverted, front faces are faces with a clockwise
     * winding order.
     * 反转前面的缠绕顺序。默认情况下，前面使用逆时针
     * 缠绕顺序。当缠绕顺序被反转时，前面是具有顺时针
     * 缠绕顺序的面。
     *
     * Changing the winding order will directly affect the culling mode in materials
     * (see Material#getCullingMode).
     * 更改缠绕顺序将直接影响材质中的剔除模式
     * （参见Material#getCullingMode）。
     *
     * Inverting the winding order of front faces is useful when rendering mirrored reflections
     * (water, mirror surfaces, front camera in AR, etc.).
     * 反转前面的缠绕顺序在渲染镜像反射时很有用
     * （水面、镜面、AR中的前置摄像头等）。
     *
     * @param inverted True to invert front faces, false otherwise.
     * @param inverted True反转前面，否则为false。
     */
    public void setFrontFaceWindingInverted(boolean inverted) {
        nSetFrontFaceWindingInverted(getNativeObject(), inverted);
    }

    /**
     * Returns true if transparent picking is enabled.
     * 如果启用透明拾取则返回true。
     *
     * @see #setTransparentPickingEnabled
     */
    public boolean isTransparentPickingEnabled() {
        return nIsTransparentPickingEnabled(getNativeObject());
    }

    /**
     * Enables or disables transparent picking. Disabled by default.
     * 启用或禁用透明拾取。默认禁用。
     *
     * When transparent picking is enabled, View::pick() will pick from both
     * transparent and opaque renderables. When disabled, View::pick() will only
     * pick from opaque renderables.
     * 当启用透明拾取时，View::pick()将从透明和不透明
     * 可渲染对象中拾取。当禁用时，View::pick()只会
     * 从不透明可渲染对象中拾取。
     *
     * <p>
     * Transparent picking will create an extra pass for rendering depth
     * from both transparent and opaque renderables. 
     * 透明拾取将创建一个额外的通道来渲染
     * 透明和不透明可渲染对象的深度。
     * </p>
     *
     * @param enabled true enables transparent picking, false disables it.
     * @param enabled true启用透明拾取，false禁用它。
     */
    public void setTransparentPickingEnabled(boolean enabled) {
        nSetTransparentPickingEnabled(getNativeObject(), enabled);
    }

    /**
     * Sets options relative to dynamic lighting for this view.
     * 设置此视图的动态光照相关选项。
     *
     * <p>
     * Together <code>zLightNear</code> and <code>zLightFar</code> must be chosen so that the
     * visible influence of lights is spread between these two values.
     * <code>zLightNear</code>和<code>zLightFar</code>必须一起选择，以便
     * 光源的可见影响分布在这两个值之间。
     * </p>
     *
     * @param zLightNear Distance from the camera where the lights are expected to shine.
     *                   This parameter can affect performance and is useful because depending
     *                   on the scene, lights that shine close to the camera may not be
     *                   visible -- in this case, using a larger value can improve performance.
     *                   e.g. when standing and looking straight, several meters of the ground
     *                   isn't visible and if lights are expected to shine there, there is no
     *                   point using a short zLightNear. (Default 5m).
     * @param zLightNear 距离摄像机预期光源照射的距离。
     *                   此参数可能影响性能，很有用，因为根据
     *                   场景，靠近摄像机照射的光源可能不
     *                   可见——在这种情况下，使用较大的值可以提高性能。
     *                   例如，当站立并直视时，地面的几米
     *                   不可见，如果光源预期在那里照射，则没有
     *                   必要使用短的zLightNear。（默认5m）。
     *
     * @param zLightFar Distance from the camera after which lights are not expected to be visible.
     *                  Similarly to zLightNear, setting this value properly can improve
     *                  performance. (Default 100m).
     * @param zLightFar 距离摄像机之后光源预期不可见的距离。
     *                  与zLightNear类似，正确设置此值可以提高
     *                  性能。（默认100m）。
     *
     */
    public void setDynamicLightingOptions(float zLightNear, float zLightFar) {
        nSetDynamicLightingOptions(getNativeObject(), zLightNear, zLightFar);
    }

    /**
     * Sets the shadow mapping technique this View uses.
     * 设置此视图使用的阴影映射技术。
     *
     * The ShadowType affects all the shadows seen within the View.
     * ShadowType影响视图中看到的所有阴影。
     *
     * <p>
     * {@link ShadowType#VSM} imposes a restriction on marking renderables as only shadow receivers
     * (but not casters). To ensure correct shadowing with VSM, all shadow participant renderables
     * should be marked as both receivers and casters. Objects that are guaranteed to not cast
     * shadows on themselves or other objects (such as flat ground planes) can be set to not cast
     * shadows, which might improve shadow quality.
     * {@link ShadowType#VSM}对将可渲染对象标记为仅阴影接收者
     * （但不是投射者）施加限制。为了确保VSM的正确阴影，所有阴影参与的可渲染对象
     * 应该被标记为接收者和投射者。保证不会在自己或其他对象上投射
     * 阴影的对象（如平坦的地面）可以设置为不投射
     * 阴影，这可能会提高阴影质量。
     * </p>
     *
     * <strong>Warning: This API is still experimental and subject to change.</strong>
     * <strong>警告：此API仍处于实验阶段，可能会发生变化。</strong>
     */
    public void setShadowType(ShadowType type) {
        nSetShadowType(getNativeObject(), type.ordinal());
    }

    /**
     * Sets VSM shadowing options that apply across the entire View.
     * 设置适用于整个视图的VSM阴影选项。
     *
     * Additional light-specific VSM options can be set with
     * {@link LightManager.Builder#shadowOptions}.
     * 可以使用{@link LightManager.Builder#shadowOptions}设置
     * 额外的特定光源VSM选项。
     *
     * Only applicable when shadow type is set to ShadowType::VSM.
     * 仅在阴影类型设置为ShadowType::VSM时适用。
     *
     * <strong>Warning: This API is still experimental and subject to change.</strong>
     * <strong>警告：此API仍处于实验阶段，可能会发生变化。</strong>
     *
     * @param options Options for shadowing.
     * @param options 阴影选项。
     * @see #setShadowType
     */
    public void setVsmShadowOptions(@NonNull VsmShadowOptions options) {
        mVsmShadowOptions = options;
        nSetVsmShadowOptions(getNativeObject(), options.anisotropy, options.mipmapping,
                options.highPrecision, options.minVarianceScale, options.lightBleedReduction);
    }

    /**
     * Gets the VSM shadowing options.
     * 获取VSM阴影选项。
     * @see #setVsmShadowOptions
     * @return VSM shadow options currently set.
     * @return 当前设置的VSM阴影选项。
     */
    @NonNull
    public VsmShadowOptions getVsmShadowOptions() {
        if (mVsmShadowOptions == null) {
            mVsmShadowOptions = new VsmShadowOptions();
        }
        return mVsmShadowOptions;
    }

    /**
     * Sets soft shadowing options that apply across the entire View.
     * 设置适用于整个视图的软阴影选项。
     *
     * Additional light-specific VSM options can be set with
     * {@link LightManager.Builder#shadowOptions}.
     * 可以使用{@link LightManager.Builder#shadowOptions}设置
     * 额外的特定光源VSM选项。
     *
     * Only applicable when shadow type is set to ShadowType.DPCF.
     * 仅在阴影类型设置为ShadowType.DPCF时适用。
     *
     * <strong>Warning: This API is still experimental and subject to change.</strong>
     * <strong>警告：此API仍处于实验阶段，可能会发生变化。</strong>
     *
     * @param options Options for shadowing.
     * @param options 阴影选项。
     * @see #setShadowType
     */
    public void setSoftShadowOptions(@NonNull SoftShadowOptions options) {
        mSoftShadowOptions = options;
        nSetSoftShadowOptions(getNativeObject(), options.penumbraScale, options.penumbraRatioScale);
    }

    /**
     * Gets soft shadowing options associated with this View.
     * 获取与此视图关联的软阴影选项。
     * @see #setSoftShadowOptions
     * @return soft shadow options currently set.
     * @return 当前设置的软阴影选项。
     */
    @NonNull
    public SoftShadowOptions getSoftShadowOptions() {
        if (mSoftShadowOptions == null) {
            mSoftShadowOptions = new SoftShadowOptions();
        }
        return mSoftShadowOptions;
    }

    /**
     * Activates or deactivates ambient occlusion.
     * 激活或停用环境光遮蔽。
     * @see #setAmbientOcclusionOptions
     * @param ao Type of ambient occlusion to use.
     * @param ao 要使用的环境光遮蔽类型。
     */
    @Deprecated
    public void setAmbientOcclusion(@NonNull AmbientOcclusion ao) {
        nSetAmbientOcclusion(getNativeObject(), ao.ordinal());
    }

    /**
     * Queries the type of ambient occlusion active for this View.
     * 查询此视图激活的环境光遮蔽类型。
     * @see #getAmbientOcclusionOptions
     * @return ambient occlusion type.
     * @return 环境光遮蔽类型。
     */
    @Deprecated
    @NonNull
    public AmbientOcclusion getAmbientOcclusion() {
        return sAmbientOcclusionValues[nGetAmbientOcclusion(getNativeObject())];
    }

    /**
     * Sets ambient occlusion options.
     * 设置环境光遮蔽选项。
     *
     * @param options Options for ambient occlusion.
     * @param options 环境光遮蔽选项。
     */
    public void setAmbientOcclusionOptions(@NonNull AmbientOcclusionOptions options) {
        mAmbientOcclusionOptions = options;
        nSetAmbientOcclusionOptions(getNativeObject(), options.radius, options.bias, options.power,
                options.resolution, options.intensity, options.bilateralThreshold,
                options.quality.ordinal(), options.lowPassFilter.ordinal(), options.upsampling.ordinal(),
                options.enabled, options.bentNormals, options.minHorizonAngleRad);
        nSetSSCTOptions(getNativeObject(), options.ssctLightConeRad, options.ssctShadowDistance,
                options.ssctContactDistanceMax,  options.ssctIntensity,
                options.ssctLightDirection[0], options.ssctLightDirection[1], options.ssctLightDirection[2],
                options.ssctDepthBias, options.ssctDepthSlopeBias, options.ssctSampleCount,
                options.ssctRayCount, options.ssctEnabled);
    }

    /**
     * Gets the ambient occlusion options.
     * 获取环境光遮蔽选项。
     *
     * @return ambient occlusion options currently set.
     * @return 当前设置的环境光遮蔽选项。
     */
    @NonNull
    public AmbientOcclusionOptions getAmbientOcclusionOptions() {
        if (mAmbientOcclusionOptions == null) {
            mAmbientOcclusionOptions = new AmbientOcclusionOptions();
        }
        return mAmbientOcclusionOptions;
    }

    /**
     * Sets bloom options.
     * 设置泛光选项。
     *
     * @param options Options for bloom.
     * @param options 泛光选项。
     * @see #getBloomOptions
     */
    public void setBloomOptions(@NonNull BloomOptions options) {
        mBloomOptions = options;
        nSetBloomOptions(getNativeObject(), options.dirt != null ? options.dirt.getNativeObject() : 0,
                options.dirtStrength, options.strength, options.resolution,
                options.levels, options.blendMode.ordinal(),
                options.threshold, options.enabled, options.highlight,
                options.lensFlare, options.starburst, options.chromaticAberration,
                options.ghostCount, options.ghostSpacing, options.ghostThreshold,
                options.haloThickness, options.haloRadius, options.haloThreshold);
    }

    /**
     * Gets the bloom options
     * 获取泛光选项
     * @see #setBloomOptions
     *
     * @return bloom options currently set.
     * @return 当前设置的泛光选项。
     */
    @NonNull
    public BloomOptions getBloomOptions() {
        if (mBloomOptions == null) {
            mBloomOptions = new BloomOptions();
        }
        return mBloomOptions;
    }

    /**
     * Sets vignette options.
     * 设置暗角选项。
     *
     * @param options Options for vignetting.
     * @param options 暗角选项。
     * @see #getVignetteOptions
     */
    public void setVignetteOptions(@NonNull VignetteOptions options) {
        assertFloat4In(options.color);
        mVignetteOptions = options;
        nSetVignetteOptions(getNativeObject(),
                options.midPoint, options.roundness, options.feather,
                options.color[0], options.color[1], options.color[2], options.color[3],
                options.enabled);
    }

    /**
     * Gets the vignette options
     * 获取暗角选项
     * @see #setVignetteOptions
     *
     * @return vignetting options currently set.
     * @return 当前设置的暗角选项。
     */
    @NonNull
    public VignetteOptions getVignetteOptions() {
        if (mVignetteOptions == null) {
            mVignetteOptions = new VignetteOptions();
        }
        return mVignetteOptions;
    }

    /**
     * Sets fog options.
     * 设置雾效选项。
     *
     * @param options Options for fog.
     * @param options 雾效选项。
     * @see #getFogOptions
     */
    public void setFogOptions(@NonNull FogOptions options) {
        assertFloat3In(options.color);
        mFogOptions = options;
        nSetFogOptions(getNativeObject(), options.distance, options.maximumOpacity, options.height,
                options.heightFalloff, options.cutOffDistance,
                options.color[0], options.color[1], options.color[2],
                options.density, options.inScatteringStart, options.inScatteringSize,
                options.fogColorFromIbl,
                options.skyColor == null ? 0 : options.skyColor.getNativeObject(),
                options.enabled);
    }

    /**
     * Gets the fog options
     * 获取雾效选项
     *
     * @return fog options currently set.
     * @return 当前设置的雾效选项。
     * @see #setFogOptions
     */
    @NonNull
    public FogOptions getFogOptions() {
        if (mFogOptions == null) {
            mFogOptions = new FogOptions();
        }
        return mFogOptions;
    }


    /**
     * Sets Depth of Field options.
     * 设置景深选项。
     *
     * @param options Options for depth of field effect.
     * @param options 景深效果选项。
     * @see #getDepthOfFieldOptions
     */
    public void setDepthOfFieldOptions(@NonNull DepthOfFieldOptions options) {
        mDepthOfFieldOptions = options;
        nSetDepthOfFieldOptions(getNativeObject(), options.cocScale,
                options.maxApertureDiameter, options.enabled, options.filter.ordinal(),
                options.nativeResolution, options.foregroundRingCount, options.backgroundRingCount,
                options.fastGatherRingCount, options.maxForegroundCOC, options.maxBackgroundCOC);
    }

    /**
     * Gets the Depth of Field options
     * 获取景深选项
     *
     * @return Depth of Field options currently set.
     * @return 当前设置的景深选项。
     * @see #setDepthOfFieldOptions
     */
    @NonNull
    public DepthOfFieldOptions getDepthOfFieldOptions() {
        if (mDepthOfFieldOptions == null) {
            mDepthOfFieldOptions = new DepthOfFieldOptions();
        }
        return mDepthOfFieldOptions;
    }

    /**
     * Enables use of the stencil buffer.
     * 启用模板缓冲区的使用。
     *
     * <p>
     * The stencil buffer is an 8-bit, per-fragment unsigned integer stored alongside the depth
     * buffer. The stencil buffer is cleared at the beginning of a frame and discarded after the
     * color pass.
     * 模板缓冲区是一个8位的、每个片段的无符号整数，与深度
     * 缓冲区一起存储。模板缓冲区在帧开始时被清除，在
     * 颜色通道后被丢弃。
     * </p>
     *
     * <p>
     * Each fragment's stencil value is set during rasterization by specifying stencil operations on
     * a {@link Material}. The stencil buffer can be used as a mask for later rendering by setting a
     * {@link Material}'s stencil comparison function and reference value. Fragments that don't pass
     * the stencil test are then discarded.
     * 每个片段的模板值在光栅化期间通过在{@link Material}上指定模板操作来设置。
     * 模板缓冲区可以通过设置{@link Material}的模板比较函数和参考值
     * 用作后续渲染的掩码。不通过模板测试的片段
     * 然后被丢弃。
     * </p>
     *
     * <p>
     * If post-processing is disabled, then the SwapChain must have the CONFIG_HAS_STENCIL_BUFFER
     * flag set in order to use the stencil buffer.
     * 如果禁用后处理，则SwapChain必须设置CONFIG_HAS_STENCIL_BUFFER
     * 标志才能使用模板缓冲区。
     * </p>
     *
     * <p>
     * A renderable's priority (see {@link RenderableManager#setPriority(int, int)}) is useful to
     * control the order in which primitives are drawn.
     * 可渲染对象的优先级（参见{@link RenderableManager#setPriority(int, int)}）对于
     * 控制绘制图元的顺序很有用。
     * </p>
     *
     * @param enabled True to enable the stencil buffer, false disables it (default)
     * @param enabled True启用模板缓冲区，false禁用它（默认）
     */
    public void setStencilBufferEnabled(boolean enabled) {
        nSetStencilBufferEnabled(getNativeObject(), enabled);
    }

    /**
     * @return true if the stencil buffer is enabled.
     * @return 如果启用模板缓冲区则返回true。
     * @see View#setStencilBufferEnabled(boolean)
     */
    public boolean isStencilBufferEnabled() {
        return nIsStencilBufferEnabled(getNativeObject());
    }

    /**
     * Sets the stereoscopic rendering options for this view.
     * 设置此视图的立体渲染选项。
     *
     * <p>
     * Currently, only one type of stereoscopic rendering is supported: side-by-side.
     * Side-by-side stereo rendering splits the viewport into two halves: a left and right half.
     * Eye 0 will render to the left half, while Eye 1 will render into the right half.
     * 目前，只支持一种立体渲染类型：并排。
     * 并排立体渲染将视口分为两半：左半部分和右半部分。
     * 眼睛0将渲染到左半部分，而眼睛1将渲染到右半部分。
     * </p>
     *
     * <p>
     * Currently, the following features are not supported with stereoscopic rendering:
     * - post-processing
     * - shadowing
     * - punctual lights
     * 目前，立体渲染不支持以下功能：
     * - 后处理
     * - 阴影
     * - 点光源
     * </p>
     *
     * <p>
     * Stereo rendering depends on device and platform support. To check if stereo rendering is
     * supported, use {@link Engine#isStereoSupported()}. If stereo rendering is not supported, then
     * the stereoscopic options have no effect.
     * 立体渲染取决于设备和平台支持。要检查是否支持立体渲染，
     * 请使用{@link Engine#isStereoSupported()}。如果不支持立体渲染，则
     * 立体选项无效。
     * </p>
     *
     * @param options The stereoscopic options to use on this view
     * @param options 在此视图上使用的立体选项
     * @see #getStereoscopicOptions
     */
    public void setStereoscopicOptions(@NonNull StereoscopicOptions options) {
        mStereoscopicOptions = options;
        nSetStereoscopicOptions(getNativeObject(), options.enabled);
    }

    /**
     * Gets the stereoscopic options.
     * 获取立体选项。
     *
     * @return options Stereoscopic options currently set.
     * @return options 当前设置的立体选项。
     * @see #setStereoscopicOptions
     */
    @NonNull
    public StereoscopicOptions getStereoscopicOptions() {
        if (mStereoscopicOptions == null) {
            mStereoscopicOptions = new StereoscopicOptions();
        }
        return mStereoscopicOptions;
    }


    /**
     * A class containing the result of a picking query
     * 包含拾取查询结果的类
     */
    public static class PickingQueryResult {
        /** The entity of the renderable at the picking query location */
        /** 拾取查询位置处可渲染对象的实体 */
        @Entity public int renderable;
        /** The value of the depth buffer at the picking query location */
        /** 拾取查询位置处深度缓冲区的值 */
        public float depth;
        /** The fragment coordinate in GL convention at the picking query location */
        /** 拾取查询位置处GL约定中的片段坐标 */
        @NonNull public float[] fragCoords = new float[3];
    };

    /**
     * An interface to implement a custom class to receive results of picking queries.
     * 实现自定义类以接收拾取查询结果的接口。
     */
    public interface OnPickCallback {
        /**
         * onPick() is called by the specified Handler in {@link View#pick} when the picking query
         * result is available.
         * 当拾取查询结果可用时，{@link View#pick}中指定的Handler调用onPick()。
         * @param result An instance of {@link PickingQueryResult}.
         * @param result {@link PickingQueryResult}的实例。
         */
        void onPick(@NonNull PickingQueryResult result);
    }

    /**
     * Creates a picking query. Multiple queries can be created (e.g.: multi-touch).
     * Picking queries are all executed when {@link Renderer#render} is called on this View.
     * The provided callback is guaranteed to be called at some point in the future.
     * 创建拾取查询。可以创建多个查询（例如：多点触控）。
     * 当在此视图上调用{@link Renderer#render}时，所有拾取查询都会执行。
     * 提供的回调保证在将来的某个时刻被调用。
     *
     * Typically it takes a couple frames to receive the result of a picking query.
     * 通常需要几帧才能收到拾取查询的结果。
     *
     * @param x        Horizontal coordinate to query in the viewport with origin on the left.
     * @param x        在视口中查询的水平坐标，原点在左侧。
     * @param y        Vertical coordinate to query on the viewport with origin at the bottom.
     * @param y        在视口中查询的垂直坐标，原点在底部。
     * @param handler  An {@link java.util.concurrent.Executor Executor}.
     *                 On Android this can also be a {@link android.os.Handler Handler}.
     * @param handler  一个{@link java.util.concurrent.Executor Executor}。
     *                 在Android上，这也可以是{@link android.os.Handler Handler}。
     * @param callback User callback executed by <code>handler</code> when the picking query
     *                 result is available.
     * @param callback 当拾取查询结果可用时由<code>handler</code>执行的用户回调。
     */
    public void pick(int x, int y,
            @Nullable Object handler, @Nullable OnPickCallback callback) {
        InternalOnPickCallback internalCallback = new InternalOnPickCallback(callback);
        nPick(getNativeObject(), x, y, handler, internalCallback);
    }

    @UsedByNative("View.cpp")
    private static class InternalOnPickCallback implements Runnable {
        private final OnPickCallback mUserCallback;
        private final PickingQueryResult mPickingQueryResult = new PickingQueryResult();

        @UsedByNative("View.cpp")
        @Entity
        int mRenderable;

        @UsedByNative("View.cpp")
        float mDepth;

        @UsedByNative("View.cpp")
        float mFragCoordsX;
        @UsedByNative("View.cpp")
        float mFragCoordsY;
        @UsedByNative("View.cpp")
        float mFragCoordsZ;

        public InternalOnPickCallback(OnPickCallback mUserCallback) {
            this.mUserCallback = mUserCallback;
        }

        @Override
        public void run() {
            mPickingQueryResult.renderable = mRenderable;
            mPickingQueryResult.depth = mDepth;
            mPickingQueryResult.fragCoords[0] = mFragCoordsX;
            mPickingQueryResult.fragCoords[1] = mFragCoordsY;
            mPickingQueryResult.fragCoords[2] = mFragCoordsZ;
            mUserCallback.onPick(mPickingQueryResult);
        }
    }

    /**
     * Set the value of material global variables. There are up-to four such variable each of
     * type float4. These variables can be read in a user Material with
     * `getMaterialGlobal{0|1|2|3}()`. All variable start with a default value of { 0, 0, 0, 1 }
     * 设置材质全局变量的值。最多有四个这样的变量，每个都是
     * float4类型。这些变量可以在用户材质中通过
     * `getMaterialGlobal{0|1|2|3}()`读取。所有变量都以默认值{ 0, 0, 0, 1 }开始
     *
     * @param index index of the variable to set between 0 and 3.
     * @param index 要设置的变量的索引，介于0和3之间。
     * @param value new value for the variable.
     * @param value 变量的新值。
     * @see #getMaterialGlobal
     */
    public void setMaterialGlobal(int index, @NonNull @Size(min = 4) float[] value) {
        Asserts.assertFloat4In(value);
        nSetMaterialGlobal(getNativeObject(), index, value[0], value[1], value[2], value[3]);
    }

    /**
     * Get the value of the material global variables.
     * All variable start with a default value of { 0, 0, 0, 1 }
     * 获取材质全局变量的值。
     * 所有变量都以默认值{ 0, 0, 0, 1 }开始
     *
     * @param index index of the variable to set between 0 and 3.
     * @param index 要设置的变量的索引，介于0和3之间。
     * @param out A 4-float array where the value will be stored, or null in which case the array is
     *            allocated.
     * @param out 存储值的4个浮点数组，或null，在这种情况下分配数组。
     * @return A 4-float array containing the current value of the variable.
     * @return 包含变量当前值的4个浮点数组。
     * @see #setMaterialGlobal
     */
    @NonNull @Size(min = 4)
    public float[] getMaterialGlobal(int index, @Nullable @Size(min = 4) float[] out) {
        out = Asserts.assertFloat4(out);
        nGetMaterialGlobal(getNativeObject(), index, out);
        return out;
    }

    /**
     * Get an Entity representing the large scale fog object.
     * This entity is always inherited by the View's Scene.
     *
     * It is for example possible to create a TransformManager component with this
     * Entity and apply a transformation globally on the fog.
     * 获取表示大规模雾对象的实体。
     * 此实体始终由视图的场景继承。
     *
     * 例如，可以使用此实体创建TransformManager组件，
     * 并在雾上全局应用变换。
     *
     * @return an Entity representing the large scale fog object.
     * @return 表示大规模雾对象的实体。
     */
    @Entity
    public int getFogEntity() {
        return nGetFogEntity(getNativeObject());
    }

    /**
     * When certain temporal features are used (e.g.: TAA or Screen-space reflections), the view
     * keeps a history of previous frame renders associated with the Renderer the view was last
     * used with. When switching Renderer, it may be necessary to clear that history by calling
     * this method. Similarly, if the whole content of the screen change, like when a cut-scene
     * starts, clearing the history might be needed to avoid artifacts due to the previous frame
     * being very different.
     * 当使用某些时间特性（例如：TAA或屏幕空间反射）时，视图会保留
     * 与上次使用视图的渲染器相关联的先前帧渲染历史。
     * 当切换渲染器时，可能需要通过调用此方法来清除该历史。
     * 同样，如果整个屏幕内容发生变化，比如过场动画开始时，
     * 可能需要清除历史以避免由于前一帧差异很大而产生的伪影。
     */
    public void clearFrameHistory(Engine engine) {
        nClearFrameHistory(getNativeObject(), engine.getNativeObject());
    }

    public long getNativeObject() {
        if (mNativeObject == 0) {
            throw new IllegalStateException("Calling method on destroyed View");
        }
        return mNativeObject;
    }

    void clearNativeObject() {
        mNativeObject = 0;
    }

    private static native void nSetName(long nativeView, String name);
    private static native void nSetScene(long nativeView, long nativeScene);
    private static native void nSetCamera(long nativeView, long nativeCamera);
    private static native boolean nHasCamera(long nativeView);
    private static native void nSetViewport(long nativeView, int left, int bottom, int width, int height);
    private static native void nSetVisibleLayers(long nativeView, int select, int value);
    private static native void nSetShadowingEnabled(long nativeView, boolean enabled);
    private static native void nSetRenderTarget(long nativeView, long nativeRenderTarget);
    private static native void nSetSampleCount(long nativeView, int count);
    private static native int nGetSampleCount(long nativeView);
    private static native void nSetAntiAliasing(long nativeView, int type);
    private static native int nGetAntiAliasing(long nativeView);
    private static native void nSetDithering(long nativeView, int dithering);
    private static native int nGetDithering(long nativeView);
    private static native void nSetDynamicResolutionOptions(long nativeView, boolean enabled, boolean homogeneousScaling, float minScale, float maxScale, float sharpness, int quality);
    private static native void nSetRenderQuality(long nativeView, int hdrColorBufferQuality);
    private static native void nSetDynamicLightingOptions(long nativeView, float zLightNear, float zLightFar);
    private static native void nSetShadowType(long nativeView, int type);
    private static native void nSetVsmShadowOptions(long nativeView, int anisotropy, boolean mipmapping, boolean highPrecision, float minVarianceScale, float lightBleedReduction);
    private static native void nSetSoftShadowOptions(long nativeView, float penumbraScale, float penumbraRatioScale);
    private static native void nSetColorGrading(long nativeView, long nativeColorGrading);
    private static native void nSetPostProcessingEnabled(long nativeView, boolean enabled);
    private static native boolean nIsPostProcessingEnabled(long nativeView);
    private static native void nSetFrontFaceWindingInverted(long nativeView, boolean inverted);
    private static native boolean nIsFrontFaceWindingInverted(long nativeView);
    private static native void nSetTransparentPickingEnabled(long nativeView, boolean enabled);
    private static native boolean nIsTransparentPickingEnabled(long nativeView);
    private static native void nSetAmbientOcclusion(long nativeView, int ordinal);
    private static native int nGetAmbientOcclusion(long nativeView);
    private static native void nSetAmbientOcclusionOptions(long nativeView, float radius, float bias, float power, float resolution, float intensity, float bilateralThreshold, int quality, int lowPassFilter, int upsampling, boolean enabled, boolean bentNormals, float minHorizonAngleRad);
    private static native void nSetSSCTOptions(long nativeView, float ssctLightConeRad, float ssctStartTraceDistance, float ssctContactDistanceMax, float ssctIntensity, float v, float v1, float v2, float ssctDepthBias, float ssctDepthSlopeBias, int ssctSampleCount, int ssctRayCount, boolean ssctEnabled);
    private static native void nSetBloomOptions(long nativeView, long dirtNativeObject, float dirtStrength, float strength, int resolution, int levels, int blendMode, boolean threshold, boolean enabled, float highlight,
            boolean lensFlare, boolean starburst, float chromaticAberration, int ghostCount, float ghostSpacing, float ghostThreshold, float haloThickness, float haloRadius, float haloThreshold);
    private static native void nSetFogOptions(long nativeView, float distance, float maximumOpacity, float height, float heightFalloff, float cutOffDistance, float v, float v1, float v2, float density, float inScatteringStart, float inScatteringSize, boolean fogColorFromIbl, long skyColorNativeObject, boolean enabled);
    private static native void nSetStereoscopicOptions(long nativeView, boolean enabled);
    private static native void nSetBlendMode(long nativeView, int blendMode);
    private static native void nSetDepthOfFieldOptions(long nativeView, float cocScale, float maxApertureDiameter, boolean enabled, int filter,
            boolean nativeResolution, int foregroundRingCount, int backgroundRingCount, int fastGatherRingCount, int maxForegroundCOC, int maxBackgroundCOC);
    private static native void nSetVignetteOptions(long nativeView, float midPoint, float roundness, float feather, float r, float g, float b, float a, boolean enabled);
    private static native void nSetTemporalAntiAliasingOptions(long nativeView, float feedback, float filterWidth, boolean enabled);
    private static native void nSetScreenSpaceReflectionsOptions(long nativeView, float thickness, float bias, float maxDistance, float stride, boolean enabled);
    private static native void nSetMultiSampleAntiAliasingOptions(long nativeView, boolean enabled, int sampleCount, boolean customResolve);
    private static native boolean nIsShadowingEnabled(long nativeView);
    private static native void nSetScreenSpaceRefractionEnabled(long nativeView, boolean enabled);
    private static native void nSetGuardBandOptions(long nativeView, boolean enabled);
    private static native boolean nIsScreenSpaceRefractionEnabled(long nativeView);
    private static native void nPick(long nativeView, int x, int y, Object handler, InternalOnPickCallback internalCallback);
    private static native void nSetStencilBufferEnabled(long nativeView, boolean enabled);
    private static native boolean nIsStencilBufferEnabled(long nativeView);
    private static native void nSetMaterialGlobal(long nativeView, int index, float x, float y, float z, float w);
    private static native void nGetMaterialGlobal(long nativeView, int index, float[] out);
    private static native int nGetFogEntity(long nativeView);
    private static native void nClearFrameHistory(long nativeView, long nativeEngine);

    /**
     * List of available ambient occlusion techniques.
     * @deprecated use setAmbientOcclusionOptions instead
     * @see #setAmbientOcclusion
     * 可用环境光遮蔽技术列表。
     * @deprecated 请使用setAmbientOcclusionOptions代替
     * @see #setAmbientOcclusion
     */
    @Deprecated
    public enum AmbientOcclusion {
        NONE,
        SSAO
    }

    // The remainder of this file is generated by beamsplitter

    /**
     * Generic quality level.
     * 通用质量级别。
     */
    public enum QualityLevel {
        LOW,
        MEDIUM,
        HIGH,
        ULTRA,
    }

    public enum BlendMode {
        OPAQUE,
        TRANSLUCENT,
    }

    /**
     * Dynamic resolution can be used to either reach a desired target frame rate
     * by lowering the resolution of a View, or to increase the quality when the
     * rendering is faster than the target frame rate.
     *
     * This structure can be used to specify the minimum scale factor used when
     * lowering the resolution of a View, and the maximum scale factor used when
     * increasing the resolution for higher quality rendering. The scale factors
     * can be controlled on each X and Y axis independently. By default, all scale
     * factors are set to 1.0.
     *
     * enabled:   enable or disables dynamic resolution on a View
     *
     * homogeneousScaling: by default the system scales the major axis first. Set this to true
     *                     to force homogeneous scaling.
     *
     * minScale:  the minimum scale in X and Y this View should use
     *
     * maxScale:  the maximum scale in X and Y this View should use
     *
     * quality:   upscaling quality.
     *            LOW: 1 bilinear tap, Medium: 4 bilinear taps, High: 9 bilinear taps (tent)
     *
     * \note
     * Dynamic resolution is only supported on platforms where the time to render
     * a frame can be measured accurately. Dynamic resolution is currently only
     * supported on Android.
     * 动态分辨率可用于通过降低视图分辨率来达到所需的目标帧率，
     * 或在渲染速度快于目标帧率时提高质量。
     *
     * 此结构可用于指定降低视图分辨率时使用的最小缩放因子，
     * 以及提高分辨率进行高质量渲染时使用的最大缩放因子。
     * 缩放因子可以在X和Y轴上独立控制。默认情况下，所有缩放因子都设置为1.0。
     *
     * enabled:   启用或禁用视图上的动态分辨率
     *
     * homogeneousScaling: 默认情况下，系统首先缩放主轴。将此设置为true
     *                     以强制均匀缩放。
     *
     * minScale:  此视图应使用的X和Y的最小缩放
     *
     * maxScale:  此视图应使用的X和Y的最大缩放
     *
     * quality:   上采样质量。
     *            LOW: 1个双线性采样，Medium: 4个双线性采样，High: 9个双线性采样（帐篷）
     *
     * \note
     * 动态分辨率仅在可以准确测量渲染帧时间的平台上受支持。
     * 动态分辨率目前仅在Android上受支持。
     *
     * @see Renderer::FrameRateOptions
     *
     */
    public static class DynamicResolutionOptions {
        /**
         * minimum scale factors in x and y
         * x和y的最小缩放因子
         */
        public float minScale = 0.5f;
        /**
         * maximum scale factors in x and y
         * x和y的最大缩放因子
         */
        public float maxScale = 1.0f;
        /**
         * sharpness when QualityLevel::MEDIUM or higher is used [0 (disabled), 1 (sharpest)]
         * 当使用QualityLevel::MEDIUM或更高级别时的锐度 [0（禁用），1（最锐利）]
         */
        public float sharpness = 0.9f;
        /**
         * enable or disable dynamic resolution
         * 启用或禁用动态分辨率
         */
        public boolean enabled = false;
        /**
         * set to true to force homogeneous scaling
         * 设置为true以强制均匀缩放
         */
        public boolean homogeneousScaling = false;
        /**
         * Upscaling quality
         * LOW:    bilinear filtered blit. Fastest, poor quality
         * MEDIUM: Qualcomm Snapdragon Game Super Resolution (SGSR) 1.0
         * HIGH:   AMD FidelityFX FSR1 w/ mobile optimizations
         * ULTRA:  AMD FidelityFX FSR1
         *      FSR1 and SGSR require a well anti-aliased (MSAA or TAA), noise free scene. Avoid FXAA and dithering.
         *
         * The default upscaling quality is set to LOW.
         * 上采样质量
         * LOW:    双线性过滤传输。最快，质量差
         * MEDIUM: 高通骁龙游戏超分辨率（SGSR）1.0
         * HIGH:   AMD FidelityFX FSR1 移动优化版
         * ULTRA:  AMD FidelityFX FSR1
         *      FSR1和SGSR需要良好的抗锯齿（MSAA或TAA）、无噪声场景。避免FXAA和抖动。
         *
         * 默认上采样质量设置为LOW。
         */
        @NonNull
        public QualityLevel quality = QualityLevel.LOW;
    }

    /**
     * Options to control the bloom effect
     *
     * enabled:     Enable or disable the bloom post-processing effect. Disabled by default.
     *
     * levels:      Number of successive blurs to achieve the blur effect, the minimum is 3 and the
     *              maximum is 12. This value together with resolution influences the spread of the
     *              blur effect. This value can be silently reduced to accommodate the original
     *              image size.
     *
     * resolution:  Resolution of bloom's minor axis. The minimum value is 2^levels and the
     *              the maximum is lower of the original resolution and 4096. This parameter is
     *              silently clamped to the minimum and maximum.
     *              It is highly recommended that this value be smaller than the target resolution
     *              after dynamic resolution is applied (horizontally and vertically).
     *
     * strength:    how much of the bloom is added to the original image. Between 0 and 1.
     *
     * blendMode:   Whether the bloom effect is purely additive (false) or mixed with the original
     *              image (true).
     *
     * threshold:   When enabled, a threshold at 1.0 is applied on the source image, this is
     *              useful for artistic reasons and is usually needed when a dirt texture is used.
     *
     * dirt:        A dirt/scratch/smudges texture (that can be RGB), which gets added to the
     *              bloom effect. Smudges are visible where bloom occurs. Threshold must be
     *              enabled for the dirt effect to work properly.
     *
     * dirtStrength: Strength of the dirt texture.
     * 控制泛光效果的选项
     *
     * enabled:     启用或禁用泛光后处理效果。默认禁用。
     *
     * levels:      实现模糊效果的连续模糊次数，最小值为3，最大值为12。
     *              此值与分辨率一起影响模糊效果的扩散。
     *              此值可能会被静默减少以适应原始图像大小。
     *
     * resolution:  泛光次轴的分辨率。最小值为2^levels，
     *              最大值为原始分辨率和4096中的较小值。此参数被静默钳制到最小值和最大值。
     *              强烈建议此值小于应用动态分辨率后的目标分辨率（水平和垂直）。
     *
     * strength:    添加到原始图像的泛光量。介于0和1之间。
     *
     * blendMode:   泛光效果是纯加性的（false）还是与原始图像混合的（true）。
     *
     * threshold:   启用时，在源图像上应用1.0的阈值，这对于艺术原因很有用，
     *              通常在使用污垢纹理时需要。
     *
     * dirt:        污垢/划痕/污迹纹理（可以是RGB），添加到泛光效果中。
     *              污迹在泛光发生的地方可见。必须启用阈值才能使污垢效果正常工作。
     *
     * dirtStrength: 污垢纹理的强度。
     */
    public static class BloomOptions {
        public enum BlendMode {
            /**
             * Bloom is modulated by the strength parameter and added to the scene
             */
            ADD,
            /**
             * Bloom is interpolated with the scene using the strength parameter
             */
            INTERPOLATE,
        }

        /**
         * user provided dirt texture
         */
        @Nullable
        public Texture dirt = null;
        /**
         * strength of the dirt texture
         */
        public float dirtStrength = 0.2f;
        /**
         * bloom's strength between 0.0 and 1.0
         */
        public float strength = 0.10f;
        /**
         * resolution of vertical axis (2^levels to 2048)
         */
        public int resolution = 384;
        /**
         * number of blur levels (1 to 11)
         */
        public int levels = 6;
        /**
         * how the bloom effect is applied
         */
        @NonNull
        public BloomOptions.BlendMode blendMode = BloomOptions.BlendMode.ADD;
        /**
         * whether to threshold the source
         */
        public boolean threshold = true;
        /**
         * enable or disable bloom
         */
        public boolean enabled = false;
        /**
         * limit highlights to this value before bloom [10, +inf]
         */
        public float highlight = 1000.0f;
        /**
         * Bloom quality level.
         * LOW (default): use a more optimized down-sampling filter, however there can be artifacts
         *      with dynamic resolution, this can be alleviated by using the homogenous mode.
         * MEDIUM: Good balance between quality and performance.
         * HIGH: In this mode the bloom resolution is automatically increased to avoid artifacts.
         *      This mode can be significantly slower on mobile, especially at high resolution.
         *      This mode greatly improves the anamorphic bloom.
         */
        @NonNull
        public QualityLevel quality = QualityLevel.LOW;
        /**
         * enable screen-space lens flare
         */
        public boolean lensFlare = false;
        /**
         * enable starburst effect on lens flare
         */
        public boolean starburst = true;
        /**
         * amount of chromatic aberration
         */
        public float chromaticAberration = 0.005f;
        /**
         * number of flare "ghosts"
         */
        public int ghostCount = 4;
        /**
         * spacing of the ghost in screen units [0, 1[
         */
        public float ghostSpacing = 0.6f;
        /**
         * hdr threshold for the ghosts
         * 鬼影的HDR阈值
         */
        public float ghostThreshold = 10.0f;
        /**
         * thickness of halo in vertical screen units, 0 to disable
         * 光晕在垂直屏幕单位中的厚度，0表示禁用
         */
        public float haloThickness = 0.1f;
        /**
         * radius of halo in vertical screen units [0, 0.5]
         * 光晕在垂直屏幕单位中的半径 [0, 0.5]
         */
        public float haloRadius = 0.4f;
        /**
         * hdr threshold for the halo
         * 光晕的HDR阈值
         */
        public float haloThreshold = 10.0f;
    }

    /**
     * Options to control large-scale fog in the scene
     * 控制场景中大规模雾的选项
     */
    public static class FogOptions {
        /**
         * Distance in world units [m] from the camera to where the fog starts ( >= 0.0 )
         * 从相机到雾开始位置的世界单位距离[m]（>= 0.0）
         */
        public float distance = 0.0f;
        /**
         * Distance in world units [m] after which the fog calculation is disabled.
         * This can be used to exclude the skybox, which is desirable if it already contains clouds or
         * fog. The default value is +infinity which applies the fog to everything.
         *
         * Note: The SkyBox is typically at a distance of 1e19 in world space (depending on the near
         * plane distance and projection used though).
         * 禁用雾计算后的世界单位距离[m]。
         * 这可用于排除天空盒，如果天空盒已包含云或雾，这是理想的。
         * 默认值为+无穷大，将雾应用于所有内容。
         *
         * 注意：天空盒通常在世界空间中距离为1e19（尽管取决于使用的近平面距离和投影）。
         */
        public float cutOffDistance = Float.POSITIVE_INFINITY;
        /**
         * fog's maximum opacity between 0 and 1
         * 雾的最大不透明度，介于0和1之间
         */
        public float maximumOpacity = 1.0f;
        /**
         * Fog's floor in world units [m]. This sets the "sea level".
         * 雾的底部世界单位[m]。这设置了"海平面"。
         */
        public float height = 0.0f;
        /**
         * How fast the fog dissipates with altitude. heightFalloff has a unit of [1/m].
         * It can be expressed as 1/H, where H is the altitude change in world units [m] that causes a
         * factor 2.78 (e) change in fog density.
         *
         * A falloff of 0 means the fog density is constant everywhere and may result is slightly
         * faster computations.
         */
        public float heightFalloff = 1.0f;
        /**
         *  Fog's color is used for ambient light in-scattering, a good value is
         *  to use the average of the ambient light, possibly tinted towards blue
         *  for outdoors environments. Color component's values should be between 0 and 1, values
         *  above one are allowed but could create a non energy-conservative fog (this is dependant
         *  on the IBL's intensity as well).
         *
         *  We assume that our fog has no absorption and therefore all the light it scatters out
         *  becomes ambient light in-scattering and has lost all directionality, i.e.: scattering is
         *  isotropic. This somewhat simulates Rayleigh scattering.
         *
         *  This value is used as a tint instead, when fogColorFromIbl is enabled.
         *
         *  @see fogColorFromIbl
         */
        @NonNull @Size(min = 3)
        public float[] color = {1.0f, 1.0f, 1.0f};
        /**
         * Extinction factor in [1/m] at altitude 'height'. The extinction factor controls how much
         * light is absorbed and out-scattered per unit of distance. Each unit of extinction reduces
         * the incoming light to 37% of its original value.
         *
         * Note: The extinction factor is related to the fog density, it's usually some constant K times
         * the density at sea level (more specifically at fog height). The constant K depends on
         * the composition of the fog/atmosphere.
         *
         * For historical reason this parameter is called `density`.
         */
        public float density = 0.1f;
        /**
         * Distance in world units [m] from the camera where the Sun in-scattering starts.
         */
        public float inScatteringStart = 0.0f;
        /**
         * Very inaccurately simulates the Sun's in-scattering. That is, the light from the sun that
         * is scattered (by the fog) towards the camera.
         * Size of the Sun in-scattering (>0 to activate). Good values are >> 1 (e.g. ~10 - 100).
         * Smaller values result is a larger scattering size.
         */
        public float inScatteringSize = -1.0f;
        /**
         * The fog color will be sampled from the IBL in the view direction and tinted by `color`.
         * Depending on the scene this can produce very convincing results.
         *
         * This simulates a more anisotropic phase-function.
         *
         * `fogColorFromIbl` is ignored when skyTexture is specified.
         *
         * @see skyColor
         */
        public boolean fogColorFromIbl = false;
        /**
         * skyTexture must be a mipmapped cubemap. When provided, the fog color will be sampled from
         * this texture, higher resolution mip levels will be used for objects at the far clip plane,
         * and lower resolution mip levels for objects closer to the camera. The skyTexture should
         * typically be heavily blurred; a typical way to produce this texture is to blur the base
         * level with a strong gaussian filter or even an irradiance filter and then generate mip
         * levels as usual. How blurred the base level is somewhat of an artistic decision.
         *
         * This simulates a more anisotropic phase-function.
         *
         * `fogColorFromIbl` is ignored when skyTexture is specified.
         *
         * @see Texture
         * @see fogColorFromIbl
         */
        @Nullable
        public Texture skyColor = null;
        /**
         * Enable or disable large-scale fog
         */
        public boolean enabled = false;
    }

    /**
     * Options to control Depth of Field (DoF) effect in the scene.
     *
     * cocScale can be used to set the depth of field blur independently from the camera
     * aperture, e.g. for artistic reasons. This can be achieved by setting:
     *      cocScale = cameraAperture / desiredDoFAperture
     *
     * @see Camera
     */
    public static class DepthOfFieldOptions {
        public enum Filter {
            NONE,
            UNUSED,
            MEDIAN,
        }

        /**
         * circle of confusion scale factor (amount of blur)
         */
        public float cocScale = 1.0f;
        /**
         * width/height aspect ratio of the circle of confusion (simulate anamorphic lenses)
         */
        public float cocAspectRatio = 1.0f;
        /**
         * maximum aperture diameter in meters (zero to disable rotation)
         */
        public float maxApertureDiameter = 0.01f;
        /**
         * enable or disable depth of field effect
         */
        public boolean enabled = false;
        /**
         * filter to use for filling gaps in the kernel
         */
        @NonNull
        public DepthOfFieldOptions.Filter filter = DepthOfFieldOptions.Filter.MEDIAN;
        /**
         * perform DoF processing at native resolution
         */
        public boolean nativeResolution = false;
        /**
         * Number of of rings used by the gather kernels. The number of rings affects quality
         * and performance. The actual number of sample per pixel is defined
         * as (ringCount * 2 - 1)^2. Here are a few commonly used values:
         *       3 rings :   25 ( 5x 5 grid)
         *       4 rings :   49 ( 7x 7 grid)
         *       5 rings :   81 ( 9x 9 grid)
         *      17 rings : 1089 (33x33 grid)
         *
         * With a maximum circle-of-confusion of 32, it is never necessary to use more than 17 rings.
         *
         * Usually all three settings below are set to the same value, however, it is often
         * acceptable to use a lower ring count for the "fast tiles", which improves performance.
         * Fast tiles are regions of the screen where every pixels have a similar
         * circle-of-confusion radius.
         *
         * A value of 0 means default, which is 5 on desktop and 3 on mobile.
         *
         * @{
         */
        public int foregroundRingCount = 0;
        /**
         * number of kernel rings for background tiles
         */
        public int backgroundRingCount = 0;
        /**
         * number of kernel rings for fast tiles
         */
        public int fastGatherRingCount = 0;
        /**
         * maximum circle-of-confusion in pixels for the foreground, must be in [0, 32] range.
         * A value of 0 means default, which is 32 on desktop and 24 on mobile.
         */
        public int maxForegroundCOC = 0;
        /**
         * maximum circle-of-confusion in pixels for the background, must be in [0, 32] range.
         * A value of 0 means default, which is 32 on desktop and 24 on mobile.
         */
        public int maxBackgroundCOC = 0;
    }

    /**
     * Options to control the vignetting effect.
     */
    public static class VignetteOptions {
        /**
         * high values restrict the vignette closer to the corners, between 0 and 1
         */
        public float midPoint = 0.5f;
        /**
         * controls the shape of the vignette, from a rounded rectangle (0.0), to an oval (0.5), to a circle (1.0)
         */
        public float roundness = 0.5f;
        /**
         * softening amount of the vignette effect, between 0 and 1
         */
        public float feather = 0.5f;
        /**
         * color of the vignette effect, alpha is currently ignored
         */
        @NonNull @Size(min = 4)
        public float[] color = {0.0f, 0.0f, 0.0f, 1.0f};
        /**
         * enables or disables the vignette effect
         */
        public boolean enabled = false;
    }

    /**
     * Structure used to set the precision of the color buffer and related quality settings.
     *
     * @see setRenderQuality, getRenderQuality
     */
    public static class RenderQuality {
        /**
         * Sets the quality of the HDR color buffer.
         *
         * A quality of HIGH or ULTRA means using an RGB16F or RGBA16F color buffer. This means
         * colors in the LDR range (0..1) have a 10 bit precision. A quality of LOW or MEDIUM means
         * using an R11G11B10F opaque color buffer or an RGBA16F transparent color buffer. With
         * R11G11B10F colors in the LDR range have a precision of either 6 bits (red and green
         * channels) or 5 bits (blue channel).
         */
        @NonNull
        public QualityLevel hdrColorBuffer = QualityLevel.HIGH;
    }

    /**
     * Options for screen space Ambient Occlusion (SSAO) and Screen Space Cone Tracing (SSCT)
     * @see setAmbientOcclusionOptions()
     */
    public static class AmbientOcclusionOptions {
        public enum AmbientOcclusionType {
            /**
             * use Scalable Ambient Occlusion
             */
            SAO,
            /**
             * use Ground Truth-Based Ambient Occlusion
             */
            GTAO,
        }

        /**
         * Type of ambient occlusion algorithm.
         */
        @NonNull
        public AmbientOcclusionOptions.AmbientOcclusionType aoType = AmbientOcclusionOptions.AmbientOcclusionType.SAO;
        /**
         * Ambient Occlusion radius in meters, between 0 and ~10.
         */
        public float radius = 0.3f;
        /**
         * Controls ambient occlusion's contrast. Must be positive.
         */
        public float power = 1.0f;
        /**
         * Self-occlusion bias in meters. Use to avoid self-occlusion.
         * Between 0 and a few mm. No effect when aoType set to GTAO
         */
        public float bias = 0.0005f;
        /**
         * How each dimension of the AO buffer is scaled. Must be either 0.5 or 1.0.
         */
        public float resolution = 0.5f;
        /**
         * Strength of the Ambient Occlusion effect.
         */
        public float intensity = 1.0f;
        /**
         * depth distance that constitute an edge for filtering
         */
        public float bilateralThreshold = 0.05f;
        /**
         * affects # of samples used for AO and params for filtering
         */
        @NonNull
        public QualityLevel quality = QualityLevel.LOW;
        /**
         * affects AO smoothness. Recommend setting to HIGH when aoType set to GTAO.
         */
        @NonNull
        public QualityLevel lowPassFilter = QualityLevel.MEDIUM;
        /**
         * affects AO buffer upsampling quality
         */
        @NonNull
        public QualityLevel upsampling = QualityLevel.LOW;
        /**
         * enables or disables screen-space ambient occlusion
         */
        public boolean enabled = false;
        /**
         * enables bent normals computation from AO, and specular AO
         */
        public boolean bentNormals = false;
        /**
         * min angle in radian to consider. No effect when aoType set to GTAO.
         */
        public float minHorizonAngleRad = 0.0f;
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        public float ssctLightConeRad = 1.0f;
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        public float ssctShadowDistance = 0.3f;
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        public float ssctContactDistanceMax = 1.0f;
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        public float ssctIntensity = 0.8f;
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        @NonNull @Size(min = 3)
        public float[] ssctLightDirection = {0f, -1f, 0f};
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        public float ssctDepthBias = 0.01f;
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        public float ssctDepthSlopeBias = 0.01f;
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        public int ssctSampleCount = 4;
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        public int ssctRayCount = 1;
        /**
         * Screen Space Cone Tracing (SSCT) options
         * Ambient shadows from dominant light
         */
        public boolean ssctEnabled = false;

        /**
         * Ground Truth-base Ambient Occlusion (GTAO) options
         */
        public int gtaoSampleSliceCount = 4;
        /**
         * Ground Truth-base Ambient Occlusion (GTAO) options
         */
        public int gtaoSampleStepsPerSlice = 3;
        /**
         * Ground Truth-base Ambient Occlusion (GTAO) options
         */
        public float gtaoThicknessHeuristic = 0.004f;

    }

    /**
     * Options for Multi-Sample Anti-aliasing (MSAA)
     * @see setMultiSampleAntiAliasingOptions()
     */
    public static class MultiSampleAntiAliasingOptions {
        /**
         * enables or disables msaa
         */
        public boolean enabled = false;
        /**
         * sampleCount number of samples to use for multi-sampled anti-aliasing.\n
         *              0: treated as 1
         *              1: no anti-aliasing
         *              n: sample count. Effective sample could be different depending on the
         *                 GPU capabilities.
         */
        public int sampleCount = 4;
        /**
         * custom resolve improves quality for HDR scenes, but may impact performance.
         */
        public boolean customResolve = false;
    }

    /**
     * Options for Temporal Anti-aliasing (TAA)
     * Most TAA parameters are extremely costly to change, as they will trigger the TAA post-process
     * shaders to be recompiled. These options should be changed or set during initialization.
     * `filterWidth`, `feedback` and `jitterPattern`, however, can be changed at any time.
     *
     * `feedback` of 0.1 effectively accumulates a maximum of 19 samples in steady state.
     * see "A Survey of Temporal Antialiasing Techniques" by Lei Yang and all for more information.
     *
     * @see setTemporalAntiAliasingOptions()
     */
    public static class TemporalAntiAliasingOptions {
        public enum BoxType {
            /**
             * use an AABB neighborhood
             */
            AABB,
            /**
             * use the variance of the neighborhood (not recommended)
             */
            VARIANCE,
            /**
             * use both AABB and variance
             */
            AABB_VARIANCE,
        }

        public enum BoxClipping {
            /**
             * Accurate box clipping
             */
            ACCURATE,
            /**
             * clamping
             */
            CLAMP,
            /**
             * no rejections (use for debugging)
             */
            NONE,
        }

        public enum JitterPattern {
            RGSS_X4,
            UNIFORM_HELIX_X4,
            HALTON_23_X8,
            HALTON_23_X16,
            HALTON_23_X32,
        }

        /**
         * reconstruction filter width typically between 1 (sharper) and 2 (smoother)
         */
        public float filterWidth = 1.0f;
        /**
         * history feedback, between 0 (maximum temporal AA) and 1 (no temporal AA).
         */
        public float feedback = 0.12f;
        /**
         * texturing lod bias (typically -1 or -2)
         */
        public float lodBias = -1.0f;
        /**
         * post-TAA sharpen, especially useful when upscaling is true.
         */
        public float sharpness = 0.0f;
        /**
         * enables or disables temporal anti-aliasing
         */
        public boolean enabled = false;
        /**
         * 4x TAA upscaling. Disables Dynamic Resolution. [BETA]
         */
        public boolean upscaling = false;
        /**
         * whether to filter the history buffer
         */
        public boolean filterHistory = true;
        /**
         * whether to apply the reconstruction filter to the input
         */
        public boolean filterInput = true;
        /**
         * whether to use the YcoCg color-space for history rejection
         */
        public boolean useYCoCg = false;
        /**
         * type of color gamut box
         */
        @NonNull
        public TemporalAntiAliasingOptions.BoxType boxType = TemporalAntiAliasingOptions.BoxType.AABB;
        /**
         * clipping algorithm
         */
        @NonNull
        public TemporalAntiAliasingOptions.BoxClipping boxClipping = TemporalAntiAliasingOptions.BoxClipping.ACCURATE;
        @NonNull
        public TemporalAntiAliasingOptions.JitterPattern jitterPattern = TemporalAntiAliasingOptions.JitterPattern.HALTON_23_X16;
        public float varianceGamma = 1.0f;
        /**
         * adjust the feedback dynamically to reduce flickering
         */
        public boolean preventFlickering = false;
        /**
         * whether to apply history reprojection (debug option)
         */
        public boolean historyReprojection = true;
    }

    /**
     * Options for Screen-space Reflections.
     * @see setScreenSpaceReflectionsOptions()
     */
    public static class ScreenSpaceReflectionsOptions {
        /**
         * ray thickness, in world units
         */
        public float thickness = 0.1f;
        /**
         * bias, in world units, to prevent self-intersections
         */
        public float bias = 0.01f;
        /**
         * maximum distance, in world units, to raycast
         */
        public float maxDistance = 3.0f;
        /**
         * stride, in texels, for samples along the ray.
         */
        public float stride = 2.0f;
        public boolean enabled = false;
    }

    /**
     * Options for the  screen-space guard band.
     * A guard band can be enabled to avoid some artifacts towards the edge of the screen when
     * using screen-space effects such as SSAO. Enabling the guard band reduces performance slightly.
     * Currently the guard band can only be enabled or disabled.
     */
    public static class GuardBandOptions {
        public boolean enabled = false;
    }

    /**
     * List of available post-processing anti-aliasing techniques.
     * @see setAntiAliasing, getAntiAliasing, setSampleCount
     */
    public enum AntiAliasing {
        /**
         * no anti aliasing performed as part of post-processing
         */
        NONE,
        /**
         * FXAA is a low-quality but very efficient type of anti-aliasing. (default).
         */
        FXAA,
    }

    /**
     * List of available post-processing dithering techniques.
     */
    public enum Dithering {
        /**
         * No dithering
         */
        NONE,
        /**
         * Temporal dithering (default)
         */
        TEMPORAL,
    }

    /**
     * List of available shadow mapping techniques.
     * @see setShadowType
     */
    public enum ShadowType {
        /**
         * percentage-closer filtered shadows (default)
         */
        PCF,
        /**
         * variance shadows
         */
        VSM,
        /**
         * PCF with contact hardening simulation
         */
        DPCF,
        /**
         * PCF with soft shadows and contact hardening
         */
        PCSS,
        PCFd,
    }

    /**
     * View-level options for VSM Shadowing.
     * @see setVsmShadowOptions()
     * @warning This API is still experimental and subject to change.
     */
    public static class VsmShadowOptions {
        /**
         * Sets the number of anisotropic samples to use when sampling a VSM shadow map. If greater
         * than 0, mipmaps will automatically be generated each frame for all lights.
         *
         * The number of anisotropic samples = 2 ^ vsmAnisotropy.
         */
        public int anisotropy = 0;
        /**
         * Whether to generate mipmaps for all VSM shadow maps.
         */
        public boolean mipmapping = false;
        /**
         * The number of MSAA samples to use when rendering VSM shadow maps.
         * Must be a power-of-two and greater than or equal to 1. A value of 1 effectively turns
         * off MSAA.
         * Higher values may not be available depending on the underlying hardware.
         */
        public int msaaSamples = 1;
        /**
         * Whether to use a 32-bits or 16-bits texture format for VSM shadow maps. 32-bits
         * precision is rarely needed, but it does reduces light leaks as well as "fading"
         * of the shadows in some situations. Setting highPrecision to true for a single
         * shadow map will double the memory usage of all shadow maps.
         */
        public boolean highPrecision = false;
        /**
         * VSM minimum variance scale, must be positive.
         */
        public float minVarianceScale = 0.5f;
        /**
         * VSM light bleeding reduction amount, between 0 and 1.
         */
        public float lightBleedReduction = 0.15f;
    }

    /**
     * View-level options for DPCF and PCSS Shadowing.
     * @see setSoftShadowOptions()
     * @warning This API is still experimental and subject to change.
     */
    public static class SoftShadowOptions {
        /**
         * Globally scales the penumbra of all DPCF and PCSS shadows
         * Acceptable values are greater than 0
         */
        public float penumbraScale = 1.0f;
        /**
         * Globally scales the computed penumbra ratio of all DPCF and PCSS shadows.
         * This effectively controls the strength of contact hardening effect and is useful for
         * artistic purposes. Higher values make the shadows become softer faster.
         * Acceptable values are equal to or greater than 1.
         */
        public float penumbraRatioScale = 1.0f;
    }

    /**
     * Options for stereoscopic (multi-eye) rendering.
     */
    public static class StereoscopicOptions {
        public boolean enabled = false;
    }
}
