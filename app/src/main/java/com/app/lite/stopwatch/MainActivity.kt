package com.app.lite.stopwatch

import android.Manifest
import android.R.attr.name
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.InputType
import android.text.SpannableString
import android.text.TextWatcher
import android.text.format.DateFormat
import android.text.style.AbsoluteSizeSpan
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.main_layout.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object{
        var isStarted = false
        var isPaused = true
        var descriptionText: String? = null
        var millisCount = 0L
        const val millisInSecond = 1000
        const val millisInMinute = 60 * millisInSecond
        const val millisInHour = 60 *millisInMinute
        var timer : Timer? = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        parentLayout.setOnTouchListener { _, motionEvent ->
            if(!isPaused) {
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    stopTimer()
                } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                    startTimer()
                }
                true
            }else
                false
        }

        circleBg.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (!isPaused) {
                        stopTimer()
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isPaused)
                        pauseStopwatch()
                    else
                        startStopwatch()
                    true
                }
                else -> false
            }
        }

        playButton.setOnClickListener {
            isStarted = true

            if(isPaused)
                startStopwatch()
            else
                pauseStopwatch()
        }

        repeatButton.setOnClickListener {
            clearStopwatch()
        }

        descriptionEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun afterTextChanged(p0: Editable?) {
                descriptionText = p0.toString()
            }

        })

        screenshotButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    takeScreenshot()
                }else{
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        12345
                    );
                }
            }else{
                takeScreenshot()
            }
        }
    }

    override fun onStop() {
        super.onStop()

        if(isStarted && !isChangingConfigurations)
            startService(Intent(this, NotificationService::class.java).apply {
                action = NotificationService.ACTION_START_SERVICE
            })
    }

    override fun onStart() {
        super.onStart()
        startService(Intent(this, NotificationService::class.java).apply {
            action = NotificationService.ACTION_STOP_SERVICE
        })
       init()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && requestCode == 12345) {
            takeScreenshot()
        }
    }

    private fun init(){

        setTimerText(0)

        if(!isPaused)
            startTimer()
        else
            setTimerText(MainActivity.millisCount)

        playButton.isActivated = !isPaused && isStarted
        descriptionEditText.setText(descriptionText)
        if(isStarted) {
            showButtons()
            if(descriptionEditText.text.isNullOrEmpty())
                hideEditText()
            else {
                disableEditText()
                descriptionEditText.clearFocus()
            }
        }else {
            hideButtons()
            showEditText()
            enableEditText()
        }
    }

    private fun pauseStopwatch(){
        isPaused = true
        stopTimer()
        playButton.isActivated = false
    }

    private fun clearStopwatch(){
        isPaused = true
        isStarted = false
        playButton.isActivated = false
        hideButtons()
        showEditText()
        enableEditText()
        stopTimer()
        millisCount = 0
        setTimerText(0)
    }

    private fun startStopwatch(){
        isPaused = false
        startTimer()
        playButton.isActivated = true
        if (descriptionEditText.text.isNullOrEmpty())
            hideEditText()
        else {
            disableEditText()
            descriptionEditText.clearFocus()
        }
        showButtons()
    }

    private fun stopTimer(){
        timer?.let{
            it.cancel()
            it.purge()
        }
    }

    private fun startTimer(){
       stopTimer()
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    if (!isPaused) {
                        millisCount += 10
                        setTimerText(millisCount)
                    }
                }
            }
        }, 10, 10)
    }

    private fun disableEditText(){
        descriptionEditText.isEnabled = false
        descriptionEditText.isFocusable = false
    }

    private fun enableEditText(){
        descriptionEditText.isEnabled = true
        descriptionEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        descriptionEditText.isFocusable = true
        descriptionEditText.isFocusableInTouchMode = true
    }

    private fun showEditText(){
        descriptionEditText.visibility = View.VISIBLE
    }

    private fun hideEditText(){
        descriptionEditText.visibility = View.GONE
        descriptionEditText.clearFocus()
        hideKeyboard()
    }

    private fun hideKeyboard(){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(circleBg.windowToken, 0)
    }

    private fun showButtons(){
        repeatButton.visibility = View.VISIBLE
        screenshotButton.visibility = View.VISIBLE
    }

    private fun hideButtons(){
        repeatButton.visibility = View.GONE
        screenshotButton.visibility = View.GONE
    }

    private fun setTimerText(timeInMillis: Long){

        when{
            timeInMillis.isSecondsTime() -> {
                formSecondsTime(timeInMillis)
            }
            timeInMillis.isMinutesTime() -> {
                formMinutesTime(timeInMillis)
            }
            timeInMillis.isHoursTime() -> {
                formHoursTime(timeInMillis)
            }
        }
    }

    private fun formSecondsTime(timeInMillis: Long){
        val trimmedString = timeInMillis.toString().subSequence(
            0,
            timeInMillis.toString().length - 1
        )

        val text = if(trimmedString.length < 3){
            "0".repeat(3 - trimmedString.length)+trimmedString
        }else{
            trimmedString
        }

        val span = SpannableString(
            "${text.subSequence(0, text.length - 2)} ${
                text.subSequence(
                    text.length - 2,
                    text.length
                )
            }"
        )
        span.setSpan(
            AbsoluteSizeSpan((timeTextView.textSize / 1.5).toInt()),
            span.length - 2,
            span.length,
            0
        )
        timeTextView.text = span
    }

    @SuppressLint("SetTextI18n")
    private fun formMinutesTime(timeInMillis: Long){
        val seconds = (timeInMillis % millisInMinute)/ millisInSecond
        val minutes = timeInMillis/ millisInMinute

        val secondsPart = if(seconds.toString().length < 2){
            "0".repeat(2 - seconds.toString().length) + seconds.toString()
        }else{
            seconds.toString()
        }
        timeTextView.text = "$minutes:${secondsPart}"
    }

    @SuppressLint("SetTextI18n")
    private fun formHoursTime(timeInMillis: Long){
        val seconds = (timeInMillis % millisInMinute)/ millisInSecond
        val minutes = (timeInMillis % millisInHour)/ millisInMinute
        val hours = timeInMillis/ millisInHour

        val secondsPart = if(seconds.toString().length < 2){
            "0".repeat(2 - seconds.toString().length) + seconds.toString()
        }else{
            seconds.toString()
        }

        val minutesPart = if(minutes.toString().length < 2){
            "0".repeat(2 - minutes.toString().length) + minutes.toString()
        }else{
            minutes.toString()
        }

        val span = SpannableString("$hours:$minutesPart:$secondsPart")
        span.setSpan(
            AbsoluteSizeSpan((timeTextView.textSize - 8F.spToPx()).toInt()),
            0,
            span.length,
            0
        )
        timeTextView.text = span
    }

    private fun Long.isSecondsTime() = this < millisInMinute

    private fun Long.isMinutesTime() = this < millisInHour

    private fun Long.isHoursTime() = this >= millisInHour

    private fun takeScreenshot() {
        val now = Date()
        DateFormat.format("yyyy-MM-dd_hh:mm:ss", now)
        try {
            val v1 = window.decorView.rootView
            v1.isDrawingCacheEnabled = true
            val bitmap = Bitmap.createBitmap(v1.drawingCache)
            v1.isDrawingCacheEnabled = false

            val outputStream : OutputStream?

            val imageRoot = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), "Stopwatch"
            )
            imageRoot.mkdirs()

            outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues()
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/Stopwatch")
                val imageUri: Uri? =
                    contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )
                imageUri?.let { contentResolver.openOutputStream(it) }
            }else {
                val imageFile = File(imageRoot, "$now.jpg")
                FileOutputStream(imageFile)
            }
            val quality = 100
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            outputStream?.flush()
            outputStream?.close()
            Toast.makeText(this, "Screenshot successfully saved!", Toast.LENGTH_LONG).show()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }


    private fun Float.spToPx(): Int = (this * Resources.getSystem().displayMetrics.scaledDensity).toInt()

    fun Int.pxToSp(): Float = (this / Resources.getSystem().displayMetrics.scaledDensity)
}