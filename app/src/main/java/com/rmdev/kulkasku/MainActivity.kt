package com.rmdev.kulkasku

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.rmdev.kulkasku.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        setTheme(R.style.Theme_Kulkasku)

        // Inflate layout and setup binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(binding.root)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Setup bottom navigation view and nav controller
        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        // Setup AppBarConfiguration with the top-level destinations
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_shop, R.id.navigation_reels
            )
        )

        navView.setupWithNavController(navController)

        // Manually handle bottom navigation item clicks
        navView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    navController.navigate(R.id.navigation_home)
                    true
                }
                R.id.navigation_shop -> {
                    navController.navigate(R.id.navigation_shop)
                    true
                }
                R.id.navigation_cart -> {
                    //navController.navigate(R.id.navigation_cart)
                    true
                }
                R.id.navigation_history -> {
                    // Do nothing
                    true
                }
                R.id.navigation_reels -> {
                    navController.navigate(R.id.navigation_reels)
                    true
                }
                else -> false
            }
        }

        // Navigation listener to show/hide the bottom navigation bar based on the destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            navView.visibility = when (destination.id) {
                R.id.navigation_home, R.id.navigation_shop, R.id.navigation_cart -> View.VISIBLE
                else -> View.GONE
            }
        }

        // Direct users based on their authentication status
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.loginFragment && auth.currentUser == null) {
                // No user is signed in and we are not on the login page, redirect to login
                navController.navigate(R.id.loginFragment)
            } else if (destination.id == R.id.loginFragment && auth.currentUser != null) {
                // User is signed in but we are on the login page, redirect to home
                navController.navigate(R.id.navigation_home)
            }
        }
    }

    fun getBottomNavBarHeight(): Int {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.navView)
        bottomNavigationView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        return bottomNavigationView.measuredHeight
    }


    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
