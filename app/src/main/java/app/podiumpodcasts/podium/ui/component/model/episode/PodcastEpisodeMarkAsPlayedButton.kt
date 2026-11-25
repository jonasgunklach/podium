package app.podiumpodcasts.podium.ui.component.model.episode

import app.podiumpodcasts.podium.api.db.AppDatabase

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import app.podiumpodcasts.podium.R
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.ui.helper.LocalDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PodcastEpisodeMarkAsPlayedButton(
    bundle: PodcastEpisodeBundle,
    colors: IconToggleButtonColors = IconButtonDefaults.filledIconToggleButtonColors()
) {
    val scope = rememberCoroutineScope()
    val db = LocalDatabase.current

    val playState = bundle.playState

    FilledIconToggleButton(
        checked = playState?.played == true,
        onCheckedChange = {
            if(playState == null) return@FilledIconToggleButton

            scope.launch {
                setEpisodePlayed(db, bundle, !playState.played)
            }
        },

        colors = colors
    ) {
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = stringResource(R.string.common_action_mark_as_played)
        )
    }
}

/** Mark played/unplayed, including the "new" flag and the sync action. Shared by the button and the long-press menu. */
suspend fun setEpisodePlayed(
    db: AppDatabase,
    bundle: PodcastEpisodeBundle,
    played: Boolean
) {
    if(played) {
        db.podcastEpisodes()
            .unnewAndUpdateNewEpisodesCount(
                origin = bundle.episode.origin,
                episodeId = bundle.episode.id
            )

        db.podcastEpisodePlayStates()
            .savePlayed(bundle.episode.id, true)

        db.syncActions()
            .addPlayState(
                origin = bundle.episode.origin,
                episodeId = bundle.episode.id,
                audioUrl = bundle.episode.audioUrl,
                duration = bundle.episode.duration,
                state = bundle.episode.duration,
                played = true
            )
    } else {
        db.podcastEpisodePlayStates().set(
            bundle.episode.id, 0, false
        )

        db.syncActions()
            .addPlayState(
                origin = bundle.episode.origin,
                episodeId = bundle.episode.id,
                audioUrl = bundle.episode.audioUrl,
                duration = bundle.episode.duration,
                state = 0,
                played = false
            )
    }
}
