@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.dashboardwol

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dashboardwol.ui.theme.DashboardWOLTheme
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlinx.coroutines.delay
import java.net.InetAddress

@Suppress("DEPRECATION")

// Add this enum class at the top level of your file
enum class FontType {
    RESOURCE,  // Custom font from res/font
    SYSTEM     // System font
}

// Update the data structure to hold font information
data class FontInfo(
    val name: String,
    val resourceId: Int = 0,
    val type: FontType = FontType.RESOURCE,
    val systemFont: String = ""
)

class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            DashboardWOLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WolControlScreen()
                }
            }
        }
    }
}

@Composable
fun WolControlScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("WolPrefs", Context.MODE_PRIVATE) }

    // MAC Address states
    var mac1 by remember { mutableStateOf(prefs.getString("mac1", "34:5A:60:13:39:CD") ?: "34:5A:60:13:39:CD") }
    var mac2 by remember { mutableStateOf(prefs.getString("mac2", "60:CF:84:63:0E:58") ?: "60:CF:84:63:0E:58") }
    var mac3 by remember { mutableStateOf(prefs.getString("mac3", "12:34:56:78:90:AB") ?: "12:34:56:78:90:AB") }

    // Display preferences
    var showTimePreference by remember { mutableStateOf(prefs.getBoolean("showTime", true)) }
    var buttonText1 by remember { mutableStateOf(prefs.getString("buttonText1", "1") ?: "1") }
    var buttonText2 by remember { mutableStateOf(prefs.getString("buttonText2", "2") ?: "2") }
    var buttonText3 by remember { mutableStateOf(prefs.getString("buttonText3", "3") ?: "3") }

    // Size preferences
    var buttonWidth by remember { mutableStateOf(prefs.getFloat("buttonWidth", 300f)) }
    var buttonHeight by remember { mutableStateOf(prefs.getFloat("buttonHeight", 150f)) }
    var fontSize by remember { mutableStateOf(prefs.getFloat("fontSize", 200f)) }

    // Color preferences
    val defaultButtonColor = Color(0xFF0072BD)
    val defaultTextColor = Color.White
    var buttonColor by remember {
        mutableStateOf(
            Color(
                prefs.getLong(
                    "buttonColor",
                    defaultButtonColor.value.toLong()
                ).toULong()
            )
        )
    }
    var textColor by remember {
        mutableStateOf(
            Color(
                prefs.getLong(
                    "textColor",
                    defaultTextColor.value.toLong()
                ).toULong()
            )
        )
    }

    // Font preference
    val availableFonts = remember { getFontResources(context) }
    var fontRes by remember {
        mutableStateOf(prefs.getInt("fontRes", R.font.lcddot))
    }

    // Selected font info
    var selectedFontInfo by remember(fontRes) {
        mutableStateOf(
            availableFonts.firstOrNull {
                it.type == FontType.RESOURCE && it.resourceId == fontRes
            } ?: availableFonts.first()
        )
    }

    // Get font family based on selected font info
    val fontFamily by remember(selectedFontInfo) {
        mutableStateOf(loadFontFamily(selectedFontInfo))
    }

    // Network settings
    var broadcastIp by remember {
        mutableStateOf(prefs.getString("broadcastIp", "192.168.50.255") ?: "192.168.50.255")
    }

    // Other states
    var showDialog by remember { mutableStateOf(false) }
    var editingMac by remember { mutableStateOf("") }
    var currentMacType by remember { mutableStateOf<Int?>(null) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeParts = timeFormatter.format(Date(currentTime)).split(":")

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current.density

    val screenWidthPx = configuration.screenWidthDp * density
    val screenHeightPx = configuration.screenHeightDp * density

    val buttonWidthDp = buttonWidth.dp
    val buttonHeightDp = buttonHeight.dp
    val spacingDp = 16.dp

    val buttonWidthPx = with(LocalDensity.current) { buttonWidthDp.toPx() }
    val buttonHeightPx = with(LocalDensity.current) { buttonHeightDp.toPx() }
    val spacingPx = with(LocalDensity.current) { spacingDp.toPx() }

    val groupWidthPx = buttonWidthPx
    val groupHeightPx = buttonHeightPx * 3 + spacingPx * 2

    val maxX = screenWidthPx - groupWidthPx
    val maxY = screenHeightPx - groupHeightPx

    var groupPosition by remember {
        mutableStateOf(
            Offset(
                x = Random.nextFloat() * maxX,
                y = Random.nextFloat() * maxY
            )
        )
    }

    var groupDirection by remember {
        mutableStateOf(
            Offset(
                x = 48f * (if (Random.nextBoolean()) 1 else -1),
                y = 48f * (if (Random.nextBoolean()) 1 else -1)
            )
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            var newX = groupPosition.x + groupDirection.x
            var newY = groupPosition.y + groupDirection.y

            if (newX < 0 || newX > maxX) {
                groupDirection = groupDirection.copy(x = -groupDirection.x)
                newX = groupPosition.x.coerceIn(0f, maxX)
            }
            if (newY < 0 || newY > maxY) {
                groupDirection = groupDirection.copy(y = -groupDirection.y)
                newY = groupPosition.y.coerceIn(0f, maxY)
            }
            groupPosition = Offset(newX, newY)
        }
    }

    val executor = Executors.newSingleThreadExecutor()
    fun sendWolPacket(mac: String) {
        executor.execute {
            try {
                val macBytes = mac.split(":", "-").map { it.toInt(16).toByte() }.toByteArray()
                val bytes = ByteArray(6 + 16 * macBytes.size) {
                    if (it < 6) 0xFF.toByte() else macBytes[it % macBytes.size]
                }
                val address = InetAddress.getByName(broadcastIp)
                val packet = DatagramPacket(bytes, bytes.size, address, 9)
                DatagramSocket().use { socket ->
                    socket.send(packet)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Edit MAC Address") },
            text = {
                TextField(
                    value = editingMac,
                    onValueChange = { editingMac = it },
                    label = { Text("New MAC Address") },
                    placeholder = { Text("Format: 01:23:45:67:89:AB") }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isValidMac(editingMac)) {
                            when (currentMacType) {
                                1 -> mac1 = editingMac
                                2 -> mac2 = editingMac
                                3 -> mac3 = editingMac
                            }
                            prefs.edit().putString("mac${currentMacType}", editingMac).apply()
                            showDialog = false
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) { Text("Cancel") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Surface(
                color = Color(0xFF1A1A1A),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Top
                ) {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall, color = Color.White)

                    // Display Settings
                    Text("Display Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                        Text("Show time",
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = showTimePreference,
                            onCheckedChange = {
                                showTimePreference = it
                                prefs.edit().putBoolean("showTime", it).apply()
                            }
                        )
                    }

                    if (!showTimePreference) {
                        OutlinedTextField(
                            value = buttonText1,
                            onValueChange = {
                                buttonText1 = it
                                prefs.edit().putString("buttonText1", it).apply()
                            },
                            label = { Text("Button 1 Text") },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = buttonText2,
                            onValueChange = {
                                buttonText2 = it
                                prefs.edit().putString("buttonText2", it).apply()
                            },
                            label = { Text("Button 2 Text") },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        OutlinedTextField(
                            value = buttonText3,
                            onValueChange = {
                                buttonText3 = it
                                prefs.edit().putString("buttonText3", it).apply()
                            },
                            label = { Text("Button 3 Text") },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Divider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))

                    // Size Settings
                    Text("Size Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    SizeSlider(
                        label = "Button Width",
                        value = buttonWidth,
                        onValueChange = { buttonWidth = it; prefs.edit().putFloat("buttonWidth", it).apply() },
                        range = 100f..300f
                    )
                    SizeSlider(
                        label = "Button Height",
                        value = buttonHeight,
                        onValueChange = { buttonHeight = it; prefs.edit().putFloat("buttonHeight", it).apply() },
                        range = 100f..200f
                    )
                    SizeSlider(
                        label = "Font Size",
                        value = fontSize,
                        onValueChange = { fontSize = it; prefs.edit().putFloat("fontSize", it).apply() },
                        range = 100f..400f
                    )

                    Divider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))

                    // Color Settings
                    Text("Color Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    ColorPicker(
                        title = "Button Color",
                        selectedColor = buttonColor,
                        onColorSelected = {
                            buttonColor = it
                            prefs.edit().putLong("buttonColor", it.value.toLong()).apply()
                        }
                    )
                    ColorPicker(
                        title = "Text Color",
                        selectedColor = textColor,
                        onColorSelected = {
                            textColor = it
                            prefs.edit().putLong("textColor", it.value.toLong()).apply()
                        }
                    )

                    Divider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))

                    // Font Settings
                    Text("Font Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    FontDropdown(
                        fonts = availableFonts,
                        selectedFontRes = fontRes,
                        onFontSelected = { newFontInfo ->
                            selectedFontInfo = newFontInfo
                            if (newFontInfo.type == FontType.RESOURCE) {
                                fontRes = newFontInfo.resourceId
                                prefs.edit().putInt("fontRes", newFontInfo.resourceId).apply()
                            } else {
                                // For system fonts, we can store a special value or
                                // add additional preference fields if needed
                                // For now, we'll just use the first font resource as a placeholder
                                // to maintain compatibility with the existing code
                                fontRes = R.font.lcddot
                                prefs.edit().putInt("fontRes", R.font.lcddot).apply()
                            }
                        }
                    )

                    Divider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))

                    // Network Settings
                    Text("Network Settings",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    OutlinedTextField(
                        value = broadcastIp,
                        onValueChange = {
                            broadcastIp = it
                            prefs.edit().putString("broadcastIp", it).apply()
                        },
                        label = { Text("Broadcast IP Address") },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(groupPosition.x.toInt(), groupPosition.y.toInt()) }
                    .size(buttonWidthDp, buttonHeightDp * 3 + spacingDp * 2)
            ) {
                WolButton(
                    text = if (showTimePreference) timeParts[0] else buttonText1,
                    isTimeDisplay = showTimePreference,
                    onClick = { sendWolPacket(mac1) },
                    onLongClick = {
                        editingMac = mac1
                        currentMacType = 1
                        showDialog = true
                    },
                    modifier = Modifier
                        .size(buttonWidthDp, buttonHeightDp)
                        .align(Alignment.TopStart),
                    fontSize = fontSize.sp,
                    fontFamily = fontFamily,
                    buttonColor = buttonColor,
                    textColor = textColor
                )

                WolButton(
                    text = if (showTimePreference) timeParts[1] else buttonText2,
                    isTimeDisplay = showTimePreference,
                    onClick = { sendWolPacket(mac2) },
                    onLongClick = {
                        editingMac = mac2
                        currentMacType = 2
                        showDialog = true
                    },
                    modifier = Modifier
                        .size(buttonWidthDp, buttonHeightDp)
                        .align(Alignment.CenterStart),
                    fontSize = fontSize.sp,
                    fontFamily = fontFamily,
                    buttonColor = buttonColor,
                    textColor = textColor
                )

                WolButton(
                    text = if (showTimePreference) timeParts[2] else buttonText3,
                    isTimeDisplay = showTimePreference,
                    onClick = { sendWolPacket(mac3) },
                    onLongClick = {
                        editingMac = mac3
                        currentMacType = 3
                        showDialog = true
                    },
                    modifier = Modifier
                        .size(buttonWidthDp, buttonHeightDp)
                        .align(Alignment.BottomStart),
                    fontSize = fontSize.sp,
                    fontFamily = fontFamily,
                    buttonColor = buttonColor,
                    textColor = textColor
                )
            }
        }
    }
}

