package hu.drosztok.android

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast // EZ A SOR A JAVÍTÁS
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MapFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private lateinit var db: FirebaseFirestore
    private var driverListener: ListenerRegistration? = null
    private val driverMarkers = mutableMapOf<String, Marker>()

    // Változók a követéshez
    private var trackedDriverId: String? = null
    private var cameraIsFollowing = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        db = FirebaseFirestore.getInstance()
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        setupSearch(view)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val budapest = LatLng(47.4979, 19.0402)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(budapest, 11f))
        startListeningForDrivers()
    }

    private fun setupSearch(view: View) {
        val searchEditText: EditText = view.findViewById(R.id.map_search_edittext)
        val clearButton: ImageButton = view.findViewById(R.id.clear_search_button)

        searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val usernameToSearch = v.text.toString()
                if (usernameToSearch.isNotBlank()) {
                    searchAndTrackDriver(usernameToSearch)
                }
                hideKeyboard(v)
                return@setOnEditorActionListener true
            }
            false
        }

        searchEditText.addTextChangedListener { text ->
            clearButton.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        }

        clearButton.setOnClickListener {
            searchEditText.text.clear()
            stopTracking()
        }
    }

    private fun searchAndTrackDriver(username: String) {
        val profilesRef = db.collection("profiles")
        val query: Query = profilesRef.whereEqualTo("username", username)
        query.get().addOnSuccessListener { documents ->
            if (documents.isEmpty) {
                Toast.makeText(context, "A(z) $username hívójelű sofőr nem található.", Toast.LENGTH_SHORT).show()
                stopTracking()
            } else {
                val driverDoc = documents.documents[0]
                trackedDriverId = driverDoc.id
                cameraIsFollowing = true
                Toast.makeText(context, "Követés indítva: $username", Toast.LENGTH_SHORT).show()
                focusOnTrackedDriver()
            }
        }
    }

    private fun stopTracking() {
        trackedDriverId = null
        cameraIsFollowing = false
        Toast.makeText(context, "Követés leállítva.", Toast.LENGTH_SHORT).show()
    }

    private fun focusOnTrackedDriver() {
        trackedDriverId?.let { driverId ->
            driverMarkers[driverId]?.let { marker ->
                googleMap?.animateCamera(CameraUpdateFactory.newLatLng(marker.position))
                marker.showInfoWindow()
            }
        }
    }

    private fun startListeningForDrivers() {
        driverListener = db.collection("driver_locations").addSnapshotListener { snapshots, e ->
            if (e != null || snapshots == null) {
                Log.w("MapFragment", "Listen failed.", e)
                return@addSnapshotListener
            }

            for (docChange in snapshots.documentChanges) {
                val driverId = docChange.document.id
                val locationData = docChange.document.data
                val lat = locationData["lat"] as? Double
                val lng = locationData["lng"] as? Double

                if (lat != null && lng != null) {
                    val position = LatLng(lat, lng)
                    db.collection("profiles").document(driverId).get().addOnSuccessListener { profileDoc ->
                        val username = profileDoc?.getString("username") ?: driverId
                        updateMarker(driverId, position, username)
                    }
                }
            }
        }
    }

    private fun updateMarker(driverId: String, position: LatLng, title: String) {
        val map = googleMap ?: return
        val existingMarker = driverMarkers[driverId]
        if (existingMarker != null) {
            existingMarker.position = position
            existingMarker.title = title
        } else {
            val markerOptions = MarkerOptions().position(position).title(title)
            map.addMarker(markerOptions)?.let {
                driverMarkers[driverId] = it
            }
        }

        if (driverId == trackedDriverId && cameraIsFollowing) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLng(position))
        }
    }

    private fun hideKeyboard(view: View) {
        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onStop() {
        super.onStop()
        driverListener?.remove()
    }
}