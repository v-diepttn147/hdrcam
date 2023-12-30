//
// Created by JK on 7/27/20.
//

#include "fast_img_conversion.h"

#if defined(X86_NEON) // x86 NEON
#include "NEON_2_SSE.h"
#include "LOG_x86.h"


#elif defined(ARM_NEON) //ARM NEON
#include <arm_neon.h>
#include "LOG.h"
#endif
//#include <random>

uint16x4_t
clamp_to_char_range(uint16x4_t input, int16x4_t const minUChar, int16x4_t const maxUChar) {
    uint16x4_t tmp = vmin_u16(input, maxUChar);
    tmp = vmax_u16(tmp, minUChar);
    return tmp;
}

uint16x4_t clamp_to_char_range(float32x4_t input, int16x4_t const minUChar, int16x4_t const
maxUChar) {
    //  uint16x4_t input_uint_32 = vmovn_s32(vcvtq_s32_f32(input));
    uint16x4_t tmp = vmin_s16(vmovn_s32(vcvtq_s32_f32(input)), maxUChar);
    tmp = vmax_s16(tmp, minUChar);
    return tmp;
}
//#include <arm_neon.h>

template<typename trait>
bool decode_yuv_neon_float_android(unsigned char *out, int rgbType, unsigned char const *y,
                                   unsigned char
                                   const *uv, int yuvType, int width, int height,
                                   unsigned char fill_alpha = 0xff) {
    LOGI("NEON function: decode_yuv_neon_float_android");
    // pre-condition : width, height must be even
    if (0 != (width & 1) || width < 2 || 0 != (height & 1) || height < 2 || !out || !y || !uv) {
        LOGE("Unsupported image size.");
        return false;
    }


    unsigned char *dst = out;

    // constants
    int const stride = width * trait::bytes_per_pixel;
    int const itHeight = height >> 1;
    int const itWidth = width >> 3;

    int16x8_t const half = vdupq_n_u16(128);



    int16x4_t const minUChar = vdup_n_s16(0);
    int16x4_t const maxUChar = vdup_n_s16(255);
    // tmp variable
    uint16x8_t t;

    // pixel block to temporary store 8 pixels
    typename trait::PixelBlock pblock = trait::init_pixelblock(fill_alpha);

    // default value for YUV - NV12

    int uOrder = 0;
    int vOrder = 1;

    if (yuvType == YUV_TYPE::NV21) {
        uOrder = 1;
        vOrder = 0;
    }

    float32_t rK[3] = {1.0f, 1.370705f, 0.0f};
    float32_t gK[3] = {1.0f, -0.698001f, -0.337633f};
    float32_t bK[3] = {1.0f, 1.732446f, 0.0f};

    for (int j = 0; j < itHeight; ++j, y += width, dst += stride) {
        for (int i = 0; i < itWidth; ++i, y += 8, uv += 8, dst += (8 * trait::bytes_per_pixel)) {
            t = vmovl_u8(vld1_u8(y));
            float32x4_t const Y00 = vcvtq_f32_s32(vmovl_u16(vget_low_u16(t)));
            float32x4_t const Y01 = vcvtq_f32_s32(vmovl_u16(vget_high_u16(t)));// 298*c

            t = vmovl_u8(vld1_u8(y + width));
            float32x4_t const Y10 = vcvtq_f32_s32(vmovl_u16(vget_low_u16(t)));// 298*c
            float32x4_t const Y11 = vcvtq_f32_s32(vmovl_u16(vget_high_u16(t)));// 298*c

            // trait::loadvu pack 4 sets of uv into a uint8x8_t, layout : { v0,u0, v1,u1, v2,u2, v3,u3 }
            uint8x8_t vvv = trait::loadvu(uv);
            t = vsubq_s16((int16x8_t) vmovl_u8(vvv), half);

            // UV.val[0] : v0, v1, v2, v3
            // UV.val[1] : u0, u1, u2, u3
            float32x4x2_t const UV = vuzpq_f32(vcvtq_f32_s32(vmovl_s16(vget_low_s16(t))),
                                               vcvtq_f32_s32(vmovl_s16(vget_high_s16(t))));

            //  int32x4_t const tR = vmlal_n_s16(rounding, UV.val[vOrder], 409);


            float32x4_t tR = vmulq_n_f32((UV.val[vOrder]), rK[1]);
            float32x4_t tG = vaddq_f32(vmulq_n_f32((UV.val[vOrder]), gK[1]),
                                       vmulq_n_f32((UV.val[uOrder]), gK[2]));
            float32x4_t tB = vmulq_n_f32((UV.val[uOrder]), bK[1]);

            float32x4_t R00_f = ((vaddq_f32(tR, Y00)));
            float32x4_t R01_f = ((vaddq_f32(tR, Y01)));
            float32x4_t G00_f = ((vaddq_f32(tG, Y00)));
            float32x4_t G01_f = ((vaddq_f32(tG, Y01)));
            float32x4_t B00_f = ((vaddq_f32(tB, Y00)));
            float32x4_t B01_f = ((vaddq_f32(tB, Y01)));

            uint16x4_t R00 = clamp_to_char_range(R00_f, minUChar, maxUChar);
            uint16x4_t R01 = clamp_to_char_range(R01_f, minUChar, maxUChar);
            uint16x4_t G00 = clamp_to_char_range(G00_f, minUChar, maxUChar);
            uint16x4_t G01 = clamp_to_char_range(G01_f, minUChar, maxUChar);
            uint16x4_t B00 = clamp_to_char_range(B00_f, minUChar, maxUChar);
            uint16x4_t B01 = clamp_to_char_range(B01_f, minUChar, maxUChar);

            uint16x8_t rC = vcombine_u16(R00, R01);
            uint16x8_t gC = vcombine_u16(G00, G01);
            uint16x8_t bC = vcombine_u16(B00, B01);
            // upper 8 pixels
            trait::store_pixel_block(dst, pblock,
                                     vqmovun_s16(rC),
                                     vqmovun_s16(gC),
                                     vqmovun_s16(bC));

            // lower 8 pixels
            float32x4_t R10_f = ((vaddq_f32(tR, Y10)));
            float32x4_t R11_f = ((vaddq_f32(tR, Y11)));
            float32x4_t G10_f = ((vaddq_f32(tG, Y10)));
            float32x4_t G11_f = ((vaddq_f32(tG, Y11)));
            float32x4_t B10_f = ((vaddq_f32(tB, Y10)));
            float32x4_t B11_f = ((vaddq_f32(tB, Y11)));

            uint16x4_t R10 = clamp_to_char_range(R10_f, minUChar, maxUChar);
            uint16x4_t R11 = clamp_to_char_range(R11_f, minUChar, maxUChar);
            uint16x4_t G10 = clamp_to_char_range(G10_f, minUChar, maxUChar);
            uint16x4_t G11 = clamp_to_char_range(G11_f, minUChar, maxUChar);
            uint16x4_t B10 = clamp_to_char_range(B10_f, minUChar, maxUChar);
            uint16x4_t B11 = clamp_to_char_range(B11_f, minUChar, maxUChar);

            rC = vcombine_u16(R10, R11);
            gC = vcombine_u16(G10, G11);
            bC = vcombine_u16(B10, B11);
            trait::store_pixel_block(dst + stride, pblock,
                                     vqmovun_s16(rC
                                     ),
                                     vqmovun_s16(gC
                                     ),
                                     vqmovun_s16(bC
                                     ));
        }
    }
    return true;
}

