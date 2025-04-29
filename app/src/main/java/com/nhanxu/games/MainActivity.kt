package com.nhanxu.games

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private var bottomNav: BottomNavigationView? = null

    private var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav?.setOnItemSelectedListener(navListener)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private val navListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        var selectedFragment: Fragment? = null

        when (item.itemId) {
            R.id.nav_home -> {
                selectedFragment = supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName)
                if (selectedFragment == null) {
                    selectedFragment = HomeFragment()
                }
            }
            R.id.nav_all_games -> {
                selectedFragment = supportFragmentManager.findFragmentByTag(AllGamesFragment::class.java.simpleName)
                if (selectedFragment == null) {
                    selectedFragment = AllGamesFragment()
                }
            }
            R.id.nav_settings -> {
                selectedFragment = supportFragmentManager.findFragmentByTag(SettingsFragment::class.java.simpleName)
                if (selectedFragment == null) {
                    selectedFragment = SettingsFragment()
                }
            }
        }

        if (selectedFragment != null && currentFragment != selectedFragment) {
            loadFragment(selectedFragment)
        }
        true
    }

    internal fun loadFragment(fragment: Fragment?) {
        fragment?.let {
            val transaction = supportFragmentManager.beginTransaction()

            val fragmentTag = it.javaClass.simpleName
            val existingFragment = supportFragmentManager.findFragmentByTag(fragmentTag)

            if (existingFragment == null) {
                transaction.add(R.id.fragment_container, it, fragmentTag)
            } else {
                transaction.show(existingFragment)
            }

            if (currentFragment != null && currentFragment != it) {
                transaction.hide(currentFragment!!)
            }

            transaction.commit()
            currentFragment = it
        }
    }

    fun hideBottomNav() {
        if (bottomNav?.visibility != View.GONE) {
            bottomNav?.visibility = View.GONE
        }
    }

    fun showBottomNav() {
        if (bottomNav?.visibility != View.VISIBLE) {
            bottomNav?.visibility = View.VISIBLE
        }
    }
}
