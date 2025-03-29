package com.abspi.pitvexplorer

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.abspi.pitvexplorer.utils.PreferenceManager
import com.abspi.pitvexplorer.views.activities.LoginActivity
import com.abspi.pitvexplorer.views.activities.FileBrowserActivity

/**
 * Main entry point for the PiTV Explorer app.
 * Checks for saved server IP and redirects to login or file browser accordingly.
 */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check for cached server IP
        val preferenceManager = PreferenceManager(applicationContext)
        val savedServerIp = preferenceManager.getLastServerIp()

        if (savedServerIp != null) {
            // If we have a cached server IP, try to go directly to file browser
            val intent = Intent(this, FileBrowserActivity::class.java).apply {
                putExtra("SERVER_IP", savedServerIp)
            }
            startActivity(intent)
        } else {
            // Otherwise, show the login screen
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }

        // Close MainActivity as we're redirecting
        finish()
    }
}