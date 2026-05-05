package com.alfastore.checkimagefromcamera;

import static com.alfastore.checkimagefromcamera.ImageAnalyzer.getBlurScore;
import static com.alfastore.checkimagefromcamera.ImageAnalyzer.isBlurry;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.PictureResult;
import com.otaliastudios.cameraview.size.SizeSelectors;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ImageView ivPhoto, previewImage;
    private ImageButton ibCapture;
    private Button btnUbah, btnSelesai, captureButton, ulangButton;
    private LinearLayout container, llWatermark;

    private String currentPhotoPath;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private Bitmap photo;
    boolean cekPhotoSingleClick = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        String[] permission = new String[0];
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permission = new String[]{Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.CAMERA};
        } else {
            permission = new String[]{Manifest.permission.CAMERA};
        }
        permissionLauncherMultiple.launch(permission);

//        ivPhoto = findViewById(R.id.ivPhoto);
//        btnSelesai = findViewById(R.id.btnSelesai);
//
//        ibCapture = findViewById(R.id.ibCapture);
//        ibCapture.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                openCamera();
//            }
//        });
//
//        btnUbah = findViewById(R.id.btnUbah);
//        btnUbah.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                openCamera();
//            }
//        });

        // Check OpenCV
        if (OpenCVLoader.initDebug()){
            Log.d("TAG", "Success OpenCV");
        }else {
            Log.d("TAG", "Err OpenCV");
        }

        /*
        previewImage = findViewById(R.id.imagePreview);
        captureButton = findViewById(R.id.captureButton);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        CameraView camera = findViewById(R.id.camera);
        camera.setPictureSize(SizeSelectors.and(
                SizeSelectors.minWidth(4000),
                SizeSelectors.minHeight(3000)
        ));
        camera.setLifecycleOwner(this);

        camera.addCameraListener(new CameraListener() {
            @Override
            public void onPictureTaken(@NonNull PictureResult result) {
                Log.d("TAG", "onPictureTaken: ");

                // Access the byte[] or file
                byte[] data = result.getData();
                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

                // Show in ImageView
                llWatermark = findViewById(R.id.llWatermark);

                BitmapDrawable backgroundDrawable = new BitmapDrawable(getResources(), bitmap);
                previewImage.setBackground(backgroundDrawable);

//                previewImage.setImageBitmap(bitmap);
                previewImage.setVisibility(View.VISIBLE);

                // Hide the camera view if you want
                camera.close();
                camera.setVisibility(View.GONE);
                llWatermark.setVisibility(View.GONE);
                captureButton.setVisibility(View.GONE);
                ulangButton.setVisibility(View.VISIBLE);

                // Or save to file
//                result.toFile(new File(getFilesDir(), "picture.jpg"), file -> {
//                    // File saved!
//                });
            }
        });

        captureButton.setOnClickListener(v -> camera.takePicture());

        ulangButton = findViewById(R.id.ulangButton);
        ulangButton.setOnClickListener(view -> {
            previewImage.setVisibility(View.GONE);
            ulangButton.setVisibility(View.GONE);

            camera.open();
            camera.setVisibility(View.VISIBLE);
            captureButton.setVisibility(View.VISIBLE);
            llWatermark.setVisibility(View.VISIBLE);
        });
        */

        container = findViewById(R.id.container_layout);

        // Loop to create 5 flexible counters
        int counter = 5;
        for (int i = 1; i <= counter; i++) {
            createCounter(i, counter);
        }
    }

    private ActivityResultLauncher<String[]> permissionLauncherMultiple = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    boolean allGranted = true;
                    for (Boolean isGranted : result.values()){
                        Log.d("TAG", "onActivityResult: isGranted: "+isGranted);
                        allGranted = allGranted && isGranted;
                    }

                    if (allGranted){
                        Log.d("TAG", "onActivityResult: All permission Granted...");
                    }else {
                        Log.d("TAG", "onActivityResult: All or some permission denied...");
                        Toast.makeText(getApplicationContext(), "All or some permission denied...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void openCamera(){
        String fileName = "photo";
        File storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        try {
            File imageFile = File.createTempFile(fileName, ".jpg", storageDirectory);

            currentPhotoPath = imageFile.getAbsolutePath();
            Uri imageUri = FileProvider.getUriForFile(MainActivity.this,
                    "com.alfastore.checkimagefromcamera.fileProvider", imageFile);

            // Membuat Bingkai Camera menjadi 1:1 dengan resolusi 640x640
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePictureIntent.putExtra("aspectX",1);
            takePictureIntent.putExtra("aspectY",1);
            takePictureIntent.putExtra("outputX",640);
            takePictureIntent.putExtra("outputY",640);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            photo = BitmapFactory.decodeFile(currentPhotoPath);
            // Set resolution to 640x640
            if (photo != null){
                try {
                    photo = ResizeBitmap(photo, 640);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                cekPhotoSingleClick = true;
            }else {
                cekPhotoSingleClick = false;
            }

            // Cara 1
//            BitmapDrawable bitmapDrawable = new BitmapDrawable(getResources(), photo);
//            ivPhoto[a].setBackground(bitmapDrawable);

            // Cara 2 - tambahkan scaleType di Image XML
            ivPhoto.setImageBitmap(photo);

            ImageAnalyzer analyzer = new ImageAnalyzer();

            // Assuming you have a Bitmap object
            analyzer.checkIfImageIsBlack(photo, new ImageAnalyzer.AnalysisCallback() {
                @Override
                public void onResult(boolean isBlack) {
                    double scoreBlur = getBlurScore(photo);
                    Log.d("TAG", "scoreBlur: " + String.valueOf(scoreBlur));

                    if (isBlack) {
                        // Handle black image (e.g., show a warning or discard)
                        Toast.makeText(MainActivity.this, "Image is blank!", Toast.LENGTH_SHORT).show();
                    } else if (isBlurry(photo)) {
                        Toast.makeText(MainActivity.this, "Image is blurry!", Toast.LENGTH_SHORT).show();
                    } else {
                        // Image is fine
                        Toast.makeText(MainActivity.this, "Image is fine!", Toast.LENGTH_SHORT).show();

                        btnSelesai.setVisibility(View.VISIBLE);
                    }
                }
            });

            if (cekPhotoSingleClick){
                ibCapture.setVisibility(View.INVISIBLE);
                btnUbah.setVisibility(View.VISIBLE);
            }
        }
    }

    private Bitmap ResizeBitmap(Bitmap bitmap, int newWidth) throws IOException {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float temp = ((float) height) / ((float) width);
        int newHeight = (int) ((newWidth) * temp);
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);

        ExifInterface ei = new ExifInterface(currentPhotoPath);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED);

        switch(orientation) {

            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;

            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;

            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;

            case ExifInterface.ORIENTATION_NORMAL:
            default:
                matrix.postRotate(0);
        }

        Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        bitmap.recycle();
        return resizedBitmap;
    }

    private void createCounter(int index, int counter) {
        // Create a sub-layout
        RelativeLayout layout = new RelativeLayout(this);

        // Add Image
        ImageView imageView = new ImageView(this);
        imageView.setBackgroundColor(Color.BLUE);

        RelativeLayout.LayoutParams paramsImageView = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        imageView.setLayoutParams(paramsImageView);

        layout.addView(imageView);

        // Create the increment button
        Button btn = new Button(this);
        btn.setText("+");

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
        );
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        btn.setLayoutParams(params);

        layout.addView(btn);

        // Add Action to Button
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageView.setBackgroundColor(Color.RED);
                Toast.makeText(MainActivity.this, "Button Clicked!", Toast.LENGTH_SHORT).show();
                btn.setText("Ubah");
            }
        });

        // Add Margin to container
        LinearLayout.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 300);
        if (index==counter){
            linearLayoutParams.setMargins(15, 10, 15, 10);
        }else {
            linearLayoutParams.setMargins(15, 10, 15, 0);
        }

        container.addView(layout, linearLayoutParams);
    }
}