package com.roland.moi.ui

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    private var stoppedAt = 0L

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
                    locked = false
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

    @Composable
    private fun Root() {
        Box(Modifier.fillMaxSize().background(Black).systemBarsPadding()) {
            when {
                !hasPin || changingPin -> SetupPinScreen {
                    hasPin = true; changingPin = false; locked = false
                }
                locked -> LockScreen(bioAvailable(), ::askBiometric) { locked = false }
                else -> MainTabs()
            }
        }
    }

    @Composable
    private fun MainTabs() {
        val d by Store.data.collectAsState()
        var tab by rememberSaveable { mutableIntStateOf(0) }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> HomeScreen(d, shieldOn)
                    1 -> ShieldSettingsScreen(d, shieldOn)
                    else -> SettingsScreen(onChangePin = { changingPin = true })
                }
            }
            NavigationBar(containerColor = Surface1) {
                val items = listOf(
                    Triple("Accueil", Icons.Outlined.Home, 0),
                    Triple("Bouclier", Icons.Outlined.Shield, 1),
                    Triple("Réglages", Icons.Outlined.Settings, 2),
                )
                items.forEach { (label, icon, i) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) },
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
