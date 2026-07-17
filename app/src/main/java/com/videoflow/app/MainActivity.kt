package com.videoflow.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.videoflow.app.ui.PermissionScreen
import com.videoflow.app.ui.VideoScreen
import com.videoflow.app.ui.theme.VideoFlowTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VideoFlowTheme {
                val vm: MainViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}

/** 读取视频所需的权限：Android 13+ 用 READ_MEDIA_VIDEO，更低版本用 READ_EXTERNAL_STORAGE */
fun requiredReadPermission(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

@androidx.compose.runtime.Composable
private fun AppRoot(vm: MainViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var granted by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, requiredReadPermission()
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    androidx.compose.runtime.LaunchedEffect(granted) {
        if (granted) vm.loadVideos()
    }

    if (granted) {
        VideoScreen(viewModel = vm)
    } else {
        PermissionScreen(onGranted = { granted = true })
    }
}
