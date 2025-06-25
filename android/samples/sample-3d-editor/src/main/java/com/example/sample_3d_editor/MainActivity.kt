package com.example.sample_3d_editor

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.opengl.Matrix
import android.os.Bundle
import android.view.Choreographer
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout

import com.google.android.filament.*
import com.google.android.filament.RenderableManager.*
import com.google.android.filament.VertexBuffer.*
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import kotlin.math.*

// MainActivity 是 Filament 3D 编辑器的主界面，负责初始化 UI、Filament 渲染环境、摄像机与场景，并处理用户交互。
// Filament 是一个实时渲染引擎，提供高效的图形渲染能力。
// 业务流程包括：
// 1. 初始化 UI 组件，包括 SurfaceView 和 TextView。
// 2. 设置 SurfaceView 以显示 3D 内容，并添加触摸事件监听器以控制摄像机。
// 3. 初始化 Filament 引擎及其相关组件，如渲染器、场景、视图和摄像机。
// 4. 创建几何体（立方体和坐标轴）并将其添加到场景中。
// 5. 设置光照以增强视觉效果。
// 6. 通过触摸事件处理用户交互，允许用户旋转视图和切换视角。
// Filament 用法：
// - 使用 Engine.create() 创建渲染引擎。
// - 使用 Renderer 渲染场景。
// - 使用 Scene 和 View 管理 3D 场景和视图。
// - 使用 Camera 设置摄像机参数和视角。
// - 使用 VertexBuffer 和 IndexBuffer 创建几何体。
// - 使用 RenderableManager 管理可渲染实体。
// - 使用 LightManager 设置光照参数。
class MainActivity : Activity() {
    // 静态代码块，确保在使用 Filament API 前完成初始化。
    // 这是 Filament 的强制要求，必须在使用任何 Filament 功能之前调用。
    companion object {
        init {
            Filament.init() // 初始化 Filament 库
        }
    }

    // UI 组件
    private lateinit var surfaceView: SurfaceView // 用于显示 3D 内容的 SurfaceView，是 Filament 渲染的目标
    private lateinit var infoText: TextView // 用于显示提示信息的文本框
    private lateinit var rootLayout: ConstraintLayout // 界面的根布局

    // Filament 相关组件
    private lateinit var uiHelper: UiHelper // 辅助管理 SurfaceView 的生命周期，处理与 Android UI 的集成
    private lateinit var displayHelper: DisplayHelper // 处理显示相关的事件，如屏幕方向和分辨率变化
    private lateinit var choreographer: Choreographer // 用于同步渲染帧，确保动画和渲染的平滑性
    private lateinit var engine: Engine // Filament 渲染引擎，是所有 Filament 操作的核心
    private lateinit var renderer: Renderer // 渲染器，负责将场景渲染到 SurfaceView
    private lateinit var scene: Scene // 场景对象，包含所有需要渲染的实体，如模型、光源等
    private lateinit var view: View // 视图对象，定义了场景的观察方式，包括摄像机和视口
    private lateinit var camera: Camera // 摄像机，定义了观察场景的视角和投影

    // 材质（Materials）
    // 这些材质定义了物体的外观，但在此示例中被注释掉了。
    // private lateinit var litMaterial: Material // 受光照影响的材质
    // private lateinit var unlitMaterial: Material // 不受光照影响的材质
    // private lateinit var cubeMaterialInstance: MaterialInstance // 立方体的材质实例
    // private lateinit var axisMaterialInstance: MaterialInstance // 坐标轴的材质实例

    // 几何体（Geometry）
    private lateinit var cubeVertexBuffer: VertexBuffer // 存储立方体顶点数据的缓冲区
    private lateinit var cubeIndexBuffer: IndexBuffer // 存储立方体索引数据的缓冲区
    private lateinit var axisVertexBuffer: VertexBuffer // 存储坐标轴顶点数据的缓冲区
    private lateinit var axisIndexBuffer: IndexBuffer // 存储坐标轴索引数据的缓冲区

