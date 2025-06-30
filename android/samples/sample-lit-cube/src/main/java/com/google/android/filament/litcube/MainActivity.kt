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

// Filament 光照立方体示例的包声明
package com.google.android.filament.litcube

// Android 动画相关导入

// Filament 3D渲染引擎相关导入

// Java NIO 相关导入，用于高效的内存操作
import android.animation.ValueAnimator
import android.app.Activity
import android.opengl.Matrix
import android.os.Bundle
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import com.google.android.filament.Box
import com.google.android.filament.Camera
import com.google.android.filament.Colors
import com.google.android.filament.Engine
import com.google.android.filament.Entity
import com.google.android.filament.EntityManager
import com.google.android.filament.Filament
import com.google.android.filament.IndexBuffer
import com.google.android.filament.LightManager
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.MathUtils
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
import java.nio.channels.Channels

/**
 * MainActivity - Filament 光照立方体示例的主Activity
 * 这个示例展示了如何使用Filament渲染引擎创建一个带有光照效果的旋转立方体
 */
class MainActivity : Activity() {
    // Make sure to initialize Filament first
    // 确保首先初始化Filament
    // This loads the JNI library needed by most API calls
    // 这会加载大多数API调用所需的JNI库
    companion object {
        init {
            // 初始化Filament渲染引擎
            Filament.init()
        }
    }

    // === UI和显示相关组件 ===

    /**
     * The View we want to render into
     * 我们要渲染到的视图
     */
    private lateinit var surfaceView: SurfaceView

    /**
     * UiHelper is provided by Filament to manage SurfaceView and SurfaceTexture
     * UiHelper由Filament提供，用于管理SurfaceView和SurfaceTexture的生命周期
     */
    private lateinit var uiHelper: UiHelper

    /**
     * DisplayHelper is provided by Filament to manage the display
     * DisplayHelper由Filament提供，用于管理显示器相关功能（如HDR、刷新率等）
     */
    private lateinit var displayHelper: DisplayHelper

    /**
     * Choreographer is used to schedule new frames
     * Choreographer用于调度新帧，确保渲染与显示器刷新率同步
     */
    private lateinit var choreographer: Choreographer

    // === Filament核心渲染组件 ===

    /**
     * Engine creates and destroys Filament resources
     * Engine创建和销毁Filament资源
     * Each engine must be accessed from a single thread of your choosing
     * 每个引擎实例必须从单一线程访问（由您选择的线程）
     * Resources cannot be shared across engines
     * 资源不能在不同的引擎实例之间共享
     */
    private lateinit var engine: Engine

    /**
     * A renderer instance is tied to a single surface (SurfaceView, TextureView, etc.)
     * 一个渲染器实例绑定到单个表面（SurfaceView、TextureView等）
     */
    private lateinit var renderer: Renderer

    /**
     * A scene holds all the renderable, lights, etc. to be drawn
     * 场景包含所有要绘制的可渲染对象、灯光等
     */
    private lateinit var scene: Scene

    /**
     * A view defines a viewport, a scene and a camera for rendering
     * 视图定义了用于渲染的视口、场景和相机
     */
    private lateinit var view: View

    /**
     * Should be pretty obvious :)
     * 相机（这个应该很明显 :)）
     * 用于定义观察者在3D空间中的位置和视角
     */
    private lateinit var camera: Camera

    // === 材质和几何数据 ===

    /**
     * 材质定义了物体表面的视觉属性（如颜色、粗糙度、金属度等）
     */
    private lateinit var material: Material

    /**
     * 材质实例允许为同一材质设置不同的参数值
     */
    private lateinit var materialInstance: MaterialInstance

    /**
     * 顶点缓冲区存储几何体的顶点数据（位置、法线、切线等）
     */
    private lateinit var vertexBuffer: VertexBuffer

    /**
     * 索引缓冲区定义如何连接顶点形成三角形
     */
    private lateinit var indexBuffer: IndexBuffer

    // === Filament实体系统 ===

