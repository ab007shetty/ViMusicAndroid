package com.abshetty.vimusic.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abshetty.vimusic.core.designsystem.vimusic.LocalAppearance
import com.abshetty.vimusic.core.designsystem.vimusic.semiBold
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.activity.compose.BackHandler

@Composable
fun SetupScreen(
    onFinish: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel(),
) {
    val (colorPalette, typography) = LocalAppearance.current
    val context = LocalContext.current

    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var notificationsAsked by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refresh++ }

    val account by viewModel.account.collectAsStateWithLifecycle()
    val signingIn by viewModel.busy.collectAsStateWithLifecycle()
    val signInError by viewModel.error.collectAsStateWithLifecycle()

    val allSteps = remember(refresh, account, signingIn, signInError) {
        buildList {
            add(
                SetupStep(
                    icon = Icons.Rounded.Notifications,
                    title = "Notifications",

                    detail = "Puts the player on your lock screen and in the shade. " +
                        "Music plays without it, but you get no controls outside the app.",
                    done = hasNotificationPermission(context),
                    actionLabel = if (notificationsAsked) "Open app settings" else "Allow",
                    action = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (notificationsAsked) {
                                context.openAppSettings()
                            } else {
                                notificationsAsked = true
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                )
            )

            add(
                SetupStep(
                    icon = Icons.Rounded.Layers,
                    title = "Keep playing in the background",
                    detail = "YouTube's player only keeps running if it has a window " +
                        "of its own. It is invisible and draws nothing. Without this, " +
                        "music stops when you leave the app.",
                    done = Settings.canDrawOverlays(context),
                    actionLabel = "Allow",
                    action = {
                        context.startSafely(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + context.packageName),
                            )
                        )
                    },
                )
            )

            add(
                SetupStep(
                    icon = Icons.Rounded.BatterySaver,
                    title = "Ignore battery optimisation",
                    detail = "Stops the system killing playback once the screen goes " +
                        "off. Android will ask; answer Allow.",
                    done = isBatteryExempt(context),

                    actionLabel = "Battery settings",
                    keepsAction = true,
                    action = { context.requestBatteryExemption() },

                    extraLabel = "Open phone settings for ViMusic",
                    extraDetail = "Samsung and Xiaomi add a restriction Android cannot " +
                        "see. Battery -> allow background activity, and check ViMusic is " +
                        "not a sleeping app.",
                    extraAction = { context.openAppSettings() },
                )
            )

            add(
                SetupStep(
                    icon = Icons.Rounded.SwapVert,
                    title = "Skip tracks with the volume keys",
                    detail = "Hold a volume key on the lock screen to change track. " +
                        "Android only allows this through an accessibility service. " +
                        "Tap below, then Installed apps, then ViMusic.",
                    done = isVolumeServiceEnabled(context),
                    actionLabel = "Open accessibility settings",
                    action = { context.openVolumeServiceSettings() },

                    extraLabel = "Open app info",
                    extraDetail = "If Android says \"Restricted setting\", open app info, " +
                        "tap the three dots at the top right, and choose Allow restricted " +
                        "settings. Then come back and switch ViMusic on.",
                    extraAction = { context.openAppSettings() },
                )
            )

            add(
                SetupStep(
                    icon = Icons.Rounded.AccountCircle,
                    title = "Sign in with Google",
                    detail = "Signed in, your favourites and playlists follow you to the " +
                        "website and every device. A guest can search, play and hear " +
                        "their own files, but nothing is saved.",
                    done = account != null,
                    doneLabel = account?.email,
                    actionLabel = "Sign in",
                    busy = signingIn,
                    error = signInError,
                    action = { viewModel.signIn(context) },
                )
            )
        }
    }

    val shown = remember { allSteps.filter { !it.done }.map { it.title } }
    val steps = allSteps.filter { it.title in shown }

    LaunchedEffect(steps.isEmpty()) {
        if (steps.isEmpty()) onFinish()
    }
    if (steps.isEmpty()) return

    var index by remember { mutableStateOf(0) }
    val step = steps[index.coerceIn(steps.indices)]
    val isLast = index == steps.lastIndex

    fun goTo(next: Int) {
        index = next.coerceIn(steps.indices)
    }

    BackHandler(enabled = index > 0) { goTo(index - 1) }

    val doneOnArrival = remember(index) { step.done }
    LaunchedEffect(index, step.done) {
        if (step.done && !doneOnArrival && !isLast) {
            delay(700)
            goTo(index + 1)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colorPalette.background0)

            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text("Set up ViMusic", style = typography.xxl.semiBold, color = colorPalette.text)
        Text(
            "Step " + (index + 1) + " of " + steps.size,
            style = typography.xxs,
            color = colorPalette.textSecondary,
            modifier = Modifier.padding(top = 6.dp),
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            steps.indices.forEach { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (i == index) colorPalette.accent else colorPalette.textDisabled
                        )
                )
            }
        }

        AnimatedContent(
            targetState = index,
            transitionSpec = {
                val forward = targetState > initialState
                val enter = slideInHorizontally { width ->
                    if (forward) width / 3 else -width / 3
                } + fadeIn()
                val exit = slideOutHorizontally { width ->
                    if (forward) -width / 3 else width / 3
                } + fadeOut()
                (enter togetherWith exit).using(SizeTransform(clip = false))
            },
            modifier = Modifier
                .weight(1f)

                .pointerInput(steps.size) {
                    var travelled = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { travelled = 0f },
                        onDragEnd = {
                            val threshold = size.width * SWIPE_FRACTION
                            when {
                                travelled > threshold -> goTo(index - 1)
                                travelled < -threshold -> goTo(index + 1)
                            }
                        },
                    ) { change, amount ->
                        change.consume()
                        travelled += amount
                    }
                },
            label = "setupStep",
        ) { shown ->
            StepBody(steps[shown.coerceIn(steps.indices)])
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val secondary = when {
                !isLast -> if (step.done) "Next" else "Skip"
                account == null -> "Guest mode"
                else -> null
            }

            secondary?.let { label ->
                Text(
                    label,
                    style = typography.xs.semiBold,
                    color = colorPalette.textSecondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(colorPalette.background2)
                        .clickable { if (isLast) onFinish() else goTo(index + 1) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                )
            }

            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colorPalette.accent)
                    .clickable {
                        when {
                            isLast && step.done -> onFinish()
                            step.done && !step.keepsAction -> goTo(index + 1)

                            else -> step.action()
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when {
                        isLast && step.done -> "Start listening"
                        step.done && !step.keepsAction -> "Next"
                        else -> step.actionLabel
                    },
                    style = typography.xs.semiBold,
                    color = colorPalette.onAccent,
                )
            }
        }
    }
}

