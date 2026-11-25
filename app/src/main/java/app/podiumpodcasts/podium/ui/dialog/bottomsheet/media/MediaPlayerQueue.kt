package app.podiumpodcasts.podium.ui.dialog.bottomsheet.media

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import app.podiumpodcasts.podium.manager.AutoplayReason
import app.podiumpodcasts.podium.ui.dialog.ShimmerAsyncImage
import app.podiumpodcasts.podium.ui.helper.LocalSettingsRepository
import kotlinx.coroutines.launch
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.podiumpodcasts.podium.R
import app.podiumpodcasts.podium.ui.component.media.MediaItemListItem
import app.podiumpodcasts.podium.ui.theme.Typography
import app.podiumpodcasts.podium.ui.vm.MediaPlayerViewModel
import app.podiumpodcasts.podium.utils.getEpisodeId
import app.podiumpodcasts.podium.utils.getOrigin

/** Second page of the player sheet. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MediaPlayerQueuePage(
    onOpenEpisode: (
        origin: String,
        id: String
    ) -> Unit
) {
    val vm = viewModel<MediaPlayerViewModel>()
    val scope = rememberCoroutineScope()
    val settingsRepository = LocalSettingsRepository.current
    val autoplay = settingsRepository.behavior.autoplay.collectAsState(true)

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.player_up_next),
            style = Typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        if(vm.queueIndex.isEmpty()) Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            text = stringResource(R.string.player_queue_empty),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        vm.queueIndex.forEachIndexed { position, index ->
            key(index) {
                vm.rememberMediaItemAt(index)?.let { mediaItem ->
                    MediaItemListItem(
                        mediaItem = mediaItem,
                        index = position,
                        count = vm.queueIndex.size,
                        onClick = {
                            onOpenEpisode(mediaItem.getOrigin(), mediaItem.getEpisodeId())
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.player_autoplay), style = Typography.titleMedium)
                Text(
                    stringResource(R.string.player_autoplay_description),
                    style = Typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = autoplay.value,
                onCheckedChange = { scope.launch { settingsRepository.behavior.setAutoplay(it) } }
            )
        }

        val pick = vm.autoplayPick
        if(autoplay.value && pick != null) Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { onOpenEpisode(pick.bundle.episode.origin, pick.bundle.episode.id) }
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShimmerAsyncImage(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                model = pick.bundle.episode.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.player_autoplay_then) + " · " + when(pick.reason) {
                        AutoplayReason.NEXT_IN_SHOW -> stringResource(R.string.player_autoplay_next_in_show, pick.bundle.episode.podcastTitle)
                        AutoplayReason.CONTINUE_PLAYING -> stringResource(R.string.route_continue_playing)
                        AutoplayReason.NEWEST -> stringResource(R.string.player_autoplay_newest)
                    },
                    style = Typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pick.bundle.episode.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
