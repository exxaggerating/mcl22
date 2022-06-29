package com.example.speechmood.audio

import android.media.AudioFormat
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MoodDetectionHelper(private val tflite: Interpreter, private val labels: List<String>,
                          private val format: AudioFormat, private val inputLength: Int)
    : AudioRecorderDataReceiveListener {

    private val prediction: PredictionScores
        get() {
            return outputBuffer.floatArray.mapIndexed {
                    _index: Int, _score: Float -> labels[_index] to _score
            }
        }

    private var inputTensor: TensorAudio = TensorAudio.create(format, inputLength)

    private val outputBuffer: TensorBuffer by lazy {
        TensorBuffer.createFixedSize(IntArray(1) { labels.size }, DataType.FLOAT32)
    }

    override fun onDataReceive(buffer: Array<Float>, samples: Int) {
        inputTensor.load(buffer.toFloatArray())
    }

    fun predict(): PredictionScores {
        tflite.run(inputTensor.tensorBuffer.buffer, outputBuffer.buffer)

        return prediction
    }

    fun clearBuffer() {
        inputTensor = TensorAudio.create(format, inputLength)
    }
}

typealias PredictionScores = List<Pair<String, Float>>
