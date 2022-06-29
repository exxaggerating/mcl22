package com.example.speechmood.ui

import android.app.Application
import android.media.AudioFormat
import androidx.lifecycle.AndroidViewModel
import com.example.speechmood.audio.AudioRecorder
import com.example.speechmood.audio.AudioRecorderDataReceiveListener

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val isRecordingInProgress: Boolean
        get() { return recordingInProgress}

    val isRecording: Boolean
        get() { return recorder.isRecording }

    val recordingFormat: AudioFormat?
        get() { return recorder.recordingFormat }

    private val recorder: AudioRecorder = AudioRecorder()

    private var recordingInProgress: Boolean = false

    fun registerDataReceiveListener(listener: AudioRecorderDataReceiveListener) {
        recorder.registerDataReceiveListener(listener)
    }

    fun startRecording() {
        if (!recordingInProgress) {
            recordingInProgress = true
            recorder.startRecording()
        } else {
            recorder.continueRecording()
        }
    }

    fun pauseRecording() {
        recorder.pauseRecording()
    }

    fun clearRecording() {
        recordingInProgress = false
        recorder.clearRecording()
    }
}
