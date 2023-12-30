package com.hdrcam.camera.render;

import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.hdrcam.camera.capture.VideoCameraPreview;
import com.hdrcam.camera.utils.Constants;
import com.hdrcam.camera.utils.ExImageFormat;
import com.hdrcam.camera.utils.ImageData;
import com.hdrcam.camera.utils.ImgUtils;
import com.hdrcam.camera.utils.gles.GlUtil;
import com.hdrcam.camera.utils.gles.program.ProgramTexture2d;
import com.hdrcam.camera.utils.gles.program.ProgramTextureOES;
import com.hdrcam.camera.utils.gles.program.TextureNVProgram;
import com.hdrcam.camera.utils.gles.program.TextureProgram;
import com.hdrcam.camera.utils.gles.program.TextureYUV420Program;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class PreviewRenderer implements GLSurfaceView.Renderer {
    private Boolean isDebug = false;
    public static int brightnessVal = 0;
    public static int contrastVal = 0;
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "attribute vec2 aTexCoord;" +
                    "varying vec2 vTexCoord;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "  vTexCoord = aTexCoord;" +
                    "}";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform sampler2D uTextureUnit;" +
                    "varying vec2 vTexCoord;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(uTextureUnit, vTexCoord);" +
                    "}";
    private static final float[] VERTEX = {
            1, 1,  // top right
            -1, 1, // top left
            1, -1, // bottom right
            -1, -1,// bottom left
    };
    private static final float[] TEXTURE = {
            1, 0,  // top right
            0, 0,  // top left
            1, 1,  // bottom right
            0, 1,  // bottom left
    };

    private float[] mMvpMatrix = new float[16];
    private int mProgram;


    private int mPreviewWidth;
    private int mPreviewHeight;
    public ByteBuffer m_yBuffer;
    public ByteBuffer m_uBuffer;
    public ByteBuffer m_vBuffer;


    private static final float[] TEXTURE_MATRIX = {0.0f, -1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f};
    private static final float[] SURFACE_TEXTURE_MATRIX = {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f};

    protected float[] mTexMatrix = Arrays.copyOf(TEXTURE_MATRIX, TEXTURE_MATRIX.length);

    protected SurfaceTexture mSurfaceTexture;
    protected int mCameraTexId;
    private ProgramTexture2d mProgramTexture2d;
    private ProgramTextureOES mProgramTextureOES;
    protected GLSurfaceView mGlSurfaceView;
    private TextureProgram mTextureProgram;
    private TextureNVProgram mTextureNVProgram21;
    private TextureNVProgram mTextureNVProgram12;

    private boolean mIsFrontCam;


    private Bitmap renderedBitmap;

    private ImageData mImageData;

    public synchronized ImageData getmImageData() {
        return mImageData;
    }


    public synchronized void setPreviewData(ImageData mImageData) {
        this.mImageData = mImageData;
    }


    public synchronized Bitmap getRenderedBitmap() {
        return renderedBitmap;
    }

    public synchronized void setRenderedBitmap(Bitmap renderedBitmap) {
        this.renderedBitmap = renderedBitmap;
    }

    public synchronized Bitmap getProcessedBitmap() {
        return processedBitmap;
    }

    public synchronized void setProcessedBitmap(Bitmap processedBitmap) {
        this.processedBitmap = processedBitmap;
    }

    private Bitmap processedBitmap;
    private VideoCameraPreview mMainCamPreview;
    private VideoCameraPreview mSubCamPreview;
    private int mPreviewFormat;

    private boolean mScreenChanged = true;
/*    public PreviewRenderer(int previewWidth, int previewHeight, Boolean isFrontCam) {

        mVertexBuffer = GLESUtils.createFloatBuffer(VERTEX);
        mTextureBuffer = GLESUtils.createFloatBuffer(TEXTURE);
        mTextureId = new int[1];

        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;

        mIsFrontCam = isFrontCam;

        m_yBuffer = ByteBuffer.allocateDirect(previewWidth * previewHeight);
        m_uBuffer = ByteBuffer.allocateDirect(previewWidth * previewHeight / 4);
        m_vBuffer = ByteBuffer.allocateDirect(previewWidth * previewHeight / 4);

    }*/

    public PreviewRenderer(VideoCameraPreview mainCamPreview, VideoCameraPreview subCamPreview,
                           int previewFormat) {
        if (mainCamPreview != null) {
            mPreviewWidth = mainCamPreview.getPreviewSize().getWidth();
            mPreviewHeight = mainCamPreview.getPreviewSize().getHeight();
            mIsFrontCam = mainCamPreview.getCameraFacing();
        } else {
            mPreviewWidth = 4032;
            mPreviewHeight = 3000;
            mIsFrontCam = false;
        }
        mPreviewFormat = previewFormat;

        m_yBuffer = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight);
        m_uBuffer = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight / 4);
        m_vBuffer = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight / 4);

        mMainCamPreview = mainCamPreview;
        mSubCamPreview = subCamPreview;
    }

    public PreviewRenderer(int previewWidth, int previewHeight,
                           int previewFormat) {

        mPreviewWidth = previewWidth;
        mPreviewHeight = previewHeight;
        mIsFrontCam = false;

        mPreviewFormat = previewFormat;

        m_yBuffer = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight);
        m_uBuffer = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight / 4);
        m_vBuffer = ByteBuffer.allocateDirect(mPreviewWidth * mPreviewHeight / 4);

        mMainCamPreview = null;
        mSubCamPreview = null;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        Matrix.setIdentityM(mTexMatrix, 0);
        mTexMatrix[5] = -1;
        mTexMatrix[0] = -1;
        int vertexShader = GLESUtils.createVertexShader(VERTEX_SHADER);
        int fragmentShader = GLESUtils.createFragmentShader(FRAGMENT_SHADER);
        mProgram = GLESUtils.createProgram(vertexShader, fragmentShader);


        mProgramTexture2d = new ProgramTexture2d();
        mProgramTextureOES = new ProgramTextureOES();
        mCameraTexId = GlUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mSurfaceTexture = new SurfaceTexture(mCameraTexId);
        mSurfaceTexture.setDefaultBufferSize(mPreviewWidth, mPreviewHeight);

        mTextureProgram = TextureProgram.createTexture2D();
        mTextureNVProgram21 = TextureNVProgram.createTexture2D(ExImageFormat.NV21);
        mTextureNVProgram12 = TextureNVProgram.createTexture2D(ExImageFormat.NV12);

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mScreenChanged = true;
        Log.d("RENDER", "Front cam is " + mIsFrontCam);
