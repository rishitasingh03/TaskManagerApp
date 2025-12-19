package com.example.taskmanagerapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.taskmanagerapp.ui.theme.TaskManagerAppTheme

//BiometricPrompt requires a FragmentActivity
class MainActivity : FragmentActivity() {

    private lateinit var taskViewModel: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        taskViewModel = ViewModelProvider(
            this,
            TaskViewModelFactory(application)
        )[TaskViewModel::class.java]

        setContent {
            //Tracks whether the user has successfully authenticated.
            var isAuthenticated by rememberSaveable { mutableStateOf(false) }
            //Prevents showing the biometric prompt multiple times
            var biometricShown by rememberSaveable { mutableStateOf(false) }
            //for showing notification
            var showNotificationDialog by rememberSaveable { mutableStateOf(false) }
            //if user not authenticated, task ui not open
            if (!isAuthenticated) {
                LaunchedEffect(Unit) { //compose way to run side effects
                    if (!biometricShown) {
                        biometricShown = true
                        showBiometricPrompt {
                            isAuthenticated = true

                            //  check every time app opens
                            if (!isNotificationPermissionGranted()) {
                                showNotificationDialog = true
                            }
                        }
                    }
                }
            } else {
                TaskManagerAppTheme {
                    TaskManagerApp(taskViewModel)

                    //  Custom dialog shown every time if permission not granted
                    if (showNotificationDialog) {
                        AlertDialog(
                            onDismissRequest = { showNotificationDialog = false },
                            title = { Text("Enable Notifications") },
                            text = {
                                Text(
                                    "Notifications are required for task reminders. " +
                                            "Please enable them from settings."
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    openNotificationSettings()
                                    showNotificationDialog = false
                                }) {
                                    Text("Open Settings")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = {
                                    showNotificationDialog = false
                                }) {
                                    Text("Not Now")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
    //Checks whether the app is allowed to show notifications
    //true- allowed, false- not allowed
    //called before scheduling or relying on notifications
    private fun isNotificationPermissionGranted(): Boolean {
        //this check is required
        // because before android 13 Notification permission was granted automatically
        // and no runtime permission required
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission( //checks if the user has granted
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    //Opens system settings page for this app
    //Allows user to manually enable notifications
    private fun openNotificationSettings() {
        //Open this app’s detail settings screen
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            //Attach app package name
            //Settings app needs to know which app’s settings to open
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)//Launch settings screen
    }

    //biometric logic
    private fun showBiometricPrompt(onSuccess: () -> Unit) {
        //Executor setup
        val executor = ContextCompat.getMainExecutor(this)
        //Create BiometricPrompt
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                //Authentication success callback
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
                //Authentication error / cancel
                //Prevents bypassing authentication
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    finish()
                }
            }
        )
        //prompt configuration
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate")
            .setSubtitle("Verify your identity to access tasks")
            .setNegativeButtonText("Exit") // allow user to exit the app
            .build() //finalizes biometric configuration

        biometricPrompt.authenticate(promptInfo) // launch biometric ui
    }
}
