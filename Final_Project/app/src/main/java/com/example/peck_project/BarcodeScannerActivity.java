package com.example.peck_project;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BarcodeScannerActivity extends AppCompatActivity {

    private static final int PERMISSION_CAMERA_REQUEST = 101;
    private PreviewView cameraPreview;
    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;
    private boolean isScanComplete = false; // Prevents duplicate scans from firing multiple times

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        cameraPreview = findViewById(R.id.camera_preview);
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize the ML Kit Barcode Scanner
        scanner = BarcodeScanning.getClient();

        // Check for runtime camera hardware permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCameraSource();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_CAMERA_REQUEST);
        }
    }

    private void startCameraSource() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. Setup Camera Live Viewfinder Preview
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                // 2. Setup Image Analyzer to catch live background frames
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, imageProxy -> analyzeFrameImage(imageProxy));

                // Select the back camera lens by default
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Bind camera tightly to the Activity Lifecycle
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("ScannerActivity", "Camera initialization failed", e);
                Toast.makeText(this, "Failed to start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void analyzeFrameImage(ImageProxy imageProxy) {
        if (isScanComplete || imageProxy.getImage() == null) {
            imageProxy.close();
            return;
        }

        // Wrap the raw frame bytes into an ML Kit readable object structure
        InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        // Process the payload via the ML Kit barcode scanning pipelines
        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode barcode : barcodes) {
                        String rawBarcodeValue = barcode.getRawValue();
                        if (rawBarcodeValue != null && !rawBarcodeValue.isEmpty()) {
                            isScanComplete = true; // Lock the code loop immediately

                            // Package up the scan result envelope
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("SCANNED_BARCODE", rawBarcodeValue);
                            setResult(RESULT_OK, resultIntent);

                            // Close camera screen and fall back to AddItemActivity cleanly
                            finish();
                            break;
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ScannerActivity", "ML Kit parsing failed", e))
                .addOnCompleteListener(task -> imageProxy.close()); // Crucial: Always close proxy to receive next frame
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_CAMERA_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCameraSource();
            } else {
                Toast.makeText(this, "Camera permission is required to scan barcodes.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        scanner.close();
    }
}