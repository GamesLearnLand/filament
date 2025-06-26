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

// IBL（基于图像的光照）相关功能的包
package com.google.android.filament.ibl

// Android资源管理器，用于访问应用程序资源
import android.content.res.AssetManager
// Android位图工厂，用于解码图像
import android.graphics.BitmapFactory

// Filament渲染引擎核心类
import com.google.android.filament.Engine
// 间接光照类，用于环境光照
import com.google.android.filament.IndirectLight
// 天空盒类，用于渲染背景环境
import com.google.android.filament.Skybox
// 纹理类，用于存储图像数据
import com.google.android.filament.Texture
// 缓冲读取器（未使用）
import java.io.BufferedReader
// 输入流读取器（未使用）
import java.io.InputStreamReader

// 字节缓冲区，用于存储二进制数据
import java.nio.ByteBuffer

// Kotlin数学库中的对数函数
import kotlin.math.log2

/**
 * IBL（基于图像的光照）数据类
 * 包含间接光照和天空盒的所有必要组件
 * 
 * @param indirectLight 间接光照对象，用于环境光照计算
 * @param indirectLightTexture 间接光照使用的纹理
 * @param skybox 天空盒对象，用于渲染背景环境
 * @param skyboxTexture 天空盒使用的纹理
 */
data class Ibl(val indirectLight: IndirectLight,
               val indirectLightTexture: Texture,
               val skybox: Skybox,
               val skyboxTexture: Texture)

/**
 * 加载IBL资源
 * Load IBL resources
 * 
 * @param assets Android资源管理器
 * @param name IBL资源的名称（文件夹名）
 * @param engine Filament渲染引擎实例
 * @return 包含所有IBL组件的Ibl对象
 */
fun loadIbl(assets: AssetManager, name: String, engine: Engine): Ibl {
    // 加载间接光照和对应纹理
    val (ibl, iblTexture) = loadIndirectLight(assets, name, engine)
    // 加载天空盒和对应纹理
    val (skybox, skyboxTexture) = loadSkybox(assets, name, engine)
    return Ibl(ibl, iblTexture, skybox, skyboxTexture)
}

/**
 * 销毁IBL资源
 * Destroy IBL resources
 * 
 * @param engine Filament渲染引擎实例
 * @param ibl 要销毁的IBL对象
 */
fun destroyIbl(engine: Engine, ibl: Ibl) {
    // 销毁天空盒
    engine.destroySkybox(ibl.skybox)
    // 销毁天空盒纹理
    engine.destroyTexture(ibl.skyboxTexture)
    // 销毁间接光照
    engine.destroyIndirectLight(ibl.indirectLight)
    // 销毁间接光照纹理
    engine.destroyTexture(ibl.indirectLightTexture)
}

/**
 * 获取图像文件的尺寸信息
 * Peek at the size of an image file
 * 
 * @param assets Android资源管理器
 * @param name 图像文件的路径名称
 * @return 图像的宽度和高度的配对
 */
private fun peekSize(assets: AssetManager, name: String): Pair<Int, Int> {
    // 使用use确保资源正确关闭
    assets.open(name).use { input ->
        // 设置解码选项，只获取边界信息而不加载实际像素数据
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // 解码流以获取图像尺寸
        BitmapFactory.decodeStream(input, null, opts)
        // 返回宽度和高度
        return opts.outWidth to opts.outHeight
    }
}

/**
 * 加载间接光照
 * Load indirect light for IBL
 * 
 * @param assets Android资源管理器
 * @param name IBL资源文件夹名称
 * @param engine Filament渲染引擎实例
 * @return 间接光照对象和对应纹理的配对
 */
private fun loadIndirectLight(
        assets: AssetManager,
        name: String,
        engine: Engine): Pair<IndirectLight, Texture> {
    // 获取基础mipmap级别的图像尺寸
    val (w, h) = peekSize(assets, "$name/m0_nx.rgb32f")
    // 创建立方体贴图纹理，用于存储预过滤的环境贴图
    val texture = Texture.Builder()
            .width(w)  // 设置纹理宽度
            .height(h) // 设置纹理高度
            .levels(log2(w.toFloat()).toInt() + 1) // 计算mipmap级别数量
            .format(Texture.InternalFormat.R11F_G11F_B10F) // 使用HDR格式
            .sampler(Texture.Sampler.SAMPLER_CUBEMAP) // 设置为立方体贴图采样器
            .build(engine)

    // 加载所有mipmap级别的立方体贴图
    for (i in 0 until texture.levels) {
        // 加载第i级mipmap，如果失败则停止
        if (!loadCubemap(texture, assets, name, engine, "m${i}_", i)) break
    }

    // 创建间接光照对象
    return IndirectLight.Builder()
            .reflections(texture) // 设置反射纹理
            .intensity(30_000.0f) // 设置光照强度（流明）
            .build(engine) to texture
}

