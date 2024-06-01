package io.github.vulka.ui.screens.auth

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import dev.medzik.android.components.rememberMutableBoolean
import dev.medzik.android.components.rememberMutableString
import dev.medzik.android.components.ui.LoadingButton
import dev.medzik.android.utils.runOnIOThread
import dev.medzik.android.utils.runOnUiThread
import io.github.vulka.core.api.Platform
import io.github.vulka.core.api.RequestData
import io.github.vulka.database.Credentials
import io.github.vulka.impl.librus.LibrusLoginClient
import io.github.vulka.impl.librus.LibrusLoginData
import io.github.vulka.impl.vulcan.VulcanLoginClient
import io.github.vulka.impl.vulcan.VulcanLoginData
import io.github.vulka.impl.vulcan.hebe.login.HebeKeystore
import io.github.vulka.ui.R
import io.github.vulka.ui.VulkaViewModel
import io.github.vulka.ui.common.TextInputField
import io.github.vulka.ui.crypto.serializeCredentials
import io.github.vulka.ui.crypto.serializeCredentialsAndEncrypt
import io.github.vulka.ui.screens.dashboard.Home
import kotlinx.serialization.Serializable

@Serializable
class Login(val platform: Platform)

@Composable
fun LoginScreen(
    args: Login,
    navController: NavController,
    viewModel: VulkaViewModel = hiltViewModel()
) {
    val client = when (args.platform) {
        Platform.Vulcan -> VulcanLoginClient()
        Platform.Librus -> LibrusLoginClient()
    }

    val context = LocalContext.current

    var requestData: RequestData? by remember { mutableStateOf(null) }

    var loading by rememberMutableBoolean()

    var vulcanSymbol by rememberMutableString()
    var vulcanToken by rememberMutableString()
    var vulcanPin by rememberMutableString()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when (args.platform) {
                Platform.Vulcan -> {
                    TextInputField(
                        label = stringResource(R.string.Field_Symbol),
                        value = vulcanSymbol,
                        onValueChange = {
                            vulcanSymbol = it
                        },
                    )

                    TextInputField(
                        label = stringResource(R.string.Field_Token),
                        value = vulcanToken,
                        onValueChange = {
                            vulcanToken = it
                        },
                    )

                    TextInputField(
                        label = stringResource(R.string.Field_Pin),
                        value = vulcanPin,
                        onValueChange = {
                            vulcanPin = it
                        },
                    )
                }

                Platform.Librus -> {
                    var login by rememberMutableString()
                    var password by rememberMutableString()

                    TextInputField(
                        label = stringResource(R.string.Field_Login),
                        value = login,
                        onValueChange = {
                            login = it
                            requestData = LibrusLoginData(
                                login = login,
                                password =  password
                            )
                        },
                    )

                    TextInputField(
                        label = stringResource(R.string.Field_Password),
                        value = password,
                        onValueChange = {
                            password = it
                            requestData = LibrusLoginData(
                                login = login,
                                password =  password
                            )
                        },
                        hidden = true,
                        keyboardType = KeyboardType.Password
                    )
                }
            }
        }

        LoadingButton(
            onClick = {
                runOnIOThread {
                    loading = true

                    // For Vulcan we must create keystore first
                    if (args.platform == Platform.Vulcan) {
                        val keystore = HebeKeystore.create(
                            context = context,
                            alias = HebeKeystore.generateKeystoreName(vulcanSymbol),
                            firebaseToken = "",
                            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL} (Vulka)")

                        requestData = VulcanLoginData(
                            symbol = vulcanSymbol,
                            token = vulcanToken,
                            pin = vulcanPin,
                            keystore = keystore
                        )
                    }

                    try {
                        // Credentials will be encrypted in ChooseStudents screen,
                        // because Vulcan implementation must encrypt credentials for every student,
                        // then can save it to Room database
                        // Currently encrypts only one credential
                        val response = client.login(requestData!!)

                        val data = serializeCredentials(response)

                        runOnUiThread {
                            navController.navigate(ChooseStudents(
                                platform = args.platform,
                                credentialsData = data
                            ))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    loading = false
                }
            },
            loading = loading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(stringResource(R.string.Login))
        }
    }
}
