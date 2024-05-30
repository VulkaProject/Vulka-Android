package io.github.vulka.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.google.gson.Gson
import dev.medzik.android.components.LoadingButton
import dev.medzik.android.components.rememberMutableBoolean
import dev.medzik.android.components.rememberMutableString
import dev.medzik.android.utils.runOnIOThread
import dev.medzik.android.utils.runOnUiThread
import io.github.vulka.core.api.Platform
import io.github.vulka.core.api.RequestData
import io.github.vulka.database.Credentials
import io.github.vulka.impl.librus.LibrusLoginClient
import io.github.vulka.impl.librus.LibrusLoginData
import io.github.vulka.impl.librus.LibrusLoginResponse
import io.github.vulka.impl.vulcan.VulcanLoginClient
import io.github.vulka.impl.vulcan.VulcanLoginResponse
import io.github.vulka.ui.R
import io.github.vulka.ui.VulkaViewModel
import io.github.vulka.ui.common.TextInputField
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

    var requestData: RequestData? by remember { mutableStateOf(null) }

    var loading by rememberMutableBoolean()

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

                    var symbol by rememberMutableString()
                    var token by rememberMutableString()
                    var pin by rememberMutableString()

                    TextInputField(
                        label = stringResource(R.string.Field_Symbol),
                        value = symbol,
                        onValueChange = { symbol = it },
                    )

                    TextInputField(
                        label = stringResource(R.string.Field_Token),
                        value = token,
                        onValueChange = { token = it },
                    )

                    TextInputField(
                        label = stringResource(R.string.Field_Pin),
                        value = pin,
                        onValueChange = { pin = it },
                    )

//           try {
//                val keystore = HebeKeystore.create(context,"key2","","Vulca ALPHA")
//                val loginData = VulcanLoginData(
//                    symbol = "powiatstrzelecki",
//                    token = "3S1FANN",
//                    pin = "149167",
//                    keystore = keystore
//                )
//                val loginClient = VulcanLoginClient()
//                val session = loginClient.login(loginData)
//
//                x = session.account!!.userLogin
//            } catch (e: Exception) {
//                x = "Error " + e.message
//                e.printStackTrace()
//            }
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
                if (requestData != null) {
                    runOnIOThread {
                        loading = true

                        try {
                            val response = client.login(requestData!!)

                            println(Gson().toJson(response))

                            val credentials = Credentials(
                                platform = args.platform,
                                data = Gson().toJson(
                                    when (response) {
                                        is VulcanLoginResponse -> TODO()
                                        is LibrusLoginResponse -> response.cookies
                                        else -> throw IllegalStateException()
                                    }
                                )
                            )

                            viewModel.credentialRepository.insert(credentials)

                            runOnUiThread {
                                navController.navigate(
                                    Home(
                                        platform = credentials.platform,
                                        userId = credentials.id.toString(),
                                        credentials = credentials.data
                                    )
                                ) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                        inclusive = true
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        loading = false
                    }
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
