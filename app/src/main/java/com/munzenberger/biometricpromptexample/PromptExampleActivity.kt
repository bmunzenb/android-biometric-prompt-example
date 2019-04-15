package com.munzenberger.biometricpromptexample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import kotlinx.android.synthetic.main.activity_prompt_example.*
import java.util.concurrent.Executors

class PromptExampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_prompt_example)

        showPromptButton.setOnClickListener { showBiometricPrompt() }
    }

    private fun showBiometricPrompt() {

        val executor = Executors.newSingleThreadExecutor()

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                this@PromptExampleActivity.runOnUiThread {
                    Toast.makeText(
                        this@PromptExampleActivity,
                        getString(R.string.biometric_prompt_error, errString, errorCode),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                this@PromptExampleActivity.runOnUiThread {
                    Toast.makeText(
                        this@PromptExampleActivity,
                        getString(R.string.biometric_prompt_succeeded, result.toString()),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onAuthenticationFailed() {
                this@PromptExampleActivity.runOnUiThread {
                    Toast.makeText(
                        this@PromptExampleActivity,
                        getString(R.string.biometric_prompt_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Title")
            .setSubtitle("Subtitle")
            .setDescription("Description")
            .setNegativeButtonText("Negative Button")
            .build()

        BiometricPrompt(this, executor, callback).authenticate(promptInfo)
    }
}
