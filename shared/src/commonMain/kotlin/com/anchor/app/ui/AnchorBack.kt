package com.anchor.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
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
 * Shared so every screen sits at the same height; heightIn lets large fonts grow instead of clipping.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun AnchorBackRow(
    onBack: () -> Unit,
    label: String = "返回",
) {
    BackHandler(onBack = onBack)
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = AnchorBackBarHeight)
            .padding(start = 4.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.heightIn(min = AnchorBackBarHeight),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        ) {
            Icon(AnchorBackIcon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, fontSize = 16.sp)
        }
    }
}

@Composable
internal fun AnchorBackBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "返回",
    content: @Composable () -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        AnchorBackRow(onBack = onBack, label = label)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            content()
        }
    }
}
