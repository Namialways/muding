package com.muding.android.presentation.bridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle

class ForegroundLaunchBridgeActivity : Activity() {

    private var hasLaunchedTarget = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        hasLaunchedTarget = false
    }

    override fun onPostResume() {
        super.onPostResume()
        launchTargetIfNeeded()
    }

    private fun launchTargetIfNeeded() {
        if (hasLaunchedTarget) return
        hasLaunchedTarget = true

        val targetIntent = readTargetIntent()
        if (targetIntent == null) {
            finish()
            overridePendingTransition(0, 0)
            return
        }

        startActivity(targetIntent.apply { addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION) })
        finish()
        overridePendingTransition(0, 0)
    }

    @Suppress("DEPRECATION")
    private fun readTargetIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_TARGET_INTENT, Intent::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_TARGET_INTENT)
        }
    }

    companion object {
        private const val EXTRA_TARGET_INTENT = "extra_target_intent"

        fun createIntent(context: Context, targetIntent: Intent): Intent {
            return Intent(context, ForegroundLaunchBridgeActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION
                )
                putExtra(EXTRA_TARGET_INTENT, targetIntent)
            }
        }
    }
}
