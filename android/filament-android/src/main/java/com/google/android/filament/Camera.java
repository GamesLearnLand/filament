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
import androidx.annotation.Nullable;
import androidx.annotation.Size;

/**
 * Camera represents the eye through which the scene is viewed. 相机类 - 代表观察场景的眼睛
 * <p>
 * A Camera has a position and orientation and controls the projection and exposure parameters.
 * 相机具有位置和方向，并控制投影和曝光参数。
 *
 * <h1><u>Creation and destruction  创建和销毁 </u></h1>
 * <p>
 * In Filament, Camera is a component that must be associated with an entity. To do so,
 * use {@link Engine#createCamera(int)}. A Camera component is destroyed using
 * {@link Engine#destroyCameraComponent(int Entity)} ()}.
 * <p>
 * 在Filament中，相机是一个必须与实体关联的组件。要创建相机，使用 {@link Engine#createCamera(int)}。
 * 相机组件通过{@link Engine#destroyCameraComponent(int Entity)} 来销毁。
 *
 * <pre>
 *  Camera myCamera = engine.createCamera(myCameraEntity);
 *  myCamera.setProjection(45, 16.0/9.0, 0.1, 1.0);
 *  myCamera.lookAt(0, 1.60, 1,
 *                  0, 0, 0,
 *                  0, 1, 0);
 *  engine.destroyCameraComponent(myCameraEntity);
 * </pre>
 *
 *
 * <h1><u>Coordinate system  坐标系统</u></h1>
 * <p>
 * The camera coordinate system defines the <b>view space</b>. The camera points towards its -z axis
 * and is oriented such that its top side is in the direction of +y, and its right side in the
 * direction of +x.
 * 相机坐标系统定义了<b>视图空间</b>。相机指向其-z轴方向，其顶部朝向+y方向，右侧朝向+x方向。
 *
 * <p>
 * Since the <b>near</b> and <b>far</b> planes are defined by the distance from the camera,
 * their respective coordinates are -distance<sub>near</sub> and -distance<sub>far</sub>.
 * 由于<b>近平面</b>和<b>远平面</b>是通过距离相机的距离来定义的，它们各自的坐标是-distance<sub>near</sub>和-distance<sub>far</sub>。
 *
 * <h1><u>Clipping planes  裁剪平面</u></h1>
 * <p>
 * The camera defines six <b>clipping planes</b> which together create a <b>clipping volume</b>. The
 * geometry outside this volume is clipped.
 * 相机定义了六个<b>裁剪平面</b>，它们共同创建一个<b>裁剪体积</b>。此体积外的几何体将被裁剪。
 * <p>
 * The clipping volume can either be a box or a frustum depending on which projection is used,
 * respectively {@link Projection#ORTHO ORTHO} or {@link Projection#PERSPECTIVE PERSPECTIVE}.
 * The six planes are specified either directly or indirectly using  {@link #setProjection} or
 * {@link #setLensProjection}.
 * 裁剪体积可以是盒子或视锥体，这取决于使用的投影类型，分别对应 {@link Projection#ORTHO ORTHO} 或 {@link Projection#PERSPECTIVE PERSPECTIVE}。
 * 这六个平面通过 {@link #setProjection} 或 {@link #setLensProjection} 直接或间接指定。
 * <p>
 * The six planes are:六个平面是：
 * <ul>
 * <li> left    左平面</li>
 * <li> right   右平面</li>
 * <li> bottom  底平面</li>
 * <li> top     顶平面</li>
 * <li> near    近平面</li>
 * <li> far     远平面</li>
 * </ul>
 * <p>
 * <p>
 * To increase the depth-buffer precision, the <b>far</b> clipping plane is always assumed to be at
 * infinity for rendering. That is, it is not used to clip geometry during rendering.
 * However, it is used during the culling phase (objects entirely behind the <b>far</b>
 * plane are culled).
 * 为了提高深度缓冲区精度，<b>远</b>裁剪平面在渲染时总是假设为无穷远。也就是说，它不用于在渲染期间裁剪几何体。
 * 但是，它在剔除阶段使用（完全在<b>远</b>平面后面的对象被剔除）。
 *
 * <h1><u>Choosing the <b>near</b> plane distance</u></h1><h1><u>选择<b>近</b>平面距离</u></h1>
 * <p>
 * The <b>near</b> plane distance greatly affects the depth-buffer resolution.
 * <b>近</b>平面距离极大地影响深度缓冲区分辨率。
 * <p>
 * <p>
 * Example: Precision at 1m, 10m, 100m and 1Km for various near distances assuming a 32-bit float
 * depth-buffer
 * <p>
 * 示例：假设32位浮点深度缓冲区，在1米、10米、100米和1公里处的精度，
 * 对应不同的近距离
 *
 * <center>
 * <table border="1">
 *     <tr>
 *         <th> near (m) </th><th> 1 m </th><th> 10 m </th><th> 100 m</th><th> 1 Km </th>
 *     </tr>
 *     <tr>
 *         <td>0.001</td><td>7.2e-5</td><td>0.0043</td><td>0.4624</td><td>48.58</td>
 *     </tr>
 *     <tr>
 *         <td>0.01</td><td>6.9e-6</td><td>0.0001</td><td>0.0430</td><td>4.62</td>
 *     </tr>
 *     <tr>
 *         <td>0.1</td><td>3.6e-7</td><td>7.0e-5</td><td>0.0072</td><td>0.43</td>
 *     </tr>
 *     <tr>
 *         <td>1.0</td><td>0</td><td>3.8e-6</td><td>0.0007</td><td>0.07</td>
 *     </tr>
 * </table>
 * </center>
 * <p>
 * <p>
 * As can be seen in the table above, the depth-buffer precision drops rapidly with the
 * distance to the camera.
 * 如上表所示，深度缓冲区精度随着到相机的距离而急剧下降。
 * <p>
 * Make sure to pick the highest <b>near</b> plane distance possible.
 * 确保选择尽可能高的<b>近</b>平面距离。
 *
 *
 * <h1><u>Exposure  曝光</u></h1>
 * <p>
 * The Camera is also used to set the scene's exposure, just like with a real camera. The lights
 * intensity and the Camera exposure interact to produce the final scene's brightness.
 * <p>
 * 相机也用于设置场景的曝光，就像真实相机一样。光源强度和相机曝光相互作用产生最终场景的亮度。
 *
 * @see View
 */
