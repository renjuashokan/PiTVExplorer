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

        // Always show the login screen - saved IP will be handled in LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)

        // Close MainActivity as we're redirecting
        finish()
    }
}