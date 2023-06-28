package com.axiel7.anihyou.ui.screens.explore.search

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.axiel7.anihyou.SearchCharacterQuery
import com.axiel7.anihyou.SearchMediaQuery
import com.axiel7.anihyou.SearchStaffQuery
import com.axiel7.anihyou.SearchStudioQuery
import com.axiel7.anihyou.SearchUserQuery
import com.axiel7.anihyou.data.model.SearchType
import com.axiel7.anihyou.data.repository.DataResult
import com.axiel7.anihyou.data.repository.PagedResult
import com.axiel7.anihyou.data.repository.SearchRepository
import com.axiel7.anihyou.type.MediaSort
import com.axiel7.anihyou.type.MediaType
import com.axiel7.anihyou.ui.base.BaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchViewModel(
    initialMediaType: MediaType?,
    initialMediaSort: MediaSort?,
    initialGenre: String?,
    initialTag: String?,
) : BaseViewModel() {

    private val perPage = 25

    var searchType by mutableStateOf(
        if (initialMediaType == MediaType.MANGA) SearchType.MANGA else SearchType.ANIME
    )
        private set
    fun onSearchTypeChanged(value: SearchType) {
        searchType = value
    }

    var mediaSort by mutableStateOf(initialMediaSort ?: MediaSort.SEARCH_MATCH)
        private set
    fun onMediaSortChanged(value: MediaSort) {
        mediaSort = value
    }

    val genreCollection = if (initialGenre != null) mutableStateMapOf(initialGenre to true)
    else mutableStateMapOf()

    val tagCollection = if (initialTag != null) mutableStateMapOf(initialTag to true)
    else mutableStateMapOf()

    val selectedGenres by derivedStateOf { genreCollection.filter { it.value } }
    val selectedTags by derivedStateOf { tagCollection.filter { it.value } }

    suspend fun runSearch(query: String) {
        viewModelScope.launch {
            when (searchType) {
                SearchType.ANIME -> searchMedia(MediaType.ANIME, query, resetPage = true)
                SearchType.MANGA -> searchMedia(MediaType.MANGA, query, resetPage = true)
                SearchType.CHARACTER -> searchCharacter(query)
                SearchType.STAFF -> searchStaff(query)
                SearchType.STUDIO -> searchStudio(query)
                SearchType.USER -> searchUser(query)
            }
        }
    }

    private var pageMedia = 1
    private var hasNextPageMedia = true
    val searchedMedia = mutableStateListOf<SearchMediaQuery.Medium>()

    suspend fun searchMedia(
        mediaType: MediaType,
        query: String,
        resetPage: Boolean
    ) {
        if (resetPage) pageMedia = 1

        val selectedGenres = genreCollection.filterValues { it }.keys.toList()
        val selectedTags = tagCollection.filterValues { it }.keys.toList()

        if (selectedGenres.isNotEmpty() || selectedTags.isNotEmpty()) {
            if (mediaSort == MediaSort.SEARCH_MATCH) mediaSort = MediaSort.POPULARITY_DESC
        }

        SearchRepository.searchMedia(
            mediaType = mediaType,
            query = query,
            sort = listOf(mediaSort),
            genreIn = selectedGenres,
            tagIn = selectedTags,
            page = pageMedia,
            perPage = perPage,
        ).collect { result ->
            isLoading = pageMedia == 1 && result is PagedResult.Loading

            if (result is PagedResult.Success) {
                if (resetPage) searchedMedia.clear()
                searchedMedia.addAll(result.data)
                hasNextPageMedia = result.nextPage != null
                pageMedia = result.nextPage ?: pageMedia
            }
            else if (result is PagedResult.Error) {
                message = result.message
            }
        }
    }

    val searchedCharacters = mutableStateListOf<SearchCharacterQuery.Character>()

    private suspend fun searchCharacter(query: String) {
        SearchRepository.searchCharacter(query = query).collect { result ->
            isLoading = result is PagedResult.Loading

            if (result is PagedResult.Success) {
                searchedCharacters.clear()
                searchedCharacters.addAll(result.data)
            }
            else if (result is PagedResult.Error) {
                message = result.message
            }
        }
    }

    val searchedStaff = mutableStateListOf<SearchStaffQuery.Staff>()

    private suspend fun searchStaff(query: String) {
        SearchRepository.searchStaff(query = query).collect { result ->
            isLoading = result is PagedResult.Loading

            if (result is PagedResult.Success) {
                searchedStaff.clear()
                searchedStaff.addAll(result.data)
            }
            else if (result is PagedResult.Error) {
                message = result.message
            }
        }
    }

    val searchedStudios = mutableStateListOf<SearchStudioQuery.Studio>()

    private suspend fun searchStudio(query: String) {
        SearchRepository.searchStudio(query = query).collect { result ->
            isLoading = result is PagedResult.Loading

            if (result is PagedResult.Success) {
                searchedStudios.clear()
                searchedStudios.addAll(result.data)
            }
            else if (result is PagedResult.Error) {
                message = result.message
            }
        }
    }

    val searchedUsers = mutableStateListOf<SearchUserQuery.User>()

    private suspend fun searchUser(query: String) {
        SearchRepository.searchUser(query = query).collect { result ->
            isLoading = result is PagedResult.Loading

            if (result is PagedResult.Success) {
                searchedUsers.clear()
                searchedUsers.addAll(result.data)
            }
            else if (result is PagedResult.Error) {
                message = result.message
            }
        }
    }

    var isLoadingGenres by mutableStateOf(false)
        private set

    suspend fun getGenreTagCollection() = viewModelScope.launch {
        SearchRepository.getGenreTagCollection().collect { result ->
            isLoadingGenres = result is DataResult.Loading

            if (result is DataResult.Success) {
                result.data.genres.forEach {
                    genreCollection[it] = false
                }
                val externalGenre =
                    if (genreCollection.size == 1) genreCollection.firstNotNullOf { it.key }
                    else null
                externalGenre?.let { genreCollection[externalGenre] = true }

                result.data.tags.forEach {
                    tagCollection[it] = false
                }
                val externalTag = if (tagCollection.size == 1) tagCollection.firstNotNullOf { it.key }
                else null
                externalTag?.let { tagCollection[externalTag] = true }
            }
            else if (result is DataResult.Error) {
                message = result.message
            }
        }
    }

    fun unselectAllGenresAndTags() {
        viewModelScope.launch(Dispatchers.IO) {
            genreCollection.forEach { (t, _) -> genreCollection[t] = false }
            tagCollection.forEach { (t, _) -> tagCollection[t] = false }
        }
    }
}