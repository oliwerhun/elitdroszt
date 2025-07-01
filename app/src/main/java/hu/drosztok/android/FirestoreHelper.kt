package hu.drosztok.android

import android.util.Log
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

object FirestoreHelper {
    fun checkoutFromAllLocations(db: FirebaseFirestore, user: FirebaseUser?, onComplete: () -> Unit) {
        if (user == null) { onComplete(); return }
        db.collection("profiles").document(user.uid).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val username = document.getString("username") ?: ""
                val userType = document.getString("userType") ?: ""
                val licensePlate = document.getString("licensePlate") ?: ""
                val typeSuffix = when (userType) { "Taxi" -> "S"; "Kombi Taxi" -> "SK"; "V-Osztály" -> "V"; "VIP" -> ""; "VIP Kombi" -> "K"; else -> "" }
                val finalDisplayName = "$username$typeSuffix $licensePlate"
                val memberObject = Member(user.uid, username, finalDisplayName, licensePlate, userType)
                val batch = db.batch()
                val allLocationNames = listOf("Akadémia", "Belváros", "Budai", "Conti", "Crowne", "Kozmo", "Reptér", "V-Osztály")
                allLocationNames.forEach { locName ->
                    val docRef = db.collection("locations").document(locName)
                    batch.update(docRef, "members", FieldValue.arrayRemove(memberObject))
                }
                val airportDocRef = db.collection("locations").document("Reptér")
                batch.update(airportDocRef, "emiratesMembers", FieldValue.arrayRemove(memberObject))
                batch.commit().addOnSuccessListener {
                    Log.d("FirestoreHelper", "Sikeres kiléptetés minden sorból.")
                    onComplete()
                }.addOnFailureListener { e ->
                    Log.w("FirestoreHelper", "Hiba a kiléptetés során", e)
                    onComplete()
                }
            } else { onComplete() }
        }.addOnFailureListener { onComplete() }
    }
}