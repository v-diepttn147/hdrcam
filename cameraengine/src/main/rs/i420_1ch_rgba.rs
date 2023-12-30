#pragma version(1)
  #pragma rs java_package_name(com.hdrcam.camera);
  #pragma  rs_fp_relaxed


  //uint picWidth;
  uint uvRowStride;
  uint y_len;
  uint u_len;

  rs_allocation ypsIn;

 // The LaunchOptions ensure that the Kernel does not enter the padding  zone of Y, so yRowStride can be ignored WITHIN the Kernel.
 uchar4 __attribute__((kernel)) doConvert(uint32_t x, uint32_t y) {

 // index for accessing the uIn's and vIn's
    uint uIndex= y_len + x/2 + uvRowStride*(y/2);

    // get the y,u,v values
    uchar yps = rsGetElementAt_uchar(ypsIn, x, y);
    uchar u= rsGetElementAt_uchar(ypsIn, uIndex);
    uchar v= rsGetElementAt_uchar(ypsIn, uIndex+u_len);



uchar4 argb;
    argb.r = clamp(yps + v * 1436 / 1024 - 179,0,255);
   // argb.g = clamp(yps - u * 46549 / 131072 + 44 -v * 93604 / 131072 + 91,0,255);
    argb.g = clamp(yps - u * 46549 / 131072 -v * 93604 / 131072 + 135,0,255);
    argb.b = clamp(yps + u * 1814 / 1024 - 227,0,255);
    argb.a = 255;

//uchar4 out = convert_uchar4(argb);
return argb;
}

    //argb.r = clamp(yps + v * 1436 / 1024 - 179,0,255);
   // argb.g = clamp(yps - u * 46549 / 131072 + 44 -v * 93604 / 131072 + 91,0,255);
    //argb.b = clamp(yps + u * 1814 / 1024 - 227,0,255);
//    argb.a = 255;
//        int r = (int) (yps + (1.370705 * (v-128)));
//        int g = (int) (yps - (0.698001 * (v-128)) - (0.337633 * (u-128)));
//        int b = (int)(yps + (1.732446 * (u-128)));
//
//        r = r>255? 255 : r<0 ? 0 : r;
//        g = g>255? 255 : g<0 ? 0 : g;
//        b = b>255? 255 : b<0 ? 0 : b;
//
//
//        uchar4 res4;
//        res4.r = (uchar)r;
//        res4.g = (uchar)g;
//        res4.b = (uchar)b;
//        res4.a = 0xFF;
//
//        return res4;