@Composable
private fun StepBody(step: SetupStep) {
    val (colorPalette, typography) = LocalAppearance.current

    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 32.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colorPalette.background1)
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (step.done) colorPalette.accent else colorPalette.background2),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    step.icon,
                    contentDescription = null,
                    tint = if (step.done) colorPalette.onAccent else colorPalette.textSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }

            Text(
                step.title,
                style = typography.l.semiBold,
                color = colorPalette.text,
                modifier = Modifier.weight(1f),
            )

            if (step.done) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Already granted",
                    tint = colorPalette.accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Text(
            step.detail,
            style = typography.xs,
            color = colorPalette.textSecondary,
            modifier = Modifier.padding(top = 20.dp),
        )

        if (step.busy) {
            Row(
                Modifier.padding(top = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = colorPalette.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("Waiting for Google", style = typography.xxs, color = colorPalette.textSecondary)
            }
        }

        step.error?.takeIf { !step.done }?.let { message ->
            Text(
                message,
                style = typography.xxs,
                color = colorPalette.red,
                modifier = Modifier.padding(top = 16.dp),
            )
        }

        step.doneLabel?.takeIf { step.done }?.let {
            Text(
                it,
                style = typography.xs.semiBold,
                color = colorPalette.accent,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        if (step.extraLabel != null && step.extraAction != null) {
            step.extraDetail?.let {
                Text(
                    it,
                    style = typography.xxs,
                    color = colorPalette.textSecondary,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            Box(
                Modifier
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorPalette.background2)
                    .clickable(onClick = step.extraAction)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    step.extraLabel,
                    style = typography.xxs.semiBold,
                    color = colorPalette.text,
                )
            }
        }
    }
}

private const val SWIPE_FRACTION = 0.18f

private data class SetupStep(
    val icon: ImageVector,

    val title: String,
    val detail: String,
    val done: Boolean,
    val actionLabel: String,
    val action: () -> Unit,

    val keepsAction: Boolean = false,

    val extraLabel: String? = null,
    val extraDetail: String? = null,
    val extraAction: (() -> Unit)? = null,

    val doneLabel: String? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun isBatteryExempt(context: Context): Boolean = runCatching {
    context.getSystemService(PowerManager::class.java)
        .isIgnoringBatteryOptimizations(context.packageName)
}.getOrDefault(false)

private fun Context.requestBatteryExemption() {
    val asked = runCatching {
        startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:" + packageName),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.isSuccess

    if (!asked) startSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

private fun Context.openVolumeServiceSettings() {
    startSafely(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

private fun isVolumeServiceEnabled(context: Context): Boolean = runCatching {
    Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ).orEmpty().split(':').any { it.contains("VolumeKeyAccessibilityService") }
}.getOrDefault(false)

private fun Context.openAppSettings() {
    startSafely(
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
    )
}

private fun Context.startSafely(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
