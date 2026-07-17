package com.videoflow.app.data

import android.content.Context

/**
 * 用 SharedPreferences 保存收藏的视频 id 集合。
 */
class FavoriteStore(context: Context) {
    private val prefs = context.getSharedPreferences("favorites", Context.MODE_PRIVATE)
    private val key = "favorite_ids"

    fun getAll(): Set<Long> {
        return prefs.getStringSet(key, emptySet())!!
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    fun toggle(id: Long): Boolean {
        val current = getAll().toMutableSet()
        val nowFavorite: Boolean
        if (current.contains(id)) {
            current.remove(id)
            nowFavorite = false
        } else {
            current.add(id)
            nowFavorite = true
        }
        save(current)
        return nowFavorite
    }

    fun remove(id: Long) {
        val current = getAll().toMutableSet()
        if (current.remove(id)) save(current)
    }

    private fun save(ids: Set<Long>) {
        prefs.edit().putStringSet(key, ids.map { it.toString() }.toSet()).apply()
    }
}
