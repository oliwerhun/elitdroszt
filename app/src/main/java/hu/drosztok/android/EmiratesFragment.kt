package hu.drosztok.android

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

class EmiratesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var firestoreListener: ListenerRegistration? = null
    private lateinit var memberAdapter: MemberAdapter
    private val locationName = "Reptér"
    private val membersFieldName = "emiratesMembers"
    private var currentUserAsMember: Member? = null
    private var isCurrentUserCheckedIn = false
    private var lastCheckedOutMember: Member? = null
    private var lastCheckedOutIndex: Int = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        memberAdapter = MemberAdapter()
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val locationNameTextView: TextView = view.findViewById(R.id.location_name_textview)
        val membersRecyclerView: RecyclerView = view.findViewById(R.id.members_recycler_view)
        val checkInButton: Button = view.findViewById(R.id.check_in_button)
        val checkOutButton: Button = view.findViewById(R.id.check_out_button)
        val flameButton: Button = view.findViewById(R.id.flame_button)
        val foodPhoneButton: Button = view.findViewById(R.id.food_phone_button)
        view.findViewById<LinearLayout>(R.id.button_panel).visibility = View.VISIBLE

        membersRecyclerView.adapter = memberAdapter
        fetchCurrentUserProfile()
        startListeningForMembers(checkInButton, checkOutButton, flameButton, foodPhoneButton, locationNameTextView)

        checkInButton.setOnClickListener { checkIn() }
        checkOutButton.setOnClickListener { checkOut() }
        flameButton.setOnClickListener { flameCheckIn() }
        foodPhoneButton.setOnClickListener { toggleFoodPhoneStatus() }
    }

    private fun mapToMember(memberMap: Map<String, Any>): Member = Member(
        uid = memberMap["uid"] as? String ?: "",
        username = memberMap["username"] as? String ?: "",
        displayName = memberMap["displayName"] as? String ?: "",
        licensePlate = memberMap["licensePlate"] as? String ?: "",
        userType = memberMap["userType"] as? String ?: ""
    )

    private fun fetchCurrentUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("profiles").document(userId).get().addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val username = document.getString("username") ?: ""
                val userType = document.getString("userType") ?: ""
                val licensePlate = document.getString("licensePlate") ?: ""
                val typeSuffix = when (userType) { "Taxi" -> "S"; "Kombi Taxi" -> "SK"; "V-Osztály" -> "V"; "VIP" -> ""; "VIP Kombi" -> "K"; else -> "" }
                val finalDisplayName = "$username$typeSuffix $licensePlate"
                currentUserAsMember = Member(userId, username, finalDisplayName, licensePlate, userType)
            }
        }
    }

    private fun startListeningForMembers(checkInButton: Button, checkOutButton: Button, flameButton: Button, foodPhoneButton: Button, titleTextView: TextView) {
        val locationRef = db.collection("locations").document(locationName)
        firestoreListener = locationRef.addSnapshotListener { snapshot, e ->
            if (e != null) { Log.w("EmiratesFragment", "Listen failed.", e); return@addSnapshotListener }
            val membersList = if (snapshot != null && snapshot.exists()) {
                (snapshot.get(membersFieldName) as? List<Map<String, Any>> ?: listOf()).map { mapToMember(it) }
            } else { listOf() }
            memberAdapter.submitList(membersList)
            titleTextView.text = "Emirates (${membersList.size})"
            isCurrentUserCheckedIn = membersList.any { it.uid == auth.currentUser?.uid }
            updateButtonStates(checkInButton, checkOutButton, flameButton, foodPhoneButton)
        }
    }

    private fun updateButtonStates(checkInButton: Button, checkOutButton: Button, flameButton: Button, foodPhoneButton: Button) {
        val canFlame = lastCheckedOutMember != null && !isCurrentUserCheckedIn
        checkInButton.isEnabled = !isCurrentUserCheckedIn
        checkOutButton.isEnabled = isCurrentUserCheckedIn
        foodPhoneButton.isEnabled = isCurrentUserCheckedIn
        flameButton.isEnabled = canFlame
    }

    private fun checkIn() {
        if (currentUserAsMember == null) { return }
        FirestoreHelper.checkoutFromAllLocations(db, auth.currentUser) {
            lastCheckedOutMember = null
            val locationRef = db.collection("locations").document(locationName)
            locationRef.set(mapOf(membersFieldName to FieldValue.arrayUnion(currentUserAsMember)), SetOptions.merge())
        }
    }

    private fun checkOut() {
        val myUid = auth.currentUser?.uid ?: return
        val memberObjectInList = memberAdapter.currentList.find { it.uid == myUid } ?: return
        lastCheckedOutMember = memberObjectInList
        lastCheckedOutIndex = memberAdapter.currentList.indexOf(memberObjectInList)
        val locationRef = db.collection("locations").document(locationName)
        locationRef.update(membersFieldName, FieldValue.arrayRemove(memberObjectInList))
    }

    private fun flameCheckIn() {
        if (lastCheckedOutMember == null || lastCheckedOutIndex == -1) return
        FirestoreHelper.checkoutFromAllLocations(db, auth.currentUser) {
            val memberToReinsert = lastCheckedOutMember!!
            val indexToInsert = lastCheckedOutIndex
            val cleanName = memberToReinsert.displayName.replace("🔥 ", "").replace("🍔 ", "")
            val flamedMember = memberToReinsert.copy(displayName = "🔥 $cleanName")
            val locationRef = db.collection("locations").document(locationName)
            db.runTransaction { transaction ->
                val snapshot = transaction.get(locationRef)
                val oldMembersData = snapshot.get(membersFieldName) as? List<Map<String, Any>> ?: listOf()
                val oldMembers = oldMembersData.map { mapToMember(it) }
                val newMembers = oldMembers.toMutableList()
                if (indexToInsert <= newMembers.size) { newMembers.add(indexToInsert, flamedMember) } else { newMembers.add(flamedMember) }
                transaction.update(locationRef, membersFieldName, newMembers)
            }.addOnSuccessListener { lastCheckedOutMember = null }
        }
    }

    private fun toggleFoodPhoneStatus() {
        val myUid = auth.currentUser?.uid ?: return
        val myObject = memberAdapter.currentList.find { it.uid == myUid } ?: return
        val hasFood = myObject.displayName.contains("🍔")
        val cleanName = myObject.displayName.replace("🔥 ", "").replace("🍔 ", "")
        val newName = if (hasFood) { if (myObject.displayName.startsWith("🔥")) "🔥 $cleanName" else cleanName } else { if (myObject.displayName.startsWith("🔥")) "🔥 🍔 $cleanName" else "🍔 $cleanName" }
        val updatedMember = myObject.copy(displayName = newName)
        val locationRef = db.collection("locations").document(locationName)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(locationRef)
            val oldMembersData = snapshot.get(membersFieldName) as? List<Map<String, Any>> ?: listOf()
            val oldMembers = oldMembersData.map { mapToMember(it) }
            val newMembers = oldMembers.map { if (it.uid == myUid) updatedMember else it }
            transaction.update(locationRef, membersFieldName, newMembers)
        }
    }

    override fun onStop() {
        super.onStop()
        firestoreListener?.remove()
    }

    companion object {
        fun newInstance(): EmiratesFragment {
            return EmiratesFragment()
        }
    }
}