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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.closet.data.*
import java.io.File

@Composable
fun ClosetApp(vm: ClosetViewModel) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) darkColorScheme(primary = Color(0xFFB9CAA5), secondary = Color(0xFFD8C6AC), background = Color(0xFF171B16), surface = Color(0xFF20251F))
    else lightColorScheme(primary = Color(0xFF526440), secondary = Color(0xFF806B50), background = Color(0xFFF8F6EF), surface = Color(0xFFF8F6EF), surfaceVariant = Color(0xFFEAEBDD))
    MaterialTheme(colorScheme = scheme, shapes = Shapes(medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(28.dp))) {
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
                    if (vm.draft == null) NavigationBar {
                        listOf("Closet" to Icons.Outlined.Checkroom, "Style" to Icons.Outlined.AutoAwesome, "Saved" to Icons.Outlined.CollectionsBookmark).forEachIndexed { i, (label, icon) ->
                            NavigationBarItem(enabled = !vm.busy, selected = page == i, onClick = { page = i }, icon = { Icon(icon, label) }, label = { Text(label) })
                        }
                    }
                },
                floatingActionButton = {
                    if (page == 0 && vm.draft == null && !vm.busy) ExtendedFloatingActionButton(onClick = { add = true }, icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add piece") })
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    val draft = vm.draft
                    if (draft != null) ItemEditor(draft, vm.busy, vm::saveDraft, vm::skipDraft)
                    else when (page) {
                        0 -> Wardrobe(items, vm::edit, { add = true })
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
fun Heading(kicker: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(kicker.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Choice(label: String, value: String, options: List<String>, modifier: Modifier = Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Column(Modifier.weight(1f)) { Text(label, style = MaterialTheme.typography.labelSmall); Text(value.replaceFirstChar { it.uppercase() }) }
            Icon(Icons.Outlined.ExpandMore, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option.replaceFirstChar { it.uppercase() }) }, onClick = { onSelect(option); expanded = false }) }
        }
    }
}

@Composable
fun Wardrobe(items: List<ClothingItem>, edit: (ClothingItem) -> Unit, add: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("all") }
    val filtered = items.filter { (category == "all" || it.category == category) && listOf(it.name, it.category, it.color, it.formality, it.season).any { text -> text.contains(search, true) } }
    var selected by remember { mutableStateOf<ClothingItem?>(null) }
    val vm: ClosetViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    Column {
        Heading("Your everyday archive", "Closet", "${items.size} pieces. A little less “what to wear?”.")
        OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Find a piece, color, or season") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(50))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("all") + categories) { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.replaceFirstChar { it.uppercase() }) }) }
        }
        if (filtered.isEmpty()) EmptyState(Icons.Outlined.Checkroom, if(items.isEmpty()) "Make room for your favorites" else "No pieces found", if(items.isEmpty()) "Your wardrobe starts with one photo. Everything stays on this phone." else "Try a different search or category.", if(items.isEmpty()) "Add your first piece" else null, add)
        else LazyVerticalGrid(columns = GridCells.Adaptive(155.dp), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 100.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filtered, key = { it.id }) { item ->
                Card(onClick = { selected = item }, shape = RoundedCornerShape(24.dp)) {
                    AsyncImage(File(item.imagePath), item.name.ifBlank { item.category }, Modifier.fillMaxWidth().aspectRatio(.88f).padding(12.dp), contentScale = ContentScale.Fit)
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 16.dp)) {
                        Text(item.name.ifBlank { item.category.replaceFirstChar { it.uppercase() } }, maxLines = 2, fontWeight = FontWeight.Medium)
                        Text("${item.color} · ${item.category}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            confirmButton = { TextButton(onClick = { if(confirmDelete) vm.delete(item) else edit(item); selected = null }) { Text(if(confirmDelete) "Delete" else "Edit / fix cutout") } },
            dismissButton = { TextButton(onClick = { if(confirmDelete) selected = null else confirmDelete = true }) { Text(if(confirmDelete) "Cancel" else "Delete piece") } })
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, description: String, action: String? = null, onAction: () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Icon(icon, null, Modifier.padding(24.dp).size(40.dp)) }
        Text(title, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
        Text(description, style = MaterialTheme.typography.bodyMedium)
        if(action != null) Button(onClick = onAction) { Text(action) }
    }
}

@Composable
fun Builder(vm: ClosetViewModel, items: List<ClothingItem>) {
    var season by rememberSaveable { mutableStateOf("all") }
    var formality by rememberSaveable { mutableStateOf("any") }
    var swap by remember { mutableStateOf<String?>(null) }
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item { Heading("Good together", "The outfit studio", "Your pieces. Fresh combinations. Zero scrolling for inspiration.") }
        item {
            Row(Modifier.widthIn(max = 600.dp).padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Choice("Season", season, seasons, Modifier.weight(1f)) { season = it }
                Choice("Mood", formality, listOf("any") + formalities, Modifier.weight(1f)) { formality = it }
            }
        }
        item {
            Card(Modifier.padding(20.dp).widthIn(max = 450.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                val bitmap = vm.preview
                if(bitmap == null) EmptyState(Icons.Outlined.AutoAwesome, "Meet your next outfit", "Add a top, bottom, and shoes, then shuffle. Hats and jackets join when they fit.")
                else Image(bitmap.asImageBitmap(), "Outfit: " + vm.current.joinToString { it.name.ifBlank { it.category } }, Modifier.fillMaxWidth().aspectRatio(.625f).padding(16.dp))
            }
        }
        item {
            Row(Modifier.padding(horizontal = 20.dp).widthIn(max = 600.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.shuffle(season, formality) }, modifier = Modifier.weight(1f)) { Icon(Icons.Outlined.Shuffle, null); Spacer(Modifier.width(6.dp)); Text("Shuffle") }
                OutlinedButton(onClick = { vm.saveOutfit() }, enabled = vm.preview != null) { Text("Save") }
                IconButton(onClick = { vm.saveOutfit(!vm.favorite) }, enabled = vm.preview != null) { Icon(if(vm.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Favorite outfit") }
            }
        }
        if(vm.preview != null) {
            item { Text("MAKE IT YOURS", Modifier.fillMaxWidth().padding(start = 24.dp, top = 28.dp, bottom = 8.dp), style = MaterialTheme.typography.labelMedium) }
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
    Column {
        Heading("Keep the good ones", "On repeat", "Finished looks, ready when you are.")
        FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("Favorites only") }, modifier = Modifier.padding(start = 20.dp, bottom = 12.dp))
        val shown = outfits.filter { !favoritesOnly || it.favorite }
        if(shown.isEmpty()) EmptyState(Icons.Outlined.CollectionsBookmark, "A space for great outfits", "Save a look from the studio and it will be waiting here.")
        LazyVerticalGrid(columns = GridCells.Adaptive(160.dp), contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shown, key = { it.id }) { outfit ->
                Card(onClick = { selected = outfit }) {
                    AsyncImage(File(outfit.imagePath), outfit.name, Modifier.fillMaxWidth().aspectRatio(.625f).padding(12.dp), contentScale = ContentScale.Fit)
                    Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(outfit.name, Modifier.weight(1f), maxLines = 1)
                        IconButton(onClick = { vm.toggleFavorite(outfit) }) { Icon(if(outfit.favorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Toggle favorite") }
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
