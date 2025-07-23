package com.example.myapplication.ui.home

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.myapplication.feedupload.RecordItem
import org.json.JSONArray
import org.json.JSONObject

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val _subtypeCounts = MutableLiveData<Map<String, Int>>()
    val subtypeCounts: LiveData<Map<String, Int>> get() = _subtypeCounts

    init {
        loadSubtypeCountsFromRecords()
    }

    /**
     * 從 upload_records 中解析記錄，統計各副屬性的出現次數
     */
    private fun loadSubtypeCountsFromRecords() {
        val sharedPref = getApplication<Application>()
            .getSharedPreferences("upload_records", Context.MODE_PRIVATE)

        val jsonString = sharedPref.getString("records", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)

        val records = mutableListOf<RecordItem>()

        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val subtype = obj.optString("subtype", "")
            val format = obj.optString("format", "")
            val intro = obj.optString("intro", "")
            val content = obj.optString("content", "")
            val timestamp = obj.optString("timestamp", "")
            records.add(RecordItem(subtype, format, intro, content, timestamp))
        }

        val counts = records.groupingBy { it.subtype }
            .eachCount()

        _subtypeCounts.value = counts
    }

    /**
     * 手動刷新統計（例如從 FeedUploadActivity 回來後）
     */
    fun refreshCounts() {
        loadSubtypeCountsFromRecords()
    }
}