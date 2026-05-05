package com.m3u.tv

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.m3u.core.foundation.util.basic.title
import com.m3u.data.database.model.Channel
import com.m3u.data.database.model.Playlist
import com.m3u.i18n.R.string
import kotlinx.coroutines.yield

@Composable
fun TvBrowsePane(
    destination: TvDestination,
    state: TvUiState,
    onOpenLibrary: () -> Unit,
    onPlaylist: (Playlist) -> Unit,
    onRefresh: () -> Unit,
    onPlay: (Channel) -> Unit,
    onPlayRecent: () -> Unit,
    onSelectCategory: (String?) -> Unit,
    onSubscribeXtream: (title: String, basicUrl: String, username: String, password: String) -> Unit,
    onSubscribeM3u: (title: String, url: String) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (state.playlists.isEmpty()) {
            EmptyLibraryScreen(
                onSubscribeXtream = onSubscribeXtream,
                onSubscribeM3u = onSubscribeM3u,
            )
        } else {
            when (destination) {
                TvDestination.Home -> GuideScreen(
                    state = state,
                    onSelectCategory = onSelectCategory,
                    onPlay = onPlay,
                )

                TvDestination.Library -> LibraryScreen(
                    state = state,
                    onPlaylist = onPlaylist,
                    onRefresh = onRefresh,
                    onPlay = onPlay,
                    onSubscribeXtream = onSubscribeXtream,
                    onSubscribeM3u = onSubscribeM3u,
                )

                TvDestination.Favorites -> GuideScreen(
                    state = state.copy(selectedCategory = TvUiState.CATEGORY_FAVORITES),
                    onSelectCategory = onSelectCategory,
                    onPlay = onPlay,
                )

                TvDestination.Status -> StatusScreen(state)
            }
        }
    }
}

private const val CATEGORY_SEARCH = "__search__"

