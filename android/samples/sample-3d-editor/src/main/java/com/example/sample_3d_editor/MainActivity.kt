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

class MainActivity : Activity() {
    // Make sure to initialize Filament first
    companion object {
        init {
            Filament.init()
        }
    }

    // UI components
    private lateinit var surfaceView: SurfaceView
    private lateinit var infoText: TextView
    private lateinit var rootLayout: ConstraintLayout
    
    // Filament components
    private lateinit var uiHelper: UiHelper
    private lateinit var displayHelper: DisplayHelper
    private lateinit var choreographer: Choreographer
    private lateinit var engine: Engine
    private lateinit var renderer: Renderer
    private lateinit var scene: Scene
    private lateinit var view: View
    private lateinit var camera: Camera

    // Materials
//    private lateinit var litMaterial: Material
//    private lateinit var unlitMaterial: Material
//    private lateinit var cubeMaterialInstance: MaterialInstance
//    private lateinit var axisMaterialInstance: MaterialInstance

    // Geometry
    private lateinit var cubeVertexBuffer: VertexBuffer
    private lateinit var cubeIndexBuffer: IndexBuffer
    private lateinit var axisVertexBuffer: VertexBuffer
    private lateinit var axisIndexBuffer: IndexBuffer

    // Entities
    @Entity private var cubeRenderable = 0
    @Entity private var axisRenderable = 0
    @Entity private var light = 0

    private var swapChain: SwapChain? = null
    private val frameScheduler = FrameCallback()
    private val animator = ValueAnimator.ofFloat(0.0f, 360.0f)

    // Camera control
    private var cameraDistance = 8.0f
    private var cameraAngleX = 30.0f
    private var cameraAngleY = 45.0f
    private val cameraMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Create UI
        setupUI()
        
        choreographer = Choreographer.getInstance()
        displayHelper = DisplayHelper(this)

