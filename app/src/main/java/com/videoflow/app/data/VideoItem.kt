package com.videoflow.app.data

import android.net.Uri

/**
 * 一个视频文件的信息。
 * @param id       MediaStore 中的唯一 id
 * @param uri      可用于播放/删除的内容 Uri
 * @param name     文件名
 * @param durationMs 时长（毫秒）
 * @param sizeBytes  文件大小（字节）
 * @param dateAdded  加入时间（秒）
 */
data class VideoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAdded: Long
) {
    /** 时长所属的分组 */
    val durationBucket: DurationBucket
        get() = DurationBucket.of(durationMs)

    /** 大小所属的分组 */
    val sizeBucket: SizeBucket
        get() = SizeBucket.of(sizeBytes)

    /** 友好的时长文本，如 03:25 */
    val durationText: String
        get() {
            val totalSec = durationMs / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
        }

    /** 友好的大小文本，如 128 MB */
    val sizeText: String
        get() {
            val mb = sizeBytes / 1024.0 / 1024.0
            return if (mb >= 1024) "%.1f GB".format(mb / 1024) else "%.1f MB".format(mb)
        }
}

/** 按时长分组 */
enum class DurationBucket(val label: String) {
    UNDER_1M("1分钟内"),
    M1_5("1-5分钟"),
    M5_30("5-30分钟"),
    OVER_30M("30分钟以上");

    companion object {
        fun of(durationMs: Long): DurationBucket {
            val min = durationMs / 1000.0 / 60.0
            return when {
                min < 1 -> UNDER_1M
                min < 5 -> M1_5
                min < 30 -> M5_30
                else -> OVER_30M
            }
        }
    }
}

/** 按大小分组 */
enum class SizeBucket(val label: String) {
    UNDER_10M("10MB内"),
    MB10_50("10-50MB"),
    MB50_200("50-200MB"),
    OVER_200M("200MB以上");

    companion object {
        fun of(sizeBytes: Long): SizeBucket {
            val mb = sizeBytes / 1024.0 / 1024.0
            return when {
                mb < 10 -> UNDER_10M
                mb < 50 -> MB10_50
                mb < 200 -> MB50_200
                else -> OVER_200M
            }
        }
    }
}

/** 顶部主分类模式 */
enum class FilterMode(val label: String) {
    ALL("全部"),
    DURATION("按时长"),
    SIZE("按大小"),
    FAVORITE("收藏")
}
