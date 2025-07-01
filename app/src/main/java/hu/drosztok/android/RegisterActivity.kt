package hu.drosztok.android

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val usernameInput: TextInputEditText = findViewById(R.id.register_username)
        val passwordInput: TextInputEditText = findViewById(R.id.register_password)
        val licensePlateInput: TextInputEditText = findViewById(R.id.register_license_plate)
        val typeSpinner: Spinner = findViewById(R.id.register_type_spinner)
        val registerButton: Button = findViewById(R.id.register_button)

        registerButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val licensePlate = licensePlateInput.text.toString().trim().uppercase()
            val vehicleType = typeSpinner.selectedItem.toString()

            if (username.length != 3 || password.isBlank() || licensePlate.isBlank() || vehicleType == "Válassz típust...") {
                Toast.makeText(this, "Kérlek, tölts ki minden mezőt helyesen!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val email = "$username@example.com"

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("REGISTER_AUTH", "Felhasználó létrehozva: $email")
                        // Itt már az új, okosabb függvényt hívjuk meg
                        checkIfFirstUserAndSaveProfile(username, licensePlate, vehicleType)
                    } else {
                        Log.w("REGISTER_AUTH", "Hiba a felhasználó létrehozásakor", task.exception)
                        Toast.makeText(this, "Regisztráció sikertelen: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun checkIfFirstUserAndSaveProfile(username: String, licensePlate: String, vehicleType: String) {
        val userId = auth.currentUser?.uid ?: return

        // Mielőtt mentünk, ellenőrizzük, hogy létezik-e már felhasználó az adatbázisban
        db.collection("profiles").limit(1).get()
            .addOnSuccessListener { querySnapshot ->

                val isFirstUser = querySnapshot.isEmpty

                val userProfile = hashMapOf(
                    "username" to username,
                    "licensePlate" to licensePlate,
                    "userType" to vehicleType,
                    // Ha ez az első felhasználó, 'approved' és 'admin' lesz, egyébként 'pending' és 'user'
                    "status" to if (isFirstUser) "approved" else "pending",
                    "role" to if (isFirstUser) "admin" else "user",
                    "canSee213" to isFirstUser // Az admin láthatja a 213-at
                )

                // Profil mentése a "profiles" kollekcióba
                db.collection("profiles").document(userId)
                    .set(userProfile)
                    .addOnSuccessListener {
                        Log.d("FIRESTORE_SUCCESS", "Profil sikeresen mentve. Admin: $isFirstUser")
                        val message = if (isFirstUser) "Sikeres admin regisztráció!" else "Sikeres regisztráció! A fiókod jóváhagyásra vár."
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        finish() // Visszalépés a bejelentkező képernyőre
                    }
                    .addOnFailureListener { e ->
                        Log.w("FIRESTORE_FAILURE", "Hiba a profil mentésekor", e)
                        Toast.makeText(this, "Hiba történt a profil mentésekor.", Toast.LENGTH_LONG).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.w("FIRESTORE_FAILURE", "Hiba a felhasználók ellenőrzésekor", e)
                Toast.makeText(this, "Hiba az adatbázis-kapcsolatban.", Toast.LENGTH_LONG).show()
            }
    }
}