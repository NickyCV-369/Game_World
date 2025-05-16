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
import android.app.AlarmManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

class MainActivity : AppCompatActivity() {

    private var bottomNav: BottomNavigationView? = null
    private var currentFragment: Fragment? = null
    private var isDialogVisible = false
    private var isNetworkChecked = false
    private var isNetworkRecentlyPlayedChecked = false
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback
    private var selectedFragment: Fragment? = null
    private var alertDialog: AlertDialog? = null
    private lateinit var mAdView: AdView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myApp = application as MyApplication

        myApp.loadAd(this)

        Handler(Looper.getMainLooper()).postDelayed({
            myApp.showAdIfAvailable(this, object : MyApplication.OnShowAdCompleteListener {
                override fun onShowAdComplete() {
                }
            })
        }, 3000)

        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

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
        when (item.itemId) {
            R.id.nav_home -> {
                selectedFragment = supportFragmentManager.findFragmentByTag(HomeFragment::class.java.simpleName)
                if (selectedFragment == null) {
                    selectedFragment = HomeFragment()
                }
            }
            R.id.nav_recently_played_games-> {
                if (!isNetworkRecentlyPlayedChecked) {
                    lifecycleScope.launch {
                        if (isNetworkAvailableAndCanConnectToInternet()) {
                            selectedFragment = supportFragmentManager.findFragmentByTag(RecentlyPlayedGamesFragment::class.java.simpleName)
                            if (selectedFragment == null) {
                                selectedFragment = RecentlyPlayedGamesFragment()
                            }
                            loadFragment(selectedFragment)
                        } else {
                            showNoInternetDialog()
                        }
                    }
                    isNetworkRecentlyPlayedChecked = true
                    return@OnNavigationItemSelectedListener true
                }

                selectedFragment = supportFragmentManager.findFragmentByTag(RecentlyPlayedGamesFragment::class.java.simpleName)
                if (selectedFragment == null) {
                    selectedFragment = RecentlyPlayedGamesFragment()
                }
            }
            R.id.nav_all_games -> {
                if (!isNetworkChecked) {
                    lifecycleScope.launch {
                        if (isNetworkAvailableAndCanConnectToInternet()) {
                            selectedFragment = supportFragmentManager.findFragmentByTag(AllGamesFragment::class.java.simpleName)
                            if (selectedFragment == null) {
                                selectedFragment = AllGamesFragment()
                            }
                            loadFragment(selectedFragment)
                        } else {
                            showNoInternetDialog()
                        }
                    }
                    isNetworkChecked = true
                    return@OnNavigationItemSelectedListener true
                }

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
        mAdView.pause()
        unregisterReceiverSafe()
    }

    override fun onResume() {
        super.onResume()
        mAdView.resume()
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

    fun showAdView() {
        mAdView.visibility = View.VISIBLE
    }

    fun hideAdView() {
        mAdView.visibility = View.GONE
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
        alertDialog = AlertDialog.Builder(this)
            .setTitle("No Internet Connection")
            .setMessage("No internet access. Please check your connection to continue playing.")
            .setCancelable(false)
            .setPositiveButton("Retry") { _, _ ->
                lifecycleScope.launch {
                    if (isNetworkAvailableAndCanConnectToInternet()) {
                        val adRequest = AdRequest.Builder().build()
                        mAdView.loadAd(adRequest)

                        when (bottomNav?.selectedItemId) {
                            R.id.nav_home -> {
                                navListener.onNavigationItemSelected(bottomNav?.menu?.findItem(R.id.nav_home)!!)
                            }
                            R.id.nav_recently_played_games -> {
                                navListener.onNavigationItemSelected(bottomNav?.menu?.findItem(R.id.nav_recently_played_games)!!)
                            }
                            R.id.nav_all_games -> {
                                navListener.onNavigationItemSelected(bottomNav?.menu?.findItem(R.id.nav_all_games)!!)
                            }
                            R.id.nav_settings -> {
                                navListener.onNavigationItemSelected(bottomNav?.menu?.findItem(R.id.nav_settings)!!)
                            }
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Internet is still not available.", Toast.LENGTH_SHORT).show()
                        showNoInternetDialog()
                    }
                }
            }
            .setNegativeButton("Exit") { _, _ -> finish() }
            .show()
    }

    private suspend fun isNetworkAvailableAndCanConnectToInternet(): Boolean = withContext(Dispatchers.IO) {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = cm.activeNetwork ?: return@withContext false
        val capabilities = cm.getNetworkCapabilities(network) ?: return@withContext false
        val isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        if (!isConnected) return@withContext false

        return@withContext canConnectToInternet()
    }

    private fun canConnectToInternet(): Boolean {
        return try {
            val url = URL("https://nhanxu.com/test-internet")
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

                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        lifecycleScope.launch {
                            if (isNetworkAvailableAndCanConnectToInternet()) {
                                Toast.makeText(this@MainActivity, "Internet reconnected!", Toast.LENGTH_SHORT).show()
                                if (!isFinishing) {
                                    val adRequest = AdRequest.Builder().build()
                                    mAdView.loadAd(adRequest)

                                    alertDialog?.dismiss()

                                    when (bottomNav?.selectedItemId) {
                                        R.id.nav_home -> {
                                            navListener.onNavigationItemSelected(bottomNav?.menu?.findItem(R.id.nav_home)!!)
                                        }
                                        R.id.nav_recently_played_games -> {
                                            navListener.onNavigationItemSelected(bottomNav?.menu?.findItem(R.id.nav_recently_played_games)!!)
                                        }
                                        R.id.nav_all_games -> {
                                            navListener.onNavigationItemSelected(bottomNav?.menu?.findItem(R.id.nav_all_games)!!)
                                        }
                                        R.id.nav_settings -> {
                                            navListener.onNavigationItemSelected(bottomNav?.menu?.findItem(R.id.nav_settings)!!)
                                        }
                                    }
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
        mAdView.destroy()

        if (isDialogVisible) {
            unregisterReceiverSafe()
        }

            val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            if (::networkCallback.isInitialized) {
                try {
                    cm.unregisterNetworkCallback(networkCallback)
                } catch (e: IllegalArgumentException) {
                }
            }

        isReceiverRegistered = false
    }
}
