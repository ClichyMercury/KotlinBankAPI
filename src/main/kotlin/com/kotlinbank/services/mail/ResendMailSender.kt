package com.kotlinbank.services.mail

import com.kotlinbank.config.AppConfig
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

class ResendMailSender(
    engine: HttpClientEngine = CIO.create(),
    private val apiKey: String = AppConfig.Mail.resendApiKey,
    private val from: String = AppConfig.Mail.from,
    private val resetUrlTemplate: String = AppConfig.Mail.resetUrlTemplate
) : MailSender {

    private val log = LoggerFactory.getLogger(ResendMailSender::class.java)

    private val client = HttpClient(engine) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 5_000
        }
        expectSuccess = false
    }

    override suspend fun sendPasswordReset(email: String, token: String) {
        val link = resetUrlTemplate.replace("{token}", token)
        val payload = ResendEmail(
            from = from,
            to = listOf(email),
            subject = "Réinitialisation de votre mot de passe FinSim",
            html = resetHtml(link),
            text = resetText(link)
        )

        val response = runCatching {
            client.post("https://api.resend.com/emails") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        }.getOrElse {
            log.error("Resend call failed for $email: ${it.message}")
            return
        }

        if (!response.status.isSuccess()) {
            log.error("Resend rejected the password reset for $email: ${response.status} ${response.bodyAsText()}")
        }
    }

    fun close() = client.close()

    private fun resetHtml(link: String): String = """
        <!doctype html>
        <html lang="fr">
          <body style="margin:0;padding:24px;background:#f5f6f8;font-family:-apple-system,Segoe UI,Roboto,sans-serif;color:#1a1a1a">
            <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:12px;padding:32px">
              <h1 style="margin:0 0 16px;font-size:20px">Réinitialisation de votre mot de passe</h1>
              <p style="margin:0 0 16px;line-height:1.5">
                Vous avez demandé à réinitialiser le mot de passe de votre compte FinSim.
                Ce lien est valable <strong>${AppConfig.PasswordReset.expirationMinutes} minutes</strong> et ne peut servir qu'une fois.
              </p>
              <p style="margin:0 0 24px">
                <a href="$link" style="display:inline-block;background:#1a7f5a;color:#ffffff;text-decoration:none;padding:12px 20px;border-radius:8px">
                  Choisir un nouveau mot de passe
                </a>
              </p>
              <p style="margin:0 0 8px;font-size:13px;color:#666;line-height:1.5">
                Si le bouton ne fonctionne pas, copiez ce lien dans votre navigateur :<br>
                <span style="word-break:break-all">$link</span>
              </p>
              <p style="margin:24px 0 0;font-size:13px;color:#666;line-height:1.5">
                Vous n'êtes pas à l'origine de cette demande ? Ignorez cet email, votre mot de passe reste inchangé.
              </p>
            </div>
          </body>
        </html>
    """.trimIndent()

    private fun resetText(link: String): String = """
        Réinitialisation de votre mot de passe FinSim

        Vous avez demandé à réinitialiser le mot de passe de votre compte.
        Ce lien est valable ${AppConfig.PasswordReset.expirationMinutes} minutes et ne peut servir qu'une fois :

        $link

        Vous n'êtes pas à l'origine de cette demande ? Ignorez cet email, votre mot de passe reste inchangé.
    """.trimIndent()

    @Serializable
    private data class ResendEmail(
        val from: String,
        val to: List<String>,
        val subject: String,
        val html: String,
        val text: String
    )
}
