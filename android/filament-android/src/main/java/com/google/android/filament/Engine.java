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

import com.google.android.filament.proguard.UsedByReflection;

/**
 * Engine is filament's main entry-point.
 * Engine 是 filament 的主要入口点。
 * <p>
 * An Engine instance main function is to keep track of all resources created by the user and
 * manage the rendering thread as well as the hardware renderer.
 * </p>
 * <p>
 * Engine 实例的主要功能是跟踪用户创建的所有资源，并管理渲染线程以及硬件渲染器。
 * </p>
 * <p>
 * To use filament, an Engine instance must be created first:
 * </p>
 * <p>
 * 要使用 filament，必须首先创建一个 Engine 实例：
 * </p>
 *
 * <pre>
 * import com.google.android.filament.*
 *
 * Engine engine = Engine.create();
 * </pre>
 * <p>
 * Engine essentially represents (or is associated to) a hardware context
 * (e.g. an OpenGL ES context).
 * </p>
 * <p>
 * Engine 本质上表示（或关联到）一个硬件上下文（例如 OpenGL ES 上下文）。
 * </p>
 * <p>
 * Rendering typically happens in an operating system's window (which can be full screen), such
 * window is managed by a {@link Renderer}.
 * </p>
 * <p>
 * 渲染通常发生在操作系统的窗口中（可以是全屏），这样的窗口由 {@link Renderer} 管理。
 * </p>
 * <p>
 * A typical filament render loop looks like this:
 * </p>
 * <p>
 * 典型的 filament 渲染循环如下所示：
 * </p>
 *
 *
 * <pre>
 * import com.google.android.filament.*
 *
 * Engine engine        = Engine.create();
 * SwapChain swapChain  = engine.createSwapChain(nativeWindow);
 * Renderer renderer    = engine.createRenderer();
 * Scene scene          = engine.createScene();
 * View view            = engine.createView();
 *
 * view.setScene(scene);
 *
 * do {
 *     // typically we wait for VSYNC and user input events
 *     // 通常我们等待 VSYNC 和用户输入事件
 *     if (renderer.beginFrame(swapChain)) {
 *         renderer.render(view);
 *         renderer.endFrame();
 *     }
 * } while (!quit);
 *
 * engine.destroyView(view);
 * engine.destroyScene(scene);
 * engine.destroyRenderer(renderer);
 * engine.destroySwapChain(swapChain);
 * engine.destroy();
 * </pre>
 *
 * <h1><u>Resource Tracking</u></h1>
 * <h1><u>资源跟踪</u></h1>
 * <p>
 * Each <code>Engine</code> instance keeps track of all objects created by the user, such as vertex
 * and index buffers, lights, cameras, etc...
 * The user is expected to free those resources, however, leaked resources are freed when the
 * engine instance is destroyed and a warning is emitted in the console.
 * </p>
 * <p>
 * 每个 <code>Engine</code> 实例都会跟踪用户创建的所有对象，如顶点和索引缓冲区、灯光、相机等。
 * 用户应该释放这些资源，但是，当引擎实例被销毁时，泄漏的资源会被释放，并在控制台中发出警告。
 * </p>
 *
 * <h1><u>Thread safety</u></h1>
 * <h1><u>线程安全</u></h1>
 * <p>
 * An <code>Engine</code> instance is not thread-safe. The implementation makes no attempt to
 * synchronize calls to an <code>Engine</code> instance methods.
 * If multi-threading is needed, synchronization must be external.
 * </p>
 * <p>
 * <code>Engine</code> 实例不是线程安全的。实现不会尝试同步对 <code>Engine</code> 实例方法的调用。
 * 如果需要多线程，必须进行外部同步。
 * </p>
 *
 * <h1><u>Multi-threading</u></h1>
 * <h1><u>多线程</u></h1>
 * <p>
 * When created, the <code>Engine</code> instance starts a render thread as well as multiple worker
 * threads, these threads have an elevated priority appropriate for rendering, based on the
 * platform's best practices. The number of worker threads depends on the platform and is
 * automatically chosen for best performance.
 * </p>
 * <p>
 * 创建时，<code>Engine</code> 实例会启动一个渲染线程以及多个工作线程，这些线程具有适合渲染的提升优先级，
 * 基于平台的最佳实践。工作线程的数量取决于平台，并自动选择以获得最佳性能。
 * </p>
 * <p>
 * On platforms with asymmetric cores (e.g. ARM's Big.Little), <code>Engine</code> makes some
 * educated guesses as to which cores to use for the render thread and worker threads. For example,
 * it'll try to keep an OpenGL ES thread on a Big core.
 * </p>
 * <p>
 * 在具有非对称核心的平台上（例如 ARM 的 Big.Little），<code>Engine</code> 会对渲染线程和工作线程
 * 使用哪些核心做出一些有根据的猜测。例如，它会尝试将 OpenGL ES 线程保持在大核心上。
 * </p>
 *
 * <h1><u>Swap Chains</u></h1>
 * <h1><u>交换链</u></h1>
 * <p>
 * A swap chain represents an Operating System's <b>native</b> renderable surface.
 * Typically it's a window or a view. Because a {@link SwapChain} is initialized from a native
 * object, it is given to filament as an <code>Object</code>, which must be of the proper type for
 * each platform filament is running on.
 * </p>
 * <p>
 * 交换链表示操作系统的<b>原生</b>可渲染表面。通常它是一个窗口或视图。
 * 因为 {@link SwapChain} 是从原生对象初始化的，它作为 <code>Object</code> 传递给 filament，
 * 必须是 filament 运行平台的正确类型。
 * </p>
 * <p>
 *
 * @see SwapChain
 * @see Renderer
 */
public class Engine {
    private static final Backend[] sBackendValues = Backend.values();
    private static final FeatureLevel[] sFeatureLevelValues = FeatureLevel.values();

    private long mNativeObject;

    private Config mConfig;

    @NonNull private final TransformManager mTransformManager;
    @NonNull private final LightManager mLightManager;
    @NonNull private final RenderableManager mRenderableManager;
    @NonNull private final EntityManager mEntityManager;

    /**
     * Denotes a backend
     * 表示后端
     */
    public enum Backend {
        /**
         * Automatically selects an appropriate driver for the platform.
         * 自动为平台选择合适的驱动程序。
         */
        DEFAULT,
        /**
         * Selects the OpenGL driver (which supports OpenGL ES as well).
         * 选择 OpenGL 驱动程序（也支持 OpenGL ES）。
         */
        OPENGL,
        /**
         * Selects the Vulkan driver if the platform supports it.
         * 如果平台支持，选择 Vulkan 驱动程序。
         */
        VULKAN,
        /**
         * Selects the Metal driver if the platform supports it.
         * 如果平台支持，选择 Metal 驱动程序。
         */
        METAL,
        /**
         * Select the WebGPU driver if platform supports it.
         * 如果平台支持，选择 WebGPU 驱动程序。
         */
        WEBGPU,
        /**
         * Selects the no-op driver for testing purposes.
         * 选择无操作驱动程序用于测试目的。
         */
        NOOP,
    }

    /**
     * Defines the backend's feature levels.
     * 定义后端的功能级别。
     */
    public enum FeatureLevel {
        /** Reserved, don't use */
        /** 保留，请勿使用 */
        FEATURE_LEVEL_0,
        /** OpenGL ES 3.0 features (default) */
        /** OpenGL ES 3.0 功能（默认） */
        FEATURE_LEVEL_1,
        /** OpenGL ES 3.1 features + 16 textures units + cubemap arrays */
        /** OpenGL ES 3.1 功能 + 16 个纹理单元 + 立方体贴图数组 */
        FEATURE_LEVEL_2,
        /** OpenGL ES 3.1 features + 31 textures units + cubemap arrays */
        /** OpenGL ES 3.1 功能 + 31 个纹理单元 + 立方体贴图数组 */
        FEATURE_LEVEL_3,
    };

    /**
     * The type of technique for stereoscopic rendering. (Note that the materials used will need to be
     * compatible with the chosen technique.)
     * 立体渲染技术的类型。（注意使用的材质需要与所选技术兼容。）
     */
    public enum StereoscopicType {
        /** No stereoscopic rendering. */
        /** 无立体渲染。 */
        NONE,
        /** Stereoscopic rendering is performed using instanced rendering technique. */
        /** 使用实例化渲染技术执行立体渲染。 */
        INSTANCED,
        /** Stereoscopic rendering is performed using the multiview feature from the graphics backend. */
        /** 使用图形后端的多视图功能执行立体渲染。 */
        MULTIVIEW,
    };

    /**
     * Constructs <code>Engine</code> objects using a builder pattern.
     * 使用构建器模式构造 <code>Engine</code> 对象。
     */
    public static class Builder {
        @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
        private final BuilderFinalizer mFinalizer;
        private final long mNativeBuilder;
        private Config mConfig;

        public Builder() {
            mNativeBuilder = nCreateBuilder();
            mFinalizer = new BuilderFinalizer(mNativeBuilder);
        }

