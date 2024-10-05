package com.example.picsta

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import kotlin.math.abs


class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var captureImage: ImageButton
    private lateinit var flipCamera: ImageButton

    var cameraFacing: Int = CameraSelector.LENS_FACING_BACK
    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result: Boolean ->
        if (result) {
            startCamera(cameraFacing)
        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        previewView = findViewById(R.id.pvCameraPreview)
        captureImage = findViewById(R.id.iBtnCaptureImage)
        flipCamera = findViewById(R.id.iBtnFlipCamera)

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            activityResultLauncher.launch(android.Manifest.permission.CAMERA)
        } else {
            startCamera(cameraFacing)
        }

        flipCamera.setOnClickListener {
            if (cameraFacing == CameraSelector.LENS_FACING_BACK) {
                cameraFacing = CameraSelector.LENS_FACING_FRONT;
            } else {
                cameraFacing = CameraSelector.LENS_FACING_BACK;
            }
            startCamera(cameraFacing);
        }
    }

    fun startCamera(cameraFacing: Int) {
        val aspectRatio = aspectRatio(previewView.width, previewView.height)
        val listenableFuture = ProcessCameraProvider.getInstance(this)

        listenableFuture.addListener({
            try {
                val cameraProvider = listenableFuture.get()

                val preview = Preview.Builder().setTargetAspectRatio(aspectRatio).build()

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraFacing).build()

                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

                captureImage.setOnClickListener {
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        activityResultLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    takePicture(imageCapture)
                }


                preview.setSurfaceProvider(previewView.surfaceProvider)
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = maxOf(width, height).toDouble() / minOf(width, height)
        return if (abs(previewRatio - 4.0 / 3.0) <= abs(previewRatio - 16.0 / 9.0)) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }

    private fun takePicture(imageCapture: ImageCapture) {
        val file = File(getExternalFilesDir(null), "${System.currentTimeMillis()}.jpg")
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(outputFileOptions, Executors.newCachedThreadPool(), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Image saved at: ${file.path}", Toast.LENGTH_SHORT).show()
                }
                startCamera(cameraFacing)
            }

            override fun onError(exception: ImageCaptureException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Failed to save: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
                startCamera(cameraFacing)
            }
        })
    }



}


