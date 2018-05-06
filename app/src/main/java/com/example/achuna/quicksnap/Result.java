package com.example.achuna.quicksnap;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;

public class Result extends AppCompatActivity {

    final String TAG = "Result";
    final int IMAGE_READ = 10;
    String filePath;
    int orientation;
    Bitmap image;

    //Views
    ImageButton shareButton;
    Button viewImageButton;
    Button webserach;
    Button retry;
    EditText textResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        //Initialize Views
        shareButton = findViewById(R.id.shareButton);
        viewImageButton = findViewById(R.id.viewImageBtn);
        textResult = findViewById(R.id.resultText);
        webserach = findViewById(R.id.webSearch);
        retry = findViewById(R.id.retryBtn);

        retry.setVisibility(View.VISIBLE);
        viewImageButton.setVisibility(View.VISIBLE);

        //Retrieve picture file path
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            if (extras.getBoolean("isImported")) {
                retry.setVisibility(View.INVISIBLE);
                viewImageButton.setVisibility(View.INVISIBLE);
                if(extras.getString("text").length() == 0) {
                    textResult.setText("Text Not Found");
                } else {
                    textResult.setText(extras.getString("text"));
                }
            } else {
                filePath = extras.getString("picture path");
                image = BitmapFactory.decodeFile(filePath);
                if(extras.getString("text").length() == 0) {
                    textResult.setText("Text Not Found");
                } else {
                    textResult.setText(extras.getString("text"));
                }
            }


        }

        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap finalImage;
                if(MainActivity.orientation == 0) {
                    finalImage = rotate(image, 90);
                    textResult.setText(extractText(finalImage));
                } else {
                    finalImage = rotate(image, 0);
                    textResult.setText(extractText(finalImage));
                }
            }
        });

        viewImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewImage = new Intent(getApplicationContext(), ImageReview.class);
                viewImage.putExtra("picture", filePath);
                startActivity(viewImage);
            }
        });

        webserach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(textResult.getText().toString().contains("https")) {
                    Intent openUrl = new Intent(Intent.ACTION_VIEW, Uri.parse(textResult.getText().toString()));
                    try {
                        startActivity(openUrl);
                    } catch (Exception e) {
                        //Used to make a search query
                        Uri uri = Uri.parse("http://www.google.com/#q=" + textResult.getText().toString());
                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        try {
                            startActivity(intent);
                        } catch (Exception j) {
                            Toast.makeText(getApplicationContext(), "Unable to Search", Toast.LENGTH_SHORT).show();
                        }
                        Toast.makeText(getApplicationContext(), "Cannot Open URL", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Uri uri = Uri.parse("http://www.google.com/#q=" + textResult.getText().toString());
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Unable to Search", Toast.LENGTH_SHORT).show();
                    }

                }

            }
        });

        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //This will allow the user to select options to share their text
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, textResult.getText().toString());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, "Share"));
            }
        });

    }

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

    /** rotate image in order for mobile vision to read the text easily */
    private Bitmap rotate(Bitmap image, int rotate) {
        int width = image.getWidth();
        int height = image.getHeight();

        Matrix matrix = new Matrix();
        matrix.setRotate(rotate);

        return Bitmap.createBitmap(image, 0, 0, width, height, matrix, true);
    }
}
