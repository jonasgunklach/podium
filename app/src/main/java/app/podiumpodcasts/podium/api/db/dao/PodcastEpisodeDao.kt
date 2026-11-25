package app.podiumpodcasts.podium.api.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeDownloadModel
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeDownloadState
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeModel
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodePlayStateModel
import kotlinx.coroutines.flow.Flow

enum class PodcastEpisodesOrder(
    val value: String
) {
    ASCENDING("ASC"),
    DESCENDING("DESC")
}

enum class PodcastEpisodesOrderBy(
    val value: String
) {
    DATE("e.pubDate"),
    TITLE("e.title"),
    DURATION("e.duration")
}

enum class PodcastEpisodesFilter(
    val condition: String
) {
    PLAYED("p.played = 1"),
    PAUSED("p.played = 0 AND p.state > 0"),
    NEW("e.new = 1"),
    FAVORITE("EXISTS(SELECT 1 FROM listItem WHERE contentId = e.id AND listId = -2)"),
    DOWNLOADED("COALESCE(d.state, 0) = ${PodcastEpisodeDownloadState.DOWNLOADED.value}")
}

@Dao
interface PodcastEpisodeDao {
    @Transaction
    @Query("SELECT * FROM podcastEpisode WHERE origin=:origin ORDER BY pubDate DESC")
    fun all(origin: String): Flow<List<PodcastEpisodeBundle>>

    @RawQuery(
        observedEntities = [
            PodcastEpisodeModel::class,
            PodcastEpisodePlayStateModel::class,
            PodcastEpisodeDownloadModel::class
        ]
    )
    fun queryPaged(query: SupportSQLiteQuery): PagingSource<Int, PodcastEpisodeBundle>

    fun buildQuery(
        origin: String,
        searchQuery: String?,
        orderBy: PodcastEpisodesOrderBy,
        order: PodcastEpisodesOrder,
        filter: Set<PodcastEpisodesFilter>,
        filterNot: Set<PodcastEpisodesFilter>
    ): SimpleSQLiteQuery {
        val builder = StringBuilder(
            """
            SELECT e.* FROM podcastEpisode AS e
            LEFT JOIN podcastEpisodePlayState AS p ON e.id = p.episodeId
            LEFT JOIN podcastEpisodeDownload AS d ON e.id = d.episodeId
            WHERE e.origin = ?
        """.trimIndent()
        )

        val bindArgs = mutableListOf<Any>(origin)

        if(!searchQuery.isNullOrBlank()) {
            builder.append(" AND (e.title LIKE ? OR e.description LIKE ?)")

            val wildcard = "%$searchQuery%"
            bindArgs.add(wildcard)
            bindArgs.add(wildcard)
        }

        filter.forEach { builder.append(" AND ${it.condition}") }
        filterNot.forEach { builder.append(" AND NOT( ${it.condition} )") }

        builder.append(" ORDER BY ${orderBy.value} ${order.value}")
        return SimpleSQLiteQuery(builder.toString(), bindArgs.toTypedArray())
    }

    @Transaction
    @Query("SELECT * FROM podcastEpisode WHERE origin=:origin ORDER BY pubDate DESC")
    suspend fun allSync(origin: String): List<PodcastEpisodeBundle>

    @Query("SELECT * FROM podcastEpisode WHERE origin=:origin ORDER BY pubDate DESC LIMIT :limit OFFSET :offset")
    suspend fun get(origin: String, limit: Int, offset: Int): List<PodcastEpisodeBundle>

    @Transaction
    @Query("SELECT * FROM podcastEpisode WHERE new=1 ORDER BY pubDate DESC")
    fun allNew(): PagingSource<Int, PodcastEpisodeBundle>

    // auto-downloads outside the latest [keep] unplayed episodes; never touches manual downloads
    // or episodes the user is in the middle of
    @Query(
        """
        SELECT e.* FROM podcastEpisode e
        JOIN podcastEpisodeDownload d ON d.episodeId = e.id
        LEFT JOIN podcastEpisodePlayState p ON p.episodeId = e.id
        WHERE e.origin = :origin AND d.auto = 1
          AND NOT (COALESCE(p.played, 0) = 0 AND COALESCE(p.state, 0) > 0)
          AND e.id NOT IN (
              SELECT e2.id FROM podcastEpisode e2
              JOIN podcastEpisodePlayState p2 ON p2.episodeId = e2.id
              WHERE e2.origin = :origin AND p2.played = 0
              ORDER BY e2.pubDate DESC LIMIT :keep
          )
    """
    )
    suspend fun outdatedAutoDownloads(origin: String, keep: Int): List<PodcastEpisodeModel>

    // auto-download backfill: newest N unplayed, or (limit 0 = "All new") the ones still marked new
    @Query(
        """
        SELECT e.* FROM podcastEpisode e
        JOIN podcastEpisodePlayState p ON p.episodeId = e.id
        WHERE e.origin = :origin AND p.played = 0 AND (:limit > 0 OR e.new = 1)
        ORDER BY e.pubDate DESC
        LIMIT CASE WHEN :limit > 0 THEN :limit ELSE -1 END
    """
    )
    suspend fun autoDownloadCandidates(origin: String, limit: Int): List<PodcastEpisodeModel>

