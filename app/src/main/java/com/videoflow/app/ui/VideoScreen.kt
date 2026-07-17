package com.videoflow.app.ui

import android.app.Activity
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.videoflow.app.MainViewModel
import com.videoflow.app.data.DurationBucket
import com.videoflow.app.data.FilterMode
import com.videoflow.app.data.SizeBucket
import com.videoflow.app.data.VideoItem
import com.videoflow.app.data.VideoRepository
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // 全局唯一播放器，循环播放当前视频
    // 启用 FFmpeg 软解扩展：EXTENSION_RENDERER_MODE_ON 表示只有当设备自带的解码器不支持时才回退到 FFmpeg 软解，
    // 这样主流格式仍走硬件解码（省电流畅），冷门格式也能放。
    val player = remember {
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
            }
    }
    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    // 删除流程：系统确认弹窗
    var pendingDelete by remember { mutableStateOf<VideoItem?>(null) }
    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val item = pendingDelete
        if (result.resultCode == Activity.RESULT_OK && item != null) {
            viewModel.onVideoDeleted(item)
            Toast.makeText(context, "已删除：${item.name}", Toast.LENGTH_SHORT).show()
        }
        pendingDelete = null
    }

    fun requestDelete(item: VideoItem) {
        pendingDelete = item
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pi = VideoRepository.createDeleteRequest(
                    context.contentResolver, listOf(item.uri)
                )
                deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            } else {
                val sender = VideoRepository.deleteOnQ(context, item.uri)
                if (sender == null) {
                    viewModel.onVideoDeleted(item)
                    pendingDelete = null
                    Toast.makeText(context, "已删除：${item.name}", Toast.LENGTH_SHORT).show()
                } else {
                    deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
            }
        } catch (e: Exception) {
            pendingDelete = null
            Toast.makeText(context, "删除失败：${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val videos = state.visibleVideos

        when {
            state.loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            }

            videos.isEmpty() -> {
                Text(
                    text = "这里没有视频\n换个分类试试吧",
                    color = Color.White,
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            else -> {
                val pagerState = rememberPagerState(pageCount = { videos.size })

                // 切换到某个视频时加载并播放
                LaunchedEffect(pagerState.currentPage, videos) {
                    val idx = pagerState.currentPage
                    if (idx in videos.indices) {
                        val item = videos[idx]
                        player.setMediaItem(MediaItem.fromUri(item.uri))
                        player.prepare()
                        player.play()
                    }
                }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val item = videos[page]
                    VideoPage(
                        item = item,
                        isCurrent = page == pagerState.currentPage,
                        isFavorite = state.favoriteIds.contains(item.id),
                        player = player,
                        onDelete = { requestDelete(item) },
                        onFavorite = {
                            val nowFav = viewModel.toggleFavorite(item)
                            Toast.makeText(
                                context,
                                if (nowFav) "已收藏" else "已取消收藏",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }

        // 顶部分类筛选栏（悬浮在画面上方）
        FilterBar(
            state = state,
            onModeChange = viewModel::setFilterMode,
            onDurationChange = viewModel::setDurationBucket,
            onSizeChange = viewModel::setSizeBucket,
            modifier = Modifier
                .align(Alignment.TopCenter)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoPage(
    item: VideoItem,
    isCurrent: Boolean,
    isFavorite: Boolean,
    player: ExoPlayer,
    onDelete: () -> Unit,
    onFavorite: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember(item.id) { Animatable(0f) }

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val thresholdPx = with(density) { (configuration.screenWidthDp.dp * 0.28f).toPx() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 左右滑动的手势层 + 内容随手指偏移
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(item.id) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                        },
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > thresholdPx -> {
                                        onDelete()          // 右滑：删除
                                        offsetX.animateTo(0f)
                                    }
                                    offsetX.value < -thresholdPx -> {
                                        onFavorite()        // 左滑：收藏
                                        offsetX.animateTo(0f)
                                    }
                                    else -> offsetX.animateTo(0f)
                                }
                            }
                        }
                    )
                }
        ) {
            if (isCurrent) {
                VideoPlayerView(player = player, modifier = Modifier.fillMaxSize())
            }

            // 底部信息条
            VideoInfoBar(
                item = item,
                isFavorite = isFavorite,
                modifier = Modifier
                    .align(Alignment.BottomStart)
            )
        }

        // 右滑提示（删除，红色，出现在左侧）
        AnimatedVisibility(
            visible = offsetX.value > 40f,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            SwipeHint(icon = Icons.Filled.Delete, text = "删除", tint = Color(0xFFFF3B5C))
        }

        // 左滑提示（收藏，金色，出现在右侧）
        AnimatedVisibility(
            visible = offsetX.value < -40f,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            SwipeHint(icon = Icons.Filled.Star, text = "收藏", tint = Color(0xFFFFC107))
        }
    }
}

@Composable
private fun SwipeHint(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(24.dp)
            .background(Color(0x88000000), shape = androidx.compose.foundation.shape.CircleShape)
            .padding(16.dp)
    ) {
        Icon(icon, contentDescription = text, tint = tint, modifier = Modifier.height(40.dp))
        Text(text, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun VideoInfoBar(
    item: VideoItem,
    isFavorite: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x66000000))
            .padding(16.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isFavorite) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = "已收藏",
                    tint = Color(0xFFFF3B5C),
                    modifier = Modifier.height(18.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = item.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "时长 ${item.durationText}   ·   大小 ${item.sizeText}",
            color = Color(0xFFDDDDDD),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "右滑删除  ·  左滑收藏  ·  上滑下一个",
            color = Color(0x99FFFFFF),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun FilterBar(
    state: com.videoflow.app.UiState,
    onModeChange: (FilterMode) -> Unit,
    onDurationChange: (DurationBucket?) -> Unit,
    onSizeChange: (SizeBucket?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x55000000))
            .statusBarsPadding()
            .padding(vertical = 8.dp)
    ) {
        // 一级：模式
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(FilterMode.entries) { mode ->
                Chip(
                    text = mode.label,
                    selected = state.filterMode == mode,
                    onClick = { onModeChange(mode) }
                )
            }
        }

        // 二级：具体区间
        if (state.filterMode == FilterMode.DURATION) {
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Chip("全部", state.durationBucket == null) { onDurationChange(null) }
                }
                items(DurationBucket.entries) { b ->
                    Chip(b.label, state.durationBucket == b) { onDurationChange(b) }
                }
            }
        } else if (state.filterMode == FilterMode.SIZE) {
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Chip("全部", state.sizeBucket == null) { onSizeChange(null) }
                }
                items(SizeBucket.entries) { b ->
                    Chip(b.label, state.sizeBucket == b) { onSizeChange(b) }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontSize = 13.sp) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color(0x66000000),
            labelColor = Color.White,
            selectedContainerColor = Color(0xFFFF3B5C),
            selectedLabelColor = Color.White
        )
    )
}
