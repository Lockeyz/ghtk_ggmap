package bdl.lockey.ghtk_ggmap

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import bdl.lockey.ghtk_ggmap.databinding.ActivityMapsBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var polyline: Polyline? = null
    private var currentLocationLatLng: LatLng? = null
    private val fixedLocation = LatLng(21.014007826514074, 105.78438394043823)

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }



    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, ACCESS_FINE_LOCATION
            )
        ) {
            Snackbar.make(
                binding.root, R.string.location_access_required, Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.ok, View.OnClickListener { // Request the permission
                ActivityCompat.requestPermissions(
                    this, arrayOf(ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
                )
            }).show()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Snackbar.make(
                    binding.root, R.string.location_permission_granted, Snackbar.LENGTH_SHORT
                ).show()
                getCurrentLocation()
            } else {
                Snackbar.make(
                    binding.root, R.string.location_permission_denied, Snackbar.LENGTH_SHORT
                ).show()
            }
        } else {
            // Ignore all other requests.
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLocationLatLng = LatLng(location.latitude, location.longitude)
                currentLocationLatLng?.let {
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

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkLocationPermission()

        mMap.isMyLocationEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        getDistance()
    }

    @SuppressLint("MissingPermission")
    private fun getDistance() {
        mMap.setOnPolylineClickListener {
            val distance = FloatArray(1)
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocationLatLng = LatLng(location.latitude, location.longitude)
                    currentLocationLatLng?.let {
                        Location.distanceBetween(
                            currentLocationLatLng?.latitude ?: 0.0,
                            currentLocationLatLng?.longitude ?: 0.0,
                            fixedLocation.latitude,
                            fixedLocation.longitude,
                            distance,
                        )
                        Toast.makeText(this, "Distance: ${distance[0] / 1000} km", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e("MapsActivity", "Location is null")
                }
            }

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
                ).apply {
                    // Đảm bảo polyline có thể nhấn được
                    isClickable = true
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

}