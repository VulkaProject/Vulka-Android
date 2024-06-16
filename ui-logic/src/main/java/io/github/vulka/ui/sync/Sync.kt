package io.github.vulka.ui.sync

import com.google.gson.Gson
import io.github.vulka.core.api.Platform
import io.github.vulka.database.LuckyNumber
import io.github.vulka.impl.librus.LibrusLoginCredentials
import io.github.vulka.impl.librus.LibrusUserClient
import io.github.vulka.impl.vulcan.VulcanLoginCredentials
import io.github.vulka.impl.vulcan.VulcanUserClient
import io.github.vulka.ui.VulkaViewModel
import java.util.Date
import java.util.UUID

suspend fun sync(
    platform: Platform,
    userId: String,
    credentials: String,
    viewModel: VulkaViewModel
) {
    val client = when (platform) {
        Platform.Vulcan -> {
            val loginData = Gson().fromJson(credentials, VulcanLoginCredentials::class.java)
            VulcanUserClient(loginData)
        }
        Platform.Librus -> {
            val loginData = Gson().fromJson(credentials, LibrusLoginCredentials::class.java)
            LibrusUserClient(loginData)
        }
    }

    // Renew Librus credentials
    if (platform == Platform.Librus)
        (client as LibrusUserClient).renewCredentials()

    val student = viewModel.credentialRepository.getById(UUID.fromString(userId))!!.student

    // Sync lucky number
    val luckyNumber = client.getLuckyNumber(student, Date())
    val luckyNumberObject = viewModel.luckyNumberRepository.getByCredentialsId(UUID.fromString(userId))
    if (luckyNumberObject == null) {
        viewModel.luckyNumberRepository.insert(LuckyNumber(
            number = luckyNumber,
            credentialsId = UUID.fromString(userId)
        ))
    } else {
        viewModel.luckyNumberRepository.update(luckyNumberObject.copy(
            number = luckyNumber
        ))
    }

}