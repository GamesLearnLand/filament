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
 * Filament Hello Triangle 示例应用
 * 
 * 这是一个使用 Google Filament 渲染引擎的基础示例，展示了如何：
 * 1. 初始化 Filament 引擎和相关组件
 * 2. 创建一个简单的三角形几何体
 * 3. 设置材质和着色器
 * 4. 实现基本的渲染循环
 * 5. 添加旋转动画效果
 * 
 * Filament 是 Google 开发的实时物理渲染引擎，专为移动设备优化，
 * 支持现代渲染技术如基于物理的渲染(PBR)。
 */
package com.google.android.filament.hellotriangle

import android.animation.ValueAnimator
import android.app.Activity
import android.opengl.Matrix
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import android.view.animation.LinearInterpolator
import com.google.android.filament.*
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.VertexBuffer.AttributeType
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.FilamentHelper
import com.google.android.filament.android.UiHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.Channels
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * MainActivity - Filament Hello Triangle 主活动类
 * 
 * 这个类演示了 Filament 渲染引擎的基本用法，包括：
 * - 引擎初始化和资源管理
 * - 几何体创建和材质设置
 * - 渲染循环和动画系统
 */
class MainActivity : Activity() {
    
    companion object {
        init {
            // 首先必须初始化 Filament
            // 这会加载大部分 API 调用所需的 JNI 库
            // 这是使用 Filament 的第一步，必须在任何其他 Filament 调用之前执行
            Filament.init()
        }
    }

    // ========== Android UI 组件 ==========
    
    /** 用于渲染的 Android SurfaceView */
    private lateinit var surfaceView: SurfaceView
    
    /** Filament 提供的 UI 辅助类，用于管理 SurfaceView 和 SurfaceTexture */
    private lateinit var uiHelper: UiHelper
    
    /** Filament 提供的显示辅助类，用于管理显示相关功能 */
    private lateinit var displayHelper: DisplayHelper
    
    /** Android 的 Choreographer，用于调度新帧的渲染 */
    private lateinit var choreographer: Choreographer

    // ========== Filament 核心组件 ==========
    
    /** 
     * Filament 引擎 - 创建和销毁 Filament 资源的核心组件
     * 注意：每个引擎必须从单一线程访问，资源不能在引擎间共享
     */
    private lateinit var engine: Engine
    
    /** 
     * 渲染器实例 - 绑定到单一表面（SurfaceView、TextureView 等）
     * 负责执行实际的渲染操作
     */
    private lateinit var renderer: Renderer
    
    /** 
     * 场景 - 包含所有要绘制的可渲染对象、灯光等
     * 类似于 3D 场景图的概念
     */
    private lateinit var scene: Scene
    
    /** 
     * 视图 - 定义视口、场景和用于渲染的相机
     * 连接场景和相机，定义渲染参数
     */
    private lateinit var view: View
    
    /** 
     * 相机 - 定义观察点和投影方式
     * 控制从哪个角度和如何观察 3D 场景
     */
    private lateinit var camera: Camera

    // ========== 渲染资源 ==========
    
    /** 材质 - 定义物体的外观属性（颜色、纹理、着色器等） */
    private lateinit var material: Material
    
    /** 顶点缓冲区 - 存储几何体的顶点数据（位置、颜色、法线等） */
    private lateinit var vertexBuffer: VertexBuffer
    
    /** 索引缓冲区 - 存储顶点索引，定义如何连接顶点形成三角形 */
    private lateinit var indexBuffer: IndexBuffer

    /** 
     * Filament 实体 - 代表一个可渲染对象
     * 使用 @Entity 注解标记，这是 Filament 的实体组件系统的一部分
     */
    @Entity private var renderable = 0

    /** 
     * 交换链 - Filament 对表面的表示
     * 管理前后缓冲区的交换，实现双缓冲渲染
     */
    private var swapChain: SwapChain? = null

    // ========== 动画和帧调度 ==========
    
    /** 帧回调 - 执行渲染并调度新帧 */
    private val frameScheduler = FrameCallback()

    /** 值动画器 - 用于创建三角形的旋转动画效果 */
    private val animator = ValueAnimator.ofFloat(0.0f, 360.0f)

