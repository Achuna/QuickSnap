package com.example.achuna.quicksnap;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private final int CAMERA_REQEST_CODE = 100;
    private static final int IMAGE_REQUEST_CODE = 99;
    private Camera camera;
    private CameraPreview preview;
    private Camera.PictureCallback jpegCallback;
    private Camera.Parameters parameters;
    static int orientation = 0; //0 for portrait, 1 for landscape



    //Views
    FrameLayout cameraPreview;
    ImageButton shutterButton;
    Button rotate, importBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initialize Views
        cameraPreview = findViewById(R.id.camera_preview);
        shutterButton = findViewById(R.id.snapButton);
        rotate = findViewById(R.id.rotateButton);
        importBtn = findViewById(R.id.importBtn);


        //Get Camera Permission
        if(cameraExists(getApplicationContext())) {
            if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startCameraPreview();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA}, CAMERA_REQEST_CODE);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Camera Not Found", Toast.LENGTH_SHORT).show();
            closeApplication();
        }



        jpegCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                //Pass Picture data to result activity
                Bitmap image = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                String path = tempFileImage(getApplicationContext(), image, "photo");


                File picLocation = new File(path);

                Bitmap imageToAnalyze;
                String text;

                if(orientation == 0) {
                    imageToAnalyze = rotate(BitmapFactory.decodeFile(picLocation.getAbsolutePath()), 90);
                    text = extractText(imageToAnalyze);
                } else {
                    imageToAnalyze = rotate(BitmapFactory.decodeFile(picLocation.getAbsolutePath()), 0);
                    text = extractText(imageToAnalyze);
                }

                Intent viewResult = new Intent(getApplicationContext(), Result.class);
                viewResult.putExtra("picture path", path);
                viewResult.putExtra("text", text);
                viewResult.putExtra("isImport", false);
                startActivity(viewResult);
            }
        };


        shutterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                camera.takePicture(null, null, jpegCallback);

            }
        });

        importBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, IMAGE_REQUEST_CODE);
                } else {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, IMAGE_REQUEST_CODE);
                }
            }
        });

        rotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(orientation==1) {
                    rotate.setText("Portrait");
                    orientation = 0;
                } else {
                        rotate.setText("Landscape");
                        orientation = 1;
                }
            }
        });


    }

    //////////////ACTIVITY FUNCTIONALITY METHODS////////////////////

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCameraPreview();
                } else {
                    Toast.makeText(getApplicationContext(), "Camera Permission Missing", Toast.LENGTH_SHORT).show();
                    closeApplication();
                }
                break;
            }
            // other 'case' lines to check for other
            // permissions this app might request.

            case IMAGE_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(galleryIntent, IMAGE_REQUEST_CODE);
                } else {
                    Toast.makeText(getApplicationContext(), "Storage Permission Missing", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_REQUEST_CODE) {
                if (data != null) {
                    Uri contentURI = data.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentURI);
                        String text = extractText(bitmap);
                        Intent results = new Intent(getApplicationContext(), Result.class);
                        results.putExtra("isImported", true);
                        results.putExtra("text", text);
                        startActivity(results);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, "Failed to Load Image", Toast.LENGTH_SHORT).show();
                    }
                }
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //Get Camera Permission
        if(cameraExists(getApplicationContext())) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.CAMERA}, CAMERA_REQEST_CODE);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Camera Not Found", Toast.LENGTH_SHORT).show();
            closeApplication();
        }
    }

    @Override
    protected void onDestroy() {
        if(camera!=null){
            camera.stopPreview();
            camera.setPreviewCallback(null);

            camera.release();
            camera = null;
        }
        super.onDestroy();
    }

    ////////////METHODS//////////////


    /** Access Google's mobile vision api to extract text from bitmap */
    public String extractText(Bitmap image) {

        TextRecognizer textRecognizer = new TextRecognizer.Builder(getApplicationContext()).build();

        if (!textRecognizer.isOperational()) {
            Toast.makeText(getApplicationContext(), "Text not detected", Toast.LENGTH_SHORT).show();
            return "";
        } else {
            Frame frame = new Frame.Builder().setBitmap(image).build();
            SparseArray<TextBlock> items = textRecognizer.detect(frame);

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < items.size(); ++i) {
                TextBlock myItem = items.valueAt(i);
                builder.append(myItem.getValue() + " ");
                builder.append("\n");
            }

            textRecognizer.release();

            return builder.toString();

        }
    }

    /**creates a temporary file containing the bitmap return the absolute file path*/
    public static String tempFileImage(Context context, Bitmap bitmap, String name) {

        //places file in cache directory
        File outputDir = context.getCacheDir();
        File imageFile = new File(outputDir, name + ".jpg");

        OutputStream output;
        try {
            output = new FileOutputStream(imageFile);

            //IMPORTANT: Compress bitmap image or app will crash
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
            output.flush();
            output.close();
        } catch (Exception e) {
          //  "Error writing file
        }

        return imageFile.getAbsolutePath();
    }

    /** rotate image in order for mobile vision to read the text easily */
    private Bitmap rotate(Bitmap image, int rotate) {
        int width = image.getWidth();
        int height = image.getHeight();

        Matrix matrix = new Matrix();
        matrix.setRotate(rotate);

        return Bitmap.createBitmap(image, 0, 0, width, height, matrix, true);
    }

    /** Starts Camera preview */
    private void startCameraPreview() {
        //Start Camera Preview Here
        camera = getCameraInstance();
        camera.setDisplayOrientation(90);

        preview = new CameraPreview(MainActivity.this, camera);

        parameters = camera.getParameters();

        camera.setDisplayOrientation(90);
        parameters.setPreviewFrameRate(30);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

            //Start Camera Auto Focus
        Camera.Parameters params = camera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);

        cameraPreview.addView(preview);

    }

//    private void focusCamera(int focusType) {
//        if(focusType == 0) {
//            camera.autoFocus(new Camera.AutoFocusCallback() {
//                @Override
//                public void onAutoFocus(boolean success, Camera camera) {
//                    camera.cancelAutoFocus();
//                    Camera.Parameters params = camera.getParameters();
//                    if (params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
//                        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//                        camera.setParameters(params);
//                    }
//                }
//            });
//        } else {
//            //Start Camera Auto Focus
//        Camera.Parameters params = camera.getParameters();
//        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//        camera.setParameters(params);
//        }
//
//
//    }

    /** Check if this device has a camera */
    private boolean cameraExists(Context context) {
        return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA);
        //Returns true if the device has a camera; false otherwise
    }

    /** A safe way to get an instance of the Camera object. */
    private static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }

    private void closeApplication() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory( Intent.CATEGORY_HOME );
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
    }
}




    /*
    Project Goals:
    Make a real time camera with app with two features:
    --- Shutter Button
    --- Tap screen to focus on a subject
    --- Swipe to change modes

    Make a save feature for the picture
    ---> Google photos integration
    ---> Learn how to have to locale storage
    ---> This can be done on a separate screen with the preview of the photo
    before saving to storage.
    ---> OCR (Optical Character Recognition)


    Extra Feature:
    ---> Real time photo recognition. Display what the object is using an external library

    Next Steps:
    Create a virtual reality application using sample environments.
     */