package com.example.speechmood.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.util.*


@SuppressLint("MissingPermission")
class AudioRecorder {
    val isRecording: Boolean
        get() { return audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING }

    val recordingFormat: AudioFormat?
        get() = audioRecord?.format

    private val isStopped: Boolean
        get() { return audioRecord?.recordingState == AudioRecord.RECORDSTATE_STOPPED }

    private var minBufferSizeInBytes: Int = 0

    private var audioRecord: AudioRecord? = null

    private val recordingHandler: Handler
    private val callbacksHandler: Handler

    private val dataReceiveListeners = mutableListOf<AudioRecorderDataReceiveListener>()

    init {
        minBufferSizeInBytes =  AudioRecord.getMinBufferSize(recordingRate, channel, format)

        if (minBufferSizeInBytes == AudioRecord.ERROR
            || minBufferSizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
            minBufferSizeInBytes = recordingRate * 2
//            minBufferSizeInBytes = recordingRate / 10 * 1 * Float.SIZE_BYTES
        }

        val recordingThread = HandlerThread("RecordingThread")
        val callbacksThread = HandlerThread("CallbacksExecThread")

        recordingThread.start()
        callbacksThread.start()

        recordingHandler = Handler(recordingThread.looper)
        callbacksHandler = Handler(callbacksThread.looper)
    }

    fun registerDataReceiveListener(listener: AudioRecorderDataReceiveListener) {
        dataReceiveListeners.add(listener)
    }

    fun startRecording() {
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            recordingRate,
            channel,
            format,
            minBufferSizeInBytes
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            Log.d(this.toString(), "Audio Record can't be initialised")
            return
        }

        Log.d(this.toString(), String.format(
            Locale.getDefault(),
            "Audio Client successfully created with minimum buffer of %d bytes",
            minBufferSizeInBytes
        ))

        audioRecord?.let { startRecording(it, recordingHandler) }

        Log.d(this.toString(), String.format(
            Locale.getDefault(),
            "%d: Recording started",
            System.currentTimeMillis()
        ))
    }

    fun pauseRecording() {
        if (isRecording) {
            audioRecord?.let { stopRecording(it, recordingHandler) }

            Log.d(this.toString(), String.format(
                Locale.getDefault(), "%d: Recording paused", System.currentTimeMillis()
            ))
        } else {
            Log.d(this.toString(), "Attempt to pause recording while not recording")
        }
    }

    fun continueRecording() {
        if (isStopped) {
            audioRecord?.let { startRecording(it, recordingHandler) }
        } else {
            Log.d(
                this.toString(),
                "Attempt to continue recording before initializing recording context or" +
                        " while already recording."
            )
        }
    }

    fun clearRecording() {
        audioRecord ?: return

        audioRecord?.let {
            stopRecording(it, recordingHandler)
            it.release()
        }

        audioRecord = null

        Log.d(this.toString(), String.format(
            Locale.getDefault(), "%d: Recording cleared", System.currentTimeMillis()
        ))
    }


    private fun createRecurringReadAudioTask(): Runnable {
        return Runnable {
            audioRecord ?: return@Runnable

            recordingHandler.post(createRecurringReadAudioTask())

            val audioBuffer = FloatArray(size = minBufferSizeInBytes / Float.SIZE_BYTES)

            val readSamples: Int? = audioRecord?.read(
                audioBuffer,
                0,
                audioBuffer.size,
                AudioRecord.READ_BLOCKING
            )

            readSamples ?: return@Runnable

            when {
                readSamples > 0 -> {
                    asyncProcessReceivedData(audioBuffer.toTypedArray(), readSamples)
                }
                readSamples == 0 -> {
                    // skip
                }
                else -> {
                    Log.e(this.toString(), String.format(
                        Locale.getDefault(), "Reading Error: %d", readSamples
                    ))
                }
            }
        }
    }

    private fun startRecording(ar: AudioRecord, handler: Handler) {
        ar.startRecording()
        handler.post(createRecurringReadAudioTask())
    }

    private fun stopRecording(ar: AudioRecord, handler: Handler) {
        ar.stop()
        handler.removeCallbacksAndMessages(null)
    }

    private fun asyncProcessReceivedData(buffer: Array<Float>, samples: Int) {
        dataReceiveListeners.forEach {
            callbacksHandler.post { it.onDataReceive(buffer, samples) }
        }
    }

    companion object {
        private val tag = AudioRecorder::class.java.simpleName

        private const val recordingRate: Int = 22050
        private const val channel: Int = AudioFormat.CHANNEL_IN_MONO
        private const val format: Int = AudioFormat.ENCODING_PCM_FLOAT
    }
}
