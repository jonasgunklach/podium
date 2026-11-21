package app.podiumpodcasts.podium.ui.route.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.paging.compose.itemKey
import app.podiumpodcasts.podium.ui.component.model.SubscriptionCard
import app.podiumpodcasts.podium.ui.theme.Typography
import app.podiumpodcasts.podium.ui.vm.home.SubscriptionsViewModel
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import app.podiumpodcasts.podium.R
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeModel
import app.podiumpodcasts.podium.ui.component.library.LibraryItem
import app.podiumpodcasts.podium.ui.component.media.FloatingMediaPlayerSpacer
import app.podiumpodcasts.podium.ui.component.media.LocalFloatingMediaPlayerShown
import app.podiumpodcasts.podium.ui.component.model.list.ListLibraryItem
import app.podiumpodcasts.podium.ui.dialog.bottomsheet.ListCreateBottomSheet
import app.podiumpodcasts.podium.ui.helper.LocalDatabase
import app.podiumpodcasts.podium.ui.vm.DownloadsViewModel
import app.podiumpodcasts.podium.ui.vm.home.ContinuePlayingViewModel
import app.podiumpodcasts.podium.ui.vm.home.NewEpisodesViewModel
import app.podiumpodcasts.podium.ui.vm.library.LibraryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryRoute(
    onClickDownloads: () -> Unit,
    onClickHistory: () -> Unit,
    onClickContinuePlaying: () -> Unit,
    onClickNewEpisodes: () -> Unit,

    onClickPodcast: (origin: String) -> Unit,
    onClickEpisode: (episode: PodcastEpisodeModel) -> Unit,
    onClickList: (listId: Int) -> Unit
) {
    val db = LocalDatabase.current

    val vm = viewModel { LibraryViewModel(db) }

    val systemLists = vm.systemLists.collectAsLazyPagingItems()
    val nonSystemLists = vm.nonSystemLists.collectAsLazyPagingItems()

    val downloadsVm = viewModel { DownloadsViewModel(db) }
    val continuePlayingVm = viewModel { ContinuePlayingViewModel(db) }
    val newEpisodesVm = viewModel { NewEpisodesViewModel(db) }

    val downloadsPager = remember { downloadsVm.downloads }.collectAsLazyPagingItems()
    val continuePlayingPager =
        remember { continuePlayingVm.continuePlaying }.collectAsLazyPagingItems()
    val newEpisodesPager = remember { newEpisodesVm.newEpisodes }.collectAsLazyPagingItems()

    val subscriptionsVm = viewModel { SubscriptionsViewModel(db) }
    val subscriptionsPager = remember { subscriptionsVm.subscriptions }.collectAsLazyPagingItems()

    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()

    Scaffold(
        Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LibrarySearch(
                scrollBehavior = scrollBehavior,

                onClickPodcast = onClickPodcast,
                onClickEpisode = onClickEpisode
            )
        }
    ) { inset ->
        val floatingMediaPlayerShown = LocalFloatingMediaPlayerShown.current

        LazyVerticalGrid(
            state = if(
                (systemLists.loadState.isIdle || systemLists.itemCount > 0)
                && (nonSystemLists.loadState.isIdle || nonSystemLists.itemCount > 0)
            ) {
                vm.lazyGridState
            } else {
                rememberLazyGridState()
            },
            modifier = Modifier.padding(inset),
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LibraryShortcut(Icons.Rounded.Download, stringResource(R.string.route_downloads), downloadsPager.itemCount, onClickDownloads)
                    LibraryShortcut(Icons.Rounded.Pause, stringResource(R.string.route_continue_playing), continuePlayingPager.itemCount, onClickContinuePlaying)
                    LibraryShortcut(Icons.Rounded.NewReleases, stringResource(R.string.route_new_episodes), newEpisodesPager.itemCount, onClickNewEpisodes)
                    LibraryShortcut(Icons.Rounded.History, stringResource(R.string.route_history), 0, onClickHistory)
                }
            }

            if(subscriptionsPager.itemCount > 0) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        modifier = Modifier.padding(top = 8.dp),
                        text = stringResource(R.string.route_home_your_shows),
                        color = MaterialTheme.colorScheme.primary,
                        style = Typography.titleMediumEmphasized
                    )
                }

                items(
                    subscriptionsPager.itemCount,
                    key = subscriptionsPager.itemKey { it.podcast.origin }
                ) {
                    subscriptionsPager[it]?.let { bundle ->
                        SubscriptionCard(
                            modifier = Modifier.animateItem(),
                            bundle = bundle,
                            size = Dp.Unspecified,
                            onClick = { onClickPodcast(bundle.podcast.origin) }
                        )
                    }
                }
            }

            item(span = { GridItemSpan(maxLineSpan) }) {
                HorizontalDivider(
                    Modifier.padding(vertical = 8.dp)
                )
            }

            items(systemLists.itemCount) {
                systemLists[it]?.let { list ->
                    ListLibraryItem(
                        modifier = Modifier.animateItem(),
                        list = list,
                        onClick = {
                            onClickList(list.id)
                        }
                    )
                }
            }

            items(nonSystemLists.itemCount) {
                nonSystemLists[it]?.let { list ->
                    ListLibraryItem(
                        modifier = Modifier.animateItem(),
                        list = list,
                        onClick = {
                            onClickList(list.id)
                        }
                    )
                }
            }

            item {
                LibraryItem(
                    modifier = Modifier.animateItem(),
                    title = stringResource(R.string.common_action_create),
                    icon = Icons.Rounded.Add,
                    contentDescription = stringResource(R.string.common_action_create),
                    onClick = {
                        vm.showListCreateBottomSheet.value = true
                    }
                )
            }

            if(floatingMediaPlayerShown) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FloatingMediaPlayerSpacer()
                }
            }
        }
    }

    if(vm.showListCreateBottomSheet.value) ListCreateBottomSheet {
        vm.showListCreateBottomSheet.value = false
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibraryShortcut(
    icon: ImageVector,
    label: String,
    count: Int,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shapes = ButtonDefaults.shapes()
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
        Text(if(count > 0) "$label · $count" else label)
    }
}
