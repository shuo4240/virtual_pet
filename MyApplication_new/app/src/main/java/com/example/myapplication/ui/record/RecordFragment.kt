package com.example.myapplication.ui.record

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.databinding.FragmentRecordBinding
import com.example.myapplication.feedupload.RecordListActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter

class RecordFragment : Fragment() {

    private var _binding: FragmentRecordBinding? = null
    private val binding get() = _binding!!

    private lateinit var recordViewModel: RecordViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val petType = requireActivity().title.toString()

        recordViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        ).get(RecordViewModel::class.java)

        recordViewModel.loadData(petType)

        recordViewModel.subtypeCounts.observe(viewLifecycleOwner) { counts ->

            val allSubtypes = mapOf(
                "書摘" to listOf("文學", "自然", "歷史", "藝術"),
                "景點" to listOf("人文景", "自然景", "餐廳"),
                "食譜" to listOf("中式", "西式", "東南亞"),
                "電影" to listOf("喜劇", "悲劇", "恐怖片", "愛情片")
            )

            val currentPetType = requireActivity().title.toString()
            val subtypeList = allSubtypes[currentPetType] ?: emptyList()

            val fullCounts = subtypeList.associateWith { subtype ->
                counts[subtype] ?: 0
            }

            val textBuilder = StringBuilder()
            fullCounts.forEach { (subtype, count) ->
                textBuilder.append("$subtype：$count 筆\n")
            }
            binding.textRecord.text = textBuilder.toString()

            // 準備雷達圖資料
            val entries = subtypeList.map { RadarEntry(fullCounts[it]?.toFloat() ?: 0f) }
            val labels = subtypeList

            val totalSum = entries.sumOf { it.value.toDouble() }
            val adjustedEntries = if (totalSum == 0.0) {
                entries.map { RadarEntry(0.1f) }
            } else {
                entries
            }

            val dataSet = RadarDataSet(entries, "類別統計").apply {
                color = Color.parseColor("#FFA500")
                fillColor = Color.parseColor("#FFA500")
                setDrawFilled(true)
                valueTextSize = 12f
                lineWidth = 2f
            }

            val radarData = RadarData(dataSet)

            radarData.setValueFormatter(object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            })

            binding.radarChart.data = radarData

            binding.radarChart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                textSize = 12f
                position = XAxis.XAxisPosition.BOTTOM_INSIDE
                textColor = Color.BLACK
                setDrawLabels(true)
            }

            binding.radarChart.yAxis.apply {
                axisMinimum = 0f
                axisMaximum = (counts.values.maxOrNull() ?: 5).toFloat() + 1f
                textSize = 12f
                textColor = Color.GRAY
                setDrawLabels(false)
            }

            binding.radarChart.description.isEnabled = false
            binding.radarChart.legend.isEnabled = false
            binding.radarChart.invalidate()
        }

        // ✅ 加入跳轉功能
        binding.btnRecordlist.setOnClickListener {
            val intent = Intent(requireContext(), RecordListActivity::class.java)
            intent.putExtra("pet_type", petType) // 若你在歷史頁要用到類型
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
