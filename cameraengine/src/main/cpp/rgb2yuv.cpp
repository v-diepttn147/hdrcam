
/*
  * The latest version and instruction for NEON_2_SSE.h is at:
  *    https://github.com/intel/ARM_NEON_2_x86_SSE
  */

#if defined(X86_NEON)// x86 NEON
#include "NEON_2_SSE.h"
#include "LOG_x86.h"

#elif defined(ARM_NEON) //ARM NEON
#include <arm_neon.h>
#include "LOG.h"
#endif

#include <cassert>

#include "fast_img_conversion.h"


int16x4_t vecMul(int32_t kR, int32_t kG, int32_t kB, int32x4_t R, int32x4_t G, int32x4_t B) {
    int32x4_t rt;
    rt = vmulq_n_s32(R, kR);
    int32x4_t gt;
    gt = vmulq_n_s32(G, kG);
    int32x4_t bt;
    bt = vmulq_n_s32(B, kB);

    float32x4_t sum;
    sum = vcvtq_f32_s32(vaddq_u32(vaddq_u32(rt, gt), bt));
    float32_t r1000 = 0.001;
    float32x4_t yT = vmulq_n_f32(sum, r1000);
    int16x4_t y_ret = vmovn_s32(vcvtq_s32_f32(yT));
    return y_ret;
}

