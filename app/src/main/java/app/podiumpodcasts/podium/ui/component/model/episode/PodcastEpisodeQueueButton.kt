package app.podiumpodcasts.podium.ui.component.model.episode

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material3.HorizontalDivider
import app.podiumpodcasts.podium.manager.DownloadManager
import app.podiumpodcasts.podium.ui.helper.LocalDatabase
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import app.podiumpodcasts.podium.AppActivity
import app.podiumpodcasts.podium.R
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.ui.vm.MediaPlayerViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PodcastEpisodeQueueButton(
    bundle: PodcastEpisodeBundle
) {
    val context = LocalContext.current
    val vm = viewModel<MediaPlayerViewModel>()

    val inQueue = vm.queue.contains(bundle.episode.id)

    AnimatedVisibility(
        visible = vm.isPlayerVisible,
        enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
        exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
    ) {
        FilledTonalIconToggleButton(
            checked = inQueue,
            onCheckedChange = {
                if(inQueue) {
                    vm.dequeue(bundle.episode.id)
                } else {
                    vm.enqueue(context, bundle)
                }
            }
        ) {
            AnimatedContent(
                targetState = inQueue
            ) { inQueue ->
                when(inQueue) {
                    true -> Icon(
                        imageVector = Icons.Rounded.PlaylistRemove,
                        contentDescription = stringResource(R.string.common_action_remove_from_queue)
                    )

                    else -> Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.common_action_add_to_queue)
                    )
                }
            }
        }
    }
}

/** Long-press menu for an episode: queue, played state, download, go to podcast. */
@Composable
fun PodcastEpisodeQueueMenu(
    bundle: PodcastEpisodeBundle,
    expanded: Boolean,
    onOpenPodcast: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val db = LocalDatabase.current
    val scope = rememberCoroutineScope()
    val vm = viewModel<MediaPlayerViewModel>()

    val inQueue = vm.queue.contains(bundle.episode.id)
    val isCurrent = vm.metadataEpisodeId == bundle.episode.id
    val played = bundle.playState?.played == true

    fun act(block: suspend () -> Unit) {
        scope.launch { block() }
        onDismiss()
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if(!isCurrent) DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.QueuePlayNext, contentDescription = null) },
            text = { Text(stringResource(R.string.common_action_play_next)) },
            onClick = { act { vm.playNext(context, bundle) } }
        )

        if(!isCurrent && !inQueue) DropdownMenuItem(
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) },
            text = { Text(stringResource(R.string.common_action_add_to_queue)) },
            onClick = { act { vm.enqueue(context, bundle) } }
        )

        if(inQueue) DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.PlaylistRemove, contentDescription = null) },
            text = { Text(stringResource(R.string.common_action_remove_from_queue)) },
            onClick = { act { vm.dequeue(bundle.episode.id) } }
        )

        HorizontalDivider()

        DropdownMenuItem(
            leadingIcon = {
                Icon(if(played) Icons.Rounded.RemoveDone else Icons.Rounded.Check, contentDescription = null)
            },
            text = {
                Text(stringResource(if(played) R.string.common_action_mark_as_unplayed else R.string.common_action_mark_as_played))
            },
            onClick = { act { setEpisodePlayed(db, bundle, !played) } }
        )

        DropdownMenuItem(
            leadingIcon = {
                Icon(if(bundle.download == null) Icons.Rounded.Download else Icons.Rounded.Delete, contentDescription = null)
            },
            text = {
                Text(stringResource(if(bundle.download == null) R.string.common_action_download else R.string.common_action_remove_download))
            },
            onClick = {
                act {
                    if(bundle.download == null) DownloadManager.downloadEpisode(context, db, bundle.episode.id)
                    else DownloadManager.deleteEpisodeDownload(context, db, bundle.episode)
                }
            }
        )

        if(onOpenPodcast != null) DropdownMenuItem(
            leadingIcon = { Icon(Icons.Rounded.Podcasts, contentDescription = null) },
            text = { Text(stringResource(R.string.common_action_go_to_podcast)) },
            onClick = {
                onOpenPodcast()
                onDismiss()
            }
        )
    }
}
