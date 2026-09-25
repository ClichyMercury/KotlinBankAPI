package com.kotlinbank.services.mail

import com.kotlinbank.config.AppConfig
import org.slf4j.LoggerFactory

interface MailSender {
    suspend fun sendPasswordReset(email: String, token: String)
}

object LogMailSender : MailSender {

    private val log = LoggerFactory.getLogger(LogMailSender::class.java)

    override suspend fun sendPasswordReset(email: String, token: String) {
        if (AppConfig.isProduction) {
            log.warn("Password reset requested for $email but no mail provider is configured, token not delivered")
        } else {
            log.info("Password reset token for $email: $token")
        }
    }
}

object MailService : MailSender {

    var delegate: MailSender = LogMailSender

    override suspend fun sendPasswordReset(email: String, token: String) =
        delegate.sendPasswordReset(email, token)
}