    // 实体（Entities）
    // 实体是场景中的基本对象，通过关联组件（如 Renderable、Transform）来定义其行为和外观。
    @Entity private var cubeRenderable = 0 // 立方体的可渲染实体
    @Entity private var axisRenderable = 0 // 坐标轴的可渲染实体
    @Entity private var light = 0 // 光源实体

    private var swapChain: SwapChain? = null // 用于将渲染结果呈现到屏幕的交换链
    private val frameScheduler = FrameCallback() // 帧回调，用于在每一帧触发渲染
    private val animator = ValueAnimator.ofFloat(0.0f, 360.0f) // 用于动画的值动画器

    // 摄像机控制参数
    private var cameraDistance = 8.0f // 摄像机与目标的距离
    private var cameraAngleX = 30.0f // 摄像机的水平旋转角度
    private var cameraAngleY = 45.0f // 摄像机的垂直旋转角度
    private val cameraMatrix = FloatArray(16) // 摄像机的变换矩阵
    private val viewMatrix = FloatArray(16) // 视图矩阵

    // Activity 的 onCreate 方法，是应用的入口点。
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 UI
        setupUI()
        
        // 获取 Choreographer 实例，用于同步渲染循环
        choreographer = Choreographer.getInstance()
        // 初始化 DisplayHelper，用于处理屏幕相关的事件
        displayHelper = DisplayHelper(this)