    /**
     * Filament entity representing a renderable object
     * Filament实体，代表一个可渲染对象（立方体）
     */
    @Entity
    private var renderable = 0

    /**
     * 光源实体，为场景提供照明
     */
    @Entity
    private var light = 0

    // === 渲染管道组件 ===
    /**
     * A swap chain is Filament's representation of a surface
     * 交换链是Filament对表面的表示，用于双缓冲渲染
     */
    private var swapChain: SwapChain? = null

    /**
     * Performs the rendering and schedules new frames
     * 执行渲染并调度新帧的回调
     */
    private val frameScheduler = FrameCallback()

    // === 动画系统 ===
    /**
     * 值动画器，用于创建0到360度的旋转动画
     */
    private val animator = ValueAnimator.ofFloat(0.0f, 360.0f)

    /**
     * Activity创建时的初始化方法
     * 按顺序设置Surface视图、Filament引擎、视图配置和3D场景
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建用于渲染的SurfaceView
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // 获取Choreographer实例，用于帧同步
        choreographer = Choreographer.getInstance()

        // 创建显示辅助器，管理显示相关功能
        displayHelper = DisplayHelper(this)

        // 按顺序初始化各个组件
        setupSurfaceView()  // 设置Surface视图
        setupFilament()     // 初始化Filament引擎组件
        setupView()         // 配置渲染视图
        setupScene()        // 创建3D场景内容
    }

    /**
     * 设置SurfaceView和UI辅助器
     * 配置渲染回调和Surface生命周期管理
     */
    private fun setupSurfaceView() {
        // 创建UI辅助器，DONT_CHECK表示不检查OpenGL上下文错误（提高性能）
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        // 设置渲染回调，处理Surface的创建、销毁和尺寸变化
        uiHelper.renderCallback = SurfaceCallback()

        // NOTE: To choose a specific rendering resolution, add the following line:
        // 注意：要选择特定的渲染分辨率，请添加以下行：
        // uiHelper.setDesiredSize(1280, 720)
        // 这可以用于固定渲染分辨率，而不是使用Surface的实际尺寸

        // 将UI辅助器附加到SurfaceView，开始管理其生命周期
        uiHelper.attachTo(surfaceView)
    }

    /**
     * 初始化Filament引擎和核心组件
     * 创建引擎、渲染器、场景、视图和相机
     */
    private fun setupFilament() {
        // 创建Filament引擎实例
        engine = Engine.create()
        // 创建渲染器，负责实际的渲染操作
        renderer = engine.createRenderer()
        // 创建场景，用于容纳所有3D对象
        scene = engine.createScene()
        // 创建视图，定义渲染的视口和参数
        view = engine.createView()
        // 创建相机实体和相机组件
        camera = engine.createCamera(engine.entityManager.create())
    }

    /**
     * 配置渲染视图
     * 设置天空盒、后处理效果，并关联相机和场景
     */
    private fun setupView() {
        // 创建深灰色的天空盒作为背景
        scene.skybox = Skybox.Builder().color(0.035f, 0.035f, 0.035f, 1.0f).build(engine)

        // NOTE: Try to disable post-processing (tone-mapping, etc.) to see the difference
        // 注意：尝试禁用后处理（色调映射等）以查看差异
        // view.isPostProcessingEnabled = false
        // 后处理包括色调映射、伽马校正、抗锯齿等效果

        // Tell the view which camera we want to use
        // 告诉视图要使用哪个相机
        view.camera = camera

        // Tell the view which scene we want to render
        // 告诉视图要渲染哪个场景
        view.scene = scene
    }