    /**
     * Activity 创建时的初始化流程
     * 
     * 业务逻辑：
     * 1. 创建 Android UI 组件
     * 2. 初始化 Filament 相关组件
     * 3. 设置渲染管道
     * 4. 创建和配置 3D 场景
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 创建 SurfaceView 作为渲染表面
        surfaceView = SurfaceView(this)
        setContentView(surfaceView)

        // 获取 Choreographer 实例用于帧同步
        choreographer = Choreographer.getInstance()

        // 创建显示辅助类
        displayHelper = DisplayHelper(this)

        // 按顺序初始化各个组件
        // 注意：这个顺序很重要，后面的步骤依赖前面的初始化结果
        setupSurfaceView()  // 1. 设置 Android 表面
        setupFilament()     // 2. 初始化 Filament 引擎
        setupView()         // 3. 配置视图和相机
        setupScene()        // 4. 创建 3D 场景内容
    }

    /**
     * 设置 Android SurfaceView
     * 
     * 业务逻辑：
     * 1. 创建 UiHelper 来管理 Surface 生命周期
     * 2. 设置渲染回调来响应 Surface 变化
     * 3. 将 UiHelper 绑定到 SurfaceView
     */
    private fun setupSurfaceView() {
        // 创建 UiHelper，DONT_CHECK 表示不检查 OpenGL 上下文错误
        // UiHelper 是 Filament 提供的工具类，简化了 Surface 管理
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        
        // 设置渲染回调，当 Surface 状态改变时会调用相应方法
        uiHelper.renderCallback = SurfaceCallback()

        // 注意：如果要选择特定的渲染分辨率，可以添加以下行：
        // uiHelper.setDesiredSize(1280, 720)
        
        // 将 UiHelper 附加到 SurfaceView
        // 这会开始监听 Surface 的生命周期事件
        uiHelper.attachTo(surfaceView)
    }

    /**
     * 初始化 Filament 引擎和核心组件
     * 
     * 业务逻辑：
     * 1. 配置引擎参数
     * 2. 创建引擎实例
     * 3. 创建渲染管道的各个组件
     * 
     * Filament 架构说明：
     * - Engine: 资源管理和渲染引擎核心
     * - Renderer: 执行渲染命令
     * - Scene: 场景图，包含所有渲染对象
     * - View: 视图配置，连接场景和相机
     * - Camera: 相机组件，定义观察参数
     */
    private fun setupFilament() {
        // 创建引擎配置
        val config = Engine.Config()
        // 可选：强制使用 OpenGL ES 2.0 上下文（用于兼容性测试）
        // config.forceGLES2Context = true

        // 使用建造者模式创建 Filament 引擎
        engine = Engine.Builder()
            .config(config)
            // 设置功能级别为 0（最基础级别，兼容性最好）
            // FEATURE_LEVEL_0: 基础功能，不支持后处理等高级特性
            .featureLevel(Engine.FeatureLevel.FEATURE_LEVEL_0)
            .build()
            
        // 创建渲染器 - 负责执行渲染命令
        renderer = engine.createRenderer()
        
        // 创建场景 - 包含所有要渲染的对象
        scene = engine.createScene()
        
        // 创建视图 - 定义渲染视口和参数
        view = engine.createView()
        
        // 创建相机组件
        // 首先创建一个实体，然后为其添加相机组件
        camera = engine.createCamera(engine.entityManager.create())
    }

    /**
     * 配置视图和渲染参数
     * 
     * 业务逻辑：
     * 1. 设置天空盒（背景）
     * 2. 根据功能级别配置后处理
     * 3. 连接相机和场景到视图
     * 
     * Filament 渲染管道说明：
     * - Skybox: 天空盒，提供场景背景
     * - Post-processing: 后处理效果（如色调映射、抗锯齿等）
     * - View: 连接场景、相机和渲染参数的桥梁
     */
    private fun setupView() {
        // 创建天空盒作为场景背景
        // 使用深灰色 (0.035, 0.035, 0.035) 作为背景色
        scene.skybox = Skybox.Builder()
            .color(0.035f, 0.035f, 0.035f, 1.0f)
            .build(engine)

        // 功能级别 0 不支持后处理效果
        // 后处理包括色调映射、抗锯齿、景深等高级渲染特性
        if (engine.activeFeatureLevel == Engine.FeatureLevel.FEATURE_LEVEL_0) {
            view.isPostProcessingEnabled = false
        }

        // 告诉视图使用哪个相机
        // 这建立了视图和相机之间的连接
        view.camera = camera

        // 告诉视图要渲染哪个场景
        // 这建立了视图和场景之间的连接
        view.scene = scene
    }

