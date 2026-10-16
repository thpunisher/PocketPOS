package com.pocketpos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketpos.ui.theme.PosColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PosProductCard(
    name: String,
    category: String,
    priceCents: Long,
    stockQuantity: Int,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(
        Locale("es", "ES")
    )

    val price = currencyFormatter.format(priceCents / 100.0)

    val stockColor = when {
        stockQuantity == 0 -> PosColors.Danger
        stockQuantity <= 5 -> PosColors.Warning
        else -> PosColors.Success
    }

    val stockBackground = when {
        stockQuantity == 0 -> PosColors.DangerLight
        stockQuantity <= 5 -> PosColors.WarningLight
        else -> PosColors.SuccessLight
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = PosColors.Surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PosColors.TextPrimary
                    )

                    Text(
                        text = category,
                        fontSize = 13.sp,
                        color = PosColors.TextSecondary
                    )
                }

                Text(
                    text = price,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PosColors.Primary
                )
            }

            Row(
                modifier = Modifier
                    .background(
                        color = stockBackground,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(
                        horizontal = 10.dp,
                        vertical = 7.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        stockQuantity == 0 -> "Sin existencias"
                        stockQuantity <= 5 -> "Stock bajo: $stockQuantity"
                        else -> "Stock: $stockQuantity"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = stockColor
                )
            }
        }
    }
}