    /**
     * 设置3D场景内容
     * 创建立方体几何体、材质、光源，并配置相机和动画
     */
    private fun setupScene() {
        // 加载材质文件
        loadMaterial()
        // 配置材质参数
        setupMaterial()
        // 创建立方体的几何数据
        createMesh()

        // === 创建可渲染对象 ===
        // To create a renderable we first create a generic entity
        // 要创建可渲染对象，我们首先创建一个通用实体
        renderable = EntityManager.get().create()

        // 获取实体的变换管理器实例
        val ti = engine.transformManager.getInstance(renderable)
        // 设置变换矩阵（单位矩阵，表示无变换）
        engine.transformManager.setTransform(
            ti, floatArrayOf(
                1.0f, 0.0f, 0.0f, 0.0f,  // 第一行：X轴方向
                0.0f, 1.0f, 0.0f, 0.0f,  // 第二行：Y轴方向
                0.0f, 0.0f, 1.0f, 0.0f,  // 第三行：Z轴方向
                0.0f, 0.0f, 0.0f, 1.0f   // 第四行：平移和齐次坐标
            )
        )

        // We then create a renderable component on that entity
        // 然后在该实体上创建可渲染组件
        // A renderable is made of several primitives; in this case we declare only 1
        // 可渲染对象由多个图元组成；在这种情况下我们只声明1个
        // If we wanted each face of the cube to have a different material, we could
        // 如果我们希望立方体的每个面都有不同的材质，我们可以
        // declare 6 primitives (1 per face) and give each of them a different material
        // 声明6个图元（每个面一个）并为每个图元分配不同的材质
        // instance, setup with different parameters
        // 实例，设置不同的参数
        RenderableManager.Builder(1)
            // Overall bounding box of the renderable
            // 可渲染对象的整体包围盒
            .boundingBox(Box(0.0f, 0.0f, 0.0f,
                1.0f, 1.0f, 1.0f))
            // Sets the mesh data of the first primitive, 6 faces of 6 indices each
            // 设置第一个图元的网格数据，6个面，每个面6个索引
            .geometry(0, PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer, 0, 6 * 6)
            // Sets the material of the first primitive
            // 设置第一个图元的材质
            .material(0, materialInstance)
            .culling(false)      // 禁用背面剔除
            .receiveShadows(false)  // 不接收阴影
            .castShadows(false)     // 不投射阴影
            .build(engine, renderable)

        // Add the entity to the scene to render it
        // 将实体添加到场景中以渲染它
        scene.addEntity(renderable)

        // === 创建光源 ===
        // We now need a light, let's create a directional light
        // 现在我们需要一个光源，让我们创建一个方向光
        light = EntityManager.get().create()

        // Create a color from a temperature (5,500K)
        // 从色温（5,500K）创建颜色，模拟日光的颜色
        val (r, g, b) = Colors.cct(5_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            // 设置光源颜色为日光色温
            .color(r, g, b)
            // Intensity of the sun in lux on a clear day
            // 晴天太阳的光照强度（以勒克斯为单位）
            .intensity(110_000.0f)
            // The direction is normalized on our behalf
            // 光照方向（会自动归一化）
            .direction(0.0f, -0.5f, -1.0f)
            // 启用阴影投射
            .castShadows(true)
            .build(engine, light)

        // Add the entity to the scene to light it
        // 将光源实体添加到场景中以照亮场景
        scene.addEntity(light)

        // === 配置相机 ===
        // Set the exposure on the camera, this exposure follows the sunny f/16 rule
        // 设置相机曝光，此曝光遵循阳光f/16法则
        // Since we've defined a light that has the same intensity as the sun, it
        // 由于我们定义了与太阳强度相同的光源，它
        // guarantees a proper exposure
        // 保证了适当的曝光
        // 参数：光圈f/16，快门速度1/125秒，ISO 100
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)

        // Move the camera back to see the object
        // 将相机向后移动以查看对象
        // 参数：眼睛位置(0,3,4)，目标位置(0,0,0)，上方向(0,1,0)
        camera.lookAt(0.0, 3.0, 4.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)

        // 开始旋转动画
        startAnimation()
    }

    /**
     * 从资源文件加载材质
     * 读取预编译的.filamat材质文件
     */
    private fun loadMaterial() {
        // 读取assets目录下的材质文件并创建Material对象
        readUncompressedAsset("materials/lit.filamat").let {
            material = Material.Builder().payload(it, it.remaining()).build(engine)
        }
    }

