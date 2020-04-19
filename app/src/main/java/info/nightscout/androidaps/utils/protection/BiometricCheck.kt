package info.nightscout.androidaps.utils.protection

import androidx.biometric.BiometricConstants
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import info.nightscout.androidaps.R
import info.nightscout.androidaps.utils.ToastUtils
import java.util.concurrent.Executors

object BiometricCheck {
    fun biometricPrompt(activity: FragmentActivity, title: Int, ok: Runnable?, cancel: Runnable? = null, fail: Runnable? = null) {
        val executor = Executors.newSingleThreadExecutor()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricConstants.ERROR_UNABLE_TO_PROCESS,
                    BiometricConstants.ERROR_TIMEOUT,
                    BiometricConstants.ERROR_CANCELED,
                    BiometricConstants.ERROR_LOCKOUT,
                    BiometricConstants.ERROR_VENDOR,
                    BiometricConstants.ERROR_LOCKOUT_PERMANENT,
                    BiometricConstants.ERROR_USER_CANCELED        -> {
                        ToastUtils.showToastInUiThread(activity.baseContext, errString.toString())
                        fail?.run()
                    }

                    BiometricConstants.ERROR_NEGATIVE_BUTTON      ->
                        cancel?.run()

                    BiometricConstants.ERROR_NO_DEVICE_CREDENTIAL ->
                        // call ok, because it's not possible to bypass it when biometrics is setup, hw not present and no pin set
                        ok?.run()

                    BiometricConstants.ERROR_NO_SPACE,
                    BiometricConstants.ERROR_HW_UNAVAILABLE,
                    BiometricConstants.ERROR_HW_NOT_PRESENT,
                    BiometricConstants.ERROR_NO_BIOMETRICS        ->
                        // call ok, because it's not possible to bypass it when biometrics fail
                        // ok?.run()
                        // changed to fail as you can use PIN instead with setDeviceCredentialAllowed enabled
                        fail?.run()
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Called when a biometric is recognized.
                ok?.run()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Called when a biometric is valid but not recognized.
                fail?.run()
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(title))
            .setDescription(activity.getString(R.string.biometric_title))
//            .setNegativeButtonText(activity.getString(R.string.cancel)) // not possible with setDeviceCredentialAllowed
            .setDeviceCredentialAllowed(true)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

}