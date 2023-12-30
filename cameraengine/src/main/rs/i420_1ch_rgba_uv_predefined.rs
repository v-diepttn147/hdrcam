#pragma version(1)
  #pragma rs java_package_name(com.hdrcam.camera);
  #pragma  rs_fp_relaxed

  uint uvRowStride;
  uint y_len;
  uint u_len;

  rs_allocation ypsIn;
  rs_allocation uIndexIn;
  rs_allocation vIndexIn;

 // The LaunchOptions ensure that the Kernel does not enter the padding  zone of Y, so yRowStride can be ignored WITHIN the Kernel.
 uchar4 __attribute__((kernel)) doConvert(uint32_t x, uint32_t y) {

 // index for accessing the uIn's and vIn's
    uint32_t uIndex= rsGetElementAt_int(uIndexIn, x,y);  //y_len + x/2 + uvRowStride*(y/2);
    uint32_t vIndex= rsGetElementAt_int(vIndexIn, x,y);  //y_len + x/2 + uvRowStride*(y/2);

    // get the y,u,v values
    uchar yps = rsGetElementAt_uchar(ypsIn, x, y);
    uchar u= rsGetElementAt_uchar(ypsIn, uIndex);
    uchar v= rsGetElementAt_uchar(ypsIn, vIndex);



uchar4 argb;
    argb.r = clamp(yps + v * 1436 / 1024 - 179,0,255);
   // argb.g = clamp(yps - u * 46549 / 131072 + 44 -v * 93604 / 131072 + 91,0,255);
    argb.g = clamp(yps - u * 46549 / 131072 -v * 93604 / 131072 + 135,0,255);
    argb.b = clamp(yps + u * 1814 / 1024 - 227,0,255);
    argb.a = 255;

//uchar4 out = convert_uchar4(argb);
return argb;
}