package dev.closet.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import dev.closet.data.*
import java.io.File
import java.time.Instant
import java.time.ZoneId

@Composable
fun BatchReview(vm: ClosetViewModel) {
    var discard by remember{mutableStateOf(false)}
    BackHandler{discard=true}
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{Heading("Review",vm.review.size,0.dp)}
        item{Text("Guesses are editable. Check each piece before saving.",style=MaterialTheme.typography.bodyMedium)}
        items(vm.review,key={it.item.id}){value->
            Surface(shape=ClosetShape) { Column {
                ListItem(headlineContent={Text(value.item.name.ifBlank{"New piece"})},supportingContent={Text(value.warning?:"${value.item.category} · ${value.item.color}")},
                    leadingContent={AsyncImage(File(value.item.imagePath),null,Modifier.size(72.dp),contentScale=ContentScale.Fit)})
                Row(Modifier.padding(horizontal=12.dp)){TextButton(onClick={vm.editReview(value)}){Text("Review / fix")};TextButton(onClick={vm.removeReview(value)}){Text("Remove")}}
            } }
        }
        item{Button(onClick=vm::saveAll,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)){Text("Save all")}}
        item{TextButton(onClick={discard=true}){Text("Discard batch")}}
    }
    if(discard)AlertDialog(onDismissRequest={discard=false},title={Text("Discard these photos?")},text={Text("Your saved closet stays unchanged.")},confirmButton={TextButton(onClick={vm.discardReview();discard=false}){Text("Discard")}},dismissButton={TextButton(onClick={discard=false}){Text("Keep reviewing")}})
}

@Composable
fun ItemDetail(item: ClothingItem,vm: ClosetViewModel,back: ()->Unit) {
    var deleting by remember{mutableStateOf(false)}
    var turns by remember(item.imagePath){mutableIntStateOf(0)}
    var flip by remember(item.imagePath){mutableStateOf(false)}
    val transformed by produceState<android.graphics.Bitmap?>(null,item.imagePath,turns,flip){
        value= kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){dev.closet.processing.PhotoProcessing.transform(vm.images.read(item.imagePath),turns,flip)}
    }
    BackHandler(onBack=back)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{TextButton(onClick=back){Icon(Icons.Outlined.ArrowBack,null);Text("Closet")}}
        item{Text(item.name.ifBlank{item.category},style=MaterialTheme.typography.headlineMedium)}
        item{PhotoStage(Modifier.fillMaxWidth().heightIn(min=220.dp,max=380.dp).aspectRatio(1f)){AsyncImage(transformed ?: File(item.imagePath),item.name,Modifier.fillMaxSize().padding(20.dp),contentScale=ContentScale.Fit)}}
        item{FlowRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){
            IconButton(onClick={turns=(turns+3)%4}){Icon(Icons.Outlined.RotateLeft,"Rotate left")}
            IconButton(onClick={turns=(turns+1)%4}){Icon(Icons.Outlined.RotateRight,"Rotate right")}
            IconButton(onClick={flip=!flip}){Icon(Icons.Outlined.Flip,"Flip horizontal")}
            TextButton(onClick={turns=0;flip=false}){Text("Reset")}
            if(turns!=0||flip) TextButton(onClick={vm.saveOrientation(item,turns,flip)}){Text("Save orientation")}
        }}
        item{Text(listOf(item.color,item.subcategory.ifBlank{item.category},item.pattern,item.formality,item.season).joinToString(" · "),style=MaterialTheme.typography.bodyMedium)}
        if(item.brand.isNotBlank())item{Text(item.brand)}
        item{Text("Worn ${item.timesWorn} times · Last: ${if(item.lastWorn==0L) "never" else Instant.ofEpochMilli(item.lastWorn).atZone(ZoneId.systemDefault()).toLocalDate()}",style=MaterialTheme.typography.bodySmall)}
        item{Button(onClick={vm.edit(item)},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.Edit,null);Spacer(Modifier.width(8.dp));Text("Edit / rotate")}}
        item{FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            FilledTonalButton(onClick={vm.recut(item)}){Text("Re-cutout")}
            FilledTonalButton(onClick={vm.favoriteItem(item)}){Icon(if(item.favorite)Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,null);Spacer(Modifier.width(6.dp));Text("Favorite")}
            OutlinedButton(onClick={vm.duplicate(item)}){Text("Duplicate")}
            TextButton(onClick={deleting=true}){Text("Delete",color=MaterialTheme.colorScheme.error)}
        }}
    }
    if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("Delete piece?")},text={Text("Its photos will be removed. Saved outfit images stay.")},confirmButton={TextButton(onClick={vm.delete(item);back()}){Text("Delete")}},dismissButton={TextButton(onClick={deleting=false}){Text("Cancel")}})
}