        // 设置 SurfaceView，将其与 Filament 集成
        setupSurfaceView()
        // 初始化 Filament 核心组件
        setupFilament()
        // 设置视图和摄像机
        setupView()
        // 设置场景，包括创建物体和光源
        setupScene()
    }

    // 初始化 UI 组件
    private fun setupUI() {
        // 创建根布局
        rootLayout = ConstraintLayout(this)
        rootLayout.setBackgroundColor(0xFF000000.toInt()) // 设置背景为黑色
        
        // 创建 SurfaceView 用于 3D 渲染
        surfaceView = SurfaceView(this)
        val surfaceParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        surfaceView.layoutParams = surfaceParams
        rootLayout.addView(surfaceView)
        
        // 创建 TextView 用于显示提示信息
//        infoText = TextView(this)
//        infoText.text = "3D坐标轴演示\n点击坐标轴改变视角\n拖拽旋转视图"
//        infoText.setTextColor(0xFFFFFFFF.toInt()) // 设置文字颜色为白色
//        infoText.textSize = 12f
//        infoText.setPadding(24, 24, 24, 24)
//        infoText.setBackgroundColor(0x80000000.toInt()) // 设置半透明背景
//
//        // 设置 TextView 的布局参数
//        val textParams = ConstraintLayout.LayoutParams(
//            ConstraintLayout.LayoutParams.WRAP_CONTENT,
//            ConstraintLayout.LayoutParams.WRAP_CONTENT
//        )
//        textParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
//        textParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
//        textParams.setMargins(48, 48, 0, 0)
//        infoText.layoutParams = textParams
//        rootLayout.addView(infoText)
        
        // 将根布局设置为 Activity 的内容视图
        setContentView(rootLayout)
    }

    // 设置 SurfaceView，将其与 Filament 的 UiHelper 集成
    @SuppressLint("ClickableViewAccessibility")
    private fun setupSurfaceView() {
        // 初始化 UiHelper，用于处理 SurfaceView 的生命周期和渲染回调
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(surfaceView)
        
        // 添加触摸事件监听器，用于控制摄像机
        surfaceView.setOnTouchListener { _, event ->
            handleTouch(event)
        }
    }

    // 初始化 Filament 的核心组件
    private fun setupFilament() {
        // 创建 Filament 引擎
        engine = Engine.create()
        // 创建渲染器
        renderer = engine.createRenderer()
        // 创建场景
        scene = engine.createScene()
        // 创建视图
        view = engine.createView()
        // 创建摄像机
        camera = engine.createCamera(engine.entityManager.create())
    }

    // 设置视图和摄像机
    private fun setupView() {
        // 设置场景的天空盒，提供背景颜色
        scene.skybox = Skybox.Builder().color(0.1f, 0.1f, 0.1f, 1.0f).build(engine)
        // 将摄像机与视图关联
        view.camera = camera
        // 将场景与视图关联
        view.scene = scene
        // 更新摄像机的位置和朝向
        updateCamera()
    }

    // 设置场景，包括创建物体、材质和光源
    private fun setupScene() {
        // 在此示例中，材质加载和设置被注释掉了
        // loadMaterials()
        // setupMaterials()
        
        // 创建立方体的网格数据
        createCubeMesh()
        // 创建坐标轴的网格数据
        createAxisMesh()
        // 创建可渲染实体并将其添加到场景中
        createRenderables()
        // 设置场景光照
        setupLighting()
    }

    // 加载材质文件
    private fun loadMaterials() {
        // 从 assets 中加载 lit.filamat 文件作为受光照的材质
        // readUncompressedAsset("materials/lit.filamat").let {
        //     litMaterial = Material.Builder().payload(it, it.remaining()).build(engine)
        // }
        //
        // // 从 assets 中加载 unlit.filamat 文件作为不受光照的材质
        // readUncompressedAsset("materials/unlit.filamat").let {
        //     unlitMaterial = Material.Builder().payload(it, it.remaining()).build(engine)
        // }
    }

    // 设置材质属性
    // private fun setupMaterials() {
    //     // 设置立方体的材质实例，定义其颜色、金属度和粗糙度
    //     // cubeMaterialInstance = litMaterial.createInstance()
    //     // cubeMaterialInstance.setParameter("baseColor", Colors.RgbType.SRGB, 0.2f, 0.5f, 1.0f)
    //     // cubeMaterialInstance.setParameter("metallic", 0.8f)
    //     // cubeMaterialInstance.setParameter("roughness", 0.2f)
    //     //
    //     // // 设置坐标轴的材质实例
    //     // axisMaterialInstance = unlitMaterial.createInstance()
    // }

    // 创建立方体的网格数据，包括顶点和索引
    private fun createCubeMesh() {
        val floatSize = 4 // Float 类型占用的字节数
        val vertexSize = 3 * floatSize + 4 * floatSize // 每个顶点的大小（位置 + 切线）
        val vertexCount = 24 // 立方体有 6 个面，每个面 4 个顶点

        // 定义顶点数据结构
        data class Vertex(val x: Float, val y: Float, val z: Float, val tangents: FloatArray)
        // 扩展 ByteBuffer 以方便地添加顶点数据
        fun ByteBuffer.put(v: Vertex): ByteBuffer {
            putFloat(v.x)
            putFloat(v.y)
            putFloat(v.z)
            v.tangents.forEach { putFloat(it) }
            return this
        }

        // 为每个面创建切线帧，用于光照计算
        val tfPX = FloatArray(4)
        val tfNX = FloatArray(4)
        val tfPY = FloatArray(4)
        val tfNY = FloatArray(4)
        val tfPZ = FloatArray(4)
        val tfNZ = FloatArray(4)

        // 使用 MathUtils.packTangentFrame 计算切线帧
        MathUtils.packTangentFrame( 0.0f,  1.0f, 0.0f, 0.0f, 0.0f, -1.0f,  1.0f,  0.0f,  0.0f, tfPX)
        MathUtils.packTangentFrame( 0.0f,  1.0f, 0.0f, 0.0f, 0.0f, -1.0f, -1.0f,  0.0f,  0.0f, tfNX)
        MathUtils.packTangentFrame(-1.0f,  0.0f, 0.0f, 0.0f, 0.0f, -1.0f,  0.0f,  1.0f,  0.0f, tfPY)
        MathUtils.packTangentFrame(-1.0f,  0.0f, 0.0f, 0.0f, 0.0f,  1.0f,  0.0f, -1.0f,  0.0f, tfNY)
        MathUtils.packTangentFrame( 0.0f,  1.0f, 0.0f, 1.0f, 0.0f,  0.0f,  0.0f,  0.0f,  1.0f, tfPZ)
        MathUtils.packTangentFrame( 0.0f, -1.0f, 0.0f, 1.0f, 0.0f,  0.0f,  0.0f,  0.0f, -1.0f, tfNZ)

        // 分配 ByteBuffer 并填充顶点数据
        val vertexData = ByteBuffer.allocate(vertexCount * vertexSize)
                .order(ByteOrder.nativeOrder()) // 使用本地字节序
                // -Z 面
                .put(Vertex(-1.0f, -1.0f, -1.0f, tfNZ))
                .put(Vertex(-1.0f,  1.0f, -1.0f, tfNZ))
                .put(Vertex( 1.0f,  1.0f, -1.0f, tfNZ))
                .put(Vertex( 1.0f, -1.0f, -1.0f, tfNZ))
                // +X 面
                .put(Vertex( 1.0f, -1.0f, -1.0f, tfPX))
                .put(Vertex( 1.0f,  1.0f, -1.0f, tfPX))
                .put(Vertex( 1.0f,  1.0f,  1.0f, tfPX))
                .put(Vertex( 1.0f, -1.0f,  1.0f, tfPX))
                // +Z 面
                .put(Vertex(-1.0f, -1.0f,  1.0f, tfPZ))
                .put(Vertex( 1.0f, -1.0f,  1.0f, tfPZ))
                .put(Vertex( 1.0f,  1.0f,  1.0f, tfPZ))
                .put(Vertex(-1.0f,  1.0f,  1.0f, tfPZ))
                // -X 面
                .put(Vertex(-1.0f, -1.0f,  1.0f, tfNX))
                .put(Vertex(-1.0f,  1.0f,  1.0f, tfNX))
                .put(Vertex(-1.0f,  1.0f, -1.0f, tfNX))
                .put(Vertex(-1.0f, -1.0f, -1.0f, tfNX))
                // -Y 面
                .put(Vertex(-1.0f, -1.0f,  1.0f, tfNY))
                .put(Vertex(-1.0f, -1.0f, -1.0f, tfNY))
                .put(Vertex( 1.0f, -1.0f, -1.0f, tfNY))
                .put(Vertex( 1.0f, -1.0f,  1.0f, tfNY))
                // +Y 面
                .put(Vertex(-1.0f,  1.0f, -1.0f, tfPY))
                .put(Vertex(-1.0f,  1.0f,  1.0f, tfPY))
                .put(Vertex( 1.0f,  1.0f,  1.0f, tfPY))
                .put(Vertex( 1.0f,  1.0f, -1.0f, tfPY))
                .flip() // 重置缓冲区的位置

        // 创建 VertexBuffer，定义顶点属性
        cubeVertexBuffer = VertexBuffer.Builder()
                .bufferCount(1) // 使用一个缓冲区
                .vertexCount(vertexCount) // 顶点数量
                .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0, vertexSize) // 位置属性
                .attribute(VertexAttribute.TANGENTS, 0, AttributeType.FLOAT4, 3 * floatSize, vertexSize) // 切线属性
                .build(engine)
        // 将顶点数据设置到 VertexBuffer
        cubeVertexBuffer.setBufferAt(engine, 0, vertexData)

        // 创建立方体的索引数据
        val shortSize = 2 // Short 类型占用的字节数
        val indexData = ByteBuffer.allocate(6 * 2 * 3 * shortSize) // 6 个面，每个面 2 个三角形，每个三角形 3 个顶点
                .order(ByteOrder.nativeOrder())
        repeat(6) { // 为每个面生成索引
            val i = (it * 4).toShort()
            indexData
                    .putShort(i).putShort((i + 1).toShort()).putShort((i + 2).toShort())
                    .putShort(i).putShort((i + 2).toShort()).putShort((i + 3).toShort())
        }
        indexData.flip()

        // 创建 IndexBuffer
        cubeIndexBuffer = IndexBuffer.Builder()
                .indexCount(36) // 索引数量
                .bufferType(IndexBuffer.Builder.IndexType.USHORT) // 索引类型
                .build(engine)
        // 将索引数据设置到 IndexBuffer
        cubeIndexBuffer.setBuffer(engine, indexData)
    }

    // 创建坐标轴的网格数据
    private fun createAxisMesh() {
        val floatSize = 4
        val vertexSize = 3 * floatSize + 4 * floatSize // 每个顶点的大小（位置 + 颜色）
        
        // 坐标轴顶点：原点 + 3 个轴端点
        val axisLength = 3.0f
        
        // 定义带颜色的顶点数据结构
        data class ColorVertex(val x: Float, val y: Float, val z: Float, val r: Float, val g: Float, val b: Float, val a: Float)
        fun ByteBuffer.put(v: ColorVertex): ByteBuffer {
            putFloat(v.x)
            putFloat(v.y)
            putFloat(v.z)
            putFloat(v.r)
            putFloat(v.g)
            putFloat(v.b)
            putFloat(v.a)
            return this
        }

        // 填充坐标轴的顶点数据
        val axisVertexData = ByteBuffer.allocate(6 * vertexSize)
                .order(ByteOrder.nativeOrder())
                // X 轴 (红色)
                .put(ColorVertex(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f))
                .put(ColorVertex(axisLength, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f))
                // Y 轴 (绿色)
                .put(ColorVertex(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f))
                .put(ColorVertex(0.0f, axisLength, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f))
                // Z 轴 (蓝色)
                .put(ColorVertex(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f))
                .put(ColorVertex(0.0f, 0.0f, axisLength, 0.0f, 0.0f, 1.0f, 1.0f))
                .flip()

        // 创建坐标轴的 VertexBuffer
        axisVertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)
                .vertexCount(6)
                .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0, vertexSize) // 位置属性
                .attribute(VertexAttribute.COLOR, 0, AttributeType.FLOAT4, 3 * floatSize, vertexSize) // 颜色属性
                .build(engine)
        axisVertexBuffer.setBufferAt(engine, 0, axisVertexData)

        // 创建坐标轴的索引数据 (3 条线)
        val indexData = ByteBuffer.allocate(6 * 2)
                .order(ByteOrder.nativeOrder())
                .putShort(0).putShort(1) // X 轴
                .putShort(2).putShort(3) // Y 轴
                .putShort(4).putShort(5) // Z 轴
                .flip()

        // 创建坐标轴的 IndexBuffer
        axisIndexBuffer = IndexBuffer.Builder()
                .indexCount(6)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                .build(engine)
        axisIndexBuffer.setBuffer(engine, indexData)
    }

    // 创建可渲染实体并将其添加到场景中
    private fun createRenderables() {
        // 创建立方体的可渲染实体
        cubeRenderable = EntityManager.get().create()
        RenderableManager.Builder(1)
                .boundingBox(Box(-1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f)) // 设置包围盒，用于剔除
                .geometry(0, PrimitiveType.TRIANGLES, cubeVertexBuffer, cubeIndexBuffer, 0, 36) // 关联几何体
                // .material(0, cubeMaterialInstance) // 关联材质（已注释）
                .build(engine, cubeRenderable)
        scene.addEntity(cubeRenderable) // 将实体添加到场景

        // 创建坐标轴的可渲染实体
        axisRenderable = EntityManager.get().create()
        RenderableManager.Builder(1)
                .boundingBox(Box(-3.0f, -3.0f, -3.0f, 3.0f, 3.0f, 3.0f))
                .geometry(0, PrimitiveType.LINES, axisVertexBuffer, axisIndexBuffer, 0, 6) // 关联几何体
                // .material(0, axisMaterialInstance) // 关联材质（已注释）
                .build(engine, axisRenderable)
        scene.addEntity(axisRenderable)
    }

    // 设置场景光照
    private fun setupLighting() {
        // 创建光源实体
        light = EntityManager.get().create()
        // 将色温转换为 RGB 颜色
        val (r, g, b) = Colors.cct(6_500.0f)
        // 创建平行光
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b) // 设置颜色
                .intensity(120_000.0f) // 设置强度
                .direction(-0.5f, -1.0f, -0.5f) // 设置方向
                .castShadows(true) // 开启阴影
                .build(engine, light)
        // 将光源添加到场景
        scene.addEntity(light)
        
        // 设置摄像机曝光
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)
    }

    // 更新摄像机的位置和朝向
    private fun updateCamera() {
        // 将角度转换为弧度
        val radX = Math.toRadians(cameraAngleX.toDouble())
        val radY = Math.toRadians(cameraAngleY.toDouble())
        
        // 根据距离和角度计算摄像机在球坐标系中的位置
        val x = (cameraDistance * cos(radX) * cos(radY)).toFloat()
        val y = (cameraDistance * sin(radX)).toFloat()
        val z = (cameraDistance * cos(radX) * sin(radY)).toFloat()
        
        // 设置摄像机的位置、目标和上方向
        camera.lookAt(x.toDouble(), y.toDouble(), z.toDouble(), 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
    }

    // 触摸事件处理
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false

    // 处理触摸事件，用于旋转摄像机
    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            // 手指按下时，记录初始位置并开始拖动
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                isDragging = true
                
                // 检查是否点击了坐标轴端点以切换视角
                checkAxisClick(event.x, event.y)
                return true
            }
            // 手指移动时，根据移动距离更新摄像机角度
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaX = event.x - lastX
                    val deltaY = event.y - lastY
                    
                    // 更新摄像机角度
                    cameraAngleY += deltaX * 0.5f
                    cameraAngleX -= deltaY * 0.5f
                    
                    // 限制垂直角度范围，防止摄像机翻转
                    cameraAngleX = cameraAngleX.coerceIn(-89f, 89f)
                    
                    // 更新摄像机
                    updateCamera()
                    
                    // 更新最后位置
                    lastX = event.x
                    lastY = event.y
                }
                return true
            }
            // 手指抬起时，停止拖动
            MotionEvent.ACTION_UP -> {
                isDragging = false
                return true
            }
        }
        return false
    }

    // 检查是否点击了坐标轴以切换视角
    private fun checkAxisClick(x: Float, y: Float) {
        // 简单的点击检测，用于切换坐标轴视角
        val centerX = surfaceView.width / 2f
        val centerY = surfaceView.height / 2f
        
        // 检查点击是否在屏幕下半部分（坐标轴大致位置）
        if (y > centerY + 100) {
            when {
                x < centerX - 50 -> switchToView(-90f, 0f) // X 轴视角
                x > centerX + 50 -> switchToView(0f, 90f)   // Z 轴视角
                else -> switchToView(90f, 0f)               // Y 轴视角
            }
        }
    }

    // 切换到指定的摄像机视角
    private fun switchToView(angleX: Float, angleY: Float) {
        // 使用 ValueAnimator 平滑地过渡到新的摄像机角度
        val startAngleX = cameraAngleX
        val startAngleY = cameraAngleY
        
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500 // 动画时长
            interpolator = LinearInterpolator() // 线性插值器
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                // 根据动画进度更新摄像机角度
                cameraAngleX = startAngleX + (angleX - startAngleX) * progress
                cameraAngleY = startAngleY + (angleY - startAngleY) * progress
                updateCamera()
            }
            start()
        }
    }

    // Activity onResume 时，注册帧回调以开始渲染
    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameScheduler)
    }

    // Activity onPause 时，移除帧回调以停止渲染
    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
    }

    // Activity onDestroy 时，销毁所有 Filament 资源
    override fun onDestroy() {
        super.onDestroy()
        
        // 停止渲染循环并分离 UiHelper
        choreographer.removeFrameCallback(frameScheduler)
        uiHelper.detach()
        
        // 销毁所有 Filament 创建的资源，防止内存泄漏
        engine.destroyEntity(light)
        engine.destroyEntity(cubeRenderable)
        engine.destroyEntity(axisRenderable)
        engine.destroyRenderer(renderer)
        engine.destroyVertexBuffer(cubeVertexBuffer)
        engine.destroyIndexBuffer(cubeIndexBuffer)
        engine.destroyVertexBuffer(axisVertexBuffer)
        engine.destroyIndexBuffer(axisIndexBuffer)
        // engine.destroyMaterialInstance(cubeMaterialInstance)
        // engine.destroyMaterialInstance(axisMaterialInstance)
        // engine.destroyMaterial(litMaterial)
        // engine.destroyMaterial(unlitMaterial)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)
        
        // 销毁 EntityManager 中的实体
        val entityManager = EntityManager.get()
        entityManager.destroy(light)
        entityManager.destroy(cubeRenderable)
        entityManager.destroy(axisRenderable)
        entityManager.destroy(camera.entity)
        
        // 最后销毁引擎
        engine.destroy()
    }

    // 帧回调，在每一帧被 Choreographer 调用
    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // 注册下一帧的回调
            choreographer.postFrameCallback(this)
            
            // 如果可以渲染，则开始渲染一帧
            if (uiHelper.isReadyToRender) {
                if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
            }
        }
    }

    // UiHelper 的渲染回调，处理 Surface 的生命周期事件
    inner class SurfaceCallback : UiHelper.RendererCallback {
        // 当本地窗口（Surface）创建或改变时调用
        override fun onNativeWindowChanged(surface: Surface) {
            // 销毁旧的 SwapChain 并创建新的
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
            displayHelper.attach(renderer, surfaceView.display)
        }

        // 当从 Surface 分离时调用
        override fun onDetachedFromSurface() {
            displayHelper.detach()
            // 销毁 SwapChain 并等待 GPU 完成
            swapChain?.let {
                engine.destroySwapChain(it)
                engine.flushAndWait()
                swapChain = null
            }
        }

        // 当 Surface 尺寸改变时调用
        override fun onResized(width: Int, height: Int) {
            // 更新摄像机的投影矩阵和视口
            val aspect = width.toDouble() / height.toDouble()
            camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)
            view.viewport = Viewport(0, 0, width, height)
            FilamentHelper.synchronizePendingFrames(engine)
        }
    }

    // 从 assets 目录读取未压缩的文件
    private fun readUncompressedAsset(assetName: String): ByteBuffer {
        assets.openFd(assetName).use { fd ->
            val input = fd.createInputStream()
            val dst = ByteBuffer.allocate(fd.length.toInt())
            val src = Channels.newChannel(input)
            src.read(dst)
            src.close()
            return dst.apply { rewind() }
        }
    }
}

