package hu.drosztok.android

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections

class NoteAdapter(
    private val touchHelper: ItemTouchHelper,
    private val onNoteEdited: (Int, String) -> Unit,
    private val onDeleteClicked: (Int) -> Unit
) : ListAdapter<String, NoteAdapter.NoteViewHolder>(StringDiffCallback()) {

    private var onNoteOrderChanged: (() -> Unit)? = null
    fun setOnNoteOrderChangedListener(listener: () -> Unit) {
        this.onNoteOrderChanged = listener
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val noteEditText: EditText = itemView.findViewById(R.id.note_edit_text)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_note_button)
        val dragHandle: ImageView = itemView.findViewById(R.id.drag_handle)

        fun bind() {
            val item = getItem(bindingAdapterPosition)
            noteEditText.setText(item)

            deleteButton.setOnClickListener {
                if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                    onDeleteClicked(bindingAdapterPosition)
                }
            }

            noteEditText.setOnEditorActionListener { textView, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (bindingAdapterPosition != RecyclerView.NO_POSITION) {
                        onNoteEdited(bindingAdapterPosition, textView.text.toString())
                    }
                    textView.clearFocus()
                    val imm = textView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(textView.windowToken, 0)
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind()
        holder.dragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(holder)
            }
            true
        }
    }

    fun moveItem(from: Int, to: Int) {
        val list = currentList.toMutableList()
        Collections.swap(list, from, to)
        submitList(list, onNoteOrderChanged)
    }
}

class StringDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean = oldItem == newItem
}