package com.pocketpos.ui.inventory

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pocketpos.ui.components.PosButton
import com.pocketpos.ui.theme.PosColors

@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    viewModel: AddProductViewModel = viewModel()
) {
    val form = viewModel.form
    val errors = viewModel.errors
    val categories by viewModel.categories.collectAsState()
    val taxRates by viewModel.taxRates.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showNewCategory by remember { mutableStateOf(false) }

    val goBack = {
        viewModel.reset()
        onBack()
    }
    BackHandler(onBack = goBack)

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        containerColor = PosColors.Background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                Column {
                    TextButton(onClick = goBack) {
                        Text(
                            text = "←  Inventario",
                            color = PosColors.Primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Nuevo producto",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = PosColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Rellena los datos del artículo",
                        fontSize = 14.sp,
                        color = PosColors.TextSecondary
                    )
                }

                // ---------- Basic info ----------
                FormSection(title = "Información básica") {
                    PosTextField(
                        label = "Nombre *",
                        value = form.name,
                        onValueChange = { viewModel.onFormChange(form.copy(name = it)) },
                        error = errors.name,
                        capitalization = KeyboardCapitalization.Words
                    )

                    val categoryText = categories.firstOrNull { it.id == form.categoryId }?.name ?: "Sin categoría"
                    PosDropdown(
                        label = "Categoría",
                        selectedText = categoryText,
                        options = listOf("Sin categoría") + categories.map { it.name },
                        onSelect = { index ->
                            val id = if (index == 0) null else categories[index - 1].id
                            viewModel.onFormChange(form.copy(categoryId = id))
                        },
                        footerLabel = "+  Nueva categoría",
                        onFooterClick = { showNewCategory = true }
                    )
                }

                // ---------- Price ----------
                FormSection(title = "Precio") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PosTextField(
                            label = "Precio de venta *",
                            value = form.price,
                            onValueChange = { viewModel.onFormChange(form.copy(price = it)) },
                            error = errors.price,
                            keyboardType = KeyboardType.Decimal,
                            filter = ::isDecimalInput,
                            suffix = "€",
                            modifier = Modifier.weight(1f)
                        )
                        PosTextField(
                            label = "Coste",
                            value = form.cost,
                            onValueChange = { viewModel.onFormChange(form.copy(cost = it)) },
                            error = errors.cost,
                            keyboardType = KeyboardType.Decimal,
                            filter = ::isDecimalInput,
                            suffix = "€",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val taxText = taxRates.firstOrNull { it.id == form.taxRateId }?.name ?: "Por defecto"
                    PosDropdown(
                        label = "IVA",
                        selectedText = taxText,
                        options = listOf("Por defecto") + taxRates.map { it.name },
                        onSelect = { index ->
                            val id = if (index == 0) null else taxRates[index - 1].id
                            viewModel.onFormChange(form.copy(taxRateId = id))
                        }
                    )
                    Text(
                        text = "El precio de venta ya incluye IVA.",
                        fontSize = 12.sp,
                        color = PosColors.TextSecondary
                    )
                }

                // ---------- Stock ----------
                FormSection(title = "Existencias") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Controlar stock",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = PosColors.TextPrimary
                            )
                            Text(
                                text = "Desactívalo para servicios",
                                fontSize = 12.sp,
                                color = PosColors.TextSecondary
                            )
                        }
                        Switch(
                            checked = form.trackStock,
                            onCheckedChange = { viewModel.onFormChange(form.copy(trackStock = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PosColors.Primary
                            )
                        )
                    }

                    if (form.trackStock) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            PosTextField(
                                label = "Stock inicial",
                                value = form.stock,
                                onValueChange = { viewModel.onFormChange(form.copy(stock = it)) },
                                error = errors.stock,
                                keyboardType = KeyboardType.Number,
                                filter = ::isDigitsInput,
                                modifier = Modifier.weight(1f)
                            )
                            PosTextField(
                                label = "Aviso stock bajo",
                                value = form.lowStock,
                                onValueChange = { viewModel.onFormChange(form.copy(lowStock = it)) },
                                error = errors.lowStock,
                                keyboardType = KeyboardType.Number,
                                filter = ::isDigitsInput,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ---------- Codes ----------
                FormSection(title = "Códigos (opcional)") {
                    PosTextField(
                        label = "Código de barras",
                        value = form.barcode,
                        onValueChange = { viewModel.onFormChange(form.copy(barcode = it)) },
                        keyboardType = KeyboardType.Number,
                        filter = ::isDigitsInput
                    )
                    PosTextField(
                        label = "Referencia (SKU)",
                        value = form.sku,
                        onValueChange = { viewModel.onFormChange(form.copy(sku = it)) },
                        capitalization = KeyboardCapitalization.Characters
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                PosButton(
                    text = if (viewModel.saving) "Guardando..." else "Guardar producto",
                    onClick = { viewModel.save(onSaved = onBack) },
                    enabled = !viewModel.saving
                )
            }
        }
    }

    if (showNewCategory) {
        NewCategoryDialog(
            onDismiss = { showNewCategory = false },
            onConfirm = { name ->
                viewModel.createCategory(name)
                showNewCategory = false
            }
        )
    }
}