@Composable
private fun GuideScreen(
    state: TvUiState,
    onSelectCategory: (String?) -> Unit,
    onPlay: (Channel) -> Unit,
) {
    val firstCategoryFocusRequester = remember { FocusRequester() }
    val searchFocusRequester = remember { FocusRequester() }
    var initialFocusRequested by remember { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sidebarMode by rememberSaveable { mutableStateOf(false) } // true = search active

    val displayedChannels = remember(state.visibleChannels, searchQuery, sidebarMode) {
        if (sidebarMode && searchQuery.isNotBlank()) {
            val q = searchQuery.trim().lowercase()
            state.channels.filter { it.title.lowercase().contains(q) || it.category.lowercase().contains(q) }
        } else {
            state.visibleChannels
        }
    }

    LaunchedEffect(state.categories) {
        if (!initialFocusRequested && state.categories.isNotEmpty()) {
            yield()
            firstCategoryFocusRequester.requestFocus()
            initialFocusRequested = true
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
    ) {
        // ── Left: category sidebar ──────────────────────────────────────
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 16.dp, horizontal = 12.dp),
            modifier = Modifier
                .width(236.dp)
                .fillMaxHeight()
                .background(TvColors.Surface.copy(alpha = 0.72f))
                .focusGroup()
        ) {
            // ── Search entry ───────────────────────────────────────────
            item(key = CATEGORY_SEARCH) {
                SearchSidebarEntry(
                    query = searchQuery,
                    active = sidebarMode,
                    focusRequester = firstCategoryFocusRequester,
                    onActivate = {
                        sidebarMode = true
                        searchFocusRequester.requestFocus()
                    },
                    onQueryChange = { searchQuery = it },
                    onClear = { searchQuery = ""; sidebarMode = false },
                    searchFocusRequester = searchFocusRequester,
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Category list ──────────────────────────────────────────
            val allCategories = buildList {
                add(TvUiState.CATEGORY_FAVORITES to "⭐  Favorites")
                add(TvUiState.CATEGORY_ALL to "📺  All Channels")
                state.categories
                    .filter { it != TvUiState.CATEGORY_FAVORITES && it != TvUiState.CATEGORY_ALL }
                    .sorted()
                    .forEach { add(it to it) }
            }
            itemsIndexed(allCategories, key = { _, it -> it.first }) { _, (key, label) ->
                val isSelected = !sidebarMode && when {
                    state.selectedCategory == null && key == TvUiState.CATEGORY_ALL -> true
                    else -> state.selectedCategory == key
                }
                FocusFrame(
                    onClick = {
                        sidebarMode = false
                        searchQuery = ""
                        onSelectCategory(if (key == TvUiState.CATEGORY_ALL) null else key)
                    },
                    selected = isSelected,
                    shape = RoundedCornerShape(8.dp),
                    focusedScale = 1f,
                    modifier = Modifier.fillMaxWidth()
                ) { focused ->
                    Text(
                        text = label,
                        color = when {
                            focused || isSelected -> TvColors.OnFocus
                            else -> TvColors.TextPrimary
                        },
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = TvFonts.Body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
                Spacer(Modifier.height(2.dp))
            }
        }

        // ── Right: channel list ─────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            // header row
            val headerText = when {
                sidebarMode && searchQuery.isNotBlank() ->
                    "Search: \"$searchQuery\"  •  ${displayedChannels.size} results"
                state.selectedCategory == null -> "All Channels  •  ${displayedChannels.size}"
                state.selectedCategory == TvUiState.CATEGORY_FAVORITES -> "Favorites  •  ${displayedChannels.size}"
                else -> "${state.selectedCategory}  •  ${displayedChannels.size}"
            }
            Text(
                text = headerText,
                color = TvColors.TextSecondary,
                fontSize = 12.sp,
                fontFamily = TvFonts.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            )

            if (displayedChannels.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = if (sidebarMode && searchQuery.isNotBlank()) "No channels match \"$searchQuery\""
                               else "No channels in this category",
                        color = TvColors.TextMuted,
                        fontSize = 16.sp,
                        fontFamily = TvFonts.Body
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .focusGroup()
                ) {
                    itemsIndexed(displayedChannels, key = { _, ch -> ch.id }) { index, channel ->
                        ChannelListItem(
                            channel = channel,
                            onClick = { onPlay(channel) },
                            focusRequester = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchSidebarEntry(
    query: String,
    active: Boolean,
    focusRequester: FocusRequester,
    searchFocusRequester: FocusRequester,
    onActivate: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active) TvColors.Focus.copy(alpha = 0.18f)
                else TvColors.Surface.copy(alpha = 0.5f)
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (active) TvColors.Focus else Color.White.copy(alpha = 0.10f)
                ),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search",
            tint = if (active) TvColors.Focus else TvColors.TextMuted,
            modifier = Modifier.size(18.dp)
        )
        if (active) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                cursorBrush = SolidColor(TvColors.Focus),
                textStyle = TextStyle(
                    color = TvColors.TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = TvFonts.Body,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.moveFocus(FocusDirection.Right) }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(searchFocusRequester)
            )
            if (query.isNotEmpty()) {
                FocusFrame(
                    onClick = onClear,
                    shape = RoundedCornerShape(4.dp),
                    focusedScale = 1f,
                    modifier = Modifier.size(22.dp)
                ) { _ ->
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = TvColors.TextMuted,
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.Center)
                    )
                }
            }
        } else {
            Text(
                text = "Search channels…",
                color = TvColors.TextMuted,
                fontSize = 13.sp,
                fontFamily = TvFonts.Body,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { if (it.isFocused) onActivate() }
                    .focusable()
                    .clickable { onActivate() }
            )
        }
    }
}

@Composable
private fun FeaturedCarouselPane(
    state: TvUiState,
    channel: Channel?,
    primaryFocusRequester: FocusRequester,
    nextFocusRequester: FocusRequester,
    onOpenLibrary: () -> Unit,
    onPlayRecent: () -> Unit,
    onPlay: (Channel) -> Unit
) {
    val primaryHeroAction = {
        if (channel == null) {
            onOpenLibrary()
        } else if (channel == state.recent) {
            onPlayRecent()
        } else {
            onPlay(channel)
        }
    }
    var selectedAction by remember(channel?.id) { mutableStateOf(HeroAction.Primary) }
    val secondaryAvailable = channel != null
    val selectedHeroAction = if (secondaryAvailable) selectedAction else HeroAction.Primary

    FocusFrame(
        onClick = {
            when (selectedHeroAction) {
                HeroAction.Primary -> primaryHeroAction()
                HeroAction.Secondary -> onOpenLibrary()
            }
        },
        shape = RoundedCornerShape(16.dp),
        focusRequester = primaryFocusRequester,
        focusedScale = 1f,
        focusedBorderWidth = 0.dp,
        focusedBorderColor = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (channel == null) 280.dp else 288.dp)
            .focusProperties { down = nextFocusRequester },
        onKey = { event ->
            if (event.type != KeyEventType.KeyDown || !secondaryAvailable) {
                false
            } else {
                when (event.key) {
                    Key.DirectionLeft -> {
                        selectedAction = HeroAction.Primary
                        true
                    }
                    Key.DirectionRight -> {
                        selectedAction = HeroAction.Secondary
                        true
                    }
                    else -> false
                }
            }
        }
    ) { heroFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TvColors.BackgroundSoft)
        ) {
            if (channel != null) {
                PosterArt(
                    model = channel.cover,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.36f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            0f to Color.Black.copy(alpha = 0.92f),
                            0.48f to Color.Black.copy(alpha = 0.72f),
                            1f to Color.Transparent
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 48.dp, top = 32.dp, end = 48.dp, bottom = 32.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(0.54f)
                ) {
                    Text(
                        text = channel?.title?.title() ?: stringResource(string.tv_home_title),
                        color = TvColors.TextPrimary,
                        fontSize = 38.sp,
                        lineHeight = 42.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = TvFonts.Body,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = channel?.category?.takeIf { it.isNotBlank() }
                            ?: stringResource(string.tv_home_subtitle),
                        color = TvColors.TextSecondary,
                        fontSize = 17.sp,
                        lineHeight = 25.sp,
                        fontFamily = TvFonts.Body,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (channel == null) {
                            HeroActionChip(
                                text = stringResource(string.tv_action_open_library),
                                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                selected = heroFocused,
                                expanded = heroFocused
                            )
                        } else {
                            HeroActionChip(
                                text = stringResource(string.tv_action_resume),
                                icon = Icons.Rounded.PlayArrow,
                                selected = heroFocused && selectedHeroAction == HeroAction.Primary,
                                expanded = heroFocused && selectedHeroAction == HeroAction.Primary
                            )
                            HeroActionChip(
                                text = stringResource(string.tv_action_open_library),
                                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                selected = heroFocused && selectedHeroAction == HeroAction.Secondary,
                                expanded = heroFocused && selectedHeroAction == HeroAction.Secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class HeroAction {
    Primary,
    Secondary
}

@Composable
private fun HeroActionChip(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(if (expanded) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) TvColors.Focus else TvColors.Surface.copy(alpha = 0.86f))
            .border(
                BorderStroke(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) Color.White else Color.White.copy(alpha = 0.08f)
                ),
                RoundedCornerShape(24.dp)
            )
            .padding(horizontal = if (expanded) 16.dp else 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) TvColors.OnFocus else TvColors.TextPrimary,
            modifier = Modifier.size(24.dp)
        )
        if (expanded) {
            Text(
                text = text,
                color = if (selected) TvColors.OnFocus else TvColors.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TvFonts.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LibraryScreen(
    state: TvUiState,
    onPlaylist: (Playlist) -> Unit,
    onRefresh: () -> Unit,
    onPlay: (Channel) -> Unit,
    onSubscribeXtream: (title: String, basicUrl: String, username: String, password: String) -> Unit,
    onSubscribeM3u: (title: String, url: String) -> Unit,
) {
    val playlistFocusRequester = remember { FocusRequester() }
    val focusTarget = state.selectedPlaylist ?: state.playlists.firstOrNull()
    var initialFocusRequested by remember { mutableStateOf(false) }
    var showSubscribeDialog by rememberSaveable { mutableStateOf(false) }

    if (showSubscribeDialog) {
        SubscribeDialog(
            onSubscribeXtream = { t, b, u, p ->
                onSubscribeXtream(t, b, u, p)
                showSubscribeDialog = false
            },
            onSubscribeM3u = { t, u ->
                onSubscribeM3u(t, u)
                showSubscribeDialog = false
            },
            onDismiss = { showSubscribeDialog = false }
        )
    }

    LaunchedEffect(Unit) {
        if (focusTarget != null && !initialFocusRequested) {
            yield()
            playlistFocusRequester.requestFocus()
            initialFocusRequested = true
        }
    }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(start = 48.dp, top = 48.dp, end = 64.dp, bottom = 48.dp),
        modifier = Modifier
            .fillMaxSize()
            .focusGroup()
    ) {
        item {
            SectionTitle(
                title = stringResource(string.tv_library_title),
                subtitle = stringResource(string.tv_library_subtitle)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 8.dp),
                modifier = Modifier.focusGroup()
            ) {
                items(state.playlists, key = { it.url }) { playlist ->
                    PlaylistCard(
                        playlist = playlist,
                        count = state.counts[playlist] ?: 0,
                        selected = playlist == state.selectedPlaylist,
                        onClick = { onPlaylist(playlist) },
                        focusRequester = if (playlist.url == focusTarget?.url) playlistFocusRequester else null,
                        modifier = Modifier
                            .widthIn(min = 256.dp, max = 336.dp)
                            .height(144.dp)
                    )
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.selectedPlaylist?.title?.title().orEmpty(),
                        color = TvColors.TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = TvFonts.Body,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(string.tv_channel_count, state.channels.size),
                        color = TvColors.TextSecondary,
                        fontSize = 14.sp,
                        fontFamily = TvFonts.Body,
                        maxLines = 1
                    )
                }
                TvActionButton(
                    text = stringResource(string.feat_setting_label_subscribe),
                    icon = Icons.Rounded.Refresh,
                    onClick = onRefresh
                )
                TvActionButton(
                    text = "Add Playlist",
                    icon = Icons.Rounded.Add,
                    onClick = { showSubscribeDialog = true }
                )
            }
        }

        item {
            ChannelGrid(
                channels = state.channels,
                onPlay = onPlay,
                modifier = Modifier.height(620.dp)
            )
        }
    }
}

@Composable
private fun ChannelGridScreen(
    title: String,
    subtitle: String,
    channels: List<Channel>,
    onPlay: (Channel) -> Unit
) {
    val firstChannelFocusRequester = remember { FocusRequester() }

    LaunchedEffect(channels.size) {
        if (channels.isNotEmpty()) {
            yield()
            firstChannelFocusRequester.requestFocus()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 48.dp, top = 48.dp, end = 64.dp, bottom = 48.dp)
            .focusGroup()
    ) {
        SectionTitle(title = title, subtitle = subtitle)
        ChannelGrid(
            channels = channels,
            onPlay = onPlay,
            firstItemFocusRequester = firstChannelFocusRequester
        )
    }
}

@Composable
private fun StatusScreen(state: TvUiState) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 48.dp, top = 48.dp, end = 64.dp, bottom = 48.dp)
    ) {
        SectionTitle(
            title = stringResource(string.tv_settings_title),
            subtitle = stringResource(string.tv_settings_subtitle)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricTile(
                title = stringResource(string.tv_metric_playlists),
                value = state.playlists.size.toString(),
                icon = Icons.Rounded.VideoLibrary,
                modifier = Modifier
                    .weight(1f)
                    .height(136.dp)
            )
            MetricTile(
                title = stringResource(string.tv_metric_channels),
                value = state.channelCount.toString(),
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                modifier = Modifier
                    .weight(1f)
                    .height(136.dp)
            )
            MetricTile(
                title = stringResource(string.tv_metric_favorites),
                value = state.favorites.size.toString(),
                icon = Icons.Rounded.Favorite,
                modifier = Modifier
                    .weight(1f)
                    .height(136.dp)
            )
        }
    }
}