public class Camera {
    private long mNativeObject;

    @Entity
    private final int mEntity;

    /**
     * Denotes the projection type used by this camera.表示此相机使用的投影类型。
     *
     * @see #setProjection
     */
    public enum Projection {
        /** Perspective projection, objects get smaller as they are farther.  */
        /**
         * 透视投影，物体距离越远显得越小。
         */
        PERSPECTIVE,
        /** Orthonormal projection, preserves distances. */
        /**
         * 正交投影，保持距离不变。
         */
        ORTHO
    }

    /**
     * Denotes a field-of-view direction.表示视野方向。
     *
     * @see #setProjection
     */
    public enum Fov {

        /**
         * The field-of-view angle is defined on the vertical axis.
         * 视野角度在垂直轴上定义。
         */
        VERTICAL,
        /**
         * The field-of-view angle is defined on the horizontal axis.
         * 视野角度在水平轴上定义。
         */
        HORIZONTAL
    }

    /**
     * 相机构造函数
     *
     * @param nativeCamera 本地相机对象指针
     * @param entity       相机关联的实体ID
     */
    Camera(long nativeCamera, @Entity int entity) {
        mNativeObject = nativeCamera;
        mEntity = entity;
    }

    /**
     * Sets the projection matrix from a frustum defined by six planes.
     * 从由六个平面定义的视锥体设置投影矩阵。
     *
     * @param projection type of projection to use 要使用的投影类型
     * @param left       distance in world units from the camera to the left plane,
     *                   at the near plane. Precondition: <code>left</code> != <code>right</code>
     *                   从相机到左平面的世界单位距离，
     *                   在近平面处。前提条件：<code>left</code> != <code>right</code>
     * @param right      distance in world units from the camera to the right plane,
     *                   at the near plane. Precondition: <code>left</code> != <code>right</code>
     *                   从相机到右平面的世界单位距离，
     *                   在近平面处。前提条件：<code>left</code> != <code>right</code>
     * @param bottom     distance in world units from the camera to the bottom plane,
     *                   at the near plane. Precondition: <code>bottom</code> != <code>top</code>
     *                   从相机到底平面的世界单位距离，
     *                   在近平面处。前提条件：<code>bottom</code> != <code>top</code>
     * @param top        distance in world units from the camera to the top plane,
     *                   at the near plane. Precondition: <code>bottom</code> != <code>top</code>
     *                   从相机到顶平面的世界单位距离，
     *                   在近平面处。前提条件：<code>bottom</code> != <code>top</code>
     * @param near       distance in world units from the camera to the near plane.
     *                   The near plane's position in view space is z = -<code>near</code>.
     *                   Precondition:
     *                   <code>near</code> > 0 for {@link Projection#PERSPECTIVE} or
     *                   <code>near</code> != <code>far</code> for {@link Projection#ORTHO}.
     *                   从相机到近平面的世界单位距离。
     *                   近平面在视图空间中的位置是 z = -<code>near</code>。
     *                   前提条件：
     *                   对于 {@link Projection#PERSPECTIVE}，<code>near</code> > 0 或
     *                   对于 {@link Projection#ORTHO}，<code>near</code> != <code>far</code>。
     * @param far        distance in world units from the camera to the far plane.
     *                   The far plane's position in view space is z = -<code>far</code>.
     *                   Precondition:
     *                   <code>far</code> > <code>near</code>
     *                   for {@link Projection#PERSPECTIVE} or
     *                   <code>far</code> != <code>near</code>
     *                   for {@link Projection#ORTHO}.
     *                   从相机到远平面的世界单位距离。
     *                   远平面在视图空间中的位置是 z = -<code>far</code>。
     *                   前提条件：
     *                   对于 {@link Projection#PERSPECTIVE}，<code>far</code> > <code>near</code>
     *                   或
     *                   对于 {@link Projection#ORTHO}，<code>far</code> != <code>near</code>。
     *
     *                   <p>
     *                   These parameters are silently modified to meet the preconditions above.
     *                   这些参数会被静默修改以满足上述前提条件。
     * @see Projection
     */
    public void setProjection(@NonNull Projection projection, double left, double right,
                              double bottom, double top, double near, double far) {
        nSetProjection(getNativeObject(), projection.ordinal(), left, right, bottom, top, near, far);
    }

