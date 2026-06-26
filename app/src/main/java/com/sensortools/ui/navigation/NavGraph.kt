package com.sensortools.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.sensortools.data.local.PreferencesManager
import com.sensortools.data.model.SensorInfo
import com.sensortools.data.repository.SensorRepository
import com.sensortools.ui.about.AboutScreen
import com.sensortools.ui.calibration.CalibrationScreen
import com.sensortools.ui.components.BubbleLevel
import com.sensortools.ui.components.CompassView
import com.sensortools.ui.detail.DetailScreen
import com.sensortools.ui.health.HealthScreen
import com.sensortools.ui.home.HomeScreen
import com.sensortools.ui.intro.IntroScreen
import com.sensortools.ui.record.RecordScreen
import com.sensortools.ui.settings.SettingsScreen
import com.sensortools.ui.theme.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Screen("home", "首页", Icons.Filled.Home)
    data object Calibration : Screen("calibration", "校准", Icons.Filled.Tune)
    data object Health : Screen("health", "健康", Icons.Filled.HealthAndSafety)
    data object Record : Screen("record", "数据", Icons.Filled.SaveAlt)
    data object Tools : Screen("tools", "工具", Icons.Filled.Build)

    companion object {
        const val RECORD_ROUTE = "record?sensorType={sensorType}"

        fun recordRoute(sensorType: Int = -1): String = "record?sensorType=$sensorType"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }

    var showIntro by remember { mutableStateOf(prefs.isFirstLaunch()) }

    // Intro overlay
    if (showIntro) {
        SensorToolsTheme {
            IntroScreen(onDone = {
                prefs.setFirstLaunchDone()
                showIntro = false
            })
        }
        return
    }

    SensorToolsTheme {
        Scaffold(
            containerColor = Background,
            bottomBar = {
                NavigationBar(
                    containerColor = CardBackground,
                    contentColor = TextPrimary,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    listOf(Screen.Home, Screen.Calibration, Screen.Health, Screen.Record, Screen.Tools).forEach { screen ->
                        val selected = currentRoute?.startsWith(screen.route) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                val targetRoute = if (screen == Screen.Record) {
                                    Screen.recordRoute()
                                } else {
                                    screen.route
                                }
                                if (currentRoute?.startsWith(screen.route) != true) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(Screen.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(screen.icon, contentDescription = screen.label, tint = if (selected) TextPrimary else TextTertiary)
                            },
                            label = {
                                Text(screen.label, color = if (selected) TextPrimary else TextTertiary)
                            },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = CardBackgroundAlt)
                        )
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screen.Home.route) {
                    val repo = remember { SensorRepository(context) }
                    HomeScreen(
                        onSensorClick = { sensor -> navController.navigate("detail/${sensor.type}") },
                        onSettingsClick = { navController.navigate("settings") }
                    )
                }

                composable("detail/{sensorType}", arguments = listOf(navArgument("sensorType") { type = NavType.IntType })) { entry ->
                    val sensorType = entry.arguments?.getInt("sensorType") ?: Sensor.TYPE_ACCELEROMETER
                    val repo = remember { SensorRepository(context) }
                    val sensorInfo = remember(sensorType) { repo.getAllSensors().find { it.type == sensorType } }
                    if (sensorInfo != null) {
                        DetailScreen(
                            sensorInfo = sensorInfo,
                            onBack = { navController.popBackStack() },
                            onRecord = {
                                navController.navigate(Screen.recordRoute(sensorInfo.type)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }

                composable(Screen.Calibration.route) { CalibrationScreen() }
                composable(Screen.Health.route) { HealthScreen() }
                composable(
                    route = Screen.RECORD_ROUTE,
                    arguments = listOf(
                        navArgument("sensorType") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { entry ->
                    val sensorType = entry.arguments?.getInt("sensorType") ?: -1
                    RecordScreen(
                        preselectedSensorType = sensorType.takeIf { it >= 0 }
                    )
                }
                composable(Screen.Tools.route) { ToolsScreen() }

                composable("settings") {
                    SettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable("about") {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun ToolsScreen() {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager }

    var accelX by remember { mutableFloatStateOf(0f) }
    var accelY by remember { mutableFloatStateOf(0f) }
    var accelZ by remember { mutableFloatStateOf(0f) }
    var azimuth by remember { mutableFloatStateOf(0f) }
    var magAccuracy by remember { mutableIntStateOf(0) }
    var rotationMatrix = remember { FloatArray(9) }
    var orientation = remember { FloatArray(3) }
    var gravity = remember { floatArrayOf(0f, 0f, 0f) }
    var geomagnetic = remember { floatArrayOf(0f, 0f, 0f) }

    DisposableEffect(sensorManager) {
        val accelListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val g = event.values.clone()
                    gravity = g; accelX = g[0]; accelY = g[1]; accelZ = g[2]
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                        SensorManager.getOrientation(rotationMatrix, orientation)
                        azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                        if (azimuth < 0) azimuth += 360f
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val magListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    geomagnetic = event.values.clone(); magAccuracy = event.accuracy
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(accelListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(magListener, sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(accelListener); sensorManager.unregisterListener(magListener) }
    }

    val levelX = Math.atan2(accelX.toDouble(), kotlin.math.sqrt((accelY * accelY + accelZ * accelZ).toDouble())).toFloat()
    val levelY = Math.atan2(accelY.toDouble(), kotlin.math.sqrt((accelX * accelX + accelZ * accelZ).toDouble())).toFloat()

    Column(
        modifier = Modifier.fillMaxSize().background(Background).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("实用工具", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBackground), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { BubbleLevel(x = levelX, y = levelY) }
        }
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = CardBackground), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) { CompassView(azimuth = azimuth, accuracy = magAccuracy) }
        }
        Spacer(Modifier.height(16.dp))
    }
}
