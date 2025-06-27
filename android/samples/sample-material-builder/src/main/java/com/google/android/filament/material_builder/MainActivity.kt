/*
 * Copyright (C) 2018 The Android Open Source Project
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

/**
 * Filament Material Builder 示例应用
 * 
 * 这个示例展示了如何使用 Filament 的 MaterialBuilder API 在运行时动态创建材质。
 * 该应用创建了一个具有清漆涂层(clear coat)效果的金属材质，并将其应用到一个球体网格上。
 * 
 * 主要功能：
 * - 使用 MaterialBuilder 动态构建 PBR 材质
 * - 演示 Filament 的标准材质模型(Lit model)
 * - 展示基于图像的光照(IBL)
 * - 实现相机动画和渲染循环
 * 
 * 材质属性说明：
 * - baseColor: 基础颜色，对于金属表面是镜面反射颜色
 * - roughness: 表面粗糙度，影响反射的锐利程度
 * - metallic: 金属度，决定表面是电介质还是导体
 * - clearCoat: 清漆涂层强度，模拟汽车漆面等效果
 */

package com.google.android.filament.material_builder

import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.animation.LinearInterpolator

import com.google.android.filament.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.filamat.MaterialBuilder

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Filament Material Builder 主活动类
 * 
 * 这个类演示了如何使用 Filament 引擎创建和渲染具有动态材质的 3D 场景。
 * 它展示了 Filament 的核心概念：引擎、渲染器、场景、视图、相机和材质系统。
 */
class MainActivity : Activity() {
    // Make sure to initialize Filament first
    // This loads the JNI library needed by most API calls
    // 确保首先初始化 Filament
    // 这会加载大多数 API 调用所需的 JNI 库
    companion object {
        init {
            Filament.init()
        }
    }

    // The View we want to render into
    // 我们要渲染到的视图
    private lateinit var surfaceView: SurfaceView
    
    // UiHelper is provided by Filament to manage SurfaceView and SurfaceTexture
    // UiHelper 由 Filament 提供，用于管理 SurfaceView 和 SurfaceTexture
    // 它处理 Android 表面生命周期和渲染回调
    private lateinit var uiHelper: UiHelper
    
    // DisplayHelper is provided by Filament to manage the display
    // DisplayHelper 由 Filament 提供，用于管理显示器
    // 它处理显示器相关的配置和同步
    private lateinit var displayHelper: DisplayHelper
    
    // Choreographer is used to schedule new frames
    // Choreographer 用于调度新帧
    // 它与 Android 的 VSync 信号同步，确保流畅的动画
    private lateinit var choreographer: Choreographer

    // Engine creates and destroys Filament resources
    // Each engine must be accessed from a single thread of your choosing
    // Resources cannot be shared across engines
    // Engine 创建和销毁 Filament 资源
    // 每个引擎必须从您选择的单个线程访问
    // 资源不能在引擎之间共享
    private lateinit var engine: Engine
    
    // A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
    // 渲染器实例绑定到单个表面（SurfaceView、TextureView 等）
    // 负责执行实际的渲染命令和 GPU 操作
    private lateinit var renderer: Renderer
    
    // A scene holds all the renderable, lights, etc. to be drawn
    // 场景包含所有要绘制的可渲染对象、光源等
    // 它是所有 3D 对象的容器
    private lateinit var scene: Scene
    
    // A view defines a viewport, a scene and a camera for rendering
    // 视图定义了用于渲染的视口、场景和相机
    // 它将场景、相机和渲染设置组合在一起
    private lateinit var view: View
    
    // Should be pretty obvious :)
    // 相机对象，定义观察点和投影参数
    private lateinit var camera: Camera

    // 材质定义，包含着色器代码和参数定义
    private lateinit var material: Material
    
    // 材质实例，可以设置具体的参数值
    // 多个实例可以共享同一个材质定义但使用不同的参数
    private lateinit var materialInstance: MaterialInstance

