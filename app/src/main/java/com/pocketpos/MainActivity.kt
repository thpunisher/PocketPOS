package com.pocketpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.pocketpos.ui.inventory.InventoryScreen
import com.pocketpos.ui.theme.PocketPOSTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PocketPOSTheme {
                InventoryScreen()
            }
        }
    }
}