/*
 * Copyright (C) 2018 The Android Open Source Project
 * 版权所有 (C) 2018 Android开源项目
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * 根据Apache许可证2.0版本（"许可证"）获得许可；
 * you may not use this file except in compliance with the License.
 * 除非遵守许可证，否则您不得使用此文件。
 * You may obtain a copy of the License at
 * 您可以在以下网址获得许可证副本：
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * 除非适用法律要求或书面同意，否则根据许可证分发的软件
 * distributed under the License is distributed on an "AS IS" BASIS,
 * 按"原样"分发，不提供任何明示或暗示的保证或条件。
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 请参阅许可证以了解管理权限和限制的特定语言。
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// 包声明：定义了此类所属的包命名空间
package com.google.android.filament.ibl

// Android系统相关导入

// Filament 3D渲染引擎相关导入

// Java NIO和数学相关导入
import android.animation.ValueAnimator
import android.app.Activity
import android.os.Bundle
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import com.google.android.filament.Camera
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.SwapChain
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.channels.Channels
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 主活动类：基于图像的光照(IBL)示例
 * 这个示例展示了如何使用Filament渲染引擎实现基于图像的光照效果
 * IBL是一种使用环境贴图来照亮3D场景的技术，可以产生非常真实的光照效果
 */
class MainActivity : Activity() {
    /**
     * Make sure to initialize Filament first
     * 确保首先初始化Filament
     * This loads the JNI library needed by most API calls
     * 这会加载大多数API调用所需的JNI库
     */
    companion object {
        init {
            // 初始化Filament渲染引擎
            Filament.init()
        }
    }

    // ========== UI相关组件 ==========

    /**
     * The View we want to render into
     * 我们要渲染到的视图
     */
    private lateinit var surfaceView: SurfaceView

    /**
     * UiHelper is provided by Filament to manage SurfaceView and SurfaceTexture
     * UiHelper由Filament提供，用于管理SurfaceView和SurfaceTexture
     */
    private lateinit var uiHelper: UiHelper

    /**
     * DisplayHelper is provided by Filament to manage the display
     * DisplayHelper由Filament提供，用于管理显示器
     */
    private lateinit var displayHelper: DisplayHelper

    /**
     * Choreographer is used to schedule new frames
     * Choreographer用于调度新帧的渲染
     */
    private lateinit var choreographer: Choreographer

    // ========== Filament核心渲染组件 ==========

    /**
     * Engine creates and destroys Filament resources
     * 引擎负责创建和销毁Filament资源
     * Each engine must be accessed from a single thread of your choosing
     * 每个引擎必须从您选择的单个线程访问
     * Resources cannot be shared across engines
     * 资源不能在引擎之间共享
     */
    private lateinit var engine: Engine

    /**
     * A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
     * 渲染器实例绑定到单个表面（SurfaceView、TextureView等）
     */
    private lateinit var renderer: Renderer

    /**
     * A scene holds all the renderable, lights, etc. to be drawn
     * 场景包含所有要绘制的可渲染对象、光源等
     */
    private lateinit var scene: Scene

    /**
     * A view defines a viewport, a scene and a camera for rendering
     * 视图定义了用于渲染的视口、场景和相机
     */
    private lateinit var view: View

    /**
     * Should be pretty obvious :)
     * 相机对象（这个应该很明显了 :)）
     */
    private lateinit var camera: Camera

    // ========== 材质和渲染资源 ==========
    /**
     * 材质定义了物体表面的外观属性（颜色、反射率、粗糙度等）
     */
    private lateinit var material: Material

    /**
     * 材质实例允许我们为同一材质设置不同的参数值
     */
    private lateinit var materialInstance: MaterialInstance

    /**
     * 网格包含3D模型的几何数据（顶点、法线、纹理坐标等）
     */
    private lateinit var mesh: Mesh

    /**
     * IBL（基于图像的光照）包含环境光照信息
     */
    private lateinit var ibl: Ibl

    /**
     * Filament entity representing a renderable object
     * Filament实体，表示一个可渲染的光源对象
     */
    @Entity
    private var light = 0

    /**
     * A swap chain is Filament's representation of a surface
     * 交换链是Filament对表面的表示，用于双缓冲渲染
     */
    private var swapChain: SwapChain? = null

    // ========== 动画和帧调度 ==========

    /**
     * Performs the rendering and schedules new frames
     * 执行渲染并调度新帧
     */
    private val frameScheduler = FrameCallback()

