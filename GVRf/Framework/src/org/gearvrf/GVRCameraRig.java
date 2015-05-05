/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf;

import static org.gearvrf.utility.Preconditions.checkStringNotNullOrEmpty;
import static org.gearvrf.utility.Preconditions.checkFloatNotNaNOrInfinity;

/** Holds the GVRCameras. */
public class GVRCameraRig extends GVRComponent {
    /** Ways to use the rotation sensor data. */
    public abstract static class GVRCameraRigType {
        /** Rotates freely. Default. */
        public abstract static class Free {
            public static final int ID = 0;
        }

        /** Yaw rotation (naively speaking, rotation by y-axis) only. */
        public abstract static class YawOnly {
            public static final int ID = 1;
        }

        /** No roll rotation (naively speaking, rotation by z-axis). */
        public abstract static class RollFreeze {
            public static final int ID = 2;
        }

        /** No rotation at all. */
        public abstract static class Freeze {
            public static final int ID = 3;
        }

        /** Orbits the pivot. */
        public abstract static class OrbitPivot {
            public static final int ID = 4;
            public static final String DISTANCE = "distance";
            public static final String PIVOT = "pivot";
        }
    };

    /** Constructs a camera rig without cameras attached. */
    public GVRCameraRig(GVRContext gvrContext) {
        super(gvrContext, NativeCameraRig.ctor());
    }

    private GVRCameraRig(GVRContext gvrContext, long ptr) {
        super(gvrContext, ptr);
    }

    static GVRCameraRig factory(GVRContext gvrContext, long ptr) {
        GVRHybridObject wrapper = wrapper(ptr);
        return wrapper == null ? new GVRCameraRig(gvrContext, ptr)
                : (GVRCameraRig) wrapper;
    }

    @Override
    protected final boolean registerWrapper() {
        return true;
    }

    /** @return The {@link GVRCameraRigType type} of the camera rig. */
    public int getCameraRigType() {
        return NativeCameraRig.getCameraRigType(getPtr());
    }

    /**
     * Set the {@link GVRCameraRigType type} of the camera rig.
     * 
     * @param cameraRigType
     *            The rig {@link GVRCameraRigType type}.
     */
    public void setCameraRigType(int cameraRigType) {
        NativeCameraRig.setCameraRigType(getPtr(), cameraRigType);
    }

    /**
     * @return Get the left {@link GVRCamera camera}, if one has been
     *         {@link #attachLeftCamera(GVRCamera) attached}; {@code null} if
     *         not.
     */
    public GVRCamera getLeftCamera() {
        long ptr = NativeCameraRig.getLeftCamera(getPtr());
        return ptr == 0 ? null : GVRCamera.factory(getGVRContext(), ptr);
    }

    /**
     * @return Get the right {@link GVRCamera camera}, if one has been
     *         {@link #attachRightCamera(GVRCamera) attached}; {@code null} if
     *         not.
     */
    public GVRCamera getRightCamera() {
        long ptr = NativeCameraRig.getRightCamera(getPtr());
        return ptr == 0 ? null : GVRCamera.factory(getGVRContext(), ptr);
    }

    /**
     * @return The global default distance separating the left and right
     *         cameras.
     */
    public static float getDefaultCameraSeparationDistance() {
        return NativeCameraRig.getDefaultCameraSeparationDistance();
    }

    /**
     * Sets the global default distance separating the left and right cameras.
     * 
     * @param distance
     *            Global default separation.
     */
    public static void setDefaultCameraSeparationDistance(float distance) {
        NativeCameraRig.setDefaultCameraSeparationDistance(distance);
    }

    /**
     * @return The distance separating the left and right cameras of the camera
     *         rig.
     */
    public float getCameraSeparationDistance() {
        return NativeCameraRig.getCameraSeparationDistance(getPtr());
    }

    /**
     * Set the distance separating the left and right cameras of the camera rig.
     * 
     * @param distance
     *            Separation distance.
     */
    public void setCameraSeparationDistance(float distance) {
        NativeCameraRig.setCameraSeparationDistance(getPtr(), distance);
    }