//        if (mMainCamPreview != null) mMainCamPreview.openCamera(width, height);
//        if (mSubCamPreview != null) mSubCamPreview.openCamera(width, height);

        int viewPortWidth;
        viewPortWidth = width;
        int viewPortHeight = (int) ((float) mPreviewWidth / mPreviewHeight * viewPortWidth);

        int x0 = 0;
        int y0 = (height - viewPortHeight) / 4 * 3;

        GLES20.glViewport(x0, y0, viewPortWidth, viewPortHeight);
        Matrix.setIdentityM(mMvpMatrix, 0);
        Matrix.rotateM(mMvpMatrix, 0, -90, 0.0f, 0.0f, 1.0f);

        mIsFrontCam = mMainCamPreview.getCameraFacing();

        if (!mIsFrontCam)
            Matrix.rotateM(mMvpMatrix, 0, 180, 0.0f, 1.0f, 0.0f);


    }

/*    private void drawYUV420888NoPadding(byte[] data, int width, int height) {
        if (yuvdata == null ) return;
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        m_yBuffer.clear();
        m_uBuffer.clear();
        m_vBuffer.clear();

        m_yBuffer.put(data, 0, m_yBuffer.capacity());
        m_uBuffer.put(data, width * height, m_uBuffer.capacity());
        m_vBuffer.put(data, width * height * 5 / 4, m_vBuffer.capacity());

        int[] texture_yuv;
        texture_yuv = GLESUtils.loadTextureYUV420(m_yBuffer, m_uBuffer, m_vBuffer, width,
                height);
        Log.d("draw", "draw");
        mTextureYUV420Program.drawFrame(texture_yuv, mTexMatrix, mMvpMatrix);
        setImageData(null);
    }*/

    private void drawImageData(ImageData imageData) {
        if (imageData == null) return;
        int width = imageData.getWidth();
        int height = imageData.getHeight();
        byte[] data = imageData.getData();




        int[] texture_yuv = null;

        switch (imageData.getFormat()) {
            case ExImageFormat.NV12:
            case ExImageFormat.NV21:
                ByteBuffer yBuffer = ByteBuffer.allocateDirect(width * height);
                ByteBuffer uvBuffer = ByteBuffer.allocateDirect(width * height / 2);

                yBuffer.put(data, 0, yBuffer.capacity());
                uvBuffer.put(data, width * height, uvBuffer.capacity());
                texture_yuv = GLESUtils.loadTextureNV(yBuffer, uvBuffer, width, height);
                Log.d("draw", "draw");
                if (imageData.getFormat() == ExImageFormat.NV21) {
                    mTextureNVProgram21.drawFrame(texture_yuv, mTexMatrix, mMvpMatrix,
                            (float) (brightnessVal) / 256, (float) (contrastVal) / 256);
                }
                else if (imageData.getFormat() == ExImageFormat.NV12) {
                    mTextureNVProgram12.drawFrame(texture_yuv, mTexMatrix, mMvpMatrix,
                        (float)(brightnessVal)/256, (float)(contrastVal)/256);
                }
                break;
            case ExImageFormat.RGBA:
                texture_yuv = new int[1];
                ByteBuffer rgbaBuffer = ByteBuffer.allocateDirect(width * height * 4);
                rgbaBuffer.put(data, 0, rgbaBuffer.capacity());
                texture_yuv[0] = GLESUtils.loadTexture(rgbaBuffer, width, height);

                mTextureProgram.drawFrame(texture_yuv[0], mTexMatrix, mMvpMatrix);
                break;
            default:
                break;
        }
        setPreviewData(null);

        if (texture_yuv != null) {
            GLES20.glDeleteTextures(texture_yuv.length, texture_yuv, 0);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClearColor(0f, 0f, 0f, 1f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        Log.d("RENDER", "mScreenChanged " + mScreenChanged);

        if (mScreenChanged) {
            mImageData = null;
            mScreenChanged = false;
          //  return;
        }
        drawImageData(mImageData);
        Constants.mainPreviewImData = null;
        Constants.subPreviewImData = null;
    }


    public Bitmap glConvertYUV420ToBitmap(byte[] data, int width, int height,
                                          TextureYUV420Program textureYUV420Program) {


        long start1 = System.currentTimeMillis();


        long finish1 = System.currentTimeMillis();
        long timeElapsed1 = finish1 - start1;
        Log.i("render", String.format("yuv420-parse() *%d", timeElapsed1));

        long start2 = System.currentTimeMillis();


        m_yBuffer.clear();
        m_uBuffer.clear();
        m_vBuffer.clear();

        m_yBuffer.put(data, 0, m_yBuffer.capacity());
        m_uBuffer.put(data, width * height, m_uBuffer.capacity());
        m_vBuffer.put(data, width * height * 5 / 4, m_vBuffer.capacity());

        long finish2 = System.currentTimeMillis();
        long timeElapsed2 = finish2 - start2;
        Log.i("render", String.format("yuv420-tobuffer() *%d", timeElapsed2));

        int[] texture_yuv;


        texture_yuv = GLESUtils.loadTextureYUV420(m_yBuffer, m_uBuffer, m_vBuffer, width,
                height);


        long start3 = System.currentTimeMillis();
        float[] texMat = new float[16];
        android.opengl.Matrix.setIdentityM(texMat, 0);

        //android.opengl.Matrix.rotateM(texMat, 0, 90, 0.0f, 0.0f, 1.0f);
        //android.opengl.Matrix.rotateM(texMat, 0, 180, 0.0f, 1.0f, 0.0f);

        float[] mvp = new float[16];
        android.opengl.Matrix.setIdentityM(mvp, 0);
        Bitmap bmp = GLESUtils.glReadCameraBitmap(texture_yuv, texMat, mvp, width, height,
                textureYUV420Program);

        long finish3 = System.currentTimeMillis();
        long timeElapsed3 = finish3 - start3;
        Log.i("render", String.format("glReadbitmap() *%d", timeElapsed3));

        GLES20.glDeleteTextures(3, texture_yuv, 0);

        ImgUtils.bmpToFileDebug(false, bmp, "out2.png");
        return bmp;

    }
}
