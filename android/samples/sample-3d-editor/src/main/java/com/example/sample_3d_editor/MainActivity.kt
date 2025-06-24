package com.example.sample_3d_editor

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.filament.*
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.android.filament.utils.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    companion object {
        init {
            Utils.init()
        }

        private const val TAG = "3d-editor"
    }

    private lateinit var surfaceView: SurfaceView
    private lateinit var choreographer: Choreographer
    private lateinit var modelViewer: ModelViewer
    private lateinit var infoText: TextView
    private val frameScheduler = FrameCallback()

    // 3D objects
    private val axisEntities = mutableListOf<Int>()
    private val elementEntities = mutableListOf<Int>()
    private val localAxisEntities = mutableListOf<Int>()
    private var selectedElement: Int = -1

    // Touch handling
//    private lateinit var gestureDetector: GestureDetector
    private var isDragging = false
    private var dragStartX = 0f
    private var dragStartY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.surface_view)
        infoText = findViewById(R.id.info_text)
        choreographer = Choreographer.getInstance()

        modelViewer = ModelViewer(surfaceView)

        // Setup gesture detection
//        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
//            override fun onSingleTapUp(e: MotionEvent): Boolean {
//                handleTap(e.x, e.y)
//                return true
//            }
//
//            override fun onDown(e: MotionEvent): Boolean {
//                dragStartX = e.x
//                dragStartY = e.y
//                return true
//            }
//        })

        surfaceView.setOnTouchListener { _, event ->
            modelViewer.onTouchEvent(event)
//            gestureDetector.onTouchEvent(event)

            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (selectedElement != -1 && !isDragging) {
                        val dx = abs(event.x - dragStartX)
                        val dy = abs(event.y - dragStartY)
                        if (dx > 20 || dy > 20) {
                            isDragging = true
                            handleDrag(event.x, event.y)
                        }
                    } else if (isDragging) {
                        handleDrag(event.x, event.y)
                    }
                }

                MotionEvent.ACTION_UP -> {
                    isDragging = false
                }
            }
            true
        }

        setupScene()
        createCoordinateSystem()
        createElements()

        // Start rendering
        choreographer.postFrameCallback(frameScheduler)
    }

    private fun setupScene() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene

        // Setup lighting
        val (r, g, b) = Colors.cct(6500.0f)
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(r, g, b)
            .intensity(100_000.0f)
            .direction(0.0f, -1.0f, 0.0f)
            .castShadows(true)
            .build(engine, modelViewer.light)

        // Setup camera
        val camera = modelViewer.camera
        camera.setProjection(
            45.0,
            surfaceView.width.toDouble() / surfaceView.height.toDouble(),
            0.1,
            1000.0,
            Camera.Fov.VERTICAL
        )