template<typename trait>
bool decode_yuv_neon_float_opencv_like(unsigned char *out, int rgbType, unsigned char const *y,
        unsigned
char
const *uv, int yuvType, int width, int height, unsigned char fill_alpha = 0xff) {
    LOGI( "NEON function: decode_yuv_neon_float");


    // pre-condition : width, height must be even
    if (0 != (width & 1) || width < 2 || 0 != (height & 1) || height < 2 || !out || !y || !uv){
        LOGE( "Unsupported image size.");
        return false;
    }

    //vld3q_u8(out);
    // in & out pointers
    unsigned char *dst = out;

    // constants
    int const stride = width * trait::bytes_per_pixel;
    int const itHeight = height >> 1;
    int const itWidth = width >> 3;

    uint8x8_t const Yshift = vdup_n_u8(0);
    int16x8_t const half = vdupq_n_u16(0);

    int32x4_t const r_rounding = vdupq_n_s32(-179);
    int32x4_t const g_rounding = vdupq_n_s32(135);
    int32x4_t const b_rounding = vdupq_n_s32(-227);

    int16x4_t const minUChar = vget_low_s16(vdupq_n_s16(0));
    int16x4_t const maxUChar = vget_low_s16(vdupq_n_s16(255));
    // tmp variable
    uint16x8_t t;

    // pixel block to temporary store 8 pixels
    typename trait::PixelBlock pblock = trait::init_pixelblock(fill_alpha);

    // default value for YUV - NV12

    int uOrder = 0;
    int vOrder = 1;

    if (yuvType == YUV_TYPE::NV21) {
        uOrder = 1;
        vOrder = 0;
    }
    float32_t r1024 = 1.0 / 1024.0;
    float32_t r131072 = 1.0 / 131072.0;
    for (int j = 0; j < itHeight; ++j, y += width, dst += stride) {
        for (int i = 0; i < itWidth; ++i, y += 8, uv += 8, dst += (8 * trait::bytes_per_pixel)) {
            t = vmovl_u8(vqsub_u8(vld1_u8(y), Yshift)); //c = Y - 16;
            int32x4_t const Y00 = vmulq_n_u32(vmovl_u16(vget_low_u16(t)), 1); // 298*c
            int32x4_t const Y01 = vmulq_n_u32(vmovl_u16(vget_high_u16(t)), 1);// 298*c

            t = vmovl_u8(vqsub_u8(vld1_u8(y + width), Yshift));
            int32x4_t const Y10 = vmulq_n_u32(vmovl_u16(vget_low_u16(t)), 1);// 298*c
            int32x4_t const Y11 = vmulq_n_u32(vmovl_u16(vget_high_u16(t)), 1);// 298*c

            // trait::loadvu pack 4 sets of uv into a uint8x8_t, layout : { v0,u0, v1,u1, v2,u2, v3,u3 }
            t = vsubq_s16((int16x8_t) vmovl_u8(trait::loadvu(uv)), half);

            // UV.val[0] : v0, v1, v2, v3
            // UV.val[1] : u0, u1, u2, u3
            int16x4x2_t const UV = vuzp_s16(vget_low_s16(t), vget_high_s16(t));

            // tR : 128+409V
            // tG : 128-100U-208V
            // tB : 128+516U
            int32x4_t const tR = vmlal_n_s16
                    (r_rounding,
                     vmovn_u32(
                             vmulq_n_f32(
                                     vmulq_n_f32(
                                             vmovl_u16(UV.val[vOrder]),
                                             1436),
                                     r1024
                             )
                     ),
                     1
                    );
            int32x4_t const tG =
                    vmlal_n_s16(
                            vmlal_n_s16
                                    (g_rounding,
                                     vmovn_u32(
                                             vmulq_n_f32(
                                                     vmulq_n_f32(
                                                             vmovl_u16(UV.val[vOrder]),
                                                             93604
                                                     ),
                                                     r131072
                                             )),
                                     -1),
                            vmovn_u32(
                                    vmulq_n_f32(
                                            vmulq_n_f32(
                                                    vmovl_u16(UV.val[uOrder]),
                                                    46549),
                                            r131072
                                    )),
                            -1);
            int32x4_t const tB = vmlal_n_s16(
                    b_rounding,
                    vmovn_u32(
                            vmulq_n_f32(
                                    vmulq_n_f32(
                                            vmovl_u16(UV.val[uOrder]),
                                            1814),
                                    r1024)),
                    1);

            int32x4x2_t const R = vzipq_s32(tR, tR); // [tR0, tR0, tR1, tR1] [ tR2, tR2, tR3, tR3]
            int32x4x2_t const G = vzipq_s32(tG, tG); // [tG0, tG0, tG1, tG1] [ tG2, tG2, tG3, tG3]
            int32x4x2_t const B = vzipq_s32(tB, tB); // [tB0, tB0, tB1, tB1] [ tB2, tB2, tB3, tB3]

            uint16x8_t rC = vcombine_u16((vqmovun_s32(vaddq_s32(R.val[0], Y00))),
                                         (vqmovun_s32(
                                                 vaddq_s32(R.val[1], Y01))));
            uint16x8_t gC = vcombine_u16((vqmovun_s32(vaddq_s32(G.val[0], Y00))),
                                         (vqmovun_s32(
                                                 vaddq_s32(G.val[1], Y01))));

            uint16x8_t bC = vcombine_u16((vqmovun_s32(vaddq_s32(B.val[0], Y00))),
                                         (vqmovun_s32(
                                                 vaddq_s32(B.val[1], Y01))));
            // upper 8 pixels
            trait::store_pixel_block(dst, pblock,
                                     vqmovun_s16(rC),
                                     vqmovun_s16(gC),
                                     vqmovun_s16(bC));

            // lower 8 pixels
            rC = vcombine_u16((vqmovun_s32(vaddq_s32(R.val[0], Y10))),
                              (vqmovun_s32(
                                      vaddq_s32(R.val[1], Y11))));
            gC = vcombine_u16((vqmovun_s32(vaddq_s32(G.val[0], Y10))),
                              (vqmovun_s32(
                                      vaddq_s32(G.val[1], Y11))));

            bC = vcombine_u16((vqmovun_s32(vaddq_s32(B.val[0], Y10))),
                              (vqmovun_s32(
                                      vaddq_s32(B.val[1], Y11))));
            trait::store_pixel_block(dst + stride, pblock,
                                     vqmovun_s16(rC
                                     ),
                                     vqmovun_s16(gC
                                     ),
                                     vqmovun_s16(bC
                                     ));
        }
    }
    return true;
}


