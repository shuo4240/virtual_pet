package com.example.myapplication
import android.content.ClipData.Item
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
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

class SharedListActivity : AppCompatActivity() {
    //private lateinit var listView: ListView
    private val client = OkHttpClient()
    private lateinit var adapter: ArrayAdapter<RecordItem>
    private val records = mutableListOf<RecordItem>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_shared_list)

        val listView = findViewById<ListView>(R.id.shared_list_view)


        val currentPetType = intent.getStringExtra("pet_type") ?: "書摘"
        title = currentPetType

        val subtypeMap = mapOf(
            "書摘" to listOf("文學", "自然", "歷史", "藝術"),
            "景點" to listOf("人文景", "自然景", "餐廳"),
            "食譜" to listOf("中式", "西式", "東南亞"),
            "電影" to listOf("喜劇", "悲劇", "恐怖片", "愛情片")
        )


        val validSubtypes = subtypeMap[currentPetType] ?: emptyList()

        fetchShareFromBackend(validSubtypes)
        // 建立 adapter 但資料先空

        adapter = object : ArrayAdapter<RecordItem>(
            this,
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
                    Toast.makeText(this@SharedListActivity, "餵食成功", Toast.LENGTH_SHORT).show()
                    saveRecord(item.subtype, item.format, item.intro,item.content,item.timestamp)


                }

                infoButton.setOnClickListener {
                    try {
                        val intent = Intent(this@SharedListActivity, ViewContentActivity::class.java)
                        intent.putExtra("file_url", item.content)
                        intent.putExtra("format", item.format)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@SharedListActivity, "無法開啟檔案", Toast.LENGTH_SHORT).show()
                    }
                }

                return row
            }
        }


        listView.adapter = adapter




    }

    private fun saveRecord(subtype: String, format: String, intro:String,content: String, timestamp: String) {
        val sharedPref = getSharedPreferences("upload_records", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("records", "[]")
        val jsonArray = JSONArray(jsonString)

//        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//        val formattedTime = sdf.format(Date())


        val record = JSONObject()
        record.put("subtype", subtype)
        record.put("format", format)
        record.put("intro",intro)
        record.put("content", content)
        record.put("timestamp", timestamp)

        jsonArray.put(record)

        sharedPref.edit().putString("records", jsonArray.toString()).apply()
    }

    private fun fetchShareFromBackend(validSubtypes: List<String>) {
        val request = Request.Builder()
            .url("http://192.168.63.223:5000/messages") // 請改成你的實際後端 API
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@SharedListActivity, "讀取失敗: ${e.message}", Toast.LENGTH_LONG).show()
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

                    resultList.add(RecordItem(subtype, format,intro, content, timestamp))
                }

                runOnUiThread {
                    records.clear()
                    records.addAll(resultList)
                    adapter.notifyDataSetChanged()
                }
            }
        })
    }

}