        /**
         * Sets the {@link Backend} for the Engine.
         * 为引擎设置 {@link Backend}。
         *
         * @param backend Driver backend to use
         *                要使用的驱动程序后端
         * @return A reference to this Builder for chaining calls.
         *         返回此 Builder 的引用以便链式调用。
         */
        public Builder backend(Backend backend) {
            nSetBuilderBackend(mNativeBuilder, backend.ordinal());
            return this;
        }

        /**
         * Sets a sharedContext for the Engine.
         * 为引擎设置共享上下文。
         *
         * @param sharedContext  A platform-dependant OpenGL context used as a shared context
         *                       when creating filament's internal context. On Android this parameter
         *                       <b>must be</b> an instance of {@link android.opengl.EGLContext}.
         *                       创建 filament 内部上下文时用作共享上下文的平台相关 OpenGL 上下文。
         *                       在 Android 上，此参数<b>必须</b>是 {@link android.opengl.EGLContext} 的实例。
         * @return A reference to this Builder for chaining calls.
         *         返回此 Builder 的引用以便链式调用。
         */
        public Builder sharedContext(Object sharedContext) {
            if (Platform.get().validateSharedContext(sharedContext)) {
                nSetBuilderSharedContext(mNativeBuilder,
                        Platform.get().getSharedContextNativeHandle(sharedContext));
                return this;
            }
            throw new IllegalArgumentException("Invalid shared context " + sharedContext);
        }

        /**
         * Configure the Engine with custom parameters.
         * 使用自定义参数配置引擎。
         *
         * @param config A {@link Config} object
         *               一个 {@link Config} 对象
         * @return A reference to this Builder for chaining calls.
         *         返回此 Builder 的引用以便链式调用。
         */
        public Builder config(Config config) {
            mConfig = config;
            nSetBuilderConfig(mNativeBuilder, config.commandBufferSizeMB,
                    config.perRenderPassArenaSizeMB, config.driverHandleArenaSizeMB,
                    config.minCommandBufferSizeMB, config.perFrameCommandsSizeMB,
                    config.jobSystemThreadCount, config.disableParallelShaderCompile,
                    config.stereoscopicType.ordinal(), config.stereoscopicEyeCount,
                    config.resourceAllocatorCacheSizeMB, config.resourceAllocatorCacheMaxAge,
                    config.disableHandleUseAfterFreeCheck,
                    config.preferredShaderLanguage.ordinal(),
                    config.forceGLES2Context, config.assertNativeWindowIsValid);
            return this;
        }

        /**
         * Sets the initial featureLevel for the Engine.
         * 为引擎设置初始功能级别。
         *
         * @param featureLevel The feature level at which initialize Filament.
         *                     初始化 Filament 的功能级别。
         * @return A reference to this Builder for chaining calls.
         *         返回此 Builder 的引用以便链式调用。
         */
        public Builder featureLevel(FeatureLevel featureLevel) {
            nSetBuilderFeatureLevel(mNativeBuilder, featureLevel.ordinal());
            return this;
        }

        /**
         * Sets the initial paused state of the rendering thread.
         * 设置渲染线程的初始暂停状态。
         *
         * <p>Warning: This is an experimental API. See {@link Engine#setPaused(boolean)} for
         * caveats.
         * <p>警告：这是一个实验性 API。有关注意事项，请参阅 {@link Engine#setPaused(boolean)}。
         *
         * @param paused Whether to start the rendering thread paused.
         *               是否以暂停状态启动渲染线程。
         * @return A reference to this Builder for chaining calls.
         *         返回此 Builder 的引用以便链式调用。
         */
        public Builder paused(boolean paused) {
            nSetBuilderPaused(mNativeBuilder, paused);
            return this;
        }

        /**
         * Set a feature flag value. This is the only way to set constant feature flags.
         * 设置功能标志值。这是设置常量功能标志的唯一方法。
         * @param name feature name
         *             功能名称
         * @param value true to enable, false to disable
         *              true 表示启用，false 表示禁用
         * @return A reference to this Builder for chaining calls.
         *         返回此 Builder 的引用以便链式调用。
         */
        public Builder feature(@NonNull String name, boolean value) {
            nSetBuilderFeature(mNativeBuilder, name, value);
            return this;
        }

        /**
         * Creates an instance of Engine
         * 创建引擎实例
         *
         * @return A newly created <code>Engine</code>, or <code>null</code> if the GPU driver couldn't
         *         be initialized, for instance if it doesn't support the right version of OpenGL or
         *         OpenGL ES.
         *         新创建的 <code>Engine</code>，如果 GPU 驱动程序无法初始化（例如不支持正确版本的 OpenGL 或
         *         OpenGL ES），则返回 <code>null</code>。
         *
         * @exception IllegalStateException can be thrown if there isn't enough memory to
         * allocate the command buffer.
         *                                   如果没有足够的内存来分配命令缓冲区，可能会抛出此异常。
         */
        public Engine build() {
            long nativeEngine = nBuilderBuild(mNativeBuilder);
            if (nativeEngine == 0) throw new IllegalStateException("Couldn't create Engine");
            return new Engine(nativeEngine, mConfig);
        }

        private static class BuilderFinalizer {
            private final long mNativeObject;

            BuilderFinalizer(long nativeObject) {
                mNativeObject = nativeObject;
            }

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
     * Parameters for customizing the initialization of {@link Engine}.
     * 用于自定义 {@link Engine} 初始化的参数。
     */
    public static class Config {

        // #defines in Engine.h
        private static final long FILAMENT_PER_RENDER_PASS_ARENA_SIZE_IN_MB = 3;
        private static final long FILAMENT_PER_FRAME_COMMANDS_SIZE_IN_MB = 2;
        private static final long FILAMENT_MIN_COMMAND_BUFFERS_SIZE_IN_MB = 1;
        private static final long FILAMENT_COMMAND_BUFFER_SIZE_IN_MB =
                FILAMENT_MIN_COMMAND_BUFFERS_SIZE_IN_MB * 3;

        /**
         * Size in MiB of the low-level command buffer arena.
         * 低级命令缓冲区竞技场的大小（以 MiB 为单位）。
         *
         * Each new command buffer is allocated from here. If this buffer is too small the program
         * might terminate or rendering errors might occur.
         * 每个新的命令缓冲区都从这里分配。如果此缓冲区太小，程序可能会终止或出现渲染错误。
         *
         * This is typically set to minCommandBufferSizeMB * 3, so that up to 3 frames can be
         * batched-up at once.
         * 通常设置为 minCommandBufferSizeMB * 3，以便一次最多可以批处理 3 帧。
         *
         * This value affects the application's memory usage.
         * 此值影响应用程序的内存使用量。
         */
        public long commandBufferSizeMB = FILAMENT_COMMAND_BUFFER_SIZE_IN_MB;

        /**
         * Size in MiB of the per-frame data arena.
         * 每帧数据竞技场的大小（以 MiB 为单位）。
         *
         * This is the main arena used for allocations when preparing a frame.
         * e.g.: Froxel data and high-level commands are allocated from this arena.
         * 这是准备帧时用于分配的主要竞技场。
         * 例如：Froxel 数据和高级命令从此竞技场分配。
         *
         * If this size is too small, the program will abort on debug builds and have undefined
         * behavior otherwise.
         * 如果此大小太小，程序将在调试构建中中止，否则会有未定义的行为。
         *
         * This value affects the application's memory usage.
         * 此值影响应用程序的内存使用量。
         */
        public long perRenderPassArenaSizeMB = FILAMENT_PER_RENDER_PASS_ARENA_SIZE_IN_MB;

        /**
         * Size in MiB of the backend's handle arena.
         * 后端句柄竞技场的大小（以 MiB 为单位）。
         *
         * Backends will fallback to slower heap-based allocations when running out of space and
         * log this condition.
         * 当空间不足时，后端将回退到较慢的基于堆的分配并记录此情况。
         *
         * If 0, then the default value for the given platform is used
         * 如果为 0，则使用给定平台的默认值
         *
         * This value affects the application's memory usage.
         * 此值影响应用程序的内存使用量。
         */
        public long driverHandleArenaSizeMB = 0;

        /**
         * Minimum size in MiB of a low-level command buffer.
         * 低级命令缓冲区的最小大小（以 MiB 为单位）。
         *
         * This is how much space is guaranteed to be available for low-level commands when a new
         * buffer is allocated. If this is too small, the engine might have to stall to wait for
         * more space to become available, this situation is logged.
         * 这是分配新缓冲区时保证可用于低级命令的空间大小。如果太小，引擎可能必须停顿等待
         * 更多空间变为可用，这种情况会被记录。
         *
         * This value does not affect the application's memory usage.
         * 此值不影响应用程序的内存使用量。
         */
        public long minCommandBufferSizeMB = FILAMENT_MIN_COMMAND_BUFFERS_SIZE_IN_MB;