template<typename trait>
bool decode_yuv_neon_int(unsigned char *out, int rgbType, unsigned char const *y, unsigned char
const *uv, int yuvType, int width, int height, unsigned char fill_alpha = 0xff) {
    LOGI( "NEON function: decode_yuv_neon_int");

    // pre-condition : width, height must be even
    if (0 != (width & 1) || width < 2 || 0 != (height & 1) || height < 2 || !out || !y || !uv) {
        LOGE("Unsupported image size.");
        return false;
    }

    //vld3q_u8(out);
    // in & out pointers
    unsigned char *dst = out;

    // constants
    int const stride = width * trait::bytes_per_pixel;
    int const itHeight = height >> 1;
    int const itWidth = width >> 3;

    uint8x8_t const Yshift = vdup_n_u8(16);
    int16x8_t const half = vdupq_n_u16(128);
    int32x4_t const rounding = vdupq_n_s32(128);

    // tmp variable
    uint16x8_t t;

    // pixel block to temporary store 8 pixels
    typename trait::PixelBlock pblock = trait::init_pixelblock(fill_alpha);

    // default value for YUV - NV12

    int uOrder = 0;
    int vOrder = 1;

    if (yuvType == YUV_TYPE::NV21) {
        uOrder = 1;
        vOrder = 0;
    }
    for (int j = 0; j < itHeight; ++j, y += width, dst += stride) {
        for (int i = 0; i < itWidth; ++i, y += 8, uv += 8, dst += (8 * trait::bytes_per_pixel)) {
            t = vmovl_u8(vqsub_u8(vld1_u8(y), Yshift)); //c = Y - 16;
            int32x4_t const Y00 = vmulq_n_u32(vmovl_u16(vget_low_u16(t)), 298); // 298*c
            int32x4_t const Y01 = vmulq_n_u32(vmovl_u16(vget_high_u16(t)), 298);// 298*c

            t = vmovl_u8(vqsub_u8(vld1_u8(y + width), Yshift));
            int32x4_t const Y10 = vmulq_n_u32(vmovl_u16(vget_low_u16(t)), 298);// 298*c
            int32x4_t const Y11 = vmulq_n_u32(vmovl_u16(vget_high_u16(t)), 298);// 298*c

            // trait::loadvu pack 4 sets of uv into a uint8x8_t, layout : { v0,u0, v1,u1, v2,u2, v3,u3 }
            t = vsubq_s16((int16x8_t) vmovl_u8(trait::loadvu(uv)), half);

            // UV.val[0] : v0, v1, v2, v3
            // UV.val[1] : u0, u1, u2, u3
            int16x4x2_t const UV = vuzp_s16(vget_low_s16(t), vget_high_s16(t));

            // tR : 128+409V
            // tG : 128-100U-208V
            // tB : 128+516U
            int32x4_t const tR = vmlal_n_s16(rounding, UV.val[vOrder], 409);
            int32x4_t const tG = vmlal_n_s16(vmlal_n_s16(rounding, UV.val[vOrder], -208),
                                             UV.val[uOrder],
                                             -100);
            int32x4_t const tB = vmlal_n_s16(rounding, UV.val[uOrder], 516);

            int32x4x2_t const R = vzipq_s32(tR, tR); // [tR0, tR0, tR1, tR1] [ tR2, tR2, tR3, tR3]
            int32x4x2_t const G = vzipq_s32(tG, tG); // [tG0, tG0, tG1, tG1] [ tG2, tG2, tG3, tG3]
            int32x4x2_t const B = vzipq_s32(tB, tB); // [tB0, tB0, tB1, tB1] [ tB2, tB2, tB3, tB3]

            // upper 8 pixels
            trait::store_pixel_block(dst, pblock,
                                     vshrn_n_u16(vcombine_u16(vqmovun_s32(vaddq_s32(R.val[0], Y00)),
                                                              vqmovun_s32(
                                                                      vaddq_s32(R.val[1], Y01))),
                                                 8),
                                     vshrn_n_u16(vcombine_u16(vqmovun_s32(vaddq_s32(G.val[0], Y00)),
                                                              vqmovun_s32(
                                                                      vaddq_s32(G.val[1], Y01))),
                                                 8),
                                     vshrn_n_u16(vcombine_u16(vqmovun_s32(vaddq_s32(B.val[0], Y00)),
                                                              vqmovun_s32(
                                                                      vaddq_s32(B.val[1], Y01))),
                                                 8));

            // lower 8 pixels
            trait::store_pixel_block(dst + stride, pblock,
                                     vshrn_n_u16(vcombine_u16(vqmovun_s32(vaddq_s32(R.val[0], Y10)),
                                                              vqmovun_s32(
                                                                      vaddq_s32(R.val[1], Y11))),
                                                 8),
                                     vshrn_n_u16(vcombine_u16(vqmovun_s32(vaddq_s32(G.val[0], Y10)),
                                                              vqmovun_s32(
                                                                      vaddq_s32(G.val[1], Y11))),
                                                 8),
                                     vshrn_n_u16(vcombine_u16(vqmovun_s32(vaddq_s32(B.val[0], Y10)),
                                                              vqmovun_s32(
                                                                      vaddq_s32(B.val[1], Y11))),
                                                 8));
        }
    }
    return true;
}

