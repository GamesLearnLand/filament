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

package com.google.android.filament.utils;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * 相机操作器，提供类似sketchfab或Google Maps的相机交互功能
 * 
 * 客户端通过各种鼠标/触摸事件通知操作器，然后周期性调用getLookAt()方法调整相机
 * 支持三种操作模式：
 * - ORBIT：轨道模式，绕目标点旋转（如三维模型查看）
 * - MAP：地图模式，平面移动（如二维地图浏览）
 * - FREE_FLIGHT：自由飞行模式（如三维场景漫游）
 * 
 * 使用Builder模式构建实例，包含以下核心功能：
 * 1. 相机姿态控制（位置/目标点/朝向）
 * 2. 多种交互模式支持
 * 3. 视口尺寸适配
 * 4. 地面投影计算
 * 5. 书签功能（保存/恢复相机状态）
 * 
 * @see Bookmark 用于保存相机状态的书签类
 */
public class Manipulator {
    private static final Mode[] sModeValues = Mode.values();

    private final long mNativeObject;

    private Manipulator(long nativeIndexBuffer) {
        mNativeObject = nativeIndexBuffer;
    }

    /**
     * 操作模式枚举
     * ORBIT - 轨道模式：绕目标点旋转，适合三维模型查看
     * MAP - 地图模式：平面移动，适合二维地图浏览
     * FREE_FLIGHT - 自由飞行模式：全自由度移动，适合三维场景漫游
     */
    public enum Mode { ORBIT, MAP, FREE_FLIGHT };

    /**
     * 视场角方向枚举
     * VERTICAL - 垂直方向视场角固定（默认）
     * HORIZONTAL - 水平方向视场角固定
     */
    public enum Fov { VERTICAL, HORIZONTAL };

    /**
     * 自由飞行模式控制键位
     * FORWARD/BACKWARD - 前进/后退
     * LEFT/RIGHT - 左移/右移
     * UP/DOWN - 上升/下降
     */
    public enum Key {
        FORWARD,
        LEFT,
        BACKWARD,
        RIGHT,
        UP,
        DOWN
    }

    /**
     * 构建器类，用于配置Manipulator实例的创建参数
     * 提供链式调用接口设置以下参数：
     * - 视口尺寸（viewport）
     * - 相机目标点（targetPosition）
     * - 初始朝向（upVector）
     * - 缩放速度（zoomSpeed）
     * - 轨道模式参数（orbitHomePosition, orbitSpeed）
     * - 视场参数（fovDirection, fovDegrees）
     * - 渲染参数（farPlane）
     * - 地图模式参数（mapExtent, mapMinDistance）
     * - 自由飞行模式参数（flightStartPosition, flightStartOrientation等）
     */
    public static class Builder {
        @SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
        // Keep to finalize native resources
        private final BuilderFinalizer mFinalizer;
        private final long mNativeBuilder;

        public Builder() {
            mNativeBuilder = nCreateBuilder();
            mFinalizer = new BuilderFinalizer(mNativeBuilder);
        }

