package com.pocketpos.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketpos.ui.components.PosButton
import com.pocketpos.ui.components.PosProductCard
import com.pocketpos.ui.theme.PosColors

@Composable
fun InventoryScreen(viewModel: InventoryViewModel = viewModel()) {
    var searchQuery by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    val products by viewModel.products.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val lowStockCount by viewModel.lowStockCount.collectAsState()
    val outOfStockCount by viewModel.outOfStockCount.collectAsState()

    LaunchedEffect(searchQuery) { viewModel.onQueryChange(searchQuery) }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = PosColors.Background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PosColors.Background)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "PocketPOS",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = PosColors.Primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Inventario",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = PosColors.TextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Gestiona tus productos y existencias",
                fontSize = 14.sp,
                color = PosColors.TextSecondary
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = {
                    Text("Buscar productos...")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PosColors.Primary,
                    unfocusedBorderColor = PosColors.Border,
                    focusedContainerColor = PosColors.Surface,
                    unfocusedContainerColor = PosColors.Surface
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    title = "Productos",
                    value = totalCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Stock bajo",
                    value = lowStockCount.toString(),
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "Agotados",
                    value = outOfStockCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Todos los productos",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PosColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${products.size} resultados",
                    fontSize = 12.sp,
                    color = PosColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = products,
                    key = { it.product.id }
                ) { item ->
                    PosProductCard(
                        name = item.product.name,
                        category = item.category?.name ?: "Sin categoría",
                        priceCents = item.product.priceCents,
                        stockQuantity = item.product.stockQuantity
                    )
                }

                if (products.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se han encontrado productos",
                                color = PosColors.TextSecondary
                            )
                        }
                    }
                }
            }

            PosButton(
                text = "+  Nuevo producto",
                onClick = viewModel::addTestProduct,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = PosColors.Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = PosColors.TextSecondary
            )

            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = PosColors.TextPrimary
            )
        }
    }
}