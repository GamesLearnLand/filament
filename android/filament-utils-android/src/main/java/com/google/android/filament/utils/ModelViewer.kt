/*
 * 版权所有 (C) 2020 The Android Open Source Project
 *
 * 根据Apache许可证2.0版本授权，许可信息可在http://www.apache.org/licenses/LICENSE-2.0获取
 *
 * 除非适用法律要求或书面同意，按"原样"分发，不附带任何担保或条件声明
 * 详见许可证的具体语言权限和限制
 */

package com.google.android.filament.utils

import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import com.google.android.filament.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.*
import kotlinx.coroutines.*
import java.nio.Buffer

/**
 * 模型查看器类（ModelViewer）
 *
 * 主要功能：
 * - 管理Filament引擎核心组件（引擎/渲染器/视图/场景）
 * - 支持加载和渲染glTF/GLB格式3D模型
 * - 提供基于触摸的相机控制（轨道/缩放/平移）
 * - 自动适配SurfaceView/TextureView渲染目标
 *
 * 核心特性：
 * - 自动创建默认光源和相机系统
 * - 支持PBR物理材质渲染
 * - 内置动画系统支持
 * - 响应式窗口尺寸适配
 */
class ModelViewer(
        val engine: Engine,
        private val uiHelper: UiHelper
) : android.view.View.OnTouchListener {
    /**
     * 当前加载的模型资源对象
     * 可通过asset.entities访问场景实体
     */
    var asset: FilamentAsset? = null
        private set

    /**
     * 动画控制器
     * 用于驱动模型骨骼动画播放
     */
    var animator: Animator? = null
        private set

    /**
     * 资源加载进度百分比
     * 用于监控模型加载状态
     */
    @Suppress("unused")
    val progress
        get() = resourceLoader.asyncGetLoadProgress()

    /**
     * 是否归一化骨骼权重
     * 影响骨骼动画计算方式
     */
    var normalizeSkinningWeights = true

    /**
     * 相机焦距设置（单位：毫米）
     * 默认28mm，修改后自动更新投影矩阵
     */
    var cameraFocalLength = 28f
        set(value) {
            field = value
            updateCameraProjection()
        }

    /**
     * 相机近裁剪面距离（单位：米）
     * 默认0.05米，修改后自动更新投影矩阵
     */
    var cameraNear = kNearPlane
        set(value) {
            field = value
            updateCameraProjection()
        }

    /**
     * 相机远裁剪面距离（单位：米）
     * 默认1000米，修改后自动更新投影矩阵
     */
    var cameraFar = kFarPlane
        set(value) {
            field = value
            updateCameraProjection()
        }

    /**
     * 场景对象
     * 包含所有渲染实体
     */
    val scene: Scene

    /**
     * 视图对象
     * 定义渲染输出参数
     */
    val view: View

    /**
     * 相机对象
     * 定义视角和投影矩阵
     */
    val camera: Camera

    /**
     * 渲染器对象
     * 执行实际渲染操作
     */
    val renderer: Renderer

    /**
     * 环境光源实体
     * 用于模拟全局光照
     */
    @Entity val light: Int

    /**
     * 环境光照立方体贴图
     * 用于间接光照计算
     */
    var indirectLightCubemap: Texture? = null

    /**
     * 天空盒立方体贴图
     * 用于背景渲染
     */
    var skyboxCubemap: Texture? = null

    /**
     * 显示辅助对象
     * 用于处理SurfaceView/TextureView生命周期
     */
    private lateinit var displayHelper: DisplayHelper

    /**
     * 相机控制器
     * 处理触摸输入以控制相机视角
     */
    private lateinit var cameraManipulator: Manipulator

    /**
     * 触摸事件检测器
     * 用于处理触摸事件分发
     */
    private lateinit var gestureDetector: GestureDetector

    /**
     * 渲染目标SurfaceView
     * 用于OpenGL渲染输出
     */
    private var surfaceView: SurfaceView? = null

    /**
     * 渲染目标TextureView
     * 用于OpenGL渲染输出
     */
    private var textureView: TextureView? = null

    /**
     * 资源加载协程任务
     * 用于异步加载外部资源
     */
    private var fetchResourcesJob: Job? = null

    /**
     * 交换链对象
     * 用于管理渲染输出缓冲区
     */
    private var swapChain: SwapChain? = null

    /**
     * 资产加载器
     * 用于加载glTF/GLB模型资源
     */
    private var assetLoader: AssetLoader

    /**
     * 材质提供器
     * 用于创建和管理材质对象
     */
    private var materialProvider: MaterialProvider

    /**
     * 资源加载器
     * 用于异步加载外部资源
     */
    private var resourceLoader: ResourceLoader

    /**
     * 临时数组
     * 用于存储准备好的渲染实体
     */
    private val readyRenderables = IntArray(128) // add up to 128 entities at a time

    /**
     * 相机位置
     * 用于更新相机视角矩阵
     */
    private val eyePos = DoubleArray(3)

    /**
     * 相机目标点
     * 用于更新相机视角矩阵
     */
    private val target = DoubleArray(3)

    /**
     * 相机上方向量
     * 用于更新相机视角矩阵
     */
    private val upward = DoubleArray(3)

    init {
        renderer = engine.createRenderer()
        scene = engine.createScene()
        camera = engine.createCamera(engine.entityManager.create()).apply { setExposure(kAperture, kShutterSpeed, kSensitivity) }
        view = engine.createView()
        view.scene = scene
        view.camera = camera

        materialProvider = UbershaderProvider(engine)
        assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
        resourceLoader = ResourceLoader(engine, normalizeSkinningWeights)

        // Always add a direct light source since it is required for shadowing.
        // We highly recommend adding an indirect light as well.

        light = EntityManager.get().create()

        val (r, g, b) = Colors.cct(6_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b)
                .intensity(100_000.0f)
                .direction(0.0f, -1.0f, 0.0f)
                .castShadows(true)
                .build(engine, light)

        scene.addEntity(light)
    }

    /**
     * 构造函数（SurfaceView版本）
     * 
     * 参数说明：
     * @param surfaceView 渲染目标SurfaceView
     * @param engine Filament引擎实例（默认自动创建）
     * @param uiHelper UI助手（默认创建时不检查上下文错误）
     * @param manipulator 相机控制器（null时创建默认轨道控制器）
     */
    constructor(
            surfaceView: SurfaceView,
            engine: Engine = Engine.create(),
            uiHelper: UiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK),
            manipulator: Manipulator? = null
    ) : this(engine, uiHelper) {
        cameraManipulator = manipulator ?: Manipulator.Builder()
                .targetPosition(kDefaultObjectPosition.x, kDefaultObjectPosition.y, kDefaultObjectPosition.z)
                .viewport(surfaceView.width, surfaceView.height)
                .build(Manipulator.Mode.ORBIT)

        this.surfaceView = surfaceView
        gestureDetector = GestureDetector(surfaceView, cameraManipulator)
        displayHelper = DisplayHelper(surfaceView.context)
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(surfaceView)
        addDetachListener(surfaceView)
    }

    /**
     * 构造函数（TextureView版本）
     * 
     * 参数说明：
     * @param textureView 渲染目标TextureView
     * @param engine Filament引擎实例（默认自动创建）
     * @param uiHelper UI助手（默认创建时不检查上下文错误）
     * @param manipulator 相机控制器（null时创建默认轨道控制器）
     */
    @Suppress("unused")
    constructor(
            textureView: TextureView,
            engine: Engine = Engine.create(),
            uiHelper: UiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK),
            manipulator: Manipulator? = null
    ) : this(engine, uiHelper) {
        cameraManipulator = manipulator ?: Manipulator.Builder()
                .targetPosition(kDefaultObjectPosition.x, kDefaultObjectPosition.y, kDefaultObjectPosition.z)
                .viewport(textureView.width, textureView.height)
                .build(Manipulator.Mode.ORBIT)

        this.textureView = textureView
        gestureDetector = GestureDetector(textureView, cameraManipulator)
        displayHelper = DisplayHelper(textureView.context)
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(textureView)
        addDetachListener(textureView)
    }

    /**
     * 同步加载GLB格式模型
     * 
     * 参数说明：
     * @param buffer 包含GLB文件数据的缓冲区
     * 
     * 功能流程：
     * 1. 销毁现有模型资源
     * 2. 创建Filament资产对象
     * 3. 异步加载资源数据
     * 4. 初始化动画控制器
     * 5. 释放源数据内存
     */
    fun loadModelGlb(buffer: Buffer) {
        destroyModel()
        asset = assetLoader.createAsset(buffer)
        asset?.let { asset ->
            resourceLoader.asyncBeginLoad(asset)
            animator = asset.instance.animator
            asset.releaseSourceData()
        }
    }

    /**
     * 异步加载glTF格式模型
     * 
     * 参数说明：
     * @param buffer 包含glTF文件数据的缓冲区
     * @param callback 外部资源加载回调
     * 
     * 功能流程：
     * 1. 销毁现有模型资源
     * 2. 创建Filament资产对象
     * 3. 遍历资源URI并调用回调加载数据
     * 4. 异步加载资源数据
     * 5. 初始化动画控制器
     * 6. 释放源数据内存
     */
    fun loadModelGltf(buffer: Buffer, callback: (String) -> Buffer?) {
        destroyModel()
        asset = assetLoader.createAsset(buffer)
        asset?.let { asset ->
            for (uri in asset.resourceUris) {
                val resourceBuffer = callback(uri)
                if (resourceBuffer == null) {
                    this.asset = null
                    return
                }
                resourceLoader.addResourceData(uri, resourceBuffer)
            }
            resourceLoader.asyncBeginLoad(asset)
            animator = asset.instance.animator
            asset.releaseSourceData()
        }
    }

    /**
     * 异步加载glTF格式模型（协程版本）
     * 
     * 参数说明：
     * @param buffer 包含glTF文件数据的缓冲区
     * @param callback 外部资源加载回调
     * 
     * 功能流程：
     * 1. 销毁现有模型资源
     * 2. 创建Filament资产对象
     * 3. 启动协程加载资源数据
     * 4. 初始化动画控制器
     * 5. 释放源数据内存
     */
    fun loadModelGltfAsync(buffer: Buffer, callback: (String) -> Buffer) {
        destroyModel()
        asset = assetLoader.createAsset(buffer)
        fetchResourcesJob = CoroutineScope(Dispatchers.IO).launch {
            fetchResources(asset!!, callback)
        }
    }

    /**
     * 世界坐标系转换
     * 
     * 将模型变换到单位立方体空间
     * 
     * 参数说明：
     * @param centerPoint 目标中心点坐标（默认0,0,-4）
     * 
     * 计算流程：
     * 1. 获取模型包围盒信息
     * 2. 计算缩放系数（基于最大包围盒尺寸）
     * 3. 计算变换矩阵（缩放+平移）
     * 4. 应用变换到模型根节点
     */
    fun transformToUnitCube(centerPoint: Float3 = kDefaultObjectPosition) {
        asset?.let { asset ->
            val tm = engine.transformManager
            var center = asset.boundingBox.center.let { v -> Float3(v[0], v[1], v[2]) }
            val halfExtent = asset.boundingBox.halfExtent.let { v -> Float3(v[0], v[1], v[2]) }
            val maxExtent = 2.0f * max(halfExtent)
            val scaleFactor = 2.0f / maxExtent
            center -= centerPoint / scaleFactor
            val transform = scale(Float3(scaleFactor)) * translation(-center)
            tm.setTransform(tm.getInstance(asset.root), transpose(transform).toFloatArray())
        }
    }

    /**
     * 清除根节点变换
     * 
     * 将模型根节点变换重置为单位矩阵
     */
    fun clearRootTransform() {
        asset?.let {
            val tm = engine.transformManager
            tm.setTransform(tm.getInstance(it.root), Mat4().toFloatArray())
        }
    }

    /**
     * 销毁当前模型资源
     * 
     * 释放所有相关资源和内存
     */
    fun destroyModel() {
        fetchResourcesJob?.cancel()
        resourceLoader.asyncCancelLoad()
        resourceLoader.evictResourceData()
        asset?.let { asset ->
            this.scene.removeEntities(asset.entities)
            assetLoader.destroyAsset(asset)
            this.asset = null
            this.animator = null
        }
    }

    /**
     * 渲染帧处理
     * 
     * 参数说明：
     * @param frameTimeNanos 帧开始时间（纳秒级）
     * 
     * 执行流程：
     * 1. 检查渲染准备状态
     * 2. 更新异步加载资源
     * 3. 构建场景渲染队列
     * 4. 更新相机视角矩阵
     * 5. 执行渲染帧
     */
    fun render(frameTimeNanos: Long) {
        if (!uiHelper.isReadyToRender) {
            return
        }

        // Allow the resource loader to finalize textures that have become ready.
        resourceLoader.asyncUpdateLoad()

        // Add renderable entities to the scene as they become ready.
        asset?.let { populateScene(it) }

        // Extract the camera basis from the helper and push it to the Filament camera.
        cameraManipulator.getLookAt(eyePos, target, upward)
        camera.lookAt(
                eyePos[0], eyePos[1], eyePos[2],
                target[0], target[1], target[2],
                upward[0], upward[1], upward[2])

        // Render the scene, unless the renderer wants to skip the frame.
        if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    /**
     * 构建场景渲染队列
     * 
     * 参数说明：
     * @param asset Filament资产对象
     * 
     * 功能流程：
     * 1. 遍历准备好的渲染实体
     * 2. 设置屏幕空间接触阴影
     * 3. 添加实体到场景
     * 4. 添加光源实体到场景
     */
    private fun populateScene(asset: FilamentAsset) {
        val rcm = engine.renderableManager
        var count = 0
        val popRenderables = { count = asset.popRenderables(readyRenderables); count != 0 }
        while (popRenderables()) {
            for (i in 0 until count) {
                val ri = rcm.getInstance(readyRenderables[i])
                rcm.setScreenSpaceContactShadows(ri, true)
            }
            scene.addEntities(readyRenderables.take(count).toIntArray())
        }
        scene.addEntities(asset.lightEntities)
    }

    /**
     * 添加视图生命周期监听器
     * 
     * 参数说明：
     * @param view 目标视图对象
     * 
     * 功能流程：
     * 1. 监听视图附加/分离事件
     */
    private fun addDetachListener(view: android.view.View) {
        view.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: android.view.View) {
                uiHelper.detach()
            }

            override fun onViewAttachedToWindow(v: android.view.View) {
                // no-op
            }
        })
    }

    /**
     * 触摸事件处理
     * 
     * 参数说明：
     * @param v 触摸事件目标视图
     * @param event 触摸事件对象
     * 
     * 功能流程：
     * 1. 传递触摸事件到手势检测器
     * 2. 返回事件处理结果
     */
    override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    /**
     * 相机投影矩阵更新
     * 
     * 基于当前视口尺寸和相机参数重新计算投影矩阵
     * 用于处理屏幕旋转或尺寸变化
     */
    private fun updateCameraProjection() {
        camera.setProjection(cameraFocalLength, cameraNear, cameraFar, cameraManipulator.viewportWidth, cameraManipulator.viewportHeight)
    }

    companion object {
        /**
         * 默认模型位置常量
         * 初始放置在Z轴-4单位处
         */
        private val kDefaultObjectPosition = Float3(0.0f, 0.0f, -4.0f)
    }
}