        /**
         * 设置视口尺寸
         * 
         * @param width 视口宽度（像素），必须≥1
         * @param height 视口高度（像素），必须≥1
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder viewport(@IntRange(from = 1) int width, @IntRange(from = 1) int height) {
            nBuilderViewport(mNativeBuilder, width, height);
            return this;
        }

        /**
         * 设置相机目标点位置
         * 
         * @param x 目标点X坐标
         * @param y 目标点Y坐标
         * @param z 目标点Z坐标
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder targetPosition(float x, float y, float z) {
            nBuilderTargetPosition(mNativeBuilder, x, y, z);
            return this;
        }

        /**
         * 设置初始朝向向量
         * 
         * @param x 向量X分量
         * @param y 向量Y分量
         * @param z 向量Z分量
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder upVector(float x, float y, float z) {
            nBuilderUpVector(mNativeBuilder, x, y, z);
            return this;
        }

        /**
         * 设置缩放速度
         * 
         * @param arg 缩放速度系数，默认0.01
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder zoomSpeed(float arg) {
            nBuilderZoomSpeed(mNativeBuilder, arg);
            return this;
        }

        /**
         * 设置轨道模式初始位置
         * 
         * @param x 初始位置X坐标
         * @param y 初始位置Y坐标
         * @param z 初始位置Z坐标
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder orbitHomePosition(float x, float y, float z) {
            nBuilderOrbitHomePosition(mNativeBuilder, x, y, z);
            return this;
        }

        /**
         * 设置轨道模式速度系数
         * 
         * @param x X方向速度系数
         * @param y Y方向速度系数
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder orbitSpeed(float x, float y) {
            nBuilderOrbitSpeed(mNativeBuilder, x, y);
            return this;
        }

        /**
         * 设置视场角方向
         * 
         * @param fov 视场角方向（VERTICAL/HORIZONTAL）
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder fovDirection(Fov fov) {
            nBuilderFovDirection(mNativeBuilder, fov.ordinal());
            return this;
        }

        /**
         * 设置视场角大小
         * 
         * @param arg 视场角大小（度数）
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder fovDegrees(float arg) {
            nBuilderFovDegrees(mNativeBuilder, arg);
            return this;
        }

        /**
         * 设置远裁剪面距离
         * 
         * @param arg 远裁剪面距离
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder farPlane(float arg) {
            nBuilderFarPlane(mNativeBuilder, arg);
            return this;
        }

        /**
         * 设置地图模式地面尺寸
         * 
         * @param width 地面宽度
         * @param height 地面高度
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder mapExtent(float width, float height) {
            nBuilderMapExtent(mNativeBuilder, width, height);
            return this;
        }

        /**
         * 设置地图模式最小距离
         * 
         * @param arg 最小距离
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder mapMinDistance(float arg) {
            nBuilderMapMinDistance(mNativeBuilder, arg);
            return this;
        }

        /**
         * 设置自由飞行模式初始位置
         * 
         * @param x 初始位置X坐标
         * @param y 初始位置Y坐标
         * @param z 初始位置Z坐标
         * @return 构建器实例用于链式调用
         */
        public Builder flightStartPosition(float x, float y, float z) {
            nBuilderFlightStartPosition(mNativeBuilder, x, y, z);
            return this;
        }

        /**
         * 设置自由飞行模式初始朝向
         * 
         * @param pitch 俯仰角
         * @param yaw 偏航角
         * @return 构建器实例用于链式调用
         */
        public Builder flightStartOrientation(float pitch, float yaw) {
            nBuilderFlightStartOrientation(mNativeBuilder, pitch, yaw);
            return this;
        }

        /**
         * 设置自由飞行模式最大移动速度
         * 
         * @param maxSpeed 最大移动速度
         * @return 构建器实例用于链式调用
         */
        public Builder flightMaxMoveSpeed(float maxSpeed) {
            nBuilderFlightMaxMoveSpeed(mNativeBuilder, maxSpeed);
            return this;
        }

        /**
         * 设置自由飞行模式速度步数
         * 
         * @param steps 速度步数
         * @return 构建器实例用于链式调用
         */
        public Builder flightSpeedSteps(int steps) {
            nBuilderFlightSpeedSteps(mNativeBuilder, steps);
            return this;
        }

       /**
        * 设置自由飞行模式平移速度系数
        * 
        * @param x X方向速度系数
        * @param y Y方向速度系数
        * @return 构建器实例用于链式调用
        */
        public Builder flightPanSpeed(float x, float y) {
            nBuilderFlightPanSpeed(mNativeBuilder, x, y);
            return this;
        }

       /**
        * 设置自由飞行模式移动阻尼
        * 
        * @param damping 阻尼系数
        * @return 构建器实例用于链式调用
        */
        public Builder flightMoveDamping(float damping) {
            nBuilderFlightMoveDamping(mNativeBuilder, damping);
            return this;
        }

        /**
         * 设置地面平面方程
         * 
         * @param a 平面方程系数A
         * @param b 平面方程系数B
         * @param c 平面方程系数C
         * @param d 平面方程系数D
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder groundPlane(float a, float b, float c, float d) {
            nBuilderGroundPlane(mNativeBuilder, a, b, c, d);
            return this;
        }

        /**
         * 设置是否启用平移操作
         * 
         * @param enabled 是否启用平移
         * @return 构建器实例用于链式调用
         */
        @NonNull
        public Builder panning(Boolean enabled) {
            nBuilderPanning(mNativeBuilder, enabled);
            return this;
        }

