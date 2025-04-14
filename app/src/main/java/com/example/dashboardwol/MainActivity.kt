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
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import com.example.dashboardwol.ui.theme.DashboardWOLTheme
import kotlinx.coroutines.delay
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()

    @SuppressLint("SourceLockedOrientationActivity", "ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set portrait orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Handle fullscreen differently based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Modern approach for Android 11+ (API 30+)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        } else {
            // Legacy approach for older Android versions
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        // Keep screen on
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enable edge-to-edge display (only beneficial on newer Android versions)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            enableEdgeToEdge()
        }

        setContent {
            DashboardWOLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WolControlScreen(executor)
                }
            }
        }
    }

    override fun onDestroy() {
        // Properly shutdown the executor to avoid memory leaks
        executor.shutdown()
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (_: InterruptedException) {
            executor.shutdownNow()
        }
        super.onDestroy()
    }
}

@Composable
fun WolControlScreen(executor: ExecutorService) {
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
    var buttonWidth by remember { mutableFloatStateOf(prefs.getFloat("buttonWidth", 300f)) }
    var buttonHeight by remember { mutableFloatStateOf(prefs.getFloat("buttonHeight", 150f)) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("fontSize", 200f)) }

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

    // Fixed font family - always use lcddot.ttf
    val fontFamily = FontFamily(Font(R.font.lcddot, FontWeight.Normal))

    // Network settings
    var broadcastIp by remember {
        mutableStateOf(prefs.getString("broadcastIp", "192.168.50.255") ?: "192.168.50.255")
    }

    // Other states
    var showDialog by remember { mutableStateOf(false) }
    var editingMac by remember { mutableStateOf("") }
    var currentMacType by remember { mutableStateOf<Int?>(null) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var lastError by remember { mutableStateOf<String?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Clock timer effect with lifecycle awareness
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeParts = timeFormatter.format(Date(currentTime)).split(":")

    // Screen configuration and positioning
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

    // Button group position and movement
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

    // Movement effect with lifecycle awareness
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

    // Function to send Wake-on-LAN packet
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
                lastError = null
            } catch (e: Exception) {
                e.printStackTrace()
                lastError = "Error: ${e.message}"
            }
        }
    }

    // MAC Address Edit Dialog
    if (showDialog) {
        MacAddressDialog(
            initialMac = editingMac,
            onDismiss = { showDialog = false },
            onConfirm = { newMac ->
                if (isValidMac(newMac)) {
                    when (currentMacType) {
                        1 -> mac1 = newMac
                        2 -> mac2 = newMac
                        3 -> mac3 = newMac
                    }
                    prefs.edit { putString("mac${currentMacType}", newMac) }
                    showDialog = false
                }
            }
        )
    }

    // Error dialog if needed
    lastError?.let { error ->
        AlertDialog(
            onDismissRequest = { lastError = null },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = { lastError = null }) {
                    Text("OK")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SettingsDrawerContent(
                showTimePreference = showTimePreference,
                onShowTimeChange = {
                    showTimePreference = it
                    prefs.edit { putBoolean("showTime", it) }
                },
                buttonText1 = buttonText1,
                onButtonText1Change = {
                    buttonText1 = it
                    prefs.edit { putString("buttonText1", it) }
                },
                buttonText2 = buttonText2,
                onButtonText2Change = {
                    buttonText2 = it
                    prefs.edit { putString("buttonText2", it) }
                },
                buttonText3 = buttonText3,
                onButtonText3Change = {
                    buttonText3 = it
                    prefs.edit { putString("buttonText3", it) }
                },
                buttonWidth = buttonWidth,
                onButtonWidthChange = {
                    buttonWidth = it
                    prefs.edit { putFloat("buttonWidth", it) }
                },
                buttonHeight = buttonHeight,
                onButtonHeightChange = {
                    buttonHeight = it
                    prefs.edit { putFloat("buttonHeight", it) }
                },
                fontSize = fontSize,
                onFontSizeChange = {
                    fontSize = it
                    prefs.edit { putFloat("fontSize", it) }
                },
                buttonColor = buttonColor,
                onButtonColorChange = {
                    buttonColor = it
                    prefs.edit { putLong("buttonColor", it.value.toLong()) }
                },
                textColor = textColor,
                onTextColorChange = {
                    textColor = it
                    prefs.edit { putLong("textColor", it.value.toLong()) }
                },
                broadcastIp = broadcastIp,
                onBroadcastIpChange = {
                    broadcastIp = it
                    prefs.edit { putString("broadcastIp", it) }
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            ButtonGroup(
                position = groupPosition,
                buttonWidth = buttonWidthDp,
                buttonHeight = buttonHeightDp,
                spacing = spacingDp,
                showTime = showTimePreference,
                buttonTexts = listOf(
                    if (showTimePreference) timeParts[0] else buttonText1,
                    if (showTimePreference) timeParts[1] else buttonText2,
                    if (showTimePreference) timeParts[2] else buttonText3
                ),
                fontSize = fontSize.sp,
                fontFamily = fontFamily,
                buttonColor = buttonColor,
                textColor = textColor,
                onButtonClick = { index ->
                    when (index) {
                        0 -> sendWolPacket(mac1)
                        1 -> sendWolPacket(mac2)
                        2 -> sendWolPacket(mac3)
                    }
                },
                onButtonLongClick = { index ->
                    editingMac = when (index) {
                        0 -> mac1
                        1 -> mac2
                        else -> mac3
                    }
                    currentMacType = index + 1
                    showDialog = true
                }
            )
        }
    }
}

@Composable
fun MacAddressDialog(
    initialMac: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var mac by remember { mutableStateOf(initialMac) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit MAC Address") },
        text = {
            TextField(
                value = mac,
                onValueChange = { mac = it },
                label = { Text("New MAC Address") },
                placeholder = { Text("Format: 01:23:45:67:89:AB") }
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(mac) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SettingsDrawerContent(
    showTimePreference: Boolean,
    onShowTimeChange: (Boolean) -> Unit,
    buttonText1: String,
    onButtonText1Change: (String) -> Unit,
    buttonText2: String,
    onButtonText2Change: (String) -> Unit,
    buttonText3: String,
    onButtonText3Change: (String) -> Unit,
    buttonWidth: Float,
    onButtonWidthChange: (Float) -> Unit,
    buttonHeight: Float,
    onButtonHeightChange: (Float) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    buttonColor: Color,
    onButtonColorChange: (Color) -> Unit,
    textColor: Color,
    onTextColorChange: (Color) -> Unit,
    broadcastIp: String,
    onBroadcastIpChange: (String) -> Unit
) {
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
                    onCheckedChange = onShowTimeChange
                )
            }

            if (!showTimePreference) {
                OutlinedTextField(
                    value = buttonText1,
                    onValueChange = onButtonText1Change,
                    label = { Text("Button 1 Text") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OutlinedTextField(
                    value = buttonText2,
                    onValueChange = onButtonText2Change,
                    label = { Text("Button 2 Text") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                OutlinedTextField(
                    value = buttonText3,
                    onValueChange = onButtonText3Change,
                    label = { Text("Button 3 Text") },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))

            // Size Settings
            Text("Size Settings",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            SizeSlider(
                label = "Button Width",
                value = buttonWidth,
                onValueChange = onButtonWidthChange,
                range = 100f..300f
            )
            SizeSlider(
                label = "Button Height",
                value = buttonHeight,
                onValueChange = onButtonHeightChange,
                range = 100f..200f
            )
            SizeSlider(
                label = "Font Size",
                value = fontSize,
                onValueChange = onFontSizeChange,
                range = 100f..400f
            )

            HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))

            // Color Settings
            Text("Color Settings",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            ColorPicker(
                title = "Button Color",
                selectedColor = buttonColor,
                onColorSelected = onButtonColorChange
            )
            ColorPicker(
                title = "Text Color",
                selectedColor = textColor,
                onColorSelected = onTextColorChange
            )

            HorizontalDivider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))

            // Network Settings
            Text("Network Settings",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            OutlinedTextField(
                value = broadcastIp,
                onValueChange = onBroadcastIpChange,
                label = { Text("Broadcast IP Address") },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ButtonGroup(
    position: Offset,
    buttonWidth: androidx.compose.ui.unit.Dp,
    buttonHeight: androidx.compose.ui.unit.Dp,
    spacing: androidx.compose.ui.unit.Dp,
    showTime: Boolean,
    buttonTexts: List<String>,
    fontSize: TextUnit,
    fontFamily: FontFamily,
    buttonColor: Color,
    textColor: Color,
    onButtonClick: (Int) -> Unit,
    onButtonLongClick: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.toInt(), position.y.toInt()) }
            .size(buttonWidth, buttonHeight * 3 + spacing * 2)
    ) {
        // Top button
        WolButton(
            text = buttonTexts[0],
            isTimeDisplay = showTime,
            onClick = { onButtonClick(0) },
            onLongClick = { onButtonLongClick(0) },
            modifier = Modifier
                .size(buttonWidth, buttonHeight)
                .align(Alignment.TopStart),
            fontSize = fontSize,
            fontFamily = fontFamily,
            buttonColor = buttonColor,
            textColor = textColor
        )

        // Middle button
        WolButton(
            text = buttonTexts[1],
            isTimeDisplay = showTime,
            onClick = { onButtonClick(1) },
            onLongClick = { onButtonLongClick(1) },
            modifier = Modifier
                .size(buttonWidth, buttonHeight)
                .align(Alignment.CenterStart),
            fontSize = fontSize,
            fontFamily = fontFamily,
            buttonColor = buttonColor,
            textColor = textColor
        )

        // Bottom button
        WolButton(
            text = buttonTexts[2],
            isTimeDisplay = showTime,
            onClick = { onButtonClick(2) },
            onLongClick = { onButtonLongClick(2) },
            modifier = Modifier
                .size(buttonWidth, buttonHeight)
                .align(Alignment.BottomStart),
            fontSize = fontSize,
            fontFamily = fontFamily,
            buttonColor = buttonColor,
            textColor = textColor
        )
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
                        .clickable { onColorSelected(color) }
                ) {
                    Surface(
                        color = color,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxSize(),
                        border = if (color == selectedColor) BorderStroke(2.dp, Color.White) else null
                    ) {}
                }
            }
        }
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
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            },
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
                                slideInVertically { height -> slideDirection * height } +
                                        fadeIn(animationSpec = tween(durationMillis = 150)) togetherWith
                                        slideOutVertically { height -> -slideDirection * height } +
                                        fadeOut(animationSpec = tween(durationMillis = 150))
                            },
                            contentAlignment = Alignment.Center,
                            label = "Digit Animation"
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

@Preview(showBackground = true)
@Composable
fun PreviewWolControlScreen() {
    DashboardWOLTheme {
        WolControlScreen(Executors.newSingleThreadExecutor())
    }
}