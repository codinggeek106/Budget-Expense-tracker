package com.spendtrack.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.spendtrack.app.ui.nav.SpendTrackNavGraph
import com.spendtrack.app.ui.theme.SpendTrackTheme
import com.spendtrack.app.ui.theme.SystemBarsForTheme
import com.spendtrack.app.ui.theme.rememberAppearance

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val appearance = rememberAppearance()
            SystemBarsForTheme(appearance.darkTheme)
            SpendTrackTheme(darkTheme = appearance.darkTheme, dynamicColor = appearance.dynamicColor) {
                SpendTrackNavGraph()
            }
        }
    }
}
