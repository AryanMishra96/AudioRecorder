package com.example.audiorecorder
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.audiorecorder.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: Button
    private lateinit var playButton: Button
    private var isRecording = false
    private lateinit var audioFile: File
    private val requestRecordAudioPermission = 200
    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recordButton = findViewById(R.id.recordBtn)
        playButton = findViewById(R.id.playBtn)
        if (!hasPermissions()) {
            requestAudioPermissions()
        } else {
            permissionToRecordAccepted = true
        }
        audioFile = File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "recording.pcm")
        recordButton.setOnClickListener {
            if (permissionToRecordAccepted) {
                if (!isRecording) {
                    startRecording()
                } else {
                    stopRecording()
                }
            } else {
                requestAudioPermissions()
            }
        }
        playButton.setOnClickListener {
            startPlaying()
        }
    }
    private fun hasPermissions(): Boolean {
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
    private fun requestAudioPermissions() {
        ActivityCompat.requestPermissions(this, permissions, requestRecordAudioPermission)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestRecordAudioPermission) {
            permissionToRecordAccepted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (!permissionToRecordAccepted) {
                // Permission denied, inform the user and disable functionality that requires permission
                recordButton.isEnabled = false
                playButton.isEnabled = false
            }
        }
    }
    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestAudioPermissions()
            return
        }

        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig, audioFormat, bufferSize)
        audioRecord.startRecording()
        isRecording = true
        recordButton.text = "Stop Recording"

        val recordingThread = Thread {
            val data = ByteArray(bufferSize)
            FileOutputStream(audioFile).use { fos ->
                while (isRecording) {
                    val read = audioRecord.read(data, 0, data.size)
                    if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                        fos.write(data, 0, read)
                    }
                }
            }
            audioRecord.stop()
            audioRecord.release()
        }
        recordingThread.start()
    }
    private fun stopRecording() {
        isRecording = false
        recordButton.text = "Record"
    }
    private fun startPlaying() {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val bufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(audioFormat)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .build()

        val audioData = audioFile.readBytes()
        audioTrack.play()
        audioTrack.write(audioData, 0, audioData.size)
    }
}
