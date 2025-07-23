package com.example.myapplication.feedupload

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivityViewContentBinding

class ViewContentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewContentBinding
    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewContentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val fileUrl = intent.getStringExtra("file_url")
        val format = intent.getStringExtra("format") ?: ""

        /*測試階段使用固定 URL 來排除資料問題
        val fileUrl = when (format) {
            "影片" -> "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
            "圖片" -> "https://upload.wikimedia.org/wikipedia/commons/3/3a/Cat03.jpg"
            "音檔" -> "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"
            else -> fileUrlRaw
        }*/

        if (fileUrl.isNullOrBlank()) {
            Toast.makeText(this, "無效的連結", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        when (format) {
            "影片" -> {
                binding.videoView.visibility = View.VISIBLE
                binding.imageView.visibility = View.GONE
                binding.audioText.visibility = View.GONE
                binding.videoContainer.visibility = View.VISIBLE
                binding.videoView.setVideoURI(Uri.parse(fileUrl))
                binding.videoView.setMediaController(MediaController(this))
                binding.videoView.requestFocus()
                binding.videoView.setOnErrorListener { _, what, extra ->
                    Toast.makeText(this, "影片播放失敗 (code: $what)", Toast.LENGTH_LONG).show()
                    true
                }
                binding.videoView.start()
            }
            "圖片" -> {
                binding.imageView.visibility = View.VISIBLE
                binding.videoView.visibility = View.GONE
                binding.audioText.visibility = View.GONE
                Glide.with(this).load(fileUrl).into(binding.imageView)
            }
            "音檔" -> {
                binding.videoView.visibility = View.GONE
                binding.imageView.visibility = View.GONE
                binding.audioText.visibility = View.GONE
                binding.audioControls.visibility = View.VISIBLE
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(fileUrl)
                    prepare()
                    start()

                    setOnPreparedListener {
                        binding.audioSeekBar.max = duration
                        updateSeekBar()
                    }
                }

                binding.btnPauseResume.setOnClickListener {
                    mediaPlayer?.let {
                        if (it.isPlaying) {
                            it.pause()
                            isPaused = true
                            binding.btnPauseResume.text = "繼續"
                        } else {
                            it.start()
                            isPaused = false
                            binding.btnPauseResume.text = "暫停"
                        }
                    }
                }
                binding.audioSeekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            mediaPlayer?.seekTo(progress)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
                })
            }

            "文字" -> {
                binding.videoView.visibility = View.GONE
                binding.imageView.visibility = View.GONE
                binding.audioText.visibility = View.VISIBLE
                binding.audioText.text = fileUrl
            }
            else -> {
                Toast.makeText(this, "不支援的格式", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    private fun updateSeekBar() {
        mediaPlayer?.let {
            binding.audioSeekBar.progress = it.currentPosition
            if (it.isPlaying) {
                handler.postDelayed({ updateSeekBar() }, 500)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
