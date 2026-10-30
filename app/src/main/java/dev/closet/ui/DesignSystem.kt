package dev.closet.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.closet.R

val ClosetAccent = Color(0xFFB13D68)
val ClosetShape = RoundedCornerShape(24.dp)
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val ClosetFont = FontFamily(
    Font(R.font.nunito, weight = FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.nunito, weight = FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.nunito, weight = FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

@Composable
fun ClosetTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFFFFB1CC), onPrimary = Color(0xFF5B1735), primaryContainer = Color(0xFF542D43), onPrimaryContainer = Color(0xFFFFD9E7),
        secondary = Color(0xFFD6BEFF), onSecondary = Color(0xFF38225C), secondaryContainer = Color(0xFF413352), onSecondaryContainer = Color(0xFFECDFFF),
        tertiary = Color(0xFFFFC1A7), onTertiary = Color(0xFF512B1C), tertiaryContainer = Color(0xFF53362D), onTertiaryContainer = Color(0xFFFFDDCC),
        background = Color(0xFF19151D), onBackground = Color(0xFFF7EDF5), surface = Color(0xFF251F2A), onSurface = Color(0xFFF7EDF5),
        surfaceVariant = Color(0xFF362B3B), onSurfaceVariant = Color(0xFFD4C3D0), outline = Color(0xFFA18B9F), outlineVariant = Color(0xFF4D3E50),
        surfaceContainerLowest = Color(0xFF151018), surfaceContainerLow = Color(0xFF211A26), surfaceContainer = Color(0xFF2A2230), surfaceContainerHigh = Color(0xFF34293B), surfaceContainerHighest = Color(0xFF3D3145)
    ) else lightColorScheme(
        primary = ClosetAccent, onPrimary = Color.White, primaryContainer = Color(0xFFFFDEEB), onPrimaryContainer = Color(0xFF6F2342),
        secondary = Color(0xFF7654A6), onSecondary = Color.White, secondaryContainer = Color(0xFFEDE2FF), onSecondaryContainer = Color(0xFF4B326E),
        tertiary = Color(0xFF8D4B30), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFE8D8), onTertiaryContainer = Color(0xFF66321F),
        background = Color(0xFFFFF8FC), onBackground = Color(0xFF342733), surface = Color.White, onSurface = Color(0xFF342733),
        surfaceVariant = Color(0xFFF4EAF5), onSurfaceVariant = Color(0xFF756474), outline = Color(0xFF947E91), outlineVariant = Color(0xFFEADCE8),
        surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFF2F8), surfaceContainer = Color(0xFFF9EDF5), surfaceContainerHigh = Color(0xFFF4E6F1), surfaceContainerHighest = Color(0xFFEFDFED)
    )
    fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(fontFamily = ClosetFont, fontSize = size.sp, lineHeight = height.sp, fontWeight = weight)
    MaterialTheme(colorScheme = scheme, shapes = Shapes(RoundedCornerShape(12.dp), RoundedCornerShape(16.dp), ClosetShape, RoundedCornerShape(28.dp), RoundedCornerShape(32.dp)),
        typography = Typography(displayLarge = type(32, 40, FontWeight.Bold), headlineLarge = type(30, 38, FontWeight.Bold), headlineMedium = type(24, 32, FontWeight.Bold),
            titleLarge = type(22, 30, FontWeight.Bold), titleMedium = type(16, 22, FontWeight.SemiBold), bodyLarge = type(16, 24), bodyMedium = type(14, 21), bodySmall = type(12, 18),
            labelLarge = type(14, 20, FontWeight.Bold), labelMedium = type(12, 18, FontWeight.SemiBold), labelSmall = type(11, 16)), content = content)
}

