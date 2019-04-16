package com.munzenberger.biometricpromptexample

import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.widget.addTextChangedListener
import kotlinx.android.synthetic.main.activity_crypto_example.*
import java.security.KeyStore
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec

class CryptoExampleActivity : AppCompatActivity() {

    companion object {

        private const val KEY_ALIAS = "CryptoExample"

        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"

        private const val KEY_SIZE = 256
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val MODE = KeyProperties.BLOCK_MODE_CBC
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7

        private const val CIPHER_TRANSFORMATION = "$ALGORITHM/$MODE/$PADDING"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crypto_example)

        plainTextEditText.addTextChangedListener(
            onTextChanged = { text, _, _, _ ->
                encryptButton.isEnabled = !text.isNullOrEmpty()
            }
        )

        encryptButton.setOnClickListener { encrypt() }

        cipherTextView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                decryptButton.isEnabled = !s.isNullOrEmpty()
            }
        })

        decryptButton.setOnClickListener { decrypt() }
    }

    private fun encrypt() {

        val encryptionCipher = getEncryptionCipher(KEY_ALIAS)

        val cryptoObject = BiometricPrompt.CryptoObject(encryptionCipher)

        val executor = Executors.newSingleThreadExecutor()

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                this@CryptoExampleActivity.runOnUiThread {
                    Toast.makeText(
                        this@CryptoExampleActivity,
                        getString(R.string.biometric_prompt_error, errString, errorCode),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {

                val plainText = plainTextEditText.text.toString().toByteArray(Charsets.UTF_8)

                val cipher = result.cryptoObject?.cipher

                cipher?.doFinal(plainText)?.let {

                    this@CryptoExampleActivity.runOnUiThread {
                        ivTextView.text = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
                        cipherTextView.text = Base64.encodeToString(it, Base64.NO_WRAP)
                        plainTextView.text = null
                    }
                }
            }

            override fun onAuthenticationFailed() {
                this@CryptoExampleActivity.runOnUiThread {
                    Toast.makeText(
                        this@CryptoExampleActivity,
                        getString(R.string.biometric_prompt_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Encrypt")
            .setNegativeButtonText("Cancel")
            .build()

        BiometricPrompt(this, executor, callback).authenticate(promptInfo, cryptoObject)
    }

    private fun getEncryptionCipher(alias: String): Cipher {

        val spec = KeyGenParameterSpec
            .Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(KEY_SIZE)
            .setBlockModes(MODE)
            .setEncryptionPaddings(PADDING)
            .setRandomizedEncryptionRequired(true)
            .setUserAuthenticationRequired(true)
            .build()

        val gen = KeyGenerator.getInstance(ALGORITHM, KEYSTORE_PROVIDER)

        gen.init(spec)

        val key = gen.generateKey()

        return Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
    }

    private fun decrypt() {

        val iv = Base64.decode(ivTextView.text.toString(), Base64.NO_WRAP)

        val decryptionCipher = getDecryptionCipher(KEY_ALIAS, iv)

        val cryptoObject = BiometricPrompt.CryptoObject(decryptionCipher)

        val executor = Executors.newSingleThreadExecutor()

        val callback = object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                this@CryptoExampleActivity.runOnUiThread {
                    Toast.makeText(
                        this@CryptoExampleActivity,
                        getString(R.string.biometric_prompt_error, errString, errorCode),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {

                val cipherText = Base64.decode(cipherTextView.text.toString(), Base64.NO_WRAP)

                val cipher = result.cryptoObject?.cipher

                cipher?.doFinal(cipherText)?.let {
                    this@CryptoExampleActivity.runOnUiThread {
                        plainTextView.text = String(it, Charsets.UTF_8)
                    }
                }
            }

            override fun onAuthenticationFailed() {
                this@CryptoExampleActivity.runOnUiThread {
                    Toast.makeText(
                        this@CryptoExampleActivity,
                        getString(R.string.biometric_prompt_failed),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Decrypt")
            .setNegativeButtonText("Cancel")
            .build()

        BiometricPrompt(this, executor, callback).authenticate(promptInfo, cryptoObject)
    }

    private fun getDecryptionCipher(alias: String, iv: ByteArray): Cipher {

        val keystore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }

        val key = keystore.getKey(alias, null)

        return Cipher.getInstance(CIPHER_TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        }
    }
}