// 业务流程说明：
// 1. onCreate() 初始化 UI、SurfaceView、Filament 渲染环境和场景。
// 2. setupUI() 构建界面布局和提示文本。
// 3. setupSurfaceView() 配置 SurfaceView 并绑定触摸事件。
// 4. setupFilament() 创建 Filament 引擎及核心对象。
// 5. setupView() 设置天空盒、摄像机和视图参数。
// 6. setupScene() 创建立方体和坐标轴网格，添加到场景。
// 7. setupLighting() 添加方向光源并设置曝光。
// 8. handleTouch() 处理用户拖拽和点击，实现视角旋转与切换。
// 9. FrameCallback 实现每帧渲染。
// 10. SurfaceCallback 响应 Surface 变化，调整视口和投影。
// 11. onDestroy() 释放所有资源，防止内存泄漏。
// Filament 用法说明：
// - 通过 Engine.create() 创建渲染引擎。
// - 使用 VertexBuffer/IndexBuffer 构建几何体。
// - RenderableManager.Builder 创建可渲染实体。
// - Scene.addEntity() 添加实体到场景。
// - Camera 控制视角，lookAt 设置观察点。
// - Renderer.beginFrame()/render()/endFrame() 完成一帧渲染。
// - 资源需在 onDestroy() 中全部销毁。
