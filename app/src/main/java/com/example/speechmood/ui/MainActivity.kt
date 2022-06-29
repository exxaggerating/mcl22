package com.example.speechmood.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Chronometer
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.speechmood.R
import com.example.speechmood.audio.MoodDetectionHelper
import com.example.speechmood.audio.PredictionScores
import com.example.speechmood.util.Constants
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.scheduleAtFixedRate

class MainActivity : AppCompatActivity() {
    private val isRecordAudioPermissionGranted: Boolean
        get() = ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private val playButton: FloatingActionButton by lazy { findViewById(R.id.play) }
    private val stopButton: FloatingActionButton by lazy { findViewById(R.id.stop) }

    private val chronometer: Chronometer by lazy { findViewById(R.id.chronometer) }

    private val model: MainViewModel by lazy {
        ViewModelProvider(this).get(MainViewModel::class.java)
    }

    private val tflite: Interpreter by lazy {
        Interpreter(FileUtil.loadMappedFile(this, modelPath))
    }

    private val tfInputSize: Size by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1])
    }

    private val labels: List<String> by lazy {
        FileUtil.loadLabels(this, labelsPath)
    }

    private val predictionAdapter: PredictionAdapter by lazy { PredictionAdapter() }

    private var detector: MoodDetectionHelper? = null

    private val predictions: ArrayList<PredictionScores> = ArrayList()
    private val predictionTimestamps: ArrayList<Long> = ArrayList()

    private val predictionTimer: Timer = Timer()
    private var activeTimerTask: TimerTask? = null

    private var startTimestamp: Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val visualiser = findViewById<SoundVisualiser>(R.id.visualizer)

        with(findViewById<RecyclerView>(R.id.prediction_adapter)) {
            setHasFixedSize(false)
            adapter = predictionAdapter
        }

        if (model.isRecordingInProgress) {
            if (model.isRecording) {
                playButton.visibility = View.INVISIBLE
            }

            stopButton.visibility = View.VISIBLE
        }

        model.registerDataReceiveListener(visualiser)

        requestMicrophonePermission()
    }

    fun onPlayClick(view: View) {
        initRecording()
        continueRecording()
    }

    fun onStopClick(view: View) {
        if (model.isRecordingInProgress) {
            pauseRecording()
            clearRecording()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != requestRecordAudio) {
            return
        }

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(this.toString(), "Audio permission granted")
        } else {
            Log.e(this.toString(), "Audio permission not granted :(")
        }
    }

    private fun initRecording() {
        if (!model.isRecordingInProgress) {
            stopButton.visibility = View.VISIBLE
        }
    }

    private fun continueRecording() {
        if (model.isRecording) {
            return
        }

        playButton.visibility = View.INVISIBLE
        model.startRecording()

        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()

        startTimestamp = System.currentTimeMillis()

        predictions.add(labels.map { it to 0.0F })
        predictionTimestamps.add(startTimestamp)

        activeTimerTask = predictionTimer.scheduleAtFixedRate(predictionPeriod, predictionPeriod) {
            runDetector()
        }
    }

    private fun pauseRecording() {
        if (model.isRecording) {
            model.pauseRecording()
            playButton.visibility = View.VISIBLE
        }
    }

    private fun clearRecording() {
        model.clearRecording()
        detector?.clearBuffer()
        activeTimerTask?.cancel()
        predictionTimer.purge()

        stopButton.visibility = View.INVISIBLE
        playButton.visibility = View.VISIBLE

        chronometer.stop()

        val intent = Intent(this, AnalysisActivity::class.java)

        with(intent) {
            putExtra(Constants.IntentNames.StartTime, startTimestamp)
            putExtra(Constants.IntentNames.Timestamps, predictionTimestamps.toLongArray())

            val scores = predictions.flatMap {
                it.map { itt -> itt.second }
            }

            putExtra(Constants.IntentNames.FlattenedScores, scores.toFloatArray())
            putExtra(Constants.IntentNames.Labels, labels.toTypedArray())
        }

        startActivity(intent)
    }

    private fun requestMicrophonePermission() {
        if (isRecordAudioPermissionGranted) {
            Log.i(this.toString(), "Permission already granted")
            return
        }

        requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), requestRecordAudio)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun runDetector() {
        if (!isRecordAudioPermissionGranted) {
            return
        }

        detector ?: run {
            model.recordingFormat ?: return@run

            detector = MoodDetectionHelper(
                tflite,
                labels,
                model.recordingFormat!!,
                tfInputSize.height
            )

            model.registerDataReceiveListener(detector!!)
        }

        detector?.let { moodDetectionHelper ->
            val p = moodDetectionHelper.predict()
            val time = System.currentTimeMillis()

            Log.d(this.toString(), p.toString())

            predictions.add(p)
            predictionTimestamps.add(time)

            val filteredOutput = p.filter { it.second > minimumDisplayThreshold }
                .sortedByDescending { it.second }
                .map {
                    Pair(
                        resources.getString(resources.getIdentifier(
                            "label_${it.first}", "string", packageName)
                        ),
                        it.second
                    )
                }

            runOnUiThread {
                predictionAdapter.predictionScores = filteredOutput
                predictionAdapter.notifyDataSetChanged()
            }
        }
    }

    companion object {
        private val tag = MainActivity::class.java.simpleName

        private const val modelPath = "model.tflite"
        private const val labelsPath = "labels.txt"

        private const val requestRecordAudio: Int = 13

        private const val minimumDisplayThreshold: Float = 0.25F
        private const val predictionPeriod: Long = 1000L
    }
}