        /**
         * Size in MiB of the per-frame high level command buffer.
         * 每帧高级命令缓冲区的大小（以 MiB 为单位）。
         *
         * This buffer is related to the number of draw calls achievable within a frame, if it is
         * too small, the program will abort on debug builds and have undefined behavior otherwise.
         * 此缓冲区与帧内可实现的绘制调用数量相关，如果太小，程序将在调试构建中中止，
         * 否则会有未定义的行为。
         *
         * It is allocated from the 'per-render-pass arena' above. Make sure that at least 1 MiB is
         * left in the per-render-pass arena when deciding the size of this buffer.
         * 它从上面的"每渲染通道竞技场"分配。在决定此缓冲区大小时，确保每渲染通道竞技场中
         * 至少剩余 1 MiB。
         *
         * This value does not affect the application's memory usage.
         * 此值不影响应用程序的内存使用量。
         */
        public long perFrameCommandsSizeMB = FILAMENT_PER_FRAME_COMMANDS_SIZE_IN_MB;

        /**
         * Number of threads to use in Engine's JobSystem.
         * 引擎作业系统中使用的线程数。
         *
         * Engine uses a utils::JobSystem to carry out paralleization of Engine workloads. This
         * value sets the number of threads allocated for JobSystem. Configuring this value can be
         * helpful in CPU-constrained environments where too many threads can cause contention of
         * CPU and reduce performance.
         * 引擎使用 utils::JobSystem 来执行引擎工作负载的并行化。此值设置为作业系统分配的
         * 线程数。在 CPU 受限的环境中配置此值很有帮助，因为过多的线程可能导致 CPU 争用
         * 并降低性能。
         *
         * The default value is 0, which implies that the Engine will use a heuristic to determine
         * the number of threads to use.
         * 默认值为 0，这意味着引擎将使用启发式方法来确定要使用的线程数。
         */
        public long jobSystemThreadCount = 0;

        /**
         * Number of most-recently destroyed textures to track for use-after-free.
         * 跟踪释放后使用的最近销毁纹理的数量。
         *
         * This will cause the backend to throw an exception when a texture is freed but still bound
         * to a SamplerGroup and used in a draw call. 0 disables completely.
         * 当纹理被释放但仍绑定到 SamplerGroup 并在绘制调用中使用时，这将导致后端抛出异常。
         * 0 表示完全禁用。
         *
         * Currently only respected by the Metal backend.
         * 目前仅 Metal 后端支持。
         */
        public long textureUseAfterFreePoolSize = 0;

        /**
         * Set to `true` to forcibly disable parallel shader compilation in the backend.
         * Currently only honored by the GL backend.
         * 设置为 `true` 以强制禁用后端中的并行着色器编译。
         * 目前仅 GL 后端支持。
         * @Deprecated use "backend.disable_parallel_shader_compile" feature flag instead
         * @已弃用 请使用 "backend.disable_parallel_shader_compile" 功能标志代替
         */
        public boolean disableParallelShaderCompile = false;

        /**
         * The type of technique for stereoscopic rendering.
         * 立体渲染技术的类型。
         *
         * This setting determines the algorithm used when stereoscopic rendering is enabled. This
         * decision applies to the entire Engine for the lifetime of the Engine. E.g., multiple
         * Views created from the Engine must use the same stereoscopic type.
         * 此设置确定启用立体渲染时使用的算法。此决定适用于引擎生命周期内的整个引擎。
         * 例如，从引擎创建的多个视图必须使用相同的立体类型。
         *
         * Each view can enable stereoscopic rendering via the StereoscopicOptions::enable flag.
         * 每个视图都可以通过 StereoscopicOptions::enable 标志启用立体渲染。
         *
         * @see View#setStereoscopicOptions
         */
        public StereoscopicType stereoscopicType = StereoscopicType.NONE;

        /**
         * The number of eyes to render when stereoscopic rendering is enabled. Supported values are
         * between 1 and Engine#getMaxStereoscopicEyes() (inclusive).
         *
         * @see View#setStereoscopicOptions
         * @see Engine#getMaxStereoscopicEyes
         */
        public long stereoscopicEyeCount = 2;

        /**
         * @Deprecated This value is no longer used.
         */
        public long resourceAllocatorCacheSizeMB = 64;

        /**
         * This value determines how many frames texture entries are kept for in the cache. This
         * is a soft limit, meaning some texture older than this are allowed to stay in the cache.
         * Typically only one texture is evicted per frame.
         * The default is 1.
         */
        public long resourceAllocatorCacheMaxAge = 1;

        /**
         * Disable backend handles use-after-free checks.
         * @Deprecated use "backend.disable_handle_use_after_free_check" feature flag instead
         */
        public boolean disableHandleUseAfterFreeCheck = false;

        /**
         * Sets a preferred shader language for Filament to use.
         *
         * The Metal backend supports two shader languages: MSL (Metal Shading Language) and
         * METAL_LIBRARY (precompiled .metallib). This option controls which shader language is
         * used when materials contain both.
         *
         * By default, when preferredShaderLanguage is unset, Filament will prefer METAL_LIBRARY
         * shaders if present within a material, falling back to MSL. Setting
         * preferredShaderLanguage to ShaderLanguage::MSL will instead instruct Filament to check
         * for the presence of MSL in a material first, falling back to METAL_LIBRARY if MSL is not
         * present.
         *
         * When using a non-Metal backend, setting this has no effect.
         */
        public enum ShaderLanguage {
            DEFAULT,
            MSL,
            METAL_LIBRARY,
        };
        public ShaderLanguage preferredShaderLanguage = ShaderLanguage.DEFAULT;

        /**
         * When the OpenGL ES backend is used, setting this value to true will force a GLES2.0
         * context if supported by the Platform, or if not, will have the backend pretend
         * it's a GLES2 context. Ignored on other backends.
         * 当使用 OpenGL ES 后端时，将此值设置为 true 将强制使用 GLES2.0 上下文（如果平台支持），
         * 或者如果不支持，将让后端假装它是 GLES2 上下文。在其他后端上被忽略。
         */
        public boolean forceGLES2Context = false;

        /**
         * Assert the native window associated to a SwapChain is valid when calling makeCurrent().
         * This is only supported for:
         *      - PlatformEGLAndroid
         * 断言在调用 makeCurrent() 时与 SwapChain 关联的原生窗口是有效的。
         * 仅支持以下平台：
         *      - PlatformEGLAndroid
         * @Deprecated use "backend.opengl.assert_native_window_is_valid" feature flag instead
         * @已弃用 请使用 "backend.opengl.assert_native_window_is_valid" 功能标志代替
         */
        public boolean assertNativeWindowIsValid = false;
    }

    private Engine(long nativeEngine, Config config) {
        mNativeObject = nativeEngine;
        mTransformManager = new TransformManager(nGetTransformManager(nativeEngine));
        mLightManager = new LightManager(nGetLightManager(nativeEngine));
        mRenderableManager = new RenderableManager(nGetRenderableManager(nativeEngine));
        mEntityManager = new EntityManager(nGetEntityManager(nativeEngine));
        mConfig = config;
    }

    /**
     * Creates an instance of Engine using the default {@link Backend}
     * 使用默认 {@link Backend} 创建引擎实例
     * <p>
     * This method is one of the few thread-safe methods.
     * <p>
     * 此方法是少数几个线程安全的方法之一。
     *
     * @return A newly created <code>Engine</code>, or <code>null</code> if the GPU driver couldn't
     *         be initialized, for instance if it doesn't support the right version of OpenGL or
     *         OpenGL ES.
     *         新创建的 <code>Engine</code>，如果 GPU 驱动程序无法初始化（例如不支持正确版本的 OpenGL 或
     *         OpenGL ES），则返回 <code>null</code>。
     *
     * @exception IllegalStateException can be thrown if there isn't enough memory to
     * allocate the command buffer.
     *                                   如果没有足够的内存来分配命令缓冲区，可能会抛出此异常。
     *
     */
    @NonNull
    public static Engine create() {
        return new Builder().build();
    }

    /**
     * Creates an instance of Engine using the specified {@link Backend}
     * 使用指定的 {@link Backend} 创建引擎实例
     * <p>
     * This method is one of the few thread-safe methods.
     * <p>
     * 此方法是少数几个线程安全的方法之一。
     *
     * @param backend           driver backend to use
     *                          要使用的驱动程序后端
     *
     * @return A newly created <code>Engine</code>, or <code>null</code> if the GPU driver couldn't
     *         be initialized, for instance if it doesn't support the right version of OpenGL or
     *         OpenGL ES.
     *         新创建的 <code>Engine</code>，如果 GPU 驱动程序无法初始化（例如不支持正确版本的 OpenGL 或
     *         OpenGL ES），则返回 <code>null</code>。
     *
     * @exception IllegalStateException can be thrown if there isn't enough memory to
     * allocate the command buffer.
     *                                   如果没有足够的内存来分配命令缓冲区，可能会抛出此异常。
     *
     */
    @NonNull
    public static Engine create(@NonNull Backend backend) {
        return new Builder()
            .backend(backend)
            .build();
    }

