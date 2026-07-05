package com.securechat.phoenix.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient

/**
 * Certificate pinning configuration for OkHttp.
 * Pins the server's certificate chain to prevent MITM attacks.
 *
 * In production: replace placeholder hashes with actual certificate SHA-256 pins.
 * Get pins with: openssl s_client -connect host:443 | openssl x509 -pubkey | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | base64
 */
object CertificatePinning {

    // Production certificate pins (replace with real values)
    private const val PRODUCTION_HOST = "securechat.example.com"
    private const val PIN_LEAF = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=" // Replace
    private const val PIN_INTERMEDIATE = "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=" // Replace

    /**
     * Create a CertificatePinner for production use.
     */
    fun createPinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add(PRODUCTION_HOST, PIN_LEAF)
            .add(PRODUCTION_HOST, PIN_INTERMEDIATE)
            .build()
    }

    /**
     * Apply certificate pinning to an OkHttpClient builder.
     * Only applies in production (skips for debug/local development).
     */
    fun applyToClient(builder: OkHttpClient.Builder, isDebug: Boolean): OkHttpClient.Builder {
        if (!isDebug) {
            builder.certificatePinner(createPinner())
        }
        return builder
    }
}