@Composable
fun SettingsScreen(vm: ClosetViewModel,back: ()->Unit) {
    val settings=vm.settings
    val context=LocalContext.current
    var restoring by remember{mutableStateOf<android.net.Uri?>(null)}
    var deleting by remember{mutableStateOf(false)}
    var glossary by remember{mutableStateOf(false)}
    val backup=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){it?.let(vm::backup)}
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){restoring=it}
    val storage=remember(vm.busy){context.filesDir.walkTopDown().filter{it.isFile}.sumOf{it.length()}}
    BackHandler(onBack=back)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item{TextButton(onClick=back){Icon(Icons.Outlined.ArrowBack,null);Text("Back")};Heading("Settings",horizontalPadding=0.dp)}
        item{Choice("Theme",settings.theme,listOf("system","light","dark")){vm.updateSettings(settings.copy(theme=it))}}
        item{Choice("Accent",settings.accent,listOf("rose","lilac","teal")){vm.updateSettings(settings.copy(accent=it))}}
        item{Choice("Hemisphere",settings.hemisphere,listOf("north","south","tropical")){vm.updateSettings(settings.copy(hemisphere=it))};Text("Season now: ${settings.season()}",style=MaterialTheme.typography.bodySmall)}
        item{SectionLine("Photos","")}
        item{SettingsToggle("Auto-fill details",settings.autoFill){vm.updateSettings(settings.copy(autoFill=it))}}
        item{SettingsToggle("Suggest orientation",settings.autoRotate){vm.updateSettings(settings.copy(autoRotate=it))}}
        item{SettingsToggle("Clean cutouts",settings.cleanup){vm.updateSettings(settings.copy(cleanup=it))}}
        item{SectionLine("Outfits","")}
        item{PreferenceSlider("Hats",settings.hatFrequency){vm.updateSettings(settings.copy(hatFrequency=it))}}
        item{PreferenceSlider("Jackets",settings.jacketFrequency){vm.updateSettings(settings.copy(jacketFrequency=it))}}
        item{Choice("Days before repeating",settings.repeatDays.toString(),listOf("0","1","3","7","14","30","60")){vm.updateSettings(settings.copy(repeatDays=it.toInt()))}}
        item{SectionLine("Your data","${storage/1024/1024} MB")}
        item{Button(onClick={backup.launch("Closet-${java.time.LocalDate.now()}.zip")},modifier=Modifier.fillMaxWidth()){Icon(Icons.Outlined.SaveAlt,null);Spacer(Modifier.width(8.dp));Text("Export backup")}}
        item{OutlinedButton(onClick={restore.launch(arrayOf("application/zip","application/octet-stream"))},modifier=Modifier.fillMaxWidth()){Text("Restore backup")}}
        item{Text("Backups contain your photos and wear history. Store them privately; uninstalling removes the on-device closet.",style=MaterialTheme.typography.bodySmall)}
        item{TextButton(onClick={deleting=true}){Text("Delete all data",color=MaterialTheme.colorScheme.error)}}
        item{HorizontalDivider();SectionLine("Closet",dev.closet.BuildConfig.VERSION_NAME);Text("Everything stays on this device.",style=MaterialTheme.typography.bodyMedium);TextButton(onClick={glossary=true}){Text("Glossary")}}
    }
    restoring?.let{uri->AlertDialog(onDismissRequest={restoring=null},title={Text("Replace your closet?")},text={Text("Restore replaces saved pieces, outfits, settings and history. Export a backup first if you want to keep them.")},confirmButton={TextButton(onClick={vm.restore(uri);restoring=null}){Text("Restore")}},dismissButton={TextButton(onClick={restoring=null}){Text("Cancel")}})}
    if(deleting)AlertDialog(onDismissRequest={deleting=false},title={Text("Delete all closet data?")},text={Text("This removes photos, outfits and wear history from this device. Export a backup first.")},confirmButton={TextButton(onClick={vm.deleteAll();deleting=false}){Text("Delete all")}},dismissButton={TextButton(onClick={deleting=false}){Text("Cancel")}})
    if(glossary)AlertDialog(onDismissRequest={glossary=false},title={Text("Glossary")},text={val text=remember{context.assets.open("glossary.md").bufferedReader().use{it.readLines()}.filterNot{it.startsWith("| ---")}.joinToString("\n\n"){line->
        if(line.startsWith("|")) line.trim('|',' ').split("|").joinToString("\n"){it.trim()} else line.trimStart('#',' ')
    }};Text(text,Modifier.heightIn(max=440.dp).verticalScroll(rememberScrollState()),style=MaterialTheme.typography.bodySmall)},confirmButton={TextButton(onClick={glossary=false}){Text("Done")}})
}
@Composable private fun SettingsToggle(title: String,value: Boolean,change: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(title,Modifier.weight(1f));Switch(value,change)}
}
@Composable private fun PreferenceSlider(title: String,value: Float,change: (Float)->Unit) {
    var local by remember(value){mutableFloatStateOf(value)}
    Column{Text("$title · ${(local*100).toInt()}%",style=MaterialTheme.typography.labelLarge);Slider(local,{local=it},onValueChangeFinished={change(local)})}
}
@Composable
fun SmartBuilder(vm: ClosetViewModel,items: List<ClothingItem>) {
    OutfitBuilder(items,vm.preview,vm.current,vm.favorite,vm::shuffle,{vm.saveOutfit()},{vm.saveOutfit(!vm.favorite)},vm::swap,
        occasion=vm.occasion,onOccasion=vm::changeOccasion,initialSeason=vm.settings.season(),options=vm.options,onOption=vm::selectOption,
        locks=vm.locks,onLock=vm::toggleLock,onFeedback=vm::feedback)
}
