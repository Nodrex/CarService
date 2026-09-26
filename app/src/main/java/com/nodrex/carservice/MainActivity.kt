package com.nodrex.carservice

import com.nodrex.carservice.screens.CarConnectScreen
import com.nodrex.carservice.screens.PermissionsScreen
import com.nodrex.carservice.data.CarDevicePreferences
import com.nodrex.carservice.viewmodel.BluetoothPickerViewModelFactory
import com.nodrex.carservice.viewmodel.BluetoothPickerViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nodrex.carservice.ui.theme.CarServiceTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        installSplashScreen()
        
        val preferences = CarDevicePreferences(applicationContext)
        val factory = BluetoothPickerViewModelFactory(applicationContext, preferences)
        
        enableEdgeToEdge()
        setContent {
            CarServiceTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    var allGranted by androidx.compose.runtime.remember { 
                        androidx.compose.runtime.mutableStateOf(false) 
                    }

                    androidx.compose.foundation.layout.Box(modifier = Modifier.padding(innerPadding)) {
                        if (allGranted) {
                            val viewModel: BluetoothPickerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(factory = factory)
                            CarConnectScreen(viewModel = viewModel)
                        } else {
                            PermissionsScreen(onAllPermissionsGranted = { allGranted = true })
                        }
                    }
                }
            }
        }
    }
}
