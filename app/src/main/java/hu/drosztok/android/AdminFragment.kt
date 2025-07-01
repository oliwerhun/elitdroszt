package hu.drosztok.android

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

data class AdminUser(
    val id: String = "",
    val username: String = "",
    val licensePlate: String = "",
    val userType: String = "",
    val status: String = "",
    val role: String = "",
    val canSee213: Boolean = false
)

class AdminFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adminUserAdapter: AdminUserAdapter
    private var userListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Az adapternek átadjuk a függvényeket, amiket a gombok meg fognak hívni
        adminUserAdapter = AdminUserAdapter(
            onApproveClick = { userId -> updateUserStatus(userId, "approved") },
            onSuspendClick = { userId -> updateUserStatus(userId, "pending") },
            onRoleChangeClick = { userId, newRole -> updateUserRole(userId, newRole) },
            onSee213ChangeClick = { userId, canSee -> updateSee213(userId, canSee) },
            onDeleteUserClick = { userId -> confirmUserDeletion(userId) }
        )

        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = view.findViewById(R.id.admin_user_list_recyclerview)
        recyclerView.adapter = adminUserAdapter
        startListeningForUsers()
    }

    private fun startListeningForUsers() {
        val currentUserId = auth.currentUser?.uid
        userListener = db.collection("profiles")
            .orderBy("username", Query.Direction.ASCENDING)
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    Log.w("AdminFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val userList = mutableListOf<AdminUser>()
                if (documents != null) {
                    for (document in documents) {
                        if (document.id == currentUserId) continue // Saját magunkat nem listázzuk

                        val user = AdminUser(
                            id = document.id,
                            username = document.getString("username") ?: "",
                            licensePlate = document.getString("licensePlate") ?: "",
                            userType = document.getString("userType") ?: "",
                            status = document.getString("status") ?: "",
                            role = document.getString("role") ?: "",
                            canSee213 = document.getBoolean("canSee213") ?: false
                        )
                        userList.add(user)
                    }
                }
                adminUserAdapter.submitList(userList)
            }
    }

    private fun updateUserStatus(userId: String, newStatus: String) {
        db.collection("profiles").document(userId).update("status", newStatus)
    }

    private fun updateUserRole(userId: String, newRole: String) {
        db.collection("profiles").document(userId).update("role", newRole)
    }

    private fun updateSee213(userId: String, canSee: Boolean) {
        db.collection("profiles").document(userId).update("canSee213", canSee)
    }

    private fun confirmUserDeletion(userId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Felhasználó törlése")
            .setMessage("Biztosan törölni szeretnéd ezt a felhasználót? A művelet nem vonható vissza.")
            .setPositiveButton("Törlés") { _, _ ->
                deleteUser(userId)
            }
            .setNegativeButton("Mégse", null)
            .show()
    }

    private fun deleteUser(userId: String) {
        // TODO: A felhasználó hitelesítési fiókjának törlését (Firebase Auth) csak szerver oldali kóddal lehet biztonságosan elvégezni.
        // Egyelőre csak a profilját és a sorban állásait töröljük.

        // 1. Kiléptetjük minden sorból
        FirestoreHelper.checkoutFromAllLocations(db, null) { // A null user miatt a helper a profilból fogja kikeresni az adatokat, ami már lehet, hogy törölve lesz. Ezt a helperben kellene majd finomítani, de egyelőre a profil törlése a fontosabb.
            // 2. Töröljük a profilját
            db.collection("profiles").document(userId).delete()
                .addOnSuccessListener { Log.d("AdminFragment", "Felhasználó ($userId) sikeresen törölve a profilok közül.") }
                .addOnFailureListener { e -> Log.w("AdminFragment", "Hiba a felhasználó törlésekor", e) }
        }
    }

    override fun onStop() {
        super.onStop()
        userListener?.remove() // Leállítjuk a figyelőt, ha elhagyjuk a képernyőt
    }
}