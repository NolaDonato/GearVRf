package org.gearvrf.animation.keyframe;

import org.gearvrf.PrettyPrint;
import org.gearvrf.utility.Log;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Describes the animation of a set of floating point values.
 */
public final class GVRFloatAnimation implements PrettyPrint
{
    private static final String TAG = GVRFloatAnimation.class.getSimpleName();


    public static class FloatKeyInterpolator
    {
        private final int mFloatsPerKey;
        private final float[] mKeyData;
        private int mLastKeyIndex;



        public FloatKeyInterpolator(float[] keyData, int keySize)
        {
            mKeyData = keyData;
            mFloatsPerKey = keySize;
            mLastKeyIndex = -1;


        }

        protected float[] interpolate(float time, float[] destValues, String interpltnType)
        {


            int index = getKeyIndex(time);
            float curTime = getTime(index);


            float nextTime = getTime(index + 1);

            if ((index >= 0) &&
                    (curTime <= time) &&
                    (time < nextTime))
            {
                // interpolate
                float deltaTime = nextTime - curTime;
                float factor = (time - curTime) / deltaTime;

                interpolateValues(index, destValues, factor, interpltnType);
            }
            else
            {
                // time is out of range of animation time frame
                if (time <= getTime(0))
                {
                    getValues(0, destValues);

                }
                else
                {
                    getValues(getNumKeys() - 1, destValues);

                }
            }
            return destValues;
        }

        public int getKeyOffset(int keyIndex)
        {
            int index;

            if (keyIndex < 0)
            {
                return -1;
            }
            index = keyIndex * mFloatsPerKey;
            if (index + mFloatsPerKey > mKeyData.length)
            {
                return -1;
            }
            return index;
        }

        public int getNumKeys()
        {
            return mKeyData.length / mFloatsPerKey;
        }

        public float getTime(int keyIndex)
        {
            int ofs = getKeyOffset(keyIndex);
            if (ofs >= 0)
            {
                return mKeyData[ofs];
            }
            return -1.0f;
        }

        public void setTime(int keyIndex, float time)
        {
            int ofs = getKeyOffset(keyIndex);
            if(ofs>=0)
            {
                mKeyData[ofs] = time;
            }
        }

        public boolean setValues(int keyIndex, float[] values)
        {
            int ofs = getKeyOffset(keyIndex);
            if (ofs >= 0)
            {
                System.arraycopy(values, 0, mKeyData, ofs + 1, mFloatsPerKey - 1);
                return true;
            }
            return false;
        }

        public boolean interpolateValues(int keyIndex, float[] values, float factor, String interpltnType)
        {
            int firstOfs = getKeyOffset(keyIndex);
            int lastOfs = getKeyOffset(keyIndex + 1);

            if ((firstOfs < 0) || (lastOfs < 0))
            {
                return false;
            }
            ++firstOfs;
            ++lastOfs;



            if(interpltnType == "LERP") {

                for (int i = 0; i < mFloatsPerKey - 1; ++i) {
                    values[i] = factor * mKeyData[lastOfs + i] + (1.0f - factor) * mKeyData[firstOfs + i];


                }

            }

            else if(interpltnType == "SLERP")

            {

                Quaternionf rot = new Quaternionf();
                rot.x = mKeyData[firstOfs + 0];
                rot.y = mKeyData[firstOfs + 1];
                rot.z = mKeyData[firstOfs + 2];
                rot.w = mKeyData[firstOfs + 3];

                Quaternionf rotTarget = new Quaternionf();
                rotTarget.x = mKeyData[lastOfs + 0];
                rotTarget.y = mKeyData[lastOfs + 1];
                rotTarget.z = mKeyData[lastOfs + 2];
                rotTarget.w = mKeyData[lastOfs + 3];

                Quaternionf valuesQ = rot.slerp(rotTarget, factor);

                values[0] = valuesQ.x;
                values[1] = valuesQ.y;
                values[2] = valuesQ.z;
                values[3] = valuesQ.w;



            }


            return true;
        }

        public boolean getValues(int keyIndex, float[] values)
        {
            int ofs = getKeyOffset(keyIndex);
            if (ofs >= 0)
            {
                System.arraycopy(mKeyData, ofs + 1, values, 0, mFloatsPerKey - 1);
                return true;
            }
            return false;
        }

