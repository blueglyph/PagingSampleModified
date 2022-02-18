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

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.paging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * A simple [AndroidViewModel] that provides a [Flow]<[PagingData]> of delicious cheeses.
 */
class CheeseViewModel(private val dao: CheeseDaoLocal) : ViewModel() {

    val pageSize = 30
    var dataSource: PagingSource<Int, Cheese>? = null

    val allCheeses: Flow<PagingData<CheeseListItem>> = Pager(
        config = PagingConfig(
            pageSize = pageSize,
            enablePlaceholders = false,
            maxSize = 90,
            jumpThreshold = 30
        )
    ) {
        dataSource = dao.getDataSource(pageSize)
        dataSource!!
    }.flow
        .map { pagingData ->
            pagingData
                // Map cheeses to common UI model.
                .map { cheese -> CheeseListItem.Item(cheese) }
        }
        //.cachedIn(viewModelScope)

    init {
        dao.sortedData.observeForever {
            dataSource?.invalidate()
        }
    }

    fun insert(text: CharSequence) {
        MainScope().launch(Dispatchers.IO) {
            dao.insert(Cheese(name = text.toString()))
//            dataSource.invalidate()
        }
    }

    fun remove(cheese: Cheese) {
        MainScope().launch(Dispatchers.IO) {
            dao.delete(cheese)
//            dataSource.invalidate()
        }
    }
}
