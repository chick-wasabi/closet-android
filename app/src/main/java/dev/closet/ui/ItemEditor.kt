package dev.closet.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun ItemEditor(draft: ItemDraft, busy: Boolean, save: (Bitmap, String, String, String, String, String) -> Unit, cancel: () -> Unit) {
    var bitmap by remember(draft) { mutableStateOf(draft.cutout.copy(Bitmap.Config.ARGB_8888, true)) }
    var revision by remember(draft) { mutableIntStateOf(0) }
    var name by remember(draft) { mutableStateOf(draft.item?.name ?: "") }
    var category by remember(draft) { mutableStateOf(draft.item?.category ?: "top") }
    var color by remember(draft) { mutableStateOf(draft.item?.color ?: "neutral") }
    var formality by remember(draft) { mutableStateOf(draft.item?.formality ?: "casual") }
    var season by remember(draft) { mutableStateOf(draft.item?.season ?: "all") }
    var brush by remember { mutableStateOf(false) }
    var restore by remember { mutableStateOf(false) }
    var brushSize by remember { mutableFloatStateOf(32f) }
    var discard by remember { mutableStateOf(false) }
    val history = remember(draft) { mutableStateListOf<Bitmap>() }
    fun checkpoint() {
        if(history.size >= 5) history.removeAt(0).recycle()
        history.add(bitmap.copy(Bitmap.Config.ARGB_8888, true))
    }
    BackHandler(enabled = !busy) { discard = true }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { discard = true }) { Icon(Icons.Outlined.Close, null); Text("Cancel / skip") }
                TextButton(enabled = !busy, onClick = { save(bitmap, name, category, color, formality, season) }) { Text("Save piece") }
            }
            Heading("One piece at a time", if(draft.item == null) "Make it yours" else "Refine this piece", "Check the cutout, then give it a few details.")
        }
        draft.warning?.let { warning -> item {
            Surface(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) { Text(warning, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) }
        } }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                val ratio = bitmap.width.toFloat() / bitmap.height
                val drawWidth = minOf(maxWidth, 480.dp, 380.dp * ratio)
                Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    val currentRestore by rememberUpdatedState(restore)
                    val currentBrushSize by rememberUpdatedState(brushSize)
                    Canvas(Modifier.width(drawWidth).aspectRatio(ratio)
                        .pointerInput(brush, bitmap) {
                            if(!brush) return@pointerInput
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()
                                checkpoint()
                                val scale = bitmap.width.toFloat() / size.width
                                var last = down.position * scale
                                val canvas = AndroidCanvas(bitmap)
                                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                    strokeWidth = currentBrushSize * scale
                                    strokeCap = Paint.Cap.ROUND
                                    strokeJoin = Paint.Join.ROUND
                                    if(currentRestore) shader = BitmapShader(draft.original, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                                    else xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                                }
                                canvas.drawPoint(last.x, last.y, paint); revision++
                                do {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    val point = change.position * scale
                                    canvas.drawLine(last.x, last.y, point.x, point.y, paint)
                                    last = point; change.consume(); revision++
                                } while (event.changes.any { it.id == down.id && it.pressed })
                            }
                        }) {
                        // Revision is read during drawing so in-place brush edits invalidate the canvas.
                        @Suppress("UNUSED_VARIABLE") val redraw = revision
                        val tile = 12.dp.toPx()
                        drawRect(Color(0xFFF6F5F1))
                        for(y in 0..(size.height / tile).toInt()) for(x in 0..(size.width / tile).toInt()) {
                            if((x + y) % 2 == 0) drawRect(Color(0xFFDEDED7), Offset(x * tile, y * tile), androidx.compose.ui.geometry.Size(tile, tile))
                        }
                        drawImage(bitmap.asImageBitmap(), dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(brush, onClick = { brush = !brush }, label = { Text("Brush") }, leadingIcon = { Icon(Icons.Outlined.Brush, null) })
                    TextButton(enabled = history.isNotEmpty(), onClick = { bitmap = history.removeAt(history.lastIndex); revision++ }) { Text("Undo") }
                    TextButton(onClick = { checkpoint(); bitmap = draft.original.copy(Bitmap.Config.ARGB_8888, true); revision++ }) { Text("Keep original") }
                }
                if(brush) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(!restore, onClick = { restore = false }, label = { Text("Erase") })
                        FilterChip(restore, onClick = { restore = true }, label = { Text("Restore") })
                        TextButton(onClick = { checkpoint(); bitmap = draft.cutout.copy(Bitmap.Config.ARGB_8888, true); revision++ }) { Text("Reset cutout") }
                    }
                    Text("Brush size · ${brushSize.toInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(value = brushSize, onValueChange = { brushSize = it }, valueRange = 4f..90f)
                    Text("Draw on the image. Restore paints back the original photo. Turn Brush off to scroll over the image.", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Name (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Choice("Category", category, categories, onSelect = { category = it })
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Choice("Main color", color, colors, Modifier.weight(1f)) { color = it }
                    Choice("Formality", formality, formalities, Modifier.weight(1f)) { formality = it }
                }
                Choice("Season", season, seasons, onSelect = { season = it })
                Button(onClick = { save(bitmap, name, category, color, formality, season) }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Save to closet") }
            }
        }
    }
    if(discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard these edits?") }, text = { Text("This photo’s unsaved changes will be lost. If you selected more photos, you’ll move to the next one.") },
        confirmButton = { TextButton(onClick = { discard = false; cancel() }) { Text("Discard / next") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
}
