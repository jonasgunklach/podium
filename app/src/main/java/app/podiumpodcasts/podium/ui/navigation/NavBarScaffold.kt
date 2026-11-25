package app.podiumpodcasts.podium.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldState
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.Composable
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import app.podiumpodcasts.podium.ui.component.common.SwitchableDynamicMaterialExpressiveTheme
import app.podiumpodcasts.podium.ui.vm.MediaPlayerViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavKey
import app.podiumpodcasts.podium.ui.helper.LocalSettingsRepository

@Composable
fun NavBarScaffold(
    layoutType: NavigationSuiteType =
        NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo()),
    state: NavigationSuiteScaffoldState,

    currentNavKey: @Composable () -> NavKey,
    onClickItem: (item: NavBarItems) -> Unit,
    content: @Composable () -> Unit
) {
    val currentNavKey = currentNavKey()

    val settingsRepository = LocalSettingsRepository.current
    val disableApplePodcastsApi = settingsRepository.privacy.disableApplePodcastsApi
        .collectAsState(false)

    // same palette as the mini player / player sheet: derive it from the now-playing artwork
    val vm = viewModel<MediaPlayerViewModel>()
    val enableArtworkColors = settingsRepository.appearance.enableArtworkColors.collectAsState(true)
    var playingScheme by remember { mutableStateOf<ColorScheme?>(null) }

    if(enableArtworkColors.value && vm.isPlayerVisible) {
        // ponytail: renders nothing, only hoists the theme's colors so we don't re-derive materialkolor params here
        SwitchableDynamicMaterialExpressiveTheme(
            enable = true,
            seedColor = Color(vm.metadataImageSeedColor ?: 1)
        ) {
            val scheme = MaterialTheme.colorScheme
            SideEffect { playingScheme = scheme }
        }
    } else {
        SideEffect { playingScheme = null }
    }

    val scheme = playingScheme ?: MaterialTheme.colorScheme
    val barColor by animateColorAsState(scheme.surfaceContainer)
    val railColor by animateColorAsState(scheme.surface)
    val indicatorColor by animateColorAsState(scheme.primaryContainer)
    val selectedColor by animateColorAsState(scheme.onPrimaryContainer)
    val unselectedColor by animateColorAsState(scheme.onSurfaceVariant)
    // label sits on the bar, not on the indicator
    val labelColor by animateColorAsState(scheme.onSurface)

    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = selectedColor,
            selectedTextColor = labelColor,
            indicatorColor = indicatorColor,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor
        ),
        navigationRailItemColors = NavigationRailItemDefaults.colors(
            selectedIconColor = selectedColor,
            selectedTextColor = labelColor,
            indicatorColor = indicatorColor,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor
        )
    )

    NavigationSuiteScaffold(
        layoutType = layoutType,
        state = state,
        containerColor = Color.Transparent,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = barColor,
            navigationRailContainerColor = railColor
        ),
        navigationSuiteItems = {
            NavBarItems.entries.forEach {
                if(disableApplePodcastsApi.value && it == NavBarItems.DISCOVER)
                    return@forEach

                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = stringResource(it.label)
                        )
                    },
                    label = { Text(stringResource(it.label)) },
                    selected = currentNavKey == it.navKey,
                    colors = itemColors,
                    onClick = {
                        onClickItem(it)
                    }
                )
            }
        },
        content = content
    )
}