package com.example.speechmood.ui

import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.speechmood.R
import com.example.speechmood.audio.PredictionScores

class PredictionAdapter : RecyclerView.Adapter<PredictionAdapter.ViewHolder>() {

    var predictionScores: PredictionScores = emptyList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val textView: TextView by lazy { view.findViewById(R.id.label_text_view) }
        private val progressBar: ProgressBar by lazy { view.findViewById(R.id.progress_bar) }

        fun bind(position: Int, label: String, score: Float) {
            textView.text = label

            progressBar.progressBackgroundTintList = progressColorPairList[position % 3].first
            progressBar.progressTintList = progressColorPairList[position % 3].second

            val animation = ObjectAnimator.ofInt(progressBar, "progress", progressBar.progress, (score * 100).toInt())
            animation.duration = 100
            animation.interpolator = AccelerateDecelerateInterpolator()
            animation.start()
        }

        companion object {
            /** List of pairs of background tint and progress tint */
            private val progressColorPairList = listOf(
                ColorStateList.valueOf(0xfff9e7e4.toInt()) to ColorStateList.valueOf(0xffd97c2e.toInt()),
                ColorStateList.valueOf(0xfff7e3e8.toInt()) to ColorStateList.valueOf(0xffc95670.toInt()),
                ColorStateList.valueOf(0xffecf0f9.toInt()) to ColorStateList.valueOf(0xff714Fe7.toInt()),
            )
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.probability, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val prediction = predictionScores[position]

        holder.bind(position, prediction.first, prediction.second)
    }

    override fun getItemCount() = predictionScores.size
}