        /**
         * 构建并返回Manipulator实例
         * 
         * @param mode 操作模式（ORBIT/MAP/FREE_FLIGHT）
         * @return 新创建的Manipulator实例
         * @exception IllegalStateException 如果创建失败抛出异常
         */
        @NonNull
        public Manipulator build(Mode mode) {
            long nativeManipulator = nBuilderBuild(mNativeBuilder, mode.ordinal());
            if (nativeManipulator == 0)
                throw new IllegalStateException("Couldn't create Manipulator");
            return new Manipulator(nativeManipulator);
        }

        private static class BuilderFinalizer {
            private final long mNativeObject;

            BuilderFinalizer(long nativeObject) {
                mNativeObject = nativeObject;
            }

            @Override
            public void finalize() {
                try {
                    super.finalize();
                } catch (Throwable t) { // Ignore
                } finally {
                    nDestroyBuilder(mNativeObject);
                }
            }
        }
    };

    @Override
    public void finalize() {
        try {
            super.finalize();
        } catch (Throwable t) { // Ignore
        } finally {
            nDestroyManipulator(mNativeObject);
        }
    }

    /**
     * 获取当前操作模式
     * 
     * @return 当前模式（ORBIT/MAP/FREE_FLIGHT）
     */
    public Mode getMode() { return sModeValues[nGetMode(mNativeObject)]; }

    /**
     * 设置视口尺寸（像素级）
     * 
     * @param width 视口宽度
     * @param height 视口高度
     */
    public void setViewport(int width, int height) {
        nSetViewport(mNativeObject, width, height);
    }

    /**
     * 获取当前相机姿态矩阵
     * 
     * @param eyePosition 相机位置数组（至少3个元素）
     * @param targetPosition 目标点位置数组（至少3个元素）
     * @param upward 向上方向数组（至少3个元素）
     */
    public void getLookAt(
            @NonNull @Size(min = 3) float[] eyePosition,
            @NonNull @Size(min = 3) float[] targetPosition,
            @NonNull @Size(min = 3) float[] upward) {
        nGetLookAtFloat(mNativeObject, eyePosition, targetPosition, upward);
    }

    public void getLookAt(
            @NonNull @Size(min = 3) double[] eyePosition,
            @NonNull @Size(min = 3) double[] targetPosition,
            @NonNull @Size(min = 3) double[] upward) {
        nGetLookAtDouble(mNativeObject, eyePosition, targetPosition, upward);
    }

    /**
     * 执行视口坐标到地面平面的射线检测
     * 
     * @param x 视口X坐标
     * @param y 视口Y坐标
     * @return 交点坐标数组（长度3），若无交点返回null
     */
    @Nullable @Size(min = 3)
    public float[] raycast(int x, int y) {
        float[] result = new float[3];
        nRaycast(mNativeObject, x, y, result);
        return result;
    }

    /**
     * 开始抓取操作（用户开始拖动）
     * 
     * @param x 起始点X坐标
     * @param y 起始点Y坐标
     * @param strafe 是否为平移操作（仅ORBIT模式有效）
     */
    public void grabBegin(int x, int y, boolean strafe) {
        nGrabBegin(mNativeObject, x, y, strafe);
    }

    /**
     * 更新抓取操作（用户持续拖动）
     * 
     * @param x 当前X坐标
     * @param y 当前Y坐标
     */
    public void grabUpdate(int x, int y) {
        nGrabUpdate(mNativeObject, x, y);
    }

    /**
     * 结束抓取操作（用户停止拖动）
     */
    public void grabEnd() {
        nGrabEnd(mNativeObject);
    }

    /**
     * 按下控制键（自由飞行模式专用）
     * 
     * @param key 按下的键位（Key枚举）
     */
    public void keyDown(Key key) {
        nKeyDown(mNativeObject, key.ordinal());
    }

    /**
     * 释放控制键
     * 
     * @param key 释放的键位（Key枚举）
     */
    public void keyUp(Key key) {
        nKeyUp(mNativeObject, key.ordinal());
    }

    /**
     * 处理滚轮事件
     * 
     * @param x 事件X坐标（MAP/ORBIT模式忽略）
     * @param y 事件Y坐标（MAP/ORBIT模式忽略）
     * @param scrolldelta 滚轮增量：
     *                    - MAP/ORBIT模式：负值放大，正值缩小
     *                    - FREE_FLIGHT模式：负值减速，正值加速
     */
    public void scroll(int x, int y, float scrolldelta) {
        nScroll(mNativeObject, x, y, scrolldelta);
    }

