package com.finadapt.adaptivefinance.ui.components

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@Composable
fun NotificationPermissionHandler() {
    val context = LocalContext.current

    //Only Android 13+ (TIRAMISU) requires this runtime permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

        //Check if we ALREADY have permission
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        //The launcher that triggers the Android OS pop-up
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
            if (isGranted) {
                println(" Push Notification Permission Granted!")
            } else {
                println(" Push Notification Permission Denied. User won't get streak warnings.")
            }
        }

        //Trigger the request immediately when this composable enters the screen
        LaunchedEffect(Unit) {
            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}