//---------------------------------------------------- --------------------------
class YUVtoRGB_neon {
public:
    enum {
        bytes_per_pixel = 3
    };
    typedef uint8x8x3_t PixelBlock;

    static PixelBlock const init_pixelblock(unsigned char /*fill_alpha*/) {
        return uint8x8x3_t();
    }

    static uint8x8_t const loadvu(unsigned char const *uv) {
        return vld1_u8(uv);
    }

    static void store_pixel_block(unsigned char *dst, PixelBlock &pblock, uint8x8_t const &r,
                                  uint8x8_t const &g, uint8x8_t const &b) {
        pblock.val[0] = r;
        pblock.val[1] = g;
        pblock.val[2] = b;

        vst3_u8(dst, pblock);
    }
};

class YUVtoBGR_neon {
public:
    enum {
        bytes_per_pixel = 3
    };
    typedef uint8x8x3_t PixelBlock;

    static PixelBlock const init_pixelblock(unsigned char /*fill_alpha*/) {
        return uint8x8x3_t();
    }

    static uint8x8_t const loadvu(unsigned char const *uv) {
        return vld1_u8(uv);
    }

    static void store_pixel_block(unsigned char *dst, PixelBlock &pblock, uint8x8_t const &r,
                                  uint8x8_t const &g, uint8x8_t const &b) {
        pblock.val[0] = b;
        pblock.val[1] = g;
        pblock.val[2] = r;
        vst3_u8(dst, pblock);
    }
};
//bool yuv_to_rgb(unsigned char *rgb, int rgbType, unsigned char const *yuv, int yuvType, int width,
//                int height) {
//    return decode_yuv_neon<NV21toRGB_neon>(rgb, rgbType, yuv, yuv + (width * height), yuvType,
//                                           width,
//                                           height);
//}