//        // Position camera to view the coordinate system
//        modelViewer.cameraManipulator.setBookmark(Manipulator.Bookmark().apply {
//            setEyePosition(5.0f, 5.0f, 5.0f)
//            setTargetPosition(0.0f, 0.0f, 0.0f)
//            setUpVector(0.0f, 1.0f, 0.0f)
//        })
    }

    private fun createCoordinateSystem() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene

        // Create coordinate axes
        createAxis(Float3(1.0f, 0.0f, 0.0f), Float3(1.0f, 0.0f, 0.0f)) // X-axis (red)
        createAxis(Float3(0.0f, 1.0f, 0.0f), Float3(0.0f, 1.0f, 0.0f)) // Y-axis (green)
        createAxis(Float3(0.0f, 0.0f, 1.0f), Float3(0.0f, 0.0f, 1.0f)) // Z-axis (blue)
    }

    private fun createAxis(direction: Float3, color: Float3) {
        val engine = modelViewer.engine
        val scene = modelViewer.scene

        // Create line geometry
        val vertices = floatArrayOf(
            0.0f, 0.0f, 0.0f,  // Start point
            direction.x * 3.0f, direction.y * 3.0f, direction.z * 3.0f  // End point
        )

        val indices = shortArrayOf(0, 1)

        val vertexBuffer = VertexBuffer.Builder()
            .vertexCount(2)
            .bufferCount(1)
            .attribute(VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
            .build(engine)

        val indexBuffer = IndexBuffer.Builder()
            .indexCount(2)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)

        vertexBuffer.setBufferAt(
            engine, 0,
            ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices)
                .flip()
        )

        indexBuffer.setBuffer(
            engine,
            ByteBuffer.allocateDirect(indices.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(indices)
                .flip()
        )

        // Create material
        val material = Material.Builder()
            .payload(createUnlitMaterial(), 0)
            .build(engine)

        val materialInstance = material.createInstance()
        materialInstance.setParameter("baseColor", color.x, color.y, color.z, 1.0f)

        // Create renderable
        val entity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(0.0f, 0.0f, 0.0f, 3.0f, 3.0f, 3.0f))
            .material(0, materialInstance)
            .geometry(0, RenderableManager.PrimitiveType.LINES, vertexBuffer, indexBuffer)
            .build(engine, entity)

        scene.addEntity(entity)
        axisEntities.add(entity)
    }

    private fun createElements() {
        // Create several elements along the axes
        val positions = arrayOf(
            Float3(2.0f, 0.0f, 0.0f),  // On X-axis
            Float3(0.0f, 2.0f, 0.0f),  // On Y-axis
            Float3(0.0f, 0.0f, 2.0f),  // On Z-axis
            Float3(1.5f, 1.5f, 0.0f), // Between X and Y
            Float3(1.0f, 0.0f, 1.0f)  // Between X and Z
        )

        val colors = arrayOf(
            Float3(1.0f, 0.5f, 0.5f), // Light red
            Float3(0.5f, 1.0f, 0.5f), // Light green
            Float3(0.5f, 0.5f, 1.0f), // Light blue
            Float3(1.0f, 1.0f, 0.5f), // Yellow
            Float3(1.0f, 0.5f, 1.0f)  // Magenta
        )

        for (i in positions.indices) {
            createElement(positions[i], colors[i])
        }
    }

    private fun createElement(position: Float3, color: Float3) {
        val engine = modelViewer.engine
        val scene = modelViewer.scene

        // Create cube geometry
        val size = 0.2f
        val vertices = floatArrayOf(
            // Front face
            -size, -size, size,
            size, -size, size,
            size, size, size,
            -size, size, size,
            // Back face
            -size, -size, -size,
            -size, size, -size,
            size, size, -size,
            size, -size, -size
        )

        val indices = shortArrayOf(
            0, 1, 2, 0, 2, 3,    // Front
            4, 5, 6, 4, 6, 7,    // Back
            0, 4, 7, 0, 7, 1,    // Bottom
            2, 6, 5, 2, 5, 3,    // Top
            0, 3, 5, 0, 5, 4,    // Left
            1, 7, 6, 1, 6, 2     // Right
        )

        val vertexBuffer = VertexBuffer.Builder()
            .vertexCount(8)
            .bufferCount(1)
            .attribute(VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
            .build(engine)

        val indexBuffer = IndexBuffer.Builder()
            .indexCount(36)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)

        vertexBuffer.setBufferAt(
            engine, 0,
            ByteBuffer.allocateDirect(vertices.size * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertices)
                .flip()
        )

        indexBuffer.setBuffer(
            engine,
            ByteBuffer.allocateDirect(indices.size * 2)
                .order(ByteOrder.nativeOrder())
                .asShortBuffer()
                .put(indices)
                .flip()
        )

        // Create material
        val material = Material.Builder()
            .payload(createUnlitMaterial(), 0)
            .build(engine)

        val materialInstance = material.createInstance()
        materialInstance.setParameter("baseColor", color.x, color.y, color.z, 1.0f)

        // Create renderable
        val entity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(-size, -size, -size, size, size, size))