    /**
     * 值动画器，用于创建相机围绕物体旋转的动画效果
     */
    private val animator = ValueAnimator.ofFloat(0.0f, (2.0 * PI).toFloat())

    /**
     * Activity创建时的回调方法
     * 在这里初始化所有必要的组件和设置
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建SurfaceView作为渲染表面
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // 获取Choreographer实例，用于帧同步
        choreographer = Choreographer.getInstance()

        // 创建显示辅助器
        displayHelper = DisplayHelper(this)

        // 按顺序初始化各个组件
        setupSurfaceView()  // 设置渲染表面
        setupFilament()     // 初始化Filament引擎
        setupView()         // 配置渲染视图
        setupScene()        // 构建3D场景
    }

    /**
     * 设置SurfaceView和相关的UI组件
     * 配置渲染回调和表面管理
     */
    private fun setupSurfaceView() {
        // 创建UiHelper，不检查上下文错误（用于性能优化）
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        // 设置渲染回调，处理表面生命周期事件
        uiHelper.renderCallback = SurfaceCallback()

        // NOTE: To choose a specific rendering resolution, add the following line:
        // 注意：要选择特定的渲染分辨率，请添加以下行：
        // uiHelper.setDesiredSize(1280, 720)

        // 将UiHelper附加到SurfaceView
        uiHelper.attachTo(surfaceView)
    }

    /**
     * 初始化Filament渲染引擎和核心组件
     * 创建引擎、渲染器、场景、视图和相机
     */
    private fun setupFilament() {
        // 创建Filament引擎实例
        engine = Engine.create()
        // 创建渲染器，负责实际的渲染工作
        renderer = engine.createRenderer()
        // 创建场景，用于容纳所有3D对象
        scene = engine.createScene()
        // 创建视图，定义渲染的视口和参数
        view = engine.createView()
        // 创建相机实体和相机组件
        camera = engine.createCamera(engine.entityManager.create())
    }

    /**
     * 配置渲染视图的各种选项
     * 设置环境光遮蔽、后处理效果等
     */
    private fun setupView() {
        // ambient occlusion is the cheapest effect that adds a lot of quality
        // 环境光遮蔽是成本最低但能显著提升质量的效果
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
            enabled = true  // 启用环境光遮蔽
        }

        // NOTE: Try to disable post-processing (tone-mapping, etc.) to see the difference
        // 注意：尝试禁用后处理（色调映射等）来查看差异
        // view.isPostProcessingEnabled = false

        // Tell the view which camera we want to use
        // 告诉视图我们要使用哪个相机
        view.camera = camera

