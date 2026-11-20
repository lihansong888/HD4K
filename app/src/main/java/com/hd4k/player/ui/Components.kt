package com.hd4k.player.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hd4k.player.data.Item

/** 遥控器焦点：获得焦点时放大并显示金色描边；同时支持触摸点击 */
@Composable
fun Modifier.tvClickable(
    radius: Int = 12,
    focusScale: Float = 1.06f,
    onClick: () -> Unit,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (focused) focusScale else 1f, label = "focusScale")
    val shape = RoundedCornerShape(radius.dp)
    return this
        .scale(s)
        .clip(shape)
        .border(BorderStroke(if (focused) 3.dp else 0.dp, if (focused) Accent else Color.Transparent), shape)
        .onFocusChanged { focused = it.isFocused }
        .clickable(onClick = onClick)
}

@Composable
fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .tvClickable(radius = 20, focusScale = 1.08f, onClick = onClick)
            .background(if (selected) Accent else Panel)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (selected) Color.Black else Color.White, maxLines = 1)
    }
}

@Composable
fun PosterCard(item: Item, onClick: () -> Unit) {
    Column(Modifier.padding(6.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .tvClickable(onClick = onClick)
                .background(Panel),
        ) {
            if (item.poster.isNotBlank()) {
                AsyncImage(
                    model = item.poster, contentDescription = item.title,
                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(item.title, Modifier.align(Alignment.Center).padding(8.dp), color = TextDim)
            }
            if (item.remark.isNotBlank()) {
                Text(
                    item.remark,
                    Modifier
                        .align(Alignment.TopEnd)
                        .background(Accent)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    color = Color.Black, style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Text(
            item.title, Modifier.padding(top = 6.dp, start = 2.dp),
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