// =====================================================================
//  Small building blocks
// =====================================================================

private fun isDecimalInput(text: String): Boolean =
    text.length <= 10 && text.all { it.isDigit() || it == ',' || it == '.' }

private fun isDigitsInput(text: String): Boolean =
    text.length <= 13 && text.all { it.isDigit() }

@Composable
private fun FormSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PosColors.Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = PosColors.TextPrimary
            )
            content()
        }
    }
}

@Composable
private fun posFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PosColors.TextPrimary,
    unfocusedTextColor = PosColors.TextPrimary,
    errorTextColor = PosColors.TextPrimary,
    focusedBorderColor = PosColors.Primary,
    unfocusedBorderColor = PosColors.Border,
    errorBorderColor = PosColors.Danger,
    focusedContainerColor = PosColors.Surface,
    unfocusedContainerColor = PosColors.Surface,
    errorContainerColor = PosColors.Surface,
    focusedLabelColor = PosColors.Primary,
    unfocusedLabelColor = PosColors.TextSecondary,
    errorLabelColor = PosColors.Danger,
    errorSupportingTextColor = PosColors.Danger,
    cursorColor = PosColors.Primary
)

@Composable
private fun PosTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    suffix: String? = null,
    filter: (String) -> Boolean = { true }
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (filter(it)) onValueChange(it) },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        suffix = suffix?.let { s -> { Text(s, color = PosColors.TextSecondary) } },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization
        ),
        shape = RoundedCornerShape(16.dp),
        colors = posFieldColors()
    )
}

/** Read-only field that opens a menu. Built from basic parts so it does not depend on experimental APIs. */
@Composable
private fun PosDropdown(
    label: String,
    selectedText: String,
    options: List<String>,
    onSelect: (Int) -> Unit,
    footerLabel: String? = null,
    onFooterClick: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { Text("▾", color = PosColors.TextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = posFieldColors()
        )

        // Transparent layer that captures the tap over the whole field.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { expanded = true }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(PosColors.Surface)
        ) {
            options.forEachIndexed { index, text ->
                DropdownMenuItem(
                    text = { Text(text, color = PosColors.TextPrimary) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    }
                )
            }
            if (footerLabel != null) {
                DropdownMenuItem(
                    text = {
                        Text(footerLabel, color = PosColors.Primary, fontWeight = FontWeight.SemiBold)
                    },
                    onClick = {
                        expanded = false
                        onFooterClick()
                    }
                )
            }
        }
    }
}

@Composable
private fun NewCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = PosColors.Surface,
        title = {
            Text("Nueva categoría", color = PosColors.TextPrimary, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Nombre") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(16.dp),
                colors = posFieldColors()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text("Crear", color = PosColors.Primary, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = PosColors.TextSecondary)
            }
        }
    )
}