//            .material(0, materialInstance)
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vertexBuffer, indexBuffer)
            .build(engine, entity)

        // Set transform
        val tm = engine.transformManager
        val transform = tm.getInstance(entity)
        val matrix = translation(position)
        tm.setTransform(transform, matrix.toFloatArray())

        scene.addEntity(entity)
        elementEntities.add(entity)
    }

    private fun handleTap(x: Float, y: Float) {
        val view = modelViewer.view

        // Convert screen coordinates to normalized device coordinates
        val ndcX = (2.0f * x / surfaceView.width) - 1.0f
        val ndcY = 1.0f - (2.0f * y / surfaceView.height)

        // Perform picking
        view.pick(ndcX.toInt(), ndcY.toInt(), null, object : View.OnPickCallback {

            override fun onPick(result: com.google.android.filament.View.PickingQueryResult) {

                if (result.renderable != 0) {
                    val entityIndex = elementEntities.indexOf(result.renderable)
                    if (entityIndex != -1) {
                        selectElement(result.renderable, entityIndex)
                    }
                } else {
                    deselectElement()
                }
            }
        })
    }

    private fun selectElement(entity: Int, index: Int) {
        selectedElement = entity

        // Clear previous local axes
        clearLocalAxes()

        // Get element position
        val tm = modelViewer.engine.transformManager
        val transform = tm.getInstance(entity)
        var matrix = FloatArray(16)
        matrix = tm.getTransform(transform, matrix)
        val position = Float3(matrix[12], matrix[13], matrix[14])

        // Create local coordinate system at element position
        createLocalAxes(position)

        runOnUiThread {
            infoText.text = "已选择元素 ${index + 1}\n显示局部坐标系\n可拖拽移动元素"
        }
    }

    private fun deselectElement() {
        selectedElement = -1
        clearLocalAxes()

        runOnUiThread {
            infoText.text = "3D坐标轴演示\n点击元素查看局部坐标系\n拖拽元素进行移动"
        }
    }

    private fun createLocalAxes(position: Float3) {
        val engine = modelViewer.engine
        val scene = modelViewer.scene
        val size = 1.0f

        // Create smaller local axes
        val axes = arrayOf(
            Pair(Float3(1.0f, 0.0f, 0.0f), Float3(1.0f, 0.3f, 0.3f)), // X-axis
            Pair(Float3(0.0f, 1.0f, 0.0f), Float3(0.3f, 1.0f, 0.3f)), // Y-axis
            Pair(Float3(0.0f, 0.0f, 1.0f), Float3(0.3f, 0.3f, 1.0f))  // Z-axis
        )

        for ((direction, color) in axes) {
            val vertices = floatArrayOf(
                position.x,
                position.y,
                position.z,
                position.x + direction.x * size,
                position.y + direction.y * size,
                position.z + direction.z * size
            )

            val indices = shortArrayOf(0, 1)

            val vertexBuffer = VertexBuffer.Builder()
                .vertexCount(2)
                .bufferCount(1)
                .attribute(VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 12)
                .build(engine)

            val indexBuffer = IndexBuffer.Builder()
                .indexCount(2)
                .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                .build(engine)

            vertexBuffer.setBufferAt(
                engine, 0,
                ByteBuffer.allocateDirect(vertices.size * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
                    .put(vertices)
                    .flip()
            )

            indexBuffer.setBuffer(
                engine,
                ByteBuffer.allocateDirect(indices.size * 2)
                    .order(ByteOrder.nativeOrder())
                    .asShortBuffer()
                    .put(indices)
                    .flip()
            )

            val material = Material.Builder()
                .payload(createUnlitMaterial(), 0)
                .build(engine)

            val materialInstance = material.createInstance()
            materialInstance.setParameter("baseColor", color.x, color.y, color.z, 1.0f)

            val entity = EntityManager.get().create()
            RenderableManager.Builder(1)
                .boundingBox(
                    Box(
                        position.x - size, position.y - size, position.z - size,
                        position.x + size, position.y + size, position.z + size
                    )
                )
                .material(0, materialInstance)
                .geometry(0, RenderableManager.PrimitiveType.LINES, vertexBuffer, indexBuffer)
                .build(engine, entity)

            scene.addEntity(entity)
            localAxisEntities.add(entity)
        }
    }

    private fun clearLocalAxes() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene

        for (entity in localAxisEntities) {
            scene.removeEntity(entity)
            engine.destroyEntity(entity)
        }
        localAxisEntities.clear()
    }

    private fun handleDrag(x: Float, y: Float) {
        if (selectedElement == -1) return

        // Simple drag implementation - move element based on screen movement
        val deltaX = (x - dragStartX) * 0.01f
        val deltaY = (dragStartY - y) * 0.01f // Invert Y

        val tm = modelViewer.engine.transformManager
        val transform = tm.getInstance(selectedElement)
        var currentMatrix = FloatArray(16)
        currentMatrix = tm.getTransform(transform, currentMatrix)

        // Update position
        val newMatrix = currentMatrix.clone()
        newMatrix[12] += deltaX
        newMatrix[13] += deltaY

        tm.setTransform(transform, newMatrix)

        // Update local axes if visible
        if (localAxisEntities.isNotEmpty()) {
            clearLocalAxes()
            val position = Float3(newMatrix[12], newMatrix[13], newMatrix[14])
            createLocalAxes(position)
        }

        dragStartX = x
        dragStartY = y
    }

    private fun createUnlitMaterial(): ByteBuffer {
        // Simple unlit material shader
        val materialSource = """
            material {
                name : "unlit",
                shadingModel : unlit,
                parameters : [
                    {
                        type : float4,
                        name : baseColor
                    }
                ]
            }
            
            fragment {
                void material(inout MaterialInputs material) {
                    prepareMaterial(material);
                    material.baseColor = materialParams.baseColor;
                }
            }
        """.trimIndent()

        return ByteBuffer.wrap(materialSource.toByteArray())
    }

    private inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)
            modelViewer.render(frameTimeNanos)
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

        // Cleanup
        clearLocalAxes()

        val engine = modelViewer.engine
        val scene = modelViewer.scene

        for (entity in axisEntities + elementEntities) {
            scene.removeEntity(entity)
            engine.destroyEntity(entity)
        }
    }
}
