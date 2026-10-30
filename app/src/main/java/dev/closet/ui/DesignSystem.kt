package dev.closet.ui

import android.graphics.Typeface
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ClosetBlue = Color(0xFF214CFF)
val ClosetShape = RoundedCornerShape(4.dp)
val DisplayFace = FontFamily(Typeface.create("sans-serif-condensed", Typeface.BOLD))
val LabelFace = FontFamily.Monospace

@Composable
fun ClosetTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val scheme = if (dark) darkColorScheme(
        primary = Color(0xFFA9BEFF), onPrimary = Color(0xFF082567), primaryContainer = ClosetBlue, onPrimaryContainer = Color.White,
        secondary = Color(0xFFFFAA9D), onSecondary = Color(0xFF531408), secondaryContainer = Color(0xFF293853), onSecondaryContainer = Color(0xFFDAE4FF),
        tertiary = Color(0xFFFFAA9D), onTertiary = Color(0xFF531408), tertiaryContainer = Color(0xFF5F2116), onTertiaryContainer = Color(0xFFFFDBD3),
        surfaceTint = Color(0xFFA9BEFF), surfaceContainerLowest = Color(0xFF080B12), surfaceContainerLow = Color(0xFF141B29), surfaceContainer = Color(0xFF1B2333), surfaceContainerHigh = Color(0xFF222D41), surfaceContainerHighest = Color(0xFF2A3851),
        background = Color(0xFF0B0E15), onBackground = Color(0xFFF4F6FF),
        surface = Color(0xFF111722), onSurface = Color(0xFFF4F6FF),
        surfaceVariant = Color(0xFF1C283D), onSurfaceVariant = Color(0xFFB9C5DF),
        outline = Color(0xFF8390AC), outlineVariant = Color(0xFF344157)
    ) else lightColorScheme(
        primary = ClosetBlue, onPrimary = Color.White, primaryContainer = Color(0xFFDDE6FF), onPrimaryContainer = Color(0xFF102D8A),
        secondary = Color(0xFFA22E1B), onSecondary = Color.White, secondaryContainer = Color(0xFFE4EAF8), onSecondaryContainer = Color(0xFF1E2B48),
        tertiary = Color(0xFFA22E1B), onTertiary = Color.White, tertiaryContainer = Color(0xFFFFE0DA), onTertiaryContainer = Color(0xFF64190A),
        surfaceTint = ClosetBlue, surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF0F3FA), surfaceContainer = Color(0xFFEAEFF9), surfaceContainerHigh = Color(0xFFE4EAF6), surfaceContainerHighest = Color(0xFFDCE4F4),
        background = Color(0xFFF4F6FC), onBackground = Color(0xFF10131C),
        surface = Color.White, onSurface = Color(0xFF10131C),
        surfaceVariant = Color(0xFFE7ECF8), onSurfaceVariant = Color(0xFF495570),
        outline = Color(0xFF6F7B93), outlineVariant = Color(0xFFC9D2E5)
    )
    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(ClosetShape, ClosetShape, RoundedCornerShape(6.dp), RoundedCornerShape(8.dp), RoundedCornerShape(12.dp)),
        typography = Typography(
            displayLarge = TextStyle(fontFamily = DisplayFace, fontSize = 76.sp, lineHeight = 74.sp, letterSpacing = (-2).sp),
            headlineLarge = TextStyle(fontFamily = DisplayFace, fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-1).sp),
            headlineMedium = TextStyle(fontFamily = DisplayFace, fontSize = 32.sp, lineHeight = 34.sp),
            titleLarge = TextStyle(fontFamily = DisplayFace, fontSize = 26.sp, lineHeight = 28.sp),
            titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
            bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 21.sp),
            bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp),
            labelMedium = TextStyle(fontFamily = LabelFace, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = .5.sp),
            labelSmall = TextStyle(fontFamily = LabelFace, fontSize = 10.sp, lineHeight = 14.sp, letterSpacing = .7.sp)
        ), content = content
    )
}

@Composable
fun MicroLabel(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text.uppercase(), modifier, style = MaterialTheme.typography.labelMedium, color = color)
}

@Composable
fun ClosetNavigation(page: Int, enabled: Boolean, onPage: (Int) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column(Modifier.navigationBarsPadding()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth().selectableGroup()) {
                listOf("Closet" to Icons.Outlined.Checkroom, "Style" to Icons.Outlined.Tune, "Saved" to Icons.Outlined.Bookmarks).forEachIndexed { i, (label, icon) ->
                    val active = i == page
                    Column(Modifier.weight(1f).heightIn(min = 58.dp).selectable(active, enabled, Role.Tab) { onPage(i) }.padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            Icon(icon, null, Modifier.size(20.dp), tint = if(active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(label, style = MaterialTheme.typography.labelLarge, color = if(active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(9.dp))
                        Box(Modifier.height(3.dp).fillMaxWidth(.55f).background(if(active) MaterialTheme.colorScheme.primary else Color.Transparent))
                    }
                }
            }
        }
    }
}

/** Quiet registration grid behind real cutouts; never substitutes sample wardrobe data. */
@Composable
fun PhotoStage(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        Canvas(Modifier.matchParentSize()) {
            val step = 32.dp.toPx()
            var x = 0f
            while (x < size.width) { drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f); x += step }
            var y = 0f
            while (y < size.height) { drawLine(line, Offset(0f, y), Offset(size.width, y), 1f); y += step }
        }
        content()
    }
}