        public int getKeyIndex(float time)
        {
            // Try cached key first
            int numKeys = getNumKeys();
            int lastOfs = getKeyOffset(mLastKeyIndex + 1);
            float lastTime = getTime(mLastKeyIndex);
            float nextTime = getTime(mLastKeyIndex + 1);

            if ((mLastKeyIndex != -1) && (lastOfs >= 0))
            {
                if ((lastTime <= time) &&
                        (time < nextTime))
                {
                    return mLastKeyIndex;
                }
                float prevTime = getTime(mLastKeyIndex - 1);

                if ((prevTime >= 0) &&
                        (prevTime <= time) &&
                        (time < lastTime))
                {
                    return --mLastKeyIndex;
                }
                lastTime = nextTime;
                nextTime = getTime(mLastKeyIndex + 2);

                // Try neighboring keys
                if ((nextTime >= 0) &&
                        (lastTime <= time) &&
                        (time < nextTime))
                {
                    return ++mLastKeyIndex;
                }
            }

            // Binary search for the interval
            // Each of the index i represents an interval I(i) = [time(i), time(i + 1)).
            int low = 0, high = numKeys - 2;
            // invariant: I(low)...I(high) contains time if time can be found
            // post-condition: |high - low| <= 1, only need to check I(low) and I(low + 1)
            while ((high - low) > 1)
            {
                int mid = (low + high) / 2;
                float midTime = getTime(mid);

                if (midTime < 0)
                {
                    break;
                }
                if (time < midTime)
                {
                    high = mid;
                }
                else if (time >= getTime(mid + 1))
                {
                    low = mid + 1;
                }
                else
                {
                    // time in I(mid) by definition
                    return mLastKeyIndex = mid;
                }
            }
            if ((getTime(low) <= time) &&
                    (time < getTime(low + 1)))
            {
                return mLastKeyIndex = low;
            }
            float lowTime = getTime(low + 2);

            if ((lowTime >= 0) &&
                    (getTime(low + 1) <= time) &&
                    (time < lowTime))
            {
                return mLastKeyIndex = low + 1;
            }
            Log.v(TAG, "Warning: interpolation failed at time " + time);
            return mLastKeyIndex = -1;
        }
    };

    final private int mFloatsPerKey;
    final protected float[] mKeys;
    final private FloatKeyInterpolator mFloatInterpolator;

    private String interpolationType = " ";

    /**
     * Constructor.
     *
     * @param keyData animation key data
     * @param keySize number of floats per key
     */
    public GVRFloatAnimation(float[] keyData, int keySize)
    {
        if (keySize <= 2)
        {
            throw new IllegalArgumentException("The number of floats per key must be > 1, the key includes time");
        }
        if ((keyData == null) || (keyData.length < keySize))
        {
            throw new IllegalArgumentException("Not enough key data");
        }
        mFloatsPerKey = keySize;
        mKeys = keyData;

        mFloatInterpolator = new FloatKeyInterpolator(mKeys, keySize);
    }

    /**
     * Constructor.
     *
     * @param numKeys number of keys
     * @param keySize number of floats per key
     */
    public GVRFloatAnimation(int numKeys, int keySize)
    {
        if (keySize <= 2)
        {
            throw new IllegalArgumentException("The number of floats per key must be > 1, the key includes time");
        }
        mFloatsPerKey = keySize;
        mKeys = new float[numKeys * keySize];

        mFloatInterpolator = new FloatKeyInterpolator(mKeys, keySize);
    }

    /**
     * Returns the number of keys.
     *
     * @return the number of keys
     */
    public int getNumKeys() {
        return mKeys.length / mFloatsPerKey;
    }

    public void setInterpolationType(String interplType)
    {
        interpolationType = interplType;



    }


    public float getDuration()
    {
        if (mKeys.length > mFloatsPerKey)
        {
            return mKeys[mKeys.length - mFloatsPerKey] - mKeys[0];
        }
        return 0;
    }

    /**
     * Returns the time component of the specified key.
     *
     * @param keyIndex the index of the position key
     * @return the time component
     */
    public float getTime(int keyIndex) {
        return mKeys[keyIndex * mFloatsPerKey];
    }

    /**
     * Returns the scaling factor as vector.<p>
     *
     * @param keyIndex the index of the scale key
     *
     * @return the scaling factor as vector
     */
    public void getKey(int keyIndex, float[] values)
    {
        int index = keyIndex * mFloatsPerKey;
        System.arraycopy(mKeys, index + 1, values, 0, values.length);
    }

    public void setKey(int keyIndex, float time, final float[] values)
    {
        int index = keyIndex * mFloatsPerKey;
        Integer valSize = mFloatsPerKey-1;

        if (values.length != valSize)
        {

            throw new IllegalArgumentException("This key needs " + valSize.toString() + " float per value");
        }
        mKeys[index] = time;
        System.arraycopy(values, 0, mKeys, index + 1, values.length);
    }



    /**
     * Obtains the transform for a specific time in animation.
     *
     * @param animationTime The time in animation.
     *
     * @return The transform.
     */






    public void animate(float animationTime, float[] destValues)
    {

        mFloatInterpolator.interpolate(animationTime, destValues, interpolationType);


    }


    @Override
    public void prettyPrint(StringBuffer sb, int indent) {
        sb.append(Log.getSpaces(indent));
        sb.append(GVRFloatAnimation.class.getSimpleName());
        sb.append(" [ Keys=" + mKeys.length + "]");
        sb.append(System.lineSeparator());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        prettyPrint(sb, 0);
        return sb.toString();
    }

}