    /**
     * 更新相机状态（每帧调用）
     * 
     * @param deltaTime 自上次更新以来的时间间隔（秒）
     */
    public void update(float deltaTime) {
        nUpdate(mNativeObject, deltaTime);
    }

    /**
     * 获取当前相机状态的书签
     * 
     * @return 可用于恢复相机状态的Bookmark对象
     * @see #jumpToBookmark(Bookmark)
     */
    public Bookmark getCurrentBookmark() {
        return new Bookmark(nGetCurrentBookmark(mNativeObject));
    }

    /**
     * 获取初始状态的书签
     * 
     * @return 可用于恢复到初始状态的Bookmark对象
     * @see #jumpToBookmark(Bookmark)
     */
    public Bookmark getHomeBookmark() {
        return new Bookmark(nGetHomeBookmark(mNativeObject));
    }

    /**
     * 跳转到指定书签状态
     * 
     * @param bookmark 包含目标状态的Bookmark对象
     * @see #getCurrentBookmark()
     * @see #getHomeBookmark()
     */
    public void jumpToBookmark(Bookmark bookmark) {
        nJumpToBookmark(mNativeObject, bookmark.getNativeObject());
    }

    private static native long nCreateBuilder();
    private static native void nDestroyBuilder(long nativeBuilder);
    private static native void nBuilderViewport(long nativeBuilder, int width, int height);
    private static native void nBuilderTargetPosition(long nativeBuilder, float x, float y, float z);
    private static native void nBuilderUpVector(long nativeBuilder, float x, float y, float z);
    private static native void nBuilderZoomSpeed(long nativeBuilder, float arg);
    private static native void nBuilderOrbitHomePosition(long nativeBuilder, float x, float y, float z);
    private static native void nBuilderOrbitSpeed(long nativeBuilder, float x, float y);
    private static native void nBuilderFovDirection(long nativeBuilder, int arg);
    private static native void nBuilderFovDegrees(long nativeBuilder, float arg);
    private static native void nBuilderFarPlane(long nativeBuilder, float distance);
    private static native void nBuilderMapExtent(long nativeBuilder, float width, float height);
    private static native void nBuilderMapMinDistance(long nativeBuilder, float arg);
    private static native void nBuilderFlightStartPosition(long nativeBuilder, float x, float y, float z);
    private static native void nBuilderFlightStartOrientation(long nativeBuilder, float pitch, float yaw);
    private static native void nBuilderFlightMaxMoveSpeed(long nativeBuilder, float maxSpeed);
    private static native void nBuilderFlightSpeedSteps(long nativeBuilder, int steps);
    private static native void nBuilderFlightPanSpeed(long nativeBuilder, float x, float y);
    private static native void nBuilderFlightMoveDamping(long nativeBuilder, float damping);
    private static native void nBuilderGroundPlane(long nativeBuilder, float a, float b, float c, float d);
    private static native void nBuilderPanning(long nativeBuilder, Boolean enabled);
    private static native long nBuilderBuild(long nativeBuilder, int mode);

    private static native void nDestroyManipulator(long nativeManip);
    private static native int nGetMode(long nativeManip);
    private static native void nSetViewport(long nativeManip, int width, int height);
    private static native void nGetLookAtFloat(long nativeManip, float[] eyePosition, float[] targetPosition, float[] upward);
    private static native void nGetLookAtDouble(long nativeManip, double[] eyePosition, double[] targetPosition, double[] upward);
    private static native void nRaycast(long nativeManip, int x, int y, float[] result);
    private static native void nGrabBegin(long nativeManip, int x, int y, boolean strafe);
    private static native void nGrabUpdate(long nativeManip, int x, int y);
    private static native void nGrabEnd(long nativeManip);
    private static native void nKeyDown(long nativeManip, int key);
    private static native void nKeyUp(long nativeManip, int key);
    private static native void nScroll(long nativeManip, int x, int y, float scrolldelta);
    private static native void nUpdate(long nativeManip, float deltaTime);
    private static native long nGetCurrentBookmark(long nativeManip);
    private static native long nGetHomeBookmark(long nativeManip);
    private static native void nJumpToBookmark(long nativeManip, long nativeBookmark);
}
