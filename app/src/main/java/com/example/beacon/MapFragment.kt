package com.example.beacon.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.beacon.databinding.FragmentMapBinding
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var mapLibreMap: MapLibreMap? = null

    private val initialLatLng = LatLng(14.5995, 120.9842)
    private val initialZoom = 12.0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return binding.root
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map

        // Load a valid style
        map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) { style ->
            // Set initial camera
            map.cameraPosition = CameraPosition.Builder()
                .target(initialLatLng)
                .zoom(initialZoom)
                .build()

            // Enable user location safely
            enableUserLocation(style)
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation(loadedMapStyle: Style) {
        // Check permission first
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        val locationComponent = mapLibreMap?.locationComponent
        val options = LocationComponentOptions.builder(requireContext()).build()
        val activationOptions = LocationComponentActivationOptions
            .builder(requireContext(), loadedMapStyle)
            .locationComponentOptions(options)
            .build()

        locationComponent?.activateLocationComponent(activationOptions)
        locationComponent?.isLocationComponentEnabled = true
        locationComponent?.cameraMode = CameraMode.TRACKING
        locationComponent?.renderMode = RenderMode.COMPASS
    }

    // MapView lifecycle
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onDestroyView() { mapView.onDestroy(); _binding = null; super.onDestroyView() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}
