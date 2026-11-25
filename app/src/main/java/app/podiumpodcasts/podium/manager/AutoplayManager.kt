package app.podiumpodcasts.podium.manager

import app.podiumpodcasts.podium.api.db.AppDatabase
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeModel

enum class AutoplayReason { NEXT_IN_SHOW, CONTINUE_PLAYING, NEWEST }

data class AutoplayPick(
    val bundle: PodcastEpisodeBundle,
    val reason: AutoplayReason
)

/**
 * What plays when the queue runs out after [last]:
 * 1. next newer unplayed episode of the same show
 * 2. the most recently touched unfinished episode
 * 3. the newest unstarted episode of a subscribed show
 */
object AutoplayManager {
    suspend fun pick(
        db: AppDatabase,
        last: PodcastEpisodeModel?,
        exclude: Collection<String>
    ): AutoplayPick? {
        val ex = exclude.toList()
        val episodes = db.podcastEpisodes()

        last?.let { episodes.nextUnplayedInShow(it.origin, it.pubDate, ex) }
            ?.let { return AutoplayPick(it, AutoplayReason.NEXT_IN_SHOW) }

        episodes.lastContinuePlaying(ex)
            ?.let { return AutoplayPick(it, AutoplayReason.CONTINUE_PLAYING) }

        episodes.newestUnplayed(ex)
            ?.let { return AutoplayPick(it, AutoplayReason.NEWEST) }

        return null
    }
}
