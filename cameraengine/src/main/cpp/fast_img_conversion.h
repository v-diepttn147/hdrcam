//
// Created by JK on 7/29/20.
//

#ifndef _IMGCONVERSION_H_
#define _IMGCONVERSION_H_
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

struct YUV_TYPE {
public:
    static const int NV12 = 10;
    static const int NV21 = 11;
};


struct RGB_TYPE {
public:
    static const int RGB = 20;
    static const int RGBA = 21;
    static const int BGR = 22;
    static const int BGRA = 23;
};

struct CONVERSION_TYPE {
public:
    static const int YUV_TO_RGB_INT_WIKI = 10; // https://en.wikipedia.org/wiki/YUV: Y′UV444 to
    static const int YUV_TO_RGB_FLOAT_OPEN_CV_LIKE = 20; //https://docs.opencv.org/4.2.0/de/d25/imgproc_color_conversions.html
    static const int YUV_TO_RGB_FLOAT_ANDROID = 21;

    static const int RGB_TO_YUV_INT_STUDIO_SWING = 30; //https://en.wikipedia.org/wiki/YUV
    static const int RGB_TO_YUV_INT_FULL_SWING = 31; //https://en.wikipedia.org/wiki/YUV
    static const int RGB_TO_YUV_FLOAT_OPEN_CV_LIKE = 40;


};

/**
 *
 * @param yuv output image: nv12 or nv21
 * @param yuvType see struct YUV_TYPE above
 * @param rgb input image: rgb of bgr
 * @param rgbType see struct RGB_TYPE above
 * @param width
 * @param height
 * @return
 */
bool rgb_to_yuv(int conversion_type, unsigned char * yuv, int yuvType, unsigned char const * rgb,
        int rgbType, int
width, int height);

/**
 * @param conversion_type type of conversion. See CONVERSION_TYPE struct for value.
 * @param rgb output image: rgb of bgr
 * @param rgbType see struct RGB_TYPE above
 * @param yuv intput image: nv12 or nv21
 * @param yuvType see struct YUV_TYPE above
 * @param width
 * @param height
 * @param uv is NULL if yuv contains the whole YUV. If uv is not NULL, Y and UV are separated
 * @return
 */

bool yuv_to_rgb(int conversion_type, unsigned char *rgb, int rgbType, unsigned char const *yuv,
                int yuvType, int width, int height, unsigned char const *uv = nullptr);





float getVersion();


#ifdef __cplusplus
}
#endif
#endif //V_CAMERA2GL3_IMGCONVERSION_H

