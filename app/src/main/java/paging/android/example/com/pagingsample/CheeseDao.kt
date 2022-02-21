/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package paging.android.example.com.pagingsample

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Database Access Object mock code for  a locally-generated data source.
 * In reality, [_data] and [data] are never entirely loaded into memory.
 */
class CheeseDaoLocal(initialData: List<String>) {
    val MAX_SIZE = 500
    companion object {
        val UNDEF = -2147483648
    }
    private val data = initialData.take(MAX_SIZE).map {
            val c = Cheese(name = it)
            Pair(c.id, c)
        }.toMap().toMutableMap()
    private var dirtySortedData = true
    private val _sortedData = ArrayList<Cheese>()
    private val sortedData: List<Cheese>
        get() {
            if (dirtySortedData) {
                _sortedData.clear()
                _sortedData.addAll(data.values.sortedBy { it.name.lowercase() })
                dirtySortedData = false
            }
            return _sortedData
        }

    init {
        Log.d("CHEESE", "DAO contains ${data.count()} item(s)")
    }

    suspend fun sliceCheeseOrdName(offset: Int, size: Int): List<Cheese> =
        sortedData.drop(offset).take(size)

    suspend fun insert(cheeses: List<Cheese>) {
        dirtySortedData = true
        for (item in cheeses)
            data[item.id] = item
    }

    suspend fun insert(cheese: Cheese) {
        dirtySortedData = true
        data[cheese.id] = cheese
    }

    suspend fun delete(cheese: Cheese) {
        dirtySortedData = true
        data.remove(cheese.id)
    }

    fun count() = data.count()

    fun getDataSource(pageSize: Int): PagingSource<Int, Cheese> {
        val source = CheeseDataSource(this, pageSize)
        return source
    }

    private class CheeseDataSource(
        val dao: CheeseDaoLocal,
        val pageSize: Int
    ): PagingSource<Int, Cheese>() {

        init {
            Log.d("CHEESE", "CheeseDataSource.init")
        }
        override val jumpingSupported: Boolean
            get() = true

        override fun getRefreshKey(state: PagingState<Int, Cheese>): Int? {
            val position = state.anchorPosition ?: 0
            val key = maxOf(0, position - state.config.initialLoadSize / 2)
            Log.d("CHEESE_SRC", "getRefreshKey(state{anchorPosition=${state.anchorPosition}}) -> $key")
            return key
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Cheese> {
            val position = params.key ?: 0
            // Condition: dao content does not vary between these two lines:
            val count = dao.count()
            val data = dao.sliceCheeseOrdName(position, params.loadSize)
            val isRefresh = params is LoadParams.Refresh
            return LoadResult.Page(
                data = data,
                prevKey = if (position > 0) maxOf(0, position - pageSize) else null,
                nextKey = if (position + data.size < count) position + data.size else null,
                itemsBefore = if (isRefresh) position else UNDEF,
                itemsAfter = if (isRefresh) maxOf(0, count - position - data.size) else UNDEF,
            ).also {
                val first = data.firstOrNull()?.id
                val p = params::class.simpleName?.first()
                Log.d("CHEESE_SRC", "load($p key=${params.key}, loadSize=${params.loadSize}) -> ${it.prevKey} / ${it.nextKey}, ${it.itemsBefore} / ${it.itemsAfter}, data=$first (${data.count()})")
            }
        }
    }

}