@Composable
private fun ContentRow(
    channels: List<Channel>,
    onPlay: (Channel) -> Unit,
    onFocused: (Channel) -> Unit = {},
    firstItemFocusRequester: FocusRequester? = null
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 48.dp, top = 8.dp, end = 48.dp, bottom = 8.dp),
        modifier = Modifier.focusGroup()
    ) {
        itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                onPlay = { onPlay(channel) },
                onFocused = { onFocused(channel) },
                focusRequester = firstItemFocusRequester.takeIf { index == 0 },
                compact = true,
                modifier = Modifier
                    .widthIn(min = 104.dp, max = 120.dp)
                    .aspectRatio(2f / 3f)
            )
        }
    }
}

@Composable
private fun ChannelGrid(
    channels: List<Channel>,
    onPlay: (Channel) -> Unit,
    modifier: Modifier = Modifier.fillMaxSize(),
    firstItemFocusRequester: FocusRequester? = null
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(168.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
        modifier = modifier.focusGroup()
    ) {
        itemsIndexed(channels, key = { _, channel -> channel.id }) { index, channel ->
            ChannelCard(
                channel = channel,
                onPlay = { onPlay(channel) },
                focusRequester = firstItemFocusRequester.takeIf { index == 0 }
            )
        }
    }
}

