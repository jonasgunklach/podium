package app.podiumpodcasts.podium.ui.route.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Alignment
import android.util.LruCache
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.ui.graphics.toArgb
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.asImageBitmap
import com.materialkolor.ktx.themeColorOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import app.podiumpodcasts.podium.background.work.PodcastUpdateWork
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.paging.compose.itemKey
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeBundle
import app.podiumpodcasts.podium.ui.component.common.SwitchableDynamicMaterialExpressiveTheme
import app.podiumpodcasts.podium.ui.component.model.episode.PodcastEpisodeDownloadButton
import app.podiumpodcasts.podium.ui.component.model.episode.PodcastEpisodePlayButton
import app.podiumpodcasts.podium.ui.dialog.ShimmerAsyncImage
import app.podiumpodcasts.podium.ui.formatPubDate
import app.podiumpodcasts.podium.ui.theme.Typography
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumExtendedFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import app.podiumpodcasts.podium.R
import app.podiumpodcasts.podium.api.db.model.PodcastEpisodeModel
import app.podiumpodcasts.podium.ui.component.layout.InfoLayout
import app.podiumpodcasts.podium.ui.component.layout.Section
import app.podiumpodcasts.podium.ui.component.layout.SectionCarousel
import app.podiumpodcasts.podium.ui.component.media.FloatingMediaPlayerBreakpoint
import app.podiumpodcasts.podium.ui.component.media.FloatingMediaPlayerSpacer
import app.podiumpodcasts.podium.ui.component.media.LocalFloatingMediaPlayerHeight
import app.podiumpodcasts.podium.ui.component.model.PodcastCard
import app.podiumpodcasts.podium.ui.component.model.SubscriptionCard
import app.podiumpodcasts.podium.ui.helper.LocalDatabase
import app.podiumpodcasts.podium.ui.helper.LocalSettingsRepository
import app.podiumpodcasts.podium.ui.helper.PagerScaffold
import app.podiumpodcasts.podium.ui.vm.home.ContinuePlayingViewModel
import app.podiumpodcasts.podium.ui.vm.home.LocallyAvailableViewModel
import app.podiumpodcasts.podium.ui.vm.home.SubscriptionsViewModel

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun HomeRoute(
    onSettings: () -> Unit,

    onClickSubscriptions: () -> Unit,
    onClickContinuePlaying: () -> Unit,
    onClickNewEpisodes: () -> Unit,
    onClickLocallyAvailable: () -> Unit,

    onClickAddPodcast: () -> Unit,

    onClickDiscover: () -> Unit,

    onClickPodcast: (origin: String) -> Unit,
    onClickEpisode: (episode: PodcastEpisodeModel) -> Unit
) {
    val db = LocalDatabase.current

    val settingsRepository = LocalSettingsRepository.current

    val useAlternativeBranding = settingsRepository.appearance.useAlternativeBranding
        .collectAsState(false)

    val enableArtworkColors = settingsRepository.appearance.enableArtworkColors
        .collectAsState(true)

    val subscriptionsVm = viewModel { SubscriptionsViewModel(db) }
    val continuePlayingVm = viewModel { ContinuePlayingViewModel(db) }
    val locallyAvailableVm = viewModel { LocallyAvailableViewModel(db) }

    val subscriptionsPager = subscriptionsVm.subscriptions.collectAsLazyPagingItems()
    val continuePlayingPager = continuePlayingVm.continuePlaying.collectAsLazyPagingItems()
    val locallyAvailablePager = locallyAvailableVm.locallyAvailable.collectAsLazyPagingItems()

    // null until the first DB emission, so the list doesn't render (and anchor) before this section exists
    val latestEpisodes = remember { db.podcastEpisodes().latestUnplayed() }
        .collectAsState(null)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val lastUpdate = remember { db.podcastSubscriptions().lastUpdate() }
        .collectAsState(null)

    val refreshing = remember { mutableStateOf(false) }
    val refresh: () -> Unit = {
        scope.launch {
            refreshing.value = true
            try {
                // same work the periodic background update runs
                withContext(Dispatchers.IO) {
                    PodcastUpdateWork(context, db, settingsRepository).doWork()
                }
            } catch(e: Exception) {
                e.printStackTrace()
            } finally {
                refreshing.value = false
            }
        }
    }

    val loaded = remember { mutableStateOf(false) }
    LaunchedEffect(
        subscriptionsPager.loadState.isIdle,
        continuePlayingPager.loadState.isIdle,
        locallyAvailablePager.loadState.isIdle,
        latestEpisodes.value != null
    ) {
        if(latestEpisodes.value == null) return@LaunchedEffect
        if(!subscriptionsPager.loadState.isIdle) return@LaunchedEffect
        if(!continuePlayingPager.loadState.isIdle) return@LaunchedEffect
        if(!locallyAvailablePager.loadState.isIdle) return@LaunchedEffect
        loaded.value = true
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val listState = rememberLazyListState()

    val expandedFab by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    BoxWithConstraints {
        Scaffold(
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                CenterAlignedTopAppBar(
                    scrollBehavior = scrollBehavior,
                    title = {
                        Text(
                            when(useAlternativeBranding.value) {
                                false -> stringResource(R.string.app_name)
                                true -> stringResource(R.string.app_name_alternative)
                            }
                        )
                    },
                    actions = {
                        FilledIconButton(
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            onClick = onSettings
                        ) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.route_settings)
                            )
                        }
                    }
                )
            },
            floatingActionButton = {
                MediumExtendedFloatingActionButton(
                    modifier = Modifier.padding(
                        bottom = if(maxWidth <= FloatingMediaPlayerBreakpoint)
                            LocalFloatingMediaPlayerHeight.current
                        else
                            0.dp
                    ),
                    expanded = expandedFab,
                    text = {
                        Text(stringResource(R.string.common_action_add))
                    },
                    icon = {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.common_action_add),
                            modifier = Modifier.size(FloatingActionButtonDefaults.MediumIconSize),
                        )
                    },
                    onClick = onClickAddPodcast
                )
            }
        ) { inset ->
            Box(
                Modifier
                    .padding(inset)
                    .fillMaxSize()
            ) {
                PagerScaffold(
                    subscriptionsPager,
                    continuePlayingPager,
                    locallyAvailablePager,
                    isEmpty = {
                        InfoLayout(
                            icon = Icons.Rounded.Podcasts,
                            title = {
                                stringResource(R.string.route_home_empty_title)
                            },
                            content = {
                                Text(
                                    text = stringResource(R.string.route_home_empty_text),
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(32.dp))

                                Button(
                                    onClick = {
                                        onClickDiscover()
                                    }
                                ) {
                                    Icon(
                                        Icons.Rounded.Explore,
                                        contentDescription = stringResource(R.string.route_discover),
                                        modifier = Modifier.size(ButtonDefaults.IconSize),
                                    )

                                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

                                    Text(stringResource(R.string.route_discover))
                                }
                            }
                        )
                    }
                ) {
                    Box(
                        Modifier.fillMaxSize()
                    ) {
                        if(loaded.value) PullToRefreshBox(
                            isRefreshing = refreshing.value,
                            onRefresh = refresh
                        ) { LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState
                        ) {
                            // always present: inserting it later would push the list's scroll anchor below it
                            item(key = "LAST_CHECKED") {
                                val millis = lastUpdate.value ?: 0L
                                Text(
                                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
                                    text = when(millis > 0) {
                                        true -> stringResource(
                                            R.string.route_home_last_checked,
                                            formatPubDate(context, millis / 1000)
                                        )
                                        false -> stringResource(R.string.route_home_pull_to_check)
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = Typography.labelMedium
                                )
                            }

                            if(!latestEpisodes.value.isNullOrEmpty()) item(
                                key = "NEW_EPISODES"
                            ) {
                                Section(
                                    modifier = Modifier.animateItem(),
                                    title = stringResource(R.string.route_new_episodes),
                                    onClickExpand = { onClickNewEpisodes() }
                                ) {
                                    val rowState = rememberLazyListState()
                                    LazyRow(
                                        state = rowState,
                                        flingBehavior = rememberSnapFlingBehavior(rowState, SnapPosition.Start),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            latestEpisodes.value.orEmpty(),
                                            key = { it.episode.id }
                                        ) { bundle ->
                                            NewEpisodeCard(
                                                modifier = Modifier.animateItem(),
                                                bundle = bundle,
                                                enableArtworkColors = enableArtworkColors.value,
                                                onClick = { onClickEpisode(bundle.episode) }
                                            )
                                        }
                                    }
                                }
                            }

                            if(continuePlayingPager.itemCount > 0) item(
                                key = "CONTINUE_PLAYING"
                            ) {
                                Section(
                                    modifier = Modifier.animateItem(),
                                    title = stringResource(R.string.route_continue_playing),
                                    onClickExpand = { onClickContinuePlaying() }
                                ) {
                                    val rowState = rememberLazyListState()
                                    LazyRow(
                                        state = rowState,
                                        flingBehavior = rememberSnapFlingBehavior(rowState, SnapPosition.Start),
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(
                                            continuePlayingPager.itemCount,
                                            key = continuePlayingPager.itemKey { it.episode.id }
                                        ) {
                                            val item = continuePlayingPager[it] ?: return@items

                                            ContinuePlayingCard(
                                                modifier = Modifier.animateItem(),
                                                bundle = item.toPodcastEpisodeBundle(),
                                                enableArtworkColors = enableArtworkColors.value,
                                                onClick = { onClickEpisode(item.episode) }
                                            )
                                        }
                                    }
                                }
                            }

                            if(subscriptionsPager.itemCount > 0) item(
                                key = "SUBSCRIPTIONS"
                            ) {
                                SectionCarousel(
                                    modifier = Modifier.animateItem(),
                                    title = stringResource(R.string.route_home_your_shows),
                                    onClickExpand = { onClickSubscriptions() }
                                ) {
                                    items(subscriptionsPager.itemCount) {
                                        val bundle = subscriptionsPager[it]
                                            ?: return@items

                                        SubscriptionCard(
                                            modifier = Modifier.animateItem(),
                                            bundle = bundle,
                                            size = 144.dp,
                                            onClick = {
                                                onClickPodcast(bundle.podcast.origin)
                                            }
                                        )
                                    }
                                }
                            }

                            if(locallyAvailablePager.itemCount > 0) item(
                                key = "LOCALLY_AVAILABLE"
                            ) {
                                SectionCarousel(
                                    modifier = Modifier.animateItem(),
                                    title = stringResource(R.string.route_locally_available),
                                    onClickExpand = { onClickLocallyAvailable() }
                                ) {
                                    items(locallyAvailablePager.itemCount) {
                                        val podcast = locallyAvailablePager[it]
                                            ?: return@items

                                        PodcastCard(
                                            modifier = Modifier.animateItem(),
                                            podcast = podcast,
                                            onClick = {
                                                onClickPodcast(podcast.origin)
                                            }
                                        )
                                    }
                                }
                            }

                            item {
                                FloatingMediaPlayerSpacer()
                            }
                        } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NewEpisodeCard(
    modifier: Modifier = Modifier,
    bundle: PodcastEpisodeBundle,
    enableArtworkColors: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val seedColor = rememberArtworkSeedColor(bundle.episode)

    SwitchableDynamicMaterialExpressiveTheme(
        enable = enableArtworkColors,
        seedColor = seedColor
    ) {
        val container = MaterialTheme.colorScheme.primaryContainer

        Card(
            modifier = modifier.width(272.dp),
            onClick = onClick,
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = container,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Box {
                ShimmerAsyncImage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(208.dp),
                    model = bundle.episode.imageUrl,
                    contentDescription = stringResource(R.string.common_cover_image),
                    contentScale = ContentScale.Crop
                )

                // fade artwork into the card color
                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1f to container
                            )
                        )
                )
            }

            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "${formatPubDate(context, bundle.episode.pubDate)} · ${bundle.episode.podcastTitle}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = Typography.labelMedium
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = bundle.episode.title,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = Typography.titleMediumEmphasized
                )

                Spacer(Modifier.height(4.dp))

                val description = remember(bundle.episode.description) {
                    AnnotatedString.fromHtml(bundle.episode.description).text
                        .replace(Regex("\\s+"), " ").trim()
                }

                Text(
                    text = description,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = Typography.bodySmall
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PodcastEpisodePlayButton(bundle = bundle)

                    Spacer(Modifier.weight(1f))

                    PodcastEpisodeDownloadButton(
                        icon = true,
                        bundle = bundle,
                        iconColors = IconButtonDefaults.filledTonalIconToggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            checkedContainerColor = MaterialTheme.colorScheme.primary,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ContinuePlayingCard(
    modifier: Modifier = Modifier,
    bundle: PodcastEpisodeBundle,
    enableArtworkColors: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val seedColor = rememberArtworkSeedColor(bundle.episode)

    SwitchableDynamicMaterialExpressiveTheme(
        enable = enableArtworkColors,
        seedColor = seedColor
    ) {
        Card(
            modifier = modifier.width(328.dp),
            onClick = onClick,
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Row(
                Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerAsyncImage(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    model = bundle.episode.imageUrl,
                    contentDescription = stringResource(R.string.common_cover_image),
                    contentScale = ContentScale.Crop
                )

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${formatPubDate(context, bundle.episode.pubDate)} · ${bundle.episode.podcastTitle}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = Typography.labelMedium
                    )

                    Text(
                        text = bundle.episode.title,
                        minLines = 2,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = Typography.titleSmallEmphasized
                    )

                    Spacer(Modifier.height(8.dp))

                    PodcastEpisodePlayButton(bundle = bundle)
                }
            }
        }
    }
}

// ponytail: process-wide cache so cards don't flash the podcast color and re-extract on every scroll-in
private val artworkSeedCache = LruCache<String, Int>(256)

/**
 * Seed color taken from the episode's own artwork (what the card shows),
 * falling back to the podcast cover color until it's known.
 */
@Composable
private fun rememberArtworkSeedColor(
    episode: PodcastEpisodeModel
): Color {
    val context = LocalContext.current
    val url = episode.imageUrl

    var seed by remember(url) {
        mutableStateOf(Color(url?.let { artworkSeedCache.get(it) } ?: episode.imageSeedColor))
    }

    LaunchedEffect(url) {
        if(url == null || artworkSeedCache.get(url) != null) return@LaunchedEffect

        val result = SingletonImageLoader.get(context).execute(
            ImageRequest.Builder(context)
                .data(url)
                .size(64)
                .allowHardware(false)
                .build()
        )

        val color = withContext(Dispatchers.Default) {
            (result as? SuccessResult)?.image?.toBitmap()?.asImageBitmap()?.themeColorOrNull()
        } ?: return@LaunchedEffect

        artworkSeedCache.put(url, color.toArgb())
        seed = color
    }

    return seed
}
