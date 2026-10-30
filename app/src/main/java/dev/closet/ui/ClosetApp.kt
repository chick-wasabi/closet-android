package dev.closet.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.closet.data.*
import java.io.File

@Composable
fun ClosetApp(vm: ClosetViewModel) {
    ClosetTheme(dark = when(vm.settings.theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}, accent=vm.settings.accent) {
        val dark=when(vm.settings.theme){"dark"->true;"light"->false;else->isSystemInDarkTheme()}
        val activity=androidx.activity.compose.LocalActivity.current
        SideEffect { activity?.let { androidx.core.view.WindowCompat.getInsetsController(it.window,it.window.decorView).apply { isAppearanceLightStatusBars=!dark;isAppearanceLightNavigationBars=!dark } } }
        Surface(Modifier.fillMaxSize()) {
            val snack = remember { SnackbarHostState() }
            val items by vm.items.collectAsStateWithLifecycle()
            val outfits by vm.outfits.collectAsStateWithLifecycle()
            var page by rememberSaveable { mutableIntStateOf(0) }
            val pageStates = androidx.compose.runtime.saveable.rememberSaveableStateHolder()
            var settingsOpen by rememberSaveable { mutableStateOf(false) }
            var detailId by rememberSaveable { mutableStateOf<String?>(null) }
            var add by remember { mutableStateOf(false) }
            var cameraPath by rememberSaveable { mutableStateOf<String?>(null) }
            val context = LocalContext.current
            val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(30)) { if(it.isNotEmpty()) vm.import(it) }
            val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                cameraPath?.let { path ->
                    if (success) vm.import(listOf(FileProvider.getUriForFile(context, "${context.packageName}.files", File(path))))
                    else File(path).delete()
                }
            }
            LaunchedEffect(Unit) { for (message in vm.messages) snack.showSnackbar(message) }
            BackHandler(enabled = (page != 0 || settingsOpen || detailId!=null) && vm.draft == null) { if(settingsOpen) settingsOpen=false else if(detailId!=null) detailId=null else page = 0 }
            Scaffold(
                topBar = { if(vm.draft==null && vm.review.isEmpty() && !settingsOpen && detailId==null) SettingsActionBar(!vm.busy,add=if(page==0 && items.isNotEmpty()) ({add=true}) else null){settingsOpen=true} },
                snackbarHost = { SnackbarHost(snack) },
                bottomBar = {
                    if (vm.draft == null && vm.review.isEmpty() && !settingsOpen && detailId==null) ClosetNavigation(page, !vm.busy) { page = it }
                },
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    val draft = vm.draft
                    if (draft != null) ItemEditor(draft, vm.busy, vm::saveDraft, vm::skipDraft, vm::saveEdit, vm.settings.cleanup && vm.review.any{it.item.id==draft.item?.id && it.warning==null && !it.reviewed})
                    else if(vm.review.isNotEmpty()) BatchReview(vm)
                    else if(settingsOpen) SettingsScreen(vm) { settingsOpen=false }
                    else if(detailId!=null) {
                        val piece=items.firstOrNull{it.id==detailId}
                        if(piece!=null) ItemDetail(piece,vm){detailId=null} else LaunchedEffect(Unit){detailId=null}
                    }
                    else Crossfade(targetState = page, animationSpec = tween(180), label = "page transition") { activePage ->
                        pageStates.SaveableStateProvider(activePage) {
                            when (activePage) {
                                0 -> Wardrobe(items, {detailId=it.id}, { add = true }, vm::delete, onDeleteMany=vm::deleteMany, directDetail=true)
                                1 -> Builder(vm, items)
                                2 -> Saved(outfits, vm)
                            }
                        }
                    }
                    if (vm.busy) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = .92f)).clickable {}, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            if(vm.total>0) {
                                LinearProgressIndicator(progress={vm.completed.toFloat()/vm.total.coerceAtLeast(1)},modifier=Modifier.widthIn(max=280.dp).fillMaxWidth(.7f))
                            } else CircularProgressIndicator()
                            Text(vm.progress,Modifier.padding(horizontal=24.dp))
                        }
                        BackHandler { /* Finish atomic image/database operations before navigation. */ }
                    }
                }
            }
            if (add) AlertDialog(onDismissRequest = { add = false }, icon = { Icon(Icons.Outlined.AddAPhoto, null) }, title = { Text("Add to your closet") },
                text = { Text("Take a photo or choose up to 30 images.") },
                confirmButton = { TextButton(onClick = {
                    add = false
                    gallery.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text("Choose photos") } },
                dismissButton = { TextButton(onClick = {
                    add = false
                    try {
                        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                        dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000 }?.forEach { it.delete() }
                        val file = File.createTempFile("capture", ".jpg", dir); cameraPath = file.absolutePath
                        camera.launch(FileProvider.getUriForFile(context, "${context.packageName}.files", file))
                    } catch (_: Exception) { vm.messages.trySend("No camera app is available. Choose a photo instead.") }
                }) { Text("Take photo") } })
        }
    }
}

