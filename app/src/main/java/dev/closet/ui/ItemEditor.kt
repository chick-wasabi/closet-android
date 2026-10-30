package dev.closet.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable
fun ItemEditor(draft: ItemDraft, busy: Boolean, save: (Bitmap, String, String, String, String, String) -> Unit, cancel: () -> Unit,
               saveDetails: ((EditResult) -> Unit)? = null, initialCleanup: Boolean = false) {
    var bitmap by remember(draft) { mutableStateOf(draft.cutout.copy(Bitmap.Config.ARGB_8888, true)) }
    var original by remember(draft) { mutableStateOf(draft.original.copy(Bitmap.Config.ARGB_8888, true)) }
    var cleanup by remember(draft) { mutableStateOf(false) }
    var tone by remember(draft) { mutableStateOf(false) }
    var before by remember(draft) { mutableStateOf(false) }
    var cleaning by remember(draft) { mutableStateOf(false) }
    var cleanPair by remember(draft) { mutableStateOf<Pair<Bitmap,Bitmap>?>(null) }
    val scope = rememberCoroutineScope()
    var error by remember(draft) { mutableStateOf<String?>(null) }
    var subcategory by remember(draft) { mutableStateOf(draft.item?.subcategory ?: "t-shirt") }
    var pattern by remember(draft) { mutableStateOf(draft.item?.pattern ?: "solid") }
    var brand by remember(draft) { mutableStateOf(draft.item?.brand ?: "") }
    var auto by remember(draft) { mutableStateOf(draft.item?.autoFields?.split(",")?.toSet() ?: emptySet()) }
    fun label(field: String, title: String) = title + if(field in auto) " · Auto" else ""
    fun manual(field: String) { auto = auto - field }
    fun refreshCleanup() {
        if(cleaning) return
        scope.launch {
            cleaning = true
            try { cleanPair = withContext(Dispatchers.Default) { dev.closet.processing.PhotoProcessing.clean(original, bitmap, tone) } }
            catch(e: Exception) { error = e.message ?: "Cleanup unavailable"; cleanup = false }
            finally { cleaning = false }
        }
    }
    LaunchedEffect(draft) { if(initialCleanup) { cleanup=true; refreshCleanup() } }
    var revision by remember(draft) { mutableIntStateOf(0) }
    var name by remember(draft) { mutableStateOf(draft.item?.name ?: "") }
    var category by remember(draft) { mutableStateOf(draft.item?.category ?: "top") }
    var color by remember(draft) { mutableStateOf(draft.item?.color ?: "neutral") }
    var formality by remember(draft) { mutableStateOf(draft.item?.formality ?: "casual") }
    var season by remember(draft) { mutableStateOf(draft.item?.season ?: "all") }
    fun commit() {
        val pair = if(cleanup) cleanPair else null
        val result=pair?.second ?: bitmap; val raw=pair?.first ?: original
        val base=draft.item ?: dev.closet.data.ClothingItem(java.util.UUID.randomUUID().toString(),imagePath="",originalPath="")
        if(saveDetails!=null) saveDetails(EditResult(raw,result,base.copy(name=name,category=category,color=color,formality=formality,season=season,subcategory=subcategory,pattern=pattern,brand=brand,autoFields=auto.joinToString(","))))
        else save(result,name,category,color,formality,season)
    }
    fun transform(turns: Int=0,flip: Boolean=false) {
        original=dev.closet.processing.PhotoProcessing.transform(original,turns,flip)
        bitmap=dev.closet.processing.PhotoProcessing.transform(bitmap,turns,flip)
        cleanup=false; cleanPair=null
    }
    val context=androidx.compose.ui.platform.LocalContext.current
    val scan=androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()) { uri ->
        if(uri!=null) scope.launch {
            cleaning=true
            try { val photo=dev.closet.imaging.ImageStore(context).import(uri)
                val text=withTimeout(15000){dev.closet.processing.PhotoProcessing.labelText(photo)}
                if(text.isBlank()) error="No label text found. Type it instead." else {brand=text;manual("brand")}
            } catch(_:Exception) { error="Could not scan this label. Type it instead." } finally {cleaning=false}
        }
    }
    var brush by remember(draft) { mutableStateOf(false) }
    var restore by remember(draft) { mutableStateOf(false) }
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
                IconButton(onClick = { discard = true }) { Icon(Icons.Outlined.Close, "Cancel edits") }
                TextButton(enabled = !busy && !cleaning, onClick = { commit() }) { Text("Save") }
            }
            Heading("Edit clothes")
        }
        draft.warning?.let { warning -> item {
            Surface(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) { Text(warning, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium) }
        } }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                val displayed = if(before) original else if(cleanup) cleanPair?.second ?: bitmap else bitmap
                val ratio = displayed.width.toFloat() / displayed.height
                val drawWidth = minOf(maxWidth, 480.dp, 380.dp * ratio)
                Box(Modifier.fillMaxWidth(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    val currentRestore by rememberUpdatedState(restore)
                    val currentBrushSize by rememberUpdatedState(brushSize)
                    Canvas(Modifier.clip(ClosetShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, ClosetShape).width(drawWidth).aspectRatio(ratio).testTag("cutoutCanvas")
                        .pointerInput(brush, bitmap, cleanup, before) {
                            if(!brush || cleanup || before) return@pointerInput
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
                                    if(currentRestore) shader = BitmapShader(original, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
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
                        drawRect(Color(0xFFFFF7FB))
                        for(y in 0..(size.height / tile).toInt()) for(x in 0..(size.width / tile).toInt()) {
                            if((x + y) % 2 == 0) drawRect(Color(0xFFF0E3EE), Offset(x * tile, y * tile), androidx.compose.ui.geometry.Size(tile, tile))
                        }
                        drawImage((if(before) original else if(cleanup) cleanPair?.second ?: bitmap else bitmap).asImageBitmap(), dstSize = IntSize(size.width.toInt(), size.height.toInt()))
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(enabled=!cleaning, onClick={ history.clear(); transform(-1) }) { Icon(Icons.Outlined.RotateLeft,"Rotate left") }
                    TextButton(enabled=!cleaning,onClick={ history.clear(); transform(1) }) { Icon(Icons.Outlined.RotateRight,"Rotate right") }
                    TextButton(enabled=!cleaning,onClick={ history.clear(); transform(flip=true) }) { Icon(Icons.Outlined.Flip,"Flip horizontal") }
                    TextButton(enabled=!cleaning,onClick={ original=draft.original.copy(Bitmap.Config.ARGB_8888,true);bitmap=draft.cutout.copy(Bitmap.Config.ARGB_8888,true);cleanup=false;history.clear() }) { Text("Reset") }
                }
                error?.let { Text(it, color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall) }
                if(cleaning) LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Clean cutout",Modifier.weight(1f));Switch(cleanup,{cleanup=it;if(it){brush=false;refreshCleanup()}},enabled=!cleaning)
                }
                if(cleanup) {
                    Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                        Text("Brighten gently",Modifier.weight(1f));Switch(tone,{tone=it;refreshCleanup()},enabled=!cleaning)
                    }
                    TextButton(onClick={before=!before}) { Text(if(before) "Show after" else "Show before") }
                    Text("Tidies edges and framing; cannot unfold sleeves or remove wrinkles.",style=MaterialTheme.typography.bodySmall)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(brush, shape = ClosetShape, enabled=!cleaning, onClick = { brush = !brush; cleanup=false; before=false }, label = { Text("Brush") }, leadingIcon = { Icon(Icons.Outlined.Brush, null) })
                    TextButton(enabled = history.isNotEmpty() && !cleaning, onClick = { bitmap = history.removeAt(history.lastIndex); revision++ }) { Text("Undo") }
                    TextButton(enabled=!cleaning,onClick = { checkpoint(); bitmap = original.copy(Bitmap.Config.ARGB_8888, true); cleanup=false; before=false; revision++ }) { Text("Keep original") }
                }
                if(brush) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(!restore, shape = ClosetShape, onClick = { restore = false }, label = { Text("Erase") })
                        FilterChip(restore, shape = ClosetShape, onClick = { restore = true }, label = { Text("Restore") })
                        TextButton(onClick = { checkpoint(); bitmap = draft.cutout.copy(Bitmap.Config.ARGB_8888, true); original=draft.original.copy(Bitmap.Config.ARGB_8888,true); history.clear(); revision++ }) { Text("Reset cutout") }
                    }
                    Text("Brush size · ${brushSize.toInt()}", style = MaterialTheme.typography.labelMedium)
                    Slider(value = brushSize, onValueChange = { brushSize = it }, valueRange = 4f..90f)
                    Text("Draw on the image. Restore paints back the original photo. Turn Brush off to scroll over the image.", style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionLine("Details", "")
                OutlinedTextField(shape = ClosetShape, value = name, onValueChange = { name = it.take(80); manual("name") }, label = { Text(label("name","Name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Choice(label("category","Category"), category, categories, onSelect = { category = it; manual("category") })
                TextButton(enabled=!cleaning,onClick={
                    val turns=dev.closet.processing.GarmentAnalysis.orientation(dev.closet.processing.PhotoProcessing.pixels(bitmap),bitmap.width,bitmap.height,category)
                    history.clear();transform(turns)
                    if(turns==0)error="Orientation looks fine, or is ambiguous. Use the rotate buttons if needed."
                }){Text("Suggest orientation")}
                Choice(label("subcategory","Type"), subcategory, dev.closet.processing.GarmentAnalysis.subcategories) { subcategory=it;manual("subcategory") }
                Choice(label("pattern","Pattern"), pattern, patterns) { pattern=it;manual("pattern") }
                OutlinedTextField(value=brand,onValueChange={brand=it.take(120);manual("brand")},label={Text("Brand / size")},shape=ClosetShape,modifier=Modifier.fillMaxWidth())
                TextButton(onClick={ scan.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },enabled=!cleaning) { Icon(Icons.Outlined.DocumentScanner,null);Spacer(Modifier.width(8.dp));Text("Scan label") }
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    if(maxWidth < 340.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Choice(label("color","Main color"), color, colors) { color = it;manual("color") }
                            Choice(label("formality","Formality"), formality, formalities) { formality = it;manual("formality") }
                        }
                    } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Choice(label("color","Main color"), color, colors, Modifier.weight(1f)) { color = it;manual("color") }
                        Choice(label("formality","Formality"), formality, formalities, Modifier.weight(1f)) { formality = it;manual("formality") }
                    }
                }
                Choice(label("season","Season"), season, seasons, onSelect = { season = it;manual("season") })
                Button(shape = ClosetShape, onClick = { commit() }, enabled = !busy && !cleaning, modifier = Modifier.fillMaxWidth().heightIn(min=52.dp)) { Text("Save clothes") }
            }
        }
    }
    if(discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard these edits?") }, text = { Text("Your saved piece or batch photo stays unchanged.") },
        confirmButton = { TextButton(onClick = { discard = false; cancel() }) { Text("Discard edits") } }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep editing") } })
}
