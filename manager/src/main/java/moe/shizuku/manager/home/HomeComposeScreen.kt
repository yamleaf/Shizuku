package moe.shizuku.manager.home

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.authorization.AuthorizationManager
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.settings.SettingsTabContent
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.ui.theme.ShizukuComposeTheme
import moe.shizuku.manager.utils.AppIconCache
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.SettingsHelper
import moe.shizuku.manager.utils.UserHandleCompat
import rikka.html.text.HtmlCompat
import androidx.core.net.toUri

private const val TAB_LAUNCH = 0
private const val TAB_RUNNING = 1
private const val TAB_SETTINGS = 2

// Expected Shizuku version bundled in this app (api/manifest.gradle.kts).
private const val SHIZUKU_API_VERSION = 13
private const val SHIZUKU_PATCH_VERSION = 6

private const val ADB_WIFI_ENABLED_KEY = "adb_wifi_enabled"
private const val ADB_KEY_PREF = "adbkey"

@Composable
fun HomeComposeScreen(
    status: ServiceStatus?,
    grantedCount: Int?,
    apps: List<PackageInfo> = emptyList(),
    onNavigateBack: () -> Unit,
    onRecreateRequested: () -> Unit,
    onStopService: () -> Unit,
    onManageApps: () -> Unit,
    onOpenTerminal: () -> Unit,
    onStartRoot: () -> Unit,
    onRestartRoot: () -> Unit,
    onOpenWirelessGuide: () -> Unit,
    onPairWireless: () -> Unit,
    onStartWirelessAdb: () -> Unit,
    onCopyAdbCommand: () -> Unit,
    onSendAdbCommand: () -> Unit,
    onOpenAdbPermissionHelp: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenLearnMore: () -> Unit
) {
    ShizukuComposeTheme {
        HomeScreenContent(
            status = status,
            grantedCount = grantedCount,
            apps = apps,
            onNavigateBack = onNavigateBack,
            onRecreateRequested = onRecreateRequested,
            onStopService = onStopService,
            onManageApps = onManageApps,
            onOpenTerminal = onOpenTerminal,
            onStartRoot = onStartRoot,
            onRestartRoot = onRestartRoot,
            onOpenWirelessGuide = onOpenWirelessGuide,
            onPairWireless = onPairWireless,
            onStartWirelessAdb = onStartWirelessAdb,
            onCopyAdbCommand = onCopyAdbCommand,
            onSendAdbCommand = onSendAdbCommand,
            onOpenAdbPermissionHelp = onOpenAdbPermissionHelp,
            onOpenBatteryOptimization = onOpenBatteryOptimization,
            onOpenLearnMore = onOpenLearnMore
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    status: ServiceStatus?,
    grantedCount: Int?,
    apps: List<PackageInfo> = emptyList(),
    onNavigateBack: () -> Unit,
    onRecreateRequested: () -> Unit,
    onStopService: () -> Unit,
    onManageApps: () -> Unit,
    onOpenTerminal: () -> Unit,
    onStartRoot: () -> Unit,
    onRestartRoot: () -> Unit,
    onOpenWirelessGuide: () -> Unit,
    onPairWireless: () -> Unit,
    onStartWirelessAdb: () -> Unit,
    onCopyAdbCommand: () -> Unit,
    onSendAdbCommand: () -> Unit,
    onOpenAdbPermissionHelp: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenLearnMore: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_RUNNING) }
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }
    val resolvedStatus = status ?: ServiceStatus()
    val running = resolvedStatus.isRunning
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }

    val launchItems = buildList {
        addAll(
            buildLaunchItems(
                context = context,
                status = resolvedStatus,
                onStartRoot = onStartRoot,
                onRestartRoot = onRestartRoot,
                onShowAdbCommand = { dialog = HomeDialog.AdbCommand },
                onOpenWirelessGuide = onOpenWirelessGuide,
                onPairWireless = onPairWireless,
                onStartWirelessAdb = onStartWirelessAdb,
                onOpenTerminal = onOpenTerminal,
                onOpenLearnMore = onOpenLearnMore
            )
        )
        add(
            HomeUiItem.Action(
                title = context.getString(R.string.action_about),
                summary = versionName,
                icon = Icons.Outlined.Info,
                enabled = true,
                onClick = { dialog = HomeDialog.About }
            )
        )
    }

    val authorizedPackages = remember(apps, running) {
        if (!running) emptyList()
        else apps.filter { pkg ->
            val ai = pkg.applicationInfo
            ai != null && AuthorizationManager.granted(pkg.packageName, ai.uid)
        }
    }

    val runningActions = buildRunningActions(
        context = context,
        status = resolvedStatus,
        wirelessAutostartEnabled = ShizukuSettings.getPreferences()
            .getBoolean(ShizukuSettings.KEEP_START_ON_BOOT_WIRELESS, false),
        onOpenAdbPermissionHelp = onOpenAdbPermissionHelp,
        onOpenBatteryOptimization = onOpenBatteryOptimization,
        onStopService = { dialog = HomeDialog.Stop }
    )

    val statusSummary = if (running) {
        val launchUser = if (resolvedStatus.uid == 0) "root" else "adb"
        val serverVersion = resolvedStatus.versionName
        val updateAvailable = resolvedStatus.apiVersion < SHIZUKU_API_VERSION ||
            (resolvedStatus.apiVersion == SHIZUKU_API_VERSION &&
                resolvedStatus.patchVersion in 0 until SHIZUKU_PATCH_VERSION)
        if (updateAvailable) {
            plainText(
                context.getString(
                    R.string.home_status_service_version_update,
                    launchUser,
                    serverVersion,
                    "$SHIZUKU_API_VERSION.$SHIZUKU_PATCH_VERSION"
                )
            )
        } else {
            context.getString(R.string.home_status_service_version, launchUser, serverVersion)
        }
    } else null

    val statusItem = HomeUiItem.Status(
        title = if (running) {
            context.getString(R.string.home_status_service_is_running, context.getString(R.string.app_name))
        } else {
            context.getString(R.string.home_status_service_not_running, context.getString(R.string.app_name))
        },
        summary = statusSummary,
        running = running
    )

    BackHandler {
        if (selectedTab != TAB_RUNNING) {
            selectedTab = TAB_RUNNING
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
        bottomBar = {
            FloatingNavigationBar(
                selectedTab = selectedTab,
                onSelect = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        val contentPadding = PaddingValues(
            start = 20.dp,
            top = innerPadding.calculateTopPadding() + 12.dp,
            end = 20.dp,
            bottom = innerPadding.calculateBottomPadding() + 20.dp
        )
        when (selectedTab) {
            TAB_LAUNCH -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { EnvironmentStatusCard() }
                items(launchItems) { item -> ActionCard(item) }
            }

            TAB_RUNNING -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { StatusCard(statusItem) }
                item {
                    if (running) {
                        RunningDetailsCard(status = resolvedStatus, grantedCount = grantedCount)
                    } else {
                        NotRunningHint()
                    }
                }
                item {
                    AutostartStatusCard(
                        enabledOnBootRoot = ShizukuSettings.getPreferences()
                            .getBoolean(ShizukuSettings.KEEP_START_ON_BOOT, false),
                        enabledOnBootWireless = ShizukuSettings.getPreferences()
                            .getBoolean(ShizukuSettings.KEEP_START_ON_BOOT_WIRELESS, false),
                        watchdogEnabled = ShizukuSettings.getPreferences()
                            .getBoolean(ShizukuSettings.WATCHDOG_ENABLED_ADB, false)
                    )
                }
                if (running && resolvedStatus.permission) {
                    item { AuthorizedAppsPreviewCard(packages = authorizedPackages, onManageApps = onManageApps) }
                }
                items(runningActions) { item -> ActionCard(item) }
            }

            TAB_SETTINGS -> SettingsTabContent(
                onRecreateRequested = onRecreateRequested,
                contentPadding = contentPadding
            )
        }
    }

    when (val state = dialog) {
        HomeDialog.About -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                confirmButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                title = { Text(stringResource(R.string.action_about)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(text = versionName)
                        LinkRow(
                            label = plainText(context.getString(R.string.about_view_source_code, "GitHub")),
                            url = "https://github.com/HSSkyBoy/Shizuku"
                        )
                        LinkRow(
                            label = plainText(context.getString(R.string.about_follow_channel, "t.me/np_nbcn")),
                            url = "https://t.me/np_nbcn"
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        HomeDialog.Stop -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                confirmButton = {
                    TextButton(onClick = {
                        dialog = null
                        onStopService()
                    }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
                title = { Text(stringResource(R.string.action_stop)) },
                text = { Text(stringResource(R.string.dialog_stop_message)) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        HomeDialog.AdbCommand -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                confirmButton = {
                    TextButton(onClick = {
                        dialog = null
                        onCopyAdbCommand()
                    }) {
                        Text(stringResource(R.string.home_adb_dialog_view_command_copy_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        dialog = null
                        onSendAdbCommand()
                    }) {
                        Text(stringResource(R.string.home_adb_dialog_view_command_button_send))
                    }
                },
                title = { Text(stringResource(R.string.home_adb_button_view_command)) },
                text = {
                    Text(
                        text = Starter.adbCommand,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        null -> Unit
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FloatingNavigationBar(
    selectedTab: Int,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 40.dp, vertical = 12.dp)
    ) {
        NavigationBar(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp), clip = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            NavigationBarItem(
                selected = selectedTab == TAB_LAUNCH,
                onClick = { onSelect(TAB_LAUNCH) },
                icon = { Icon(Icons.Outlined.RocketLaunch, contentDescription = null) },
                label = { Text(stringResource(R.string.home_tab_launch)) }
            )
            NavigationBarItem(
                selected = selectedTab == TAB_RUNNING,
                onClick = { onSelect(TAB_RUNNING) },
                icon = { Icon(Icons.Outlined.PlayCircleOutline, contentDescription = null) },
                label = { Text(stringResource(R.string.home_tab_running)) }
            )
            NavigationBarItem(
                selected = selectedTab == TAB_SETTINGS,
                onClick = { onSelect(TAB_SETTINGS) },
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                label = { Text(stringResource(R.string.settings_title)) }
            )
        }
    }
}

@Composable
private fun RunningDetailsCard(status: ServiceStatus, grantedCount: Int?) {
    val context = LocalContext.current
    val rows = remember(status) {
        buildList {
            add(context.getString(R.string.running_details_launch_mode) to launchModeLabel(context, status))
            add(context.getString(R.string.running_details_uid) to status.uid.toString())
            if (status.apiVersion >= 0) {
                add(context.getString(R.string.running_details_api_version) to status.apiVersion.toString())
            }
            if (status.patchVersion >= 0) {
                add(context.getString(R.string.running_details_patch_version) to status.patchVersion.toString())
            }
            status.seContext?.let {
                add(context.getString(R.string.running_details_selinux_context) to it)
            }
            add(
                context.getString(R.string.running_details_permission) to
                    context.getString(
                        if (status.permission) R.string.running_details_permission_granted
                        else R.string.running_details_permission_not_granted
                    )
            )
            grantedCount?.let {
                add(context.getString(R.string.running_details_authorized_count) to it.toString())
            }
        }
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = context.getString(R.string.running_details_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = value,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun NotRunningHint() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Text(
            text = stringResource(R.string.running_status_not_running_hint),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        )
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    val context = LocalContext.current
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = url,
        modifier = Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        },
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.bodySmall
    )
}

private enum class HomeDialog {
    About,
    Stop,
    AdbCommand
}

private fun launchModeLabel(context: Context, status: ServiceStatus): String {
    val default = context.getString(
        if (status.uid == 0) R.string.service_user_root else R.string.service_user_adb
    )
    return when (ShizukuSettings.getLastLaunchMode()) {
        ShizukuSettings.LaunchMethod.ROOT -> context.getString(R.string.service_user_root)
        ShizukuSettings.LaunchMethod.ADB -> context.getString(R.string.service_user_adb)
        else -> default
    }
}

private fun buildLaunchItems(
    context: Context,
    status: ServiceStatus,
    onStartRoot: () -> Unit,
    onRestartRoot: () -> Unit,
    onShowAdbCommand: () -> Unit,
    onOpenWirelessGuide: () -> Unit,
    onPairWireless: () -> Unit,
    onStartWirelessAdb: () -> Unit,
    onOpenTerminal: () -> Unit,
    onOpenLearnMore: () -> Unit
): List<HomeUiItem.Action> {
    val running = status.isRunning
    val items = mutableListOf<HomeUiItem.Action>()

    if (UserHandleCompat.myUserId() == 0) {
        val root = EnvironmentUtils.isRooted()
        val rootRestart = running && status.uid == 0
        if (root) {
            items += rootItem(context, running, rootRestart, onStartRoot, onRestartRoot)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || EnvironmentUtils.getAdbTcpPort() > 0) {
            items += HomeUiItem.Action(
                title = context.getString(R.string.home_wireless_adb_title),
                summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    plainText(context.getString(R.string.home_wireless_adb_description))
                } else {
                    plainText(context.getString(R.string.home_wireless_adb_description_pre_11))
                },
                icon = Icons.Outlined.Wifi,
                enabled = true,
                primaryActionLabel = context.getString(R.string.home_root_button_start),
                secondaryActionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.getString(R.string.adb_pairing) else null,
                tertiaryActionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.getString(R.string.home_wireless_adb_view_guide_button) else null,
                onPrimaryAction = onStartWirelessAdb,
                onSecondaryAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) onPairWireless else null,
                onTertiaryAction = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) onOpenWirelessGuide else null
            )
        }

        items += HomeUiItem.Action(
            title = context.getString(R.string.home_adb_title),
            summary = plainText(context.getString(R.string.home_adb_description, Helps.ADB.get())),
            icon = Icons.Outlined.Computer,
            enabled = true,
            primaryActionLabel = context.getString(R.string.home_adb_button_view_command),
            onPrimaryAction = onShowAdbCommand
        )

        if (!root) {
            items += rootItem(context, running, rootRestart, onStartRoot, onRestartRoot)
        }
    }

    items += HomeUiItem.Action(
        title = context.getString(R.string.home_learn_more_title),
        summary = context.getString(R.string.home_learn_more_description),
        icon = Icons.Outlined.Info,
        enabled = true,
        onClick = onOpenLearnMore
    )
    items += HomeUiItem.Action(
        title = context.getString(R.string.home_terminal_title),
        summary = context.getString(R.string.home_terminal_description),
        icon = Icons.Outlined.Terminal,
        enabled = true,
        onClick = onOpenTerminal
    )
    return items
}

private fun buildRunningActions(
    context: Context,
    status: ServiceStatus,
    wirelessAutostartEnabled: Boolean,
    onOpenAdbPermissionHelp: () -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onStopService: () -> Unit
): List<HomeUiItem.Action> {
    val running = status.isRunning
    val items = mutableListOf<HomeUiItem.Action>()

    if (running && !status.permission) {
        items += HomeUiItem.Action(
            title = context.getString(R.string.home_adb_is_limited_title),
            summary = context.getString(R.string.home_adb_is_limited_description),
            icon = Icons.Outlined.Warning,
            enabled = true,
            tonal = false,
            primaryActionLabel = context.getString(R.string.home_adb_button_view_help),
            onPrimaryAction = onOpenAdbPermissionHelp
        )
    }

    if (running && wirelessAutostartEnabled && !SettingsHelper.isIgnoringBatteryOptimizations(context)) {
        items += HomeUiItem.Action(
            title = context.getString(R.string.settings_battery_optimization),
            summary = context.getString(R.string.running_battery_optimization_warning),
            icon = Icons.Outlined.Warning,
            enabled = true,
            tonal = false,
            primaryActionLabel = context.getString(R.string.settings_title),
            onPrimaryAction = onOpenBatteryOptimization
        )
    }

    if (running) {
        items += HomeUiItem.Action(
            title = context.getString(R.string.action_stop),
            summary = context.getString(R.string.dialog_stop_message),
            icon = Icons.Outlined.Stop,
            enabled = true,
            tonal = false,
            onClick = onStopService
        )
    }
    return items
}

private fun rootItem(
    context: Context,
    running: Boolean,
    rootRestart: Boolean,
    onStartRoot: () -> Unit,
    onRestartRoot: () -> Unit
) = HomeUiItem.Action(
    title = context.getString(R.string.home_root_title),
    summary = plainText(
        buildString {
            append(context.getString(R.string.home_root_description, "Don't kill my app!"))
            if (running) {
                append("<p>")
                append(
                    context.getString(
                        R.string.home_root_description_sui,
                        "Sui",
                        "Sui"
                    )
                )
            }
        }
    ),
    icon = Icons.Outlined.PlayArrow,
    enabled = true,
    primaryActionLabel = if (rootRestart) context.getString(R.string.home_root_button_restart) else context.getString(R.string.home_root_button_start),
    onPrimaryAction = if (rootRestart) onRestartRoot else onStartRoot
)

@Composable
private fun InfoCard(title: String, rows: List<Pair<String, String>>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(10.dp))
            rows.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(text = value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun EnvironmentStatusCard() {
    val context = LocalContext.current
    val prefs = ShizukuSettings.getPreferences()
    val wirelessDebugging = Settings.Global.getInt(context.contentResolver, ADB_WIFI_ENABLED_KEY, 0) > 0
    val paired = prefs.contains(ADB_KEY_PREF)
    val systemPort = EnvironmentUtils.getAdbTcpPort()
    val configuredPort = prefs.getString(ShizukuSettings.TCPIP_PORT, "")?.trim().orEmpty()
    val portText = buildString {
        if (configuredPort.isNotEmpty()) append(configuredPort)
        if (systemPort > 0) {
            if (isNotEmpty()) append(" / ")
            append(systemPort)
        }
    }.ifEmpty { "-" }
    val on = context.getString(R.string.running_autostart_enabled)
    val off = context.getString(R.string.running_autostart_disabled)

    InfoCard(
        title = context.getString(R.string.env_status_title),
        rows = listOf(
            context.getString(R.string.adb_pairing) to
                if (paired) context.getString(R.string.env_status_paired) else context.getString(R.string.env_status_not_paired),
            context.getString(R.string.env_status_wireless_debugging) to if (wirelessDebugging) on else off,
            context.getString(R.string.env_status_tcp_adb) to if (systemPort > 0) on else off,
            context.getString(R.string.env_status_port) to portText
        )
    )
}

@Composable
private fun AutostartStatusCard(
    enabledOnBootRoot: Boolean,
    enabledOnBootWireless: Boolean,
    watchdogEnabled: Boolean
) {
    val context = LocalContext.current
    val on = context.getString(R.string.running_autostart_enabled)
    val off = context.getString(R.string.running_autostart_disabled)

    InfoCard(
        title = context.getString(R.string.running_autostart_title),
        rows = listOf(
            context.getString(R.string.settings_start_on_boot) to if (enabledOnBootRoot) on else off,
            context.getString(R.string.settings_start_on_boot_wireless) to if (enabledOnBootWireless) on else off,
            context.getString(R.string.settings_watchdog_adb) to if (watchdogEnabled) on else off
        )
    )
}

@Composable
private fun AuthorizedAppsPreviewCard(packages: List<PackageInfo>, onManageApps: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onManageApps() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = context.resources.getQuantityString(
                    R.plurals.home_app_management_authorized_apps_count,
                    packages.size,
                    packages.size
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = context.getString(R.string.home_app_management_view_authorized_apps),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (packages.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                packages.take(6).forEach { pkg ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(applicationInfo = pkg.applicationInfo, size = 34.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = remember(pkg) {
                                runCatching {
                                    context.packageManager.getApplicationLabel(pkg.applicationInfo!!).toString()
                                }.getOrDefault(pkg.packageName)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppIcon(applicationInfo: ApplicationInfo?, size: Dp) {
    if (applicationInfo == null) return
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val px = (size.value * density).toInt()
    var bitmap by remember(applicationInfo) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(applicationInfo) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching {
                AppIconCache.getOrLoadBitmap(context, applicationInfo, UserHandleCompat.myUserId(), px)
            }.getOrNull()
        }
    }
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                )
        )
    }
}

@Composable
private fun StatusCard(item: HomeUiItem.Status) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = (if (item.running) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.errorContainer
            }).copy(alpha = 0.7f),
            contentColor = if (item.running) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.running) Icons.Outlined.Link else Icons.Outlined.Warning,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (item.running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(item.title, style = MaterialTheme.typography.titleLarge)
                item.summary?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun ActionCard(item: HomeUiItem.Action) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.onClick != null && item.enabled) { item.onClick?.invoke() },
        colors = CardDefaults.cardColors(
            containerColor = (if (item.tonal) {
                MaterialTheme.colorScheme.surfaceContainerLow
            } else {
                MaterialTheme.colorScheme.errorContainer
            }).copy(alpha = 0.7f),
            contentColor = if (item.tonal) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onErrorContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (item.tonal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(item.summary, style = MaterialTheme.typography.bodyMedium)
            if (item.primaryActionLabel != null || item.secondaryActionLabel != null || item.tertiaryActionLabel != null) {
                Spacer(modifier = Modifier.height(18.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item.tertiaryActionLabel?.let { label ->
                        OutlinedButton(onClick = { item.onTertiaryAction?.invoke() }) {
                            Text(label)
                        }
                    }
                    item.secondaryActionLabel?.let { label ->
                        OutlinedButton(onClick = { item.onSecondaryAction?.invoke() }) {
                            Text(label)
                        }
                    }
                    item.primaryActionLabel?.let { label ->
                        Button(onClick = { item.onPrimaryAction?.invoke() }) {
                            Text(label)
                        }
                    }
                }
            } else if (item.onClick != null && item.enabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_open),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = stringResource(R.string.action_open),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Immutable
private sealed interface HomeUiItem {
    data class Status(
        val title: String,
        val summary: String?,
        val running: Boolean
    ) : HomeUiItem

    data class Action(
        val title: String,
        val summary: String,
        val icon: ImageVector,
        val enabled: Boolean,
        val tonal: Boolean = true,
        val onClick: (() -> Unit)? = null,
        val primaryActionLabel: String? = null,
        val secondaryActionLabel: String? = null,
        val tertiaryActionLabel: String? = null,
        val onPrimaryAction: (() -> Unit)? = null,
        val onSecondaryAction: (() -> Unit)? = null,
        val onTertiaryAction: (() -> Unit)? = null
    ) : HomeUiItem
}

private fun plainText(value: String): String {
    return HtmlCompat.fromHtml(value).toString().replace(Regex("\\s+"), " ").trim()
}