package com.m3u.tv

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Text
import com.m3u.data.tv.model.keyCode

@Composable
fun App(
    onBackPressed: () -> Unit,
    viewModel: TvHomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val player by viewModel.player.collectAsStateWithLifecycle()
    val currentChannel by viewModel.currentChannel.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val view = LocalView.current
    var destination by remember { mutableStateOf(TvDestination.Home) }
    var surface by remember { mutableStateOf(TvSurface.Browse) }
    val closePlayer = {
        viewModel.releasePlayer()
        surface = TvSurface.Browse
    }

    // Auto-subscribe using local.properties credentials if no playlists exist yet
    LaunchedEffect(state.playlists) {
        if (state.playlists.isEmpty()) {
            val basicUrl = DevDefaults.XTREAM_BASIC_URL
            val username = DevDefaults.XTREAM_USERNAME
            val password = DevDefaults.XTREAM_PASSWORD
            val title = DevDefaults.XTREAM_TITLE
            if (!basicUrl.isNullOrBlank() && !username.isNullOrBlank() && !title.isNullOrBlank()) {
                viewModel.subscribeXtream(title, basicUrl, username, password.orEmpty())
            }
        }
    }

    // Auto-play last watched channel on first launch
    var autoPlayDone by remember { mutableStateOf(false) }
    LaunchedEffect(state.recent) {
        if (!autoPlayDone && state.recent != null && surface == TvSurface.Browse) {
            autoPlayDone = true
            viewModel.play(state.recent!!)
            surface = TvSurface.Player
        }
    }

    BackHandler {
        when (surface) {
            TvSurface.Player -> surface = TvSurface.Guide
            TvSurface.Guide -> closePlayer()
            else -> onBackPressed()
        }
    }

    LaunchedEffect(view) {
        viewModel.remoteDirections.collect { direction ->
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, direction.keyCode))
            view.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, direction.keyCode))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvColors.Background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        TvBackdrop(channel = currentChannel ?: state.heroChannel)
        Row(Modifier.fillMaxSize()) {
            TvNavigationRail(
                selected = destination,
                onSelect = { destination = it }
            )
            TvBrowsePane(
                destination = destination,
                state = state,
                onOpenLibrary = { destination = TvDestination.Library },
                onPlaylist = {
                    viewModel.selectPlaylist(it)
                    destination = TvDestination.Library
                },
                onRefresh = viewModel::refreshSelectedPlaylist,
                onPlay = {
                    viewModel.play(it)
                    surface = TvSurface.Player
                },
                onPlayRecent = {
                    viewModel.playRecent()
                    surface = TvSurface.Player
                },
                onSubscribeXtream = viewModel::subscribeXtream,
                onSubscribeM3u = viewModel::subscribeM3u,
                onSelectCategory = viewModel::selectCategory,
                onToggleFavorite = viewModel::toggleFavorite,
            )
        }

        AnimatedVisibility(
            visible = surface == TvSurface.Player || surface == TvSurface.Guide,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TvPlayerScreen(
                player = player,
                channel = currentChannel,
                isPlaying = isPlaying,
                playbackState = playbackState,
                onPlayPause = { viewModel.pauseOrContinue(!isPlaying) },
                onBack = { surface = TvSurface.Guide },
                onClose = closePlayer
            )
        }

        AnimatedVisibility(
            visible = surface == TvSurface.Guide,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            EpgOverlayScreen(
                player = player,
                currentChannel = currentChannel,
                currentProgramme = state.currentProgramme,
                channels = state.visibleChannels,
                onSelectChannel = {
                    viewModel.play(it)
                    surface = TvSurface.Player
                },
                onToggleFavorite = viewModel::toggleFavorite,
                onClose = closePlayer
            )
        }


    }
}