    // autoplay: next newer unplayed episode of the same show
    @Transaction
    @Query(
        """
        SELECT e.* FROM podcastEpisode e
        JOIN podcastEpisodePlayState p ON p.episodeId = e.id
        WHERE e.origin = :origin AND e.pubDate > :afterPubDate AND p.played = 0
          AND e.id NOT IN (:exclude)
        ORDER BY e.pubDate ASC LIMIT 1
    """
    )
    suspend fun nextUnplayedInShow(origin: String, afterPubDate: Long, exclude: List<String>): PodcastEpisodeBundle?

    // autoplay: most recently touched unfinished episode
    @Transaction
    @Query(
        """
        SELECT e.* FROM podcastEpisode e
        JOIN podcastEpisodePlayState p ON p.episodeId = e.id
        WHERE p.played = 0 AND p.state > 0 AND e.id NOT IN (:exclude)
        ORDER BY p.lastUpdate DESC LIMIT 1
    """
    )
    suspend fun lastContinuePlaying(exclude: List<String>): PodcastEpisodeBundle?

    // autoplay: newest not-yet-started episode of a subscribed show
    @Transaction
    @Query(
        """
        SELECT e.* FROM podcastEpisode e
        JOIN podcastSubscription s ON s.origin = e.origin
        JOIN podcastEpisodePlayState p ON p.episodeId = e.id
        WHERE p.played = 0 AND p.state = 0 AND e.id NOT IN (:exclude)
        ORDER BY e.pubDate DESC LIMIT 1
    """
    )
    suspend fun newestUnplayed(exclude: List<String>): PodcastEpisodeBundle?

    // home: newest not-yet-started episodes of subscribed podcasts
    @Transaction
    @Query(
        """
        SELECT e.* FROM podcastEpisode e
        JOIN podcastSubscription s ON s.origin = e.origin
        JOIN podcastEpisodePlayState p ON p.episodeId = e.id
        WHERE p.played = 0 AND p.state = 0
        ORDER BY e.pubDate DESC LIMIT :limit
    """
    )
    fun latestUnplayed(limit: Int = 20): Flow<List<PodcastEpisodeBundle>>

    @Query("SELECT * FROM podcastEpisode WHERE new=1 ORDER BY pubDate DESC LIMIT :limit OFFSET :offset")
    fun getNew(limit: Int, offset: Int): List<PodcastEpisodeBundle>

    @Query("SELECT id FROM podcastEpisode WHERE origin=:origin")
    suspend fun getEpisodeIds(origin: String): List<String>

    @Query("SELECT * FROM podcastEpisode WHERE id=:id")
    fun get(id: String): Flow<PodcastEpisodeBundle>

    @Query("SELECT * FROM podcastEpisode WHERE id=:id")
    suspend fun getSync(id: String): PodcastEpisodeBundle

    @Query("SELECT * FROM podcastEpisode WHERE origin=:origin AND audioUrl=:audioUrl")
    suspend fun getSyncByOriginAndAudioUrl(origin: String, audioUrl: String): PodcastEpisodeBundle?

    @Query(
        """
        SELECT * FROM podcastEpisode 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%'
        ORDER BY pubDate DESC
    """
    )
    fun search(query: String): PagingSource<Int, PodcastEpisodeBundle>

    @Transaction
    suspend fun newAndUpdateNewEpisodesCount(
        origin: String, episodeId: String
    ) {
        _new(episodeId)
        _updateNewEpisodesCount(origin)
    }

    @Transaction
    suspend fun unnewAndUpdateNewEpisodesCount(
        origin: String, episodeId: String
    ) {
        _unnew(episodeId)
        _updateNewEpisodesCount(origin)
    }

    @Transaction
    suspend fun insertAllAndUpdateNewEpisodesCount(
        origin: String,
        vararg episodes: PodcastEpisodeModel
    ) {
        _insertAll(*episodes)
        _updateNewEpisodesCount(origin)
    }

    @Update
    suspend fun update(episode: PodcastEpisodeModel)

    @Query("DELETE FROM podcastEpisode WHERE id=:id")
    suspend fun delete(id: String)

    @Insert
    suspend fun _insertAll(vararg episodes: PodcastEpisodeModel)

    @Query("UPDATE podcastEpisode SET new=1 WHERE id=:id")
    suspend fun _new(id: String)

    @Query("UPDATE podcastEpisode SET new=0 WHERE id=:id")
    suspend fun _unnew(id: String)

    @Query(
        """
        UPDATE podcastSubscription 
        SET newEpisodes = (
            SELECT COUNT(*) 
            FROM podcastEpisode 
            WHERE podcastEpisode.origin = podcastSubscription.origin 
            AND podcastEpisode.new = 1
        ) WHERE origin=:origin
    """
    )
    suspend fun _updateNewEpisodesCount(origin: String)
}