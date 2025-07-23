package com.example.myapplication.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.AnimalCarousel
import com.example.myapplication.ImageService
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.feedupload.FeedUploadActivity
import com.example.myapplication.floating.FloatingService

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var imageService: ImageService
    private var currentImageIndex = 0
    private var totalImages = 0

    private val titles = listOf("書摘", "食譜", "電影", "景點")
    private val animalPrefixes = listOf("book", "recipe", "movie", "landmark")

    private val subtypeMap = mapOf(
        "書摘" to listOf("文學", "自然", "歷史", "藝術"),
        "景點" to listOf("人文景", "自然景", "餐廳"),
        "食譜" to listOf("中式", "西式", "東南亞"),
        "電影" to listOf("喜劇", "悲劇", "恐怖片", "愛情片")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        imageService = ImageService(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val animalCarousel = view.findViewById<AnimalCarousel>(R.id.animal_carousel)
        val prefs = requireContext().getSharedPreferences("carousel_prefs", Context.MODE_PRIVATE)

        homeViewModel.subtypeCounts.observe(viewLifecycleOwner) { counts ->
            val imageResIds = titles.mapIndexedNotNull { index, petType ->
                val prefix = animalPrefixes.getOrNull(index) ?: return@mapIndexedNotNull null
                val subtypes = subtypeMap[petType] ?: return@mapIndexedNotNull null

                val subtypeCounts = subtypes.associateWith { counts[it] ?: 0 }

                val evolveDrawableName = when {
                    subtypeCounts.count { it.value >= 4 } >= 2 -> {
                        val topTwo = subtypeCounts.entries
                            .sortedByDescending { it.value }
                            .take(2)
                            .map { convertSubtypeToCode(it.key) }
                        "${prefix}_${topTwo[0]}_${topTwo[1]}"
                    }
                    subtypeCounts.any { it.value >= 2 } -> {
                        val topOne = subtypeCounts.maxByOrNull { it.value }?.key ?: return@mapIndexedNotNull null
                        "${prefix}_${convertSubtypeToCode(topOne)}"
                    }
                    else -> "${prefix}0"
                }

                val resId = resources.getIdentifier(evolveDrawableName, "drawable", requireContext().packageName)
                if (resId != 0) resId else null
            }

            totalImages = imageResIds.size
            if (totalImages == 0) return@observe

            val savedIndex = prefs.getInt("last_index", 0).coerceIn(0, totalImages - 1)
            currentImageIndex = savedIndex

            // 設定圖片資料來源與起始位置
            animalCarousel.setDrawableResources(imageResIds)

            animalCarousel.onPageChanged = { position ->
                currentImageIndex = position % totalImages
                val newTitle = titles.getOrNull(currentImageIndex) ?: "書摘"
                requireActivity().title = newTitle

                prefs.edit().putInt("last_index", currentImageIndex).apply()

                FloatingService.updateImage(requireContext(), newTitle)
            }

            // 延遲設定初始 index，確保 view ready
            animalCarousel.post {
                animalCarousel.setCurrentIndex(currentImageIndex)
                requireActivity().title = titles.getOrNull(currentImageIndex) ?: "書摘"

                titles.getOrNull(currentImageIndex)
                    ?.let { FloatingService.updateImage(requireContext(), it) }
            }
        }

        binding.btnFeed.setOnClickListener {
            val intent = Intent(requireContext(), FeedUploadActivity::class.java)
            intent.putExtra("pet_type", requireActivity().title.toString())
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        homeViewModel.refreshCounts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun convertSubtypeToCode(subtype: String): String {
        return when (subtype) {
            "文學" -> "lit"
            "自然" -> "nat"
            "歷史" -> "hist"
            "藝術" -> "art"
            "人文景" -> "cult"
            "自然景" -> "land"
            "餐廳" -> "food"
            "中式" -> "chi"
            "西式" -> "wes"
            "東南亞" -> "sea"
            "喜劇" -> "com"
            "悲劇" -> "trag"
            "恐怖片" -> "horr"
            "愛情片" -> "rom"
            else -> "unk"
        }
    }

}
