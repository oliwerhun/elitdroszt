package hu.drosztok.android

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AirportContainerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_airport_container, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout: TabLayout = view.findViewById(R.id.airport_tab_layout)
        val viewPager: ViewPager2 = view.findViewById(R.id.airport_view_pager)
        val pagerAdapter = AirportPagerAdapter(this)

        viewPager.adapter = pagerAdapter

        // Összekötjük a füleket a lapozóval és beállítjuk a címkéket
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Reptér"
                1 -> "Rendelések"
                2 -> "Emirates"
                else -> ""
            }
        }.attach()
    }
}