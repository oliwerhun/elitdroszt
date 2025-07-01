package hu.drosztok.android

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DashboardPagerAdapter(fragmentActivity: FragmentActivity, private val tabs: List<String>) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        val tabTitle = tabs[position]
        return when (tabTitle) {
            "Admin" -> AdminFragment()
            "213" -> NotesFragment.newInstance(tabTitle)
            "V-Osztály" -> VClassContainerFragment()
            "Reptér" -> AirportContainerFragment()
            "Profil" -> ProfileFragment()
            "Térkép" -> MapFragment()
            else -> LocationFragment.newInstance(tabTitle)
        }
    }
}