package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.dokkani.notifications.NotificationChannels
import com.example.dokkani.notifications.NotificationHelper
import com.example.dokkani.notifications.NotificationWorkScheduler
import com.example.dokkani.ui.AppNavigation
import com.example.dokkani.ui.DokkaniViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    private val viewModel: DokkaniViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d("DokkaniNotifications", "POST_NOTIFICATIONS permission granted")
        } else {
            Log.w("DokkaniNotifications", "POST_NOTIFICATIONS permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. طلب إذن الإشعارات بنظام أندرويد 13+ (POST_NOTIFICATIONS)
        checkAndRequestNotificationPermission()

        // 2. إعداد قنوات الإشعارات بأمان لمنع التوقف
        try {
            NotificationChannels.createNotificationChannels(this)
        } catch (e: Exception) {
            Log.e("DokkaniInit", "Error creating notification channels safely: ${e.message}", e)
        }

        // 3. جدولة المهام التنبيهية الدورية بأمان
        try {
            NotificationWorkScheduler.schedulePeriodicMotivationalWorker(this)
        } catch (e: Exception) {
            Log.e("DokkaniInit", "Error scheduling WorkManager safely: ${e.message}", e)
        }

        // 4. جلب وتحديث FCM Registration Token بأمان ومنع الأخطاء عند غياب Firebase
        try {
            if (FirebaseApp.getApps(this).isNotEmpty()) {
                val app = FirebaseApp.getInstance()
                val projectId = app.options.projectId
                if (!projectId.isNullOrBlank() && projectId != "DEFAULT") {
                    FirebaseMessaging.getInstance().isAutoInitEnabled = true
                    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                        if (task.isSuccessful && task.result != null) {
                            val token = task.result
                            Log.d("DokkaniFCM", "FCM Device Token: $token")
                        } else {
                            Log.w("DokkaniFCM", "FCM token registration skipped or unavailable: ${task.exception?.message}")
                        }
                    }
                } else {
                    Log.i("DokkaniFCM", "FCM auto-init disabled: Default Firebase project credentials not provided.")
                }
            } else {
                Log.i("DokkaniFCM", "FirebaseApp is not initialized (google-services.json missing).")
            }
        } catch (e: Exception) {
            Log.w("DokkaniFCM", "Firebase messaging initialization handled safely: ${e.message}")
        }

        // 5. التوجيه المباشر (Deep Link) إذا تم فتح التطبيق عبر النقر على إشعار
        try {
            handleNotificationDeepLink(intent)
        } catch (e: Exception) {
            Log.e("DokkaniInit", "Error handling deep link safely: ${e.message}", e)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(dokkaniViewModel = viewModel)
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        try {
            handleNotificationDeepLink(intent)
        } catch (e: Exception) {
            Log.e("DokkaniDeepLink", "Error in onNewIntent deep link: ${e.message}", e)
        }
    }

    private fun handleNotificationDeepLink(intent: Intent?) {
        val targetScreen = intent?.getStringExtra(NotificationHelper.EXTRA_TARGET_SCREEN)
        if (!targetScreen.isNullOrBlank()) {
            Log.d("DokkaniDeepLink", "Navigating to deep link target: $targetScreen")
            viewModel.selectTabByScreenKey(targetScreen)
        }
    }
}
