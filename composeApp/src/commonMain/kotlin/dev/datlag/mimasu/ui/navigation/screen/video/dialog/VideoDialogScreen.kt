package dev.datlag.mimasu.ui.navigation.screen.video.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.swipeable
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import dev.datlag.mimasu.common.isFullyExpandedOrTargeted
import dev.datlag.mimasu.common.times
import dev.datlag.mimasu.ui.custom.WindowSize
import dev.datlag.mimasu.ui.custom.calculateWindowWidthSize
import dev.datlag.tooling.Platform
import dev.datlag.tooling.compose.ifFalse
import dev.datlag.tooling.compose.ifTrue
import dev.datlag.tooling.compose.platform.colorScheme
import dev.datlag.tooling.compose.platform.shapes
import dev.datlag.tooling.compose.platform.typography
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VideoDialogScreen(component: VideoDialogComponent) {
    val isCompactScreen = calculateWindowWidthSize() is WindowSize.Compact

    val collapsedHeight = remember { 76.dp }
    val density = LocalDensity.current
    val collapsedHeightPx = remember(collapsedHeight, density) { with(density) { collapsedHeight.toPx() } }

    var offsetY by remember(isCompactScreen) { mutableStateOf(0F) }
    var fullHeightPx by remember { mutableStateOf(0f) }

    val progress by remember(fullHeightPx, offsetY, collapsedHeightPx) { derivedStateOf {
        if (fullHeightPx > 0F) {
            1F - (offsetY / (fullHeightPx - collapsedHeightPx))
        } else {
            1F
        }
    } }

    val draggableState = rememberDraggableState { delta ->
        offsetY = (offsetY + delta).coerceIn(0F, fullHeightPx - collapsedHeightPx)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged {
                fullHeightPx = it.height.toFloat() - with(density) { 84.dp.toPx() }
            }
    ) {

        val padding by remember(progress) {
            derivedStateOf {
                lerp(8.dp, 0.dp, progress)
            }
        }

        val targetShape = Platform.shapes().small
        val shape by remember(progress) {
            derivedStateOf {
                val reversedProgress = abs(1F - progress)

                targetShape.copy(
                    topStart = targetShape.topStart.times(reversedProgress, density),
                    topEnd = targetShape.topEnd.times(reversedProgress, density),
                    bottomStart = targetShape.bottomStart.times(reversedProgress, density),
                    bottomEnd = targetShape.bottomEnd.times(reversedProgress, density),
                )
            }
        }
        val defaultRipple = LocalRippleConfiguration.current

        CompositionLocalProvider(
            LocalRippleConfiguration provides RippleConfiguration(
                color = Color.Transparent,
                rippleAlpha = RippleAlpha(0F, 0F, 0F, 0F)
            )
        ) {
            ElevatedCard(
                modifier = Modifier
                    .offset { IntOffset(0, offsetY.roundToInt()) }
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = collapsedHeight)
                    .fillMaxHeight(progress)
                    .padding(padding)
                    .background(Platform.colorScheme().background),
                shape = shape,
                enabled = progress <= 0.01F,
                onClick = {
                    offsetY = 0F
                }
            ) {
                CompositionLocalProvider(
                    LocalRippleConfiguration provides defaultRipple
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .draggable(
                                state = draggableState,
                                orientation = Orientation.Vertical,
                                enabled = isCompactScreen,
                                onDragStopped = {
                                    val nearestAnchor = listOf(0F, fullHeightPx - collapsedHeightPx).minByOrNull { abs(it - offsetY) } ?: 0F
                                    offsetY = nearestAnchor
                                }
                            ),
                        verticalArrangement = Arrangement.Center
                    ) {
                        val baseModifier = if (isCompactScreen) {
                            Modifier
                                .fillMaxWidth(max(lerp(0.33F, progress * 2F, progress), 0.33F))
                                .aspectRatio(16F/9F)
                        } else {
                            Modifier.fillMaxSize()
                        }

                        Box(
                            modifier = baseModifier
                        ) {
                            component.videoComponent.render()
                        }
                        Text(
                            modifier = Modifier.weight(1F),
                            text = "Hello World",
                            softWrap = true,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 2,
                            color = Platform.colorScheme().onBackground
                        )
                        AnimatedVisibility(
                            modifier = Modifier.fillMaxRowHeight(),
                            visible = progress <= 0.01F && fullHeightPx > 0F,
                            enter = fadeIn() + slideInHorizontally { it / 2 }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        component.videoComponent.togglePlayPause(false)
                                    }
                                ) {
                                    val playing by component.videoComponent.isPlaying.collectAsStateWithLifecycle()

                                    Icon(
                                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        component.dismiss()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}