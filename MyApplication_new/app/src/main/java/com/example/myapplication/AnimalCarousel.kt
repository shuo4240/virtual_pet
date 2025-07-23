package com.example.myapplication

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import kotlin.math.abs

class AnimalCarousel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var viewPager: ViewPager2
    private var adapter: AnimalCarouselAdapter
    private var imagePaths: List<String> = emptyList()
    private var isInitialized = false
    private val prefs = context.getSharedPreferences("carousel_prefs", Context.MODE_PRIVATE)

    var onPageChanged: ((Int) -> Unit)? = null

    init {
        viewPager = ViewPager2(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            offscreenPageLimit = 1
            clipToPadding = false
            clipChildren = false
            val padding = dpToPx(60)
            setPadding(padding, 0, padding, 0)
        }

        adapter = AnimalCarouselAdapter(imagePaths)
        viewPager.adapter = adapter
        viewPager.setPageTransformer(CarouselPageTransformer())
        addView(viewPager)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (imagePaths.isNotEmpty()) {
                    val actualIndex = position % imagePaths.size
                    if (isInitialized) {
                        prefs.edit().putInt("last_index", actualIndex).apply()
                        onPageChanged?.invoke(actualIndex)
                    }
                }
            }
        })
    }

    fun setDrawableResources(drawableIds: List<Int>) {
        imagePaths = drawableIds.map { "drawable_res:$it" }
        adapter.images = imagePaths
        adapter.notifyDataSetChanged()

        post {
            if (imagePaths.isEmpty()) return@post
            val savedIndex = prefs.getInt("last_index", 0).coerceIn(0, imagePaths.size - 1)
            val startPosition = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % imagePaths.size + savedIndex
            viewPager.setCurrentItem(startPosition, false)
            isInitialized = true
        }
    }

    fun setCurrentIndex(index: Int) {
        if (imagePaths.isEmpty()) return
        val target = Int.MAX_VALUE / 2 - (Int.MAX_VALUE / 2) % imagePaths.size + index
        post {
            viewPager.setCurrentItem(target, false)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

class CarouselPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val enlargeFactor = 0.3f
        val scaleFactor = 1.0f - enlargeFactor * abs(position)
        page.scaleY = scaleFactor
        page.scaleX = scaleFactor
        page.elevation = (1.0f - abs(position)) * 12f
        page.alpha = 0.6f + (1.0f - abs(position)) * 0.4f
    }
}

class AnimalCarouselAdapter(
    var images: List<String>
) : RecyclerView.Adapter<AnimalCarouselAdapter.CarouselViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselViewHolder {
        val cardView = CardView(parent.context).apply {
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                setMargins(16, 16, 16, 16)
            }
            radius = 15f * parent.context.resources.displayMetrics.density
            cardElevation = 8f * parent.context.resources.displayMetrics.density
            useCompatPadding = true
            setCardBackgroundColor(Color.TRANSPARENT)
        }

        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        cardView.addView(imageView)
        return CarouselViewHolder(cardView, imageView)
    }

    override fun onBindViewHolder(holder: CarouselViewHolder, position: Int) {
        if (images.isNotEmpty()) {
            val actualPosition = position % images.size
            holder.bind(images[actualPosition])
        }
    }

    override fun getItemCount(): Int {
        return if (images.isEmpty()) 0 else Int.MAX_VALUE
    }

    class CarouselViewHolder(
        itemView: View,
        private val imageView: ImageView
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(imagePath: String) {
            try {
                when {
                    imagePath.startsWith("drawable_res:") -> {
                        val resId = imagePath.removePrefix("drawable_res:").toInt()
                        imageView.setImageResource(resId)
                    }
                    imagePath.startsWith("assets/") || imagePath.startsWith("animal/") -> {
                        val assetPath = imagePath.removePrefix("assets/")
                        val inputStream = itemView.context.assets.open(assetPath)
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        imageView.setImageBitmap(bitmap)
                        inputStream.close()
                    }
                    else -> {
                        val file = File(imagePath)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(imagePath)
                            imageView.setImageBitmap(bitmap)
                        } else {
                            imageView.setImageResource(android.R.drawable.ic_menu_gallery)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }
    }
}