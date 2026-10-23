package com.pyara.slgan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Standalone address-entry page, intentionally not connected to app navigation yet.
 * It can be wired to the desired destination later without changing My addresses.
 */
@Composable
fun AddressEntryPage(
    onBack: () -> Unit = {},
    onSave: (Map<String, String>) -> Unit = {}
) {
    var recipientName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var houseNumber by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var tower by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    val cream = Color(0xFFFFF7E8)
    val ink = Color(0xFF26364D)

    Column(
        modifier = Modifier.fillMaxSize().background(cream).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ink) }
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Address details", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ink)
                Text("Enter delivery address details", fontSize = 13.sp, color = ink)
            }
        }
        OutlinedTextField(recipientName, { recipientName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Recipient name *") }, singleLine = true)
        OutlinedTextField(phone, { phone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Phone number *") }, singleLine = true)
        OutlinedTextField(houseNumber, { houseNumber = it }, modifier = Modifier.fillMaxWidth(), label = { Text("House number *") }, singleLine = true)
        OutlinedTextField(floor, { floor = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Floor") }, singleLine = true)
        OutlinedTextField(tower, { tower = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Tower / Block") }, singleLine = true)
        OutlinedTextField(street, { street = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Street / Area *") }, singleLine = true)
        OutlinedTextField(landmark, { landmark = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Landmark") }, singleLine = true)
        OutlinedTextField(city, { city = it }, modifier = Modifier.fillMaxWidth(), label = { Text("City *") }, singleLine = true)
        OutlinedTextField(state, { state = it }, modifier = Modifier.fillMaxWidth(), label = { Text("State *") }, singleLine = true)
        OutlinedTextField(pincode, { pincode = it }, modifier = Modifier.fillMaxWidth(), label = { Text("PIN code *") }, singleLine = true)
        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                onSave(mapOf(
                    "recipientName" to recipientName.trim(),
                    "phone" to phone.trim(),
                    "houseNumber" to houseNumber.trim(),
                    "floor" to floor.trim(),
                    "tower" to tower.trim(),
                    "street" to street.trim(),
                    "landmark" to landmark.trim(),
                    "city" to city.trim(),
                    "state" to state.trim(),
                    "pincode" to pincode.trim()
                ))
            },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E2E2), contentColor = Color(0xFF000000))
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            Text("Save address", fontWeight = FontWeight.Bold)
        }
    }
}
