package com.videoflow.app.data

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * 负责从系统媒体库读取视频，以及发起删除请求。
 */
object VideoRepository {

    /** 扫描设备上所有视频文件（按加入时间倒序） */
    fun loadVideos(context: Context): List<VideoItem> {
        val result = mutableListOf<VideoItem>()
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )

        val sortOrder = "${MediaStore.Video.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val name = cursor.getString(nameCol) ?: "未命名"
                val duration = cursor.getLong(durCol)
                val size = cursor.getLong(sizeCol)
                val date = cursor.getLong(dateCol)
                val uri = ContentUris.withAppendedId(collection, id)

                // 跳过大小为 0 的异常记录
                if (size <= 0) continue

                result.add(
                    VideoItem(
                        id = id,
                        uri = uri,
                        name = name,
                        durationMs = duration,
                        sizeBytes = size,
                        dateAdded = date
                    )
                )
            }
        }
        return result
    }

    /**
     * 在 Android 11 (R) 及以上，用系统级删除请求（会弹出系统确认框，安全合规）。
     * 返回一个 PendingIntent，交给界面用 StartIntentSenderForResult 发起。
     */
    fun createDeleteRequest(resolver: ContentResolver, uris: List<Uri>): PendingIntent {
        return MediaStore.createDeleteRequest(resolver, uris)
    }

    /**
     * Android 10 (Q) 的删除：直接删可能抛 RecoverableSecurityException，
     * 需要用其中的 IntentSender 去请求用户授权。
     * 返回 null 表示已直接删除成功，无需再弹窗。
     */
    fun deleteOnQ(context: Context, uri: Uri): android.content.IntentSender? {
        return try {
            context.contentResolver.delete(uri, null, null)
            null
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                e is android.app.RecoverableSecurityException
            ) {
                e.userAction.actionIntent.intentSender
            } else {
                throw e
            }
        }
    }
}