@Composable
fun WardrobePoster(modifier: Modifier = Modifier, saved: Boolean = false) {
    Box(modifier.fillMaxWidth().background(ClosetBlue)) {
        Canvas(Modifier.matchParentSize()) {
            val s = size.width / 400f
            // Garment drawing, not a pretend photograph or a saved item.
            val shirt = Path().apply {
                moveTo(244*s, 35*s); lineTo(275*s, 24*s); cubicTo(281*s, 43*s, 303*s, 43*s, 311*s, 24*s)
                lineTo(341*s, 35*s); lineTo(374*s, 78*s); lineTo(344*s, 99*s); lineTo(327*s, 79*s)
                lineTo(327*s, 177*s); lineTo(259*s, 177*s); lineTo(259*s, 79*s); lineTo(242*s, 99*s)
                lineTo(212*s, 78*s); close()
            }
            drawPath(shirt, Color.White.copy(alpha = .12f))
            drawPath(shirt, Color.White.copy(alpha = .8f), style = Stroke(1.5.dp.toPx()))
            drawLine(Color.White.copy(alpha = .35f), Offset(222*s, 198*s), Offset(384*s, 198*s), 1f)
            drawCircle(Color.White, 4*s, Offset(376*s, 20*s), style = Stroke(1f))
        }
        Column(Modifier.padding(22.dp).widthIn(max = 245.dp)) {
            MicroLabel(if(saved) "The lookbook / 03" else "Personal collection / 01", color = Color.White.copy(alpha = .8f))
            Spacer(Modifier.height(20.dp))
            Text(if(saved) "GOOD LOOKS.\nON REPEAT." else "LESS SCROLL.\nMORE STYLE.", style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Spacer(Modifier.height(22.dp))
            MicroLabel(if(saved) "Made by you. Kept here." else "Wear what you already own.", color = Color.White)
        }
    }
}

@Composable
fun SectionLine(left: String, right: String, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        MicroLabel(left)
        MicroLabel(right, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun AddPieceAction(onClick: () -> Unit) {
    ExtendedFloatingActionButton(shape = ClosetShape, containerColor = ClosetBlue, contentColor = Color.White, onClick = onClick,
        icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("Add piece") })
}

@Composable
fun OutfitGuide() {
    val ink = MaterialTheme.colorScheme.primary
    Box(Modifier.fillMaxWidth().height(236.dp).padding(16.dp)) {
        Canvas(Modifier.fillMaxHeight().fillMaxWidth(.7f)) {
            val scale = minOf(size.width / 180f, size.height / 310f)
            val left = (size.width - 180 * scale) / 2
            fun garment(points: List<Pair<Float, Float>>) {
                val path = Path().apply {
                    points.forEachIndexed { i, (x, y) -> if(i == 0) moveTo(left + x * scale, y * scale) else lineTo(left + x * scale, y * scale) }
                    close()
                }
                drawPath(path, ink.copy(alpha = .08f))
                drawPath(path, ink.copy(alpha = .8f), style = Stroke(1.3.dp.toPx()))
            }
            garment(listOf(62f to 35f, 68f to 10f, 108f to 10f, 114f to 35f, 132f to 42f, 48f to 42f))
            garment(listOf(60f to 66f, 76f to 60f, 82f to 71f, 98f to 71f, 104f to 60f, 120f to 66f, 150f to 105f, 129f to 120f, 119f to 105f, 119f to 164f, 61f to 164f, 61f to 105f, 51f to 120f, 30f to 105f))
            garment(listOf(62f to 180f, 118f to 180f, 128f to 266f, 101f to 266f, 90f to 211f, 79f to 266f, 52f to 266f))
            garment(listOf(52f to 282f, 76f to 282f, 80f to 302f, 32f to 302f, 33f to 294f))
            garment(listOf(104f to 282f, 128f to 282f, 147f to 294f, 148f to 302f, 100f to 302f))
        }
        Column(Modifier.align(Alignment.CenterEnd).fillMaxHeight(), verticalArrangement = Arrangement.SpaceAround) {
            listOf("01 / Hat", "02 / Top", "03 / Bottom", "04 / Shoes").forEach { MicroLabel(it) }
        }
    }
}
