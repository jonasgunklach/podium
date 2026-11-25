package app.podiumpodcasts.podium.manager

import android.content.Context
import app.podiumpodcasts.podium.SettingsRepository
import app.podiumpodcasts.podium.api.db.AppDatabase
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeModel
import app.podiumpodcasts.podium.api.db.model.PodcastSubscriptionModel
import app.podiumpodcasts.podium.background.worker.PodcastEpisodeDownloadWorker
import java.io.File

class DownloadManager {
    companion object {
        suspend fun requeueDownloads(
            context: Context,
            db: AppDatabase,
            settingsRepository: SettingsRepository = SettingsRepository(context)
        ) {
            db.podcastEpisodeDownloads().allNotDownloadedSync().forEach { download ->
                // don't resume queued downloads of episodes that were played meanwhile
                if(db.podcastEpisodePlayStates().get(download.episodeId)?.played == true) {
                    db.podcastEpisodeDownloads().delete(download.episodeId)
                    return@forEach
                }

                PodcastEpisodeDownloadWorker.enqueueDownload(
                    context = context,
                    episodeId = download.episodeId,
                    settingsRepository = settingsRepository
                )
            }
        }

        suspend fun downloadEpisode(
            context: Context,
            db: AppDatabase,
            episodeId: String,
            auto: Boolean = false
        ) {
            db.podcastEpisodeDownloads()
                .add(episodeId, auto)

            PodcastEpisodeDownloadWorker.enqueueDownload(
                context = context,
                episodeId = episodeId,
                auto = auto
            )
        }

        /**
         * Auto-downloads freshly fetched episodes of [subscription]: newest first,
         * capped at [PodcastSubscriptionModel.autoDownloadLimit] (0 = all), skipping played ones.
         */
        suspend fun autoDownloadNewEpisodes(
            context: Context,
            db: AppDatabase,
            subscription: PodcastSubscriptionModel?,
            newEpisodes: List<PodcastEpisodeModel>
        ) {
            if(subscription?.enableAutoDownload != true) return

            newEpisodes
                .sortedByDescending { it.pubDate }
                .let { if(subscription.autoDownloadLimit > 0) it.take(subscription.autoDownloadLimit) else it }
                .forEach {
                    try {
                        if(db.podcastEpisodePlayStates().get(it.id)?.played == true) return@forEach
                        if(db.podcastEpisodeDownloads().getSync(it.id) != null) return@forEach
                        downloadEpisode(context, db, it.id, auto = true)
                    } catch(e: Exception) {
                        e.printStackTrace()
                    }
                }

            cleanupOutdatedAutoDownloads(context, db, subscription)
        }

        /** "Remove older automatic downloads": drop auto-downloads that fell out of the latest N. */
        suspend fun cleanupOutdatedAutoDownloads(
            context: Context,
            db: AppDatabase,
            subscription: PodcastSubscriptionModel?
        ) {
            if(subscription == null) return
            if(!subscription.enableAutoDownload || !subscription.autoDownloadRemoveOlder) return
            if(subscription.autoDownloadLimit <= 0) return // "All new" has no "older"

            db.podcastEpisodes()
                .outdatedAutoDownloads(subscription.origin, subscription.autoDownloadLimit)
                .forEach { deleteEpisodeDownload(context, db, it) }
        }

        /** Run right after the user enables auto-download or changes the count, so it doesn't wait for the next feed update. */
        suspend fun autoDownloadBackfill(
            context: Context,
            db: AppDatabase,
            origin: String
        ) {
            val subscription = db.podcastSubscriptions().getSync(origin) ?: return
            autoDownloadNewEpisodes(
                context, db, subscription,
                db.podcastEpisodes().autoDownloadCandidates(origin, subscription.autoDownloadLimit)
            )
        }

        suspend fun deleteEpisodeDownload(
            context: Context,
            db: AppDatabase,
            episode: PodcastEpisodeModel
        ) {
            val downloadFile = episode.craftDownloadFile(context)
            if(downloadFile.exists()) downloadFile.delete()

            PodcastEpisodeDownloadWorker.stopDownload(
                context = context,
                episodeId = episode.id
            )

            db.podcastEpisodeDownloads()
                .delete(episode.id)
        }

        fun getDownloadsDirectory(context: Context): File {
            val downloadsDir = File(context.noBackupFilesDir, "downloads")
            downloadsDir.mkdirs()
            return downloadsDir
        }
    }
}