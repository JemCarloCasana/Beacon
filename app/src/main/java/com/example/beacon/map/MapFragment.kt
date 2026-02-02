package com.example.beacon.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.beacon.BuildConfig
import com.example.beacon.R
import com.example.beacon.databinding.FragmentMapBinding
import com.example.beacon.viewmodel.ActivityViewModel
import com.example.beacon.viewmodel.LocationStatusViewModel
import com.example.beacon.viewmodel.LocationViewModel
import com.example.beacon.viewmodel.SosViewModel
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.engine.LocationEngine
import org.maplibre.android.location.engine.LocationEngineCallback
import org.maplibre.android.location.engine.LocationEngineRequest
import org.maplibre.android.location.engine.LocationEngineResult
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillExtrusionLayer
import org.maplibre.android.style.layers.PropertyFactory

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var mapLibreMap: MapLibreMap? = null
    private var tempMarker: Marker? = null

    // State to track if we are in "Navigation Mode"
    private var isNavigationMode = false

    // For location updates
    private var locationEngine: LocationEngine? = null
    private val locationCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            val lastLocation = result?.lastLocation
            lastLocation?.let { location ->
                locationViewModel.onLocationUpdate(location)
            }
        }

        override fun onFailure(exception: Exception) {
            Log.e("MapFragment", "Location Engine Failure", exception)
            locationViewModel.onLocationError(exception.message ?: "Unknown error")
        }
    }

    private val db = Firebase.firestore

    // ViewModels
    private val activityViewModel: ActivityViewModel by activityViewModels()
    private val sosViewModel: SosViewModel by activityViewModels()
    private val locationStatusViewModel: LocationStatusViewModel by activityViewModels()
    private val locationViewModel: LocationViewModel by activityViewModels()

    private val initialLatLng = LatLng(14.5995, 120.9842)
    private val initialZoom = 12.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Wire up the Navigation/Recenter Button
        binding.btnRecenter.setOnClickListener {
            toggleNavigationMode()
            it.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        }

        // 2. Wire up the SOS Button
        binding.sosButton.setOnClickListener {
            Toast.makeText(requireContext(), "Hold SOS button to trigger", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map

        // 🔐 SECURE API KEY INJECTION
        // This reads the key from your gradle.properties via BuildConfig
        val apiKey = BuildConfig.MAPTILER_API_KEY

        // Use the "Streets" style (Google Maps lookalike)
        val mapStyleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$apiKey"

        map.setStyle(Style.Builder().fromUri(mapStyleUrl)) { style ->
            // Initial Camera Position
            map.cameraPosition = CameraPosition.Builder()
                .target(initialLatLng)
                .zoom(initialZoom)
                .build()

            enableUserLocation(style)

            // 🚀 3D Buildings are compatible with MapTiler Streets!
            enable3DBuildings(style)

            loadSosMarkers()

            // SOS Marker Click Listener -> Launch Google Maps Nav
            map.setOnMarkerClickListener { marker ->
                if (marker.title == "SOS") {
                    launchGoogleMapsNavigation(marker.position.latitude, marker.position.longitude)
                    true
                } else {
                    false
                }
            }

            // Normal Map Tap -> Drop Pin
            map.addOnMapClickListener { point: LatLng ->
                if (isNavigationMode) disableNavigationMode()
                handleMapTap(point)
                true
            }
        }
    }

    /**
     * 🚀 3D BUILDINGS LAYER
     * Adds height to buildings when zoomed in (Level 15+)
     */
    private fun enable3DBuildings(style: Style) {
        try {
            // MapTiler's standard building layer is often named 'building'
            val fillExtrusionLayer = FillExtrusionLayer("3d-buildings", "composite")
            fillExtrusionLayer.sourceLayer = "building"
            fillExtrusionLayer.minZoom = 15f

            fillExtrusionLayer.setProperties(
                PropertyFactory.fillExtrusionColor(Color.parseColor("#aaaaaa")),
                PropertyFactory.fillExtrusionHeight(
                    Expression.interpolate(
                        Expression.linear(),
                        Expression.zoom(),
                        Expression.stop(15, 0f),
                        Expression.stop(16, Expression.get("render_height")) // MapTiler uses 'render_height' often
                    )
                ),
                PropertyFactory.fillExtrusionBase(Expression.get("render_min_height")),
                PropertyFactory.fillExtrusionOpacity(0.9f)
            )

            // Attempt to place it below labels
            // If "airport-label" doesn't exist, we fall back to adding it on top
            if (style.getLayer("airport-label") != null) {
                style.addLayerBelow(fillExtrusionLayer, "airport-label")
            } else {
                style.addLayer(fillExtrusionLayer)
            }
        } catch (e: Exception) {
            Log.e("MapFragment", "Could not add 3D buildings (Layer might differ in MapTiler): ${e.message}")
        }
    }

    /**
     * 🚀 LAUNCH GOOGLE MAPS NAVIGATION
     */
    private fun launchGoogleMapsNavigation(destinationLat: Double, destinationLng: Double) {
        val gmmIntentUri = Uri.parse("google.navigation:q=$destinationLat,$destinationLng&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")

        try {
            startActivity(mapIntent)
        } catch (e: Exception) {
            val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destinationLat,$destinationLng")
            val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
            startActivity(browserIntent)
        }
    }

    private fun toggleNavigationMode() {
        if (isNavigationMode) {
            disableNavigationMode()
        } else {
            enableNavigationMode()
        }
    }

    private fun enableNavigationMode() {
        val locationComponent = mapLibreMap?.locationComponent ?: return
        if (!locationComponent.isLocationComponentEnabled) return

        // 1. Navigation View (Compass Tracking)
        locationComponent.cameraMode = CameraMode.TRACKING_COMPASS
        locationComponent.renderMode = RenderMode.COMPASS

        // 2. 3D Tilt + Zoom In
        val lastLocation = locationComponent.lastKnownLocation
        val target = if (lastLocation != null) LatLng(
            lastLocation.latitude,
            lastLocation.longitude
        ) else mapLibreMap?.cameraPosition?.target

        if (target != null) {
            val navPosition = CameraPosition.Builder()
                .target(target)
                .zoom(17.0)
                .tilt(60.0)
                .build()

            mapLibreMap?.animateCamera(CameraUpdateFactory.newCameraPosition(navPosition), 1500)
            isNavigationMode = true
            Toast.makeText(context, "Navigation Mode: ON", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disableNavigationMode() {
        val locationComponent = mapLibreMap?.locationComponent ?: return

        // 1. Reset Tracking
        locationComponent.cameraMode = CameraMode.TRACKING
        locationComponent.renderMode = RenderMode.NORMAL

        // 2. Reset Camera (Flat)
        val flatPosition = CameraPosition.Builder()
            .target(mapLibreMap?.cameraPosition?.target)
            .zoom(15.0)
            .tilt(0.0)
            .bearing(0.0)
            .build()

        mapLibreMap?.animateCamera(CameraUpdateFactory.newCameraPosition(flatPosition), 1000)
        isNavigationMode = false
    }

    private fun handleMapTap(latLng: LatLng) {
        tempMarker?.let { mapLibreMap?.removeMarker(it) }
        tempMarker = mapLibreMap?.addMarker(MarkerOptions().position(latLng).title("Pinned Location"))
        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng), 500)
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation(style: Style) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationViewModel.onPermissionDenied()
            Toast.makeText(requireContext(), "Location permission not granted", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val locationOptions = LocationComponentOptions.builder(requireContext())
                .accuracyAlpha(0.3f)
                .accuracyColor(0xFF4CAF50.toInt())
                .build()

            val activationOptions = LocationComponentActivationOptions.builder(requireContext(), style)
                .locationComponentOptions(locationOptions)
                .useDefaultLocationEngine(true)
                .build()

            val locationComponent = mapLibreMap?.locationComponent ?: return
            locationComponent.activateLocationComponent(activationOptions)
            locationComponent.isLocationComponentEnabled = true

            // Default: Simple tracking
            locationComponent.cameraMode = CameraMode.TRACKING
            locationComponent.renderMode = RenderMode.COMPASS

            locationEngine = locationComponent.locationEngine
            val request = LocationEngineRequest.Builder(5000L)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(10000L)
                .build()

            locationEngine?.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

            Log.d("MapFragment", "Location component enabled successfully")

        } catch (e: Exception) {
            locationViewModel.onLocationError("Failed to enable location: ${e.message}")
            Log.e("MapFragment", "Location error", e)
        }
    }

    private fun loadSosMarkers() {
        if (!isAdded) return

        val icon = IconFactory.getInstance(requireContext())
            .fromBitmap(bitmapFromVector(requireContext(), R.drawable.ic_location))

        db.collection("sos_sessions")
            .whereEqualTo("active", true)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val locations = doc.get("locations") as? List<Map<String, Any>>
                    val lastLocationData = locations?.lastOrNull()?.get("location")
                    if (lastLocationData is GeoPoint) {
                        mapLibreMap?.addMarker(
                            MarkerOptions()
                                .position(
                                    LatLng(
                                        lastLocationData.latitude,
                                        lastLocationData.longitude
                                    )
                                )
                                .title("SOS")
                                .icon(icon)
                        )
                    }
                }
            }
            .addOnFailureListener { Log.e("MapFragment", "Failed to load SOS markers", it) }
    }

    private fun bitmapFromVector(context: Context, vectorResId: Int): Bitmap {
        val drawable: Drawable = ContextCompat.getDrawable(context, vectorResId)!!
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() {
        super.onStop()
        mapView.onStop()
        locationEngine?.removeLocationUpdates(locationCallback)
    }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }

    override fun onDestroyView() {
        locationEngine?.removeLocationUpdates(locationCallback)
        mapView.onDestroy()
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}