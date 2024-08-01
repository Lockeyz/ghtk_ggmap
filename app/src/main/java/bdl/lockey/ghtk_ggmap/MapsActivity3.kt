package bdl.lockey.ghtk_ggmap

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class MapsActivity3 :
    AppCompatActivity(),
    OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocationLatLng: LatLng? = null
    private val fixedLocation = LatLng(21.014007826514074, 105.78438394043823)
    private var polyline: Polyline? = null

    private val locationPermissionRequest =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    getCurrentLocation()
                }
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    getCurrentLocation()
                }
                else -> {
                    Log.e("MapsActivity", "No location access granted")
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        val mapFragment =
            supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestLocationPermissions()
    }

    private fun requestLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocationLatLng = LatLng(location.latitude, location.longitude)
                    currentLocationLatLng?.let {
                        mMap.addMarker(MarkerOptions().position(it).title("Current Location"))
                        mMap.addMarker(MarkerOptions().position(fixedLocation).title("Fixed Location"))
                        drawPolyline()
                        adjustCamera()
                    }
                } else {
                    Log.e("MapsActivity", "Location is null")
                }
            }.addOnFailureListener {
                Log.e("MapsActivity", "Failed to get location")
            }
    }

    private fun drawPolyline() {
        currentLocationLatLng?.let {
            polyline?.remove()
            polyline =
                mMap.addPolyline(
                    PolylineOptions()
                        .add(it, fixedLocation)
                        .color(android.graphics.Color.RED)
                        .width(5f),
                )

            mMap.setOnPolylineClickListener { _ ->
                val distance = FloatArray(1)
                Location.distanceBetween(
                    currentLocationLatLng?.latitude ?: 0.0,
                    currentLocationLatLng?.longitude ?: 0.0,
                    fixedLocation.latitude,
                    fixedLocation.longitude,
                    distance,
                )
                Toast.makeText(this, "Distance: ${distance[0] / 1000} km", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun adjustCamera() {
        val builder = LatLngBounds.Builder()
        currentLocationLatLng?.let { builder.include(it) }
        builder.include(fixedLocation)
        val bounds = builder.build()
        val padding = 100 // padding around the edges
        val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
        mMap.animateCamera(cameraUpdate)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = true // Enable default zoom controls
        }
    }
}
