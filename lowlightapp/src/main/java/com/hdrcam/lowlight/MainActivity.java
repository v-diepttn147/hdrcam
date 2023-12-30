package com.hdrcam.lowlight;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.hdrcam.camera.utils.Constants;
import com.hdrcam.utils.ImageUtils;
import com.hdrcam.utils.TouchImageView;

public class MainActivity extends AppCompatActivity
{
    public static final String TAG = "LowLightSDK";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_GALLERY = 2;

    private static final Integer[] INPUT_SIZE = {3000, 4000};

    private Context mContext;
    private Button saveBtn;
    private MenuItem resetAction, enhanceAction;
    private TouchImageView imageView;
    private TextView noteTV;

    private boolean isEnhanced = false;
    private Bitmap selectedImage, processedImage, enhancedImage;

    private LowLightSDK lowLightSDK = new LowLightSDK();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mContext = this;
        setUpComponents();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        resetAction = menu.findItem(R.id.action_reset);
        enhanceAction = menu.findItem(R.id.action_enhance);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_camera) {
            startActivityForResult(new Intent(MainActivity.this, CameraActivity.class),
                    REQUEST_IMAGE_CAPTURE);
            return true;
        } else if (id == R.id.action_gallery) {
            Intent i = new Intent(Intent.ACTION_PICK);
            i.setType("image/*");
            startActivityForResult(i, REQUEST_GALLERY);
            return true;
        } else if (id == R.id.action_reset) {
            swapImage();
        } else if (id == R.id.action_enhance) {
            if (enhancedImage != null)
                swapImage();
            else {
                Toast.makeText(mContext, "Enhancing...", Toast.LENGTH_SHORT).show();
                enhanceAction.setIcon(R.drawable.ic_baseline_highlight_24_disable);
                enhanceAction.setEnabled(false);
                new EnhanceTask().execute();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void toggleMenuItem(){
        if(isEnhanced) {
            enhanceAction.setIcon(R.drawable.ic_baseline_highlight_24_disable);
            enhanceAction.setEnabled(false);

            resetAction.setIcon(R.drawable.ic_baseline_replay_24);
            resetAction.setEnabled(true);
        }
        else {
            enhanceAction.setIcon(R.drawable.ic_baseline_highlight_24);
            enhanceAction.setEnabled(true);

            resetAction.setIcon(R.drawable.ic_baseline_replay_24_disable);
            resetAction.setEnabled(false);
        }
    }

    @Override
    public synchronized void onResume() {
        Log.i(TAG, "onResume " + this);
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inMutable = true;
            selectedImage = BitmapFactory.decodeByteArray(Constants.mainSavedImData.getData(), 0,
                    Constants.mainSavedImData.getData().length, options);
            imageView.resetZoom();
            setPreviewImage();
            toggleMenuItem();

            Toast.makeText(mContext, "Enhancing...", Toast.LENGTH_SHORT).show();
            enhanceAction.setIcon(R.drawable.ic_baseline_highlight_24_disable);
            enhanceAction.setEnabled(false);
            new EnhanceTask().execute();
        }
        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK) {
            try {
                selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), data.getData());
                Constants.burstTimestamp = ImageUtils.getFileName(mContext, data.getData());
                imageView.resetZoom();
                setPreviewImage();
                toggleMenuItem();
            }
            catch (Exception e) {
                Log.e(TAG, "Exception " + e);
                return;
            }
        }
    }

    private void setUpComponents() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        noteTV = findViewById(R.id.noteTV);

        imageView = findViewById(R.id.imageView);
        imageView.setClickable(true);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (enhancedImage == null) return;
                swapImage();
            }
        });

        selectedImage = ImageUtils.getBitmapFromAsset(this, "test_2.jpg");
        setPreviewImage();

        saveBtn = findViewById(R.id.saveBtn);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBtn.setEnabled(false);
                new SaveImage().execute();
            }
        });
        new InitSDKTask().execute();
    }

    class InitSDKTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            initSDK();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    enhanceAction.setIcon(R.drawable.ic_baseline_highlight_24);
                    enhanceAction.setEnabled(true);
                }
            });
            return null;
        }
    }

    class EnhanceTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            Log.d(TAG, "----------- Start enhance task ------------");
            boolean rotate = false;
            if (processedImage.getWidth() > processedImage.getHeight())
                rotate = true;
            byte[] input = ImageUtils.bitmapToRGBByteArrayInt(processedImage);
            Log.d(TAG, "input array length: " + input.length);
            byte[] out = lowLightSDK.enhance(input, processedImage.getWidth(), processedImage.getHeight());
            Log.d(TAG, "output array length: " + out.length);

            if (rotate)
                enhancedImage = ImageUtils.rgbByteArrayIntToBitmap(out, INPUT_SIZE[1] / 2,
                        INPUT_SIZE[0] / 2);
            else
                enhancedImage = ImageUtils.rgbByteArrayIntToBitmap(out, INPUT_SIZE[0] / 2,
                        INPUT_SIZE[1] / 2);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "Enhance: " + lowLightSDK.getElapsedMs() + "ms",
                            Toast.LENGTH_LONG).show();
                    swapImage();
                    noteTV.setText("Click the image to compare");
                }
            });
            return null;
        }
    }

    class SaveImage extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String filename = Constants.burstTimestamp;
            if (isEnhanced) {
                filename += "_enhanced";
                ImageUtils.saveJPEG(mContext, enhancedImage, filename);
            } else {
                ImageUtils.saveJPEG(mContext, processedImage, filename);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    saveBtn.setEnabled(true);
                    Toast.makeText(mContext, "Saved", Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }

    private void initSDK() {
        long start = System.currentTimeMillis();
        lowLightSDK.init(getAssets());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mContext, "Init SDK: " + (System.currentTimeMillis() - start) + "ms",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setPreviewImage() {
        int dstWidth = INPUT_SIZE[0];
        int dstHeight = INPUT_SIZE[1];
        if (selectedImage.getWidth() > selectedImage.getHeight()) {
            dstWidth = INPUT_SIZE[1];
            dstHeight = INPUT_SIZE[0];
        }
        processedImage = Bitmap.createScaledBitmap(selectedImage, dstWidth, dstHeight, true);
        imageView.setImageBitmap(processedImage);

        if (enhancedImage != null) enhancedImage = null;
        isEnhanced = false;
        noteTV.setText("Click the flash icon to enhance the image");
    }

    private void swapImage() {
        isEnhanced = !isEnhanced;
        if (enhancedImage == null) return;
        if (!isEnhanced) {
            noteTV.setText("Original");
            imageView.setImageBitmap(processedImage);
        }
        else {
            noteTV.setText("Enhanced");
            imageView.setImageBitmap(enhancedImage);
        }
        toggleMenuItem();
    }
}
