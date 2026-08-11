package com.saariuslystoned.mbux.platform.handoff

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import com.saariuslystoned.mbux.domain.handoff.HandoffRoute
import com.saariuslystoned.mbux.domain.handoff.TaskHandoffBuilder
import com.saariuslystoned.mbux.domain.handoff.TaskHandoffDraft
import com.saariuslystoned.mbux.domain.handoff.TaskHandoffEndpoint

/**
 * Opens only user-reviewed drafts. This boundary does not observe a chooser result or claim that
 * an external app created or sent anything.
 */
class AndroidTaskHandoffLauncher(private val activity: Activity) {
    fun openAndroidChooser(draft: TaskHandoffDraft): Boolean {
        if (!draft.isReady) return false
        val endpoint = TaskHandoffBuilder.endpointFor(draft, HandoffRoute.ANDROID_CHOOSER)
            as? TaskHandoffEndpoint.AndroidChooser ?: return false
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, endpoint.plainText)
        }
        return launch(Intent.createChooser(sendIntent, "Continue in another app"))
    }

    fun openClaudeCodeDraft(draft: TaskHandoffDraft): Boolean {
        if (!draft.isReady) return false
        val endpoint = TaskHandoffBuilder.endpointFor(draft, HandoffRoute.CLAUDE_CODE_DRAFT)
            as? TaskHandoffEndpoint.ClaudeCodeDraft ?: return false
        return launch(Intent(Intent.ACTION_VIEW, Uri.parse(endpoint.uri)))
    }

    private fun launch(intent: Intent): Boolean = try {
        activity.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
