package com.example.myapplication.ui.record

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray

class RecordViewModel(application: Application) : AndroidViewModel(application) {

    private val _subtypeCounts = MutableLiveData<Map<String, Int>>()
    val subtypeCounts: LiveData<Map<String, Int>> = _subtypeCounts

    fun loadData(currentPetType: String) {
        val sharedPref = getApplication<Application>().getSharedPreferences("upload_records", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("records", "[]")
        val jsonArray = JSONArray(jsonString)

        val counts = mutableMapOf<String, Int>()
        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val subtype = item.getString("subtype")

            // 根據當前 petType 過濾
            val validSubtypes = when (currentPetType) {
                "書摘" -> listOf("文學", "自然", "歷史", "藝術")
                "食譜" -> listOf("中式", "西式", "東南亞")
                "電影" -> listOf("喜劇", "悲劇", "恐怖片", "愛情片")
                "景點" -> listOf("人文景", "自然景", "餐廳")
                else -> emptyList()
            }

            if (subtype in validSubtypes) {
                counts[subtype] = counts.getOrDefault(subtype, 0) + 1
            }
        }

        _subtypeCounts.value = counts
    }
}

