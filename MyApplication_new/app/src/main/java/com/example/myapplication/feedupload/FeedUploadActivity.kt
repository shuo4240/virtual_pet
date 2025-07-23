package com.example.myapplication.feedupload

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FeedUploadActivity : AppCompatActivity() {

    private lateinit var spinnerSubType: Spinner
    private lateinit var spinnerFormat: Spinner
    private lateinit var editText: EditText
    private lateinit var introText: EditText
    private lateinit var btnSelectFile: Button
    private lateinit var btnUpload: Button
    private var selectedUri: Uri? = null
    private val PICK_FILE_CODE = 123

    private val subTypeMap = mapOf(
        "書摘" to R.array.subtype_books,
        "景點" to R.array.subtype_places,
        "食譜" to R.array.subtype_recipes,
        "電影" to R.array.subtype_movies
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_feed)

        val petType = intent.getStringExtra("pet_type") ?: "書摘"
        title = petType

        spinnerSubType = findViewById(R.id.spinner_subtype)
        spinnerFormat = findViewById(R.id.spinner_format)
        editText = findViewById(R.id.edit_text_input)
        introText = findViewById(R.id.intro_text_input)
        btnSelectFile = findViewById(R.id.btn_select_file)
        btnUpload = findViewById(R.id.btn_upload)

        subTypeMap[petType]?.let {
            ArrayAdapter.createFromResource(this, it, android.R.layout.simple_spinner_item).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerSubType.adapter = adapter
            }
        }

        spinnerFormat.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position == 0) {
                    editText.visibility = View.VISIBLE
                    btnSelectFile.visibility = View.GONE
                } else {
                    editText.visibility = View.GONE
                    btnSelectFile.visibility = View.VISIBLE
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSelectFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            val format = spinnerFormat.selectedItem.toString()
            intent.type = when (format) {
                "圖片" -> "image/*"
                "音檔" -> "audio/*"
                "影片" -> "video/*"
                else -> "*/*"
            }
            startActivityForResult(intent, PICK_FILE_CODE)
        }

        btnUpload.setOnClickListener {
            val subtype = spinnerSubType.selectedItem.toString()
            val format = spinnerFormat.selectedItem.toString()
            val intro = introText.text.toString()
            val petType = title.toString()

            try {
                if (format == "文字") {
                    val content = editText.text.toString()

                    if (content.isEmpty()) {
                        Toast.makeText(this, "請輸入或選擇資料", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    saveRecord(subtype, format, intro,content)

                    val sharedPref = getSharedPreferences("upload_counts", Context.MODE_PRIVATE)
                    val countKey = "count_$petType"
                    val currentCount = sharedPref.getInt(countKey, 0)
                    sharedPref.edit().putInt(countKey, currentCount + 1).apply()

                    Toast.makeText(this, "餵食成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    selectedUri?.let { uri ->
                        uploadFile(subtype, format,intro, uri, petType)
                    } ?: Toast.makeText(this, "請先選擇檔案", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "發生錯誤: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveRecord(subtype: String, format: String, intro: String,content: String) {
        val sharedPref = getSharedPreferences("upload_records", Context.MODE_PRIVATE)
        val jsonString = sharedPref.getString("records", "[]")
        val jsonArray = JSONArray(jsonString)

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedTime = sdf.format(Date())

        val record = JSONObject()
        record.put("subtype", subtype)
        record.put("format", format)
        record.put("intro", intro)
        record.put("content", content)
        record.put("timestamp", formattedTime)

        jsonArray.put(record)

        sharedPref.edit().putString("records", jsonArray.toString()).apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_FILE_CODE && resultCode == Activity.RESULT_OK) {
            selectedUri = data?.data
            val fileName = selectedUri?.let { uri ->
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        cursor.getString(nameIndex)
                    } else null
                }
            } ?: "未知檔案"

            Toast.makeText(this, "已選擇檔案：$fileName", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadFile(subtype: String, format: String, intro:String, uri: Uri, petType: String) {
        val fileName = getFileName(uri)
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val inputStream = contentResolver.openInputStream(uri) ?: return
        val fileBytes = inputStream.readBytes()

        val requestBody = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, requestBody)
            .build()

        val request = Request.Builder()
            .url("http://192.168.63.223:5000//upload")
            .post(multipartBody)
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@FeedUploadActivity, "上傳失敗: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body?.string()
                val json = JSONObject(jsonString)
                val path = json.getString("url")

                runOnUiThread {
                    Toast.makeText(this@FeedUploadActivity, "餵食成功", Toast.LENGTH_SHORT).show()
                    saveRecord(subtype, format,intro, path)

                    val sharedPref = getSharedPreferences("upload_counts", Context.MODE_PRIVATE)
                    val countKey = "count_$petType"
                    val currentCount = sharedPref.getInt(countKey, 0)
                    sharedPref.edit().putInt(countKey, currentCount + 1).apply()

                    finish()
                }
            }
        })
    }

    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        return cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(index)
        } ?: "uploaded_file"
    }
}
