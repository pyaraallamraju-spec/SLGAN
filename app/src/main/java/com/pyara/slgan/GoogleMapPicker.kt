package com.pyara.slgan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.runtime.snapshotFlow
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.drop

@Composable
internal fun GoogleMapPicker(
    modifier: Modifier = Modifier,
    initialLatitude: Double,
    initialLongitude: Double,
    onLocationSelected: (Double, Double) -> Unit
) {
    val initialPosition = LatLng(initialLatitude, initialLongitude)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 16f)
    }
    val markerState = remember {
        MarkerState(position = initialPosition)
    }

    LaunchedEffect(initialLatitude, initialLongitude) {
        val updatedPosition = LatLng(initialLatitude, initialLongitude)
        markerState.position = updatedPosition
        cameraPositionState.position = CameraPosition.fromLatLngZoom(updatedPosition, 16f)
    }

    // MarkerState exposes its position as observable state. This also captures
    // the final position after a user drags the marker, without relying on an
    // onDragEnd parameter that this Maps Compose version does not provide.
    LaunchedEffect(markerState) {
        snapshotFlow { markerState.position }
            .drop(1)
            .collect { position ->
                onLocationSelected(position.latitude, position.longitude)
            }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            compassEnabled = true,
            mapToolbarEnabled = false
        ),
        onMapClick = { position ->
            markerState.position = position
        }
    ) {
        Marker(
            state = markerState,
            title = "Delivery location",
            snippet = "Drag the pin or tap the map to adjust",
            draggable = true
        )
    }
}
