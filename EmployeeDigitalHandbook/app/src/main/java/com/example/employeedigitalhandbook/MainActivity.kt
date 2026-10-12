package com.example.employeedigitalhandbook

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.employeedigitalhandbook.sessions.SessionManager
import com.example.employeedigitalhandbook.api.ApiClient

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //initalize pdf viewing
        ApiClient.init(applicationContext)

        // Initialize SessionManager
        sessionManager = SessionManager(applicationContext)

        setContentView(R.layout.activity_main)

        // Register FragmentLifecycleCallbacks to automatically track screen switches
        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    super.onFragmentResumed(fm, f)
                    val fragmentName = f.javaClass.simpleName
                    if (!fragmentName.contains("NavHostFragment")) {
                        sessionManager.onFragmentResumed(fragmentName)
                    }
                }

                override fun onFragmentPaused(fm: FragmentManager, f: Fragment) {
                    super.onFragmentPaused(fm, f)
                    val fragmentName = f.javaClass.simpleName
                    if (!fragmentName.contains("NavHostFragment")) {
                        sessionManager.onFragmentPaused()
                    }
                }
            },
            true
        )

        drawerLayout = findViewById(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val backPressedCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END)
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: View) {
                backPressedCallback.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: View) {
                backPressedCallback.isEnabled = false
            }
        })

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                }
                else -> {
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }
            }
        }

        setupDrawerMenu()

        //commented out TEMPORARILY for testing purposes
//        window.setFlags(
//            WindowManager.LayoutParams.FLAG_SECURE,
//            WindowManager.LayoutParams.FLAG_SECURE
//        )
    }

    override fun onStart() {
        super.onStart()
        // Begin tracking time whenever the app returns to foreground
        sessionManager.startTracking()
    }

    override fun onStop() {
        super.onStop()
        // End session and upload telemetry data to Cloud Run when app is backgrounded
        sessionManager.endAndSyncSession()
    }

    private fun setupDrawerMenu() {
        findViewById<TextView>(R.id.menuQuiz).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            navController.navigate(R.id.quizListFragment)
        }

        findViewById<TextView>(R.id.menuCpdInfo).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            navController.navigate(R.id.resourcesFrontFragment)
        }

        findViewById<TextView>(R.id.menuPolicies).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            navController.navigate(R.id.policiesFrontFragment)
        }

        findViewById<TextView>(R.id.menuReset).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            navController.navigate(R.id.ResetPasswordFragment)
        }

        findViewById<TextView>(R.id.menuLogOut).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)

            // Sync remaining tracking logs and clear saved user preferences
            sessionManager.clearSession()

            navController.navigate(R.id.loginFragment)
        }
    }

    // Helper method so fragments can open the drawer via a hamburger icon
    fun openMenu() {
        if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }
}