bool yuv_to_rgb(int conversion_type, unsigned char *rgb, int rgbType, unsigned char const *yuv,
                int yuvType, int width, int height, unsigned char const *uv) {
    unsigned char const *uv_ = (uv == nullptr) ?
                               (yuv + (width * height))
                                               : uv;

    switch (rgbType) {
        case RGB_TYPE::RGB:
            if (conversion_type == CONVERSION_TYPE::YUV_TO_RGB_INT_WIKI) {
                return decode_yuv_neon_int<YUVtoRGB_neon>(rgb, rgbType, yuv, uv_,
                                                          yuvType, width,
                                                          height);
            }

            if (conversion_type == CONVERSION_TYPE::YUV_TO_RGB_FLOAT_OPEN_CV_LIKE) {
                return decode_yuv_neon_float_opencv_like<YUVtoRGB_neon>(rgb, rgbType, yuv, uv_,
                                                                    yuvType, width,
                                                                    height);
            }

            if (conversion_type == CONVERSION_TYPE::YUV_TO_RGB_FLOAT_ANDROID) {
                return decode_yuv_neon_float_android<YUVtoRGB_neon>(rgb, rgbType, yuv, uv_,
                                                                        yuvType, width,
                                                                        height);
            }

        case RGB_TYPE::BGR:
            if (conversion_type == CONVERSION_TYPE::YUV_TO_RGB_INT_WIKI) {
                return decode_yuv_neon_int<YUVtoBGR_neon>(rgb, rgbType, yuv, uv_,
                                                          yuvType, width,
                                                          height);
            }

            if (conversion_type == CONVERSION_TYPE::YUV_TO_RGB_FLOAT_OPEN_CV_LIKE) {
                return decode_yuv_neon_float_android<YUVtoBGR_neon>(rgb, rgbType, yuv, uv_,
                                                                    yuvType, width,
                                                                    height);
            }

            if (conversion_type == CONVERSION_TYPE::YUV_TO_RGB_FLOAT_ANDROID) {
                return decode_yuv_neon_float_android<YUVtoBGR_neon>(rgb, rgbType, yuv, uv_,
                                                                    yuvType, width,
                                                                    height);
            }

        default:
            return false;
    }

    return false;

}

