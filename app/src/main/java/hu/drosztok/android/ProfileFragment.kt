package hu.drosztok.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var usernameTextView: TextView
    private lateinit var licensePlateEditText: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var saveButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        usernameTextView = view.findViewById(R.id.profile_username_textview)
        licensePlateEditText = view.findViewById(R.id.profile_license_plate_edittext)
        typeSpinner = view.findViewById(R.id.profile_type_spinner)
        saveButton = view.findViewById(R.id.save_profile_button)

        loadUserProfile()

        saveButton.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("profiles").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val username = document.getString("username") ?: ""
                    val licensePlate = document.getString("licensePlate") ?: ""
                    val userType = document.getString("userType") ?: ""

                    usernameTextView.text = username
                    licensePlateEditText.setText(licensePlate)

                    // Beállítjuk a Spinner-t a megfelelő értékre
                    val adapter = typeSpinner.adapter as? ArrayAdapter<String>
                    if (adapter != null) {
                        val position = adapter.getPosition(userType)
                        if (position >= 0) {
                            typeSpinner.setSelection(position)
                        }
                    }
                }
            }
    }

    private fun saveUserProfile() {
        val userId = auth.currentUser?.uid ?: return

        val newLicensePlate = licensePlateEditText.text.toString().trim().uppercase()
        val newUserType = typeSpinner.selectedItem.toString()

        if (newLicensePlate.isBlank()) {
            Toast.makeText(context, "A rendszám nem lehet üres!", Toast.LENGTH_SHORT).show()
            return
        }

        val updatedData = mapOf(
            "licensePlate" to newLicensePlate,
            "userType" to newUserType
        )

        db.collection("profiles").document(userId).update(updatedData)
            .addOnSuccessListener {
                Toast.makeText(context, "Profil sikeresen mentve!", Toast.LENGTH_SHORT).show()
                // TODO: Frissíteni kell a displayName-t mindenhol, ahol a user sorban áll
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Hiba a mentés során: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}