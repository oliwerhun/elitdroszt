package hu.drosztok.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class MemberAdapter : ListAdapter<Member, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    class MemberViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Itt összekötjük a kódot az item_member.xml-ben lévő TextView-val
        private val nameTextView: TextView = itemView.findViewById(R.id.member_display_name_textview)

        fun bind(member: Member) {
            // Beállítjuk a sofőr nevét a TextView-ban
            nameTextView.text = member.displayName
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        // Létrehozzuk egy sor kinézetét az item_member.xml alapján
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_member, parent, false)
        return MemberViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        // Elővesszük az aktuális sofőrt a listából
        val member = getItem(position)
        // És a ViewHolder 'bind' függvényével beállítjuk az adatokat
        holder.bind(member)
    }
}

// Ez a segédosztály segít a RecyclerView-nak hatékonyan frissíteni a listát
class MemberDiffCallback : DiffUtil.ItemCallback<Member>() {
    override fun areItemsTheSame(oldItem: Member, newItem: Member): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(oldItem: Member, newItem: Member): Boolean {
        return oldItem == newItem
    }
}