bool rgb_to_yuv_float(unsigned char *yuv, int yuvType, unsigned char const *rgb, int rgbType,
                           int
                           width, int height) {


    if (width < 16 || width % 2 != 0 || height % 2 != 0) {
        LOGE( "Unsupported image size. Width must be > 16. Width and height must be even.");
        return false;
    }
    const uint16x8_t u16_rounding = vdupq_n_u16(128);
    const int16x8_t s16_rounding = vdupq_n_s16(128);
//    const int8x8_t s8_rounding = vdup_n_s8(128);
    const uint8x8_t u8_rounding = vdup_n_u8(128);
    const uint8x16_t offset = vdupq_n_u8(-16);
    const uint16x8_t mask = vdupq_n_u16(255);

    int frameSize = width * height;

    int yIndex = 0;
    int uvIndex = frameSize;

    // default value for YUV - NV12
    int uOrder = 0;
    int vOrder = 1;

    // default value for RGB
    int rgbChannelNum = 3;
    int rOrder = 0;
    int bOrder = 2;

    //NV21 has swapped UV order
    if (yuvType == YUV_TYPE::NV21) {
        uOrder = 1;
        vOrder = 0;
    }

    //BGRx has swapped BR order
    if (rgbType == RGB_TYPE::BGR || rgbType == RGB_TYPE::BGRA) {
        rOrder = 0;
        bOrder = 2;
    }

    //rgb with alpha
    if (rgbType == RGB_TYPE::RGBA || rgbType == RGB_TYPE::BGRA) {
        rgbChannelNum = 4;
    }

    int i;
    int j;

    int yK[3] = {299, 587, 114};
    int vK[3] = {499, -418, -81};//Cr
    int uK[3] = {-169, -331, 499};//Cb
    for (j = 0; j < height; j++) {
        for (i = 0; i < width >> 4; i++) {
            // Load rgb
            uint8x16x3_t pixel_rgb;
            if (rgbChannelNum == 3) {
                pixel_rgb = vld3q_u8(rgb);
            } else {
                uint8x16x4_t pixel_argb = vld4q_u8(rgb);
                pixel_rgb.val[0] = pixel_argb.val[0];
                pixel_rgb.val[1] = pixel_argb.val[1];
                pixel_rgb.val[2] = pixel_argb.val[2];
            }
            rgb += rgbChannelNum * 16;

            uint16x8x2_t rT;
            uint16x8x2_t gT;
            uint16x8x2_t bT;

            rT.val[0] = vmovl_u8(vget_low_u8(pixel_rgb.val[rOrder]));
            rT.val[1] = vmovl_u8(vget_high_u8(pixel_rgb.val[rOrder]));
            gT.val[0] = vmovl_u8(vget_low_u8(pixel_rgb.val[1]));
            gT.val[1] = vmovl_u8(vget_high_u8(pixel_rgb.val[1]));
            bT.val[0] = vmovl_u8(vget_low_u8(pixel_rgb.val[bOrder]));
            bT.val[1] = vmovl_u8(vget_high_u8(pixel_rgb.val[bOrder]));

            int32x4x2_t rT0;
            int32x4x2_t rT1;
            int32x4x2_t gT0;
            int32x4x2_t gT1;
            int32x4x2_t bT0;
            int32x4x2_t bT1;

            rT0.val[0] = vmovl_u16(vget_low_u16(rT.val[0]));
            rT0.val[1] = vmovl_u16(vget_high_u16(rT.val[0]));
            rT1.val[0] = vmovl_u16(vget_low_u16(rT.val[1]));
            rT1.val[1] = vmovl_u16(vget_high_u16(rT.val[1]));

            gT0.val[0] = vmovl_u16(vget_low_u16(gT.val[0]));
            gT0.val[1] = vmovl_u16(vget_high_u16(gT.val[0]));
            gT1.val[0] = vmovl_u16(vget_low_u16(gT.val[1]));
            gT1.val[1] = vmovl_u16(vget_high_u16(gT.val[1]));

            bT0.val[0] = vmovl_u16(vget_low_u16(bT.val[0]));
            bT0.val[1] = vmovl_u16(vget_high_u16(bT.val[0]));
            bT1.val[0] = vmovl_u16(vget_low_u16(bT.val[1]));
            bT1.val[1] = vmovl_u16(vget_high_u16(bT.val[1]));

            int16x4_t y_res_0 = vecMul(299, 587, 114, rT0.val[0], gT0.val[0], bT0.val[0]);
            int16x4_t y_res_1 = vecMul(299, 587, 114, rT0.val[1], gT0.val[1], bT0.val[1]);
            int16x4_t y_res_2 = vecMul(299, 587, 114, rT1.val[0], gT1.val[0], bT1.val[0]);
            int16x4_t y_res_3 = vecMul(299, 587, 114, rT1.val[1], gT1.val[1], bT1.val[1]);

            uint8x8_t y_res_01 = vmovn_u16(vcombine_s16(y_res_0, y_res_1));
            uint8x8_t y_res_23 = vmovn_u16(vcombine_s16(y_res_2, y_res_3));

            uint8x16_t y_res = vcombine_u8(y_res_01, y_res_23);

            vst1q_u8(yuv + yIndex, y_res);
            yIndex += 16;

            // Compute u and v in the even row
            if (j % 2 == 0) {

                int32x4_t r_uv_4L = vuzpq_s32(rT0.val[0], rT0.val[1]).val[0];
                int32x4_t g_uv_4L = vuzpq_s32(gT0.val[0], gT0.val[1]).val[0];
                int32x4_t b_uv_4L = vuzpq_s32(bT0.val[0], bT0.val[1]).val[0];

                int32x4_t r_uv_4H = vuzpq_s32(rT1.val[0], rT1.val[1]).val[0];
                int32x4_t g_uv_4H = vuzpq_s32(gT1.val[0], gT1.val[1]).val[0];
                int32x4_t b_uv_4H = vuzpq_s32(bT1.val[0], bT1.val[1]).val[0];

                int16x4_t u_4L = vecMul(uK[0], uK[1], uK[2], r_uv_4L, g_uv_4L, b_uv_4L);
                int16x4_t v_4L = vecMul(vK[0], vK[1], vK[2], r_uv_4L, g_uv_4L, b_uv_4L);

                int16x4_t u_4H = vecMul(uK[0], uK[1], uK[2], r_uv_4H, g_uv_4H, b_uv_4H);
                int16x4_t v_4H = vecMul(vK[0], vK[1], vK[2], r_uv_4H, g_uv_4H, b_uv_4H);

                int16x8_t u_8A = (vcombine_s16(u_4L, u_4H));
                int16x8_t v_8A = (vcombine_s16(v_4L, v_4H));

                //int16x8x2_t uv =  vzipq_s16(u_8A, v_8A);
                uint8x8x2_t uv_ret;


                if (yuvType == YUV_TYPE::NV21) {
                    uv_ret.val[1] = vmovn_u16(vaddq_s16(u_8A, s16_rounding));
                    uv_ret.val[0] = vmovn_u16(vaddq_s16(v_8A, s16_rounding));
                } else if (yuvType == YUV_TYPE::NV12) {
                    uv_ret.val[0] = vmovn_u16(vaddq_s16(u_8A, s16_rounding));
                    uv_ret.val[1] = vmovn_u16(vaddq_s16(v_8A, s16_rounding));
                }


                vst2_u8(yuv + uvIndex, uv_ret);

                uvIndex += 2 * 8;
            }
        }

        // Handle leftovers
        for (i = ((width >> 4) << 4); i < width; i++) {
            uint8_t r = rgb[rOrder];
            uint8_t g = rgb[1];
            uint8_t b = rgb[bOrder];

            rgb += rgbChannelNum;

            uint8_t y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
            uint8_t u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
            uint8_t v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;

            yuv[yIndex++] = y;
            if (j % 2 == 0 && i % 2 == 0) {
                yuv[uvIndex + 1 + uOrder] = u;
                yuv[uvIndex + 2 - uOrder] = v;
                uvIndex += 2;
            }
        }
    }
    return true;
}

