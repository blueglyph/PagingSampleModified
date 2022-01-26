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

import android.util.ArrayMap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * Database Access Object for the Cheese database.
 */
class CheeseDaoLocal {
    private val _data = ArrayMap<Int, Cheese>()
    val data = MutableLiveData <Map<Int, Cheese>>(_data)
    val sortedData = data.map { data -> data.values.sortedBy { it.name.lowercase() } }

    suspend fun allCheesesOrdName(): List<Cheese> {
        return sortedData.value ?: listOf()
    }

    suspend fun insert(cheeses: List<Cheese>) {
        for (item in cheeses)
            _data[item.id] = item
        data.postValue(_data)
    }

    suspend fun insert(cheese: Cheese) {
        _data[cheese.id] = cheese
        data.postValue(_data)
    }

    suspend fun delete(cheese: Cheese) {
        _data.remove(cheese.id)
        data.postValue(_data)
    }

    fun findPos(cheese: Cheese): Int {
        val name = cheese.name.lowercase()
        val pos = _data.values.count { it.name.lowercase() < name }
        return pos
    }

    fun count() = _data.count()

    fun getDataSource(pageSize: Int): PagingSource<Int, Cheese> {
        val source = CheeseDataSource(this, pageSize)
        return source
    }

    private class CheeseDataSource(val dao: CheeseDaoLocal, val pageSize: Int): PagingSource<Int, Cheese>() {
        fun max(a: Int, b: Int): Int = if (a > b) a else b
        fun min(a: Int, b: Int): Int = if (a < b) a else b
        var count = dao.count()
        val UNDEF = -2147483648

        override fun getRefreshKey(state: PagingState<Int, Cheese>): Int? {
            val key = state.anchorPosition
            Log.d("CHEESE_SRC", "getRefreshKey(state{anchorPosition=${state.anchorPosition}}) -> $key")
            return key
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Cheese> {
            data class Args(
                var start: Int = 0, var size: Int = 0, var prevKey: Int? = null, var nextKey: Int? = null,
                var itemsBefore: Int = UNDEF, var itemsAfter: Int = UNDEF
            )
            val pos = params.key ?: 0
            val args = Args()
            when (params) {
                is LoadParams.Append -> {
                    args.start = pos
                    args.prevKey = params.key
                    //args.nextKey = if (args.start < count) min(args.start + params.loadSize, count) else null
                    args.nextKey = if (args.start + params.loadSize < count) args.start + params.loadSize else null
                }
                is LoadParams.Prepend -> {
                    args.start = max(pos - pageSize, 0)
                    args.prevKey = if (args.start > 0) args.start else null
                    args.nextKey = params.key
                }
                is LoadParams.Refresh -> {
                    args.start = max((pos - params.loadSize/2)/pageSize*pageSize, 0)
                    args.prevKey = if (args.start > 0) args.start else null
                    args.nextKey = if (args.start + params.loadSize < count) min(args.start + params.loadSize, count - 1) else null
                }
            }
            args.size = min(params.loadSize, count - args.start)
            if (params is LoadParams.Refresh) {
                args.itemsBefore = args.start
                args.itemsAfter = count - args.size - args.start
            }
            val source = dao.allCheesesOrdName()
            val data = source.drop(args.start).take(args.size)
            if (params.key == null && data.count() == 0) {
                Log.d("CHEESE_SRC", "load -> Error (no data) source: ${source.count()} item(s)")
                return LoadResult.Error(Exception("Don't care"))
            }
            val result = LoadResult.Page(
                data = data,
                prevKey = args.prevKey,
                nextKey = args.nextKey,
                itemsBefore = args.itemsBefore,
                itemsAfter = args.itemsAfter
            )
            val first = data.firstOrNull()?.id
            val p = params::class.simpleName?.first()
            Log.d("CHEESE_SRC", "load($p key=${params.key}, loadSize=${params.loadSize}) -> $args, data=$first (${data.count()})")
            return result
        }
    }

}