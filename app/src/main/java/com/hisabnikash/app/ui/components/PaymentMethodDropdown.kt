package com.hisabnikash.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hisabnikash.app.R
import com.hisabnikash.app.core.model.PaymentMethod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodDropdown(
    selected: PaymentMethod,
    onSelected: (PaymentMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val methods = listOf(PaymentMethod.CASH, PaymentMethod.BANK, PaymentMethod.BKASH, PaymentMethod.NAGAD)
    val selectedLabel = selected.localizedLabel()
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            label = { Text(stringResource(R.string.payment_method)) },
            leadingIcon = { Text(selected.mark) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            methods.forEach { method ->
                val label = method.localizedLabel()
                DropdownMenuItem(
                    text = { Text("${method.mark}  $label") },
                    onClick = {
                        onSelected(method)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
fun PaymentMethod.localizedLabel(): String = stringResource(
    when (this) {
        PaymentMethod.CASH -> R.string.payment_cash
        PaymentMethod.BANK -> R.string.payment_bank
        PaymentMethod.BKASH -> R.string.payment_bkash
        PaymentMethod.NAGAD -> R.string.payment_nagad
        PaymentMethod.OTHER -> R.string.payment_other
    },
)

private val PaymentMethod.mark: String
    get() = when (this) {
        PaymentMethod.CASH -> "৳"
        PaymentMethod.BANK -> "B"
        PaymentMethod.BKASH -> "b"
        PaymentMethod.NAGAD -> "N"
        PaymentMethod.OTHER -> "•"
    }
