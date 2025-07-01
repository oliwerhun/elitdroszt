package hu.drosztok.android

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class AirportPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> LocationFragment.newInstance("Reptér")
            1 -> NotesFragment.newInstance("Reptér")
            2 -> EmiratesFragment.newInstance() // Most már helyesen hívja meg
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}