/**
 * Copyright (C) 2017 The Android Open Source Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.filament;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.google.android.filament.proguard.UsedByReflection;


/**
 * EntityManager类负责Filament引擎中实体的创建、销毁和生命周期管理。
 * 采用单例模式设计，确保应用中只有一个实体管理器实例。
 * 提供了创建单个或多个实体、销毁实体以及检查实体存活状态等核心功能。
 */
public class EntityManager {
    /**
     * 指向底层C++实现的原生对象指针
     */
    private long mNativeObject = nGetEntityManager();

    /**
     * 单例模式的持有者类，用于延迟初始化EntityManager实例
     */
    private static class Holder {
        static final EntityManager INSTANCE = new EntityManager();
    }

    private EntityManager() {
    }

    EntityManager(long nativeEntityManager) {
        mNativeObject = nativeEntityManager;
    }

    /**
     * 获取EntityManager的单例实例
     *
     * @return EntityManager的唯一实例
     */
    @NonNull
    public static EntityManager get() {
        return Holder.INSTANCE;
    }

    /**
     * 创建单个实体
     *
     * @return 新创建实体的ID
     */
    @Entity
    public int create() {
        return nCreate(mNativeObject);
    }

    /**
     * 销毁指定的实体
     *
     * @param entity 要销毁的实体ID
     */
    public void destroy(@Entity int entity) {
        nDestroy(mNativeObject, entity);
    }

    /**
     * 批量创建多个实体
     *
     * @param n 要创建的实体数量，必须大于等于1
     * @return 包含新创建实体ID的数组
     * @throws ArrayIndexOutOfBoundsException 如果n小于1时抛出此异常
     */
    @Entity
    @NonNull
    public int[] create(@IntRange(from = 1) int n) {
        if (n < 1) throw new ArrayIndexOutOfBoundsException("n must be at least 1");
        int[] entities = new int[n];
        nCreateArray(mNativeObject, n, entities);
        return entities;
    }

    /**
     * 使用指定数组存储新创建的实体ID
     * @param entities 用于存储新实体ID的数组
     * @return 包含新创建实体ID的数组
     */
    @NonNull
    public int[] create(@Entity @NonNull int[] entities) {
        nCreateArray(mNativeObject, entities.length, entities);
        return entities;
    }

    /**
     * 批量销毁多个实体
     *
     * @param entities 要销毁的实体ID数组
     */
    public void destroy(@Entity @NonNull int[] entities) {
        nDestroyArray(mNativeObject, entities.length, entities);
    }

    /**
     * 检查实体是否存活
     *
     * @param entity 要检查的实体ID
     * @return 如果实体存活则返回true，否则返回false
     */
    public boolean isAlive(@Entity int entity) {
        return nIsAlive(mNativeObject, entity);
    }

    /**
     * 获取底层原生对象指针
     *
     * @return 指向底层C++实现的原生对象指针
     */
    @UsedByReflection("AssetLoader.java")
    public long getNativeObject() {
        return mNativeObject;
    }

    private static native long nGetEntityManager();

    private static native void nCreateArray(long nativeEntityManager, int n, int[] entities);

    private static native int nCreate(long nativeEntityManager);

    private static native void nDestroyArray(long nativeEntityManager, int n, int[] entities);

    private static native void nDestroy(long nativeEntityManager, int entity);

    private static native boolean nIsAlive(long nativeEntityManager, int entity);
}