    /**
     * 配置材质参数
     * 设置基础颜色、金属度和粗糙度等物理材质属性
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
        // 您可以直接传递，或使用Colors.RgbType.LINEAR
        // 设置基础颜色为温暖的金黄色
        materialInstance.setParameter("baseColor", Colors.RgbType.SRGB, 1.0f, 0.85f, 0.57f)
        // The default value is always 0, but it doesn't hurt to be clear about our intentions
        // 默认值总是0，但明确我们的意图并无害处
        // Here we are defining a dielectric material
        // 这里我们定义的是电介质材料（非金属）
        materialInstance.setParameter("metallic", 0.0f)
        // We increase the roughness to spread the specular highlights
        // 我们增加粗糙度以扩散镜面高光
        materialInstance.setParameter("roughness", 0.3f)
    }

    /**
     * 创建立方体的几何网格数据
     * 包括顶点位置、切线空间和索引数据
     */
    private fun createMesh() {
        // 数据类型大小定义
        val floatSize = 4   // float类型占4字节
        val shortSize = 2   // short类型占2字节
        // A vertex is a position + a tangent frame:
        // 一个顶点包含位置和切线空间：
        // 3 floats for XYZ position, 4 floats for normal+tangents (quaternion)
        // 3个float用于XYZ位置，4个float用于法线+切线（四元数）
        val vertexSize = 3 * floatSize + 4 * floatSize

        // Define a vertex and a function to put a vertex in a ByteBuffer
        // 定义顶点数据类和将顶点写入ByteBuffer的扩展函数
        @Suppress("ArrayInDataClass")
        data class Vertex(val x: Float, val y: Float, val z: Float, val tangents: FloatArray)

        fun ByteBuffer.put(v: Vertex): ByteBuffer {
            putFloat(v.x)       // 写入X坐标
            putFloat(v.y)       // 写入Y坐标
            putFloat(v.z)       // 写入Z坐标
            v.tangents.forEach { putFloat(it) }  // 写入切线空间数据
            return this
        }

        // 6 faces, 4 vertices per face
        // 6个面，每个面4个顶点
        val vertexCount = 6 * 4

        // === 创建切线空间 ===
        // Create tangent frames, one per face
        // 为每个面创建切线空间，用于正确的光照计算
        val tfPX = FloatArray(4)  // +X面的切线空间
        val tfNX = FloatArray(4)  // -X面的切线空间
        val tfPY = FloatArray(4)  // +Y面的切线空间
        val tfNY = FloatArray(4)  // -Y面的切线空间
        val tfPZ = FloatArray(4)  // +Z面的切线空间
        val tfNZ = FloatArray(4)  // -Z面的切线空间

        // 为每个面计算切线空间（法线、切线、副切线）
        // 参数：法线向量(3个)，切线向量(3个)，副切线向量(3个)，输出数组
        MathUtils.packTangentFrame(
            0.0f,
            1.0f,
            0.0f,
            0.0f,
            0.0f,
            -1.0f,
            1.0f,
            0.0f,
            0.0f,
            tfPX
        )  // +X面
        MathUtils.packTangentFrame(
            0.0f,
            1.0f,
            0.0f,
            0.0f,
            0.0f,
            -1.0f,
            -1.0f,
            0.0f,
            0.0f,
            tfNX
        )  // -X面
        MathUtils.packTangentFrame(
            -1.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            -1.0f,
            0.0f,
            1.0f,
            0.0f,
            tfPY
        )  // +Y面
        MathUtils.packTangentFrame(
            -1.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            1.0f,
            0.0f,
            -1.0f,
            0.0f,
            tfNY
        )  // -Y面
        MathUtils.packTangentFrame(
            0.0f,
            1.0f,
            0.0f,
            1.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            1.0f,
            tfPZ
        )  // +Z面
        MathUtils.packTangentFrame(
            0.0f,
            -1.0f,
            0.0f,
            1.0f,
            0.0f,
            0.0f,
            0.0f,
            0.0f,
            -1.0f,
            tfNZ
        )  // -Z面

        // === 创建顶点数据 ===
        val vertexData = ByteBuffer.allocate(vertexCount * vertexSize)
            // It is important to respect the native byte order
            // 重要：必须遵循本机字节序以确保数据正确性
            .order(ByteOrder.nativeOrder())
            // Face -Z (背面，朝向-Z方向)
            .put(Vertex(-1.0f, -1.0f, -1.0f, tfNZ))  // 左下角
            .put(Vertex(-1.0f, 1.0f, -1.0f, tfNZ))  // 左上角
            .put(Vertex(1.0f, 1.0f, -1.0f, tfNZ))  // 右上角
            .put(Vertex(1.0f, -1.0f, -1.0f, tfNZ))  // 右下角
            // Face +X (右面，朝向+X方向)
            .put(Vertex(1.0f, -1.0f, -1.0f, tfPX))  // 左下角
            .put(Vertex(1.0f, 1.0f, -1.0f, tfPX))  // 左上角
            .put(Vertex(1.0f, 1.0f, 1.0f, tfPX))  // 右上角
            .put(Vertex(1.0f, -1.0f, 1.0f, tfPX))  // 右下角
            // Face +Z (正面，朝向+Z方向)
            .put(Vertex(-1.0f, -1.0f, 1.0f, tfPZ))  // 左下角
            .put(Vertex(1.0f, -1.0f, 1.0f, tfPZ))  // 右下角
            .put(Vertex(1.0f, 1.0f, 1.0f, tfPZ))  // 右上角
            .put(Vertex(-1.0f, 1.0f, 1.0f, tfPZ))  // 左上角
            // Face -X (左面，朝向-X方向)
            .put(Vertex(-1.0f, -1.0f, 1.0f, tfNX))  // 左下角
            .put(Vertex(-1.0f, 1.0f, 1.0f, tfNX))  // 左上角
            .put(Vertex(-1.0f, 1.0f, -1.0f, tfNX))  // 右上角
            .put(Vertex(-1.0f, -1.0f, -1.0f, tfNX))  // 右下角
            // Face -Y (底面，朝向-Y方向)
            .put(Vertex(-1.0f, -1.0f, 1.0f, tfNY))  // 左下角
            .put(Vertex(-1.0f, -1.0f, -1.0f, tfNY))  // 左上角
            .put(Vertex(1.0f, -1.0f, -1.0f, tfNY))  // 右上角
            .put(Vertex(1.0f, -1.0f, 1.0f, tfNY))  // 右下角
            // Face +Y (顶面，朝向+Y方向)
            .put(Vertex(-1.0f, 1.0f, -1.0f, tfPY))  // 左下角
            .put(Vertex(-1.0f, 1.0f, 1.0f, tfPY))  // 左上角
            .put(Vertex(1.0f, 1.0f, 1.0f, tfPY))  // 右上角
            .put(Vertex(1.0f, 1.0f, -1.0f, tfPY))  // 右下角
            // Make sure the cursor is pointing in the right place in the byte buffer
            // 确保游标指向字节缓冲区中的正确位置
            .flip()

        // === 创建顶点缓冲区 ===
        // Declare the layout of our mesh
        // 声明网格的布局
        vertexBuffer = VertexBuffer.Builder()
            .bufferCount(1)                // 使用1个缓冲区
            .vertexCount(vertexCount)      // 设置顶点数量
            // Because we interleave position and color data we must specify offset and stride
            // 因为我们交错存储位置和切线数据，所以必须指定偏移量和步长
            // We could use de-interleaved data by declaring two buffers and giving each
            // 我们可以通过声明两个缓冲区并给每个属性不同的缓冲区索引来使用非交错数据
            // attribute a different buffer index
            // 为位置属性指定缓冲区布局（缓冲区索引，类型，偏移量，步长）
            .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0, vertexSize)
            // 为切线属性指定缓冲区布局（缓冲区索引，类型，偏移量，步长）
            .attribute(VertexAttribute.TANGENTS, 0, AttributeType.FLOAT4, 3 * floatSize, vertexSize)
            .build(engine)

