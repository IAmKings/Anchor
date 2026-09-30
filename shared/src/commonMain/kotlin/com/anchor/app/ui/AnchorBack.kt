package com.anchor.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val AnchorBackIcon: ImageVector = ImageVector.Builder(
    name = "Back",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    addPath(
        pathData = PathParser().parsePathString(
            "M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z",
        ).toNodes(),
        fill = SolidColor(Color.Black),
    )
}.build()

internal val AnchorBackBarHeight = 48.dp

/**
 * Page exit row. Flush under the status bar, start-aligned, at least 48.dp.
 * A short [title] is centered on the full row at 17.sp. Both sides keep the measured
 * back-control width so the title does not sit in the leftover space or cover the control.
 * heightIn lets large fonts grow instead of clipping; a long title ellipsizes.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AnchorBackRow(
    onBack: () -> Unit,
    label: String = "返回",
    title: String? = null,
    titleIsHeading: Boolean = true,
) {
    BackHandler(onBack = onBack)
    val density = LocalDensity.current
    SubcomposeLayout(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = AnchorBackBarHeight)
            .padding(start = 4.dp),
    ) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val back = subcompose("back") {
            TextButton(
                onClick = onBack,
                modifier = Modifier.heightIn(min = AnchorBackBarHeight),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Icon(AnchorBackIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(label, fontSize = 16.sp)
            }
        }.first().measure(loose)
        val pageTitle = title?.takeIf { it.isNotBlank() }?.let { text ->
            val side = with(density) { back.width.toDp() }
            subcompose("title") {
                Text(
                    text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = side)
                        .then(if (titleIsHeading) Modifier.semantics { heading() } else Modifier),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }.first().measure(loose)
        }
        val height = maxOf(constraints.minHeight, back.height, pageTitle?.height ?: 0)
        layout(constraints.maxWidth, height) {
            pageTitle?.placeRelative(0, (height - pageTitle.height) / 2)
            back.placeRelative(0, (height - back.height) / 2)
        }
    }
}

@Composable
internal fun AnchorBackBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "返回",
    title: String? = null,
    titleIsHeading: Boolean = true,
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        AnchorBackRow(onBack = onBack, label = label, title = title, titleIsHeading = titleIsHeading)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}
