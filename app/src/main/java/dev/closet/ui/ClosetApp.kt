package dev.closet.ui

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
    ClosetTheme {
        Surface(Modifier.fillMaxSize()) {
            val snack = remember { SnackbarHostState() }
            val items by vm.items.collectAsStateWithLifecycle()
            val outfits by vm.outfits.collectAsStateWithLifecycle()
            var page by rememberSaveable { mutableIntStateOf(0) }
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
            BackHandler(enabled = page != 0 && vm.draft == null) { page = 0 }
            Scaffold(
                snackbarHost = { SnackbarHost(snack) },
                bottomBar = {
                    if (vm.draft == null) ClosetNavigation(page, !vm.busy) { page = it }
                },
                floatingActionButton = {
                    if (page == 0 && vm.draft == null && !vm.busy) AddPieceAction { add = true }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    val draft = vm.draft
                    if (draft != null) ItemEditor(draft, vm.busy, vm::saveDraft, vm::skipDraft)
                    else when (page) {
                        0 -> Wardrobe(items, vm::edit, { add = true }, vm::delete)
                        1 -> Builder(vm, items)
                        2 -> Saved(outfits, vm)
                    }
                    if (vm.busy) Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = .92f)).clickable {}, contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            CircularProgressIndicator(); Text(vm.progress)
                        }
                        BackHandler { /* Finish atomic image/database operations before navigation. */ }
                    }
                }
            }
            if (add) AlertDialog(onDismissRequest = { add = false }, icon = { Icon(Icons.Outlined.AddAPhoto, null) }, title = { Text("A new piece") },
                text = { Text("Photograph one item against a plain background, or choose up to 30 photos. Each photo becomes one piece you can refine and label.") },
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
fun Heading(kicker: String, title: String, subtitle: String, horizontalPadding: androidx.compose.ui.unit.Dp = 22.dp) {
    Column(Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 22.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            MicroLabel(kicker, Modifier.weight(1f), MaterialTheme.colorScheme.primary)
            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary))
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale
            val display = title in listOf("CLOSET", "LOOKBOOK", "STUDIO")
            val size = minOf(76f, maxWidth.value / (title.length * .55f) / scale)
            Text(title, style = if(display) MaterialTheme.typography.displayLarge.copy(fontSize = size.sp, lineHeight = size.sp) else MaterialTheme.typography.headlineLarge)
        }
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Choice(label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, shape = ClosetShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
            Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall); Text(value.replaceFirstChar { it.uppercase() }) }
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option.replaceFirstChar { it.uppercase() }) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
fun Wardrobe(items: List<ClothingItem>, edit: (ClothingItem) -> Unit, add: () -> Unit, delete: (ClothingItem) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }
    val filtered = items.filter { (category == "all" || it.category == category) && listOf(it.name, it.category, it.color, it.formality, it.season).any { text -> text.contains(search, true) } }
    var selected by remember { mutableStateOf<ClothingItem?>(null) }
    LazyVerticalGrid(modifier = Modifier.testTag("wardrobe-grid"), columns = GridCells.Adaptive(155.dp), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 100.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { Heading("Your collection / ${items.size.toString().padStart(3, '0')} pieces", "CLOSET", "Good clothes. Better combinations.", 0.dp) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Search your wardrobe") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = ClosetShape)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(contentPadding = PaddingValues(0.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("all") + categories) { c -> FilterChip(selected = category == c, onClick = { category = c }, shape = ClosetShape, label = { Text(c.replaceFirstChar { it.uppercase() }) }) }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SectionLine(if(category == "all") "All pieces" else category, "${filtered.size.toString().padStart(2, '0')} in view")
            }
        }
        if (filtered.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                if(items.isEmpty()) WardrobePoster()
                EmptyState(Icons.Outlined.Checkroom, if(items.isEmpty()) "Start with your favorites." else "Nothing in this view.", if(items.isEmpty()) "One photo, one piece. Build a collection that is entirely yours." else "Try another search or category.", if(items.isEmpty()) "Add your first piece" else null, add)
            }
        }
        itemsIndexed(filtered, key = { _, item -> item.id }) { index, item ->
            Surface(onClick = { selected = item }, modifier = Modifier.fillMaxWidth(), shape = ClosetShape,
                color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column {
                    PhotoStage(Modifier.fillMaxWidth().aspectRatio(.82f)) {
                        MicroLabel((index + 1).toString().padStart(2, '0'), Modifier.align(Alignment.TopStart).padding(10.dp))
                        AsyncImage(File(item.imagePath), item.name.ifBlank { item.category }, Modifier.fillMaxSize().padding(16.dp), contentScale = ContentScale.Fit)
                    }
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        MicroLabel(item.category, color = MaterialTheme.colorScheme.primary)
                        Text(item.name.ifBlank { "${item.color.replaceFirstChar { it.uppercase() }} ${item.category}" }, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                        Text("${item.formality} / ${item.season}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
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
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(icon, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if(action != null) Button(shape = ClosetShape, onClick = onAction, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
            Text(action); Spacer(Modifier.weight(1f)); Icon(Icons.Outlined.NorthEast, null)
        }
    }
}

@Composable
fun Builder(vm: ClosetViewModel, items: List<ClothingItem>) {
    var season by rememberSaveable { mutableStateOf("all") }
    var formality by rememberSaveable { mutableStateOf("any") }
    var swap by remember { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Heading("02 / The styling room", "STUDIO", "Put your wardrobe to work.") }
        item {
            Row(Modifier.widthIn(max = 600.dp).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Choice("Season", season, seasons, Modifier.weight(1f)) { season = it }
                Choice("Mood", formality, listOf("any") + formalities, Modifier.weight(1f)) { formality = it }
            }
        }
        item {
            Column(Modifier.padding(20.dp).widthIn(max = 450.dp).fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.onBackground).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    MicroLabel("OUTFIT / ${vm.current.size.toString().padStart(2, '0')} PIECES", color = MaterialTheme.colorScheme.background)
                    MicroLabel("PREVIEW", color = MaterialTheme.colorScheme.background)
                }
                PhotoStage(Modifier.fillMaxWidth()) {
                    val bitmap = vm.preview
                    if(bitmap == null) Column {
                        OutfitGuide()
                        EmptyState(Icons.Outlined.Tune, "Build a look.", "A top, a bottom, a pair of shoes. Start there; add layers when they fit.")
                    }
                    else Image(bitmap.asImageBitmap(), "Outfit: " + vm.current.joinToString { it.name.ifBlank { it.category } }, Modifier.fillMaxWidth().aspectRatio(.625f).padding(20.dp))
                }
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    MicroLabel("Hat / Top / Bottom / Shoes")
                }
            }
        }

        item {
            Row(Modifier.padding(horizontal = 20.dp).widthIn(max = 600.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(shape = ClosetShape, onClick = { vm.shuffle(season, formality) }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Shuffle, null); Spacer(Modifier.width(6.dp)); Text("Shuffle") }
                OutlinedButton(shape = ClosetShape, onClick = { vm.saveOutfit() }, enabled = vm.preview != null) { Text("Save") }
                IconButton(onClick = { vm.saveOutfit(!vm.favorite) }, enabled = vm.preview != null) { Icon(if(vm.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Favorite outfit") }
            }
        }
        if(vm.preview != null) {
            item { SectionLine("Make it yours", "Swap a piece", Modifier.padding(horizontal = 24.dp)) }
            items(dev.closet.domain.OutfitEngine.stack) { category ->
                val piece = vm.current.firstOrNull { it.category == category }
                ListItem(headlineContent = { Text(piece?.name?.ifBlank { category } ?: "Add $category") }, supportingContent = { Text(piece?.let { "${it.color} · ${it.formality}" } ?: "Optional layer") },
                    leadingContent = { if(piece != null) AsyncImage(File(piece.imagePath), null, Modifier.size(48.dp), contentScale = ContentScale.Fit) },
                    trailingContent = { TextButton(onClick = { swap = category }) { Text("Swap") } }, modifier = Modifier.widthIn(max = 600.dp))
            }
            item { Text("Swaps are your choice; they can override the style filters.", Modifier.padding(24.dp), style = MaterialTheme.typography.bodySmall) }
        }
    }
    swap?.let { category ->
        AlertDialog(onDismissRequest = { swap = null }, title = { Text("Choose $category") }, text = {
            LazyColumn(Modifier.heightIn(max = 440.dp)) {
                if(category in listOf("hat", "jacket")) item { TextButton(onClick = { vm.swap(category, null); swap = null }) { Text("No $category") } }
                items(items.filter { it.category == category }) { item ->
                    ListItem(headlineContent = { Text(item.name.ifBlank { "${item.color} ${item.category}" }) }, leadingContent = { AsyncImage(File(item.imagePath), null, Modifier.size(56.dp)) }, modifier = Modifier.clickable { vm.swap(category, item); swap = null })
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
    LazyVerticalGrid(columns = GridCells.Adaptive(155.dp), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 28.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) { Heading("03 / Saved outfits", "LOOKBOOK", "Your best combinations. Worth keeping.", 0.dp) }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, shape = ClosetShape, label = { Text("Favorites only") }, leadingIcon = { Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(16.dp)) })
                MicroLabel("${shown.size.toString().padStart(2, '0')} looks")
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
        if(shown.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                WardrobePoster(saved = true)
                EmptyState(Icons.Outlined.Bookmarks, "A look worth repeating.", "Save a combination in the studio. Your favorites live here.")
            }
        }
        itemsIndexed(shown, key = { _, outfit -> outfit.id }) { index, outfit ->
            Surface(shape = ClosetShape, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column {
                    PhotoStage(Modifier.fillMaxWidth().aspectRatio(.625f).clickable(role = androidx.compose.ui.semantics.Role.Button, onClickLabel = "Open ${outfit.name}") { selected = outfit }) {
                        MicroLabel("LOOK ${(index + 1).toString().padStart(2, '0')}", Modifier.padding(10.dp))
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
            Column {
                AsyncImage(File(outfit.imagePath), outfit.name, Modifier.fillMaxWidth().height(300.dp), contentScale = ContentScale.Fit)
                Column { TextButton(onClick = { vm.share(outfit) }) { Text("Share PNG") }; TextButton(onClick = { export(outfit) }) { Text("Save to gallery") } }
                TextButton(onClick = { selected = null; deleting = outfit }) { Text("Delete outfit", color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(onClick = { selected = null }) { Text("Done") } })
    }
    deleting?.let { outfit -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete saved outfit?") }, text = { Text("Your clothing items will stay in the closet.") }, confirmButton = { TextButton(onClick = { vm.deleteOutfit(outfit); deleting = null }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}
