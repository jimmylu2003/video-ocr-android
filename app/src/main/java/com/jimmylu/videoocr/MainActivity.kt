package com.jimmylu.videoocr

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.recognizedText
import com.jimmylu.videoocr.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    
    private val cameraExecutor: ExecutorService by lazy { Executors.newSingleThreadExecutor() }
    
    private val textRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.builder().build())
    
    private var isProcessing = false
    private var processJob: Job? = null
    
    private val requiredPermissions = arrayOf(Manifest.permission.CAMERA)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startCamera()
        } else {
            Toast.makeText(this, "需要摄像头权限才能使用", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        binding.btnStartCamera.setOnClickListener {
            if (hasCameraPermission()) {
                startCamera()
            } else {
                checkPermissions()
            }
        }
        
        binding.btnStop.setOnClickListener {
            stopCamera()
        }
        
        binding.btnPickImage.setOnClickListener {
            pickImageFromGallery()
        }
        
        binding.btnCopy.setOnClickListener {
            copyToClipboard()
        }
        
        binding.btnSave.setOnClickListener {
            saveResults()
        }
    }

    private fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
                PackageManager.PERMISSION_GRANTED -> {
                startCamera()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(this, "需要摄像头权限", Toast.LENGTH_LONG).show()
                requestPermissionLauncher.launch(requiredPermissions)
            }
            else -> {
                requestPermissionLauncher.launch(requiredPermissions)
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == 
            PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                Toast.makeText(this, "摄像头启动失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return
        
        preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }
        
        imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImageProxy(imageProxy)
                }
            }
        
        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                this,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
            binding.tvStatus.text = "🟢 摄像头运行中"
            binding.btnStartCamera.isEnabled = false
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
            Toast.makeText(this, "摄像头绑定失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopCamera() {
        cameraProvider?.unbindAll()
        camera = null
        binding.tvStatus.text = "⏹️ 已停止"
        binding.btnStartCamera.isEnabled = true
        processJob?.cancel()
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        if (isProcessing) {
            imageProxy.close()
            return
        }
        
        isProcessing = true
        
        val bitmap = imageProxy.toBitmap()
        val image = InputImage.fromBitmap(bitmap, 0)
        
        processJob = lifecycleScope.launch {
            try {
                val visionText = withContext(Dispatchers.Default) {
                    textRecognizer.process(image).await()
                }
                
                withContext(Dispatchers.Main) {
                    displayResults(visionText.text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "OCR failed", e)
            } finally {
                isProcessing = false
                imageProxy.close()
            }
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val buffer = planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(bytes))
        }
    }

    private fun displayResults(text: String) {
        if (text.isNotBlank()) {
            binding.tvResults.text = text
            binding.tvResults.visibility = View.VISIBLE
        }
    }

    private fun pickImageFromGallery() {
        val launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let { processImageUri(it) }
        }
        launcher.launch("image/*")
    }

    private fun processImageUri(uri: Uri) {
        lifecycleScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { input ->
                        android.graphics.BitmapFactory.decodeStream(input)
                    }
                }
                
                bitmap?.let { bmp ->
                    val image = InputImage.fromBitmap(bmp, 0)
                    val visionText = withContext(Dispatchers.Default) {
                        textRecognizer.process(image).await()
                    }
                    
                    withContext(Dispatchers.Main) {
                        if (visionText.text.isNotBlank()) {
                            binding.tvResults.text = visionText.text
                            binding.tvResults.visibility = View.VISIBLE
                            Toast.makeText(this@MainActivity, 
                                "识别完成！共 ${visionText.textBlocks.size} 个文本块", 
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MainActivity, 
                                "未识别到文字", 
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Image processing failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, 
                        "图片处理失败：${e.message}", 
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun copyToClipboard() {
        val text = binding.tvResults.text.toString()
        if (text.isNotBlank()) {
            val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("OCR Result", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "没有可复制的内容", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveResults() {
        val text = binding.tvResults.text.toString()
        if (text.isNotBlank()) {
            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val file = java.io.File(
                            getExternalFilesDir(null),
                            "ocr_result_${System.currentTimeMillis()}.txt"
                        )
                        file.writeText(text, Charsets.UTF_8)
                    }
                    Toast.makeText(this@MainActivity, "已保存到应用目录", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Save failed", e)
                    Toast.makeText(this@MainActivity, "保存失败：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "没有可保存的内容", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCamera()
        cameraExecutor.shutdown()
        textRecognizer.close()
    }

    companion object {
        private const val TAG = "VideoOCR"
    }
}
