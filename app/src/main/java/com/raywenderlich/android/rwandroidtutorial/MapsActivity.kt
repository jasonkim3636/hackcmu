/*
 * Copyright (c) 2021 Razeware LLC
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 * 
 * This project and source code may use libraries or frameworks that are
 * released under various Open-Source licenses. Use of those libraries and
 * frameworks are governed by their own individual licenses.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.runtracking


import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.R.attr.data
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.raywenderlich.android.runtracking.databinding.ActivityMainBinding
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.annotations.AfterPermissionGranted


/**
 * Main Screen
 */
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
  private lateinit var binding: ActivityMainBinding
  lateinit var fusedLocationProviderClient: com.google.android.gms.location.FusedLocationProviderClient

  // Location & Map
  private lateinit var mMap: GoogleMap

  companion object {
    // SharedPreferences
    private const val KEY_SHARED_PREFERENCE = "com.rwRunTrackingApp.sharedPreferences"
    private const val KEY_IS_TRACKING = "com.rwRunTrackingApp.isTracking"

    // Permission
    private const val REQUEST_CODE_FINE_LOCATION = 1
    private const val REQUEST_CODE_ACTIVITY_RECOGNITION = 2
  }

  private var isTracking: Boolean
    get() = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).getBoolean(KEY_IS_TRACKING, false)
    set(value) = this.getSharedPreferences(KEY_SHARED_PREFERENCE, Context.MODE_PRIVATE).edit().putBoolean(KEY_IS_TRACKING, value).apply()

  override fun onCreate(savedInstanceState: Bundle?) {
    // Switch to AppTheme for displaying the activity
    fusedLocationProviderClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this)

    setTheme(R.style.AppTheme)
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
    val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
    mapFragment.getMapAsync(this)


    // Set up button click events
    binding.startButton.setOnClickListener {
      // Clear the PolylineOptions from Google Map
      mMap.clear()

      // Update Start & End Button
      isTracking = true
      updateButtonStatus()

      // Reset the display text
      updateAllDisplayText(0, 0f)
      startTracking()
    }
    binding.endButton.setOnClickListener { endButtonClicked() }

    if (isTracking) {
      startTracking()
    }
  }



  // UI related codes
  private fun updateButtonStatus() {
    binding.startButton.isEnabled = !isTracking
    binding.endButton.isEnabled = isTracking
  }

  private fun updateAllDisplayText(stepCount: Int, totalDistanceTravelled: Float) {
    binding.totalDistanceTextView.text = String.format("Total distance: %.2fm", totalDistanceTravelled)
  }

  private fun endButtonClicked() {
    var distancetoend = 0.0
    val prev = Location("prev")
    val cur = Location("cur")
    prev.latitude = prevLat
    prev.longitude = prevLong
    cur.latitude = 40.44476296170437
    cur.longitude = -79.94615793228151
    distancetoend = prev.distanceTo(cur).toDouble()
    if (distancetoend < 200){
      AlertDialog.Builder(this)
              .setTitle("You have reached your destination!")
              .setPositiveButton("Confirm") { _, _ ->
                isTracking = false
                updateButtonStatus()
                stopTracking()
                Log.d("TAG", "LatLngString: $LatLngString")

              }.setNegativeButton("Cancel") { _, _ ->
              }
              .create()
              .show()

    }else{
      AlertDialog.Builder(this)
              .setTitle("You still have $distancetoend meters to go!")
              .setPositiveButton("Confirm") { _, _ ->
              }
              .create()
              .show()
    }

  }

  // Tracking
  @SuppressLint("CheckResult")
  private fun startTracking() {

// ACTIVITY_RECOGNITION is not a necessary permission for Android with version below Q.
    val isActivityRecognitionPermissionFree = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    val version = Build.VERSION.SDK_INT
    val isActivityRecognitionPermissionGranted = com.vmadalin.easypermissions.EasyPermissions.hasPermissions(this,
            android.Manifest.permission_group.ACTIVITY_RECOGNITION)
    Log.d("TAG", "Is ACTIVITY_RECOGNITION permission granted $isActivityRecognitionPermissionGranted")
    Log.d("TAG", "Is ACTIVITY_RECOGNITION permission free $version")
    setupLocationChangeListener()
    if (isActivityRecognitionPermissionFree || isActivityRecognitionPermissionGranted) {
      // Action to be triggered after permission is granted
      if (isActivityRecognitionPermissionFree || isActivityRecognitionPermissionGranted) {
        // Action to be triggered after permission is granted
        setupLocationChangeListener()
      } else {
        // Do not have permissions, request them now
      }
    } else {
      // Do not have permissions, request them now
      Log.d("TAG", "ASK FOR PERMISSION!!!!!!")
//      ActivityCompat.requestPermissions(this,
//              arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
//              MY_PERMISSIONS_ACTIVITY_RECOGNITION);
      EasyPermissions.requestPermissions(
              host = this,
              rationale = "For showing your step counts and calculate the average pace.",
              requestCode = REQUEST_CODE_ACTIVITY_RECOGNITION,
              perms = *arrayOf(android.Manifest.permission_group.ACTIVITY_RECOGNITION)
      )
    }

  }

  private fun stopTracking() {
    // 1
    polylineOptions = PolylineOptions()
    fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    // 4
  }

  // Map related codes
  /**
   * Manipulates the map once available.
   * This callback is triggered when the map is ready to be used.
   * This is where we can add markers or lines, add listeners or move the camera. In this case,
   * we just add a marker near Sydney, Australia.
   * If Google Play services is not installed on the device, the user will be prompted to install
   * it inside the SupportMapFragment. This method will only be triggered once the user has
   * installed Google Play services and returned to the app.
   */
  @SuppressLint("MissingPermission")
  override fun onMapReady(googleMap: GoogleMap) {
    mMap = googleMap
    showUserLocation()


  }
  @AfterPermissionGranted(REQUEST_CODE_FINE_LOCATION)
  private fun showUserLocation() {
    if (EasyPermissions.hasPermissions(this, ACCESS_FINE_LOCATION)) {
      if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return
      }
      mMap.isMyLocationEnabled = true
    } else {
      // Do not have permissions, request them now
      EasyPermissions.requestPermissions(
              host = this,
              rationale = "For showing your current location on the map.",
              requestCode = REQUEST_CODE_FINE_LOCATION,
              perms = *arrayOf(ACCESS_FINE_LOCATION)
      )
    }
  }