@Composable
private fun EmptyLibraryScreen(
    onSubscribeXtream: (title: String, basicUrl: String, username: String, password: String) -> Unit,
    onSubscribeM3u: (title: String, url: String) -> Unit,
) {
    var showSubscribeDialog by rememberSaveable { mutableStateOf(false) }

    if (showSubscribeDialog) {
        SubscribeDialog(
            onSubscribeXtream = { t, b, u, p ->
                onSubscribeXtream(t, b, u, p)
                showSubscribeDialog = false
            },
            onSubscribeM3u = { t, u ->
                onSubscribeM3u(t, u)
                showSubscribeDialog = false
            },
            onDismiss = { showSubscribeDialog = false }
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(string.tv_home_title),
                color = TvColors.TextPrimary,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TvFonts.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(string.tv_empty_library_title),
                color = TvColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = TvFonts.Body,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(string.tv_empty_library_subtitle),
                color = TvColors.TextSecondary,
                fontSize = 17.sp,
                lineHeight = 25.sp,
                fontFamily = TvFonts.Body,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.82f)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .widthIn(max = 420.dp)
            ) {
                InfoPill(text = stringResource(string.tv_empty_library_phone_hint), modifier = Modifier.fillMaxWidth())
                InfoPill(text = stringResource(string.tv_empty_library_restore_hint), modifier = Modifier.fillMaxWidth())
            }
            TvActionButton(
                text = "Add Playlist",
                icon = Icons.Rounded.Add,
                onClick = { showSubscribeDialog = true },
                modifier = Modifier.widthIn(min = 180.dp)
            )
        }
        SetupPanel(
            modifier = Modifier
                .weight(0.88f)
                .widthIn(max = 420.dp)
        )
    }
}

