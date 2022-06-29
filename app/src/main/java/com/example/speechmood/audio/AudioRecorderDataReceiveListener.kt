package com.example.speechmood.audio

interface AudioRecorderDataReceiveListener {
    fun onDataReceive(buffer: Array<Float>, samples: Int)
}
