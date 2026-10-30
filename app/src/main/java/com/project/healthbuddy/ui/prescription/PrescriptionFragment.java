package com.project.healthbuddy.ui.prescription;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.project.healthbuddy.R;
import com.project.healthbuddy.ai.MedicineExtractionResult;
import com.project.healthbuddy.ai.PrescriptionAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PrescriptionFragment extends Fragment {

    private ProgressBar progressBar;
    private Uri cameraPhotoUri;

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Bitmap bitmap = null;
                    if (cameraPhotoUri != null) {
                        bitmap = decodeBitmapFromUri(cameraPhotoUri);
                    }
                    if (bitmap == null && result.getData() != null && result.getData().getExtras() != null) {
                        bitmap = (Bitmap) result.getData().getExtras().get("data");
                    }
                    if (bitmap != null) {
                        processImage(bitmap);
                    } else {
                        Toast.makeText(getContext(), "Could not retrieve camera photo. Please try choosing from Gallery.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null && getContext() != null) {
                        Bitmap bitmap = decodeBitmapFromUri(imageUri);
                        if (bitmap != null) {
                            processImage(bitmap);
                        } else {
                            Toast.makeText(getContext(), "Failed to load image from gallery", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_prescription, container, false);

        progressBar = root.findViewById(R.id.progress_analysis);

        root.findViewById(R.id.btn_take_photo).setOnClickListener(v -> launchCamera());
        
        root.findViewById(R.id.btn_choose_gallery).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        return root;
    }

    private void launchCamera() {
        if (getContext() == null) return;
        try {
            File photoFile = new File(getContext().getCacheDir(), "prescription_capture.jpg");
            cameraPhotoUri = FileProvider.getUriForFile(
                    getContext(),
                    "com.project.healthbuddy.fileprovider",
                    photoFile
            );
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPhotoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            cameraLauncher.launch(intent);
        } catch (Exception e) {
            Log.e("PrescriptionFragment", "Error launching camera", e);
            Toast.makeText(getContext(), "Error launching camera: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap decodeBitmapFromUri(Uri uri) {
        if (getContext() == null || uri == null) return null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContext().getContentResolver(), uri);
                return ImageDecoder.decodeBitmap(source, (decoder, info, s) -> decoder.setMutableRequired(true));
            } else {
                return MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
            }
        } catch (Exception e) {
            Log.e("PrescriptionFragment", "Error decoding bitmap from uri: " + uri, e);
            return null;
        }
    }

    private void processImage(Bitmap bitmap) {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        
        PrescriptionAnalyzer.analyze(bitmap, new PrescriptionAnalyzer.AnalysisCallback() {
            @Override
            public void onSuccess(List<MedicineExtractionResult> results) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Intent intent = new Intent(getContext(), ConfirmationActivity.class);
                intent.putParcelableArrayListExtra("results", new ArrayList<>(results));
                startActivity(intent);
            }

            @Override
            public void onFailure(String errorMessage) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }
}
