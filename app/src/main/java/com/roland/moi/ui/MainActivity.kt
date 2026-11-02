package com.roland.moi.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.roland.moi.data.PinManager
import com.roland.moi.data.Store
import com.roland.moi.shield.ShieldService

class MainActivity : FragmentActivity() {

    private var locked by mutableStateOf(true)
    private var hasPin by mutableStateOf(false)
    private var changingPin by mutableStateOf(false)
    private var shieldOn by mutableStateOf(false)
    private var tab by mutableIntStateOf(0)
    private val nav = Nav()
    private var stoppedAt = 0L

    private val askNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Rien de MOI n'apparaît dans les captures ni dans les apps récentes
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        hasPin = PinManager.hasPin(this)
        setContent { MoiTheme { Root() } }
    }

    override fun onStart() {
        super.onStart()
        if (stoppedAt > 0 && SystemClock.elapsedRealtime() - stoppedAt > 30_000) locked = true
        shieldOn = ShieldService.isEnabled(this)
    }

    override fun onResume() {
        super.onResume()
        shieldOn = ShieldService.isEnabled(this)
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = SystemClock.elapsedRealtime()
    }

    private fun bioAvailable(): Boolean =
        PinManager.biometricEnabled(this) &&
            BiometricManager.from(this).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS

    private fun askBiometric() {
        val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked()
                }
            })
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("MOI")
            .setSubtitle("Déverrouille avec ton empreinte")
            .setNegativeButtonText("Utiliser mon code")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()
        prompt.authenticate(info)
    }

    private fun unlocked() {
        locked = false
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) askNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    @Composable
    private fun Root() {
        Box(Modifier.fillMaxSize().background(Black).systemBarsPadding()) {
            when {
                !hasPin || changingPin -> SetupPinScreen {
                    hasPin = true; changingPin = false; unlocked()
                }
                locked -> LockScreen(bioAvailable(), ::askBiometric) { unlocked() }
                else -> App()
            }
        }
    }

    @Composable
    private fun App() {
        val d by Store.data.collectAsState()
        BackHandler(enabled = nav.current != null || tab != 0) {
            if (!nav.back()) tab = 0
        }
        when (val r = nav.current) {
            null -> Tabs(d)
            Route.Liberte -> LiberteScreen(d, shieldOn, nav)
            Route.ShieldSettings -> ShieldSettingsScreen(d, shieldOn, nav)
            Route.Settings -> SettingsScreen(d, nav, onChangePin = { changingPin = true })
            Route.Protection -> ProtectionScreen(d, nav)
            Route.HabitsEdit -> HabitsEditScreen(d, nav)
            is Route.Section -> SectionScreen(d, r.key, nav)
            is Route.EntryView -> EntryViewScreen(d, r.id, nav)
            is Route.EntryEdit -> EntryEditScreen(d, r.section, r.id, nav)
            is Route.Journal -> JournalScreen(d, r.day, nav)
            Route.JournalList -> JournalListScreen(d, nav)
            is Route.Review -> ReviewScreen(d, r.week, nav)
            Route.Reviews -> ReviewsScreen(d, nav)
            is Route.Month -> MonthScreen(d, r.start, nav)
        }
    }

    @Composable
    private fun Tabs(d: com.roland.moi.data.AppData) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeScreen(d, shieldOn, nav, onTab = { tab = it })
                    1 -> MaVieScreen(d, nav)
                    2 -> HabitsScreen(d, nav)
                    else -> SportScreen(d, nav)
                }
            }
            NavigationBar(containerColor = Surface1) {
                val items = listOf(
                    Triple("Accueil", Icons.Outlined.Home, 0),
                    Triple("Ma vie", Icons.Outlined.AutoStories, 1),
                    Triple("Habitudes", Icons.Outlined.CheckCircle, 2),
                    Triple("Sport", Icons.Outlined.DirectionsRun, 3),
                )
                items.forEach { (label, icon, i) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Black, indicatorColor = Gold,
                            selectedTextColor = Gold, unselectedIconColor = TextDim, unselectedTextColor = TextDim,
                        ),
                    )
                }
            }
        }
    }
}
