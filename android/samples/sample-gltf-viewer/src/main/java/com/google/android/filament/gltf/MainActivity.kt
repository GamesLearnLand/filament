/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.google.android.filament.gltf

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.GestureDetector
import android.widget.TextView
import android.widget.Toast
import com.google.android.filament.Fence
import com.google.android.filament.IndirectLight
import com.google.android.filament.Material
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.View.OnPickCallback
import com.google.android.filament.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.net.URI
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.zip.ZipInputStream

/**
 * GLTF模型查看器主Activity，展示如何使用Filament引擎加载和渲染3D模型
 * 
 * 主要功能：
 * 1. 初始化Filament引擎和gltfio组件
 * 2. 加载默认GLTF模型和光照环境
 * 3. 提供触摸交互和远程服务器功能
 * 4. 管理模型加载和渲染生命周期
 */
class MainActivity : Activity() {

    companion object {
        // 加载工具层库，该库会进一步加载gltfio和Filament核心库
        // 注意：必须在任何Filament API调用前初始化
        init { Utils.init() }
        private const val TAG = "gltf-viewer" // 日志标签
    }

    // SurfaceView用于显示Filament渲染内容
    private lateinit var surfaceView: SurfaceView
    
    // 用于协调帧渲染的Choreographer
    private lateinit var choreographer: Choreographer
    
    // 帧回调，处理每帧的渲染逻辑
    private val frameScheduler = FrameCallback()
    
    // ModelViewer是Filament提供的模型查看工具类，封装了引擎、场景、视图等核心组件
    private lateinit var modelViewer: ModelViewer
    
    // UI相关变量
    private lateinit var titlebarHint: TextView
    private val doubleTapListener = DoubleTapListener()
    private val singleTapListener = SingleTapListener()
    private lateinit var doubleTapDetector: GestureDetector
    private lateinit var singleTapDetector: GestureDetector
    
    // 远程服务器相关变量，用于接收远程模型文件
    private var remoteServer: RemoteServer? = null
    private var statusToast: Toast? = null
    private var statusText: String? = null
    private var latestDownload: String? = null
    
    // 自动化测试引擎
    private val automation = AutomationEngine()
    
    // 模型加载计时相关变量
    private var loadStartTime = 0L
    private var loadStartFence: Fence? = null
    