    /**
     * Sets the projection matrix from the field-of-view.从视野角度设置投影矩阵。
     *
     * @param fovInDegrees full field-of-view in degrees.完整视野角度（度）。
     *                     0 < <code>fovInDegrees</code> < 180
     * @param aspect       aspect ratio width/height. 宽高比 宽度/高度。<code>aspect</code> > 0
     * @param near         distance in world units from the camera to the near plane.
     *                     The near plane's position in view space is z = -<code>near</code>.
     *                     Precondition:
     *                     <code>near</code> > 0 for {@link Projection#PERSPECTIVE} or
     *                     <code>near</code> != <code>far</code> for {@link Projection#ORTHO}.
     *                     从相机到近平面的世界单位距离。
     *                     近平面在视图空间中的位置是 z = -<code>near</code>。
     *                     前提条件：
     *                     对于 {@link Projection#PERSPECTIVE}，<code>near</code> > 0 或
     *                     对于 {@link Projection#ORTHO}，<code>near</code> != <code>far</code>。
     * @param far          distance in world units from the camera to the far plane.
     *                     The far plane's position in view space is z = -<code>far</code>.
     *                     Precondition:
     *                     <code>far</code> > <code>near</code>
     *                     for {@link Projection#PERSPECTIVE} or
     *                     <code>far</code> != <code>near</code>
     *                     for {@link Projection#ORTHO}.
     *                     从相机到远平面的世界单位距离。
     *                     远平面在视图空间中的位置是 z = -<code>far</code>。
     *                     前提条件：
     *                     对于 {@link Projection#PERSPECTIVE}，<code>far</code> > <code>near</code>
     *                     或
     *                     对于 {@link Projection#ORTHO}，<code>far</code> != <code>near</code>。
     * @param direction    direction of the field-of-view parameter.视野参数的方向。
     *                     <p>
     *                     These parameters are silently modified to meet the preconditions above.这些参数会被静默修改以满足上述前提条件。
     * @see Fov
     */
    public void setProjection(double fovInDegrees, double aspect, double near, double far,
                              @NonNull Fov direction) {
        nSetProjectionFov(getNativeObject(), fovInDegrees, aspect, near, far, direction.ordinal());
    }

    /**
     * Sets the projection matrix from the focal length.从焦距设置投影矩阵。
     *
     * @param focalLength lens's focal length in millimeters. 镜头焦距（毫米）。<code>focalLength</code> > 0
     * @param aspect      aspect ratio width/height. 宽高比 宽度/高度。<code>aspect</code> > 0
     * @param near        distance in world units from the camera to the near plane.
     *                    The near plane's position in view space is z = -<code>near</code>.
     *                    Precondition:
     *                    <code>near</code> > 0 for {@link Projection#PERSPECTIVE} or
     *                    <code>near</code> != <code>far</code> for {@link Projection#ORTHO}.
     *                    从相机到近平面的世界单位距离。
     *                    近平面在视图空间中的位置是 z = -<code>near</code>。
     *                    前提条件：
     *                    对于 {@link Projection#PERSPECTIVE}，<code>near</code> > 0 或
     *                    对于 {@link Projection#ORTHO}，<code>near</code> != <code>far</code>。
     * @param far         distance in world units from the camera to the far plane.
     *                    The far plane's position in view space is z = -<code>far</code>.
     *                    Precondition:
     *                    <code>far</code> > <code>near</code>
     *                    for {@link Projection#PERSPECTIVE} or
     *                    <code>far</code> != <code>near</code>
     *                    for {@link Projection#ORTHO}.
     *                    从相机到远平面的世界单位距离。
     *                    远平面在视图空间中的位置是 z = -<code>far</code>。
     *                    前提条件：
     *                    对于 {@link Projection#PERSPECTIVE}，<code>far</code> > <code>near</code>
     *                    或
     *                    对于 {@link Projection#ORTHO}，<code>far</code> != <code>near</code>。
     */
    public void setLensProjection(double focalLength, double aspect, double near, double far) {
        nSetLensProjection(getNativeObject(), focalLength, aspect, near, far);
    }

    /**
     * Sets a custom projection matrix.设置自定义投影矩阵。
     *
     * <p>The projection matrix must define an NDC system that must match the OpenGL convention,
     * that is all 3 axis are mapped to [-1, 1].</p>
     * 投影矩阵必须定义一个符合OpenGL约定的NDC系统，即所有3个轴都映射到[-1, 1]。
     *
     * @param inProjection custom projection matrix for rendering and culling 用于渲染和剔除的自定义投影矩阵
     * @param near         distance in world units from the camera to the near plane.
     *                     The near plane's position in view space is z = -<code>near</code>.
     *                     Precondition:
     *                     <code>near</code> > 0 for {@link Projection#PERSPECTIVE} or
     *                     <code>near</code> != <code>far</code> for {@link Projection#ORTHO}.
     *                     从相机到近平面的世界单位距离。
     *                     近平面在视图空间中的位置是 z = -<code>near</code>。
     *                     前提条件：
     *                     对于 {@link Projection#PERSPECTIVE}，<code>near</code> > 0 或
     *                     对于 {@link Projection#ORTHO}，<code>near</code> != <code>far</code>。
     * @param far          distance in world units from the camera to the far plane.
     *                     The far plane's position in view space is z = -<code>far</code>.
     *                     Precondition:
     *                     <code>far</code> > <code>near</code>
     *                     for {@link Projection#PERSPECTIVE} or
     *                     <code>far</code> != <code>near</code>
     *                     for {@link Projection#ORTHO}.
     *                     从相机到远平面的世界单位距离。
     *                     远平面在视图空间中的位置是 z = -<code>far</code>。
     *                     前提条件：
     *                     对于 {@link Projection#PERSPECTIVE}，<code>far</code> > <code>near</code>
     *                     或
     *                     对于 {@link Projection#ORTHO}，<code>far</code> != <code>near</code>。
     */
    public void setCustomProjection(@NonNull @Size(min = 16) double[] inProjection,
                                    double near, double far) {
        Asserts.assertMat4dIn(inProjection);
        nSetCustomProjection(getNativeObject(), inProjection, inProjection, near, far);
    }

