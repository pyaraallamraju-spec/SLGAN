package com.pyara.slgan

import android.location.Geocoder
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
internal fun GoogleMapPicker(
    modifier: Modifier = Modifier,
    initialLatitude: Double,
    initialLongitude: Double,
    onLocationSelected: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialPosition = LatLng(initialLatitude, initialLongitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 16f)
    }
    val markerState = remember { MarkerState(position = initialPosition) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<android.location.Address>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var searching by remember { mutableStateOf(false) }

    fun searchAddress() {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            searchError = "Enter an address or place name."
            searchResults = emptyList()
            return
        }
        scope.launch {
            searching = true
            searchError = null
            val results = withContext(Dispatchers.IO) {
                runCatching {
                    if (!Geocoder.isPresent()) emptyList()
                    else {
                        @Suppress("DEPRECATION")
                        Geocoder(context, Locale.getDefault()).getFromLocationName(query, 6)
                            ?.toList().orEmpty()
                    }
                }.getOrDefault(emptyList())
            }
            searching = false
            searchResults = results
            if (results.isEmpty()) searchError = "No matching address found. Try a nearby landmark or a more specific address."
        }
    }

    fun selectAddress(address: android.location.Address) {
        val latitude = address.latitude
        val longitude = address.longitude
        val selected = LatLng(latitude, longitude)
        markerState.position = selected
        scope.launch {
            cameraPositionState.animate(CameraUpdateFactory.newCameraPosition(CameraPosition.fromLatLngZoom(selected, 17f)))
        }
        searchQuery = address.getAddressLine(0).orEmpty().ifBlank { searchQuery }
        searchResults = emptyList()
        searchError = null
        onLocationSelected(latitude, longitude)
    }

    LaunchedEffect(initialLatitude, initialLongitude) {
        val updatedPosition = LatLng(initialLatitude, initialLongitude)
        markerState.position = updatedPosition
        cameraPositionState.position = CameraPosition.fromLatLngZoom(updatedPosition, 16f)
    }

    // Observe marker changes from map taps and marker dragging.
    LaunchedEffect(markerState) {
        snapshotFlow { markerState.position }
            .drop(1)
            .collect { position ->
                onLocationSelected(position.latitude, position.longitude)
            }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                searchError = null
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            placeholder = { Text("Search address or place") },
            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { searchAddress() }, enabled = !searching) {
                    if (searching) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = "Search address")
                    }
                }
            }
        )
        if (searchError != null) {
            Text(searchError.orEmpty(), modifier = Modifier.padding(horizontal = 4.dp))
        }
        if (searchResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                LazyColumn(modifier = Modifier.height(132.dp)) {
                    items(searchResults) { address ->
                        val label = address.getAddressLine(0).orEmpty().ifBlank {
                            listOfNotNull(address.locality, address.adminArea, address.countryName)
                                .joinToString(", ")
                        }
                        androidx.compose.material3.TextButton(
                            onClick = { selectAddress(address) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
        GoogleMap(
            modifier = Modifier.fillMaxWidth().weight(1f),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = true,
                compassEnabled = true,
                mapToolbarEnabled = false
            ),
            onMapClick = { position -> markerState.position = position }
        ) {
            Marker(
                state = markerState,
                title = "Delivery location",
                draggable = true
            )
        }
    }
}