@Composable
fun ColorPicker(title: String, selectedColor: Color, onColorSelected: (Color) -> Unit) {
    val colors = listOf(
        Color.Black,       // Black
        Color.White,       // White
        Color(0xFF0072BD), // Dark Blue
        Color(0xFFD95319), // Dark Orange
        Color(0xFFEDB120), // Dark Yellow
        Color(0xFF7E2F8E), // Dark Purple
        Color(0xFF77AC30), // Medium Green
        Color(0xFF4DBEEE), // Light Blue
        Color(0xFFA2142F)  // Dark Red
    )

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = title, color = Color.White)
        Spacer(modifier = Modifier.padding(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .padding(2.dp)
                        .combinedClickable(onClick = { onColorSelected(color) })
                ) {
                    Surface(
                        color = color,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxSize(),
                        border = if (color == selectedColor) androidx.compose.foundation.BorderStroke(2.dp, Color.White) else null
                    ) {}
                }
            }
        }
    }
}

@Composable
fun FontDropdown(
    fonts: List<FontInfo>,
    selectedFontRes: Int,
    onFontSelected: (FontInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // Find the selected font info or default to the first font
    val selectedFont by remember(selectedFontRes, fonts) {
        mutableStateOf(
            fonts.firstOrNull {
                it.type == FontType.RESOURCE && it.resourceId == selectedFontRes
            } ?: fonts.firstOrNull() ?: FontInfo("Default")
        )
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedFont.name,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            textStyle = LocalTextStyle.current.copy(color = Color.White),
            colors = ExposedDropdownMenuDefaults.textFieldColors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedTrailingIconColor = Color.White,
                unfocusedTrailingIconColor = Color.White,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            fonts.forEach { fontInfo ->
                DropdownMenuItem(
                    text = { Text(fontInfo.name, color = Color.White) },
                    onClick = {
                        onFontSelected(fontInfo)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun getFontResources(context: Context): List<FontInfo> {
    val fontList = mutableListOf<FontInfo>()

    // Add custom fonts from res/font
    fontList.add(FontInfo("LCD Dot (alt)", R.font.lcddot, FontType.RESOURCE))
    fontList.add(FontInfo("Pixel Bus", R.font.pixelbus, FontType.RESOURCE))

    // Add system fonts
    fontList.add(FontInfo("Sans Serif", type = FontType.SYSTEM, systemFont = "sans-serif"))
    fontList.add(FontInfo("Serif", type = FontType.SYSTEM, systemFont = "serif"))
    fontList.add(FontInfo("Monospace", type = FontType.SYSTEM, systemFont = "monospace"))
    fontList.add(FontInfo("Sans Serif Condensed", type = FontType.SYSTEM, systemFont = "sans-serif-condensed"))
    fontList.add(FontInfo("Sans Serif Light", type = FontType.SYSTEM, systemFont = "sans-serif-light"))
    fontList.add(FontInfo("Sans Serif Medium", type = FontType.SYSTEM, systemFont = "sans-serif-medium"))
    fontList.add(FontInfo("Sans Serif Black", type = FontType.SYSTEM, systemFont = "sans-serif-black"))
    fontList.add(FontInfo("Casual", type = FontType.SYSTEM, systemFont = "casual"))
    fontList.add(FontInfo("Cursive", type = FontType.SYSTEM, systemFont = "cursive"))

    return fontList
}

fun loadFontFamily(fontInfo: FontInfo): FontFamily {
    return when (fontInfo.type) {
        FontType.RESOURCE -> FontFamily(Font(fontInfo.resourceId, FontWeight.Normal))
        FontType.SYSTEM -> FontFamily.Default
    }
}

@Composable
fun SizeSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("$label: ${value.toInt()} dp", color = Color.White)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = 20,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun WolButton(
    text: String,
    isTimeDisplay: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    buttonColor: Color,
    textColor: Color
) {
    Surface(
        modifier = modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = buttonColor,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isTimeDisplay) {
                val digits = remember(text) {
                    if (text.length == 2) text.toList() else listOf('0', text[0])
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    digits.forEachIndexed { index, digit ->
                        AnimatedContent(
                            targetState = digit,
                            transitionSpec = {
                                val slideDirection = if (targetState > initialState) -1 else 1
                                slideInVertically { height -> slideDirection * height } + fadeIn() with
                                        slideOutVertically { height -> -slideDirection * height } + fadeOut()
                            },
                            contentAlignment = Alignment.Center
                        ) { targetChar ->
                            Text(
                                text = targetChar.toString(),
                                style = TextStyle(
                                    fontFamily = fontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = fontSize,
                                    letterSpacing = (-0.1).sp
                                ),
                                color = textColor,
                                modifier = Modifier.offset(y = 15.dp)
                            )
                        }
                        if (index == 0) Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            } else {
                Text(
                    text = text,
                    style = TextStyle(
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = fontSize,
                        letterSpacing = (-0.1).sp
                    ),
                    color = textColor,
                    modifier = Modifier.offset(y = 15.dp)
                )
            }
        }
    }
}

fun isValidMac(mac: String): Boolean {
    val pattern = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()
    return pattern.matches(mac)
}

fun getFontFamilyById(fontRes: Int, fonts: List<FontInfo>): FontFamily {
    val fontInfo = fonts.firstOrNull { it.type == FontType.RESOURCE && it.resourceId == fontRes }
    return if (fontInfo != null) {
        loadFontFamily(fontInfo)
    } else {
        FontFamily(Font(fontRes, FontWeight.Normal))
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewWolControlScreen() {
    DashboardWOLTheme {
        WolControlScreen()
    }
}
