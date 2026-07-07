package com.gallery.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.app.data.Album
import com.gallery.app.data.MediaItem
import com.gallery.app.data.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DateGrouping(val label: String) {
    DAILY("Hari"),
    WEEKLY("Minggu"),
    MONTHLY("Bulan"),
    YEARLY("Tahun")
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MediaRepository(application)

    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaItems: StateFlow<List<MediaItem>> = _mediaItems.asStateFlow()

    private val _dateGrouping = MutableStateFlow(DateGrouping.DAILY)
    val dateGrouping: StateFlow<DateGrouping> = _dateGrouping.asStateFlow()

    val groupedMediaItems: StateFlow<List<Pair<String, List<MediaItem>>>> = combine(
        _mediaItems, _dateGrouping
    ) { items, grouping ->
        groupMediaItems(items, grouping)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<MediaItem>> = combine(
        _mediaItems, _searchQuery
    ) { items, query ->
        if (query.isBlank()) emptyList()
        else items.filter { it.displayName.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<MediaItem>> = _mediaItems.map { items ->
        items.filter { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val allMedia = repository.getAllMedia()
            _mediaItems.value = allMedia
            _albums.value = deriveAlbums(allMedia)
            _isLoading.value = false
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun deriveAlbums(items: List<MediaItem>): List<Album> {
        return items
            .filter { it.bucketId != null }
            .groupBy { it.bucketId }
            .map { (bucketId, mediaItems) ->
                Album(
                    id = bucketId!!,
                    name = mediaItems.first().bucketName ?: "Unknown",
                    coverUri = mediaItems.first().uri,
                    itemCount = mediaItems.size,
                )
            }
            .sortedByDescending { it.itemCount }
    }

    fun setDateGrouping(grouping: DateGrouping) {
        _dateGrouping.value = grouping
    }

    private fun groupMediaItems(items: List<MediaItem>, grouping: DateGrouping): List<Pair<String, List<MediaItem>>> {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("id", "ID"))
        val monthFormatter = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale("id", "ID"))
        val yearFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy", java.util.Locale("id", "ID"))
        
        return items.groupBy { item ->
            try {
                val instant = java.time.Instant.ofEpochSecond(item.dateAdded)
                val localDate = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()).toLocalDate()
                when (grouping) {
                    DateGrouping.DAILY -> localDate.format(formatter)
                    DateGrouping.WEEKLY -> {
                        val monday = localDate.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                        val sunday = monday.plusDays(6)
                        "Minggu: ${monday.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", java.util.Locale("id", "ID")))} - ${sunday.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale("id", "ID")))}"
                    }
                    DateGrouping.MONTHLY -> localDate.format(monthFormatter)
                    DateGrouping.YEARLY -> localDate.format(yearFormatter)
                }
            } catch (e: Exception) {
                "Lainnya"
            }
        }.toList()
    }
}