    /**
     * Creates an instance of Engine using the {@link Backend#OPENGL} and a shared OpenGL context.
     * 使用 {@link Backend#OPENGL} 和共享 OpenGL 上下文创建引擎实例。
     * <p>
     * This method is one of the few thread-safe methods.
     * <p>
     * 此方法是少数几个线程安全的方法之一。
     *
     * @param sharedContext  A platform-dependant OpenGL context used as a shared context
     *                       when creating filament's internal context. On Android this parameter
     *                       <b>must be</b> an instance of {@link android.opengl.EGLContext}.
     *                       创建 filament 内部上下文时用作共享上下文的平台相关 OpenGL 上下文。
     *                       在 Android 上，此参数<b>必须</b>是 {@link android.opengl.EGLContext} 的实例。
     *
     * @return A newly created <code>Engine</code>, or <code>null</code> if the GPU driver couldn't
     *         be initialized, for instance if it doesn't support the right version of OpenGL or
     *         OpenGL ES.
     *         新创建的 <code>Engine</code>，如果 GPU 驱动程序无法初始化（例如不支持正确版本的 OpenGL 或
     *         OpenGL ES），则返回 <code>null</code>。
     *
     * @exception IllegalStateException can be thrown if there isn't enough memory to
     * allocate the command buffer.
     *                                   如果没有足够的内存来分配命令缓冲区，可能会抛出此异常。
     *
     */
    @NonNull
    public static Engine create(@NonNull Object sharedContext) {
        return new Builder()
            .sharedContext(sharedContext)
            .build();
    }

    /**
     * @return <code>true</code> if this <code>Engine</code> is initialized properly.
     *         如果此 <code>Engine</code> 正确初始化，则返回 <code>true</code>。
     */
    public boolean isValid() {
        return mNativeObject != 0;
    }

    /**
     * Destroy the <code>Engine</code> instance and all associated resources.
     * 销毁 <code>Engine</code> 实例和所有关联的资源。
     * <p>
     * This method is one of the few thread-safe methods.
     * <p>
     * 此方法是少数几个线程安全的方法之一。
     * <p>
     * {@link Engine#destroy()} should be called last and after all other resources have been
     * destroyed, it ensures all filament resources are freed.
     * <p>
     * {@link Engine#destroy()} 应该在所有其他资源都被销毁后最后调用，它确保所有 filament 资源都被释放。
     * <p>
     * <code>Destroy</code> performs the following tasks:
     * <code>Destroy</code> 执行以下任务：
     * <li>Destroy all internal software and hardware resources.</li>
     * <li>销毁所有内部软件和硬件资源。</li>
     * <li>Free all user allocated resources that are not already destroyed and logs a warning.
     *     <p>This indicates a "leak" in the user's code.</li>
     * <li>释放所有尚未销毁的用户分配资源并记录警告。
     *     <p>这表明用户代码中存在"泄漏"。</li>
     * <li>Terminate the rendering engine's thread.</li>
     * <li>终止渲染引擎的线程。</li>
     *
     * <pre>
     * Engine engine = Engine.create();
     * engine.destroy();
     * </pre>
     */
    public void destroy() {
        nDestroyEngine(getNativeObject());
        clearNativeObject();
    }

    /**
     * @return the backend used by this <code>Engine</code>
     *         此 <code>Engine</code> 使用的后端
     */
    @NonNull
    public Backend getBackend() {
        return sBackendValues[(int) nGetBackend(getNativeObject())];
    }

    /**
     * Helper to enable accurate translations.
     * 启用精确变换的辅助方法。
     * If you need this Engine to handle a very large world space, one way to achieve this
     * automatically is to enable accurate translations in the TransformManager. This helper
     * provides a convenient way of doing that.
     * 如果您需要此引擎处理非常大的世界空间，自动实现这一点的一种方法是在 TransformManager 中
     * 启用精确变换。此辅助方法提供了一种便捷的方式来实现这一点。
     * This is typically called once just after creating the Engine.
     * 通常在创建引擎后立即调用一次。
     */
    public void enableAccurateTranslations() {
        getTransformManager().setAccurateTranslationsEnabled(true);
    }

    /**
     * Query the feature level supported by the selected backend.
     * 查询所选后端支持的功能级别。
     *
     * A specific feature level needs to be set before the corresponding features can be used.
     * 在使用相应功能之前，需要设置特定的功能级别。
     *
     * @return FeatureLevel supported the selected backend.
     *         所选后端支持的功能级别。
     * @see #setActiveFeatureLevel
     */
    @NonNull
    public FeatureLevel getSupportedFeatureLevel() {
        return sFeatureLevelValues[(int) nGetSupportedFeatureLevel(getNativeObject())];
    }

    /**
     * Activate all features of a given feature level. If an explicit feature level is not specified
     * at Engine initialization time via {@link Builder#featureLevel}, the default feature level is
     * {@link FeatureLevel#FEATURE_LEVEL_0} on devices not compatible with GLES 3.0; otherwise, the
     * default is {@link FeatureLevel::FEATURE_LEVEL_1}. The selected feature level must not be
     * higher than the value returned by {@link #getActiveFeatureLevel} and it's not possible lower
     * the active feature level. Additionally, it is not possible to modify the feature level at all
     * if the Engine was initialized at {@link FeatureLevel#FEATURE_LEVEL_0}.
     * 激活给定功能级别的所有功能。如果在引擎初始化时未通过 {@link Builder#featureLevel} 指定显式功能级别，
     * 则在不兼容 GLES 3.0 的设备上默认功能级别为 {@link FeatureLevel#FEATURE_LEVEL_0}；否则，
     * 默认为 {@link FeatureLevel::FEATURE_LEVEL_1}。所选功能级别不得高于 {@link #getActiveFeatureLevel} 
     * 返回的值，并且无法降低活动功能级别。此外，如果引擎在 {@link FeatureLevel#FEATURE_LEVEL_0} 初始化，
     * 则根本无法修改功能级别。
     *
     * @param featureLevel the feature level to activate. If featureLevel is lower than {@link
     *                     #getActiveFeatureLevel}, the current (higher) feature level is kept. If
     *                     featureLevel is higher than {@link #getSupportedFeatureLevel}, or if the
     *                     engine was initialized at feature level 0, an exception is thrown, or the
     *                     program is terminated if exceptions are disabled.
     *                     要激活的功能级别。如果 featureLevel 低于 {@link #getActiveFeatureLevel}，
     *                     则保持当前（更高的）功能级别。如果 featureLevel 高于 {@link #getSupportedFeatureLevel}，
     *                     或者引擎在功能级别 0 初始化，则抛出异常，或者如果禁用异常则终止程序。
     *
     * @return the active feature level.
     *         活动功能级别。
     *
     * @see Builder#featureLevel
     * @see #getSupportedFeatureLevel
     * @see #getActiveFeatureLevel
     */
    @NonNull
    public FeatureLevel setActiveFeatureLevel(@NonNull FeatureLevel featureLevel) {
        return sFeatureLevelValues[(int) nSetActiveFeatureLevel(getNativeObject(), featureLevel.ordinal())];
    }

    /**
     * Returns the currently active feature level.
     * 返回当前活动的功能级别。
     * @return currently active feature level
     *         当前活动的功能级别
     * @see #getSupportedFeatureLevel
     * @see #setActiveFeatureLevel
     */
    @NonNull
    public FeatureLevel getActiveFeatureLevel() {
        return sFeatureLevelValues[(int) nGetActiveFeatureLevel(getNativeObject())];
    }

    /**
     * Enables or disables automatic instancing of render primitives. Instancing of render primitive
     * can greatly reduce CPU overhead but requires the instanced primitives to be identical
     * (i.e. use the same geometry) and use the same MaterialInstance. If it is known that the
     * scene doesn't contain any identical primitives, automatic instancing can have some
     * overhead and it is then best to disable it.
     * 启用或禁用渲染图元的自动实例化。渲染图元的实例化可以大大减少 CPU 开销，但要求实例化的图元
     * 完全相同（即使用相同的几何体）并使用相同的 MaterialInstance。如果已知场景不包含任何相同的图元，
     * 自动实例化可能会有一些开销，此时最好禁用它。
     *
     * Disabled by default.
     * 默认禁用。
     *
     * @param enable true to enable, false to disable automatic instancing.
     *               true 表示启用，false 表示禁用自动实例化。
     *
     * @see RenderableManager
     * @see MaterialInstance
     */
    public void setAutomaticInstancingEnabled(boolean enable) {
        nSetAutomaticInstancingEnabled(getNativeObject(), enable);
    }

    /**
     * @return true if automatic instancing is enabled, false otherwise.
     *         如果启用了自动实例化则返回 true，否则返回 false。
     * @see #setAutomaticInstancingEnabled
     */
    public boolean isAutomaticInstancingEnabled() {
        return nIsAutomaticInstancingEnabled(getNativeObject());
    }