    /**
     * Sets a custom projection matrix.设置自定义投影矩阵。
     *
     * <p>The projection matrices must define an NDC system that must match the OpenGL convention,
     * that is all 3 axis are mapped to [-1, 1].</p>
     * 投影矩阵必须定义一个符合OpenGL约定的NDC系统，即所有3个轴都映射到[-1, 1]。
     *
     * @param inProjection           custom projection matrix for rendering.用于渲染的自定义投影矩阵。
     * @param inProjectionForCulling custom projection matrix for culling.用于剔除的自定义投影矩阵。
     * @param near                   distance in world units from the camera to the near plane.
     *                               The near plane's position in view space is z = -<code>near</code>.
     *                               Precondition:
     *                               <code>near</code> > 0 for {@link Projection#PERSPECTIVE} or
     *                               <code>near</code> != <code>far</code> for {@link Projection#ORTHO}.
     *                               从相机到近平面的世界单位距离。
     *                               近平面在视图空间中的位置是 z = -<code>near</code>。
     *                               前提条件：
     *                               对于 {@link Projection#PERSPECTIVE}，<code>near</code> > 0 或
     *                               对于 {@link Projection#ORTHO}，<code>near</code> != <code>far</code>。
     * @param far                    distance in world units from the camera to the far plane.
     *                               The far plane's position in view space is z = -<code>far</code>.
     *                               Precondition:
     *                               <code>far</code> > <code>near</code>
     *                               for {@link Projection#PERSPECTIVE} or
     *                               <code>far</code> != <code>near</code>
     *                               for {@link Projection#ORTHO}.
     *                               从相机到远平面的世界单位距离。
     *                               远平面在视图空间中的位置是 z = -<code>far</code>。
     *                               前提条件：
     *                               对于 {@link Projection#PERSPECTIVE}，<code>far</code> > <code>near</code>
     *                               或
     *                               对于 {@link Projection#ORTHO}，<code>far</code> != <code>near</code>。
     */
    public void setCustomProjection(
        @NonNull @Size(min = 16) double[] inProjection,
        @NonNull @Size(min = 16) double[] inProjectionForCulling,
        double near, double far) {
        Asserts.assertMat4dIn(inProjection);
        Asserts.assertMat4dIn(inProjectionForCulling);
        nSetCustomProjection(getNativeObject(), inProjection, inProjectionForCulling, near, far);
    }

    /**
     * Sets an additional matrix that scales the projection matrix.
     * 设置一个额外的矩阵来缩放投影矩阵。
     *
     * <p>This is useful to adjust the aspect ratio of the camera independent from its projection.
     * First, pass an aspect of 1.0 to setProjection. Then set the scaling with the desired aspect
     * ratio:<br>
     * 这对于独立于投影调整相机的宽高比很有用。首先，向setProjection传递1.0的宽高比。然后用所需的宽高比设置缩放：
     *
     * <code>
     * double aspect = width / height;
     * <p>
     * // with Fov.HORIZONTAL passed to setProjection:当向setProjection传递Fov.HORIZONTAL时：
     * camera.setScaling(1.0, aspect);
     * <p>
     * // with Fov.VERTICAL passed to setProjection:当向setProjection传递Fov.VERTICAL时：
     * camera.setScaling(1.0 / aspect, 1.0);
     * </code>
     * <p>
     * By default, this is an identity matrix.默认情况下，这是一个单位矩阵。
     * </p>
     *
     * @param xscaling horizontal scaling to be applied after the projection matrix.在投影矩阵之后应用的水平缩放。
     * @param yscaling vertical scaling to be applied after the projection matrix.在投影矩阵之后应用的垂直缩放。
     * @see Camera#setProjection
     * @see Camera#setLensProjection
     * @see Camera#setCustomProjection
     */
    public void setScaling(double xscaling, double yscaling) {
        nSetScaling(getNativeObject(), xscaling, yscaling);
    }

    /**
     * Sets an additional matrix that scales the projection matrix.
     *
     * <p>This is useful to adjust the aspect ratio of the camera independent from its projection.
     * First, pass an aspect of 1.0 to setProjection. Then set the scaling with the desired aspect
     * ratio:<br>
     *
     * <code>
     * double aspect = width / height;
     * <p>
     * // with Fov.HORIZONTAL passed to setProjection:
     * double[] s = {1.0, aspect, 1.0, 1.0};
     * camera.setScaling(s);
     * <p>
     * // with Fov.VERTICAL passed to setProjection:
     * double[] s = {1.0 / aspect, 1.0, 1.0, 1.0};
     * camera.setScaling(s);
     * </code>
     * <p>
     * By default, this is an identity matrix.
     * </p>
     *
     * @param inScaling diagonal of the scaling matrix to be applied after the projection matrix.
     * @see Camera#setProjection
     * @see Camera#setLensProjection
     * @see Camera#setCustomProjection
     * @deprecated use {@link #setScaling(double, double)}
     */
    @Deprecated
    public void setScaling(@NonNull @Size(min = 4) double[] inScaling) {
        Asserts.assertDouble4In(inScaling);
        setScaling(inScaling[0], inScaling[1]);
    }

