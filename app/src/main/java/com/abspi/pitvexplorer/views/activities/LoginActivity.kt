package com.abspi.pitvexplorer.views.activities

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.abspi.pitvexplorer.R
import com.abspi.pitvexplorer.utils.PreferenceManager
import com.abspi.pitvexplorer.viewmodels.LoginViewModel
import com.abspi.pitvexplorer.viewmodels.LoginViewModelFactory

class LoginActivity : FragmentActivity() {

    private lateinit var viewModel: LoginViewModel
    private lateinit var serverIpEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize PreferenceManager and ViewModel
        val preferenceManager = PreferenceManager(applicationContext)
        viewModel = ViewModelProvider(
            this,
            LoginViewModelFactory(preferenceManager)
        )[LoginViewModel::class.java]

        // Initialize UI components
        serverIpEditText = findViewById(R.id.serverIpEditText)
        loginButton = findViewById(R.id.loginButton)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)

        // Load saved server IP if available
        viewModel.getLastServerIp()?.let { savedIp ->
            serverIpEditText.setText(savedIp)
            // Optionally select all text so user can easily replace it
            serverIpEditText.selectAll()
        }

        // Setup button click listener
        loginButton.setOnClickListener {
            hideKeyboard()
            connectToServer()
        }

        // Setup keyboard action listener for the EditText
        serverIpEditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                connectToServer()
                return@setOnKeyListener true
            }
            false
        }

        // Handle keyboard Enter key press
        serverIpEditText.setOnEditorActionListener { _, actionId, event ->
            if (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER ||
                event != null && event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                // Hide keyboard
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(serverIpEditText.windowToken, 0)
                // Connect to server
                connectToServer()
                return@setOnEditorActionListener true
            }
            false
        }

        // Explicitly request focus and show keyboard when the EditText is clicked
        serverIpEditText.setOnClickListener {
            it.requestFocus()
            showKeyboard(it)
        }

        // Request focus on EditText and show keyboard when activity starts
        serverIpEditText.post {
            serverIpEditText.requestFocus()
            showKeyboard(serverIpEditText)
        }

        // Observe loading state
        viewModel.isLoading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            loginButton.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
            serverIpEditText.isEnabled = !isLoading
        }

        // Observe error messages
        viewModel.error.observe(this) { errorMessage ->
            errorText.visibility = if (errorMessage != null) View.VISIBLE else View.GONE
            errorText.text = errorMessage
        }
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun connectToServer() {
        // Hide keyboard when connecting
        hideKeyboard()

        val serverIp = serverIpEditText.text.toString().trim()
        if (serverIp.isEmpty()) {
            errorText.visibility = View.VISIBLE
            errorText.text = getString(R.string.error_empty_server_ip)
            return
        }

        viewModel.login(serverIp).observe(this) { success ->
            if (success) {
                // Navigate to file browser activity
                val intent = Intent(this, FileBrowserActivity::class.java).apply {
                    putExtra("SERVER_IP", serverIp)
                }
                startActivity(intent)
                finish()
            }
        }
    }
}