    /**
     * Retrieves the configuration settings of this {@link Engine}.
     * 检索此 {@link Engine} 的配置设置。
     *
     * This method returns the configuration object that was supplied to the Engine's {@link
     * Builder#config} method during the creation of this Engine. If the {@link Builder::config}
     * method was not explicitly called (or called with null), this method returns the default
     * configuration settings.
     * 此方法返回在创建此引擎期间提供给引擎的 {@link Builder#config} 方法的配置对象。
     * 如果未显式调用 {@link Builder::config} 方法（或使用 null 调用），此方法返回默认配置设置。
     *
     * @return a {@link Config} object with this Engine's configuration
     *         包含此引擎配置的 {@link Config} 对象
     * @see Builder#config
     */
    @NonNull
    public Config getConfig() {
        if (mConfig == null) {
            mConfig = new Config();
        }
        return mConfig;
    }

    /**
     * Returns the maximum number of stereoscopic eyes supported by Filament. The actual number of
     * eyes rendered is set at Engine creation time with the {@link Config#stereoscopicEyeCount}
     * setting.
     * 返回 Filament 支持的立体眼睛的最大数量。实际渲染的眼睛数量在引擎创建时通过
     * {@link Config#stereoscopicEyeCount} 设置确定。
     *
     * @return the max number of stereoscopic eyes supported
     *         支持的立体眼睛的最大数量
     * @see Config#stereoscopicEyeCount
     */
    public long getMaxStereoscopicEyes() {
        return nGetMaxStereoscopicEyes(getNativeObject());
    }


    // SwapChain

    /**
     * Creates an opaque {@link SwapChain} from the given OS native window handle.
     * 从给定的操作系统原生窗口句柄创建一个不透明的 {@link SwapChain}。
     *
     * @param surface on Android, <b>must be</b> an instance of {@link android.view.Surface}
     *                在 Android 上，<b>必须是</b> {@link android.view.Surface} 的实例
     *
     * @return a newly created {@link SwapChain} object
     *         新创建的 {@link SwapChain} 对象
     *
     * @exception IllegalStateException can be thrown if the SwapChain couldn't be created
     *                                   如果无法创建 SwapChain，可能抛出此异常
     */
    @NonNull
    public SwapChain createSwapChain(@NonNull Object surface) {
        return createSwapChain(surface, SwapChainFlags.CONFIG_DEFAULT);
    }

    /**
     * Creates a {@link SwapChain} from the given OS native window handle.
     * 从给定的操作系统原生窗口句柄创建一个 {@link SwapChain}。
     *
     * @param surface on Android, <b>must be</b> an instance of {@link android.view.Surface}
     *                在 Android 上，<b>必须是</b> {@link android.view.Surface} 的实例
     *
     * @param flags configuration flags, see {@link SwapChainFlags}
     *              配置标志，参见 {@link SwapChainFlags}
     *
     * @return a newly created {@link SwapChain} object
     *         新创建的 {@link SwapChain} 对象
     *
     * @exception IllegalStateException can be thrown if the SwapChain couldn't be created
     *                                   如果无法创建 SwapChain，可能抛出此异常
     *
     * @see SwapChainFlags#CONFIG_DEFAULT
     * @see SwapChainFlags#CONFIG_TRANSPARENT
     * @see SwapChainFlags#CONFIG_READABLE
     *
     */
    @NonNull
    public SwapChain createSwapChain(@NonNull Object surface, long flags) {
        if (Platform.get().validateSurface(surface)) {
            long nativeSwapChain = nCreateSwapChain(getNativeObject(), surface, flags);
            if (nativeSwapChain == 0) throw new IllegalStateException("Couldn't create SwapChain");
            return new SwapChain(nativeSwapChain, surface);
        }
        throw new IllegalArgumentException("Invalid surface " + surface);
    }

    /**
     * Creates a headless {@link SwapChain}
     * 创建一个无头的 {@link SwapChain}
     *
     * @param width  width of the rendering buffer
     *               渲染缓冲区的宽度
     * @param height height of the rendering buffer
     *               渲染缓冲区的高度
     * @param flags  configuration flags, see {@link SwapChainFlags}
     *               配置标志，参见 {@link SwapChainFlags}
     *
     * @return a newly created {@link SwapChain} object
     *         新创建的 {@link SwapChain} 对象
     *
     * @exception IllegalStateException can be thrown if the SwapChain couldn't be created
     *                                   如果无法创建 SwapChain，可能抛出此异常
     *
     * @see SwapChainFlags#CONFIG_DEFAULT
     * @see SwapChainFlags#CONFIG_TRANSPARENT
     * @see SwapChainFlags#CONFIG_READABLE
     *
     */
    @NonNull
    public SwapChain createSwapChain(int width, int height, long flags) {
        if (width >= 0 && height >= 0) {
            long nativeSwapChain =
                nCreateSwapChainHeadless(getNativeObject(), width, height, flags);
            if (nativeSwapChain == 0) throw new IllegalStateException("Couldn't create SwapChain");
            return new SwapChain(nativeSwapChain, null);
        }
        throw new IllegalArgumentException("Invalid parameters");
    }

    /**
     * Creates a {@link SwapChain} from a {@link NativeSurface}.
     * 从 {@link NativeSurface} 创建一个 {@link SwapChain}。
     *
     * @param surface a properly initialized {@link NativeSurface}
     *                正确初始化的 {@link NativeSurface}
     *
     * @param flags configuration flags, see {@link SwapChainFlags}
     *              配置标志，参见 {@link SwapChainFlags}
     *
     * @return a newly created {@link SwapChain} object
     *         新创建的 {@link SwapChain} 对象
     *
     * @exception IllegalStateException can be thrown if the {@link SwapChainFlags} couldn't be
     *            created
     *                                   如果无法创建 {@link SwapChainFlags}，可能抛出此异常
     */
    @NonNull
    public SwapChain createSwapChainFromNativeSurface(@NonNull NativeSurface surface, long flags) {
        long nativeSwapChain =
                nCreateSwapChainFromRawPointer(getNativeObject(), surface.getNativeObject(), flags);
        if (nativeSwapChain == 0) throw new IllegalStateException("Couldn't create SwapChain");
        return new SwapChain(nativeSwapChain, surface);
    }

