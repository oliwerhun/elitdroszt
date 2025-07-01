package hu.drosztok.android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var welcomeMessageTextView: TextView
    private lateinit var logoutTextView: TextView

    // Engedélykérési folyamat kezelője
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            when {
                permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                        permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    // Ha a felhasználó megadta a pontos vagy hozzávetőleges helymeghatározási engedélyt,
                    // akkor folytatjuk a háttérben futó engedély kérésével.
                    requestBackgroundLocationPermission()
                }
                else -> {
                    // A felhasználó elutasította az engedélyt.
                    showPermissionDeniedDialog()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        welcomeMessageTextView = findViewById(R.id.welcome_message_textview)
        logoutTextView = findViewById(R.id.logout_textview)

        logoutTextView.setOnClickListener { handleLogout() }

        checkAndRequestLocationPermissions()
    }

    private fun checkAndRequestLocationPermissions() {
        if (isFineLocationPermissionGranted()) {
            requestBackgroundLocationPermission()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun requestBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isBackgroundLocationPermissionGranted()) {
                onPermissionsGranted()
            } else {
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    showPermissionDeniedDialog()
                } else {
                    AlertDialog.Builder(this)
                        .setTitle("Háttérben futó helymeghatározás")
                        .setMessage("Az alkalmazásnak szüksége van a háttérben futó helymeghatározásra, hogy az adminisztrátorok akkor is lássák a pozíciódat, ha az app nincs a képernyőn. Kérjük, a beállításokban válaszd az 'Engedélyezés mindig' opciót.")
                        .setPositiveButton("Beállítások") { _, _ ->
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                            val uri = Uri.fromParts("package", packageName, null)
                            intent.data = uri
                            startActivity(intent)
                        }
                        .setNegativeButton("Mégse") { dialog, _ ->
                            dialog.dismiss()
                            onPermissionsGranted()
                        }
                        .create()
                        .show()
                }
            }
        } else {
            onPermissionsGranted()
        }
    }

    // Ez a függvény fut le, ha minden szükséges engedélyt megkaptunk
    private fun onPermissionsGranted() {
        Log.d("Permissions", "Minden szükséges engedély megadva.")
        startLocationService()
        // Most, hogy az engedélyek rendben vannak, betöltjük a felhasználói felületet
        fetchUserProfileAndSetupTabs()
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    private fun handleLogout() {
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)

        val currentUser = auth.currentUser
        FirestoreHelper.checkoutFromAllLocations(db, currentUser) {
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun isFineLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isBackgroundLocationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Engedély szükséges")
            .setMessage("Az alkalmazás megfelelő működéséhez elengedhetetlen a helymeghatározás engedélyezése. Kérjük, engedélyezd a beállításokban.")
            .setPositiveButton("Beállítások") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Kilépés") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun fetchUserProfileAndSetupTabs() {
        val userId = auth.currentUser?.uid
        if (userId == null) { handleLogout(); return }

        db.collection("profiles").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userProfile = document.data ?: mapOf()
                    welcomeMessageTextView.text = "Szia, ${userProfile["username"]}!"
                    setupTabsBasedOnProfile(userProfile)
                } else {
                    Toast.makeText(this, "Hiba: A felhasználói profil nem található.", Toast.LENGTH_LONG).show()
                    handleLogout()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Hiba az adatbázis-kapcsolatban.", Toast.LENGTH_LONG).show()
                handleLogout()
            }
    }

    private fun setupTabsBasedOnProfile(profile: Map<String, Any>) {
        val role = profile["role"] as? String ?: "user"
        val userType = profile["userType"] as? String ?: ""
        val canSee213 = profile["canSee213"] as? Boolean ?: false
        val tabTitles = mutableListOf("Akadémia", "Belváros", "Budai", "Conti", "Crowne", "Kozmo", "Reptér")

        if (userType == "V-Osztály" || role == "admin") {
            tabTitles.add("V-Osztály")
        }
        if (canSee213 || role == "admin") {
            tabTitles.add("213")
        }
        if (role == "admin") {
            tabTitles.add("Térkép")
            tabTitles.add("Admin")
        }
        tabTitles.add("Profil")

        val pagerAdapter = DashboardPagerAdapter(this, tabTitles)
        viewPager.adapter = pagerAdapter
        viewPager.isUserInputEnabled = false

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }
}