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


#ifndef IMAGE_H_
#define IMAGE_H_

#include <mutex>
#include <vector>
#include "objects/hybrid_object.h"
#include "util/gvr_log.h"
#include "gl/gl_headers.h"  // for GL_TEXTURE_xxx

namespace gvr {
class Texture;
class TextureParameters;

class Image : public HybridObject
{

public:
    /*
     * Indicates the type of the image.
     * Usually the type corresponds to a different
     * subclass of Image.
     * @see BitmapImage
     * @see CubemapImage
     * @see FloatImage
     */
    enum ImageType
    {
        NONE = 0,
        BITMAP = 1,
        CUBEMAP,
        FLOAT_BITMAP
    };

    enum ImageState
    {
        UNINITIALIZED = 0,
        HAS_DATA = 1,
        UPDATE_PENDING = 2,
    };

    virtual ~Image() { }

    Image() :
            HybridObject(), mState(UNINITIALIZED), mType(NONE), mFormat(0),
            mWidth(0), mHeight(0), mImageSize(0), mUpdateLock(),
            mLevels(0)
    {
        mFileName[0] = 0;
    }

    Image(ImageType type, int format) :
            HybridObject(), mState(UNINITIALIZED), mType(type), mFormat(format),
            mWidth(0), mHeight(0), mImageSize(0), mUpdateLock(),
            mLevels(0)
    {
        mFileName[0] = 0;
    }

    Image(ImageType type, short width, short height, int imagesize, int format, short levels) :
            HybridObject(), mType(type), mState(UNINITIALIZED), mUpdateLock(),
            mWidth(width), mHeight(height), mImageSize(imagesize),
            mFormat(format), mLevels(levels)
    {
        mFileName[0] = 0;
    }

    virtual int getId() = 0;
    virtual bool isReady() = 0;
    virtual void texParamsChanged(const TextureParameters&) = 0;

    bool hasData() const { return mState == HAS_DATA; }
    short getWidth() const { return mWidth; }
    short getHeight() const { return mHeight; }
    short getLevels() const { return mLevels; }
    short getType() const { return mType; }
    int getFormat() const { return mFormat; }
    void setFileName(const char* fname)
    {
        int len = strlen(fname);
        if (len < sizeof(mFileName))
        {
            len = sizeof(mFileName) - 1;
        }
        strncpy(mFileName, fname, len);
    }

    const char* getFileName() const  { return mFileName; }

    int getDataOffset(int level)
    {
        if (!mDataOffsets.empty() && (level >= 0) && (level < mDataOffsets.size()))
        {
            return mDataOffsets[level];
        }
        return 0;
    }

    bool setDataOffsets(const int* offsets, int n)
    {
        mDataOffsets.resize(n);
        for (int i = 0; i < n; ++i)
        {
            mDataOffsets[i] = offsets[i];
        }
    }

    bool checkForUpdate(int texid)
    {
        if (texid && updatePending())
        {
            const char* fname = getFileName();
            LOGV("Texture: Image::checkForUpdate %lx %d %s", this, texid, fname);
            std::lock_guard<std::mutex> lock(mUpdateLock);
            update(texid);
            updateComplete();
        }
        return hasData();
    }


protected:
    void signalUpdate() { mState = UPDATE_PENDING; }
    bool updatePending() const { return mState == UPDATE_PENDING; }
    void updateComplete() { mState = HAS_DATA; }
    virtual void update(int texid) { }

    std::mutex mUpdateLock;
    short   mType;
    short   mLevels;
    short   mWidth;
    short   mHeight;
    short   mState;
    int     mImageSize;
    int     mFormat;
    char    mFileName[64];
    std::vector<int>    mDataOffsets;

private:
    Image(const Image& image);
    Image(Image&& image);
    Image& operator=(const Image& image);
    Image& operator=(Image&& image);
};

}

#endif
