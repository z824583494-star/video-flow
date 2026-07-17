package com.videoflow.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.videoflow.app.data.DurationBucket
import com.videoflow.app.data.FavoriteStore
import com.videoflow.app.data.FilterMode
import com.videoflow.app.data.SizeBucket
import com.videoflow.app.data.VideoItem
import com.videoflow.app.data.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val loading: Boolean = true,
    val allVideos: List<VideoItem> = emptyList(),
    val filterMode: FilterMode = FilterMode.ALL,
    val durationBucket: DurationBucket? = null,   // null 表示该分类下的“全部”
    val sizeBucket: SizeBucket? = null,
    val favoriteIds: Set<Long> = emptySet()
) {
    /** 根据当前筛选条件得到要播放的列表 */
    val visibleVideos: List<VideoItem>
        get() = when (filterMode) {
            FilterMode.ALL -> allVideos
            FilterMode.FAVORITE -> allVideos.filter { favoriteIds.contains(it.id) }
            FilterMode.DURATION ->
                if (durationBucket == null) allVideos
                else allVideos.filter { it.durationBucket == durationBucket }
            FilterMode.SIZE ->
                if (sizeBucket == null) allVideos
                else allVideos.filter { it.sizeBucket == sizeBucket }
        }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val favoriteStore = FavoriteStore(app)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** 扫描视频库 */
    fun loadVideos() {
        _state.value = _state.value.copy(loading = true)
        viewModelScope.launch {
            val videos = withContext(Dispatchers.IO) {
                VideoRepository.loadVideos(getApplication())
            }
            _state.value = _state.value.copy(
                loading = false,
                allVideos = videos,
                favoriteIds = favoriteStore.getAll()
            )
        }
    }

    fun setFilterMode(mode: FilterMode) {
        _state.value = _state.value.copy(filterMode = mode)
    }

    fun setDurationBucket(bucket: DurationBucket?) {
        _state.value = _state.value.copy(durationBucket = bucket)
    }

    fun setSizeBucket(bucket: SizeBucket?) {
        _state.value = _state.value.copy(sizeBucket = bucket)
    }

    /** 切换收藏，返回切换后的状态 */
    fun toggleFavorite(item: VideoItem): Boolean {
        val nowFav = favoriteStore.toggle(item.id)
        _state.value = _state.value.copy(favoriteIds = favoriteStore.getAll())
        return nowFav
    }

    /** 从内存列表移除已删除的视频（删除成功后调用） */
    fun onVideoDeleted(item: VideoItem) {
        favoriteStore.remove(item.id)
        _state.value = _state.value.copy(
            allVideos = _state.value.allVideos.filter { it.id != item.id },
            favoriteIds = favoriteStore.getAll()
        )
    }
}
