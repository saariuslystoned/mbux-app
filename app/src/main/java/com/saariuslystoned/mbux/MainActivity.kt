package com.saariuslystoned.mbux

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.saariuslystoned.mbux.platform.audio.AndroidLocalAudioCapture
import com.saariuslystoned.mbux.platform.handoff.AndroidTaskHandoffLauncher
import com.saariuslystoned.mbux.platform.storage.PersistentSessionBoardRepository
import com.saariuslystoned.mbux.platform.storage.SharedPreferencesBoardPreferences
import com.saariuslystoned.mbux.ui.MbuxCompanionApp
import com.saariuslystoned.mbux.ui.VoiceCaptureController
import com.saariuslystoned.mbux.ui.theme.MbuxTheme

/** Phone-only local board, reviewed OS handoff, and microphone. No car or network surface is declared. */
class MainActivity : ComponentActivity() {
    private lateinit var controller: VoiceCaptureController
    private lateinit var boardRepository: PersistentSessionBoardRepository
    private lateinit var handoffLauncher: AndroidTaskHandoffLauncher

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        controller.onPermissionResult(
            granted = granted,
            permanentlyDenied = !granted && !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.RECORD_AUDIO,
            ),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        controller = VoiceCaptureController(
            capture = AndroidLocalAudioCapture(applicationContext),
            initialPermissionGranted = hasMicrophonePermission(),
        )
        boardRepository = PersistentSessionBoardRepository(
            preferences = SharedPreferencesBoardPreferences(
                getSharedPreferences(BOARD_PREFERENCES, MODE_PRIVATE),
            ),
        )
        handoffLauncher = AndroidTaskHandoffLauncher(this)

        setContent {
            MbuxTheme {
                MbuxCompanionApp(
                    controller = controller,
                    boardRepository = boardRepository,
                    onRequestPermission = ::requestMicrophonePermission,
                    onOpenSettings = ::openAppSettings,
                    onContinueHandoff = handoffLauncher::openAndroidChooser,
                    onOpenClaudeCodeDraft = handoffLauncher::openClaudeCodeDraft,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::controller.isInitialized) controller.syncPermission(hasMicrophonePermission())
    }

    override fun onStop() {
        if (::controller.isInitialized) controller.onHostStopped()
        super.onStop()
    }

    override fun onDestroy() {
        if (::controller.isInitialized) controller.close()
        super.onDestroy()
    }

    private fun requestMicrophonePermission() {
        if (hasMicrophonePermission()) {
            controller.onPermissionResult(granted = true, permanentlyDenied = false)
        } else if (controller.beginPermissionRequest()) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasMicrophonePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null),
            ),
        )
    }

    private companion object {
        const val BOARD_PREFERENCES = "personal_session_board"
    }
}