    /**
     * @param key
     *            Key of the {@code float} to get.
     * @return The {@code float} value associated with {@code key}.
     */
    public float getFloat(String key) {
        return NativeCameraRig.getFloat(getPtr(), key);
    }

    /**
     * Map {@code value} to {@code key}.
     * 
     * @param key
     *            Key to map {@code value} to.
     * @param value
     *            The {@code float} value to map.
     */
    public void setFloat(String key, float value) {
        checkStringNotNullOrEmpty("key", key);
        checkFloatNotNaNOrInfinity("value", value);
        NativeCameraRig.setFloat(getPtr(), key, value);
    }

    /**
     * @param key
     *            Key of the two-component {@code float} vector to get.
     * @return An two-element array representing the vector mapped to
     *         {@code key}.
     */
    public float[] getVec2(String key) {
        return NativeCameraRig.getVec2(getPtr(), key);
    }

    /**
     * Map a two-component {@code float} vector to {@code key}.
     * 
     * @param key
     *            Key to map the vector to.
     * @param x
     *            'X' component of vector.
     * @param y
     *            'Y' component of vector.
     */
    public void setVec2(String key, float x, float y) {
        checkStringNotNullOrEmpty("key", key);
        NativeCameraRig.setVec2(getPtr(), key, x, y);
    }

    /**
     * @param key
     *            Key of the three-component {@code float} vector to get.
     * @return An three-element array representing the vector mapped to
     *         {@code key}.
     */
    public float[] getVec3(String key) {
        return NativeCameraRig.getVec3(getPtr(), key);
    }

    /**
     * Map a three-component {@code float} vector to {@code key}.
     * 
     * @param key
     *            Key to map the vector to.
     * @param x
     *            'X' component of vector.
     * @param y
     *            'Y' component of vector.
     * @param z
     *            'Z' component of vector.
     */
    public void setVec3(String key, float x, float y, float z) {
        checkStringNotNullOrEmpty("key", key);
        NativeCameraRig.setVec3(getPtr(), key, x, y, z);
    }

    /**
     * @param key
     *            Key of the four-component {@code float} vector to get.
     * @return An four-element array representing the vector mapped to
     *         {@code key} .
     */
    public float[] getVec4(String key) {
        return NativeCameraRig.getVec4(getPtr(), key);
    }

    /**
     * Map a four-component {@code float} vector to {@code key}.
     * 
     * @param key
     *            Key to map the vector to.
     * @param x
     *            'X' component of vector.
     * @param y
     *            'Y' component of vector.
     * @param z
     *            'Z' component of vector.
     * @param w
     *            'W' component of vector.
     */
    public void setVec4(String key, float x, float y, float z, float w) {
        checkStringNotNullOrEmpty("key", key);
        NativeCameraRig.setVec4(getPtr(), key, x, y, z, w);
    }

    /**
     * Attach a {@link GVRCamera camera} as the left camera of the camera rig.
     * 
     * @param camera
     *            {@link GVRCamera Camera} to attach.
     */
    public void attachLeftCamera(GVRCamera camera) {
        if (camera.getOwnerObject() == null) {
            throw new IllegalArgumentException("Owner object not set correctly");
        }
        NativeCameraRig.attachLeftCamera(getPtr(), camera.getPtr());
    }

    /**
     * Attach a {@link GVRCamera camera} as the right camera of the camera rig.
     * 
     * @param camera
     *            {@link GVRCamera Camera} to attach.
     */
    public void attachRightCamera(GVRCamera camera) {
        if (camera.getOwnerObject() == null) {
            throw new IllegalArgumentException("Owner object not set correctly");
        }
        NativeCameraRig.attachRightCamera(getPtr(), camera.getPtr());
    }

    /**
     * Resets the rotation of the camera rig by multiplying further rotations by
     * the inverse of the current rotation.
     * <p>
     * Cancels the effect of prior calls to {@link #resetYaw()} and
     * {@link #resetYawPitch()}.
     */
    public void reset() {
        NativeCameraRig.reset(getPtr());
    }