    /**
     * Sets an additional matrix that shifts (translates) the projection matrix.
     * 设置一个额外的矩阵来偏移（平移）投影矩阵。
     * <p>
     * The shift parameters are specified in NDC coordinates, that is, if the translation must
     * be specified in pixels, the xshift and yshift parameters be scaled by 1.0 / viewport.width
     * and 1.0 / viewport.height respectively.
     * 偏移参数以NDC坐标指定，也就是说，如果平移必须以像素指定，xshift和yshift参数应分别按1.0 / viewport.width和1.0 / viewport.height进行缩放。
     * </p>
     *
     * @param xshift horizontal shift in NDC coordinates applied after the projection 在投影之后应用的NDC坐标中的水平偏移
     * @param yshift vertical shift in NDC coordinates applied after the projection 在投影之后应用的NDC坐标中的垂直偏移
     * @see Camera#setProjection
     * @see Camera#setLensProjection
     * @see Camera#setCustomProjection
     */
    public void setShift(double xshift, double yshift) {
        nSetShift(getNativeObject(), xshift, yshift);
    }

    /**
     * Sets the camera's model matrix.设置相机的模型矩阵。
     * <p>
     * Helper method to set the camera's entity transform component.设置相机实体变换组件的辅助方法。
     * Remember that the Camera "looks" towards its -z axis.记住相机"看向"其-z轴方向。
     * <p>
     * This has the same effect as calling:这与调用以下代码具有相同的效果：
     *
     * <pre>
     *  engine.getTransformManager().setTransform(
     *          engine.getTransformManager().getInstance(camera->getEntity()), modelMatrix);
     * </pre>
     *
     * @param modelMatrix The camera position and orientation provided as a <b>rigid transform</b> matrix.
     *                    作为<b>刚体变换</b>矩阵提供的相机位置和方向。
     */
    public void setModelMatrix(@NonNull @Size(min = 16) float[] modelMatrix) {
        Asserts.assertMat4fIn(modelMatrix);
        nSetModelMatrix(getNativeObject(), modelMatrix);
    }

    /**
     * Sets the camera's model matrix. 设置相机的模型矩阵。
     * <p>
     * Helper method to set the camera's entity transform component.设置相机实体变换组件的辅助方法。
     * Remember that the Camera "looks" towards its -z axis.记住相机"看向"其-z轴方向。
     * <p>
     *
     * @param modelMatrix The camera position and orientation provided as a <b>rigid transform</b> matrix.
     *                    作为<b>刚体变换</b>矩阵提供的相机位置和方向。
     */
    public void setModelMatrix(@NonNull @Size(min = 16) double[] modelMatrix) {
        Asserts.assertMat4In(modelMatrix);
        nSetModelMatrixFp64(getNativeObject(), modelMatrix);
    }