        setupSurfaceView()
        setupFilament()
        setupView()
        setupScene()
    }

    private fun setupUI() {
        rootLayout = ConstraintLayout(this)
        rootLayout.setBackgroundColor(0xFF000000.toInt())
        
        surfaceView = SurfaceView(this)
        val surfaceParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        surfaceView.layoutParams = surfaceParams
        rootLayout.addView(surfaceView)
        
        infoText = TextView(this)
        infoText.text = "3D坐标轴演示\n点击坐标轴改变视角\n拖拽旋转视图"
        infoText.setTextColor(0xFFFFFFFF.toInt())
        infoText.textSize = 12f
        infoText.setPadding(24, 24, 24, 24)
        infoText.setBackgroundColor(0x80000000.toInt())
        
        val textParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.WRAP_CONTENT,
            ConstraintLayout.LayoutParams.WRAP_CONTENT
        )
        textParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        textParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
        textParams.setMargins(48, 48, 0, 0)
        infoText.layoutParams = textParams
        rootLayout.addView(infoText)
        
        setContentView(rootLayout)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSurfaceView() {
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = SurfaceCallback()
        uiHelper.attachTo(surfaceView)
        
        // Add touch listener for camera control
        surfaceView.setOnTouchListener { _, event ->
            handleTouch(event)
        }
    }

    private fun setupFilament() {
        engine = Engine.create()
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())
    }

    private fun setupView() {
        scene.skybox = Skybox.Builder().color(0.1f, 0.1f, 0.1f, 1.0f).build(engine)
        view.camera = camera
        view.scene = scene
        updateCamera()
    }

    private fun setupScene() {
//        loadMaterials()
//        setupMaterials()
        createCubeMesh()
        createAxisMesh()
        createRenderables()
        setupLighting()
    }

    private fun loadMaterials() {
        // Load lit material for cube
//        readUncompressedAsset("materials/lit.filamat").let {
//            litMaterial = Material.Builder().payload(it, it.remaining()).build(engine)
//        }
//
//        // Load unlit material for axis
//        readUncompressedAsset("materials/unlit.filamat").let {
//            unlitMaterial = Material.Builder().payload(it, it.remaining()).build(engine)
//        }
    }

    private fun setupMaterials() {
        // Cube material (metallic blue)
//        cubeMaterialInstance = litMaterial.createInstance()
//        cubeMaterialInstance.setParameter("baseColor", Colors.RgbType.SRGB, 0.2f, 0.5f, 1.0f)
//        cubeMaterialInstance.setParameter("metallic", 0.8f)
//        cubeMaterialInstance.setParameter("roughness", 0.2f)
//
//        // Axis material (bright colors)
//        axisMaterialInstance = unlitMaterial.createInstance()
    }

    private fun createCubeMesh() {
        val floatSize = 4
        val vertexSize = 3 * floatSize + 4 * floatSize // position + tangents
        val vertexCount = 24 // 6 faces * 4 vertices

        data class Vertex(val x: Float, val y: Float, val z: Float, val tangents: FloatArray)
        fun ByteBuffer.put(v: Vertex): ByteBuffer {
            putFloat(v.x)
            putFloat(v.y)
            putFloat(v.z)
            v.tangents.forEach { putFloat(it) }
            return this
        }

        // Create tangent frames for each face
        val tfPX = FloatArray(4)
        val tfNX = FloatArray(4)
        val tfPY = FloatArray(4)
        val tfNY = FloatArray(4)
        val tfPZ = FloatArray(4)
        val tfNZ = FloatArray(4)

        MathUtils.packTangentFrame( 0.0f,  1.0f, 0.0f, 0.0f, 0.0f, -1.0f,  1.0f,  0.0f,  0.0f, tfPX)
        MathUtils.packTangentFrame( 0.0f,  1.0f, 0.0f, 0.0f, 0.0f, -1.0f, -1.0f,  0.0f,  0.0f, tfNX)
        MathUtils.packTangentFrame(-1.0f,  0.0f, 0.0f, 0.0f, 0.0f, -1.0f,  0.0f,  1.0f,  0.0f, tfPY)
        MathUtils.packTangentFrame(-1.0f,  0.0f, 0.0f, 0.0f, 0.0f,  1.0f,  0.0f, -1.0f,  0.0f, tfNY)
        MathUtils.packTangentFrame( 0.0f,  1.0f, 0.0f, 1.0f, 0.0f,  0.0f,  0.0f,  0.0f,  1.0f, tfPZ)
        MathUtils.packTangentFrame( 0.0f, -1.0f, 0.0f, 1.0f, 0.0f,  0.0f,  0.0f,  0.0f, -1.0f, tfNZ)

        val vertexData = ByteBuffer.allocate(vertexCount * vertexSize)
                .order(ByteOrder.nativeOrder())
                // Face -Z
                .put(Vertex(-1.0f, -1.0f, -1.0f, tfNZ))
                .put(Vertex(-1.0f,  1.0f, -1.0f, tfNZ))
                .put(Vertex( 1.0f,  1.0f, -1.0f, tfNZ))
                .put(Vertex( 1.0f, -1.0f, -1.0f, tfNZ))
                // Face +X
                .put(Vertex( 1.0f, -1.0f, -1.0f, tfPX))
                .put(Vertex( 1.0f,  1.0f, -1.0f, tfPX))
                .put(Vertex( 1.0f,  1.0f,  1.0f, tfPX))
                .put(Vertex( 1.0f, -1.0f,  1.0f, tfPX))
                // Face +Z
                .put(Vertex(-1.0f, -1.0f,  1.0f, tfPZ))
                .put(Vertex( 1.0f, -1.0f,  1.0f, tfPZ))
                .put(Vertex( 1.0f,  1.0f,  1.0f, tfPZ))
                .put(Vertex(-1.0f,  1.0f,  1.0f, tfPZ))
                // Face -X
                .put(Vertex(-1.0f, -1.0f,  1.0f, tfNX))
                .put(Vertex(-1.0f,  1.0f,  1.0f, tfNX))
                .put(Vertex(-1.0f,  1.0f, -1.0f, tfNX))
                .put(Vertex(-1.0f, -1.0f, -1.0f, tfNX))
                // Face -Y
                .put(Vertex(-1.0f, -1.0f,  1.0f, tfNY))
                .put(Vertex(-1.0f, -1.0f, -1.0f, tfNY))
                .put(Vertex( 1.0f, -1.0f, -1.0f, tfNY))
                .put(Vertex( 1.0f, -1.0f,  1.0f, tfNY))
                // Face +Y
                .put(Vertex(-1.0f,  1.0f, -1.0f, tfPY))
                .put(Vertex(-1.0f,  1.0f,  1.0f, tfPY))
                .put(Vertex( 1.0f,  1.0f,  1.0f, tfPY))
                .put(Vertex( 1.0f,  1.0f, -1.0f, tfPY))
                .flip()

        cubeVertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)
                .vertexCount(vertexCount)
                .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0, vertexSize)
                .attribute(VertexAttribute.TANGENTS, 0, AttributeType.FLOAT4, 3 * floatSize, vertexSize)
                .build(engine)
        cubeVertexBuffer.setBufferAt(engine, 0, vertexData)

        // Create cube indices
        val shortSize = 2
        val indexData = ByteBuffer.allocate(6 * 2 * 3 * shortSize)
                .order(ByteOrder.nativeOrder())
        repeat(6) {
            val i = (it * 4).toShort()
            indexData
                    .putShort(i).putShort((i + 1).toShort()).putShort((i + 2).toShort())
                    .putShort(i).putShort((i + 2).toShort()).putShort((i + 3).toShort())
        }
        indexData.flip()

        cubeIndexBuffer = IndexBuffer.Builder()
                .indexCount(36)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                .build(engine)
        cubeIndexBuffer.setBuffer(engine, indexData)
    }

    private fun createAxisMesh() {
        val floatSize = 4
        val vertexSize = 3 * floatSize + 4 * floatSize // position + color
        
        // Axis vertices: origin + 3 axis endpoints + 3 small spheres for clickable areas
        val axisLength = 3.0f
        val sphereRadius = 0.3f
        
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

        val axisVertexData = ByteBuffer.allocate(6 * vertexSize)
                .order(ByteOrder.nativeOrder())
                // X axis (red)
                .put(ColorVertex(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f))
                .put(ColorVertex(axisLength, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f))
                // Y axis (green)
                .put(ColorVertex(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f))
                .put(ColorVertex(0.0f, axisLength, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f))
                // Z axis (blue)
                .put(ColorVertex(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f))
                .put(ColorVertex(0.0f, 0.0f, axisLength, 0.0f, 0.0f, 1.0f, 1.0f))
                .flip()

        axisVertexBuffer = VertexBuffer.Builder()
                .bufferCount(1)
                .vertexCount(6)
                .attribute(VertexAttribute.POSITION, 0, AttributeType.FLOAT3, 0, vertexSize)
                .attribute(VertexAttribute.COLOR, 0, AttributeType.FLOAT4, 3 * floatSize, vertexSize)
                .build(engine)
        axisVertexBuffer.setBufferAt(engine, 0, axisVertexData)

        // Create axis indices (3 lines)
        val indexData = ByteBuffer.allocate(6 * 2)
                .order(ByteOrder.nativeOrder())
                .putShort(0).putShort(1) // X axis
                .putShort(2).putShort(3) // Y axis
                .putShort(4).putShort(5) // Z axis
                .flip()

        axisIndexBuffer = IndexBuffer.Builder()
                .indexCount(6)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                .build(engine)
        axisIndexBuffer.setBuffer(engine, indexData)
    }

    private fun createRenderables() {
        // Create cube renderable
        cubeRenderable = EntityManager.get().create()
        RenderableManager.Builder(1)
                .boundingBox(Box(-1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f))
                .geometry(0, PrimitiveType.TRIANGLES, cubeVertexBuffer, cubeIndexBuffer, 0, 36)
//                .material(0, cubeMaterialInstance)
                .build(engine, cubeRenderable)
        scene.addEntity(cubeRenderable)

        // Create axis renderable
        axisRenderable = EntityManager.get().create()
        RenderableManager.Builder(1)
                .boundingBox(Box(-3.0f, -3.0f, -3.0f, 3.0f, 3.0f, 3.0f))
                .geometry(0, PrimitiveType.LINES, axisVertexBuffer, axisIndexBuffer, 0, 6)
//                .material(0, axisMaterialInstance)
                .build(engine, axisRenderable)
        scene.addEntity(axisRenderable)
    }

    private fun setupLighting() {
        light = EntityManager.get().create()
        val (r, g, b) = Colors.cct(6_500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(r, g, b)
                .intensity(120_000.0f)
                .direction(-0.5f, -1.0f, -0.5f)
                .castShadows(true)
                .build(engine, light)
        scene.addEntity(light)
        
        camera.setExposure(16.0f, 1.0f / 125.0f, 100.0f)
    }

    private fun updateCamera() {
        val radX = Math.toRadians(cameraAngleX.toDouble())
        val radY = Math.toRadians(cameraAngleY.toDouble())
        
        val x = (cameraDistance * cos(radX) * cos(radY)).toFloat()
        val y = (cameraDistance * sin(radX)).toFloat()
        val z = (cameraDistance * cos(radX) * sin(radY)).toFloat()
        
        camera.lookAt(x.toDouble(), y.toDouble(), z.toDouble(), 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
    }

    private var lastX = 0f
    private var lastY = 0f
    private var isDragging = false

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                isDragging = true
                
                // Check if clicking on axis endpoints for view switching
                checkAxisClick(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    val deltaX = event.x - lastX
                    val deltaY = event.y - lastY
                    
                    cameraAngleY += deltaX * 0.5f
                    cameraAngleX -= deltaY * 0.5f
                    
                    // Clamp vertical angle
                    cameraAngleX = cameraAngleX.coerceIn(-89f, 89f)
                    
                    updateCamera()
                    
                    lastX = event.x
                    lastY = event.y
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                return true
            }
        }
        return false
    }

    private fun checkAxisClick(x: Float, y: Float) {
        // Simple click detection for axis switching
        val centerX = surfaceView.width / 2f
        val centerY = surfaceView.height / 2f
        
        // Check if click is in the lower portion (where axis would be)
        if (y > centerY + 100) {
            when {
                x < centerX - 50 -> switchToView(-90f, 0f) // X axis view
                x > centerX + 50 -> switchToView(0f, 90f)   // Z axis view
                else -> switchToView(90f, 0f)               // Y axis view
            }
        }
    }

    private fun switchToView(angleX: Float, angleY: Float) {
        // Animate camera to new position
        val startAngleX = cameraAngleX
        val startAngleY = cameraAngleY
        
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                cameraAngleX = startAngleX + (angleX - startAngleX) * progress
                cameraAngleY = startAngleY + (angleY - startAngleY) * progress
                updateCamera()
            }
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameScheduler)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameScheduler)
    }

    override fun onDestroy() {
        super.onDestroy()
        
        choreographer.removeFrameCallback(frameScheduler)
        uiHelper.detach()
        
        // Cleanup resources
        engine.destroyEntity(light)
        engine.destroyEntity(cubeRenderable)
        engine.destroyEntity(axisRenderable)
        engine.destroyRenderer(renderer)
        engine.destroyVertexBuffer(cubeVertexBuffer)
        engine.destroyIndexBuffer(cubeIndexBuffer)
        engine.destroyVertexBuffer(axisVertexBuffer)
        engine.destroyIndexBuffer(axisIndexBuffer)
//        engine.destroyMaterialInstance(cubeMaterialInstance)
//        engine.destroyMaterialInstance(axisMaterialInstance)
//        engine.destroyMaterial(litMaterial)
//        engine.destroyMaterial(unlitMaterial)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)
        
        val entityManager = EntityManager.get()
        entityManager.destroy(light)
        entityManager.destroy(cubeRenderable)
        entityManager.destroy(axisRenderable)
        entityManager.destroy(camera.entity)
        
        engine.destroy()
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            
            if (uiHelper.isReadyToRender) {
                if (renderer.beginFrame(swapChain!!, frameTimeNanos)) {
                    renderer.render(view)
                    renderer.endFrame()
                }
            }
        }
    }

    inner class SurfaceCallback : UiHelper.RendererCallback {
        override fun onNativeWindowChanged(surface: Surface) {
            swapChain?.let { engine.destroySwapChain(it) }
            swapChain = engine.createSwapChain(surface)
            displayHelper.attach(renderer, surfaceView.display)
        }

        override fun onDetachedFromSurface() {
            displayHelper.detach()
            swapChain?.let {
                engine.destroySwapChain(it)
                engine.flushAndWait()
                swapChain = null
            }
        }

        override fun onResized(width: Int, height: Int) {
            val aspect = width.toDouble() / height.toDouble()
            camera.setProjection(45.0, aspect, 0.1, 20.0, Camera.Fov.VERTICAL)
            view.viewport = Viewport(0, 0, width, height)
            FilamentHelper.synchronizePendingFrames(engine)
        }
    }

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
