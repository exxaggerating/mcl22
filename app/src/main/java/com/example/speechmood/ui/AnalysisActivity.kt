package com.example.speechmood.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.speechmood.R
import com.example.speechmood.util.Constants
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.*
import kotlin.math.max
import kotlin.math.round


class AnalysisActivity : AppCompatActivity() {

    private var startTime: Long = 0L
    private var timeStamps: LongArray = emptyArray<Long>().toLongArray()

    private var scores: Map<String, List<Float>> = emptyMap()
    private var labels: List<String> = emptyList()
    private var predictedLabels: List<String> = emptyList()

    private val lineChart: LineChart by lazy { findViewById(R.id.line_chart) }
    private val histogram: BarChart by lazy { findViewById(R.id.histogram) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analysis)

        startTime = intent.getLongExtra(Constants.IntentNames.StartTime, 0L)
        timeStamps = intent.getLongArrayExtra(Constants.IntentNames.Timestamps) ?: timeStamps

        labels = intent.getStringArrayExtra(Constants.IntentNames.Labels)?.toList() ?: emptyList()

        val flattenedScores: FloatArray = intent
            .getFloatArrayExtra(Constants.IntentNames.FlattenedScores)
            ?: emptyArray<Float>().toFloatArray()

        predictedLabels = flattenedScores.withIndex().groupBy { it.index / labels.size }.entries.map {
            labels[it.value.withIndex().maxByOrNull { it.value.value }?.index ?: 0]
        }

        scores = flattenedScores.withIndex().groupBy { it.index % labels.size }.entries.associate {
            labels[it.key] to it.value.map { itt -> itt.value }
        }

        plotLineChart()
        plotHistogram()
    }

    private fun plotHistogram() {
        val barData = BarData()

        predictedLabels.groupingBy { it }.eachCount().entries.withIndex().map {
            val barDataSet = BarDataSet(
                listOf(BarEntry(it.index.toFloat(), it.value.value.toFloat())),
                resources.getString(resources.getIdentifier(
                    "label_${it.value.key}", "string", packageName)
                )
            )

            barDataSet.color = resources.getColor(
                resources.getIdentifier("mood_${it.value.key}", "color", packageName),
                null
            )

            barData.addDataSet(barDataSet)
        }

        with(histogram) {
            data = barData
            data.barWidth = 0.9f

            data.setDrawValues(false)

            setFitBars(true)

            setDrawBorders(false)
            setDrawGridBackground(false)
            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
            description.isEnabled = false

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawLabels(false)
            xAxis.setDrawGridLines(false)

            isHighlightPerDragEnabled = false
            isHighlightPerTapEnabled = false

            axisLeft.axisMinimum = 0.0f
            axisLeft.granularity = 1.0f
            axisLeft.isGranularityEnabled = true

            axisRight.axisMinimum = 0.0f
            axisRight.granularity = 1.0f
            axisRight.isGranularityEnabled = true

            animateY(4000)

            invalidate()
        }
    }

    private fun plotLineChart() {
        val entries: Map<String, List<Entry>> = scores.entries.associate {
            it.key to it.value.withIndex().map { itt ->
                val time = (timeStamps[itt.index] - startTime).toFloat() / 1000.0F
                val score = round(itt.value * 100)

                Entry(time, score)
            }
        }

        val lineData = LineData()

        var entryCount = 0

        entries.forEach {
            val lineDataSet = LineDataSet(it.value, resources.getString(
                resources.getIdentifier("label_${it.key}", "string", packageName)
            ))

            lineDataSet.color = resources.getColor(
                resources.getIdentifier("mood_${it.key}", "color", packageName),
                null
            )

            entryCount = max(entryCount, it.value.size)

            lineData.addDataSet(lineDataSet)
        }

        with(lineChart) {
            axisLeft.axisMinimum = 0.0F
            axisLeft.axisMaximum = 100.0F

            axisRight.axisMinimum = 0.0F
            axisRight.axisMaximum = 100.0F

            data = lineData
            data.setDrawValues(false)

            legend.horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER

            setDrawGridBackground(false)

            description.isEnabled = false

            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)

            isHighlightPerDragEnabled = false
            isHighlightPerTapEnabled = false

            zoom(entryCount.toFloat(), 1.0F, 0.0F, center.y)

            zoomAndCenterAnimated(
                0.95F, 1.0F,
                center.x, center.y,
                YAxis.AxisDependency.LEFT, 4000
            )

            invalidate()
        }
    }
}