        // Feed the vertex data to the mesh
        // 将顶点数据提供给网格
        // We only set 1 buffer because the data is interleaved
        // 我们只设置1个缓冲区，因为数据是交错的
        vertexBuffer.setBufferAt(engine, 0, vertexData)

        // === 创建索引缓冲区 ===
        // Create the indices
        // 创建索引数据，用于定义三角形
        val indexData = ByteBuffer.allocate(6 * 2 * 3 * shortSize)  // 6个面，每面2个三角形，每个三角形3个索引
            .order(ByteOrder.nativeOrder())
        repeat(6) {
            // 为每个面创建两个三角形（每个面由4个顶点组成，分成2个三角形）
            val i = (it * 4).toShort()  // 当前面的第一个顶点索引
            indexData
                // 第一个三角形：顶点0-1-2
                .putShort(i).putShort((i + 1).toShort()).putShort((i + 2).toShort())
                // 第二个三角形：顶点0-2-3
                .putShort(i).putShort((i + 2).toShort()).putShort((i + 3).toShort())
        }
        indexData.flip()  // 准备读取数据

        // 6 faces, 2 triangles per face,
        // 6个面，每个面2个三角形
        indexBuffer = IndexBuffer.Builder()
            .indexCount(vertexCount * 2)  // 索引数量 = 顶点数量 * 2（每个面4个顶点组成2个三角形）
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)  // 使用无符号短整型作为索引类型
            .build(engine)
        // 将索引数据设置到索引缓冲区
        indexBuffer.setBuffer(engine, indexData)
    }

    private fun startAnimation() {
        // Animate the triangle
        animator.interpolator = LinearInterpolator()
        animator.duration = 6000
        animator.repeatMode = ValueAnimator.RESTART
        animator.repeatCount = ValueAnimator.INFINITE
        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            val transformMatrix = FloatArray(16)
            override fun onAnimationUpdate(a: ValueAnimator) {
                Matrix.setRotateM(transformMatrix, 0, a.animatedValue as Float, 0.0f, 1.0f, 0.0f)
                val tcm = engine.transformManager
                tcm.setTransform(tcm.getInstance(renderable), transformMatrix)
            }
        })
        animator.start()
    }

    /**
     * Activity恢复时调用
     * 重新启动渲染循环和动画
     */
    override fun onResume() {
        super.onResume()
        // 重新开始帧回调，恢复渲染循环
        choreographer.postFrameCallback(frameScheduler)
        // 启动动画
        animator.start()
    }

    /**
     * Activity暂停时调用
     * 停止渲染循环和动画以节省资源
     */
    override fun onPause() {
        super.onPause()
        // 移除帧回调，暂停渲染循环
        choreographer.removeFrameCallback(frameScheduler)
        // 取消动画
        animator.cancel()
    }

    /**
     * Activity销毁时调用
     * 清理所有Filament资源，防止内存泄漏
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
        // 清理所有资源
        engine.destroyEntity(light)                    // 销毁光源实体
        engine.destroyEntity(renderable)               // 销毁可渲染实体
        engine.destroyRenderer(renderer)               // 销毁渲染器
        engine.destroyVertexBuffer(vertexBuffer)       // 销毁顶点缓冲区
        engine.destroyIndexBuffer(indexBuffer)         // 销毁索引缓冲区
        engine.destroyMaterialInstance(materialInstance) // 销毁材质实例
        engine.destroyMaterial(material)               // 销毁材质
        engine.destroyView(view)                       // 销毁视图
        engine.destroyScene(scene)                     // 销毁场景
        engine.destroyCameraComponent(camera.entity)   // 销毁相机组件

        // Engine.destroyEntity() destroys Filament related resources only
        // Engine.destroyEntity()只销毁Filament相关资源
        // (components), not the entity itself
        // （组件），而不是实体本身
        val entityManager = EntityManager.get()
        entityManager.destroy(light)        // 销毁光源实体本身
        entityManager.destroy(renderable)   // 销毁可渲染实体本身
        entityManager.destroy(camera.entity) // 销毁相机实体本身

        // Destroying the engine will free up any resource you may have forgotten
        // 销毁引擎将释放您可能忘记销毁的任何资源
        // to destroy, but it's recommended to do the cleanup properly
        // 但建议正确进行清理
        engine.destroy()
    }

    /**
     * 帧回调内部类
     * 处理每一帧的渲染逻辑
     */
    inner class FrameCallback : Choreographer.FrameCallback {
        /**
         * 每一帧调用的方法
         * @param frameTimeNanos 帧时间戳（纳秒）
         */
        override fun doFrame(frameTimeNanos: Long) {
            // Schedule the next frame
            // 安排下一帧的渲染
            choreographer.postFrameCallback(this)

            // This check guarantees that we have a swap chain
            // 此检查确保我们有一个交换链
            if (uiHelper.isReadyToRender) {
                // If beginFrame() returns false you should skip the frame
                // 如果beginFrame()返回false，您应该跳过该帧
                // This means you are sending frames too quickly to the GPU
                // 这意味着您向GPU发送帧的速度太快
                if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    // 渲染当前视图
                    renderer.render(view)
                    // 结束当前帧的渲染
                    renderer.endFrame()
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
         * 当原生窗口发生变化时调用
         * @param surface 新的渲染表面
         */
        override fun onNativeWindowChanged(surface: Surface) {
            // 如果已存在交换链，先销毁它
            swapChain?.let { engine.destroySwapChain(it) }
            // 为新表面创建交换链
            swapChain = engine.createSwapChain(surface)
            // 将显示助手附加到渲染器和显示器
            displayHelper.attach(renderer, surfaceView.display)
        }

        /**
         * 当从表面分离时调用
         * 清理渲染资源
         */
        override fun onDetachedFromSurface() {
            // 分离显示助手
            displayHelper.detach()
            // 销毁交换链并等待所有操作完成
            swapChain?.let {
                engine.destroySwapChain(it)
                // Required to ensure we don't return before Filament is done executing the
                // destroySwapChain command, otherwise Android might destroy the Surface
                // too early
                // 确保在Filament完成destroySwapChain命令执行之前不返回，
                // 否则Android可能会过早销毁Surface
                engine.flushAndWait()
                swapChain = null
            }
        }

        /**
         * 当表面尺寸发生变化时调用
         * @param width 新的宽度
         * @param height 新的高度
         */
        override fun onResized(width: Int, height: Int) {
            // 计算新的宽高比
            val aspect = width.toDouble() / height.toDouble()
            // 更新相机的投影矩阵（视野角度45度，近平面0.1，远平面20.0，垂直视野）
            camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)

            // 更新视图的视口尺寸
            view.viewport = Viewport(0, 0, width, height)

            // 同步待处理的帧，确保渲染管道状态一致
            FilamentHelper.synchronizePendingFrames(engine)
        }
    }

    /**
     * 读取未压缩的资源文件
     * @param assetName 资源文件名
     * @return 包含文件内容的ByteBuffer
     */
    private fun readUncompressedAsset(assetName: String): ByteBuffer {
        // 打开资源文件描述符
        assets.openFd(assetName).use { fd ->
            // 创建输入流
            val input = fd.createInputStream()
            // 分配与文件大小相同的字节缓冲区
            val dst = ByteBuffer.allocate(fd.length.toInt())

            // 创建NIO通道进行高效读取
            val src = Channels.newChannel(input)
            // 将文件内容读取到缓冲区
            src.read(dst)
            // 关闭源通道
            src.close()

            // 重置缓冲区位置并返回
            return dst.apply { rewind() }
        }
    }
}
