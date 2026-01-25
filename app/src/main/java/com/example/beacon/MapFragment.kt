package com.example.beacon

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.beacon.databinding.FragmentMapBinding
import com.example.beacon.sos.SosViewModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
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
import java.util.* 

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var mapView: MapView
    private var mapLibreMap: MapLibreMap? = null
    private var tempMarker: Marker? = null

    private val activityViewModel: ActivityViewModel by activityViewModels()
    // --- THIS IS THE CRITICAL CHANGE ---
    // Get the SOS ViewModel that is shared with the Activity and other fragments.
    private val sosViewModel: SosViewModel by activityViewModels()

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val initialLatLng = LatLng(14.5995, 120.9842)
    private val initialZoom = 12.0

    private var locationEngine: LocationEngine? = null
    private val locationEngineCallback = object : LocationEngineCallback<LocationEngineResult> {
        override fun onSuccess(result: LocationEngineResult?) {
            result?.lastLocation?.let { location ->
                activityViewModel.updateUserLocation(location.latitude, location.longitude)
                sosViewModel.updateCurrentLocation(location)
                Log.d("MapFragment", "Location update SUCCESS: ${location.latitude}, ${location.longitude}")
            } ?: Log.d("MapFragment", "Location result was null.")
        }

        override fun onFailure(exception: Exception) {
            if(isAdded) {
                Log.e("MapFragment", "Location update FAILURE", exception)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted && isAdded) {
                mapLibreMap?.getStyle { style ->
                    enableUserLocation(style)
                }
            } else if (isAdded) {
                Snackbar.make(binding.root, "Location permission denied.", Snackbar.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        mapView = binding.mapView
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    override fun onMapReady(map: MapLibreMap) {
        mapLibreMap = map
        Log.d("MapFragment", "onMapReady called.")

        map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) { style ->
            Log.d("MapFragment", "Map style loaded.")

            map.cameraPosition = CameraPosition.Builder()
                .target(initialLatLng)
                .zoom(initialZoom)
                .build()

            enableUserLocation(style)
            loadSosMarkers(style)

            map.addOnMapClickListener { latLng ->
                handleMapTap(latLng)
                true
            }
        }
    }

    private fun setupClickListeners() {
        binding.sosButton.setOnClickListener {
            val location = mapLibreMap?.locationComponent?.lastKnownLocation
            if (location != null) {
                mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 15.0))
            } else if (isAdded) {
                Toast.makeText(requireContext(), "User location not available yet.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleMapTap(latLng: LatLng) {
        tempMarker?.let { mapLibreMap?.removeMarker(it) }
        tempMarker = mapLibreMap?.addMarker(MarkerOptions().position(latLng).title("Tapped Location"))
        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLng(latLng), 750)
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation(loadedMapStyle: Style) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("MapFragment", "Location permission is granted.")

            val locationComponent = mapLibreMap?.locationComponent
            val options = LocationComponentOptions.builder(requireContext()).build()
            val activationOptions = LocationComponentActivationOptions.builder(requireContext(), loadedMapStyle)
                .locationComponentOptions(options)
                .build()

            locationComponent?.activateLocationComponent(activationOptions)
            locationComponent?.isLocationComponentEnabled = true
            locationComponent?.cameraMode = CameraMode.TRACKING
            locationComponent?.renderMode = RenderMode.COMPASS

            locationEngine = locationComponent?.locationEngine
            val request = LocationEngineRequest.Builder(10000L) // 10 seconds
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .build()

            locationEngine?.requestLocationUpdates(request, locationEngineCallback, null)
            Log.d("MapFragment", "Location updates have been requested from the LocationEngine.")

        } else {
             Log.d("MapFragment", "Location permission not granted, requesting it.")
             requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun loadSosMarkers(style: Style) {
        if (!isAdded) return
        val sosIcon = IconFactory.getInstance(requireContext()).fromBitmap(bitmapFromVector(requireContext(), R.drawable.ic_location))

        db.collection("sos_sessions").whereEqualTo("active", true).get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener
                for (document in documents) {
                    val locations = document.get("locations") as? List<Map<String, Any>>
                    val lastLocation = locations?.lastOrNull()?.get("location") as? GeoPoint
                    val title = document.getString("userId") ?: "SOS"
                    if (lastLocation != null) {
                        mapLibreMap?.addMarker(
                            MarkerOptions()
                                .position(LatLng(lastLocation.latitude, lastLocation.longitude))
                                .title(title)
                                .icon(sosIcon)
                        )
                    }
                }
            }
            .addOnFailureListener { exception ->
                if (isAdded) {
                    Log.e("MapFragment", "Failed to load SOS markers", exception)
                }
            }
    }

    private fun bitmapFromVector(context: Context, vectorResId: Int): Bitmap {
        val vectorDrawable: Drawable = ContextCompat.getDrawable(context, vectorResId)!!
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap: Bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return bitmap
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    
    override fun onDestroyView() {
        locationEngine?.removeLocationUpdates(locationEngineCallback)
        mapView.onDestroy()
        mapLibreMap?.removeOnMapClickListener { true }
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}
