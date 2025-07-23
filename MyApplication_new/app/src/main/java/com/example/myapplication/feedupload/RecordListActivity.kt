package com.example.myapplication.feedupload
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.feedupload.RecordItem
import com.example.myapplication.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class RecordListActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_record_list)

        val listView = findViewById<ListView>(R.id.record_list_view)
        val sharedPref = getSharedPreferences("upload_records", Context.MODE_PRIVATE)
        println(sharedPref)
        val jsonString = sharedPref.getString("records", "[]")
        println(jsonString)
        val jsonArray = JSONArray(jsonString)
        println(jsonArray)

        val currentPetType = intent.getStringExtra("pet_type") ?: "書摘"

        title = currentPetType

        val subtypeMap = mapOf(
            "書摘" to listOf("文學", "自然", "歷史", "藝術"),
            "景點" to listOf("人文景", "自然景", "餐廳"),
            "食譜" to listOf("中式", "西式", "東南亞"),
            "電影" to listOf("喜劇", "悲劇", "恐怖片", "愛情片")
        )

        val validSubtypes = subtypeMap[currentPetType] ?: emptyList()

        val records = mutableListOf<RecordItem>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val subtype = obj.getString("subtype")

            // 只顯示屬於目前petType的大類資料
            if (!validSubtypes.contains(subtype)) continue
            val intro = obj.getString(("intro"))
            val format = obj.getString("format")
            val content = obj.getString("content")
            val timestamp = obj.optString("timestamp", getCurrentTime())
            records.add(RecordItem(subtype, format, intro,content, timestamp))
        }

        val adapter = object : ArrayAdapter<RecordItem>(
            this,
            android.R.layout.simple_list_item_2,
            android.R.id.text1,
            records
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val inflater = layoutInflater
                val row = inflater.inflate(R.layout.record_list_item, parent, false)
                val item = getItem(position)!!
                val text1 = row.findViewById<TextView>(R.id.record_text)
                val shareButton = row.findViewById<Button>(R.id.share_button)
                val infoButton = row.findViewById<Button>(R.id.info_button)

                val fileName = item.content
                //    if (item.format == "文字") item.content else getFileNameFromUri(Uri.parse(item.content))
                //        ?: "(檔案)"
                text1.text =
                    "類型: ${item.subtype}\n格式: ${item.format}\n介紹: ${item.intro}\n時間: ${item.timestamp}"

                shareButton.setOnClickListener {
                    /*
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type =
                        if (item.format == "文字") "text/plain" else contentResolver.getType(
                            Uri.parse(item.content)
                        ) ?: "application/octet-stream"
                    if (item.format == "文字") {
                        intent.putExtra(Intent.EXTRA_TEXT, item.content)
                    } else {
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.parse(item.content))
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    startActivity(Intent.createChooser(intent, "分享資料"))

                     */
                    val logData = JSONObject()
                    logData.put("subtype",item.subtype)
                    logData.put("format",item.format)
                    logData.put("intro",item.intro)
                    logData.put("content",item.content)
                    logData.put("timestamp",item.timestamp)

                    val body = logData.toString().toRequestBody("application/json".toMediaType())


                    val request = Request.Builder()
                        .url("http://192.168.63.223:5000/add")
                        .post(body)
                        .build()


                    OkHttpClient().newCall(request).enqueue(object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                Toast.makeText(this@RecordListActivity, "分享失敗: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            runOnUiThread {
                                Toast.makeText(this@RecordListActivity, "分享成功", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }

                infoButton.setOnClickListener {
                    try {
                        val intent = Intent(this@RecordListActivity, ViewContentActivity::class.java)
                        intent.putExtra("file_url", item.content)
                        intent.putExtra("format", item.format)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@RecordListActivity, "無法開啟檔案", Toast.LENGTH_SHORT).show()
                    }
                }

                return row
            }
        }

        listView.adapter = adapter
    }
    private fun getFileNameFromUri(uri: Uri): String {
        val nameFromCursor = try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            }
        } catch (_: Exception) {
            null
        }

        val hasExtension = nameFromCursor?.contains(".") == true
        if (nameFromCursor != null && hasExtension) return nameFromCursor

        val mimeType = contentResolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: when (mimeType) {
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "audio/mpeg" -> "mp3"
                "audio/wav" -> "wav"
                "video/mp4" -> "mp4"
                "video/3gpp" -> "3gp"
                else -> null
            }

        val base = uri.lastPathSegment?.substringAfterLast("/") ?: "未知檔案"
        return if (extension != null) "$base.$extension" else base
    }

    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

}