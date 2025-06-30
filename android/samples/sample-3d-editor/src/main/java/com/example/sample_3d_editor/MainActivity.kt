package com.example.sample_3d_editor

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.filament.Box
import com.google.android.filament.Camera
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndexBuffer
import com.google.android.filament.LightManager
import com.google.android.filament.RenderableManager
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.Renderer
import com.google.android.filament.Scene
import com.google.android.filament.Skybox
import com.google.android.filament.SwapChain
import com.google.android.filament.VertexBuffer
import com.google.android.filament.VertexBuffer.AttributeType
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.android.filament.View
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : Activity() {
    // 静态代码块，确保在使用 Filament API 前完成初始化。
    // 这是 Filament 的强制要求，必须在使用任何 Filament 功能之前调用。
    companion object {

        const val TAG = "MainActivity"

        init {
            Filament.init() // 初始化 Filament 库
        }
    }

    // UI 组件
    private lateinit var surfaceView: SurfaceView // 用于显示 3D 内容的 SurfaceView，是 Filament 渲染的目标
    private lateinit var rootLayout: ConstraintLayout // 界面的根布局

    // Filament 相关组件
    private lateinit var uiHelper: UiHelper // 辅助管理 SurfaceView 的生命周期，处理与 Android UI 的集成
    private lateinit var displayHelper: DisplayHelper // 处理显示相关的事件，如屏幕方向和分辨率变化
    private lateinit var choreographer: Choreographer // 用于同步渲染帧，确保动画和渲染的平滑性
    private lateinit var engine: Engine // Filament 渲染引擎，是所有 Filament 操作的核心
    private lateinit var renderer: Renderer // 渲染器，负责将场景渲染到 SurfaceView
    private lateinit var scene: Scene // 场景对象，包含所有需要渲染的实体，如模型、光源等
    private lateinit var view: View // 视图对象，定义了场景的观察方式，包括摄像机和视口

    /**
     * 摄像机，定义了观察场景的视角和投影
     */
    private lateinit var camera: Camera

    private lateinit var axisVertexBuffer: VertexBuffer // 存储坐标轴顶点数据的缓冲区
    private lateinit var axisIndexBuffer: IndexBuffer // 存储坐标轴索引数据的缓冲区
    private lateinit var smallBoxVertexBuffer: VertexBuffer // 存储小方块顶点数据的缓冲区
    private lateinit var smallBoxIndexBuffer: IndexBuffer // 存储小方块索引数据的缓冲区
    private lateinit var axisLabelVertexBuffer: VertexBuffer // 存储坐标轴标签顶点数据的缓冲区
    private lateinit var axisLabelIndexBuffer: IndexBuffer // 存储坐标轴标签索引数据的缓冲区

    // 实体（Entities）
    // 实体是场景中的基本对象，通过关联组件（如 Renderable、Transform）来定义其行为和外观。
    @Entity
    private var cubeRenderable = 0 // 立方体的可渲染实体

    @Entity
    private var axisRenderable = 0 // 坐标轴的可渲染实体

    @Entity
    private var xLabelRenderable = 0 // X轴标签的可渲染实体

    @Entity
    private var yLabelRenderable = 0 // Y轴标签的可渲染实体

    @Entity
    private var zLabelRenderable = 0 // Z轴标签的可渲染实体

    @Entity
    private var smallBoxRenderable = 0 // 小方块的可渲染实体

    @Entity
    private var light = 0 // 光源实体

    private var swapChain: SwapChain? = null // 用于将渲染结果呈现到屏幕的交换链
    private val frameScheduler = FrameCallback() // 帧回调，用于在每一帧触发渲染

    // 摄像机控制参数

    /**
     * 摄像机与目标的距离
     */
    private var cameraDistance = 8.0f

    /**
     * 摄像机的水平旋转角度
     */
    private var cameraAngleX = 30.0f

    /**
     * 摄像机的垂直旋转角度
     */
    private var cameraAngleY = 45.0f

    // 小方块控制参数
    private var smallBoxX = 0.0f // 小方块的X坐标
    private var smallBoxY = 0.0f // 小方块的Y坐标
    private var smallBoxZ = 0.0f // 小方块的Z坐标
    private var isSmallBoxSelected = false // 小方块是否被选中
    private var isDraggingSmallBox = false // 是否正在拖拽小方块

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
        // 创建坐标轴的网格数据
        createAxisMesh()
        // 创建小方块的网格数据
        createSmallBoxMesh()
        // 创建坐标轴标签的网格数据
        createAxisLabelMesh()
        // 创建可渲染实体并将其添加到场景中
        createRenderables()
        // 设置场景光照
        setupLighting()
    }

    // 创建坐标轴的网格数据
    private fun createAxisMesh() {
        val floatSize = 4
        val vertexSize = 3 * floatSize + 4 * floatSize // 每个顶点的大小（位置 + 颜色）

        // 坐标轴顶点：原点 + 3 个轴端点
        val axisLength = 3.0f

        // 定义带颜色的顶点数据结构
        data class ColorVertex(
            val x: Float,
            val y: Float,
            val z: Float,
            val r: Float,
            val g: Float,
            val b: Float,
            val a: Float
        )

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
            .attribute(
                VertexAttribute.COLOR,
                0,
                AttributeType.FLOAT4,
                3 * floatSize,
                vertexSize
            ) // 颜色属性
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

    // 创建小方块的网格数据
    private fun createSmallBoxMesh() {
        val floatSize = 4
        val vertexSize = 3 * floatSize + 4 * floatSize // 每个顶点的大小（位置 + 颜色）
        val boxSize = 0.3f // 小方块的大小

        // 定义带颜色的顶点数据结构
        data class ColorVertex(
            val x: Float,
            val y: Float,
            val z: Float,
            val r: Float,
            val g: Float,
            val b: Float,
            val a: Float
        )

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

        // 创建小方块的顶点数据（橙色）
        val smallBoxVertexData = ByteBuffer.allocate(8 * vertexSize)
            .order(ByteOrder.nativeOrder())
            // 8个顶点，橙色
            .put(ColorVertex(-boxSize, -boxSize, -boxSize, 1.0f, 0.5f, 0.0f, 1.0f))
            .put(ColorVertex(boxSize, -boxSize, -boxSize, 1.0f, 0.5f, 0.0f, 1.0f))
            .put(ColorVertex(boxSize, boxSize, -boxSize, 1.0f, 0.5f, 0.0f, 1.0f))
            .put(ColorVertex(-boxSize, boxSize, -boxSize, 1.0f, 0.5f, 0.0f, 1.0f))
            .put(ColorVertex(-boxSize, -boxSize, boxSize, 1.0f, 0.5f, 0.0f, 1.0f))
            .put(ColorVertex(boxSize, -boxSize, boxSize, 1.0f, 0.5f, 0.0f, 1.0f))
            .put(ColorVertex(boxSize, boxSize, boxSize, 1.0f, 0.5f, 0.0f, 1.0f))
            .put(ColorVertex(-boxSize, boxSize, boxSize, 1.0f, 0.5f, 0.0f, 1.0f))
            .flip()

        // 创建小方块的 VertexBuffer
        smallBoxVertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(8)
            .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0, vertexSize)
            .attribute(VertexAttribute.COLOR, 0, AttributeType.FLOAT4, 3 * floatSize, vertexSize)
            .build(engine)
        smallBoxVertexBuffer.setBufferAt(engine, 0, smallBoxVertexData)

        // 创建小方块的索引数据（12个三角形，36个索引）
        val indexData = ByteBuffer.allocate(36 * 2)
            .order(ByteOrder.nativeOrder())

        // 立方体的6个面，每个面2个三角形
        val faces = arrayOf(
            // 前面
            shortArrayOf(0, 1, 2, 0, 2, 3),
            // 后面
            shortArrayOf(4, 7, 6, 4, 6, 5),
            // 左面
            shortArrayOf(0, 3, 7, 0, 7, 4),
            // 右面
            shortArrayOf(1, 5, 6, 1, 6, 2),
            // 底面
            shortArrayOf(0, 4, 5, 0, 5, 1),
            // 顶面
            shortArrayOf(3, 2, 6, 3, 6, 7)
        )

        faces.forEach { face ->
            face.forEach { vertex ->
                indexData.putShort(vertex)
            }
        }
        indexData.flip()

        // 创建小方块的 IndexBuffer
        smallBoxIndexBuffer = IndexBuffer.Builder()
            .indexCount(36)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)
        smallBoxIndexBuffer.setBuffer(engine, indexData)
    }

    // 创建坐标轴标签的网格数据
    private fun createAxisLabelMesh() {
        val floatSize = 4
        val vertexSize = 3 * floatSize + 4 * floatSize // 每个顶点的大小（位置 + 颜色）
        val labelSize = 0.2f // 标签的大小
        val axisLength = 3.0f // 坐标轴长度，与createAxisMesh中的值保持一致
        val labelOffset = 0.3f // 标签相对于轴端点的偏移

        // 定义带颜色的顶点数据结构
        data class ColorVertex(
            val x: Float,
            val y: Float,
            val z: Float,
            val r: Float,
            val g: Float,
            val b: Float,
            val a: Float
        )

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

        // 创建字母"X"的线段（红色）
        val xVertexData = ByteBuffer.allocate(4 * vertexSize)
            .order(ByteOrder.nativeOrder())
            // X字母的两条对角线
            .put(
                ColorVertex(
                    axisLength + labelOffset - labelSize,
                    -labelSize,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    1.0f
                )
            )
            .put(
                ColorVertex(
                    axisLength + labelOffset + labelSize,
                    labelSize,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    1.0f
                )
            )
            .put(
                ColorVertex(
                    axisLength + labelOffset - labelSize,
                    labelSize,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    1.0f
                )
            )
            .put(
                ColorVertex(
                    axisLength + labelOffset + labelSize,
                    -labelSize,
                    0.0f,
                    1.0f,
                    0.0f,
                    0.0f,
                    1.0f
                )
            )
            .flip()

        // 创建字母"Y"的线段（绿色）
        val yVertexData = ByteBuffer.allocate(6 * vertexSize)
            .order(ByteOrder.nativeOrder())
            // Y字母的三条线段
            .put(
                ColorVertex(
                    -labelSize,
                    axisLength + labelOffset + labelSize,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    1.0f
                )
            )
            .put(ColorVertex(0.0f, axisLength + labelOffset, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f))
            .put(
                ColorVertex(
                    labelSize,
                    axisLength + labelOffset + labelSize,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    1.0f
                )
            )
            .put(ColorVertex(0.0f, axisLength + labelOffset, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f))
            .put(ColorVertex(0.0f, axisLength + labelOffset, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f))
            .put(
                ColorVertex(
                    0.0f,
                    axisLength + labelOffset - labelSize,
                    0.0f,
                    0.0f,
                    1.0f,
                    0.0f,
                    1.0f
                )
            )
            .flip()

        // 创建字母"Z"的线段（蓝色）
        val zVertexData = ByteBuffer.allocate(6 * vertexSize)
            .order(ByteOrder.nativeOrder())
            // Z字母的三条线段
            .put(
                ColorVertex(
                    -labelSize,
                    labelSize,
                    axisLength + labelOffset,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f
                )
            )
            .put(
                ColorVertex(
                    labelSize,
                    labelSize,
                    axisLength + labelOffset,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f
                )
            )
            .put(
                ColorVertex(
                    labelSize,
                    labelSize,
                    axisLength + labelOffset,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f
                )
            )
            .put(
                ColorVertex(
                    -labelSize,
                    -labelSize,
                    axisLength + labelOffset,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f
                )
            )
            .put(
                ColorVertex(
                    -labelSize,
                    -labelSize,
                    axisLength + labelOffset,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f
                )
            )
            .put(
                ColorVertex(
                    labelSize,
                    -labelSize,
                    axisLength + labelOffset,
                    0.0f,
                    0.0f,
                    1.0f,
                    1.0f
                )
            )
            .flip()

        // 合并所有标签的顶点数据
        val totalVertexData = ByteBuffer.allocate(16 * vertexSize)
            .order(ByteOrder.nativeOrder())
            .put(xVertexData as ByteBuffer)
            .put(yVertexData as ByteBuffer)
            .put(zVertexData as ByteBuffer)
            .flip()

        // 创建标签的 VertexBuffer
        axisLabelVertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)
            .vertexCount(16)
            .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0, vertexSize)
            .attribute(VertexAttribute.COLOR, 0, AttributeType.FLOAT4, 3 * floatSize, vertexSize)
            .build(engine)
        axisLabelVertexBuffer.setBufferAt(engine, 0, totalVertexData)

        // 创建标签的索引数据
        val indexData = ByteBuffer.allocate(16 * 2)
            .order(ByteOrder.nativeOrder())
            // X标签的索引
            .putShort(0).putShort(1)
            .putShort(2).putShort(3)
            // Y标签的索引
            .putShort(4).putShort(5)
            .putShort(6).putShort(7)
            .putShort(8).putShort(9)
            // Z标签的索引
            .putShort(10).putShort(11)
            .putShort(12).putShort(13)
            .putShort(14).putShort(15)
            .flip()

        // 创建标签的 IndexBuffer
        axisLabelIndexBuffer = IndexBuffer.Builder()
            .indexCount(16)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)
        axisLabelIndexBuffer.setBuffer(engine, indexData)
    }

    // 创建可渲染实体并将其添加到场景中
    private fun createRenderables() {

        // 创建坐标轴的可渲染实体
        axisRenderable = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(
                Box(
                    -3.0f, -3.0f, -3.0f,
                    3.0f, 3.0f, 3.0f
                )
            )
            .geometry(
                0, PrimitiveType.LINES,
                axisVertexBuffer, axisIndexBuffer, 0, 6
            ) // 关联几何体
            // .material(0, axisMaterialInstance) // 关联材质（已注释）
            .build(engine, axisRenderable)
        scene.addEntity(axisRenderable)

        // 创建小方块的可渲染实体
        smallBoxRenderable = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(-0.3f, -0.3f, -0.3f, 0.3f, 0.3f, 0.3f))
            .geometry(0, PrimitiveType.TRIANGLES, smallBoxVertexBuffer, smallBoxIndexBuffer, 0, 36)
            .build(engine, smallBoxRenderable)
        scene.addEntity(smallBoxRenderable)

        // 创建坐标轴标签的可渲染实体
        xLabelRenderable = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(-4.0f, -4.0f, -4.0f, 4.0f, 4.0f, 4.0f))
            .geometry(
                0,
                PrimitiveType.LINES,
                axisLabelVertexBuffer,
                axisLabelIndexBuffer,
                0,
                4
            ) // X标签的2条线，4个索引
            .build(engine, xLabelRenderable)
        scene.addEntity(xLabelRenderable)

        yLabelRenderable = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(-4.0f, -4.0f, -4.0f, 4.0f, 4.0f, 4.0f))
            .geometry(
                0,
                PrimitiveType.LINES,
                axisLabelVertexBuffer,
                axisLabelIndexBuffer,
                4,
                6
            ) // Y标签的3条线，6个索引
            .build(engine, yLabelRenderable)
        scene.addEntity(yLabelRenderable)

        zLabelRenderable = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(-4.0f, -4.0f, -4.0f, 4.0f, 4.0f, 4.0f))
            .geometry(
                0,
                PrimitiveType.LINES,
                axisLabelVertexBuffer,
                axisLabelIndexBuffer,
                10,
                6
            ) // Z标签的3条线，6个索引
            .build(engine, zLabelRenderable)
        scene.addEntity(zLabelRenderable)

        // 设置小方块的初始位置
        updateSmallBoxPosition()
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

    /**
     * 更新摄像机的位置和朝向
     */
    private fun updateCamera() {
        // 将角度转换为弧度
        val radX = Math.toRadians(cameraAngleX.toDouble())
        val radY = Math.toRadians(cameraAngleY.toDouble())

        // 根据距离和角度计算摄像机在球坐标系中的位置
        val x = (cameraDistance * cos(radX) * cos(radY)).toFloat()
        val y = (cameraDistance * sin(radX)).toFloat()
        val z = (cameraDistance * cos(radX) * sin(radY)).toFloat()

        // 设置摄像机的位置、目标和上方向
        camera.lookAt(
            x.toDouble(), y.toDouble(), z.toDouble(),
            0.0, 0.0, 0.0,
            0.0, 1.0, 0.0
        )
        
        Log.d(TAG, "Camera position updated: ($x, $y, $z)")
    }

    /**
     * 更新小方块的位置
     */
    private fun updateSmallBoxPosition() {
        val tm = engine.transformManager
        val transform = tm.getInstance(smallBoxRenderable)
        val matrix = FloatArray(16)

        // 获取当前的变换矩阵，保持现有的缩放和旋转信息
        tm.getTransform(transform, matrix)

        // 只修改平移分量，保持其他变换信息不变
        matrix[12] = smallBoxX
        matrix[13] = smallBoxY
        matrix[14] = smallBoxZ

        Log.d(TAG, "X: ${matrix[12]}")
        Log.d(TAG, "Y: ${matrix[13]}")
        Log.d(TAG, "Z: ${matrix[14]}")

        tm.setTransform(transform, matrix)
    }

    // 触摸事件处理
    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false
    
    // 小方块拖拽相关变量
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var smallBoxStartX = 0f
    private var smallBoxStartY = 0f
    private var smallBoxStartZ = 0f
    private var dragStartDepth = 0f // 拖拽开始时与摄像机的距离

    // 处理触摸事件，用于旋转摄像机或拖拽小方块
    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            // 手指按下时，记录初始位置并开始拖动
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y

                // 检查是否点击了小方块
                if (checkSmallBoxClick(event.x, event.y)) {
                    isDraggingSmallBox = true
                    isSmallBoxSelected = true
                    
                    // 记录拖拽开始时的屏幕坐标和小方块坐标
                    dragStartX = event.x
                    dragStartY = event.y
                    smallBoxStartX = smallBoxX
                    smallBoxStartY = smallBoxY
                    smallBoxStartZ = smallBoxZ
                    
                    // 计算并记录拖拽开始时与摄像机的距离
                    dragStartDepth = calculateDepthFromWorldPosition(smallBoxX, smallBoxY, smallBoxZ)
                    
                    Log.d(TAG, "Small box selected")
                } else {
                    isDragging = true
                    isSmallBoxSelected = false
                }
                return true
            }
            // 手指移动时，根据移动距离更新摄像机角度或移动小方块
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingSmallBox) {
                    // 使用深度不变原则：保持小方块与摄像机的距离不变
                    val newWorldPos = screenToWorldPositionAtCameraDistance(event.x, event.y, dragStartDepth)
                    
                    if (newWorldPos != null) {
                        // 更新小方块位置，保持与摄像机的距离不变
                        smallBoxX = newWorldPos[0]
                        smallBoxY = newWorldPos[1]
                        smallBoxZ = newWorldPos[2]
                        
                        updateSmallBoxPosition()

                        // 验证距离是否保持不变
                        val currentDistance = calculateDepthFromWorldPosition(smallBoxX, smallBoxY, smallBoxZ)
                        Log.d(
                            TAG,
                            "Moving small box to: ($smallBoxX, $smallBoxY, $smallBoxZ), distance: $currentDistance (original: $dragStartDepth)"
                        )
                    }
                } else if (isDragging) {
                    // 旋转摄像机
                    val deltaX = event.x - lastX
                    val deltaY = event.y - lastY

                    // 更新摄像机角度
                    cameraAngleY += deltaX * 0.5f
                    cameraAngleX += deltaY * 0.5f

                    // 限制垂直角度范围，防止摄像机翻转
                    cameraAngleX = cameraAngleX.coerceIn(-89f, 89f)

                    // 更新摄像机
                    updateCamera()
                }

                // 更新最后位置
                lastX = event.x
                lastY = event.y
                return true
            }
            // 手指抬起时，停止拖动
            MotionEvent.ACTION_UP -> {
                isDragging = false
                isDraggingSmallBox = false
                return true
            }
        }
        return false
    }

    // 检查是否点击了小方块
    private fun checkSmallBoxClick(screenX: Float, screenY: Float): Boolean {
        // 获取视图矩阵和投影矩阵
        val viewMatrix = DoubleArray(16)
        val projectionMatrix = DoubleArray(16)

        camera.getViewMatrix(viewMatrix)
        camera.getProjectionMatrix(projectionMatrix)

        val projectionMatrixFloat = projectionMatrix.map { it.toFloat() }.toFloatArray()
        val viewMatrixFloat = viewMatrix.map { it.toFloat() }.toFloatArray()

        // 获取小方块的模型矩阵
        val tm = engine.transformManager
        val transform = tm.getInstance(smallBoxRenderable)
        val modelMatrix = FloatArray(16)
        tm.getTransform(transform, modelMatrix)

        // 计算 Model-View 矩阵
        val mvMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvMatrix, 0, viewMatrixFloat, 0, modelMatrix, 0)

        // 计算 Model-View-Projection 矩阵
        val mvpMatrix = FloatArray(16)
        android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrixFloat, 0, mvMatrix, 0)

        // 小方块在模型空间的中心点（原点）
        val worldPos4 = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)
        val clipPos = FloatArray(4)

        // 矩阵乘法：MVP * worldPos
        android.opengl.Matrix.multiplyMV(clipPos, 0, mvpMatrix, 0, worldPos4, 0)

        // 透视除法
        if (clipPos[3] != 0.0f && clipPos[3] > 0.0f) { // 确保在摄像机前方
            val ndcX = clipPos[0] / clipPos[3]
            val ndcY = clipPos[1] / clipPos[3]

            // 检查是否在视锥体内
            if (ndcX >= -1.0f && ndcX <= 1.0f && ndcY >= -1.0f && ndcY <= 1.0f) {
                // 转换到屏幕坐标
                val projectedX = (ndcX + 1.0f) * 0.5f * surfaceView.width
                val projectedY = (1.0f - ndcY) * 0.5f * surfaceView.height

                // 检查点击是否在小方块附近
                val clickRadius = 80.0f // 增大点击区域
                val distance = kotlin.math.sqrt(
                    (screenX - projectedX) * (screenX - projectedX) +
                        (screenY - projectedY) * (screenY - projectedY)
                )

                Log.d(
                    TAG,
                    "Small box screen pos: ($projectedX, $projectedY), click: ($screenX, $screenY), distance: $distance, NDC: ($ndcX, $ndcY)"
                )

                return distance < clickRadius
            }
        }
        return false
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
        engine.destroyEntity(xLabelRenderable)
        engine.destroyEntity(yLabelRenderable)
        engine.destroyEntity(zLabelRenderable)
        engine.destroyEntity(smallBoxRenderable)
        engine.destroyRenderer(renderer)
        engine.destroyVertexBuffer(axisVertexBuffer)
        engine.destroyIndexBuffer(axisIndexBuffer)
        engine.destroyVertexBuffer(axisLabelVertexBuffer)
        engine.destroyIndexBuffer(axisLabelIndexBuffer)
        engine.destroyVertexBuffer(smallBoxVertexBuffer)
        engine.destroyIndexBuffer(smallBoxIndexBuffer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)

        // 销毁 EntityManager 中的实体
        val entityManager = EntityManager.get()
        entityManager.destroy(light)
        entityManager.destroy(cubeRenderable)
        entityManager.destroy(axisRenderable)
        entityManager.destroy(xLabelRenderable)
        entityManager.destroy(yLabelRenderable)
        entityManager.destroy(zLabelRenderable)
        entityManager.destroy(smallBoxRenderable)
        entityManager.destroy(camera.entity)

        // 最后销毁引擎
        engine.destroy()
    }

    /**
     * 计算世界坐标与摄像机的实际距离
     * @param worldX 世界坐标X
     * @param worldY 世界坐标Y
     * @param worldZ 世界坐标Z
     * @return 与摄像机的实际距离
     */
    private fun calculateDepthFromWorldPosition(worldX: Float, worldY: Float, worldZ: Float): Float {
        val viewMatrix = DoubleArray(16)
        camera.getViewMatrix(viewMatrix)
        val viewMatrixFloat = viewMatrix.map { it.toFloat() }.toFloatArray()
        
        // 获取相机在世界空间的位置
        val invViewMatrix = FloatArray(16)
        android.opengl.Matrix.invertM(invViewMatrix, 0, viewMatrixFloat, 0)
        val cameraWorldX = invViewMatrix[12]
        val cameraWorldY = invViewMatrix[13]
        val cameraWorldZ = invViewMatrix[14]
        
        // 计算与摄像机的欧几里得距离
        val deltaX = worldX - cameraWorldX
        val deltaY = worldY - cameraWorldY
        val deltaZ = worldZ - cameraWorldZ
        
        return kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
    }

    /**
     * 在指定深度平面上将屏幕坐标转换为世界坐标
     * 使用射线投射方法，确保结果在指定的Z深度平面上
     * @param screenX 屏幕X坐标
     * @param screenY 屏幕Y坐标
     * @param targetDepthZ 目标世界坐标Z值（深度）
     * @return 世界坐标数组[x, y, z]，如果转换失败返回null
     */
    private fun screenToWorldPositionAtDepth(screenX: Float, screenY: Float, targetDepthZ: Float): FloatArray? {
        try {
            // 获取视图矩阵和投影矩阵
            val viewMatrix = DoubleArray(16)
            val projectionMatrix = DoubleArray(16)
            camera.getViewMatrix(viewMatrix)
            camera.getProjectionMatrix(projectionMatrix)

            // 转换为Float数组
            val viewMatrixFloat = viewMatrix.map { it.toFloat() }.toFloatArray()
            val projectionMatrixFloat = projectionMatrix.map { it.toFloat() }.toFloatArray()

            // 计算视图投影矩阵的逆矩阵
            val vpMatrix = FloatArray(16)
            val vpInverseMatrix = FloatArray(16)
            android.opengl.Matrix.multiplyMM(vpMatrix, 0, projectionMatrixFloat, 0, viewMatrixFloat, 0)
            
            if (!android.opengl.Matrix.invertM(vpInverseMatrix, 0, vpMatrix, 0)) {
                return null // 矩阵不可逆
            }

            // 将屏幕坐标转换为NDC坐标
            val ndcX = (screenX / surfaceView.width) * 2.0f - 1.0f
            val ndcY = -((screenY / surfaceView.height) * 2.0f - 1.0f) // Y轴翻转

            // 获取相机在世界空间的位置
            val invViewMatrix = FloatArray(16)
            android.opengl.Matrix.invertM(invViewMatrix, 0, viewMatrixFloat, 0)
            val cameraWorldX = invViewMatrix[12]
            val cameraWorldY = invViewMatrix[13]
            val cameraWorldZ = invViewMatrix[14]
            
            // 在近平面上获取射线方向
            val nearNdcPos = floatArrayOf(ndcX, ndcY, -1.0f, 1.0f)
            val nearWorldPos = FloatArray(4)
            android.opengl.Matrix.multiplyMV(nearWorldPos, 0, vpInverseMatrix, 0, nearNdcPos, 0)
            if (nearWorldPos[3] != 0.0f) {
                nearWorldPos[0] /= nearWorldPos[3]
                nearWorldPos[1] /= nearWorldPos[3]
                nearWorldPos[2] /= nearWorldPos[3]
            }
            
            // 计算射线方向
            val rayDirX = nearWorldPos[0] - cameraWorldX
            val rayDirY = nearWorldPos[1] - cameraWorldY
            val rayDirZ = nearWorldPos[2] - cameraWorldZ
            
            // 计算射线与Z=targetDepthZ平面的交点
            // 射线方程: P = camera + t * rayDir
            // 平面方程: Z = targetDepthZ
            // 求解: cameraWorldZ + t * rayDirZ = targetDepthZ
            if (kotlin.math.abs(rayDirZ) < 1e-6f) {
                // 射线与平面平行，无交点
                return null
            }
            
            val t = (targetDepthZ - cameraWorldZ) / rayDirZ
            // 移除t < 0的限制，允许在相机前后方向上的投射
            // if (t < 0) {
            //     // 交点在相机后方
            //     return null
            // }
            
            val intersectionX = cameraWorldX + t * rayDirX
            val intersectionY = cameraWorldY + t * rayDirY
            
            Log.d(TAG, "Camera position: ($cameraWorldX, $cameraWorldY, $cameraWorldZ)")
            Log.d(TAG, "Ray direction: ($rayDirX, $rayDirY, $rayDirZ)")
            Log.d(TAG, "Target depth: $targetDepthZ, t: $t")
            Log.d(TAG, "Intersection: ($intersectionX, $intersectionY, $targetDepthZ)")
            
            return floatArrayOf(intersectionX, intersectionY, targetDepthZ)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error converting screen to world position at depth", e)
            return null
        }
    }

    /**
     * 在指定的摄像机距离上将屏幕坐标转换为世界坐标
     * 使用深度不变原则：保持物体与摄像机的距离不变
     * @param screenX 屏幕X坐标
     * @param screenY 屏幕Y坐标
     * @param cameraDistance 与摄像机的距离
     * @return 世界坐标数组[x, y, z]，如果转换失败返回null
     */
    private fun screenToWorldPositionAtCameraDistance(screenX: Float, screenY: Float, cameraDistance: Float): FloatArray? {
        try {
            // 获取视图矩阵和投影矩阵
            val viewMatrix = DoubleArray(16)
            val projectionMatrix = DoubleArray(16)
            camera.getViewMatrix(viewMatrix)
            camera.getProjectionMatrix(projectionMatrix)

            // 转换为Float数组
            val viewMatrixFloat = viewMatrix.map { it.toFloat() }.toFloatArray()
            val projectionMatrixFloat = projectionMatrix.map { it.toFloat() }.toFloatArray()

            // 计算视图投影矩阵的逆矩阵
            val vpMatrix = FloatArray(16)
            val vpInverseMatrix = FloatArray(16)
            android.opengl.Matrix.multiplyMM(vpMatrix, 0, projectionMatrixFloat, 0, viewMatrixFloat, 0)
            
            if (!android.opengl.Matrix.invertM(vpInverseMatrix, 0, vpMatrix, 0)) {
                return null // 矩阵不可逆
            }

            // 将屏幕坐标转换为NDC坐标
            val ndcX = (screenX / surfaceView.width) * 2.0f - 1.0f
            val ndcY = -((screenY / surfaceView.height) * 2.0f - 1.0f) // Y轴翻转

            // 获取相机在世界空间的位置
            val invViewMatrix = FloatArray(16)
            android.opengl.Matrix.invertM(invViewMatrix, 0, viewMatrixFloat, 0)
            val cameraWorldX = invViewMatrix[12]
            val cameraWorldY = invViewMatrix[13]
            val cameraWorldZ = invViewMatrix[14]
            
            // 在近平面上获取射线方向
            val nearNdcPos = floatArrayOf(ndcX, ndcY, -1.0f, 1.0f)
            val nearWorldPos = FloatArray(4)
            android.opengl.Matrix.multiplyMV(nearWorldPos, 0, vpInverseMatrix, 0, nearNdcPos, 0)
            if (nearWorldPos[3] != 0.0f) {
                nearWorldPos[0] /= nearWorldPos[3]
                nearWorldPos[1] /= nearWorldPos[3]
                nearWorldPos[2] /= nearWorldPos[3]
            }
            
            // 计算射线方向（从相机到屏幕点的方向）
            val rayDirX = nearWorldPos[0] - cameraWorldX
            val rayDirY = nearWorldPos[1] - cameraWorldY
            val rayDirZ = nearWorldPos[2] - cameraWorldZ
            
            // 归一化射线方向
            val rayLength = kotlin.math.sqrt(rayDirX * rayDirX + rayDirY * rayDirY + rayDirZ * rayDirZ)
            if (rayLength < 1e-6f) {
                return null // 射线长度为0
            }
            
            val normalizedRayDirX = rayDirX / rayLength
            val normalizedRayDirY = rayDirY / rayLength
            val normalizedRayDirZ = rayDirZ / rayLength
            
            // 沿射线移动指定的摄像机距离
            val targetWorldX = cameraWorldX + normalizedRayDirX * cameraDistance
            val targetWorldY = cameraWorldY + normalizedRayDirY * cameraDistance
            val targetWorldZ = cameraWorldZ + normalizedRayDirZ * cameraDistance
            
            Log.d(TAG, "Camera position: ($cameraWorldX, $cameraWorldY, $cameraWorldZ)")
            Log.d(TAG, "Ray direction: ($normalizedRayDirX, $normalizedRayDirY, $normalizedRayDirZ)")
            Log.d(TAG, "Camera distance: $cameraDistance")
            Log.d(TAG, "Target position: ($targetWorldX, $targetWorldY, $targetWorldZ)")
            
            return floatArrayOf(targetWorldX, targetWorldY, targetWorldZ)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error converting screen to world position at camera distance", e)
            return null
        }
    }

    /**
     * 将屏幕坐标转换为相机平面上的世界坐标
     * @param screenX 屏幕X坐标
     * @param screenY 屏幕Y坐标
     * @param depth 指定的深度值，如果为null则使用小方块当前深度
     * @return 世界坐标数组[x, y, z]，如果转换失败返回null
     */
    private fun screenToWorldPosition(screenX: Float, screenY: Float, depth: Float? = null): FloatArray? {
        try {
            // 获取视图矩阵和投影矩阵
            val viewMatrix = DoubleArray(16)
            val projectionMatrix = DoubleArray(16)
            camera.getViewMatrix(viewMatrix)
            camera.getProjectionMatrix(projectionMatrix)

            // 转换为Float数组
            val viewMatrixFloat = viewMatrix.map { it.toFloat() }.toFloatArray()
            val projectionMatrixFloat = projectionMatrix.map { it.toFloat() }.toFloatArray()

            // 计算视图投影矩阵的逆矩阵
            val vpMatrix = FloatArray(16)
            val vpInverseMatrix = FloatArray(16)
            android.opengl.Matrix.multiplyMM(vpMatrix, 0, projectionMatrixFloat, 0, viewMatrixFloat, 0)
            
            if (!android.opengl.Matrix.invertM(vpInverseMatrix, 0, vpMatrix, 0)) {
                return null // 矩阵不可逆
            }

            // 将屏幕坐标转换为NDC坐标
            val ndcX = (screenX / surfaceView.width) * 2.0f - 1.0f
            val ndcY = -((screenY / surfaceView.height) * 2.0f - 1.0f) // Y轴翻转

            // 使用指定的深度值或计算当前小方块的深度
            val useDepth = depth ?: run {
                val currentWorldPos = floatArrayOf(smallBoxX, smallBoxY, smallBoxZ, 1.0f)
                val currentViewPos = FloatArray(4)
                android.opengl.Matrix.multiplyMV(currentViewPos, 0, viewMatrixFloat, 0, currentWorldPos, 0)
                -currentViewPos[2] // 相机空间中的Z深度（负值转正值）
            }

            // 创建射线：从相机位置到屏幕点在指定深度处的世界坐标
            // 首先获取相机在世界空间的位置
            val cameraWorldPos = FloatArray(3)
            val invViewMatrix = FloatArray(16)
            android.opengl.Matrix.invertM(invViewMatrix, 0, viewMatrixFloat, 0)
            cameraWorldPos[0] = invViewMatrix[12]
            cameraWorldPos[1] = invViewMatrix[13]
            cameraWorldPos[2] = invViewMatrix[14]
            
            // 计算射线方向（从相机到屏幕点的方向）
            val nearPlane = 0.1f
            val farPlane = 20.0f
            
            // 在近平面上的点
            val nearNdcZ = -1.0f
            val nearNdcPos = floatArrayOf(ndcX, ndcY, nearNdcZ, 1.0f)
            val nearWorldPos = FloatArray(4)
            android.opengl.Matrix.multiplyMV(nearWorldPos, 0, vpInverseMatrix, 0, nearNdcPos, 0)
            if (nearWorldPos[3] != 0.0f) {
                nearWorldPos[0] /= nearWorldPos[3]
                nearWorldPos[1] /= nearWorldPos[3]
                nearWorldPos[2] /= nearWorldPos[3]
            }
            
            // 计算射线方向
            val rayDirX = nearWorldPos[0] - cameraWorldPos[0]
            val rayDirY = nearWorldPos[1] - cameraWorldPos[1]
            val rayDirZ = nearWorldPos[2] - cameraWorldPos[2]
            
            // 归一化射线方向
            val rayLength = kotlin.math.sqrt(rayDirX * rayDirX + rayDirY * rayDirY + rayDirZ * rayDirZ)
            val normalizedRayDirX = rayDirX / rayLength
            val normalizedRayDirY = rayDirY / rayLength
            val normalizedRayDirZ = rayDirZ / rayLength
            
            // 沿射线移动到指定深度
            val targetWorldX = cameraWorldPos[0] + normalizedRayDirX * useDepth
            val targetWorldY = cameraWorldPos[1] + normalizedRayDirY * useDepth
            val targetWorldZ = cameraWorldPos[2] + normalizedRayDirZ * useDepth
            
            return floatArrayOf(targetWorldX, targetWorldY, targetWorldZ)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting screen to world position", e)
            return null
        }
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
}
