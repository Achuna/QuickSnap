package com.example.achuna.quicksnap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageReview extends AppCompatActivity {

    Bitmap finalImage;
    final int IMAGE_SAVE_REQUEST = 101;

    //Views
    ImageView image;
    Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_review);

        //Initialize views
        image = findViewById(R.id.finalImage);
        saveButton = findViewById(R.id.saveImageBtn);

        Bundle extras = getIntent().getExtras();

        if (extras != null) {
            //loads the temporary file
            File file = new File(extras.getString("picture"));
            //Decode file and create a bitmap
            Bitmap image = BitmapFactory.decodeFile(file.getAbsolutePath());
            //Rotates image to be upright
            finalImage = rotate(image);
        }
        image.setImageBitmap(finalImage);


        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Save Image to external storage
                if (ActivityCompat.checkSelfPermission(ImageReview.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(ImageReview.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, IMAGE_SAVE_REQUEST);
                } else {
                    if(isExternalStorageWritable()) {
                        int i = 0;
                        File imageFile;
                        String path = Environment.getExternalStorageDirectory().toString();
                        imageFile = new File(path, "textSnap"+".jpg");
                        while(imageFile.exists()) {
                            imageFile = new File(path, "textSnap" + i +".jpg");
                            i++;
                        }

                        try {
                            OutputStream stream = null;
                            stream = new FileOutputStream(imageFile);
                            finalImage.compress(Bitmap.CompressFormat.JPEG,100,stream);
                            stream.flush();
                            stream.close();
                            Toast.makeText(getApplicationContext(), "Image Saved", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getApplicationContext(), "Image Not Saved", Toast.LENGTH_SHORT).show();

                        }

                    }
                }
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == IMAGE_SAVE_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Save Image
                if(isExternalStorageWritable()) {
                    int i = 0;
                    File imageFile;
                    String path = Environment.getExternalStorageDirectory().toString();
                    imageFile = new File(path, "textSnap"+".jpg");
                    while(imageFile.exists()) {
                        imageFile = new File(path, "textSnap" + i +".jpg");
                        i++;
                    }
                    try {
                        OutputStream stream = null;
                        stream = new FileOutputStream(imageFile);
                        finalImage.compress(Bitmap.CompressFormat.JPEG,100,stream);
                        stream.flush();
                        stream.close();
                        Toast.makeText(getApplicationContext(), "Image Saved", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "Error Saving Image", Toast.LENGTH_SHORT).show();
                    }

                }
            } else {
                Toast.makeText(getApplicationContext(), "Storage Permission Missing", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap rotate(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Matrix matrix = new Matrix();
        matrix.setRotate(90);

        return Bitmap.createBitmap(image, 0, 0, width, height, matrix, true);
    }

    /** Checks if external storage is available for write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

}
