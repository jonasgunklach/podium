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
    }
}