@Composable
fun MicroLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, modifier, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
fun ClosetNavigation(page: Int, enabled: Boolean, onPage: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 2.dp) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().selectableGroup().padding(horizontal = 10.dp, vertical = 10.dp)) {
            listOf("Closet" to Icons.Outlined.Checkroom, "Outfits" to Icons.Outlined.AutoAwesome, "Saved" to Icons.Outlined.FavoriteBorder).forEachIndexed { i, (label, icon) ->
                val selected = page == i
                val background by animateColorAsState(if(selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, label = "tab color")
                val scale by animateFloatAsState(if(selected) 1.12f else 1f, spring(dampingRatio = .65f, stiffness = 500f), label = "tab bounce")
                Column(Modifier.weight(1f).selectable(selected, enabled, Role.Tab) { onPage(i) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.width(60.dp).height(32.dp).clip(RoundedCornerShape(20.dp)).background(background), contentAlignment = Alignment.Center) {
                        Icon(icon, null, Modifier.size(22.dp).graphicsLayer(scaleX = scale, scaleY = scale), tint = if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1, color = if(selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun PhotoStage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val glow = MaterialTheme.colorScheme.primary.copy(alpha = .035f)
    Box(modifier.clip(ClosetShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
        Canvas(Modifier.matchParentSize()) { drawCircle(glow, size.minDimension * .42f, Offset(size.width * .68f, size.height * .4f)) }
        content()
    }
}

@Composable
fun SoftIllustration(saved: Boolean = false) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val scale by animateFloatAsState(if(entered) 1f else .85f, spring(dampingRatio = .65f), label = "illustration pop")
    Box(Modifier.size(176.dp, 150.dp).graphicsLayer(scaleX = scale, scaleY = scale), contentAlignment = Alignment.Center) {
        Surface(Modifier.size(96.dp, 114.dp).graphicsLayer(rotationZ = -15f, translationX = -28f), shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.secondaryContainer) {}
        Surface(Modifier.size(96.dp, 114.dp).graphicsLayer(rotationZ = 12f, translationX = 25f), shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {}
        Surface(Modifier.size(100.dp, 120.dp), shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) { Icon(if(saved) Icons.Outlined.FavoriteBorder else Icons.Outlined.Checkroom, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary) }
        }
        Icon(Icons.Outlined.AutoAwesome, null, Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp), tint = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun AddPieceAction(onClick: () -> Unit) {
    ExtendedFloatingActionButton(shape = RoundedCornerShape(28.dp), containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, onClick = onClick,
        icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add clothes") })
}

@Composable
fun BouncyCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed) .97f else 1f, spring(dampingRatio = .7f, stiffness = 600f), label = "card press")
    Surface(onClick = onClick, interactionSource = interaction, shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale)) { content() }
}

@Composable
fun SectionLine(left: String, right: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(left, style = MaterialTheme.typography.titleMedium)
        MicroLabel(right)
    }
}

@Composable
fun OutfitGuide() {
    val top = MaterialTheme.colorScheme.primary
    val bottom = MaterialTheme.colorScheme.secondary
    Box(Modifier.fillMaxWidth().height(224.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.width(160.dp).fillMaxHeight()) {
            val s = minOf(size.width / 180f, size.height / 280f)
            val left = (size.width - 180*s) / 2
            fun piece(points: List<Pair<Float, Float>>, color: Color) {
                val path = androidx.compose.ui.graphics.Path().apply {
                    points.forEachIndexed { i, (x,y) -> if(i == 0) moveTo(left+x*s,y*s) else lineTo(left+x*s,y*s) }; close()
                }
                drawPath(path, color.copy(alpha = .3f))
            }
            piece(listOf(60f to 26f,76f to 20f,82f to 31f,98f to 31f,104f to 20f,120f to 26f,150f to 65f,129f to 80f,119f to 65f,119f to 124f,61f to 124f,61f to 65f,51f to 80f,30f to 65f), top)
            piece(listOf(62f to 140f,118f to 140f,128f to 226f,101f to 226f,90f to 171f,79f to 226f,52f to 226f), bottom)
            piece(listOf(52f to 242f,76f to 242f,80f to 262f,32f to 262f,33f to 254f), top)
            piece(listOf(104f to 242f,128f to 242f,147f to 254f,148f to 262f,100f to 262f), top)
        }
    }
}
