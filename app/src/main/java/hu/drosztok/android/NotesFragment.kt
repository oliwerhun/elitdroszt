package hu.drosztok.android

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotesFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private var firestoreListener: ListenerRegistration? = null
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var pageTitle: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        db = FirebaseFirestore.getInstance()
        arguments?.getString("PAGE_TITLE")?.let { pageTitle = it }
        return inflater.inflate(R.layout.fragment_notes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleTextView: TextView = view.findViewById(R.id.notes_title_textview)
        val notesRecyclerView: RecyclerView = view.findViewById(R.id.notes_recycler_view)
        val addNoteFab: FloatingActionButton = view.findViewById(R.id.add_note_fab)
        titleTextView.text = pageTitle
        setupRecyclerView(notesRecyclerView)
        addNoteFab.setOnClickListener { addNewNote() }
        startListeningForNotes()
    }

    private fun setupRecyclerView(notesRecyclerView: RecyclerView) {
        val callback = NoteItemTouchHelperCallback(object : ItemTouchHelperAdapter {
            override fun onRowMoved(fromPosition: Int, toPosition: Int) {
                noteAdapter.moveItem(fromPosition, toPosition)
            }
            override fun onDragFinished() {
                saveNoteOrder(noteAdapter.currentList)
            }
        })
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(notesRecyclerView)

        noteAdapter = NoteAdapter(
            touchHelper = itemTouchHelper,
            onNoteEdited = { position, newText -> updateNoteAt(position, newText) },
            onDeleteClicked = { positionToDelete -> deleteNoteAt(positionToDelete) }
        )
        notesRecyclerView.adapter = noteAdapter
    }

    private fun startListeningForNotes() {
        if (pageTitle.isBlank()) return
        val notesRef = db.collection("locations").document(pageTitle)
        firestoreListener = notesRef.addSnapshotListener { snapshot, e ->
            if (e != null) { Log.w("NotesFragment", "Listen failed.", e); return@addSnapshotListener }
            val notesList = if (snapshot != null && snapshot.exists()) {
                snapshot.get("notes") as? List<String> ?: listOf()
            } else { listOf() }
            noteAdapter.submitList(notesList)
        }
    }

    private fun saveNoteOrder(updatedList: List<String>) {
        if (pageTitle.isBlank()) return
        val notesRef = db.collection("locations").document(pageTitle)
        notesRef.update("notes", updatedList)
            .addOnSuccessListener { Log.d("NotesFragment", "Sorrend mentve.") }
            .addOnFailureListener { e -> Log.w("NotesFragment", "Sorrend mentése sikertelen", e) }
    }

    private fun addNewNote() {
        if (pageTitle.isBlank()) return
        val notesRef = db.collection("locations").document(pageTitle)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(notesRef)
            val oldNotes = if (snapshot.exists()) { snapshot.get("notes") as? List<String> ?: listOf() } else { listOf() }
            val newNotes = mutableListOf("").apply { addAll(oldNotes) }
            transaction.set(notesRef, mapOf("notes" to newNotes))
        }.addOnFailureListener { e -> Log.w("NotesFragment", "Hozzáadás sikertelen", e) }
    }

    private fun deleteNoteAt(position: Int) {
        if (pageTitle.isBlank()) return
        val notesRef = db.collection("locations").document(pageTitle)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(notesRef)
            val oldNotes = snapshot.get("notes") as? List<String> ?: return@runTransaction
            if (position < oldNotes.size) {
                val newNotes = oldNotes.toMutableList().apply { removeAt(position) }
                transaction.update(notesRef, "notes", newNotes)
            }
        }.addOnFailureListener { e -> Log.w("NotesFragment", "Törlés sikertelen", e) }
    }

    private fun updateNoteAt(position: Int, newText: String) {
        if (pageTitle.isBlank()) return
        val notesRef = db.collection("locations").document(pageTitle)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(notesRef)
            val oldNotes = snapshot.get("notes") as? List<String> ?: return@runTransaction
            if (position < oldNotes.size) {
                val newNotes = oldNotes.toMutableList().apply { this[position] = newText }
                transaction.update(notesRef, "notes", newNotes)
            }
        }.addOnFailureListener { e -> Log.w("NotesFragment", "Szerkesztés sikertelen", e) }
    }

    override fun onStop() {
        super.onStop()
        firestoreListener?.remove()
    }

    companion object {
        fun newInstance(title: String): NotesFragment {
            val fragment = NotesFragment()
            val args = Bundle()
            args.putString("PAGE_TITLE", title)
            fragment.arguments = args
            return fragment
        }
    }
}