    // 网格对象，包含几何数据和渲染实体
    private lateinit var mesh: Mesh
    
    // 基于图像的光照(Image-Based Lighting)
    // 包含天空盒和间接光照信息
    private lateinit var ibl: Ibl

    // Filament entity representing a renderable object
    // Filament 实体，表示一个可渲染对象（这里是定向光源）
    @Entity private var light = 0

    // A swap chain is Filament's representation of a surface
    // 交换链是 Filament 对表面的表示
    // 它管理前后缓冲区的交换，实现双缓冲渲染
    private var swapChain: SwapChain? = null

    // Performs the rendering and schedules new frames
    // 执行渲染并调度新帧
    private val frameScheduler = FrameCallback()

    // 相机动画器，创建围绕物体的圆周运动
    private val animator = ValueAnimator.ofFloat(0.0f, (2.0 * PI).toFloat())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        choreographer = Choreographer.getInstance()

        displayHelper = DisplayHelper(this)

        setupSurfaceView()
        setupFilament()
        setupView()
        setupScene()
    }

    /**
     * 设置 SurfaceView 和相关的 UI 助手
     * 
     * UiHelper 管理 Android 表面的生命周期，包括创建、销毁和尺寸变化。
     * 它还处理与 Filament 渲染器的集成。
     */
    private fun setupSurfaceView() {
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()

        // NOTE: To choose a specific rendering resolution, add the following line:
        // 注意：要选择特定的渲染分辨率，请添加以下行：
        // uiHelper.setDesiredSize(1280, 720)

        uiHelper.attachTo(surfaceView)
    }

    /**
     * 初始化 Filament 核心组件
     * 
     * 创建引擎、渲染器、场景、视图和相机。
     * 这些是 Filament 渲染管线的基础组件。
     */
    private fun setupFilament() {
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())
    }

    /**
     * 配置视图设置
     * 
     * 将相机和场景关联到视图。视图定义了渲染的配置，
     * 包括后处理效果、色调映射等。
     */
    private fun setupView() {
        // NOTE: Try to disable post-processing (tone-mapping, etc.) to see the difference
        // 注意：尝试禁用后处理（色调映射等）以查看差异
        // view.isPostProcessingEnabled = false

        // Tell the view which camera we want to use
        // 告诉视图我们要使用哪个相机
        view.camera = camera

        // Tell the view which scene we want to render
        // 告诉视图我们要渲染哪个场景
        view.scene = scene
    }

    /**
     * 设置 3D 场景
     * 
     * 这个方法创建完整的 3D 场景，包括：
     * - 构建和配置 PBR 材质
     * - 加载基于图像的光照环境
     * - 加载和定位 3D 网格
     * - 创建定向光源
     * - 设置相机曝光参数
     */
    private fun setupScene() {
        buildMaterial()
        setupMaterial()
        loadImageBasedLight()

        // 设置场景的天空盒和间接光照
        scene.skybox = ibl.skybox
        scene.indirectLight = ibl.indirectLight

        // This map can contain named materials that will map to the material names
        // loaded from the filamesh file. The material called "DefaultMaterial" is
        // applied when no named material can be found
        // 这个映射包含命名材质，将映射到从 filamesh 文件加载的材质名称
        // 当找不到命名材质时，会应用名为 "DefaultMaterial" 的材质
        val materials = mapOf("DefaultMaterial" to materialInstance)

        // Load the mesh in the filamesh format (see filamesh tool)
        // 以 filamesh 格式加载网格（参见 filamesh 工具）
        mesh = loadMesh(assets, "models/shader_ball.filamesh", materials, engine)

        // Move the mesh down
        // Filament uses column-major matrices
        // 向下移动网格
        // Filament 使用列主序矩阵
        engine.transformManager.setTransform(engine.transformManager.getInstance(mesh.renderable),
                floatArrayOf(
                        1.0f,  0.0f, 0.0f, 0.0f,
                        0.0f,  1.0f, 0.0f, 0.0f,
                        0.0f,  0.0f, 1.0f, 0.0f,
                        0.0f, -1.2f, 0.0f, 1.0f
                ))

        // Add the entity to the scene to render it
        // 将实体添加到场景中以渲染它
        scene.addEntity(mesh.renderable)

        // We now need a light, let's create a directional light
        // 现在我们需要一个光源，让我们创建一个定向光
        light = EntityManager.get().create()

        // Create a color from a temperature (D65)
        // 从色温创建颜色（D65 标准光源）
        val (r, g, b) = Colors.cct(6_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b)
                // Intensity of the sun in lux on a clear day
                // 晴天太阳的强度（以勒克斯为单位）
                .intensity(110_000.0f)
                // The direction is normalized on our behalf
                // 方向会自动为我们标准化
                .direction(-0.753f, -1.0f, 0.890f)
                .castShadows(true)
                .build(engine, light)

        // Add the entity to the scene to light it
        // 将光源实体添加到场景中以照亮它
        scene.addEntity(light)

        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // Since we've defined a light that has the same intensity as the sun, it
        // guarantees a proper exposure
        // 设置相机曝光，此曝光遵循阳光 f/16 法则
        // 由于我们定义了与太阳相同强度的光源，它保证了正确的曝光
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)

        startAnimation()
    }

    /**
     * 构建 PBR 材质
     * 
     * 使用 MaterialBuilder API 动态创建一个具有清漆涂层效果的金属材质。
     * 这个材质使用 Filament 的标准光照模型（Lit model），支持以下特性：
     * - 基础颜色参数（可在运行时修改）
     * - 金属表面属性（metallic = 1.0）
     * - 中等粗糙度（roughness = 0.65）
     * - 完整清漆涂层（clearCoat = 1.0）
     */
    private fun buildMaterial() {
        // MaterialBuilder.init() must be called before any MaterialBuilder methods can be used.
        // It only needs to be called once per process.
        // When your app is done building materials, call MaterialBuilder.shutdown() to free
        // internal MaterialBuilder resources.
        // MaterialBuilder.init() 必须在使用任何 MaterialBuilder 方法之前调用
        // 每个进程只需要调用一次
        // 当应用完成材质构建后，调用 MaterialBuilder.shutdown() 释放内部资源
        MaterialBuilder.init()

        val matPackage = MaterialBuilder()
                // By default, materials are generated only for DESKTOP. Since we're an Android
                // app, we set the platform to MOBILE.
                // 默认情况下，材质仅为桌面平台生成。由于我们是 Android 应用，
                // 我们将平台设置为移动端
                .platform(MaterialBuilder.Platform.MOBILE)

                // Set the name of the Material for debugging purposes.
                // 设置材质名称用于调试目的
                .name("Clear coat")

                // Defaults to LIT. We could change the shading model here if we desired.
                // 默认为 LIT（标准光照）。如果需要，我们可以在这里更改着色模型
                .shading(MaterialBuilder.Shading.LIT)

                // Add a parameter to the material that can be set via the setParameter method once
                // we have a material instance.
                // 向材质添加参数，可以通过材质实例的 setParameter 方法设置
                .uniformParameter(MaterialBuilder.UniformType.FLOAT3, "baseColor")

                // Fragment block- see the material readme (docs/Materials.md.html) for the full
                // specification.
                // 片段着色器代码块 - 完整规范请参见材质文档
                .material("void material(inout MaterialInputs material) {\n" +
                        "    prepareMaterial(material);\n" +
                        "    material.baseColor.rgb = materialParams.baseColor;\n" +
                        "    material.roughness = 0.65;\n" +
                        "    material.metallic = 1.0;\n" +
                        "    material.clearCoat = 1.0;\n" +
                        "}\n")

                // Turn off shader code optimization so this sample is compatible with the "lite"
                // variant of the filamat library.
                // 关闭着色器代码优化，使此示例与 filamat 库的 "lite" 变体兼容
                .optimization(MaterialBuilder.Optimization.NONE)

                // When compiling more than one material variant, it is more efficient to pass an Engine
                // instance to reuse the Engine's job system
                // 编译多个材质变体时，传递 Engine 实例以重用作业系统更高效
                .build(engine)

        if (matPackage.isValid) {
            val buffer = matPackage.buffer
            material = Material.Builder().payload(buffer, buffer.remaining()).build(engine)
        }

        // We're done building materials, so we call shutdown here to free resources. If we wanted
        // to build more materials, we could call MaterialBuilder.init() again (with a slight
        // performance hit).
        // 材质构建完成，在此调用 shutdown 释放资源。如果要构建更多材质，
        // 可以再次调用 MaterialBuilder.init()（会有轻微的性能损失）
        MaterialBuilder.shutdown()
    }

    /**
     * 设置材质实例参数
     * 
     * 创建材质实例并设置具体的参数值。材质实例允许我们使用相同的
     * 材质定义但配置不同的参数值。
     */
    private fun setupMaterial() {
        // Create an instance of the material to set different parameters on it
        // 创建材质实例以在其上设置不同的参数
        materialInstance = material.createInstance()
        
        // Specify that our color is in sRGB so the conversion to linear
        // is done automatically for us. If you already have a linear color
        // you can pass it directly, or use Colors.RgbType.LINEAR
        // 指定我们的颜色是 sRGB 格式，这样会自动进行到线性空间的转换
        // 如果您已经有线性颜色，可以直接传递，或使用 Colors.RgbType.LINEAR
        materialInstance.setParameter("baseColor", Colors.RgbType.SRGB, 0.71f, 0.0f, 0.0f)
    }

    /**
     * 加载基于图像的光照（IBL）
     * 
     * IBL 提供环境光照和反射，使用预计算的环境贴图来模拟真实的光照条件。
     * 这包括天空盒和间接光照信息。
     */
    private fun loadImageBasedLight() {
        ibl = loadIbl(assets, "envs/flower_road_no_sun_2k", engine)
        // 设置间接光照强度，影响环境光的亮度
        ibl.indirectLight.intensity = 40_000.0f
    }

    /**
     * 启动相机动画
     * 
     * 创建一个围绕物体的圆周运动动画，相机在固定半径上旋转，
     * 始终朝向场景中心的物体。
     */
    private fun startAnimation() {
        // Animate the triangle
        // 动画化相机（注释中的 triangle 应该是 camera）
        animator.interpolator = LinearInterpolator()
        animator.duration = 18_000  // 18秒完成一圈
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener { a ->
            val v = (a.animatedValue as Float)
            // 使用三角函数创建圆周运动，相机距离中心 4.5 单位，高度 1.5
            camera.lookAt(cos(v) * 4.5, 1.5, sin(v) * 4.5, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
        }
        animator.start()
    }

    /**
     * Activity 恢复时的处理
     * 
     * 重新启动渲染循环和动画。当 Activity 从后台返回前台时调用。
     */
    override fun onResume() {
        super.onResume()
        // 重新注册帧回调以恢复渲染
        choreographer.postFrameCallback(frameScheduler)
        // 重新启动相机动画
        animator.start()
    }

    /**
     * Activity 暂停时的处理
     * 
     * 停止渲染循环和动画以节省资源。当 Activity 进入后台时调用。
     */
    override fun onPause() {
        super.onPause()
        // 移除帧回调以停止渲染
        choreographer.removeFrameCallback(frameScheduler)
        // 取消动画
        animator.cancel()
    }

    /**
     * Activity 销毁时的资源清理
     * 
     * 正确的资源清理对于避免内存泄漏至关重要。Filament 资源必须
     * 按照特定的顺序销毁，以避免悬空引用。
     */
    override fun onDestroy() {
        super.onDestroy()

        // Stop the animation and any pending frame
        // 停止动画和任何待处理的帧
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel();

        // Always detach the surface before destroying the engine
        // 在销毁引擎之前始终分离表面
        uiHelper.detach()

        // Cleanup all resources
        // 清理所有资源（按照依赖关系的逆序）
        destroyMesh(engine, mesh)
        destroyIbl(engine, ibl)
        engine.destroyEntity(light)
        engine.destroyRenderer(renderer)
        engine.destroyMaterialInstance(materialInstance)
        engine.destroyMaterial(material)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)

        // Engine.destroyEntity() destroys Filament related resources only
        // (components), not the entity itself
        // Engine.destroyEntity() 只销毁 Filament 相关资源（组件），
        // 而不是实体本身
        val entityManager = EntityManager.get()
        entityManager.destroy(light)
        entityManager.destroy(camera.entity)

        // Destroying the engine will free up any resource you may have forgotten
        // to destroy, but it's recommended to do the cleanup properly
        // 销毁引擎会释放您可能忘记销毁的任何资源，
        // 但建议正确进行清理
        engine.destroy()
    }

    /**
     * 帧回调类
     * 
     * 这个类处理每一帧的渲染。它与 Android 的 Choreographer 集成，
     * 确保渲染与设备的刷新率同步，提供流畅的动画效果。
     */
    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // Schedule the next frame
            // 调度下一帧
            choreographer.postFrameCallback(this)

            // This check guarantees that we have a swap chain
            // 此检查确保我们有一个交换链
            if (uiHelper.isReadyToRender) {
                // If beginFrame() returns false you should skip the frame
                // This means you are sending frames too quickly to the GPU
                // 如果 beginFrame() 返回 false，您应该跳过该帧
                // 这意味着您向 GPU 发送帧的速度太快
                if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
            }
        }
    }

    /**
     * 表面回调类
     * 
     * 处理 Android 表面的生命周期事件，包括创建、销毁和尺寸变化。
     * 这些事件对应于 SurfaceView 的状态变化。
     */
    inner class SurfaceCallback : UiHelper.RendererCallback {
        /**
         * 当原生窗口（Surface）发生变化时调用
         * 
         * 这通常发生在 SurfaceView 首次创建或设备旋转时。
         */
        override fun onNativeWindowChanged(surface: Surface) {
            // 销毁旧的交换链（如果存在）
            swapChain?.let { engine.destroySwapChain(it) }
            // 为新表面创建交换链
            swapChain = engine.createSwapChain(surface)
            // 将显示助手附加到渲染器
            displayHelper.attach(renderer, surfaceView.display)
        }

        /**
         * 当表面从视图分离时调用
         * 
         * 这发生在 SurfaceView 被销毁时，需要清理相关资源。
         */
        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                engine.destroySwapChain(it)
                // Required to ensure we don't return before Filament is done executing the
                // destroySwapChain command, otherwise Android might destroy the Surface
                // too early
                // 确保在 Filament 完成执行 destroySwapChain 命令之前不返回，
                // 否则 Android 可能过早销毁 Surface
                engine.flushAndWait()
                swapChain = null
            }
        }

        /**
         * 当表面尺寸发生变化时调用
         * 
         * 更新相机投影和视口以匹配新的表面尺寸。
         */
        override fun onResized(width: Int, height: Int) {
            // 计算新的宽高比
            val aspect = width.toDouble() / height.toDouble()
            // 更新相机投影：45度视野角，新宽高比，近平面0.1，远平面20.0
            camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)

            // 更新视图的视口以匹配新尺寸
            view.viewport = Viewport(0, 0, width, height)

            // 同步待处理的帧以确保尺寸变化立即生效
            FilamentHelper.synchronizePendingFrames(engine)
        }
    }
}
