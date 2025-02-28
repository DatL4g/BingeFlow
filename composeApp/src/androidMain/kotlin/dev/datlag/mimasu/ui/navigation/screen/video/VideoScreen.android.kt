package dev.datlag.mimasu.ui.navigation.screen.video

import android.graphics.Rect
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toRect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.cast.CastPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.text.TextOutput
import dev.datlag.kast.Kast
import dev.datlag.mimasu.common.calculateAspectRatio
import dev.datlag.mimasu.common.detectPinchGestures
import dev.datlag.mimasu.common.handleDPadKeyEvents
import dev.datlag.mimasu.common.handlePlayerShortcuts
import dev.datlag.mimasu.common.merge
import dev.datlag.mimasu.common.rememberCronetEngine
import dev.datlag.mimasu.core.MimasuConnection
import dev.datlag.mimasu.other.PiPHelper
import dev.datlag.mimasu.ui.navigation.screen.video.components.BottomControls
import dev.datlag.mimasu.ui.navigation.screen.video.components.CenterControls
import dev.datlag.mimasu.ui.navigation.screen.video.components.SubTitles
import dev.datlag.mimasu.ui.navigation.screen.video.components.TopControls
import dev.datlag.mimasu.ui.navigation.screen.video.components.VolumeBrightnessControl
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flowOf
import org.kodein.di.compose.rememberInstance
import org.kodein.di.compose.withDI
import kotlin.math.max
import kotlin.math.min

@OptIn(UnstableApi::class)
@Composable
actual fun VideoScreen(component: VideoComponent) = withDI(component.di) {
    val mediaItem = remember {
        MediaItem.Builder()
            //.setUri("https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8")
            .setUri("https://stream.mux.com/HDGj01zK01esWsWf9WJj5t5yuXQZJFF6bo.m3u8")
            .build()
    }

    val context = LocalContext.current
    val windowController = rememberWindowController()

    val controller = remember(component.controller) { component.controller as VideoPlayerState }
    val playerWrapper = remember(controller) { controller.getPlayer<PlayerWrapper>() }
    val player = remember(playerWrapper, controller) { playerWrapper ?: controller.getPlayer<Player>()!! }

    LaunchedEffect(playerWrapper) {
        playerWrapper?.onFirstFrame {
            windowController.isSystemBarsVisible = false
            windowController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            windowController.addWindowFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    LaunchedEffect(player, mediaItem) {
        player.setMediaItem(mediaItem)
        player.prepare()
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    val videoPlayerSecure by MimasuConnection.isVideoPlayerSecure.collectAsStateWithLifecycle()

    val aspectRatio by remember(playerWrapper, player) {
        playerWrapper?.aspectRatio ?: flowOf(player.calculateAspectRatio())
    }.collectAsStateWithLifecycle(player.calculateAspectRatio())

    val isCasting by remember(playerWrapper, player) {
        playerWrapper?.usingCastPlayer ?: flowOf(player is CastPlayer)
    }.collectAsStateWithLifecycle(player is CastPlayer)

    val pipHelper = remember(context) { PiPHelper(context) }
    val pipActive by PiPHelper.active.collectAsStateWithLifecycle()
    var videoViewBounds by remember {
        mutableStateOf(Rect())
    }

    LaunchedEffect(videoPlayerSecure) {
        if (videoPlayerSecure) {
            windowController.addWindowFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            windowController.clearWindowFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    var isZoomed by remember(isCasting) {
        mutableStateOf(false)
    }
    var zoom by remember(isCasting) {
        mutableFloatStateOf(1F)
    }

    LaunchedEffect(controller) {
        controller.poll()
    }

    LaunchedEffect(controller) {
        controller.observe()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopControls(
                state = controller,
                pipActive = pipActive
            )
        },
        bottomBar = {
            BottomControls(
                state = controller,
                pipActive = pipActive
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectPinchGestures(
                        pass = PointerEventPass.Initial,
                        onGesture = { _, newZoom ->
                            zoom *= newZoom
                        },
                        onGestureEnd = {
                            if (zoom > 1.2F) {
                                isZoomed = true
                            } else if (zoom < 0.8F) {
                                isZoomed = false
                            }

                            zoom = 1F
                        }
                    )
                }
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val sizeModifier = if (isZoomed) {
                Modifier.fillMaxSize().scale(min(max(zoom, 0.75F), 1F))
            } else {
                Modifier.aspectRatio(aspectRatio).scale(max(zoom, 0.95F))
            }
            val controlsFocus = remember { FocusRequester() }

            AndroidExternalSurface(
                modifier = sizeModifier.background(Color.Black).onGloballyPositioned {
                    videoViewBounds = it.boundsInWindow().toAndroidRectF().toRect()
                },
                isSecure = videoPlayerSecure,
                onInit = {
                    onSurface { surface, _, _ ->
                        player.setVideoSurface(surface)

                        surface.onChanged { _, _ ->
                            player.setVideoSurface(surface)
                        }
                        surface.onDestroyed {
                            player.clearVideoSurface()
                        }
                    }
                }
            )

            SubTitles(
                state = controller,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(contentPadding.merge(PaddingValues(bottom = 16.dp)))
                    .background(Color.Black.copy(alpha = 0.5F), MaterialTheme.shapes.small)
                    .padding(8.dp)
            )

            VolumeBrightnessControl(
                state = controller,
                modifier = Modifier.matchParentSize(),
                contentPadding = contentPadding.merge(PaddingValues(top = 16.dp)),
            )

            CenterControls(
                state = controller,
                shownInDialog = component.shownInDialog,
                modifier = Modifier
                    .matchParentSize()
                    .handleDPadKeyEvents(controller)
                    .handlePlayerShortcuts(controller)
                    .focusRequester(controlsFocus)
                    .focusable()
            )

            LaunchedEffect(Unit) {
                controlsFocus.requestFocus()
            }
        }
    }

    LaunchedEffect(pipHelper) {
        PiPHelper.onEnter {
            pipHelper.enter(aspectRatio, videoViewBounds)
        }
    }

    DisposableEffect(PiPHelper) {
        onDispose {
            videoViewBounds = Rect()
            PiPHelper.clearEnter()
        }
    }

    DisposableEffect(windowController) {
        onDispose {
            windowController.isSystemBarsVisible = true
            windowController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
            windowController.clearWindowFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON and WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}

