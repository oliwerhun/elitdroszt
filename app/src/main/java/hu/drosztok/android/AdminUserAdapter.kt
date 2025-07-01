package hu.drosztok.android

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class AdminUserAdapter(
    private val onApproveClick: (String) -> Unit,
    private val onSuspendClick: (String) -> Unit,
    private val onRoleChangeClick: (String, String) -> Unit,
    private val onSee213ChangeClick: (String, Boolean) -> Unit,
    private val onDeleteUserClick: (String) -> Unit
) : ListAdapter<AdminUser, AdminUserAdapter.UserViewHolder>(UserDiffCallback()) {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val usernameTextView: TextView = itemView.findViewById(R.id.admin_username_textview)
        val statusTextView: TextView = itemView.findViewById(R.id.admin_status_textview)
        val approveButton: Button = itemView.findViewById(R.id.approve_button)
        val suspendButton: Button = itemView.findViewById(R.id.suspend_button)
        val roleButton: Button = itemView.findViewById(R.id.role_button)
        val see213Checkbox: CheckBox = itemView.findViewById(R.id.see_213_checkbox)
        val deleteUserButton: ImageButton = itemView.findViewById(R.id.delete_user_button)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)

        holder.usernameTextView.text = "${user.username} - ${user.licensePlate} (${user.userType})"
        holder.statusTextView.text = user.status

        // Státusz alapján a gombok láthatósága
        holder.approveButton.visibility = if (user.status == "pending") View.VISIBLE else View.GONE
        holder.suspendButton.visibility = if (user.status == "approved") View.VISIBLE else View.GONE

        // Szerepkör alapján a gomb szövege
        if (user.role == "admin") {
            holder.roleButton.text = "Admin eltávolítása"
            holder.roleButton.setOnClickListener { onRoleChangeClick(user.id, "user") }
        } else {
            holder.roleButton.text = "Adminná tesz"
            holder.roleButton.setOnClickListener { onRoleChangeClick(user.id, "admin") }
        }

        holder.see213Checkbox.isChecked = user.canSee213

        holder.approveButton.setOnClickListener { onApproveClick(user.id) }
        holder.suspendButton.setOnClickListener { onSuspendClick(user.id) }
        holder.see213Checkbox.setOnCheckedChangeListener { _, isChecked -> onSee213ChangeClick(user.id, isChecked) }
        holder.deleteUserButton.setOnClickListener { onDeleteUserClick(user.id) }
    }
}

class UserDiffCallback : DiffUtil.ItemCallback<AdminUser>() {
    override fun areItemsTheSame(oldItem: AdminUser, newItem: AdminUser): Boolean = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: AdminUser, newItem: AdminUser): Boolean = oldItem == newItem
}