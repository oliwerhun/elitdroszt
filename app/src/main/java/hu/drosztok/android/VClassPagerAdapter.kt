package hu.drosztok.android

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

// Ez az adapter a Fragment-en belül lesz használva, ezért a konstruktora Fragment-et kér
class VClassPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val tabTitles = listOf("V-Osztály Sor", "Rendelések")

    override fun getItemCount(): Int {
        return tabTitles.size
    }

    override fun createFragment(position: Int): Fragment {
        // A pozíció alapján eldöntjük, melyik fragmentet mutatjuk
        return when (position) {
            0 -> LocationFragment.newInstance("V-Osztály") // Az első fül a sofőrsor
            1 -> NotesFragment.newInstance("V-Osztály")    // A második fül a jegyzetlista
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}