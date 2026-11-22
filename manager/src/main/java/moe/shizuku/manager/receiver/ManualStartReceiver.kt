package moe.shizuku.manager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import moe.shizuku.manager.BuildConfig

class ManualStartReceiver : BroadcastReceiver() {

    companion object {
        /** 与 DailyTask ShizukuManager.EXTRA_REMOTE_ACTIVATION 保持一致 */
        private const val EXTRA_REMOTE_ACTIVATION = "remote_activation"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == "${BuildConfig.APPLICATION_ID}.START" ||
            action == "moe.shizuku.privileged.api.START"
        ) {
            val remoteActivation = intent.getBooleanExtra(EXTRA_REMOTE_ACTIVATION, false)
            ShizukuReceiverStarter.start(
                context,
                forceStart = true,
                remoteActivation = remoteActivation
            )
        }
    }
}