        // Tell the view which scene we want to render
        // 告诉视图我们要渲染哪个场景
        view.scene = scene
    }

    /**
     * 设置3D场景的所有内容
     * 包括材质、光照、网格模型等
     */
    private fun setupScene() {
        // 加载和设置材质
        loadMaterial()      // 从文件加载材质
        setupMaterial()     // 配置材质参数
        loadImageBasedLight()  // 加载基于图像的光照

        // 设置场景的天空盒和间接光照
        scene.skybox = ibl.skybox           // 天空盒提供背景环境
        scene.indirectLight = ibl.indirectLight  // 间接光照提供环境光

        // This map can contain named materials that will map to the material names
        // 这个映射包含命名材质，将映射到从filamesh文件加载的材质名称
        // loaded from the filamesh file. The material called "DefaultMaterial" is
        // 当找不到命名材质时，会应用名为"DefaultMaterial"的材质
        // applied when no named material can be found
        val materials = mapOf("DefaultMaterial" to materialInstance)

        // Load the mesh in the filamesh format (see filamesh tool)
        // 以filamesh格式加载网格（参见filamesh工具）
        mesh = loadMesh(assets, "models/shader_ball.filamesh", materials, engine)

        // Move the mesh down
        // 将网格向下移动
        // Filament uses column-major matrices
        // Filament使用列主序矩阵
        engine.transformManager.setTransform(
            engine.transformManager.getInstance(mesh.renderable),
            floatArrayOf(
                // 4x4变换矩阵（列主序）
                1.0f, 0.0f, 0.0f, 0.0f,  // 第一列：X轴方向
                0.0f, 1.0f, 0.0f, 0.0f,  // 第二列：Y轴方向
                0.0f, 0.0f, 1.0f, 0.0f,  // 第三列：Z轴方向
                0.0f, -1.2f, 0.0f, 1.0f   // 第四列：平移（Y轴向下1.2单位）
            )
        )

        // Add the entity to the scene to render it
        // 将实体添加到场景中进行渲染
        scene.addEntity(mesh.renderable)

        // We now need a light, let's create a directional light
        // 现在我们需要一个光源，让我们创建一个方向光
        light = EntityManager.get().create()

        // Create a color from a temperature (D65)
        // 从色温创建颜色（D65标准光源，约6500K）
        val (r, g, b) = Colors.cct(6_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(r, g, b)  // 设置光源颜色
            // Intensity of the sun in lux on a clear day
            // 晴天太阳的光照强度（以勒克斯为单位）
            .intensity(110_000.0f)
            // The direction is normalized on our behalf
            // 光照方向（会自动归一化）
            .direction(-0.753f, -1.0f, 0.890f)
            .castShadows(true)  // 启用阴影投射
            .build(engine, light)

        // Add the entity to the scene to light it
        // 将光源实体添加到场景中进行照明
        scene.addEntity(light)

        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // 设置相机曝光，此曝光遵循阳光f/16法则
        // Since we've defined a light that has the same intensity as the sun, it
        // 由于我们定义了与太阳相同强度的光源，
        // guarantees a proper exposure
        // 这保证了正确的曝光
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)  // 光圈f/16, 快门1/125s, ISO100

        // 开始相机旋转动画
        startAnimation()
    }

    /**
     * 从资源文件加载材质
     * 使用clear_coat.filamat文件创建清漆材质
     */
    private fun loadMaterial() {
        readUncompressedAsset("materials/clear_coat.filamat").let {
            material = Material.Builder().payload(it, it.remaining()).build(engine)
        }
    }

    /**
     * 设置材质参数
     * 创建材质实例并配置基础颜色
     */
    private fun setupMaterial() {
        // Create an instance of the material to set different parameters on it
        // 创建材质实例以在其上设置不同的参数
        materialInstance = material.createInstance()
        // Specify that our color is in sRGB so the conversion to linear
        // 指定我们的颜色在sRGB色彩空间中，这样到线性空间的转换
        // is done automatically for us. If you already have a linear color
        // 会自动为我们完成。如果您已经有线性颜色
        // you can pass it directly, or use Colors.RgbType.LINEAR
        // 可以直接传递，或使用Colors.RgbType.LINEAR
        materialInstance.setParameter(
            "baseColor",
            Colors.RgbType.SRGB,
            0.71f,
            0.0f,
            0.0f
        )  // 设置红色基础颜色
    }

    /**
     * 加载基于图像的光照(IBL)
     * 从环境贴图文件创建天空盒和间接光照
     */
    private fun loadImageBasedLight() {
        ibl = loadIbl(assets, "envs/flower_road_no_sun_2k", engine)  // 加载花路环境贴图
        ibl.indirectLight.intensity = 40_000.0f  // 设置间接光照强度
    }

    /**
     * 开始相机旋转动画
     * 让相机围绕场景中心做圆周运动
     */
    private fun startAnimation() {
        // Animate the triangle
        // 动画化三角形（实际上是动画化相机）
        animator.interpolator = LinearInterpolator()  // 使用线性插值器，保证匀速运动
        animator.duration = 18_000  // 动画持续18秒完成一圈
        animator.repeatMode = ValueAnimator.RESTART  // 重启模式，从头开始重复
        animator.repeatCount = ValueAnimator.INFINITE  // 无限重复
        animator.addUpdateListener { a ->
            val v = (a.animatedValue as Float)  // 获取当前动画值（0到2π）
            // 使用三角函数计算相机在圆周上的位置
            // 相机距离中心4.5单位，高度1.5，围绕Y轴旋转
            camera.lookAt(
                cos(v) * 4.5, 1.5, sin(v) * 4.5,  // 相机位置（圆周运动）
                0.0, 0.0, 0.0,  // 目标点（场景中心）
                0.0, 1.0, 0.0   // 上方向（Y轴向上）
            )
        }
        animator.start()  // 启动动画
    }

    /**
     * Activity恢复时的回调
     * 重新开始渲染和动画
     */
    override fun onResume() {
        super.onResume()
        // 重新注册帧回调，开始渲染循环
        choreographer.postFrameCallback(frameScheduler)
        // 重新启动动画
        animator.start()
    }

    /**
     * Activity暂停时的回调
     * 停止渲染和动画以节省资源
     */
    override fun onPause() {
        super.onPause()
        // 移除帧回调，停止渲染循环
        choreographer.removeFrameCallback(frameScheduler)
        // 取消动画
        animator.cancel()
    }

    /**
     * Activity销毁时的回调
     * 清理所有Filament资源，防止内存泄漏
     */
    override fun onDestroy() {
        super.onDestroy()

        // Stop the animation and any pending frame
        // 停止动画和任何待处理的帧
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel()

        // Always detach the surface before destroying the engine
        // 在销毁引擎之前始终分离表面
        uiHelper.detach()

        // Cleanup all resources
        // 清理所有资源（按照创建的相反顺序）
        destroyMesh(engine, mesh)                           // 销毁网格
        destroyIbl(engine, ibl)                             // 销毁IBL资源
        engine.destroyEntity(light)                         // 销毁光源实体
        engine.destroyRenderer(renderer)                    // 销毁渲染器
        engine.destroyMaterialInstance(materialInstance)    // 销毁材质实例
        engine.destroyMaterial(material)                    // 销毁材质
        engine.destroyView(view)                            // 销毁视图
        engine.destroyScene(scene)                          // 销毁场景
        engine.destroyCameraComponent(camera.entity)        // 销毁相机组件

        // Engine.destroyEntity() destroys Filament related resources only
        // Engine.destroyEntity()只销毁Filament相关资源
        // (components), not the entity itself
        // （组件），而不是实体本身
        val entityManager = EntityManager.get()
        entityManager.destroy(light)        // 销毁光源实体
        entityManager.destroy(camera.entity) // 销毁相机实体

        // Destroying the engine will free up any resource you may have forgotten
        // 销毁引擎将释放您可能忘记销毁的任何资源，
        // to destroy, but it's recommended to do the cleanup properly
        // 但建议正确地进行清理
        engine.destroy()
    }

    /**
     * 帧回调内部类
     * 负责每帧的渲染逻辑和帧调度
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
                // 如果beginFrame()返回false，您应该跳过该帧
                // This means you are sending frames too quickly to the GPU
                // 这意味着您向GPU发送帧的速度太快
                if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    renderer.render(view)  // 渲染视图
                    renderer.endFrame()    // 结束帧渲染
                }
            }
        }
    }

    /**
     * 表面回调内部类
     * 处理渲染表面的生命周期事件
     */
    inner class SurfaceCallback : UiHelper.RendererCallback {
        /**
         * 当原生窗口改变时调用
         * 重新创建交换链以适应新的表面
         */
        override fun onNativeWindowChanged(surface: Surface) {
            // 销毁旧的交换链（如果存在）
            swapChain?.let { engine.destroySwapChain(it) }
            // 为新表面创建交换链
            swapChain = engine.createSwapChain(surface)
            // 将显示辅助器附加到渲染器和显示器
            displayHelper.attach(renderer, surfaceView.display)
        }

        /**
         * 当从表面分离时调用
         * 清理交换链和显示资源
         */
        override fun onDetachedFromSurface() {
            // 分离显示辅助器
            displayHelper.detach()
            swapChain?.let {
                // 销毁交换链
                engine.destroySwapChain(it)
                // Required to ensure we don't return before Filament is done executing the
                // 需要确保在Filament完成执行
                // destroySwapChain command, otherwise Android might destroy the Surface
                // destroySwapChain命令之前我们不会返回，否则Android可能会过早销毁Surface
                // too early
                engine.flushAndWait()
                swapChain = null
            }
        }

        /**
         * 当表面尺寸改变时调用
         * 更新相机投影和视口以适应新尺寸
         */
        override fun onResized(width: Int, height: Int) {
            // 计算新的宽高比
            val aspect = width.toDouble() / height.toDouble()
            // 设置相机投影：45度视野角，新宽高比，近平面0.1，远平面20.0
            camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)

            // 设置视图的视口以匹配新尺寸
            view.viewport = Viewport(0, 0, width, height)

            // 同步待处理的帧以确保尺寸变化立即生效
            FilamentHelper.synchronizePendingFrames(engine)
        }
    }

    /**
     * 从Assets读取未压缩的资源文件
     * 将文件内容加载到ByteBuffer中
     * @param assetName 资源文件名
     * @return 包含文件内容的ByteBuffer
     */
    private fun readUncompressedAsset(assetName: String): ByteBuffer {
        assets.openFd(assetName).use { fd ->
            // 创建输入流
            val input = fd.createInputStream()
            // 分配与文件大小相同的缓冲区
            val dst = ByteBuffer.allocate(fd.length.toInt())

            // 使用NIO通道读取文件内容
            val src = Channels.newChannel(input)
            src.read(dst)  // 读取数据到缓冲区
            src.close()    // 关闭通道

            // 重置缓冲区位置并返回
            return dst.apply { rewind() }
        }
    }
}
