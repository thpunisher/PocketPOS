package com.pocketpos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.pocketpos.ui.inventory.AddProductScreen
import com.pocketpos.ui.inventory.InventoryScreen
import com.pocketpos.ui.theme.PocketPOSTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PocketPOSTheme {
                // Two screens only, so a Boolean is enough. When you add more, move to Navigation Compose.
                var showAddProduct by rememberSaveable { mutableStateOf(false) }

                if (showAddProduct) {
                    AddProductScreen(onBack = { showAddProduct = false })
                } else {
                    InventoryScreen(onAddProduct = { showAddProduct = true })
                }
            }
        }
    }
}
