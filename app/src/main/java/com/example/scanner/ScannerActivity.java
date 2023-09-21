package com.example.scanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class ScannerActivity extends AppCompatActivity {
    private PreviewView preview;
    private static TextView barcodeView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private boolean camera_permission = false;

    private static ProcessCameraProvider cameraProvider;
    private static ImageAnalysis imageAnalyzer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.wtf("Create", "create");

        setContentView(R.layout.activity_scanner);

        preview = findViewById(R.id.camera_preview);
        barcodeView = findViewById(R.id.barcode_view);
        Button button = findViewById(R.id.button_scan);
        button.setOnClickListener(this::cameraStart);


        // Установка флага разрешений "Интернет"
        if (this.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            camera_permission = true;
        }
        else {
            // Если разрещение не получено, отправка запроса
            String[] permissions = new String[]{Manifest.permission.CAMERA};
            this.requestPermissions(permissions, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            camera_permission = true;
        }
    }

    private static class ImageAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        @OptIn(markerClass = ExperimentalGetImage.class)
        public void analyze(ImageProxy imageProxy) {

            Log.wtf("Analyze", "Run");

            Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build();

                BarcodeScanner scanner = BarcodeScanning.getClient(options);

                scanner.process(image).addOnCompleteListener(task -> {
                    if (task.isSuccessful()){
                        List<Barcode> barcodes = task.getResult();

                        if (!barcodes.isEmpty()) {
                            cameraProvider.unbindAll();
                        }

                        for (Barcode barcode: barcodes) {
                            barcodeView.append(barcode.getRawValue());
                            Log.wtf("Barcode", barcode.getRawValue());
                        }
                    }

                    imageProxy.close();
                });
            }
        }
    }

    private void cameraStart(View view) {
        if (!camera_permission){
            return;
        }

        barcodeView.setText("");

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(
                () -> {
                    try {
                        cameraProvider = cameraProviderFuture.get();

                        Preview previewCase = new Preview.Builder().build();
                        previewCase.setSurfaceProvider(preview.getSurfaceProvider());

                        imageAnalyzer = new ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build();

                        imageAnalyzer.setAnalyzer(ContextCompat.getMainExecutor(this),new ImageAnalyzer());

                        CameraSelector cameraSelector = new CameraSelector.Builder()
                                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                                .build();

                        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalyzer, previewCase);
                    }
                    catch (ExecutionException | InterruptedException e) {
                        // No errors need to be handled for this Future.
                        // This should never be reached.
                    }
                },
                ContextCompat.getMainExecutor(this)
        );
    }
}
