/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.google.android.filament;

import androidx.annotation.NonNull;

/**
 * A <code>SwapChain</code> represents an Operating System's <b>native</b> renderable surface.
 * <code>SwapChain</code> 表示操作系统的<b>原生</b>可渲染表面。
 *
 * <p>Typically it's a native window or a view. Because a <code>SwapChain</code> is initialized
 * from a native object, it is given to filament as an <code>Object</code>, which must be of the
 * proper type for each platform filament is running on.</p>
 * <p>通常它是一个原生窗口或视图。因为 <code>SwapChain</code> 是从原生对象初始化的，
 * 它作为 <code>Object</code> 传递给 filament，必须是 filament 运行平台的正确类型。</p>
 *
 * <code>
 * SwapChain swapChain = engine.createSwapChain(nativeWindow);
 * </code>
 *
 * <p>The <code>nativeWindow</code> parameter above must be of type:</p>
 * <p>上面的 <code>nativeWindow</code> 参数必须是以下类型：</p>
 *
 * <center>
 * <table border="1">
 *     <tr><th> Platform </th><th> nativeWindow type </th></tr>
 *     <tr><td> Android </td><td>{@link android.view.Surface Surface}</td></tr>
 * </table>
 * </center>
 * <p>
 *
 * <h1>Examples</h1>
 * <h1>示例</h1>
 *
 * <h2>Android</h2>
 *
 *
 * <p>A {@link android.view.Surface Surface} can be retrieved from a
 * {@link android.view.SurfaceView SurfaceView} or {@link android.view.SurfaceHolder SurfaceHolder}
 * easily using {@link android.view.SurfaceHolder#getSurface SurfaceHolder.getSurface()} and/or
 * {@link android.view.SurfaceView#getHolder SurfaceView.getHolder()}.</p>
 * <p>可以通过 {@link android.view.SurfaceHolder#getSurface SurfaceHolder.getSurface()} 和/或
 * {@link android.view.SurfaceView#getHolder SurfaceView.getHolder()} 方法，
 * 轻松地从 {@link android.view.SurfaceView SurfaceView} 或 {@link android.view.SurfaceHolder SurfaceHolder}
 * 中获取 {@link android.view.Surface Surface}。</p>
 *
 * <p>To use a {@link android.view.TextureView Textureview} as a <code>SwapChain</code>, it is
 * necessary to first get its {@link android.graphics.SurfaceTexture SurfaceTexture},
 * for instance using {@link android.view.TextureView.SurfaceTextureListener SurfaceTextureListener}
 * and then create a {@link android.view.Surface Surface}:</p>
 * <p>要将 {@link android.view.TextureView Textureview} 用作 <code>SwapChain</code>，
 * 需要首先获取其 {@link android.graphics.SurfaceTexture SurfaceTexture}，
 * 例如使用 {@link android.view.TextureView.SurfaceTextureListener SurfaceTextureListener}，
 * 然后创建一个 {@link android.view.Surface Surface}：</p>
 *
 * <pre>
 *  // using a TextureView.SurfaceTextureListener:
 *  // 使用 TextureView.SurfaceTextureListener：
 *  public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
 *      mSurface = new Surface(surfaceTexture);
 *      // mSurface can now be used with Engine.createSwapChain()
 *      // mSurface 现在可以与 Engine.createSwapChain() 一起使用
 *  }
 * </pre>
 *
 * @see Engine
 */
public class SwapChain {
    private final Object mSurface;
    private long mNativeObject;

    SwapChain(long nativeSwapChain, Object surface) {
        mNativeObject = nativeSwapChain;
        mSurface = surface;
    }

    /**
     * Return whether createSwapChain supports the CONFIG_PROTECTED_CONTENT flag.
     * The default implementation returns false.
     * 返回 createSwapChain 是否支持 CONFIG_PROTECTED_CONTENT 标志。
     * 默认实现返回 false。
     *
     * @param engine A reference to the filament Engine
     * @param engine Filament 引擎的引用
     * @return true if CONFIG_PROTECTED_CONTENT is supported, false otherwise.
     * @return 如果支持 CONFIG_PROTECTED_CONTENT 则返回 true，否则返回 false。
     * @see SwapChainFlags#CONFIG_PROTECTED_CONTENT
     */
    public static boolean isProtectedContentSupported(@NonNull Engine engine) {
        return nIsProtectedContentSupported(engine.getNativeObject());
    }

    /**
     * Return whether createSwapChain supports the CONFIG_SRGB_COLORSPACE flag.
     * The default implementation returns false.
     * 返回 createSwapChain 是否支持 CONFIG_SRGB_COLORSPACE 标志。
     * 默认实现返回 false。
     *
     * @param engine A reference to the filament Engine
     * @param engine Filament 引擎的引用
     * @return true if CONFIG_SRGB_COLORSPACE is supported, false otherwise.
     * @return 如果支持 CONFIG_SRGB_COLORSPACE 则返回 true，否则返回 false。
     * @see SwapChainFlags#CONFIG_SRGB_COLORSPACE
     */
    public static boolean isSRGBSwapChainSupported(@NonNull Engine engine) {
        return nIsSRGBSwapChainSupported(engine.getNativeObject());
    }

    /**
     * @return the native <code>Object</code> this <code>SwapChain</code> was created from or null
     *         for a headless SwapChain.
     * @return 创建此 <code>SwapChain</code> 的原生 <code>Object</code>，
     *         对于无头 SwapChain 返回 null。
     */
    public Object getNativeWindow() {
        return mSurface;
    }

    /**
     * FrameCompletedCallback is a callback function that notifies an application when a frame's
     * contents have completed rendering on the GPU.
     * FrameCompletedCallback 是一个回调函数，当帧内容在 GPU 上完成渲染时通知应用程序。
     *
     * <p>
     * Use setFrameCompletedCallback to set a callback on an individual SwapChain. Each time a frame
     * completes GPU rendering, the callback will be called.
     * </p>
     * <p>
     * 使用 setFrameCompletedCallback 在单个 SwapChain 上设置回调。每次帧完成 GPU 渲染时，
     * 都会调用该回调。
     * </p>
     *
     * <p>
     * Warning: Only Filament's Metal backend supports frame callbacks. Other backends ignore the
     * callback (which will never be called) and proceed normally.
     * </p>
     * <p>
     * 警告：只有 Filament 的 Metal 后端支持帧回调。其他后端会忽略回调（永远不会被调用）
     * 并正常进行。
     * </p>
     *
     * @param handler     A {@link java.util.concurrent.Executor Executor}.
     * @param handler     一个 {@link java.util.concurrent.Executor Executor}。
     * @param callback    The Runnable callback to invoke.
     * @param callback    要调用的 Runnable 回调。
     */
    public void setFrameCompletedCallback(@NonNull Object handler, @NonNull Runnable callback) {
        nSetFrameCompletedCallback(getNativeObject(), handler, callback);
    }

    public long getNativeObject() {
        if (mNativeObject == 0) {
            throw new IllegalStateException("Calling method on destroyed SwapChain");
        }
        return mNativeObject;
    }

    void clearNativeObject() {
        mNativeObject = 0;
    }

    private static native void nSetFrameCompletedCallback(long nativeSwapChain, Object handler, Runnable callback);
    private static native boolean nIsSRGBSwapChainSupported(long nativeEngine);
    private static native boolean nIsProtectedContentSupported(long nativeEngine);
}