    /**
     * Destroys a {@link SwapChain} and frees all its associated resources.
     * 销毁 {@link SwapChain} 并释放其所有关联资源。
     * @param swapChain the {@link SwapChain} to destroy
     *                  要销毁的 {@link SwapChain}
     */
    public void destroySwapChain(@NonNull SwapChain swapChain) {
        assertDestroy(nDestroySwapChain(getNativeObject(), swapChain.getNativeObject()));
        swapChain.clearNativeObject();
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidRenderer(@NonNull Renderer object) {
        return nIsValidRenderer(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidView(@NonNull View object) {
        return nIsValidView(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidScene(@NonNull Scene object) {
        return nIsValidScene(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidFence(@NonNull Fence object) {
        return nIsValidFence(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidStream(@NonNull Stream object) {
        return nIsValidStream(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidIndexBuffer(@NonNull IndexBuffer object) {
        return nIsValidIndexBuffer(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidVertexBuffer(@NonNull VertexBuffer object) {
        return nIsValidVertexBuffer(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidSkinningBuffer(@NonNull SkinningBuffer object) {
        return nIsValidSkinningBuffer(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidIndirectLight(@NonNull IndirectLight object) {
        return nIsValidIndirectLight(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidMaterial(@NonNull Material object) {
        return nIsValidMaterial(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param ma Material
     *           材质
     * @param mi MaterialInstance to check for validity
     *           要检查有效性的材质实例
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidMaterialInstance(@NonNull Material ma, MaterialInstance mi) {
        return nIsValidMaterialInstance(getNativeObject(), ma.getNativeObject(), mi.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidExpensiveMaterialInstance(@NonNull MaterialInstance object) {
        return nIsValidExpensiveMaterialInstance(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidSkybox(@NonNull Skybox object) {
        return nIsValidSkybox(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidColorGrading(@NonNull ColorGrading object) {
        return nIsValidColorGrading(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidTexture(@NonNull Texture object) {
        return nIsValidTexture(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidRenderTarget(@NonNull RenderTarget object) {
        return nIsValidRenderTarget(getNativeObject(), object.getNativeObject());
    }

    /**
     * Returns whether the object is valid.
     * 返回对象是否有效。
     * @param object Object to check for validity
     *               要检查有效性的对象
     * @return returns true if the specified object is valid.
     *         如果指定对象有效则返回 true。
     */
    public boolean isValidSwapChain(@NonNull SwapChain object) {
        return nIsValidSwapChain(getNativeObject(), object.getNativeObject());
    }

    // View

    /**
     * Creates a {@link View}.
     * 创建一个 {@link View}。
     * @return a newly created {@link View}
     *         新创建的 {@link View}
     * @exception IllegalStateException can be thrown if the {@link View} couldn't be created
     *                                   如果无法创建 {@link View}，可能抛出此异常
     */
    @NonNull
    public View createView() {
        long nativeView = nCreateView(getNativeObject());
        if (nativeView == 0) throw new IllegalStateException("Couldn't create View");
        return new View(nativeView);
    }

    /**
     * Destroys a {@link View} and frees all its associated resources.
     * 销毁 {@link View} 并释放其所有关联资源。
     * @param view the {@link View} to destroy
     *             要销毁的 {@link View}
     */
    public void destroyView(@NonNull View view) {
        assertDestroy(nDestroyView(getNativeObject(), view.getNativeObject()));
        view.clearNativeObject();
    }

    // Renderer

    /**
     * Creates a {@link Renderer}.
     * 创建一个 {@link Renderer}。
     * @return a newly created {@link Renderer}
     *         新创建的 {@link Renderer}
     * @exception IllegalStateException can be thrown if the {@link Renderer} couldn't be created
     *                                   如果无法创建 {@link Renderer}，可能抛出此异常
     */
    @NonNull
    public Renderer createRenderer() {
        long nativeRenderer = nCreateRenderer(getNativeObject());
        if (nativeRenderer == 0) throw new IllegalStateException("Couldn't create Renderer");
        return new Renderer(this, nativeRenderer);
    }

    /**
     * Destroys a {@link Renderer} and frees all its associated resources.
     * 销毁 {@link Renderer} 并释放其所有关联资源。
     * @param renderer the {@link Renderer} to destroy
     *                 要销毁的 {@link Renderer}
     */
    public void destroyRenderer(@NonNull Renderer renderer) {
        assertDestroy(nDestroyRenderer(getNativeObject(), renderer.getNativeObject()));
        renderer.clearNativeObject();
    }

    // Camera

    /**
     * Creates and adds a {@link Camera} component to a given <code>entity</code>.
     * 创建并向给定的 <code>entity</code> 添加 {@link Camera} 组件。
     *
     * @param entity <code>entity</code> to add the camera component to
     *               要添加相机组件的 <code>entity</code>
     * @return A newly created {@link Camera}
     *         新创建的 {@link Camera}
     * @exception IllegalStateException can be thrown if the {@link Camera} couldn't be created
     *                                   如果无法创建 {@link Camera}，可能抛出此异常
     */
    @NonNull
    public Camera createCamera(@Entity int entity) {
        long nativeCamera = nCreateCamera(getNativeObject(), entity);
        if (nativeCamera == 0) throw new IllegalStateException("Couldn't create Camera");
        return new Camera(nativeCamera, entity);
    }

    /**
     * Returns the Camera component of the given <code>entity</code>.
     * 返回给定 <code>entity</code> 的 Camera 组件。
     *
     * @param entity An <code>entity</code>.
     *               一个 <code>entity</code>。
     * @return the Camera component for this entity or null if the entity doesn't have a Camera
     *         component
     *         此实体的 Camera 组件，如果实体没有 Camera 组件则返回 null
     */
    @Nullable
    public Camera getCameraComponent(@Entity int entity) {
        long nativeCamera = nGetCameraComponent(getNativeObject(), entity);
        if (nativeCamera == 0) return null;
        return new Camera(nativeCamera, entity);
    }

    /**
     * Destroys the {@link Camera} component associated with the given entity.
     * 销毁与给定实体关联的 {@link Camera} 组件。
     *
     * @param entity an entity
     *               一个实体
     */
    public void destroyCameraComponent(@Entity int entity) {
        nDestroyCameraComponent(getNativeObject(), entity);
    }

    // Scene

    /**
     * Creates a {@link Scene}.
     * 创建一个 {@link Scene}。
     * @return a newly created {@link Scene}
     *         新创建的 {@link Scene}
     * @exception IllegalStateException can be thrown if the {@link Scene} couldn't be created
     *                                   如果无法创建 {@link Scene}，可能抛出此异常
     */
    @NonNull
    public Scene createScene() {
        long nativeScene = nCreateScene(getNativeObject());
        if (nativeScene == 0) throw new IllegalStateException("Couldn't create Scene");
        return new Scene(nativeScene);
    }

    /**
     * Destroys a {@link Scene} and frees all its associated resources.
     * 销毁 {@link Scene} 并释放其所有关联资源。
     * @param scene the {@link Scene} to destroy
     *              要销毁的 {@link Scene}
     */
    public void destroyScene(@NonNull Scene scene) {
        assertDestroy(nDestroyScene(getNativeObject(), scene.getNativeObject()));
        scene.clearNativeObject();
    }

    // Stream

    /**
     * Destroys a {@link Stream} and frees all its associated resources.
     * 销毁 {@link Stream} 并释放其所有关联资源。
     * @param stream the {@link Stream} to destroy
     *               要销毁的 {@link Stream}
     */
    public void destroyStream(@NonNull Stream stream) {
        assertDestroy(nDestroyStream(getNativeObject(), stream.getNativeObject()));
        stream.clearNativeObject();
    }

    // Fence

    /**
     * Creates a {@link Fence}.
     * 创建一个 {@link Fence}。
     * @return a newly created {@link Fence}
     *         新创建的 {@link Fence}
     * @exception IllegalStateException can be thrown if the {@link Fence} couldn't be created
     *                                   如果无法创建 {@link Fence}，可能抛出此异常
     */
    @NonNull
    public Fence createFence() {
        long nativeFence = nCreateFence(getNativeObject());
        if (nativeFence == 0) throw new IllegalStateException("Couldn't create Fence");
        return new Fence(nativeFence);
    }

    /**
     * Destroys a {@link Fence} and frees all its associated resources.
     * 销毁 {@link Fence} 并释放其所有关联资源。
     * @param fence the {@link Fence} to destroy
     *              要销毁的 {@link Fence}
     */
    public void destroyFence(@NonNull Fence fence) {
        assertDestroy(nDestroyFence(getNativeObject(), fence.getNativeObject()));
        fence.clearNativeObject();
    }

    // others...

    /**
     * Destroys a {@link IndexBuffer} and frees all its associated resources.
     * 销毁 {@link IndexBuffer} 并释放其所有关联资源。
     * @param indexBuffer the {@link IndexBuffer} to destroy
     *                    要销毁的 {@link IndexBuffer}
     */
    public void destroyIndexBuffer(@NonNull IndexBuffer indexBuffer) {
        assertDestroy(nDestroyIndexBuffer(getNativeObject(), indexBuffer.getNativeObject()));
        indexBuffer.clearNativeObject();
    }

    /**
     * Destroys a {@link VertexBuffer} and frees all its associated resources.
     * 销毁 {@link VertexBuffer} 并释放其所有关联资源。
     * @param vertexBuffer the {@link VertexBuffer} to destroy
     *                     要销毁的 {@link VertexBuffer}
     */
    public void destroyVertexBuffer(@NonNull VertexBuffer vertexBuffer) {
        assertDestroy(nDestroyVertexBuffer(getNativeObject(), vertexBuffer.getNativeObject()));
        vertexBuffer.clearNativeObject();
    }

    /**
     * Destroys a {@link SkinningBuffer} and frees all its associated resources.
     * 销毁 {@link SkinningBuffer} 并释放其所有关联资源。
     * @param skinningBuffer the {@link SkinningBuffer} to destroy
     *                       要销毁的 {@link SkinningBuffer}
     */
    public void destroySkinningBuffer(@NonNull SkinningBuffer skinningBuffer) {
        assertDestroy(nDestroySkinningBuffer(getNativeObject(), skinningBuffer.getNativeObject()));
        skinningBuffer.clearNativeObject();
    }

    /**
     * Destroys a {@link IndirectLight} and frees all its associated resources.
     * 销毁 {@link IndirectLight} 并释放其所有关联资源。
     * @param ibl the {@link IndirectLight} to destroy
     *            要销毁的 {@link IndirectLight}
     */
    public void destroyIndirectLight(@NonNull IndirectLight ibl) {
        assertDestroy(nDestroyIndirectLight(getNativeObject(), ibl.getNativeObject()));
        ibl.clearNativeObject();
    }

    /**
     * Destroys a {@link Material} and frees all its associated resources.
     * 销毁 {@link Material} 并释放其所有关联资源。
     * <p>
     * All {@link MaterialInstance} of the specified {@link Material} must be destroyed before
     * destroying it; if some {@link MaterialInstance} remain, this method fails silently.
     * 在销毁指定 {@link Material} 之前，必须先销毁其所有 {@link MaterialInstance}；
     * 如果仍有 {@link MaterialInstance} 存在，此方法会静默失败。
     *
     * @param material the {@link Material} to destroy
     *                 要销毁的 {@link Material}
     */
    public void destroyMaterial(@NonNull Material material) {
        assertDestroy(nDestroyMaterial(getNativeObject(), material.getNativeObject()));
        material.clearNativeObject();
    }

    /**
     * Destroys a {@link MaterialInstance} and frees all its associated resources.
     * 销毁 {@link MaterialInstance} 并释放其所有关联资源。
     * @param materialInstance the {@link MaterialInstance} to destroy
     *                         要销毁的 {@link MaterialInstance}
     */
    public void destroyMaterialInstance(@NonNull MaterialInstance materialInstance) {
        assertDestroy(nDestroyMaterialInstance(getNativeObject(), materialInstance.getNativeObject()));
        materialInstance.clearNativeObject();
    }

    /**
     * Destroys a {@link Skybox} and frees all its associated resources.
     * 销毁 {@link Skybox} 并释放其所有关联资源。
     * @param skybox the {@link Skybox} to destroy
     *               要销毁的 {@link Skybox}
     */
    public void destroySkybox(@NonNull Skybox skybox) {
        assertDestroy(nDestroySkybox(getNativeObject(), skybox.getNativeObject()));
        skybox.clearNativeObject();
    }

    /**
     * Destroys a {@link ColorGrading} and frees all its associated resources.
     * 销毁 {@link ColorGrading} 并释放其所有关联资源。
     * @param colorGrading the {@link ColorGrading} to destroy
     *                     要销毁的 {@link ColorGrading}
     */
    public void destroyColorGrading(@NonNull ColorGrading colorGrading) {
        assertDestroy(nDestroyColorGrading(getNativeObject(), colorGrading.getNativeObject()));
        colorGrading.clearNativeObject();
    }

    /**
     * Destroys a {@link Texture} and frees all its associated resources.
     * 销毁 {@link Texture} 并释放其所有关联资源。
     * @param texture the {@link Texture} to destroy
     *                要销毁的 {@link Texture}
     */
    public void destroyTexture(@NonNull Texture texture) {
        assertDestroy(nDestroyTexture(getNativeObject(), texture.getNativeObject()));
        texture.clearNativeObject();
    }

    /**
     * Destroys a {@link RenderTarget} and frees all its associated resources.
     * 销毁 {@link RenderTarget} 并释放其所有关联资源。
     * @param target the {@link RenderTarget} to destroy
     *               要销毁的 {@link RenderTarget}
     */
    public void destroyRenderTarget(@NonNull RenderTarget target) {
        nDestroyRenderTarget(getNativeObject(), target.getNativeObject());
        target.clearNativeObject();
    }

    /**
     * Destroys all Filament-known components from this <code>entity</code>.
     * 销毁此 <code>entity</code> 中所有 Filament 已知的组件。
     * <p>
     * This method destroys Filament components only, not the <code>entity</code> itself. To destroy
     * the <code>entity</code> use <code>EntityManager#destroy</code>.
     * 此方法仅销毁 Filament 组件，而不是 <code>entity</code> 本身。要销毁 <code>entity</code>
     * 请使用 <code>EntityManager#destroy</code>。
     *
     * It is recommended to destroy components individually before destroying their
     * <code>entity</code>, this gives more control as to when the destruction really happens.
     * Otherwise, orphaned components are garbage collected, which can happen at a later time.
     * Even when component are garbage collected, the destruction of their <code>entity</code>
     * terminates their participation immediately.
     * 建议在销毁 <code>entity</code> 之前单独销毁组件，这样可以更好地控制销毁的时机。
     * 否则，孤立的组件会被垃圾回收，这可能在稍后的时间发生。
     * 即使组件被垃圾回收，销毁其 <code>entity</code> 也会立即终止它们的参与。
     *
     * @param entity the <code>entity</code> to destroy
     *               要销毁的 <code>entity</code>
     */
    public void destroyEntity(@Entity int entity) {
        nDestroyEntity(getNativeObject(), entity);
    }

    // Managers

    /**
     * @return the {@link TransformManager} used by this {@link Engine}
     *         此 {@link Engine} 使用的 {@link TransformManager}
     */
    @NonNull
    public TransformManager getTransformManager() {
        return mTransformManager;
    }

    /**
     * @return the {@link LightManager} used by this {@link Engine}
     *         此 {@link Engine} 使用的 {@link LightManager}
     */
    @NonNull
    public LightManager getLightManager() {
        return mLightManager;
    }

    /**
     * @return the {@link RenderableManager} used by this {@link Engine}
     *         此 {@link Engine} 使用的 {@link RenderableManager}
     */
    @NonNull
    public RenderableManager getRenderableManager() {
        return mRenderableManager;
    }

    /**
     * @return the {@link EntityManager} used by this {@link Engine}
     *         此 {@link Engine} 使用的 {@link EntityManager}
     */
    @NonNull
    public EntityManager getEntityManager() {
        return mEntityManager;
    }

    /**
     * Kicks the hardware thread (e.g.: the OpenGL, Vulkan or Metal thread) and blocks until
     * all commands to this point are executed. Note that this does guarantee that the
     * hardware is actually finished.
     * 启动硬件线程（例如：OpenGL、Vulkan 或 Metal 线程）并阻塞，直到执行到此点的所有命令都被执行。
     * 注意，这并不保证硬件实际完成。
     *
     * <p>This is typically used right after destroying the <code>SwapChain</code>,
     * in cases where a guarantee about the SwapChain destruction is needed in a timely fashion,
     * such as when responding to Android's
     * {@link  android.view.SurfaceHolder.Callback#surfaceDestroyed surfaceDestroyed}.</p>
     * <p>这通常在销毁 <code>SwapChain</code> 后立即使用，在需要及时保证 SwapChain 销毁的情况下，
     * 例如在响应 Android 的 {@link android.view.SurfaceHolder.Callback#surfaceDestroyed surfaceDestroyed} 时。</p>
     */
    public void flushAndWait() {
        boolean unused = flushAndWait(Fence.WAIT_FOR_EVER);
    }

    /**
     * Kicks the hardware thread (e.g. the OpenGL, Vulkan or Metal thread) and blocks until
     * all commands to this point are executed. Note that does guarantee that the
     * hardware is actually finished.
     * 启动硬件线程（例如 OpenGL、Vulkan 或 Metal 线程）并阻塞，直到执行到此点的所有命令都被执行。
     * 注意，这并不保证硬件实际完成。
     *
     * A timeout can be specified, if for some reason this flushAndWait doesn't complete before the timeout, it will
     * return false, true otherwise.
     * 可以指定超时时间，如果由于某种原因此 flushAndWait 在超时前没有完成，它将返回 false，否则返回 true。
     *
     * <p>This is typically used right after destroying the <code>SwapChain</code>,
     * in cases where a guarantee about the <code>SwapChain</code> destruction is needed in a
     * timely fashion, such as when responding to Android's
     * <code>android.view.SurfaceHolder.Callback.surfaceDestroyed</code></p>
     * <p>这通常在销毁 <code>SwapChain</code> 后立即使用，在需要及时保证 <code>SwapChain</code> 销毁的情况下，
     * 例如在响应 Android 的 <code>android.view.SurfaceHolder.Callback.surfaceDestroyed</code> 时</p>
     *
     * @param timeout A timeout in nanoseconds
     *                超时时间（以纳秒为单位）
     * @return true if successful, false if flushAndWait timed out, in which case it wasn't successful and commands
     * might still be executing on both the CPU and GPU sides.
     *         如果成功则返回 true，如果 flushAndWait 超时则返回 false，在这种情况下操作不成功，
     *         命令可能仍在 CPU 和 GPU 端执行。
     */
    public boolean flushAndWait(long timeout) {
        return nFlushAndWait(getNativeObject(), timeout);
    }

    /**
     * Kicks the hardware thread (e.g. the OpenGL, Vulkan or Metal thread) but does not wait
     * for commands to be either executed or the hardware finished.
     * 启动硬件线程（例如 OpenGL、Vulkan 或 Metal 线程）但不等待命令执行或硬件完成。
     *
     * <p>This is typically used after creating a lot of objects to start draining the command
     * queue which has a limited size.</p>
     * <p>这通常在创建大量对象后使用，以开始排空有限大小的命令队列。</p>
     */
    public void flush() {
        nFlush(getNativeObject());
    }

    /**
     * Get paused state of rendering thread.
     * 获取渲染线程的暂停状态。
     *
     * <p>Warning: This is an experimental API.
     * <p>警告：这是一个实验性 API。
     *
     * @see #setPaused
     */
    public boolean isPaused() {
        return nIsPaused(getNativeObject());
    }

    /**
     * Pause or resume the rendering thread.
     * 暂停或恢复渲染线程。
     *
     * <p>Warning: This is an experimental API. In particular, note the following caveats.
     * <p>警告：这是一个实验性 API。特别要注意以下注意事项。
     *
     * <ul><li>
     * Buffer callbacks will never be called as long as the rendering thread is paused.
     * Do not rely on a buffer callback to unpause the thread.
     * 只要渲染线程暂停，缓冲区回调就永远不会被调用。不要依赖缓冲区回调来取消暂停线程。
     * </li><li>
     * While the rendering thread is paused, rendering commands will continue to be queued until the
     * buffer limit is reached. When the limit is reached, the program will abort.
     * 当渲染线程暂停时，渲染命令将继续排队，直到达到缓冲区限制。当达到限制时，程序将中止。
     * </li></ul>
     */
    public void setPaused(boolean paused) {
        nSetPaused(getNativeObject(), paused);
    }

    /**
     * Switch the command queue to unprotected mode. Protected mode can be activated via
     * Renderer::beginFrame() using a protected SwapChain.
     * 将命令队列切换到非保护模式。保护模式可以通过使用受保护的 SwapChain 的 Renderer::beginFrame() 激活。
     * @see Renderer
     * @see SwapChain
     */
    public void unprotected() {
        nUnprotected(getNativeObject());
    }

    /**
     * Get the current time. This is a convenience function that simply returns the
     * time in nanosecond since epoch of std::chrono::steady_clock.
     * 获取当前时间。这是一个便利函数，简单地返回自 std::chrono::steady_clock 纪元以来的纳秒时间。
     * @return current time in nanosecond since epoch of std::chrono::steady_clock.
     *         自 std::chrono::steady_clock 纪元以来的当前时间（以纳秒为单位）。
     * @see Renderer#beginFrame
     */
    public static native long getSteadyClockTimeNano();


    /**
     * Checks if a feature flag exists
     * 检查特性标志是否存在
     * @param name name of the feature flag to check
     *             要检查的特性标志名称
     * @return true if it exists false otherwise
     *         如果存在则返回 true，否则返回 false
     */
    public boolean hasFeatureFlag(@NonNull String name) {
        return nHasFeatureFlag(mNativeObject, name);
    }

    /**
     * Set the value of a non-constant feature flag.
     * 设置非常量特性标志的值。
     * @param name name of the feature flag to set
     *             要设置的特性标志名称
     * @param value value to set
     *              要设置的值
     * @return true if the value was set, false if the feature flag is constant or doesn't exist.
     *         如果值已设置则返回 true，如果特性标志是常量或不存在则返回 false。
     */
    public boolean setFeatureFlag(@NonNull String name, boolean value) {
        return nSetFeatureFlag(mNativeObject, name, value);
    }

    /**
     * Retrieves the value of any feature flag.
     * 检索任何特性标志的值。
     * @param name name of the feature flag
     *             特性标志的名称
     * @return the value of the flag if it exists
     *         如果标志存在则返回其值
     * @exception IllegalArgumentException is thrown if the feature flag doesn't exist
     *                                     如果特性标志不存在则抛出此异常
     */
    public boolean getFeatureFlag(@NonNull String name) {
        if (!hasFeatureFlag(name)) {
            throw new IllegalArgumentException("The feature flag \"" + name + "\" doesn't exist");
        }
        return nGetFeatureFlag(mNativeObject, name);
    }

    @UsedByReflection("TextureHelper.java")
    public long getNativeObject() {
        if (mNativeObject == 0) {
            throw new IllegalStateException("Calling method on destroyed Engine");
        }
        return mNativeObject;
    }

    @UsedByReflection("MaterialBuilder.java")
    public long getNativeJobSystem() {
        if (mNativeObject == 0) {
            throw new IllegalStateException("Calling method on destroyed Engine");
        }
        return nGetJobSystem(getNativeObject());
    }

    private void clearNativeObject() {
        mNativeObject = 0;
    }

    private static void assertDestroy(boolean success) {
        if (!success) {
            throw new IllegalStateException("Object couldn't be destroyed (double destroy()?)");
        }
    }

    private static native void nDestroyEngine(long nativeEngine);
    private static native long nGetBackend(long nativeEngine);
    private static native long nCreateSwapChain(long nativeEngine, Object nativeWindow, long flags);
    private static native long nCreateSwapChainHeadless(long nativeEngine, int width, int height, long flags);
    private static native long nCreateSwapChainFromRawPointer(long nativeEngine, long pointer, long flags);
    private static native long nCreateView(long nativeEngine);
    private static native long nCreateRenderer(long nativeEngine);
    private static native long nCreateCamera(long nativeEngine, int entity);
    private static native long nGetCameraComponent(long nativeEngine, int entity);
    private static native void nDestroyCameraComponent(long nativeEngine, int entity);
    private static native long nCreateScene(long nativeEngine);
    private static native long nCreateFence(long nativeEngine);

    private static native boolean nDestroyRenderer(long nativeEngine, long nativeRenderer);
    private static native boolean nDestroyView(long nativeEngine, long nativeView);
    private static native boolean nDestroyScene(long nativeEngine, long nativeScene);
    private static native boolean nDestroyFence(long nativeEngine, long nativeFence);
    private static native boolean nDestroyStream(long nativeEngine, long nativeStream);
    private static native boolean nDestroyIndexBuffer(long nativeEngine, long nativeIndexBuffer);
    private static native boolean nDestroyVertexBuffer(long nativeEngine, long nativeVertexBuffer);
    private static native boolean nDestroySkinningBuffer(long nativeEngine, long nativeSkinningBuffer);
    private static native boolean nDestroyIndirectLight(long nativeEngine, long nativeIndirectLight);
    private static native boolean nDestroyMaterial(long nativeEngine, long nativeMaterial);
    private static native boolean nDestroyMaterialInstance(long nativeEngine, long nativeMaterialInstance);
    private static native boolean nDestroySkybox(long nativeEngine, long nativeSkybox);
    private static native boolean nDestroyColorGrading(long nativeEngine, long nativeColorGrading);
    private static native boolean nDestroyTexture(long nativeEngine, long nativeTexture);
    private static native boolean nDestroyRenderTarget(long nativeEngine, long nativeTarget);
    private static native boolean nDestroySwapChain(long nativeEngine, long nativeSwapChain);
    private static native boolean nIsValidRenderer(long nativeEngine, long nativeRenderer);
    private static native boolean nIsValidView(long nativeEngine, long nativeView);
    private static native boolean nIsValidScene(long nativeEngine, long nativeScene);
    private static native boolean nIsValidFence(long nativeEngine, long nativeFence);
    private static native boolean nIsValidStream(long nativeEngine, long nativeStream);
    private static native boolean nIsValidIndexBuffer(long nativeEngine, long nativeIndexBuffer);
    private static native boolean nIsValidVertexBuffer(long nativeEngine, long nativeVertexBuffer);
    private static native boolean nIsValidSkinningBuffer(long nativeEngine, long nativeSkinningBuffer);
    private static native boolean nIsValidIndirectLight(long nativeEngine, long nativeIndirectLight);
    private static native boolean nIsValidMaterial(long nativeEngine, long nativeMaterial);
    private static native boolean nIsValidMaterialInstance(long nativeEngine, long nativeMaterial, long nativeMaterialInstance);
    private static native boolean nIsValidExpensiveMaterialInstance(long nativeEngine, long nativeMaterialInstance);
    private static native boolean nIsValidSkybox(long nativeEngine, long nativeSkybox);
    private static native boolean nIsValidColorGrading(long nativeEngine, long nativeColorGrading);
    private static native boolean nIsValidTexture(long nativeEngine, long nativeTexture);
    private static native boolean nIsValidRenderTarget(long nativeEngine, long nativeTarget);
    private static native boolean nIsValidSwapChain(long nativeEngine, long nativeSwapChain);
    private static native void nDestroyEntity(long nativeEngine, int entity);
    private static native boolean nFlushAndWait(long nativeEngine, long timeout);
    private static native void nFlush(long nativeEngine);
    private static native boolean nIsPaused(long nativeEngine);
    private static native void nSetPaused(long nativeEngine, boolean paused);
    private static native void nUnprotected(long nativeEngine);
    private static native long nGetTransformManager(long nativeEngine);
    private static native long nGetLightManager(long nativeEngine);
    private static native long nGetRenderableManager(long nativeEngine);
    private static native long nGetJobSystem(long nativeEngine);
    private static native long nGetEntityManager(long nativeEngine);
    private static native void nSetAutomaticInstancingEnabled(long nativeEngine, boolean enable);
    private static native boolean nIsAutomaticInstancingEnabled(long nativeEngine);
    private static native long nGetMaxStereoscopicEyes(long nativeEngine);
    private static native int nGetSupportedFeatureLevel(long nativeEngine);
    private static native int nSetActiveFeatureLevel(long nativeEngine, int ordinal);
    private static native int nGetActiveFeatureLevel(long nativeEngine);
    private static native boolean nHasFeatureFlag(long nativeEngine, String name);
    private static native boolean nSetFeatureFlag(long nativeEngine, String name, boolean value);
    private static native boolean nGetFeatureFlag(long nativeEngine, String name);

    private static native long nCreateBuilder();
    private static native void nDestroyBuilder(long nativeBuilder);
    private static native void nSetBuilderBackend(long nativeBuilder, long backend);
    private static native void nSetBuilderConfig(long nativeBuilder, long commandBufferSizeMB,
            long perRenderPassArenaSizeMB, long driverHandleArenaSizeMB,
            long minCommandBufferSizeMB, long perFrameCommandsSizeMB, long jobSystemThreadCount,
            boolean disableParallelShaderCompile, int stereoscopicType, long stereoscopicEyeCount,
            long resourceAllocatorCacheSizeMB, long resourceAllocatorCacheMaxAge,
            boolean disableHandleUseAfterFreeCheck,
            int preferredShaderLanguage,
            boolean forceGLES2Context, boolean assertNativeWindowIsValid);
    private static native void nSetBuilderFeatureLevel(long nativeBuilder, int ordinal);
    private static native void nSetBuilderSharedContext(long nativeBuilder, long sharedContext);
    private static native void nSetBuilderPaused(long nativeBuilder, boolean paused);
    private static native void nSetBuilderFeature(long nativeBuilder, String name, boolean value);
    private static native long nBuilderBuild(long nativeBuilder);
}