/**
 * 加载天空盒
 * Load skybox for background rendering
 * 
 * @param assets Android资源管理器
 * @param name IBL资源文件夹名称
 * @param engine Filament渲染引擎实例
 * @return 天空盒对象和对应纹理的配对
 */
private fun loadSkybox(assets: AssetManager, name: String, engine: Engine): Pair<Skybox, Texture> {
    // 获取天空盒图像的尺寸
    val (w, h) = peekSize(assets, "$name/nx.rgb32f")
    // 创建天空盒纹理（只需要一个mipmap级别）
    val texture = Texture.Builder()
            .width(w)  // 设置纹理宽度
            .height(h) // 设置纹理高度
            .levels(1) // 天空盒只需要一个mipmap级别
            .format(Texture.InternalFormat.R11F_G11F_B10F) // 使用HDR格式
            .sampler(Texture.Sampler.SAMPLER_CUBEMAP) // 设置为立方体贴图采样器
            .build(engine)

    // 加载立方体贴图数据
    loadCubemap(texture, assets, name, engine)

    // 创建天空盒对象
    return Skybox.Builder().environment(texture).build(engine) to texture
}

/**
 * 加载立方体贴图数据
 * Load cubemap data from asset files
 * 
 * @param texture 目标纹理对象
 * @param assets Android资源管理器
 * @param name IBL资源文件夹名称
 * @param engine Filament渲染引擎实例
 * @param prefix 文件名前缀（用于mipmap级别）
 * @param level mipmap级别
 * @return 加载是否成功
 */
private fun loadCubemap(texture: Texture,
                        assets: AssetManager,
                        name: String,
                        engine: Engine,
                        prefix: String = "",
                        level: Int = 0): Boolean {
    // This is important, the alpha channel does not encode opacity but some
    // of the bits of an R11G11B10F image to represent HDR data. We must tell
    // Android to not premultiply the RGB channels by the alpha channel
    // 这很重要，alpha通道不编码不透明度，而是R11G11B10F图像的一些位来表示HDR数据。
    // 我们必须告诉Android不要将RGB通道与alpha通道预乘
    val opts = BitmapFactory.Options().apply { inPremultiplied = false }

    // R11G11B10F is always 4 bytes per pixel
    // R11G11B10F格式总是每像素4字节
    val faceSize = texture.getWidth(level) * texture.getHeight(level) * 4
    // 计算每个立方体面的偏移量
    val offsets = IntArray(6) { it * faceSize }
    // Allocate enough memory for all the cubemap faces
    // 为所有立方体贴图面分配足够的内存
    val storage = ByteBuffer.allocateDirect(faceSize * 6)

    // 加载立方体贴图的6个面：正X、负X、正Y、负Y、正Z、负Z
    arrayOf("px", "nx", "py", "ny", "pz", "nz").forEach { suffix ->
        try {
            // 打开对应的资源文件并解码为位图
            assets.open("$name/$prefix$suffix.rgb32f").use {
                val bitmap = BitmapFactory.decodeStream(it, null, opts)
                // 将位图像素数据复制到缓冲区
                bitmap?.copyPixelsToBuffer(storage)
            }
        } catch (e: Exception) {
            // 如果加载失败，返回false
            return false
        }
    }

    // Rewind the texture buffer
    // 重置纹理缓冲区指针到开始位置
    storage.flip()

    // 创建像素缓冲区描述符
    val buffer = Texture.PixelBufferDescriptor(storage,
            Texture.Format.RGB, Texture.Type.UINT_10F_11F_11F_REV)
    // 将图像数据设置到纹理中
    texture.setImage(engine, level, buffer, offsets)

    return true
}