    // 查看器内容容器，保存当前场景的各种组件
    private val viewerContent = AutomationEngine.ViewerContent()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.simple_layout)
        
        // 保持屏幕常亮，避免渲染中断
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 初始化UI组件
        titlebarHint = findViewById(R.id.user_hint)
        surfaceView = findViewById(R.id.main_sv)
        choreographer = Choreographer.getInstance()

        // 设置手势检测器，用于双击重新加载模型和单点选择
        doubleTapDetector = GestureDetector(applicationContext, doubleTapListener)
        singleTapDetector = GestureDetector(applicationContext, singleTapListener)

        // 初始化ModelViewer，这会创建Filament引擎、渲染器、场景和视图
        modelViewer = ModelViewer(surfaceView)
        
        // 配置查看器内容
        viewerContent.view = modelViewer.view
        viewerContent.sunlight = modelViewer.light
        viewerContent.lightManager = modelViewer.engine.lightManager
        viewerContent.scene = modelViewer.scene
        viewerContent.renderer = modelViewer.renderer

        // 设置触摸监听器，将触摸事件传递给ModelViewer和手势检测器
        surfaceView.setOnTouchListener { _, event ->
            modelViewer.onTouchEvent(event)  // 处理模型旋转/缩放等手势
            doubleTapDetector.onTouchEvent(event)  // 处理双击事件
            singleTapDetector.onTouchEvent(event)  // 处理单点选择
            true
        }

        // 加载默认模型和光照环境
        createDefaultRenderables()
        createIndirectLight()

        // 设置初始状态文本
        setStatusText("To load a new model, go to the above URL on your host machine.")

        // 获取Filament视图对象，用于配置渲染参数
        val view = modelViewer.view

        /*
         * Note: The settings below are overriden when connecting to the remote UI.
         */

        // on mobile, better use lower quality color buffer
        view.renderQuality = view.renderQuality.apply {
            hdrColorBuffer = View.QualityLevel.MEDIUM
        }

        // dynamic resolution often helps a lot
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            enabled = true
            quality = View.QualityLevel.MEDIUM
        }

        // MSAA is needed with dynamic resolution MEDIUM
        view.multiSampleAntiAliasingOptions = view.multiSampleAntiAliasingOptions.apply {
            enabled = true
        }

        // FXAA is pretty cheap and helps a lot
        view.antiAliasing = View.AntiAliasing.FXAA

        // ambient occlusion is the cheapest effect that adds a lot of quality
        view.ambientOcclusionOptions = view.ambientOcclusionOptions.apply {
            enabled = true
        }

        // bloom is pretty expensive but adds a fair amount of realism
        view.bloomOptions = view.bloomOptions.apply {
            enabled = true
        }

        remoteServer = RemoteServer(8082)
    }

    /**
     * 创建默认的可渲染对象，加载assets中的GLTF模型
     * 
     * 流程：
     * 1. 从assets读取GLTF文件到ByteBuffer
     * 2. 异步加载GLTF模型，自动处理纹理、材质等资源
     * 3. 更新模型根变换，使其适配视图
     * 
     * 注意：GLTF是Khronos Group制定的3D模型标准格式，Filament通过gltfio组件提供原生支持
     */
    private fun createDefaultRenderables() {
        // 从assets读取GLTF文件到内存缓冲区
        val buffer = assets.open("models/scene.gltf").use { input ->
            val bytes = ByteArray(input.available())
            input.read(bytes)
            ByteBuffer.wrap(bytes)
        }

        // 异步加载GLTF模型，第二个参数是资源加载器，用于加载模型引用的外部资源
        modelViewer.loadModelGltfAsync(buffer) { uri -> 
            readCompressedAsset("models/$uri") 
        }
        
        // 调整模型位置和缩放，使其在视图中正确显示
        updateRootTransform()
    }

    /**
     * 创建间接光照环境，包括IBL(基于图像的照明)和天空盒
     * 
     * Filament使用基于物理的渲染(PBR)，需要IBL提供环境光照
     * 流程：
     * 1. 从KTX文件加载预计算的IBL立方体贴图
     * 2. 创建IndirectLight对象并设置到场景
     * 3. 加载天空盒纹理，为场景提供背景
     * 
     * @note IBL强度设置为30000，这是为了匹配Filament的物理光照单位
     */
    private fun createIndirectLight() {
        val engine = modelViewer.engine
        val scene = modelViewer.scene
        val ibl = "default_env" // 预设的环境贴图名称
        
        // 加载IBL(基于图像的照明)立方体贴图
        readCompressedAsset("envs/$ibl/${ibl}_ibl.ktx").let {
            // 使用KTX加载器创建IndirectLight资源包
            val bundle = KTX1Loader.createIndirectLight(engine, it)
            scene.indirectLight = bundle.indirectLight
            modelViewer.indirectLightCubemap = bundle.cubemap
            // 设置光照强度，Filament使用物理正确的光照单位
            scene.indirectLight!!.intensity = 30_000.0f
            viewerContent.indirectLight = modelViewer.scene.indirectLight
        }
        
        // 加载天空盒纹理
        readCompressedAsset("envs/$ibl/${ibl}_skybox.ktx").let {
            val bundle = KTX1Loader.createSkybox(engine, it)
            scene.skybox = bundle.skybox
            modelViewer.skyboxCubemap = bundle.cubemap
        }
    }

    /**
     * 从assets读取压缩资源到ByteBuffer
     * 
     * @param assetName assets中的资源路径
     * @return 包含资源数据的ByteBuffer
     * 
     * @note 这个方法用于加载GLTF模型引用的二进制资源(如纹理、二进制数据)
     *       使用ByteBuffer而不是InputStream，因为Filament的加载器需要直接内存访问
     */
    private fun readCompressedAsset(assetName: String): ByteBuffer {
        val input = assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun clearStatusText() {
        statusToast?.let {
            it.cancel()
            statusText = null
        }
    }

    private fun setStatusText(text: String) {
        runOnUiThread {
            if (statusToast == null || statusText != text) {
                statusText = text
                statusToast = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT)
                statusToast!!.show()

            }
        }
    }

    private suspend fun loadGlb(message: RemoteServer.ReceivedMessage) {
        withContext(Dispatchers.Main) {
            modelViewer.destroyModel()
            modelViewer.loadModelGlb(message.buffer)
            updateRootTransform()
            loadStartTime = System.nanoTime()
            loadStartFence = modelViewer.engine.createFence()
        }
    }

    private suspend fun loadHdr(message: RemoteServer.ReceivedMessage) {
        withContext(Dispatchers.Main) {
            val engine = modelViewer.engine
            val equirect = HDRLoader.createTexture(engine, message.buffer)
            if (equirect == null) {
                setStatusText("Could not decode HDR file.")
            } else {
                setStatusText("Successfully decoded HDR file.")

                val context = IBLPrefilterContext(engine)
                val equirectToCubemap = IBLPrefilterContext.EquirectangularToCubemap(context)
                val skyboxTexture = equirectToCubemap.run(equirect)!!
                engine.destroyTexture(equirect)

                val specularFilter = IBLPrefilterContext.SpecularFilter(context)
                val reflections = specularFilter.run(skyboxTexture)

                val ibl = IndirectLight.Builder()
                         .reflections(reflections)
                         .intensity(30000.0f)
                         .build(engine)

                val sky = Skybox.Builder().environment(skyboxTexture).build(engine)

                specularFilter.destroy()
                equirectToCubemap.destroy()
                context.destroy()

                // destroy the previous IBl
                engine.destroyIndirectLight(modelViewer.scene.indirectLight!!)
                engine.destroySkybox(modelViewer.scene.skybox!!)

                modelViewer.scene.skybox = sky
                modelViewer.scene.indirectLight = ibl
                viewerContent.indirectLight = ibl

            }
        }
    }

    private suspend fun loadZip(message: RemoteServer.ReceivedMessage) {
        // To alleviate memory pressure, remove the old model before deflating the zip.
        withContext(Dispatchers.Main) {
            modelViewer.destroyModel()
        }

        // Large zip files should first be written to a file to prevent OOM.
        // It is also crucial that we null out the message "buffer" field.
        val (zipStream, zipFile) = withContext(Dispatchers.IO) {
            val file = File.createTempFile("incoming", "zip", cacheDir)
            val raf = RandomAccessFile(file, "rw")
            raf.channel.write(message.buffer)
            message.buffer = null
            raf.seek(0)
            Pair(FileInputStream(file), file)
        }

        // Deflate each resource using the IO dispatcher, one by one.
        var gltfPath: String? = null
        var outOfMemory: String? = null
        val pathToBufferMapping = withContext(Dispatchers.IO) {
            val deflater = ZipInputStream(zipStream)
            val mapping = HashMap<String, Buffer>()
            while (true) {
                val entry = deflater.nextEntry ?: break
                if (entry.isDirectory) continue

                // This isn't strictly required, but as an optimization
                // we ignore common junk that often pollutes ZIP files.
                if (entry.name.startsWith("__MACOSX")) continue
                if (entry.name.startsWith(".DS_Store")) continue

                val uri = entry.name
                val byteArray: ByteArray? = try {
                    deflater.readBytes()
                }
                catch (e: OutOfMemoryError) {
                    outOfMemory = uri
                    break
                }
                Log.i(TAG, "Deflated ${byteArray!!.size} bytes from $uri")
                val buffer = ByteBuffer.wrap(byteArray)
                mapping[uri] = buffer
                if (uri.endsWith(".gltf") || uri.endsWith(".glb")) {
                    gltfPath = uri
                }
            }
            mapping
        }

        zipFile.delete()

        if (gltfPath == null) {
            setStatusText("Could not find .gltf or .glb in the zip.")
            return
        }

        if (outOfMemory != null) {
            setStatusText("Out of memory while deflating $outOfMemory")
            return
        }

        val gltfBuffer = pathToBufferMapping[gltfPath]!!

        // In a zip file, the gltf file might be in the same folder as resources, or in a different
        // folder. It is crucial to test against both of these cases. In any case, the resource
        // paths are all specified relative to the location of the gltf file.
        var prefix = URI(gltfPath!!).resolve(".")

        withContext(Dispatchers.Main) {
            if (gltfPath!!.endsWith(".glb")) {
                modelViewer.loadModelGlb(gltfBuffer)
            } else {
                modelViewer.loadModelGltf(gltfBuffer) { uri ->
                    val path = prefix.resolve(uri).toString()
                    if (!pathToBufferMapping.contains(path)) {
                        Log.e(TAG, "Could not find '$uri' in zip using prefix '$prefix' and base path '${gltfPath!!}'")
                        setStatusText("Zip is missing $path")
                    }
                    pathToBufferMapping[path]
                }
            }
            updateRootTransform()
            loadStartTime = System.nanoTime()
            loadStartFence = modelViewer.engine.createFence()
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
        remoteServer?.close()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    fun loadModelData(message: RemoteServer.ReceivedMessage) {
        Log.i(TAG, "Downloaded model ${message.label} (${message.buffer.capacity()} bytes)")
        clearStatusText()
        titlebarHint.text = message.label
        CoroutineScope(Dispatchers.IO).launch {
            when {
                message.label.endsWith(".zip") -> loadZip(message)
                message.label.endsWith(".hdr") -> loadHdr(message)
                else -> loadGlb(message)
            }
        }
    }

    fun loadSettings(message: RemoteServer.ReceivedMessage) {
        val json = StandardCharsets.UTF_8.decode(message.buffer).toString()
        viewerContent.assetLights = modelViewer.asset?.lightEntities
        automation.applySettings(modelViewer.engine, json, viewerContent)
        modelViewer.view.colorGrading = automation.getColorGrading(modelViewer.engine)
        modelViewer.cameraFocalLength = automation.viewerOptions.cameraFocalLength
        modelViewer.cameraNear = automation.viewerOptions.cameraNear
        modelViewer.cameraFar = automation.viewerOptions.cameraFar
        updateRootTransform()
    }

    private fun updateRootTransform() {
        if (automation.viewerOptions.autoScaleEnabled) {
            modelViewer.transformToUnitCube()
        } else {
            modelViewer.clearRootTransform()
        }
    }

    inner class FrameCallback : Choreographer.FrameCallback {
        private val startTime = System.nanoTime()
        override fun doFrame(frameTimeNanos: Long) {
            choreographer.postFrameCallback(this)

            loadStartFence?.let {
                if (it.wait(Fence.Mode.FLUSH, 0) == Fence.FenceStatus.CONDITION_SATISFIED) {
                    val end = System.nanoTime()
                    val total = (end - loadStartTime) / 1_000_000
                    Log.i(TAG, "The Filament backend took $total ms to load the model geometry.")
                    modelViewer.engine.destroyFence(it)
                    loadStartFence = null

                    val materials = mutableSetOf<Material>()
                    val rcm = modelViewer.engine.renderableManager
                    modelViewer.scene.forEach {
                        val entity = it
                        if (rcm.hasComponent(entity)) {
                            val ri = rcm.getInstance(entity)
                            val c = rcm.getPrimitiveCount(ri)
                            for (i in 0 until c) {
                                val mi = rcm.getMaterialInstanceAt(ri, i)
                                val ma = mi.material
                                materials.add(ma)
                            }
                        }
                    }
                    materials.forEach {
                        it.compile(
                            Material.CompilerPriorityQueue.HIGH,
                            Material.UserVariantFilterBit.DIRECTIONAL_LIGHTING or
                            Material.UserVariantFilterBit.DYNAMIC_LIGHTING or
                            Material.UserVariantFilterBit.SHADOW_RECEIVER,
                            null, null)
                        it.compile(
                            Material.CompilerPriorityQueue.LOW,
                            Material.UserVariantFilterBit.FOG or
                            Material.UserVariantFilterBit.SKINNING or
                            Material.UserVariantFilterBit.SSR or
                            Material.UserVariantFilterBit.VSM,
                            null, null)
                    }
                }
            }

            modelViewer.animator?.apply {
                if (animationCount > 0) {
                    val elapsedTimeSeconds = (frameTimeNanos - startTime).toDouble() / 1_000_000_000
                    applyAnimation(0, elapsedTimeSeconds.toFloat())
                }
                updateBoneMatrices()
            }

            modelViewer.render(frameTimeNanos)

            // Check if a new download is in progress. If so, let the user know with toast.
            val currentDownload = remoteServer?.peekIncomingLabel()
            if (RemoteServer.isBinary(currentDownload) && currentDownload != latestDownload) {
                latestDownload = currentDownload
                Log.i(TAG, "Downloading $currentDownload")
                setStatusText("Downloading $currentDownload")
            }

            // Check if a new message has been fully received from the client.
            val message = remoteServer?.acquireReceivedMessage()
            if (message != null) {
                if (message.label == latestDownload) {
                    latestDownload = null
                }
                if (RemoteServer.isJson(message.label)) {
                    loadSettings(message)
                } else {
                    loadModelData(message)
                }
            }
        }
    }

    // Just for testing purposes, this releases the current model and reloads the default model.
    inner class DoubleTapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            modelViewer.destroyModel()
            createDefaultRenderables()
            return super.onDoubleTap(e)
        }
    }

    // Just for testing purposes
    inner class SingleTapListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(event: MotionEvent): Boolean {
            modelViewer.view.pick(
                event.x.toInt(),
                surfaceView.height - event.y.toInt(),
                surfaceView.handler, {
                    val name = modelViewer.asset!!.getName(it.renderable)
                    Log.v("Filament", "Picked ${it.renderable}: " + name)
                },
            )
            return super.onSingleTapUp(event)
        }
    }
}