bool rgb_to_yuv_int(int conversion_type, unsigned char *yuv, int yuvType, unsigned char const
*rgb, int rgbType, int
width, int height) {
    if (width < 16 || width % 2 != 0 || height % 2 != 0) {
        LOGE("Unsupported image size. Width must be > 16. Width and height must be even.");
        return false;

    }

    //full swing
    int yK[3] = {77, 150, 29};
    int uK[3] = {-43, -84, 127};//Cr
    int vK[3] = {127, -106, -21};//Cb
    int padding = 0;

    //studio swing
    if (conversion_type == CONVERSION_TYPE::RGB_TO_YUV_INT_STUDIO_SWING) {
        yK[0] = 66; yK[1] = 129; yK[2] = 25;
        uK[0] = -38; uK[1] = -74; uK[2] = 112;
        vK[0] = 112; vK[1] = -94; vK[2] = -18;
        padding = 16;
    }
//    int yK[3] = {66, 129, 25};
//    int uK[3] = {-38, -74, 112};//Cr
//    int vK[3] = {112, -94, -18};//Cb
//    int padding = 16;

    const uint16x8_t u16_rounding = vdupq_n_u16(128);
    const int16x8_t s16_rounding = vdupq_n_s16(128);
    //const int8x8_t s8_rounding = vdup_n_s8(128);
    const uint8x8_t u8_rounding = vdup_n_u8(128);
    const uint16x8_t mask = vdupq_n_u16(255);

    int frameSize = width * height;

    int yIndex = 0;
    int uvIndex = frameSize;

    // default value for YUV - NV12
    int uOrder = 0;
    int vOrder = 1;

    // default value for RGB
    int rgbChannelNum = 3;
    int rOrder = 0;
    int bOrder = 2;

    //NV21 has swapped UV order
    if (yuvType == YUV_TYPE::NV21) {
        uOrder = 1;
        vOrder = 0;
    }

    //BGRx has swapped BR order
    if (rgbType == RGB_TYPE::BGR || rgbType == RGB_TYPE::BGRA) {
        rOrder = 0;
        bOrder = 2;
    }

    //rgb with alpha
    if (rgbType == RGB_TYPE::RGBA || rgbType == RGB_TYPE::BGRA) {
        rgbChannelNum = 4;
    }

    int i;
    int j;



    const uint8x16_t offset = vdupq_n_u8(padding);
    for (j = 0; j < height; j++) {
        for (i = 0; i < width >> 4; i++) {
            // Load rgb
            uint8x16x3_t pixel_rgb;
            if (rgbChannelNum == 3) {
                pixel_rgb = vld3q_u8(rgb);
            } else {
                uint8x16x4_t pixel_argb = vld4q_u8(rgb);
                pixel_rgb.val[0] = pixel_argb.val[0];
                pixel_rgb.val[1] = pixel_argb.val[1];
                pixel_rgb.val[2] = pixel_argb.val[2];
            }
            rgb += rgbChannelNum * 16;

            uint8x8x2_t uint8_r;
            uint8x8x2_t uint8_g;
            uint8x8x2_t uint8_b;

            uint8_r.val[0] = vget_low_u8(pixel_rgb.val[rOrder]);
            uint8_r.val[1] = vget_high_u8(pixel_rgb.val[rOrder]);
            uint8_g.val[0] = vget_low_u8(pixel_rgb.val[1]);
            uint8_g.val[1] = vget_high_u8(pixel_rgb.val[1]);
            uint8_b.val[0] = vget_low_u8(pixel_rgb.val[bOrder]);
            uint8_b.val[1] = vget_high_u8(pixel_rgb.val[bOrder]);

            uint16x8x2_t uint16_y;
            uint8x8_t scalar = vdup_n_u8(yK[0]);
            uint8x16_t y;

            uint16_y.val[0] = vmull_u8(uint8_r.val[0], scalar);
            uint16_y.val[1] = vmull_u8(uint8_r.val[1], scalar);
            scalar = vdup_n_u8(yK[1]);
            uint16_y.val[0] = vmlal_u8(uint16_y.val[0], uint8_g.val[0], scalar);
            uint16_y.val[1] = vmlal_u8(uint16_y.val[1], uint8_g.val[1], scalar);
            scalar = vdup_n_u8(yK[2]);
            uint16_y.val[0] = vmlal_u8(uint16_y.val[0], uint8_b.val[0], scalar);
            uint16_y.val[1] = vmlal_u8(uint16_y.val[1], uint8_b.val[1], scalar);

            uint16_y.val[0] = vaddq_u16(uint16_y.val[0], u16_rounding);
            uint16_y.val[1] = vaddq_u16(uint16_y.val[1], u16_rounding);

            y = vcombine_u8(vqshrn_n_u16(uint16_y.val[0], 8), vqshrn_n_u16(uint16_y.val[1], 8));
            y = vaddq_u8(y, offset);

            vst1q_u8(yuv + yIndex, y);
            yIndex += 16;

            // Compute u and v in the even row
            if (j % 2 == 0) {
                int16x8_t u_scalar = vdupq_n_s16(uK[0]);
                int16x8_t v_scalar = vdupq_n_s16(vK[0]);

                int16x8_t r = vreinterpretq_s16_u16(
                        vandq_u16(vreinterpretq_u16_u8(pixel_rgb.val[rOrder]), mask));
                int16x8_t g = vreinterpretq_s16_u16(
                        vandq_u16(vreinterpretq_u16_u8(pixel_rgb.val[1]), mask));
                int16x8_t b = vreinterpretq_s16_u16(
                        vandq_u16(vreinterpretq_u16_u8(pixel_rgb.val[bOrder]), mask));

                int16x8_t u;
                int16x8_t v;
                uint8x8x2_t uv;

                u = vmulq_s16(r, u_scalar);
                v = vmulq_s16(r, v_scalar);

                u_scalar = vdupq_n_s16(uK[1]);
                v_scalar = vdupq_n_s16(vK[1]);
                u = vmlaq_s16(u, g, u_scalar);
                v = vmlaq_s16(v, g, v_scalar);

                u_scalar = vdupq_n_s16(uK[2]);
                v_scalar = vdupq_n_s16(vK[2]);
                u = vmlaq_s16(u, b, u_scalar);
                v = vmlaq_s16(v, b, v_scalar);

                u = vaddq_s16(u, s16_rounding);
                v = vaddq_s16(v, s16_rounding);

                if (yuvType == YUV_TYPE::NV21) {
                    uv.val[0] = (
                            vadd_u8(vqshrn_n_u16(v, 8), u8_rounding));
                    uv.val[1] = (
                            vadd_u8(vqshrn_n_u16(u, 8), u8_rounding));
                } else if (yuvType == YUV_TYPE::NV12) {
                    uv.val[0] = (
                            vadd_u8(vqshrn_n_u16(u, 8), u8_rounding));
                    uv.val[1] = (
                            vadd_u8(vqshrn_n_u16(v, 8), u8_rounding));
                }

                vst2_u8(yuv + uvIndex, uv);

                uvIndex += 2 * 8;
            }
        }

        // Handle leftovers
        for (i = ((width >> 4) << 4); i < width; i++) {
            uint8_t r = rgb[rOrder];
            uint8_t g = rgb[1];
            uint8_t b = rgb[bOrder];

            rgb += rgbChannelNum;

            uint8_t y = ((yK[0] * r + yK[1] * g + yK[2] * b + 128) >> 8) + 16;
            uint8_t u = ((-uK[0] * r + uK[1] * g + uK[2] * b + 128) >> 8) + 128;
            uint8_t v = ((vK[0] * r + vK[1] * g + vK[2] * b + 128) >> 8) + 128;

            yuv[yIndex++] = y;
            if (j % 2 == 0 && i % 2 == 0) {
                yuv[uvIndex + 1 + uOrder] = u;
                yuv[uvIndex + 2 - uOrder] = v;
                uvIndex += 2;
            }
        }
    }
    return true;
}

bool rgb_to_yuv(int conversion_type, unsigned char *yuv, int yuvType, unsigned char const *rgb,
        int rgbType, int
width, int height) {
    if (conversion_type == CONVERSION_TYPE::RGB_TO_YUV_INT_STUDIO_SWING || conversion_type ==
    CONVERSION_TYPE::RGB_TO_YUV_INT_FULL_SWING) {
        return rgb_to_yuv_int(conversion_type, yuv, yuvType, rgb, rgbType, width, height);
    }

    if (conversion_type == CONVERSION_TYPE::RGB_TO_YUV_FLOAT_OPEN_CV_LIKE) {
        return rgb_to_yuv_float(yuv, yuvType, rgb, rgbType, width, height);
    }

    return false;
}