    /**
     * Resets the yaw of the camera rig by multiplying further changes in the
     * rig's yaw by the inverse of the current yaw.
     * <p>
     * Cancels the effect of prior calls to {@link #reset()} and
     * {@link #resetYawPitch()}.
     */
    public void resetYaw() {
        NativeCameraRig.resetYaw(getPtr());
    }

    /**
     * Resets the yaw and pitch of the camera rig by multiplying further changes
     * in the rig's yaw and pitch by the inverse of the current yaw and pitch.
     * <p>
     * Cancels the effect of prior calls to {@link #reset()} and
     * {@link #resetYaw()}.
     */
    public void resetYawPitch() {
        NativeCameraRig.resetYawPitch(getPtr());
    }

    /**
     * Sets the rotation and angular velocity data for the camera rig. This
     * should only be done in response to
     * {@link RotationSensorListener#onRotationSensor(long, float, float, float, float, float, float, float)
     * RotationSensorListener.onRotationSensor()}.
     * 
     * @param timeStamp
     *            Clock-time when the data was received, in nanoseconds.
     * @param w
     *            The 'W' rotation component.
     * @param x
     *            The 'X' rotation component.
     * @param y
     *            The 'Y' rotation component.
     * @param z
     *            The 'Z' rotation component.
     * @param gyroX
     *            Angular velocity on the 'X' axis.
     * @param gyroY
     *            Angular velocity on the 'Y' axis.
     * @param gyroZ
     *            Angular velocity on the 'Z' axis.
     */
    void setRotationSensorData(long timeStamp, float w, float x, float y,
            float z, float gyroX, float gyroY, float gyroZ) {
        NativeCameraRig.setRotationSensorData(getPtr(), timeStamp, w, x, y, z,
                gyroX, gyroY, gyroZ);
    }

    /**
     * Predict what the orientation of the camera rig will be at {@code time}
     * based on the current rotation and angular velocity.
     * 
     * @param time
     *            Time to predict orientation for, in seconds.
     * @see #setRotationSensorData(long, float, float, float, float, float,
     *      float, float)
     */
    void predict(float time) {
        NativeCameraRig.predict(getPtr(), time);
    }

    /**
     * The direction the camera rig is looking at. In other words, the direction
     * of the local -z axis.
     * 
     * @return Array with 3 floats corresponding to a normalized direction
     *         vector. ([0] : x, [1] : y, [2] : z)
     */
    public float[] getLookAt() {
        return NativeCameraRig.getLookAt(getPtr());
    }

}

class NativeCameraRig {
    static native long ctor();

    static native int getCameraRigType(long cameraRig);

    static native void setCameraRigType(long cameraRig, int cameraRigType);

    static native long getLeftCamera(long cameraRig);

    static native long getRightCamera(long cameraRig);

    static native float getDefaultCameraSeparationDistance();

    static native void setDefaultCameraSeparationDistance(float distance);

    static native float getCameraSeparationDistance(long cameraRig);

    static native void setCameraSeparationDistance(long cameraRig,
            float distance);

    static native float getFloat(long cameraRig, String key);

    static native void setFloat(long cameraRig, String key, float value);

    static native float[] getVec2(long cameraRig, String key);

    static native void setVec2(long cameraRig, String key, float x, float y);

    static native float[] getVec3(long cameraRig, String key);

    static native void setVec3(long cameraRig, String key, float x, float y,
            float z);

    static native float[] getVec4(long cameraRig, String key);

    static native void setVec4(long cameraRig, String key, float x, float y,
            float z, float w);

    static native void attachLeftCamera(long cameraRig, long camera);

    static native void attachRightCamera(long cameraRig, long camera);

    static native void reset(long cameraRig);

    static native void resetYaw(long cameraRig);

    static native void resetYawPitch(long cameraRig);

    static native void setRotationSensorData(long cameraRig, long timeStamp,
            float w, float x, float y, float z, float gyroX, float gyroY,
            float gyroZ);

    static native void predict(long cameraRig, float time);

    static native float[] getLookAt(long cameraRig);
}