//------------------------------------------------------------------------------
class YUVtoRGBA_neon {
public:
    enum {
        bytes_per_pixel = 4
    };
    typedef uint8x8x4_t PixelBlock;

    static PixelBlock const init_pixelblock(unsigned char fill_alpha) {
        PixelBlock block;
        block.val[3] = vdup_n_u8(fill_alpha); // alpha channel in the last
        return block;
    }

    static uint8x8_t const loadvu(unsigned char const *uv) {
        return vld1_u8(uv);
    }

    static void store_pixel_block(unsigned char *dst, PixelBlock &pblock, uint8x8_t const &r,
                                  uint8x8_t const &g, uint8x8_t const &b) {
        pblock.val[0] = r;
        pblock.val[1] = g;
        pblock.val[2] = b;
        vst4_u8(dst, pblock);
    }
};

//bool nv21_to_rgba(unsigned char *rgba, unsigned char alpha, unsigned char const *nv21, int width,
//                  int height, bool vuSwapped) {
//    return decode_yuv_neon<NV21toRGBA_neon>(rgba, nv21, nv21 + (width * height), width, height,
//                                            alpha, vuSwapped);
//}

//------------------------------------------------------------------------------
class YUVtoBGRA_neon {
public:
    enum {
        bytes_per_pixel = 4
    };
    typedef uint8x8x4_t PixelBlock;

    static PixelBlock const init_pixelblock(unsigned char fill_alpha) {
        PixelBlock block;
        block.val[3] = vdup_n_u8(fill_alpha); // alpha channel in the last
        return block;
    }

    static uint8x8_t const loadvu(unsigned char const *uv) {
        return vld1_u8(uv);
    }

    static void store_pixel_block(unsigned char *dst, PixelBlock &pblock, uint8x8_t const &r,
                                  uint8x8_t const &g, uint8x8_t const &b) {
        pblock.val[0] = b;
        pblock.val[1] = g;
        pblock.val[2] = r;
        vst4_u8(dst, pblock);
    }
};

//bool nv21_to_bgra(unsigned char *rgba, unsigned char alpha, unsigned char const *nv21, int width,
//                  int height, bool vuSwapped) {
//    return decode_yuv_neon<NV21toBGRA_neon>(rgba, nv21, nv21 + (width * height), width, height,
//                                            alpha, vuSwapped);
//}

//------------------------------------------------------------------------------


//bool nv21_to_bgr(unsigned char *bgr, unsigned char const *nv21, int width, int height, bool
//vuSwapped) {
//    return decode_yuv_neon<NV21toBGR_neon>(bgr, nv21, nv21 + (width * height), width, height,
//                                           vuSwapped);
//}
