package com.example.dashboardwol

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dashboardwol.ui.theme.DashboardWOLTheme
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import android.content.pm.ActivityInfo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.animation.SizeTransform
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width

@Suppress("DEPRECATION")
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

val customFontFamily = FontFamily(
    Font(R.font.lcddot_tr, FontWeight.Normal)
)

@Composable
fun WolControlScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("WolPrefs", Context.MODE_PRIVATE) }

    var mac1 by remember { mutableStateOf(prefs.getString("mac1", "34:5A:60:13:39:CD") ?: "34:5A:60:13:39:CD") }
    var mac2 by remember { mutableStateOf(prefs.getString("mac2", "60:CF:84:63:0E:58") ?: "60:CF:84:63:0E:58") }
    var mac3 by remember { mutableStateOf(prefs.getString("mac3", "12:34:56:78:90:AB") ?: "12:34:56:78:90:AB") }

    var showDialog by remember { mutableStateOf(false) }
    var editingMac by remember { mutableStateOf("") }
    var currentMacType by remember { mutableStateOf<Int?>(null) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

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

    val buttonWidthDp = 300.dp
    val buttonHeightDp = 150.dp
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
                val address = java.net.InetAddress.getByName("192.168.50.255")
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
                                1 -> {
                                    mac1 = editingMac
                                    prefs.edit().putString("mac1", editingMac).apply()
                                }
                                2 -> {
                                    mac2 = editingMac
                                    prefs.edit().putString("mac2", editingMac).apply()
                                }
                                3 -> {
                                    mac3 = editingMac
                                    prefs.edit().putString("mac3", editingMac).apply()
                                }
                            }
                            showDialog = false
                        }
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                Button(
                    onClick = { showDialog = false }
                ) { Text("Cancel") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = groupPosition.x.toInt(),
                        y = groupPosition.y.toInt()
                    )
                }
                .size(buttonWidthDp, buttonHeightDp * 3 + spacingDp * 2)
        ) {
            WolButton(
                timePart = timeParts[0],
                onClick = { sendWolPacket(mac1) },
                onLongClick = {
                    editingMac = mac1
                    currentMacType = 1
                    showDialog = true
                },
                modifier = Modifier
                    .size(buttonWidthDp, buttonHeightDp)
                    .align(Alignment.TopStart)
            )

            WolButton(
                timePart = timeParts[1],
                onClick = { sendWolPacket(mac2) },
                onLongClick = {
                    editingMac = mac2
                    currentMacType = 2
                    showDialog = true
                },
                modifier = Modifier
                    .size(buttonWidthDp, buttonHeightDp)
                    .align(Alignment.CenterStart)
            )

            WolButton(
                timePart = timeParts[2],
                onClick = { sendWolPacket(mac3) },
                onLongClick = {
                    editingMac = mac3
                    currentMacType = 3
                    showDialog = true
                },
                modifier = Modifier
                    .size(buttonWidthDp, buttonHeightDp)
                    .align(Alignment.BottomStart)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
@Composable
fun WolButton(
    timePart: String,  // 接收完整时间部分（如"12"）
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 将时间部分拆分为单个字符（如"59"变成['5', '9']）
            val digits = remember(timePart) {
                if (timePart.length == 2) timePart.toCharArray().toList()
                else listOf('0', timePart[0])  // 处理单数字情况
            }

            // 水平排列数字
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                                fontFamily = customFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 200.sp,
                                letterSpacing = (-0.1).sp
                            ),
                            color = Color.Black.copy(alpha = 1f),
                            modifier = Modifier
                                .offset(y=15.dp),
                        )
                    }

                    // 在数字之间添加间距（可选）
                    if (index == 0) Spacer(modifier = Modifier.width(4.dp))
                }
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
        WolControlScreen()
    }
}
