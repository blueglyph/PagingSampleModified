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

        override fun getRefreshKey(state: PagingState<Int, Cheese>): Int? {
            val lastPos = dao.count() - 1
            val key = state.anchorPosition?.let { anchorPosition ->
                val anchorPage = state.closestPageToPosition(anchorPosition)
                anchorPage?.prevKey?.plus(pageSize)?.coerceAtMost(lastPos) ?: anchorPage?.nextKey?.minus(pageSize)?.coerceAtLeast(0)
            }
            return key
        }

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Cheese> {
            val pageNumber = params.key ?: 0
            val count = dao.count()
            val data = dao.allCheesesOrdName().drop(pageNumber).take(pageSize)
            return LoadResult.Page(
                data = data,
                prevKey = if (pageNumber > 0) max(0, pageNumber - pageSize) else null,
                nextKey = if (pageNumber + pageSize < count) pageNumber + pageSize else null
            )
        }
    }

}