    /**
     * 设置 3D 场景内容
     * 
     * 业务逻辑：
     * 1. 加载材质资源
     * 2. 创建三角形几何体
     * 3. 创建可渲染实体
     * 4. 配置渲染组件
     * 5. 启动动画
     * 
     * Filament 实体组件系统说明：
     * - Entity: 实体，是一个唯一标识符
     * - RenderableManager: 管理可渲染组件
     * - 每个实体可以有多个组件（渲染、变换、灯光等）
     */
    private fun setupScene() {
        // 加载材质文件
        loadMaterial()
        
        // 创建三角形的几何数据
        createMesh()

        // 创建可渲染实体
        // 首先创建一个通用实体（只是一个 ID）
        renderable = EntityManager.get().create()

        // 然后为该实体创建可渲染组件
        // 一个可渲染对象由多个图元组成；这里我们只声明 1 个
        RenderableManager.Builder(1)
                // 设置可渲染对象的整体包围盒（用于视锥剔除等优化）
                .boundingBox(Box(0.0f, 0.0f, 0.0f,
                    1.0f, 1.0f, 0.01f))
                // 设置第一个图元的网格数据
                // 参数：图元索引、类型、顶点缓冲区、索引缓冲区、偏移、索引数量
                .geometry(0, PrimitiveType.TRIANGLES,
                    vertexBuffer, indexBuffer, 0, 3)
                // 设置第一个图元的材质
                .material(0, material.defaultInstance)
                .build(engine, renderable)

        // 将实体添加到场景中进行渲染
        scene.addEntity(renderable)

        // 启动旋转动画
        startAnimation()
    }

    /**
     * 加载材质资源
     * 
     * 业务逻辑：
     * 1. 从 assets 读取预编译的材质文件
     * 2. 创建 Filament 材质对象
     * 3. 异步编译材质
     * 4. 刷新引擎确保资源就绪
     * 
     * Filament 材质系统说明：
     * - .filamat 文件是预编译的材质文件
     * - 材质定义了着色器和渲染状态
     * - 编译过程会生成针对当前设备的优化着色器
     */
    private fun loadMaterial() {
        // 读取 assets 中的材质文件
        readUncompressedAsset("materials/baked_color.filamat").let {
            // 使用二进制数据创建材质
            material = Material.Builder()
                .payload(it, it.remaining())
                .build(engine)
                
            // 异步编译材质
            // 这会在后台线程编译着色器，避免阻塞主线程
            material.compile(
                Material.CompilerPriorityQueue.HIGH,  // 高优先级编译
                Material.UserVariantFilterBit.ALL,   // 编译所有变体
                Handler(Looper.getMainLooper())       // 完成回调在主线程执行
            ) {
                // 编译完成的回调
                android.util.Log.i("hellotriangle",
                    "Material " + material.name + " compiled.")
            }
            
            // 刷新引擎，确保所有命令都被处理
            engine.flush()
        }
    }