    /**
     * 设置相机的观察方向（通过指定眼睛位置、目标点和上向量）。
     *
     * <p>该方法用于定义相机的位置和朝向。相机会从 eyeX, eyeY, eyeZ 指定的位置，
     * 朝向由 centerX, centerY, centerZ 定义的目标点，upX, upY, upZ 提供相机的上方向。
     * 这个方法通常在设置相机视图时使用，类似于 OpenGL 的 gluLookAt 函数。</p>
     *
     * @param eyeX    相机位置的 X 坐标（世界空间）
     * @param eyeY    相机位置的 Y 坐标（世界空间）
     * @param eyeZ    相机位置的 Z 坐标（世界空间）
     * @param centerX 相机指向的目标点的 X 坐标（世界空间）
     * @param centerY 相机指向的目标点的 Y 坐标（世界空间）
     * @param centerZ 相机指向的目标点的 Z 坐标（世界空间）
     * @param upX     上方向的 X 分量，通常为 0
     * @param upY     上方向的 Y 分量，通常为 1
     * @param upZ     上方向的 Z 分量，通常为 0
     */
    public void lookAt(double eyeX, double eyeY, double eyeZ,
                       double centerX, double centerY, double centerZ,
                       double upX, double upY, double upZ) {
        nLookAt(getNativeObject(), eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
    }

    /**
     * Gets the distance to the near plane 获取到近平面的距离
     *
     * @return Distance to the near plane到近平面的距离
     */
    public float getNear() {
        return (float) nGetNear(getNativeObject());
    }

    /**
     * Gets the distance to the far plane 获取到远平面的距离
     *
     * @return Distance to the far plane 到远平面的距离
     */
    public float getCullingFar() {
        return (float) nGetCullingFar(getNativeObject());
    }

    /**
     * Retrieves the camera's projection matrix. The projection matrix used for rendering always has
     * its far plane set to infinity. This is why it may differ from the matrix set through
     * setProjection() or setLensProjection().
     * <p>
     * 获取相机的投影矩阵。用于渲染的投影矩阵总是将其远平面设置为无穷大。这就是为什么它可能与通过setProjection()或setLensProjection()设置的矩阵不同。
     *
     * @param out A 16-float array where the projection matrix will be stored, or null in which
     *            case a new array is allocated.
     *            存储投影矩阵的16个浮点数数组，如果为null则分配一个新数组。
     * @return A 16-float array containing the camera's projection as a column-major matrix.包含相机投影的16个浮点数数组，以列主序矩阵形式。
     */
    @NonNull
    @Size(min = 16)
    public double[] getProjectionMatrix(@Nullable @Size(min = 16) double[] out) {
        out = Asserts.assertMat4d(out);
        nGetProjectionMatrix(getNativeObject(), out);
        return out;
    }

    /**
     * Retrieves the camera's culling matrix. The culling matrix is the same as the projection
     * matrix, except the far plane is finite.
     * 获取相机的剔除矩阵。剔除矩阵与投影矩阵相同，除了远平面是有限的。
     *
     * @param out A 16-float array where the projection matrix will be stored, or null in which
     *            case a new array is allocated.
     *            存储投影矩阵的16个浮点数数组，如果为null则分配一个新数组。
     * @return A 16-float array containing the camera's projection as a column-major matrix.
     * 包含相机投影的16个浮点数数组，以列主序矩阵形式。
     */
    @NonNull
    @Size(min = 16)
    public double[] getCullingProjectionMatrix(@Nullable @Size(min = 16) double[] out) {
        out = Asserts.assertMat4d(out);
        nGetCullingProjectionMatrix(getNativeObject(), out);
        return out;
    }

    /**
     * Returns the scaling amount used to scale the projection matrix.
     * 返回用于缩放投影矩阵的缩放量。
     *
     * @return the diagonal of the scaling matrix applied after the projection matrix.
     * 在投影矩阵之后应用的缩放矩阵的对角线。
     * @see Camera#setScaling
     */
    @NonNull
    @Size(min = 4)
    public double[] getScaling(@Nullable @Size(min = 4) double[] out) {
        out = Asserts.assertDouble4(out);
        nGetScaling(getNativeObject(), out);
        return out;
    }

    /**
     * Retrieves the camera's model matrix. The model matrix encodes the camera position and
     * orientation, or pose.
     * <p>
     * 获取相机的模型矩阵。模型矩阵编码相机的位置和方向或姿态。
     *
     * @param out A 16-float array where the model matrix will be stored, or null in which
     *            case a new array is allocated.
     *            存储模型矩阵的16个浮点数数组，如果为null则分配一个新数组。
     * @return A 16-float array containing the camera's pose as a column-major matrix.
     * 包含相机姿态的16个浮点数数组，以列主序矩阵形式。
     */
    @NonNull
    @Size(min = 16)
    public float[] getModelMatrix(@Nullable @Size(min = 16) float[] out) {
        out = Asserts.assertMat4f(out);
        nGetModelMatrix(getNativeObject(), out);
        return out;
    }

    /**
     * Retrieves the camera's model matrix. The model matrix encodes the camera position and
     * orientation, or pose.
     *
     * @param out A 16-double array where the model matrix will be stored, or null in which
     *            case a new array is allocated.
     * @return A 16-double array containing the camera's pose as a column-major matrix.
     */
    @NonNull
    @Size(min = 16)
    public double[] getModelMatrix(@Nullable @Size(min = 16) double[] out) {
        out = Asserts.assertMat4(out);
        nGetModelMatrixFp64(getNativeObject(), out);
        return out;
    }

    /**
     * Retrieves the camera's view matrix. The view matrix is the inverse of the model matrix.
     * <p>
     * 获取相机的视图矩阵。视图矩阵是模型矩阵的逆矩阵。它将世界坐标转换为相机的坐标系统。
     *
     * <p>The returned 16-float array represents a 4x4 column-major matrix:</p>
     *
     * <pre>
     * [ Xx, Xy, Xz, 0,
     *   Yx, Yy, Yz, 0,
     *   Zx, Zy, Zz, 0,
     *   Tx, Ty, Tz, 1 ]
     * </pre>
     * <p>
     * 其中：
     * - 第一列 (Xx, Xy, Xz, 0) 表示相机的右向量（Right Vector）:
     * 右向量表示相机坐标系中的X轴方向，即相机的右侧方向。这个向量通常用于描述相机的横向朝向，
     * 它与上向量和前向量一起构成一个正交基。在三维图形学中，右向量通常是单位向量，并且与上向量、
     * 前向量保持垂直关系。这个向量可用于计算相机的横向移动（strafe movement）等操作。
     * <p>
     * - 第二列 (Yx, Yy, Yz, 0) 表示相机的上向量（Up Vector）
     * - 第三列 (Zx, Zy, Zz, 0) 表示相机的前向量（Forward Vector），通常指向 -Z 方向
     * - 第四列 (Tx, Ty, Tz, 1) 表示相机在世界中的位置（Translation）
     *
     * @param out A 16-float array where the view matrix will be stored, or null in which
     *            case a new array is allocated.
     *            <p>
     *            存储视图矩阵的16个浮点数数组，如果为null则分配一个新数组。
     * @return A 16-float array containing the camera's column-major view matrix.
     * 包含相机列主序视图矩阵的16个浮点数数组。
     */
    @NonNull
    @Size(min = 16)
    public float[] getViewMatrix(@Nullable @Size(min = 16) float[] out) {
        out = Asserts.assertMat4f(out);
        nGetViewMatrix(getNativeObject(), out);
        return out;
    }

    /**
     * Retrieves the camera's view matrix. The view matrix is the inverse of the model matrix.
     *
     * @param out A 16-double array where the model view will be stored, or null in which
     *            case a new array is allocated.
     * @return A 16-double array containing the camera's column-major view matrix.
     */
    @NonNull
    @Size(min = 16)
    public double[] getViewMatrix(@Nullable @Size(min = 16) double[] out) {
        out = Asserts.assertMat4(out);
        nGetViewMatrixFp64(getNativeObject(), out);
        return out;
    }

    /**
     * Retrieves the camera position in world space.
     * 获取相机在世界空间中的位置。
     *
     * @param out A 3-float array where the position will be stored, or null in which case a new
     *            array is allocated.
     *            存储位置的3个浮点数数组，如果为null则分配一个新数组。
     * @return A 3-float array containing the camera's position in world units.
     * 包含相机在世界单位中位置的3个浮点数数组。
     */
    @NonNull
    @Size(min = 3)
    public float[] getPosition(@Nullable @Size(min = 3) float[] out) {
        out = Asserts.assertFloat3(out);
        nGetPosition(getNativeObject(), out);
        return out;
    }

    /**
     * Retrieves the camera left unit vector in world space, that is a unit vector that points to
     * the left of the camera.
     * 获取相机在世界空间中的左单位向量，即指向相机左侧的单位向量。
     *
     * @param out A 3-float array where the left vector will be stored, or null in which case a new
     *            array is allocated.
     *            存储左向量的3个浮点数数组，如果为null则分配一个新数组。
     * @return A 3-float array containing the camera's left vector in world units.
     * 包含相机在世界单位中左向量的3个浮点数数组。
     */
    @NonNull
    @Size(min = 3)
    public float[] getLeftVector(@Nullable @Size(min = 3) float[] out) {
        out = Asserts.assertFloat3(out);
        nGetLeftVector(getNativeObject(), out);
        return out;
    }

    /**
     * Retrieves the camera up unit vector in world space, that is a unit vector that points up with
     * respect to the camera.
     * 获取相机在世界空间中的上单位向量，即相对于相机指向上方的单位向量。
     *
     * @param out A 3-float array where the up vector will be stored, or null in which case a new
     *            array is allocated.
     *            存储上向量的3个浮点数数组，如果为null则分配一个新数组。
     * @return A 3-float array containing the camera's up vector in world units.
     * 包含相机在世界单位中上向量的3个浮点数数组。
     */
    @NonNull
    @Size(min = 3)
    public float[] getUpVector(@Nullable @Size(min = 3) float[] out) {
        out = Asserts.assertFloat3(out);
        nGetUpVector(getNativeObject(), out);
        return out;
    }

    /**
     * Retrieves the camera forward unit vector in world space, that is a unit vector that points
     * in the direction the camera is looking at.
     * <p>
     * 获取相机在世界空间中的前向单位向量，即指向相机观察方向的单位向量。
     *
     * @param out A 3-float array where the forward vector will be stored, or null in which case a
     *            new  array is allocated.
     *            存储前向量的3个浮点数数组，如果为null则分配一个新数组。
     * @return A 3-float array containing the camera's forward vector in world units.
     * 包含相机在世界单位中前向量的3个浮点数数组。
     */
    @NonNull
    @Size(min = 3)
    public float[] getForwardVector(@Nullable @Size(min = 3) float[] out) {
        out = Asserts.assertFloat3(out);
        nGetForwardVector(getNativeObject(), out);
        return out;
    }

    /**
     * Sets this camera's exposure (default is f/16, 1/125s, 100 ISO)
     * 设置此相机的曝光（默认为f/16，1/125s，100 ISO）
     * <p>
     * The exposure ultimately controls the scene's brightness, just like with a real camera.
     * The default values provide adequate exposure for a camera placed outdoors on a sunny day
     * with the sun at the zenith.
     * 曝光最终控制场景的亮度，就像真实相机一样。
     * 默认值为放置在阳光明媚的日子里户外、太阳在天顶的相机提供充足的曝光。
     * <p>
     * With the default parameters, the scene must contain at least one Light of intensity
     * similar to the sun (e.g.: a 100,000 lux directional light) and/or an indirect light
     * of appropriate intensity (30,000).
     * <p>
     * 使用默认参数时，场景必须包含至少一个强度类似于太阳的光源（例如：100,000勒克斯的方向光）和/或适当强度的间接光（30,000）。
     *
     * @param aperture     Aperture in f-stops, clamped between 0.5 and 64.
     *                     A lower aperture value increases the exposure, leading to
     *                     a brighter scene. Realistic values are between 0.95 and 32.
     *                     光圈值（f档），限制在0.5和64之间。较低的光圈值增加曝光，导致场景更亮。现实值在0.95和32之间。
     * @param shutterSpeed Shutter speed in seconds, clamped between 1/25,000 and 60.
     *                     A lower shutter speed increases the exposure. Realistic values are
     *                     between 1/8000 and 30.
     *                     快门速度（秒），限制在1/25,000和60之间。较低的快门速度增加曝光。现实值在1/8000和30之间。
     * @param sensitivity  Sensitivity in ISO, clamped between 10 and 204,800.
     *                     A higher sensitivity increases the exposure. Realistic values are
     *                     between 50 and 25600.
     *                     感光度（ISO），限制在10和204,800之间。较高的感光度增加曝光。现实值在50和25600之间。
     * @see LightManager
     * @see #setExposure(float)
     */
    public void setExposure(float aperture, float shutterSpeed, float sensitivity) {
        nSetExposure(getNativeObject(), aperture, shutterSpeed, sensitivity);
    }

    /**
     * Sets this camera's exposure directly. Calling this method will set the aperture
     * to 1.0, the shutter speed to 1.2 and the sensitivity will be computed to match
     * the requested exposure (for a desired exposure of 1.0, the sensitivity will be
     * set to 100 ISO).
     * <p>
     * 直接设置此相机的曝光。调用此方法将设置光圈为1.0，快门速度为1.2，
     * 感光度将被计算以匹配请求的曝光（对于期望的1.0曝光，感光度将被设置为100 ISO）。
     * <p>
     * This method is useful when trying to match the lighting of other engines or tools.
     * Many engines/tools use unit-less light intensities, which can be matched by setting
     * the exposure manually. This can be typically achieved by setting the exposure to
     * 1.0.
     * <p>
     * 此方法在尝试匹配其他引擎或工具的照明时很有用。许多引擎/工具使用无单位的光强度，
     * 可以通过手动设置曝光来匹配。这通常可以通过将曝光设置为1.0来实现。
     *
     * @see LightManager
     * @see #setExposure(float, float, float)
     */
    public void setExposure(float exposure) {
        setExposure(1.0f, 1.2f, 100.0f * (1.0f / exposure));
    }

    /**
     * Gets the aperture in f-stops 获取光圈值（f档）
     *
     * @return Aperture in f-stops 光圈值（f档）
     */
    public float getAperture() {
        return nGetAperture(getNativeObject());
    }

    /**
     * Gets the shutter speed in seconds 获取快门速度（秒）
     *
     * @return Shutter speed in seconds 快门速度（秒）
     */
    public float getShutterSpeed() {
        return nGetShutterSpeed(getNativeObject());
    }

    /**
     * Gets the focal length in meters 获取焦距（米）
     *
     * @return focal length in meters [m] 焦距（米）[m]
     */
    public double getFocalLength() {
        return nGetFocalLength(getNativeObject());
    }

    /**
     * Set the camera focus distance in world units 设置相机焦距（世界单位）
     *
     * @param distance Distance from the camera to the focus plane in world units. Must be
     *                 positive and larger than the camera's near clipping plane.
     *                 从相机到焦平面的世界单位距离。必须为正值且大于相机的近裁剪平面。
     */
    public void setFocusDistance(float distance) {
        nSetFocusDistance(getNativeObject(), distance);
    }

    /**
     * Gets the distance from the camera to the focus plane in world units
     * 获取从相机到焦平面的世界单位距离
     *
     * @return Distance from the camera to the focus plane in world units 从相机到焦平面的世界单位距离
     */
    public float getFocusDistance() {
        return nGetFocusDistance(getNativeObject());
    }

    /**
     * Gets the sensitivity in ISO 获取感光度（ISO）
     *
     * @return Sensitivity in ISO 感光度（ISO）
     */
    public float getSensitivity() {
        return nGetSensitivity(getNativeObject());
    }

    /**
     * Gets the entity representing this Camera  获取代表此相机的实体
     *
     * @return the entity this Camera component is attached to 此相机组件附加到的实体
     */
    @Entity
    public int getEntity() {
        return mEntity;
    }

    /**
     * Helper to compute the effective focal length taking into account the focus distance
     * 计算考虑焦距的有效焦距的辅助方法
     *
     * @param focalLength   focal length in any unit (e.g. [m] or [mm]) 任何单位的焦距（例如[m]或[mm]）
     * @param focusDistance focus distance in same unit as focalLength 与focalLength相同单位的焦距
     * @return the effective focal length in same unit as focalLength 与focalLength相同单位的有效焦距
     */
    static double computeEffectiveFocalLength(double focalLength, double focusDistance) {
        return nComputeEffectiveFocalLength(focalLength, focusDistance);
    }

    /**
     * Helper to compute the effective field-of-view taking into account the focus distance
     * 计算考虑焦距的有效视野的辅助方法
     *
     * @param fovInDegrees  full field of view in degrees  完整视野（度）
     * @param focusDistance focus distance in meters [m]   焦距（米）[m]
     * @return effective full field of view in degrees  有效的完整视野（度）
     */
    static double computeEffectiveFov(double fovInDegrees, double focusDistance) {
        return nComputeEffectiveFov(fovInDegrees, focusDistance);
    }

    public long getNativeObject() {
        if (mNativeObject == 0) {
            throw new IllegalStateException("Calling method on destroyed Camera");
        }
        return mNativeObject;
    }

    void clearNativeObject() {
        mNativeObject = 0;
    }

    private static native void nSetProjection(long nativeCamera, int projection, double left, double right, double bottom, double top, double near, double far);

    private static native void nSetProjectionFov(long nativeCamera, double fovInDegrees, double aspect, double near, double far, int fov);

    private static native void nSetLensProjection(long nativeCamera, double focalLength, double aspect, double near, double far);

    private static native void nSetCustomProjection(long nativeCamera, double[] inProjection, double[] inProjectionForCulling, double near, double far);

    private static native void nSetScaling(long nativeCamera, double x, double y);

    private static native void nSetShift(long nativeCamera, double x, double y);

    private static native void nSetModelMatrix(long nativeCamera, float[] in);

    private static native void nSetModelMatrixFp64(long nativeCamera, double[] in);

    private static native void nLookAt(long nativeCamera, double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ);

    private static native double nGetNear(long nativeCamera);

    private static native double nGetCullingFar(long nativeCamera);

    private static native void nGetProjectionMatrix(long nativeCamera, double[] out);

    private static native void nGetCullingProjectionMatrix(long nativeCamera, double[] out);

    private static native void nGetScaling(long nativeCamera, double[] out);

    private static native void nGetModelMatrix(long nativeCamera, float[] out);

    private static native void nGetModelMatrixFp64(long nativeCamera, double[] out);

    private static native void nGetViewMatrix(long nativeCamera, float[] out);

    private static native void nGetViewMatrixFp64(long nativeCamera, double[] out);

    private static native void nGetPosition(long nativeCamera, float[] out);

    private static native void nGetLeftVector(long nativeCamera, float[] out);

    private static native void nGetUpVector(long nativeCamera, float[] out);

    private static native void nGetForwardVector(long nativeCamera, float[] out);

    private static native void nSetExposure(long nativeCamera, float aperture, float shutterSpeed, float sensitivity);

    private static native float nGetAperture(long nativeCamera);

    private static native float nGetShutterSpeed(long nativeCamera);

    private static native float nGetSensitivity(long nativeCamera);

    private static native void nSetFocusDistance(long nativeCamera, float distance);

    private static native float nGetFocusDistance(long nativeCamera);

    private static native double nGetFocalLength(long nativeCamera);

    private static native double nComputeEffectiveFocalLength(double focalLength, double focusDistance);

    private static native double nComputeEffectiveFov(double fovInDegrees, double focusDistance);
}
