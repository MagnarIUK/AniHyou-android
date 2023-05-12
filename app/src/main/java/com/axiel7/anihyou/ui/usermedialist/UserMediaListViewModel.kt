package com.axiel7.anihyou.ui.usermedialist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.apollographql.apollo3.api.Optional
import com.axiel7.anihyou.UserMediaListQuery
import com.axiel7.anihyou.data.repository.LoginRepository
import com.axiel7.anihyou.type.MediaListSort
import com.axiel7.anihyou.type.MediaListStatus
import com.axiel7.anihyou.type.MediaType
import com.axiel7.anihyou.ui.base.BaseViewModel

class UserMediaListViewModel(
    private val mediaType: MediaType,
    private val status: MediaListStatus
) : BaseViewModel() {

    var page = 1
    var hasNextPage = true
    var mediaList = mutableStateListOf<UserMediaListQuery.MediaList>()

    var sort by mutableStateOf(MediaListSort.UPDATED_TIME_DESC)

    suspend fun getUserList() {
        val userId = LoginRepository.getUserId()
        val response = UserMediaListQuery(
            page = Optional.present(page),
            perPage = Optional.present(15),
            userId = Optional.present(userId),
            type = Optional.present(mediaType),
            status = Optional.present(status),
            sort = Optional.present(listOf(sort))
        ).tryQuery()

        response?.data?.Page?.mediaList?.filterNotNull()?.let { mediaList.addAll(it) }
        page += 1
        hasNextPage = response?.data?.Page?.pageInfo?.hasNextPage ?: false
    }

    suspend fun refreshList() {
        page = 1
        hasNextPage = true
        mediaList.clear()
        getUserList()
    }
}