    /**
     * 创建三角形网格数据
     * 
     * 业务逻辑：
     * 1. 定义顶点数据结构
     * 2. 生成三角形的三个顶点（位置和颜色）
     * 3. 创建顶点缓冲区
     * 4. 创建索引缓冲区
     * 
     * 几何数据说明：
     * - 创建一个等边三角形，三个顶点分别为红、绿、蓝色
     * - 使用交错存储（位置和颜色数据混合存储）
     * - 顶点按逆时针顺序排列（右手坐标系）
     */
    private fun createMesh() {
        // 定义数据类型大小
        val intSize = 4      // 32位整数
        val floatSize = 4    // 32位浮点数
        val shortSize = 2    // 16位短整数
        
        // 每个顶点包含：位置（3个float）+ 颜色（1个int）
        val vertexSize = 3 * floatSize + intSize

        // 定义顶点数据类和扩展函数
        data class Vertex(val x: Float, val y: Float, val z: Float, val color: Int)
        
        // 扩展函数：将顶点数据写入 ByteBuffer
        fun ByteBuffer.put(v: Vertex): ByteBuffer {
            putFloat(v.x)     // X 坐标
            putFloat(v.y)     // Y 坐标
            putFloat(v.z)     // Z 坐标
            putInt(v.color)   // ARGB 颜色值
            return this
        }

        // 创建等边三角形的三个顶点
        val vertexCount = 3
        val a1 = PI * 2.0 / 3.0  // 120度角
        val a2 = PI * 4.0 / 3.0  // 240度角

        // 创建顶点数据缓冲区
        val vertexData = ByteBuffer.allocate(vertexCount * vertexSize)
                // 重要：必须使用本机字节序
                .order(ByteOrder.nativeOrder())
                // 第一个顶点：右侧，红色
                .put(Vertex(1.0f, 0.0f, 0.0f, 0xffff0000.toInt()))
                // 第二个顶点：左上，绿色
                .put(Vertex(cos(a1).toFloat(), sin(a1).toFloat(), 0.0f, 0xff00ff00.toInt()))
                // 第三个顶点：左下，蓝色
                .put(Vertex(cos(a2).toFloat(), sin(a2).toFloat(), 0.0f, 0xff0000ff.toInt()))
                // 重置缓冲区指针到开始位置
                .flip()

        // 声明网格的布局
        vertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)           // 使用1个缓冲区
                .vertexCount(vertexCount) // 顶点数量
                // 因为我们交错存储位置和颜色数据，必须指定偏移和步长
                // 也可以使用分离数据，为每个属性声明不同的缓冲区索引
                .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0, vertexSize)
                .attribute(VertexAttribute.COLOR, 0, AttributeType.UBYTE4, 3 * floatSize, vertexSize)
                // 颜色存储为无符号字节，但在材质（着色器）中需要0-1范围的值
                // 所以必须标记属性为归一化
                .normalized(VertexAttribute.COLOR)
                .build(engine)

        // 将顶点数据传递给网格
        // 只设置1个缓冲区，因为数据是交错的
        vertexBuffer.setBufferAt(engine, 0, vertexData)

        // 创建索引数据
        // 索引定义了如何连接顶点形成三角形
        val indexData = ByteBuffer.allocate(vertexCount * shortSize)
                .order(ByteOrder.nativeOrder())
                .putShort(0)  // 第一个顶点
                .putShort(1)  // 第二个顶点
                .putShort(2)  // 第三个顶点
                .flip()

        // 创建索引缓冲区
        indexBuffer = IndexBuffer.Builder()
                .indexCount(3)  // 3个索引
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)  // 16位无符号短整数
                .build(engine)
        indexBuffer.setBuffer(engine, indexData)
    }

    /**
     * 启动三角形旋转动画
     * 
     * 业务逻辑：
     * 1. 配置动画参数（时长、插值器、重复模式）
     * 2. 设置动画更新监听器
     * 3. 在每帧更新中计算旋转矩阵
     * 4. 应用变换到可渲染实体
     * 
     * Filament 变换系统说明：
     * - TransformManager: 管理实体的变换组件
     * - 变换矩阵定义了物体在3D空间中的位置、旋转和缩放
     * - 每个实体可以有一个变换组件实例
     */
    private fun startAnimation() {
        // 配置三角形动画
        animator.interpolator = LinearInterpolator()  // 线性插值器，匀速动画
        animator.duration = 4000                      // 动画时长4秒
        animator.repeatMode = ValueAnimator.RESTART   // 重复模式：重新开始
        animator.repeatCount = ValueAnimator.INFINITE // 无限重复
        
        // 添加动画更新监听器
        animator.addUpdateListener(object : ValueAnimator.AnimatorUpdateListener {
            // 变换矩阵，用于存储旋转变换
            val transformMatrix = FloatArray(16)
            
            override fun onAnimationUpdate(a: ValueAnimator) {
                // 获取当前动画值（0-360度）
                val angle = a.animatedValue as Float
                
                // 创建绕Z轴的旋转矩阵
                // 负号是因为我们想要逆时针旋转
                Matrix.setRotateM(transformMatrix, 0, -angle, 0.0f, 0.0f, 1.0f)
                
                // 获取变换管理器并应用变换
                val tcm = engine.transformManager
                tcm.setTransform(tcm.getInstance(renderable), transformMatrix)
            }
        })
        
        // 启动动画
        animator.start()
    }

    /**
     * Activity 恢复时的处理
     * 
     * 业务逻辑：
     * 1. 恢复帧调度器，开始渲染循环
     * 2. 重新启动动画
     * 
     * 这确保了当用户返回应用时，渲染和动画能够正常继续
     */
    override fun onResume() {
        super.onResume()
        // 重新开始帧回调，恢复渲染循环
        choreographer.postFrameCallback(frameScheduler)
        // 重新启动动画
        animator.start()
    }

    /**
     * Activity 暂停时的处理
     * 
     * 业务逻辑：
     * 1. 停止帧调度器，暂停渲染循环
     * 2. 取消动画
     * 
     * 这可以节省电池和CPU资源，当应用不可见时停止不必要的计算
     */
    override fun onPause() {
        super.onPause()
        // 移除帧回调，暂停渲染循环
        choreographer.removeFrameCallback(frameScheduler)
        // 取消动画
        animator.cancel()
    }

    /**
     * Activity 销毁时的资源清理
     * 
     * 业务逻辑：
     * 1. 停止所有动画和渲染
     * 2. 分离UI组件
     * 3. 按正确顺序销毁Filament资源
     * 4. 销毁实体
     * 5. 最后销毁引擎
     * 
     * 重要：资源清理的顺序很关键，必须先销毁依赖资源，再销毁被依赖资源
     * Filament 资源管理说明：
     * - 必须显式销毁所有创建的资源
     * - 销毁顺序：组件 -> 实体 -> 引擎
     * - 引擎销毁会清理遗忘的资源，但建议手动清理
     */
    override fun onDestroy() {
        super.onDestroy()

        // 停止动画和任何待处理的帧
        choreographer.removeFrameCallback(frameScheduler)
        animator.cancel()

        // 在销毁引擎之前，始终先分离表面
        // 这确保了Surface不会在Filament还在使用时被Android销毁
        uiHelper.detach()

        // 清理所有Filament资源
        // 注意：必须按依赖关系的逆序销毁
        
        // 1. 销毁实体的组件
        engine.destroyEntity(renderable)           // 销毁可渲染组件
        engine.destroyRenderer(renderer)           // 销毁渲染器
        engine.destroyVertexBuffer(vertexBuffer)   // 销毁顶点缓冲区
        engine.destroyIndexBuffer(indexBuffer)     // 销毁索引缓冲区
        engine.destroyMaterial(material)           // 销毁材质
        engine.destroyView(view)                   // 销毁视图
        engine.destroyScene(scene)                 // 销毁场景
        engine.destroyCameraComponent(camera.entity) // 销毁相机组件

        // 2. 销毁实体本身
        // Engine.destroyEntity() 只销毁Filament相关资源（组件），
        // 不销毁实体本身，需要通过EntityManager销毁
        val entityManager = EntityManager.get()
        entityManager.destroy(renderable)  // 销毁可渲染实体
        entityManager.destroy(camera.entity) // 销毁相机实体

        // 3. 最后销毁引擎
        // 销毁引擎会释放任何你可能忘记销毁的资源，
        // 但建议按正确顺序手动清理
        engine.destroy()
    }

    /**
     * 帧回调类 - 实现渲染循环
     * 
     * 这个内部类负责：
     * 1. 调度下一帧的渲染
     * 2. 检查渲染就绪状态
     * 3. 执行实际的渲染操作
     * 
     * Filament 渲染循环说明：
     * - Choreographer 确保渲染与显示刷新率同步
     * - SwapChain 管理双缓冲
     * - beginFrame/endFrame 包围实际的渲染调用
     */
    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            // 调度下一帧
            // 这创建了一个持续的渲染循环
            choreographer.postFrameCallback(this)

            // 检查是否有可用的交换链
            // 这确保Surface已经准备好进行渲染
            if (uiHelper.isReadyToRender) {
                // 开始帧渲染
                // 如果beginFrame()返回false，应该跳过这一帧
                // 这意味着你向GPU发送帧的速度太快了
                if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    // 渲染视图（包含场景、相机等）
                    renderer.render(view)
                    // 结束帧渲染，交换缓冲区
                    renderer.endFrame()
                }
            }
        }
    }

    /**
     * Surface 回调类 - 处理 Android Surface 生命周期事件
     * 
     * 这个内部类负责：
     * 1. 响应 Surface 创建和变化
     * 2. 管理 SwapChain 的生命周期
     * 3. 处理 Surface 尺寸变化
     * 4. 配置相机投影和视口
     * 
     * Android Surface 系统说明：
     * - Surface 是 Android 的渲染目标
     * - Surface 可能会因为屏幕旋转、应用切换等原因重建
     * - 必须正确处理这些生命周期事件以避免渲染问题
     */
    inner class SurfaceCallback : UiHelper.RendererCallback {
        
        /**
         * 当 Native Window (Surface) 改变时调用
         * 
         * 业务逻辑：
         * 1. 销毁旧的 SwapChain
         * 2. 根据功能级别配置颜色空间
         * 3. 创建新的 SwapChain
         * 4. 附加显示辅助器
         */
        override fun onNativeWindowChanged(surface: Surface) {
            // 如果已存在 SwapChain，先销毁它
            swapChain?.let { engine.destroySwapChain(it) }

            // 在功能级别0，我们没有后处理，所以需要设置颜色空间为sRGB
            // 注意：sRGB并不是在所有地方都支持
            var flags = uiHelper.swapChainFlags
            if (engine.activeFeatureLevel == Engine.FeatureLevel.FEATURE_LEVEL_0) {
                if (SwapChain.isSRGBSwapChainSupported(engine)) {
                    flags = flags or SwapChainFlags.CONFIG_SRGB_COLORSPACE
                }
            }

            // 使用新的 Surface 创建 SwapChain
            swapChain = engine.createSwapChain(surface, flags)
            
            // 将显示辅助器附加到渲染器和显示器
            displayHelper.attach(renderer, surfaceView.display)
        }

        /**
         * 当从 Surface 分离时调用
         * 
         * 业务逻辑：
         * 1. 分离显示辅助器
         * 2. 销毁 SwapChain
         * 3. 等待 Filament 完成所有操作
         * 
         * 重要：必须等待 Filament 完成销毁操作，
         * 否则 Android 可能会过早销毁 Surface
         */
        override fun onDetachedFromSurface() {
            // 分离显示辅助器
            displayHelper.detach()
            
            // 销毁 SwapChain
            swapChain?.let {
                engine.destroySwapChain(it)
                // 必须确保在返回之前 Filament 已完成执行 destroySwapChain 命令，
                // 否则 Android 可能会过早销毁 Surface
                engine.flushAndWait()
                swapChain = null
            }
        }

        /**
         * 当 Surface 尺寸改变时调用
         * 
         * 业务逻辑：
         * 1. 计算新的宽高比
         * 2. 更新相机投影矩阵
         * 3. 设置新的视口尺寸
         * 4. 同步待处理的帧
         * 
         * 相机投影说明：
         * - 使用正交投影（ORTHO）而不是透视投影
         * - 正交投影保持物体大小不变，适合2D效果
         * - 投影参数：左、右、下、上、近、远平面
         */
        override fun onResized(width: Int, height: Int) {
            // 设置缩放因子
            val zoom = 1.5
            // 计算宽高比
            val aspect = width.toDouble() / height.toDouble()
            
            // 设置正交投影
            // 参数：投影类型、左、右、下、上、近平面、远平面
            camera.setProjection(Camera.Projection.ORTHO,
                    -aspect * zoom, aspect * zoom, -zoom, zoom, 0.0, 10.0)

            // 设置视口尺寸
            // 视口定义了渲染区域在屏幕上的位置和大小
            view.viewport = Viewport(0, 0, width, height)

            // 同步待处理的帧，确保尺寸变化立即生效
            FilamentHelper.synchronizePendingFrames(engine)
        }
    }

    /**
     * 从 Assets 读取未压缩的资源文件
     * 
     * 业务逻辑：
     * 1. 打开 assets 文件描述符
     * 2. 创建输入流
     * 3. 分配目标缓冲区
     * 4. 通过 NIO Channel 读取数据
     * 5. 重置缓冲区指针
     * 
     * 这个方法专门用于读取 Filament 的二进制资源文件，
     * 如预编译的材质文件 (.filamat)、网格文件等
     * 
     * @param assetName assets 目录中的文件名
     * @return 包含文件数据的 ByteBuffer
     */
    private fun readUncompressedAsset(assetName: String): ByteBuffer {
        // 使用 use 确保资源自动关闭
        assets.openFd(assetName).use { fd ->
            // 创建输入流
            val input = fd.createInputStream()
            // 根据文件大小分配缓冲区
            val dst = ByteBuffer.allocate(fd.length.toInt())

            // 使用 NIO Channel 进行高效的数据传输
            val src = Channels.newChannel(input)
            src.read(dst)  // 读取数据到缓冲区
            src.close()    // 关闭源通道

            // 重置缓冲区指针到开始位置，准备读取
            return dst.apply { rewind() }
        }
    }
}