@Composable
fun SettingsActionBar(enabled: Boolean=true,add: (()->Unit)?=null,open: ()->Unit) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(end=8.dp),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
        if(add!=null) FilledTonalIconButton(onClick=add,enabled=enabled){Icon(Icons.Outlined.Add,"Add clothes")}
        IconButton(onClick=open,enabled=enabled){Icon(Icons.Outlined.Settings,"Settings")}
    }
}

@Composable
fun Heading(title: String, count: Int? = null, horizontalPadding: androidx.compose.ui.unit.Dp = 20.dp) {
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    Row(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BoxWithConstraints(Modifier.weight(1f)) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val available = with(density) { maxWidth.toPx() }
            val style = MaterialTheme.typography.headlineLarge
            // Measure the actual bundled font at the user's font scale, never guess character widths.
            val size = (30 downTo 20).firstOrNull { candidate ->
                measurer.measure(androidx.compose.ui.text.AnnotatedString(title), style = style.copy(fontSize = candidate.sp), softWrap = false, maxLines = 1).size.width <= available
            } ?: 20
            Text(title, Modifier.fillMaxWidth().testTag("screen-title"), style = style.copy(fontSize = size.sp), maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        if(count != null && count > 0) Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Text(count.toString(), Modifier.padding(horizontal = 12.dp, vertical = 5.dp).widthIn(max = 68.dp), style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

@Composable
fun Choice(label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, shape = ClosetShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall); Text(value.replaceFirstChar { it.uppercase() }) }
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option.replaceFirstChar { it.uppercase() }) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
fun Wardrobe(items: List<ClothingItem>, edit: (ClothingItem) -> Unit, add: () -> Unit, delete: (ClothingItem) -> Unit,
             onDeleteMany: ((List<ClothingItem>)->Unit)?=null, directDetail: Boolean=false) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }
    var sort by rememberSaveable { mutableStateOf("newest") }
    var largeGrid by rememberSaveable { mutableStateOf(false) }
    var selecting by rememberSaveable { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var confirmMany by remember { mutableStateOf(false) }
    val filtered = items.filter { (category == "all" || it.category == category) && listOf(it.name, it.category, it.color, it.formality, it.season, it.brand, it.subcategory, it.pattern).any { text -> text.contains(search, true) } }.let { rows -> when(sort){"category"->rows.sortedBy{it.category};"most worn"->rows.sortedByDescending{it.timesWorn};else->rows.sortedByDescending{it.createdAt}} }
    var selected by remember { mutableStateOf<ClothingItem?>(null) }
    val cellWidth = ((if(largeGrid) 230f else 148f) * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceIn(1f, 1.6f)).dp
    LazyVerticalGrid(modifier = Modifier.testTag("wardrobe-grid"), columns = GridCells.Adaptive(cellWidth), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 100.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { Heading("Closet", items.size, 0.dp) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Search clothes") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = ClosetShape)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(contentPadding = PaddingValues(0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("all") + categories) { c -> FilterChip(selected = category == c, onClick = { category = c }, shape = ClosetShape, label = { Text(c.replaceFirstChar { it.uppercase() }) }) }
            }
        }
        item(span={GridItemSpan(maxLineSpan)}) {
            Column {
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Choice("Sort",sort,listOf("newest","category","most worn"),Modifier.weight(1f)){sort=it}
                    IconButton(onClick={largeGrid=!largeGrid}){Icon(if(largeGrid)Icons.Outlined.GridView else Icons.Outlined.ViewAgenda,"Grid size")}
                    if(onDeleteMany!=null) IconButton(onClick={selecting=!selecting;selectedIds=emptySet()}){Icon(Icons.Outlined.Checklist,"Select pieces")}
                }
                if(selecting) Row(verticalAlignment=Alignment.CenterVertically){Text("${selectedIds.size} selected",Modifier.weight(1f));TextButton(onClick={confirmMany=true},enabled=selectedIds.isNotEmpty()){Text("Delete")}}
            }
        }
        if (filtered.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            EmptyState(Icons.Outlined.Checkroom, if(items.isEmpty()) "No clothes yet" else "No matches", if(items.isEmpty()) "Add a photo to get started." else "Try another search or category.", if(items.isEmpty()) "Add clothes" else null, add)
        }
        items(filtered, key = { it.id }) { item ->
            BouncyCard(onClick = { if(selecting) selectedIds=if(item.id in selectedIds)selectedIds-item.id else selectedIds+item.id else if(directDetail)edit(item) else selected=item }, modifier = Modifier.fillMaxWidth().animateItem()) {
                Column(Modifier.padding(8.dp)) {
                    if(selecting) Checkbox(item.id in selectedIds,{checked->selectedIds=if(checked)selectedIds+item.id else selectedIds-item.id})
                    PhotoStage(Modifier.fillMaxWidth().aspectRatio(1f)) {
                        AsyncImage(File(item.imagePath), item.name.ifBlank { item.category }, Modifier.fillMaxSize().padding(14.dp), contentScale = ContentScale.Fit)
                    }
                    Text(item.name.ifBlank { item.category.replaceFirstChar { it.uppercase() } }, Modifier.padding(horizontal = 8.dp, vertical = 12.dp), maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
    if(confirmMany) AlertDialog(onDismissRequest={confirmMany=false},title={Text("Delete ${selectedIds.size} pieces?")},text={Text("Photos will be removed. Saved outfit images stay.")},confirmButton={TextButton(onClick={onDeleteMany?.invoke(items.filter{it.id in selectedIds});selectedIds=emptySet();confirmMany=false;selecting=false}){Text("Delete")}},dismissButton={TextButton(onClick={confirmMany=false}){Text("Cancel")}})
    selected?.let { item ->
        var confirmDelete by remember(item.id) { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(if(confirmDelete) "Delete this piece?" else item.name.ifBlank { item.category }) },
            text = {
                if(confirmDelete) Text("The original and cutout will be deleted. Saved outfit images are kept.")
                else Column { AsyncImage(File(item.imagePath), item.category, Modifier.fillMaxWidth().height(220.dp), contentScale = ContentScale.Fit); Text("${item.color} · ${item.formality} · ${item.season}") }
            },
            confirmButton = { TextButton(onClick = { if(confirmDelete) delete(item) else edit(item); selected = null }) { Text(if(confirmDelete) "Delete" else "Edit / fix cutout") } },
            dismissButton = { TextButton(onClick = { if(confirmDelete) selected = null else confirmDelete = true }) { Text(if(confirmDelete) "Cancel" else "Delete piece") } })
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, description: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SoftIllustration(saved = icon == Icons.Outlined.FavoriteBorder)
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(description, Modifier.widthIn(max = 280.dp), style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if(action != null) Button(shape = RoundedCornerShape(28.dp), onClick = onAction, modifier = Modifier.heightIn(min = 52.dp)) {
            Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(8.dp)); Text(action)
        }
    }
}

@Composable
fun Builder(vm: ClosetViewModel, items: List<ClothingItem>) = SmartBuilder(vm,items)

@Composable
fun OutfitBuilder(items: List<ClothingItem>, preview: android.graphics.Bitmap?, current: List<ClothingItem>, favorite: Boolean,
                  onShuffle: (String, String) -> Unit, onSave: () -> Unit, onFavorite: () -> Unit, onSwap: (String, ClothingItem?) -> Unit,
                  occasion: String="Casual", onOccasion: (String)->Unit={}, initialSeason: String="all",
                  options: List<OutfitOption> = emptyList(), onOption: (Int)->Unit={}, locks: Set<String> = emptySet(),
                  onLock: (ClothingItem)->Unit={}, onFeedback: (Boolean)->Unit={}) {
    var season by rememberSaveable { mutableStateOf("auto") }
    var formality by rememberSaveable { mutableStateOf("any") }
    var swap by remember { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Heading("Outfits") }
        item { Box(Modifier.widthIn(max=600.dp).fillMaxWidth().padding(horizontal=20.dp,vertical=4.dp)) { Choice("Occasion",occasion,occasions,onSelect=onOccasion) } }
        item {
            BoxWithConstraints(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
                if(maxWidth < 340.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale > 1.3f) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Choice(if(season=="auto") "Season · $initialSeason" else "Season", season, listOf("auto")+seasons) { season = it }
                        Choice("Mood", formality, listOf("any") + formalities) { formality = it }
                    }
                } else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Choice(if(season=="auto") "Season · $initialSeason" else "Season", season, listOf("auto")+seasons, Modifier.weight(1f)) { season = it }
                    Choice("Mood", formality, listOf("any") + formalities, Modifier.weight(1f)) { formality = it }
                }
            }
        }

        item {
            Row(Modifier.widthIn(max = 600.dp).fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(shape = ClosetShape, onClick = { onShuffle(if(season=="auto")initialSeason else season, formality) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Shuffle") }
                FilledTonalIconButton(onClick = onSave, enabled = preview != null, modifier = Modifier.size(52.dp)) { Icon(Icons.Outlined.BookmarkBorder, "Save outfit") }
                FilledTonalIconButton(onClick = onFavorite, enabled = preview != null, modifier = Modifier.size(52.dp)) { Icon(if(favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Favorite outfit") }
            }
        }
        item {
            PhotoStage(Modifier.padding(horizontal = 20.dp, vertical = 16.dp).widthIn(max = 450.dp).fillMaxWidth()) {
                if(options.isNotEmpty()) {
                    val pager=androidx.compose.foundation.pager.rememberPagerState(pageCount={options.size})
                    LaunchedEffect(options){pager.scrollToPage(0)}
                    LaunchedEffect(pager.settledPage,options){onOption(pager.settledPage)}
                    Column(horizontalAlignment=Alignment.CenterHorizontally) {
                        androidx.compose.foundation.pager.HorizontalPager(pager) { index ->
                            Image(options[index].image.asImageBitmap(),"Outfit option ${index+1}",Modifier.fillMaxWidth().height(380.dp).padding(20.dp))
                        }
                        Text("${pager.currentPage+1} / ${options.size}",Modifier.padding(bottom=12.dp),style=MaterialTheme.typography.labelMedium)
                    }
                } else Crossfade(targetState = preview, animationSpec = tween(220), label = "outfit change") { bitmap ->
                    if(bitmap == null) Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        OutfitGuide()
                        Text("Add a top, bottom and shoes to begin.", Modifier.padding(horizontal = 24.dp, vertical = 20.dp), style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else Image(bitmap.asImageBitmap(), "Outfit: " + current.joinToString { it.name.ifBlank { it.category } }, Modifier.fillMaxWidth().height(380.dp).padding(20.dp))
                }
            }
        }

        if(preview != null) {
            item {
                Column(Modifier.widthIn(max=600.dp).fillMaxWidth().padding(horizontal=24.dp,vertical=8.dp)) {
                    Text(options.firstOrNull{it.pieces.map{p->p.id}==current.map{p->p.id}}?.why ?: "Your pieces, your way.",style=MaterialTheme.typography.bodyMedium)
                    Row { IconButton(onClick={onFeedback(true)}){Icon(Icons.Outlined.ThumbUp,"Like this pairing")};IconButton(onClick={onFeedback(false)}){Icon(Icons.Outlined.ThumbDown,"Dislike this pairing")} }
                }
            }
            item { SectionLine("Pieces", "", Modifier.padding(horizontal = 24.dp)) }
            items(if(occasion=="Sleep")listOf("pajamas")else dev.closet.domain.OutfitEngine.stack) { category ->
                val piece = current.firstOrNull { it.category == category }
                ListItem(headlineContent = { Text(piece?.name?.ifBlank { category } ?: "Add $category") }, supportingContent = { Text(piece?.let { "${it.color} · ${it.formality}" } ?: "Optional layer") },
                    leadingContent = { if(piece != null) AsyncImage(File(piece.imagePath), null, Modifier.size(48.dp), contentScale = ContentScale.Fit) },
                    trailingContent = { Row(verticalAlignment=Alignment.CenterVertically) {
                        if(piece!=null) IconButton(onClick={onLock(piece)}){Icon(if(piece.id in locks)Icons.Outlined.Lock else Icons.Outlined.LockOpen,if(piece.id in locks)"Unlock piece" else "Lock piece")}
                        TextButton(onClick = { swap = category }) { Text("Swap") }
                    } }, modifier = Modifier.widthIn(max = 600.dp))
            }
        }
    }
    swap?.let { category ->
        AlertDialog(onDismissRequest = { swap = null }, title = { Text("Choose $category") }, text = {
            LazyColumn(Modifier.heightIn(max = 440.dp)) {
                if(category in listOf("hat", "jacket")) item { TextButton(onClick = { onSwap(category, null); swap = null }) { Text("No $category") } }
                items(items.filter { it.category == category }) { item ->
                    ListItem(headlineContent = { Text(item.name.ifBlank { "${item.color} ${item.category}" }) }, leadingContent = { AsyncImage(File(item.imagePath), null, Modifier.size(56.dp)) }, modifier = Modifier.clickable { onSwap(category, item); swap = null })
                }
                if(items.none { it.category == category }) item { Text("Add a $category to your closet first.") }
            }
        }, confirmButton = { TextButton(onClick = { swap = null }) { Text("Close") } })
    }
}

@Composable
fun Saved(outfits: List<SavedOutfit>, vm: ClosetViewModel) {
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var selected by remember { mutableStateOf<SavedOutfit?>(null) }
    var deleting by remember { mutableStateOf<SavedOutfit?>(null) }
    var historyOnly by rememberSaveable { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<SavedOutfit?>(null) }
    var newName by remember { mutableStateOf("") }
    val logs by vm.wear.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<SavedOutfit?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) pendingExport?.let(vm::export) else vm.messages.trySend("Gallery permission denied. You can still share the PNG.")
        pendingExport = null
    }
    fun export(outfit: SavedOutfit) {
        if(Build.VERSION.SDK_INT <= 28 && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingExport = outfit; permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else vm.export(outfit)
    }
    val shown = outfits.filter { !favoritesOnly || it.favorite }
    val cellWidth = (148f * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceIn(1f, 1.6f)).dp
    LazyVerticalGrid(columns = GridCells.Adaptive(cellWidth), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { Heading("Saved", outfits.size, 0.dp) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, shape = ClosetShape, label = { Text("Favorites") }, leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(16.dp)) })
                FilterChip(selected=historyOnly,onClick={historyOnly=!historyOnly},shape=ClosetShape,label={Text("Wear log")})
            }
        }
        if(historyOnly) {
            if(logs.isEmpty())item(span={GridItemSpan(maxLineSpan)}){Text("Log an outfit with ‘Wore this today’.",Modifier.padding(16.dp))}
            items(logs,key={it.id}){entry->
                ListItem(headlineContent={Text(outfits.firstOrNull{it.id==entry.outfitId}?.name ?: "Past outfit")},supportingContent={Text(java.time.Instant.ofEpochMilli(entry.wornAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString())})
            }
        }
        if(!historyOnly && shown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            EmptyState(Icons.Outlined.FavoriteBorder, if(favoritesOnly) "No favorites yet" else "No saved outfits", "Save an outfit you love.")
        }
        itemsIndexed(if(historyOnly)emptyList()else shown, key = { _, outfit -> outfit.id }) { index, outfit ->
            Surface(shape = RoundedCornerShape(28.dp), modifier = Modifier.animateItem()) {
                Column(Modifier.padding(8.dp)) {
                    PhotoStage(Modifier.fillMaxWidth().aspectRatio(.625f).clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Open ${outfit.name}") { selected = outfit }) {
                        AsyncImage(File(outfit.imagePath), outfit.name, Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit)
                    }
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(outfit.name, Modifier.weight(1f), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = { vm.toggleFavorite(outfit) }) { Icon(if(outfit.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Toggle favorite", tint = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
        }
    }
    selected?.let { stale ->
        val outfit = outfits.firstOrNull { it.id == stale.id } ?: stale
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(outfit.name) }, text = {
            Column(Modifier.heightIn(max=500.dp).verticalScroll(rememberScrollState())) {
                AsyncImage(File(outfit.imagePath), outfit.name, Modifier.fillMaxWidth().height(300.dp), contentScale = ContentScale.Fit)
                TextButton(onClick={vm.wore(outfit)}){Icon(Icons.Outlined.CheckCircle,null);Spacer(Modifier.width(8.dp));Text("Wore this today")}
                TextButton(onClick={newName=outfit.name;renaming=outfit;selected=null}){Text("Rename")}
                Text("Worn ${logs.count{it.outfitId==outfit.id}} times",style=MaterialTheme.typography.bodySmall)
                Column { TextButton(onClick = { vm.share(outfit) }) { Text("Share PNG") }; TextButton(onClick = { export(outfit) }) { Text("Save to gallery") } }
                TextButton(onClick = { selected = null; deleting = outfit }) { Text("Delete outfit", color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Done") } })
    }
    renaming?.let { outfit->AlertDialog(onDismissRequest={renaming=null},title={Text("Rename outfit")},text={OutlinedTextField(newName,{newName=it.take(80)},label={Text("Name")},shape=ClosetShape)},confirmButton={TextButton(onClick={vm.rename(outfit,newName);renaming=null}){Text("Save")}},dismissButton={TextButton(onClick={renaming=null}){Text("Cancel")}}) }
    deleting?.let { outfit -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete saved outfit?") }, text = { Text("Your clothing items will stay in the closet.") }, confirmButton = { TextButton(onClick = { vm.deleteOutfit(outfit); deleting = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}
