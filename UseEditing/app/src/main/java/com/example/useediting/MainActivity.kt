package com.example.useediting

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.OutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var rotateButton: Button
    private lateinit var flipButton: Button
    private lateinit var saveButton: Button
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var contrastSeekBar: SeekBar
    private lateinit var saturationSeekBar: SeekBar

    private var originalBitmap: Bitmap? = null
    private var currentBitmap: Bitmap? = null

    private val getImageFromGallery =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val imageUri: Uri? = result.data?.data
                imageUri?.let {
                    val inputStream = contentResolver.openInputStream(it)
                    val bitmap = BitmapFactory.decodeStream(inputStream)
                    originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                    currentBitmap = originalBitmap
                    imageView.setImageBitmap(currentBitmap)
                    resetSliders()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        rotateButton = findViewById(R.id.rotateButton)
        flipButton = findViewById(R.id.flipButton)
        saveButton = findViewById(R.id.saveButton)
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        contrastSeekBar = findViewById(R.id.contrastSeekBar)
        saturationSeekBar = findViewById(R.id.saturationSeekBar)

        selectImageButton.setOnClickListener { openGallery() }
        rotateButton.setOnClickListener { rotateImage() }
        flipButton.setOnClickListener { flipImage() }
        saveButton.setOnClickListener { saveImageToGallery() }

        val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyFilters()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        brightnessSeekBar.setOnSeekBarChangeListener(seekBarListener)
        contrastSeekBar.setOnSeekBarChangeListener(seekBarListener)
        saturationSeekBar.setOnSeekBarChangeListener(seekBarListener)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        getImageFromGallery.launch(intent)
    }

    private fun rotateImage() {
        currentBitmap?.let {
            val matrix = Matrix().apply { postRotate(90f) }
            currentBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            originalBitmap = currentBitmap
            imageView.setImageBitmap(currentBitmap)
            resetSliders()
        }
    }

    private fun flipImage() {
        currentBitmap?.let {
            val matrix = Matrix().apply { preScale(-1f, 1f) }
            currentBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            originalBitmap = currentBitmap
            imageView.setImageBitmap(currentBitmap)
            resetSliders()
        }
    }

    private fun applyFilters() {
        originalBitmap?.let { bitmap ->
            val bmp = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            val canvas = Canvas(bmp)
            val paint = Paint()

            val brightness = brightnessSeekBar.progress - 100
            val contrast = contrastSeekBar.progress / 100f
            val saturation = saturationSeekBar.progress / 100f

            val colorMatrix = ColorMatrix()

            // 1. Saturation
            val saturationMatrix = ColorMatrix()
            saturationMatrix.setSaturation(saturation)
            colorMatrix.postConcat(saturationMatrix)

            // 2. Contrast and Brightness
            val contrastMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness.toFloat(),
                0f, contrast, 0f, 0f, brightness.toFloat(),
                0f, 0f, contrast, 0f, brightness.toFloat(),
                0f, 0f, 0f, 1f, 0f
            ))
            colorMatrix.postConcat(contrastMatrix)

            paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
            canvas.drawBitmap(bitmap, 0f, 0f, paint)

            currentBitmap = bmp
            imageView.setImageBitmap(currentBitmap)
        }
    }

    private fun resetSliders() {
        brightnessSeekBar.progress = 100
        contrastSeekBar.progress = 100
        saturationSeekBar.progress = 100
    }

    private fun saveImageToGallery() {
        currentBitmap?.let { bitmap ->
            val filename = "Edited_Image_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val contentResolver = contentResolver
            val uri: Uri? = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                val outputStream: OutputStream? = contentResolver.openOutputStream(it)
                outputStream.use { stream ->
                    if (stream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                }

                Toast.makeText(this, "Image saved to gallery!", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No image to save!", Toast.LENGTH_SHORT).show()
        }
    }
}