//  override fun onMapReady(googleMap: GoogleMap) {
//    mMap = googleMap
//
//    // Add a marker in Hong Kong and move the camera
//    val latitude = 40.448500
//    val longitude = -79.946930
//    val hongKongLatLong = LatLng(latitude, longitude)
//
//    val zoomLevel = 9.5f
//    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(hongKongLatLong, zoomLevel))
//  }
  val locationCallback = object: LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult?) {
      if (locationResult != null) {
        addLocationToRoute(locationResult.locations)
      }
      super.onLocationResult(locationResult)
      locationResult ?: return
      locationResult.locations.forEach {
        Log.d("TAG", "New location got: (${it.latitude}, ${it.longitude})")
      }
    }
  }
  @AfterPermissionGranted(REQUEST_CODE_FINE_LOCATION)
  private fun setupLocationChangeListener() {
    Log.d("TAG", "!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    if (EasyPermissions.hasPermissions(this, ACCESS_FINE_LOCATION)) {
      val locationRequest = LocationRequest()
      locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
      locationRequest.interval = 5000 // 5000ms (5s)
      if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        // TODO: Consider calling
        //    ActivityCompat#requestPermissions
        // here to request the missing permissions, and then overriding
        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
        //                                          int[] grantResults)
        // to handle the case where the user grants the permission. See the documentation
        // for ActivityCompat#requestPermissions for more details.
        return
      }
      fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    } else {
      // Do not have permissions, request them now
      EasyPermissions.requestPermissions(
              host = this,
              rationale = "For showing your current location on the map.",
              requestCode = REQUEST_CODE_FINE_LOCATION,
              perms = *arrayOf(ACCESS_FINE_LOCATION)
      )
    }
  }
  fun convertToPolyLineOptions(cordList: MutableList<Double>) : PolylineOptions{
    var polylineO = PolylineOptions()
    for (i in cordList.indices step 2){
      val originalLatLngList = polylineO.points
      var latLng = LatLng(cordList[i],cordList[i + 1])
      originalLatLngList.add(latLng)
      Log.d("TAG", "Save MEEEEEEEEEEEE")
    }
    mMap.addPolyline(polylineO)
    return polylineO
  }
  var polylineOptions = PolylineOptions()
  var prevLat = 40.443370
  var prevLong = -79.944730
  var distance = 0.0
  val LatLngString: MutableList<Double> = mutableListOf(40.443370,-79.944730)
  fun addLocationToRoute(locations: List<Location>) {
    mMap.clear()
    val originalLatLngList = polylineOptions.points
    val latLngList = locations.map {
      LatLng(it.latitude, it.longitude)
    }
    originalLatLngList.addAll(latLngList)
    mMap.addPolyline(polylineOptions)
    val prevLocation = Location("previous")
    locations.forEach{
      LatLngString.add(it.latitude)
      LatLngString.add(it.longitude)
      val prev = Location("prev")
      val cur = Location("cur")
      prev.latitude = prevLat
      prev.longitude = prevLong
      cur.latitude = it.latitude
      cur.longitude = it.longitude
      distance += prev.distanceTo(cur)
      prevLat = cur.latitude
      prevLong = cur.longitude

      updateAllDisplayText(0, distance.toFloat())
    }
  }




}
