package com.spendtrack.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.spendtrack.app.ui.nav.SpendTrackNavGraph
import com.spendtrack.app.ui.theme.SpendTrackTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpendTrackTheme {
                SpendTrackNavGraph()
            }
        }
    }
}
