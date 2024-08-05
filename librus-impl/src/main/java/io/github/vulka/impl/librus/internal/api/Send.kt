package io.github.vulka.impl.librus.internal.api

import io.github.vulka.impl.librus.LibrusUserClient
import io.github.vulka.impl.librus.applyCookie
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

internal suspend inline fun <reified T> LibrusUserClient.apiGET(
    endpoint: String
): T {
    val response = client.get("https://synergia.librus.pl/gateway/api/2.0/$endpoint") {
        credentials.cookies.forEach {
            applyCookie(it)
        }
    }
    val body = response.bodyAsText()
    return json.decodeFromString(body)
}
