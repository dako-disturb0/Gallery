package com.gallery.app.viewmodel

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
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
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class DateGrouping(val label: String) {
    DAILY("Hari"),
    WEEKLY("Minggu"),
    MONTHLY("Bulan"),
    YEARLY("Tahun")
}

private val ID_LOCALE = Locale("id", "ID")
private val DAY_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", ID_LOCALE)
private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", ID_LOCALE)
private val YEAR_FORMATTER = DateTimeFormatter.ofPattern("yyyy", ID_LOCALE)
private val WEEK_START_FORMATTER = DateTimeFormatter.ofPattern("d MMM", ID_LOCALE)
private val WEEK_END_FORMATTER = DateTimeFormatter.ofPattern("d MMM yyyy", ID_LOCALE)

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
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<MediaItem>> = _mediaItems.map { items ->
        items.filter { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val contentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    init {
        val resolver = application.contentResolver
        resolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, contentObserver
        )
        resolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, contentObserver
        )
    }

    fun loadMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val allMedia = repository.getAllMedia()
            _mediaItems.value = allMedia
            _albums.value = deriveAlbums(allMedia)
            _isLoading.value = false
        }
    }

    private fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val allMedia = repository.getAllMedia()
            _mediaItems.value = allMedia
            _albums.value = deriveAlbums(allMedia)
        }
    }

    override fun onCleared() {
        getApplication<Application>().contentResolver.unregisterContentObserver(contentObserver)
        super.onCleared()
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
        return items.groupBy { item ->
            try {
                val instant = Instant.ofEpochSecond(item.dateAdded)
                val localDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate()
                when (grouping) {
                    DateGrouping.DAILY -> localDate.format(DAY_FORMATTER)
                    DateGrouping.WEEKLY -> {
                        val monday = localDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        val sunday = monday.plusDays(6)
                        "Minggu: ${monday.format(WEEK_START_FORMATTER)} - ${sunday.format(WEEK_END_FORMATTER)}"
                    }
                    DateGrouping.MONTHLY -> localDate.format(MONTH_FORMATTER)
                    DateGrouping.YEARLY -> localDate.format(YEAR_FORMATTER)
                }
            } catch (e: Exception) {
                "Lainnya"
            }
        }.toList()
    }
}
