package com.axiel7.anihyou.data.api

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.axiel7.anihyou.ActivityDetailsQuery
import com.axiel7.anihyou.ActivityFeedQuery
import com.axiel7.anihyou.DeleteActivityMutation
import com.axiel7.anihyou.UpdateActivityReplyMutation
import com.axiel7.anihyou.UpdateTextActivityMutation
import com.axiel7.anihyou.type.ActivityType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityApi @Inject constructor(
    private val client: ApolloClient
) {
    fun activityFeedQuery(
        isFollowing: Boolean,
        typeIn: List<ActivityType>?,
        fetchFromNetwork: Boolean,
        page: Int,
        perPage: Int,
    ) = client
        .query(
            ActivityFeedQuery(
                page = Optional.present(page),
                perPage = Optional.present(perPage),
                isFollowing = Optional.present(isFollowing),
                typeIn = Optional.presentIfNotNull(typeIn),
            )
        )
        .fetchPolicy(if (fetchFromNetwork) FetchPolicy.NetworkFirst else FetchPolicy.CacheFirst)

    fun activityDetailsQuery(activityId: Int) = client
        .query(
            ActivityDetailsQuery(
                activityId = Optional.present(activityId)
            )
        )

    suspend fun updateActivityDetailsCache(
        id: Int,
        activity: ActivityDetailsQuery.Activity,
    ) {
        val result = client.apolloStore
            .writeOperation(
                operation = ActivityDetailsQuery(
                    activityId = Optional.present(id)
                ),
                operationData = ActivityDetailsQuery.Data(
                    Activity = activity
                )
            )
        client.apolloStore.publish(result)
    }

    fun updateTextActivityMutation(
        id: Int?,
        text: String
    ) = client
        .mutation(
            UpdateTextActivityMutation(
                id = Optional.presentIfNotNull(id),
                text = Optional.present(text)
            )
        )

    fun updateActivityReplyMutation(
        activityId: Int,
        id: Int?,
        text: String
    ) = client
        .mutation(
            UpdateActivityReplyMutation(
                activityId = Optional.present(activityId),
                id = Optional.presentIfNotNull(id),
                text = Optional.present(text)
            )
        )

    fun deleteActivityMutation(id: Int) = client
        .mutation(DeleteActivityMutation(id = Optional.present(id)))
}