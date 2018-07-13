package org.gearvrf.animation.keyframe;

import org.gearvrf.PrettyPrint;
import org.gearvrf.utility.Log;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Describes the animation of a single node.<p>
 *
 * The node name ({@link #getNodeName()} specifies the bone/node which is
 * affected by this animation channel. The keyframes are given in three
 * separate series of values, one each for position, rotation and scaling.
 * The transformation matrix computed from these values replaces the node's
 * original transformation matrix at a specific time.<p>
 *
 * This means all keys are absolute and not relative to the bone default pose.
 * The order in which the transformations are applied is - as usual -
 * scaling, rotation, translation.
 */
public final class GVRAnimationChannel implements PrettyPrint {
    private static final String TAG = GVRAnimationChannel.class.getSimpleName();

    /**
     * Constructor.
     *
     * @param nodeName name of corresponding scene graph node
     * @param numPosKeys number of position keys
     * @param numRotKeys number of rotation keys
     * @param numScaleKeys number of scaling keys
     * @param preBehavior behavior before animation start
     * @param postBehavior behavior after animation end
     */
    public GVRAnimationChannel(String nodeName, int numPosKeys, int numRotKeys,
                               int numScaleKeys, GVRAnimationBehavior preBehavior, GVRAnimationBehavior postBehavior) {

        m_nodeName = nodeName;
        mPosInterpolator = new GVRFloatAnimation(numPosKeys, 4);
        mRotInterpolator = new GVRFloatAnimation(numRotKeys, 5);
        mSclInterpolator = new GVRFloatAnimation(numScaleKeys, 4);
        mPreState = preBehavior;
        mPostState = postBehavior;
    }


    /**
     * Returns the name of the scene graph node affected by this animation.<p>
     *
     * The node must exist and it must be unique.
     *
     * @return the name of the affected node
     */
    public String getNodeName() {
        return m_nodeName;
    }


    /**
     * Returns the number of position keys.
     *
     * @return the number of position keys
     */
    public int getNumPosKeys() {
        return mPosInterpolator.getNumKeys();
    }

    /**
     * Returns the time component of the specified position key.
     *
     * @param keyIndex the index of the position key
     * @return the time component
     */
    public float getPosKeyTime(int keyIndex) {
        return mPosInterpolator.getTime(keyIndex);
    }

    /**
     * Returns the position as vector.<p>
     *
     * @param keyIndex the index of the position key
     *
     * @return the position as vector
     */
    public void getPosKeyVector(int keyIndex, float[] pos)
    {
        mPosInterpolator.getKey(keyIndex, pos);
    }



    public void setPosKeyVector(int keyIndex, float time, final float[] pos)
    {
        mPosInterpolator.setKey(keyIndex, time, pos);

    }


    /**
     * Returns the number of rotation keys.
     *
     * @return the number of rotation keys
     */
    public int getNumRotKeys() {
        return mRotInterpolator.getNumKeys();
    }


    /**
     * Returns the time component of the specified rotation key.
     *
     * @param keyIndex the index of the position key
     * @return the time component
     */
    public float getRotKeyTime(int keyIndex) {
        return mRotInterpolator.getTime(keyIndex);
    }


    /**
     * Returns the rotation as quaternion.<p>
     *
     *
     * @param keyIndex the index of the rotation key
     *
     * @return the rotation as quaternion
     */
    public void getRotKeyQuaternion(int keyIndex, float[] rot)
    {
        mRotInterpolator.getKey(keyIndex, rot);
    }

    public void setRotKeyQuaternion(int keyIndex, float time, float[] rot)
    {
        mRotInterpolator.setKey(keyIndex, time, rot);


    }

    /**
     * Returns the number of scaling keys.
     *
     * @return the number of scaling keys
     */
    public int getNumScaleKeys() {
        return mSclInterpolator.getNumKeys();
    }


    /**
     * Returns the time component of the specified scaling key.
     *
     * @param keyIndex the index of the position key
     * @return the time component
     */
    public double getScaleKeyTime(int keyIndex) {
        return mRotInterpolator.getTime(keyIndex);
    }


    /**
     * Returns the scaling factor as vector.<p>
     *
     * @param keyIndex the index of the scale key
     *
     * @return the scaling factor as vector
     */
    public void getScaleKeyVector(int keyIndex, float[] scale) {
        mSclInterpolator.getKey(keyIndex, scale);
    }

    public void setScaleKeyVector(int keyIndex, float time, final float[] scale)
    {
        mSclInterpolator.setKey(keyIndex, time, scale);
    }

    /**
     * Defines how the animation behaves before the first key is encountered.
     * <p>
     *
     * The default value is {@link org.gearvrf.jassimp.AiAnimBehavior#DEFAULT} (the original
     * transformation matrix of the affected node is used).
     *
     * @return the animation behavior before the first key
     */
    public GVRAnimationBehavior getPreState() {
        return mPreState;
    }


    /**
     * Defines how the animation behaves after the last key was processed.<p>
     *
     * The default value is {@link org.gearvrf.jassimp.AiAnimBehavior#DEFAULT} (the original
     * transformation matrix of the affected node is taken).
     *
     * @return the animation behavior before after the last key
     */
    public GVRAnimationBehavior getPostState() {
        return mPostState;
    }

    /**
     * Obtains the transform for a specific time in animation.
     *
     * @param animationTime The time in animation.
     *
     * @return The transform.
     */
    public void animate(float animationTime, Matrix4f mat)
    {

        mPosInterpolator.setInterpolationType("LERP");
        mSclInterpolator.setInterpolationType("LERP");
        mRotInterpolator.setInterpolationType("SLERP");

        mRotInterpolator.animate(animationTime, mRotKey);
        mPosInterpolator.animate(animationTime, mPosKey);
        mSclInterpolator.animate(animationTime, mScaleKey);

        mTempQuat.set(mRotKey[0], mRotKey[1], mRotKey[2], mRotKey[3]);
        mat.set(mTempQuat);
        mat.scale(mScaleKey[0], mScaleKey[1], mScaleKey[2]);
        mat.setTranslation(mPosKey[0], mPosKey[1], mPosKey[2]);

    }

    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(GVRAnimationChannel.class.getSimpleName());
        sb.append(" [nodeName=" + m_nodeName + ", positionKeys="
                + getNumPosKeys() + ", rotationKeys="
                + getNumRotKeys() + ", scaleKeys="
                + getNumScaleKeys() + ", m_preState=" + mPreState
                + ", m_postState=" + mPostState + "]");
        sb.append(System.lineSeparator());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }

    /**
     * Node name.
     */
    private final String m_nodeName;
    final private float[] mPosKey = new float[] {  0, 0, 0 };
    final private float[] mScaleKey = new float[] { 1, 1, 1 };
    final private float[] mRotKey = new float[] { 0, 0, 0, 1 };
    final private Quaternionf mTempQuat = new Quaternionf(0, 0, 0, 1);
    final private GVRFloatAnimation mPosInterpolator;
    final private GVRFloatAnimation mRotInterpolator;
    final private GVRFloatAnimation mSclInterpolator;

    /**
     * Pre-animation behavior.
     */
    private final GVRAnimationBehavior mPreState;

    /**
     * Post-animation behavior.
     */
    private final GVRAnimationBehavior mPostState;
}