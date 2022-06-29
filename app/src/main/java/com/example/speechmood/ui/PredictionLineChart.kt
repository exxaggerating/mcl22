package com.example.speechmood.ui

import android.content.Context
import android.util.AttributeSet
import com.example.speechmood.R
import com.example.speechmood.audio.PredictionScores
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlin.math.round

class PredictionLineChart : LineChart {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)

    private var startTimestamp: Long = System.currentTimeMillis()
    private var labels: List<String> = List(1) { "" }

    private var entries: Map<String, ArrayList<Entry>> = emptyMap()

    fun initialise(startTimestamp: Long, labels: List<String>? = null) {
        this.startTimestamp = startTimestamp

        this.labels = labels ?: this.labels

        entries = this.labels.associateWith { ArrayList() }
    }

    fun addPrediction(timestamp: Long, scores: PredictionScores) {
        scores.forEach {
            val entry = Entry((timestamp - startTimestamp).toFloat(), round(it.second * 100))

            entries[it.first]?.add(entry)
        }

        val lineData = LineData()

        entries.forEach {
            val lineDataSet = LineDataSet(it.value, it.key)
            // TODO: Define colours
            lineDataSet.color = R.color.colorPrimary

            lineData.addDataSet(lineDataSet)
        }

        data = lineData
        invalidate()
    }
}
