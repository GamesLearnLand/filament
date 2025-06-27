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

package com.google.android.filament;

import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Size;

public final class MathUtils {
    private MathUtils() { }

    /**
     * 将由指定的切线、副法线和法线表示的切空间框架打包到四元数中。
     *
     * <p>
     * 反射通过将反射编码为结果四元数的w分量的符号来保留。由于GPU上不能总是表示-0，
     * 此函数计算一个偏置值，以确保值始终为正或负，而不是0。该偏置值基于每个元素存储大小为2字节进行计算，
     * 使得生成的四元数适合存储到SNORM16向量中。
     * </p>
     *
     * @param tangentX   切线的X分量
     * @param tangentY   切线的Y分量
     * @param tangentZ   切线的Z分量
     * @param bitangentX 副法线的X分量
     * @param bitangentY 副法线的Y分量
     * @param bitangentZ 副法线的Z分量
     * @param normalX    法线的X分量
     * @param normalY    法线的Y分量
     * @param normalZ    法线的Z分量
     * @param quaternion 用于存储四元数结果的float数组，至少需要4个元素
     */
    public static void packTangentFrame(
            float tangentX, float tangentY, float tangentZ,
            float bitangentX, float bitangentY, float bitangentZ,
            float normalX, float normalY, float normalZ,
            @NonNull @Size(min = 4) float[] quaternion) {
        nPackTangentFrame(
            tangentX, tangentY, tangentZ,
            bitangentX, bitangentY, bitangentZ,
            normalX, normalY, normalZ, quaternion, 0);
    }

    /**
     * 将由指定的切线、副法线和法线表示的切空间框架打包到四元数中。
     *
     * <p>
     * 反射通过将反射编码为结果四元数的w分量的符号来保留。由于GPU上不能总是表示-0，
     * 此函数计算一个偏置值，以确保值始终为正或负，而不是0。该偏置值基于每个元素存储大小为2字节进行计算，
     * 使得生成的四元数适合存储到SNORM16向量中。
     * </p>
     *
     * @param tangentX   切线的X分量
     * @param tangentY   切线的Y分量
     * @param tangentZ   切线的Z分量
     * @param bitangentX 副法线的X分量
     * @param bitangentY 副法线的Y分量
     * @param bitangentZ 副法线的Z分量
     * @param normalX    法线的X分量
     * @param normalY    法线的Y分量
     * @param normalZ    法线的Z分量
     * @param quaternion 用于存储四元数结果的float数组，至少需要4个元素
     * @param offset     四元数数组中存储结果的偏移量（以元素为单位）
     */
    public static void packTangentFrame(
            float tangentX, float tangentY, float tangentZ,
            float bitangentX, float bitangentY, float bitangentZ,
            float normalX, float normalY, float normalZ,
            @NonNull @Size(min = 4) float[] quaternion, @IntRange(from = 0) int offset) {
        nPackTangentFrame(tangentX, tangentY, tangentZ,
            bitangentX, bitangentY, bitangentZ,
            normalX, normalY, normalZ, quaternion, offset);
    }

    private static native void nPackTangentFrame(
        float tangentX, float tangentY, float tangentZ,
        float bitangentX, float bitangentY, float bitangentZ,
        float normalX, float normalY, float normalZ,
        @NonNull @Size(min = 4) float[] quaternion, @IntRange(from = 0) int offset);
}
