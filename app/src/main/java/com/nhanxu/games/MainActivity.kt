package com.nhanxu.games

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.content.*
import android.content.pm.PackageManager
import android.net.*
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import android.app.PendingIntent
import android.webkit.*
import android.app.AlarmManager

class MainActivity : AppCompatActivity(), WebViewListener {

    private var bottomNav: BottomNavigationView? = null
    private var currentFragment: Fragment? = null
    private var isDialogVisible = false
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    override fun onUrlChanged(url: String) {
        lifecycleScope.launch {
            if (!isNetworkAvailableAndCanConnectToInternet()) {
                Toast.makeText(this@MainActivity, "No internet connection. Cannot access the game.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        bottomNav?.setOnItemSelectedListener(navListener)

        lifecycleScope.launch {
            if (isNetworkAvailableAndCanConnectToInternet()) {
                if (savedInstanceState == null) {
                    loadFragment(HomeFragment())
                }
            } else {
                showDialogOnUiReady()
                registerReceiver()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            } else {
                scheduleDailyNotification()
            }
        } else {
            scheduleDailyNotification()
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

    override fun onPause() {
        super.onPause()
        unregisterReceiverSafe()
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

    private val networkReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            lifecycleScope.launch {
                if (isNetworkAvailableAndCanConnectToInternet() && isDialogVisible) {
                    Toast.makeText(this@MainActivity, "Internet reconnected!", Toast.LENGTH_SHORT)
                        .show()

                    if (!isFinishing) {
                        recreate()
                    }
                }
            }
        }
    }

    private fun showDialogOnUiReady() {
        window.decorView.post { showNoInternetDialog() }
    }

    private fun showNoInternetDialog() {
        isDialogVisible = true
        AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage("No internet access. Please check your connection to continue playing.")
            .setCancelable(false)
            .setPositiveButton("Retry") { _, _ -> recreate() }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .show()
    }

    private suspend fun isNetworkAvailableAndCanConnectToInternet(): Boolean = withContext(Dispatchers.IO) {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val isConnected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return@withContext false
            val capabilities = cm.getNetworkCapabilities(network) ?: return@withContext false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = cm.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }

        if (!isConnected) return@withContext false

        return@withContext canConnectToInternet()
    }

    private fun canConnectToInternet(): Boolean {
        return try {
            val url = URL("https://www.google.com")
            val urlConnection: HttpURLConnection = url.openConnection() as HttpURLConnection
            urlConnection.apply {
                requestMethod = "GET"
                connectTimeout = 3000
                readTimeout = 3000
            }
            val responseCode = urlConnection.responseCode
            responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) {
            false
        }
    }

    private var isReceiverRegistered = false

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        lifecycleScope.launch {
                            if (isNetworkAvailableAndCanConnectToInternet()) {
                                Toast.makeText(this@MainActivity, "Internet reconnected!", Toast.LENGTH_SHORT).show()
                                if (!isFinishing) {
                                    recreate()
                                }
                            }
                        }
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        lifecycleScope.launch {
                            Toast.makeText(this@MainActivity, "Internet disconnected.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                cm.registerNetworkCallback(
                    NetworkRequest.Builder().build(),
                    networkCallback
                )
            } else {
                val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                registerReceiver(networkReceiver, filter)
            }

            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiverSafe() {
        try {
            unregisterReceiver(networkReceiver)
        } catch (_: Exception) {
        }
    }

    private fun scheduleDailyNotification() {
        val intent = Intent(this, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intervalMillis = 24 * 60 * 60 * 1000L // 24 giờ
        val triggerAtMillis = System.currentTimeMillis() + intervalMillis

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            intervalMillis,
            pendingIntent
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isDialogVisible) {
            unregisterReceiverSafe()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            if (::networkCallback.isInitialized) {
                try {
                    cm.unregisterNetworkCallback(networkCallback)
                } catch (e: IllegalArgumentException) {
                }
            }
        }

        isReceiverRegistered = false
    }
}
