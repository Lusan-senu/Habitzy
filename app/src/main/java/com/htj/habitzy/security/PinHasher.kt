package com.htj.habitzy.security

import java.security.MessageDigest

/** Hashes a PIN using SHA-256 hex so the plaintext is never stored. */
object PinHasher {

    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