@Composable
private fun SetupPanel(modifier: Modifier = Modifier) {
    FocusFrame(
        onClick = {},
        enabled = false,
        modifier = Modifier
            .then(modifier)
            .aspectRatio(1.18f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        0f to TvColors.SurfaceRaised,
                        1f to TvColors.BackgroundSoft
                    )
                )
                .padding(24.dp)
        ) {
            SectionTitle(
                title = stringResource(string.tv_empty_library_panel_title),
                subtitle = stringResource(string.tv_empty_library_panel_subtitle)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SetupStep(text = stringResource(string.tv_empty_library_step_sources))
                SetupStep(text = stringResource(string.tv_empty_library_step_sync))
                SetupStep(text = stringResource(string.tv_empty_library_step_watch))
            }
        }
    }
}

@Composable
private fun SetupStep(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(TvColors.Focus)
        )
        Text(
            text = text,
            color = TvColors.TextSecondary,
            fontSize = 14.sp,
            fontFamily = TvFonts.Body,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SubscribeDialog(
    onSubscribeXtream: (title: String, basicUrl: String, username: String, password: String) -> Unit,
    onSubscribeM3u: (title: String, url: String) -> Unit,
    onDismiss: () -> Unit,
    initialTitle: String = "",
    initialBasicUrl: String = "",
    initialUsername: String = "",
    initialPassword: String = "",
) {
    var isXtream by rememberSaveable { mutableStateOf(true) }
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var basicUrl by rememberSaveable { mutableStateOf(initialBasicUrl) }
    var username by rememberSaveable { mutableStateOf(initialUsername) }
    var password by rememberSaveable { mutableStateOf(initialPassword) }
    var m3uUrl by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .widthIn(max = 560.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(TvColors.Surface)
                .padding(32.dp)
        ) {
            Text(
                text = "Add Playlist",
                color = TvColors.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TvFonts.Body
            )

            // Source type toggle
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvActionButton(
                    text = "Xtream",
                    icon = Icons.Rounded.VideoLibrary,
                    onClick = { isXtream = true },
                    showTextWhenUnfocused = true,
                    modifier = if (isXtream) Modifier.border(
                        2.dp, TvColors.Focus, RoundedCornerShape(24.dp)
                    ) else Modifier
                )
                TvActionButton(
                    text = "M3U URL",
                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                    onClick = { isXtream = false },
                    showTextWhenUnfocused = true,
                    modifier = if (!isXtream) Modifier.border(
                        2.dp, TvColors.Focus, RoundedCornerShape(24.dp)
                    ) else Modifier
                )
            }

            // Fields
            TvInputField(
                value = title,
                onValueChange = { title = it },
                placeholder = "Title",
                imeAction = ImeAction.Next,
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
            if (isXtream) {
                TvInputField(
                    value = basicUrl,
                    onValueChange = { basicUrl = it },
                    placeholder = "Server URL  (e.g. http://server.com:port)",
                    imeAction = ImeAction.Next,
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
                TvInputField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = "Username",
                    imeAction = ImeAction.Next,
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
                TvInputField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Password",
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onDone = {
                        if (title.isNotBlank() && basicUrl.isNotBlank() && username.isNotBlank()) {
                            onSubscribeXtream(title, basicUrl, username, password)
                        }
                    }
                )
            } else {
                TvInputField(
                    value = m3uUrl,
                    onValueChange = { m3uUrl = it },
                    placeholder = "M3U URL",
                    imeAction = ImeAction.Done,
                    onDone = {
                        if (title.isNotBlank() && m3uUrl.isNotBlank()) {
                            onSubscribeM3u(title, m3uUrl)
                        }
                    }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TvActionButton(
                    text = "Subscribe",
                    icon = Icons.Rounded.Add,
                    onClick = {
                        if (isXtream) {
                            if (title.isNotBlank() && basicUrl.isNotBlank() && username.isNotBlank()) {
                                onSubscribeXtream(title, basicUrl, username, password)
                            }
                        } else {
                            if (title.isNotBlank() && m3uUrl.isNotBlank()) {
                                onSubscribeM3u(title, m3uUrl)
                            }
                        }
                    }
                )
                TvActionButton(
                    text = "Cancel",
                    icon = Icons.Rounded.Favorite,
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun TvInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
    onDone: () -> Unit = {},
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(
            color = TvColors.TextPrimary,
            fontSize = 15.sp,
            fontFamily = TvFonts.Body
        ),
        cursorBrush = SolidColor(TvColors.Focus),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onNext = { onNext() },
            onDone = { onDone() }
        ),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(TvColors.SurfaceRaised)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = TvColors.TextMuted,
                    fontSize = 15.sp,
                    fontFamily = TvFonts.Body,
                    maxLines = 1
                )
            }
            inner()
        }
    )
}