package com.example.myapplication.ui.share

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.SharedListActivity
import com.example.myapplication.databinding.FragmentShareBinding
import com.example.myapplication.feedupload.RecordListActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.feedupload.RecordItem
import com.example.myapplication.feedupload.ViewContentActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/*
class ShareFragment : Fragment() {

    private var _binding: FragmentShareBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val shareViewModel =
            ViewModelProvider(this).get(ShareViewModel::class.java)

        _binding = FragmentShareBinding.inflate(inflater, container, false)
        val root: View = binding.root

        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.button.setOnClickListener {
            val intent = Intent(requireContext(), SharedListActivity::class.java)
            intent.putExtra("pet_type", requireActivity().title) // 預設測試用
            startActivity(intent)
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}*/

class ShareFragment : Fragment() {

    private val client = OkHttpClient()
    private lateinit var adapter: ArrayAdapter<RecordItem>
    private val records = mutableListOf<RecordItem>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_share, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listView = view.findViewById<ListView>(R.id.shared_list_view)

        val currentPetType = requireActivity().title.toString()
//        val currentPetType = activity?.intent?.getStringExtra("pet_type") ?: "書摘"
//        activity?.title = currentPetType

        val subtypeMap = mapOf(
            "書摘" to listOf("文學", "自然", "歷史", "藝術"),
            "景點" to listOf("人文景", "自然景", "餐廳"),
            "食譜" to listOf("中式", "西式", "東南亞"),
            "電影" to listOf("喜劇", "悲劇", "恐怖片", "愛情片")
        )

        val validSubtypes = subtypeMap[currentPetType] ?: emptyList()

        adapter = object : ArrayAdapter<RecordItem>(
            requireContext(),
            android.R.layout.simple_list_item_2,
            android.R.id.text1,
            records
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val inflater = layoutInflater
                val row = inflater.inflate(R.layout.shared_list_item, parent, false)
                val item = getItem(position)!!
                val text1 = row.findViewById<TextView>(R.id.record_text)
                val feedButton = row.findViewById<Button>(R.id.download_button)
                val infoButton = row.findViewById<Button>(R.id.info_button)

                text1.text =
                    "類型: ${item.subtype}\n格式: ${item.format}\n簡介: ${item.intro}\n時間: ${item.timestamp}"

                feedButton.setOnClickListener {
                    Toast.makeText(requireContext(), "餵食成功", Toast.LENGTH_SHORT).show()
                    saveRecord(item.subtype, item.format, item.intro, item.content, item.timestamp)
                }

                infoButton.setOnClickListener {
                    try {
                        val intent = Intent(requireContext(), ViewContentActivity::class.java)
                        intent.putExtra("file_url", item.content)
                        intent.putExtra("format", item.format)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "無法開啟檔案", Toast.LENGTH_SHORT).show()
                    }
                }

                return row
            }
        }

        listView.adapter = adapter
        fetchShareFromBackend(validSubtypes)
    }

    private fun saveRecord(subtype: String, format: String, intro: String, content: String, timestamp: String) {
        val sharedPref = requireContext().getSharedPreferences("upload_records", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("records", "[]")
        val jsonArray = JSONArray(jsonString)

        val record = JSONObject()
        record.put("subtype", subtype)
        record.put("format", format)
        record.put("intro", intro)
        record.put("content", content)
        record.put("timestamp", timestamp)

        jsonArray.put(record)
        sharedPref.edit().putString("records", jsonArray.toString()).apply()
    }

    private fun fetchShareFromBackend(validSubtypes: List<String>) {
        val request = Request.Builder()
            .url("http://192.168.63.223:5000/messages")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "讀取失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) return

                val jsonArray = JSONArray(body)
                val resultList = mutableListOf<RecordItem>()

                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val subtype = obj.getString("subtype")
                    if (!validSubtypes.contains(subtype)) continue

                    val intro = obj.getString("intro")
                    val format = obj.getString("format")
                    val content = obj.getString("content")
                    val timestamp = obj.optString("timestamp")

                    resultList.add(RecordItem(subtype, format, intro, content, timestamp))
                }

                activity?.runOnUiThread {
                    records.clear()
                    records.addAll(resultList)
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }
}