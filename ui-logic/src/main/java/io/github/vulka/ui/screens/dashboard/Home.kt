package io.github.vulka.ui.screens.dashboard

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Looks6
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import dev.medzik.android.compose.rememberMutable
import dev.medzik.android.compose.ui.IconBox
import dev.medzik.android.compose.ui.dialog.DialogState
import dev.medzik.android.compose.ui.dialog.PickerDialog
import dev.medzik.android.compose.ui.dialog.rememberDialogState
import dev.medzik.android.utils.runOnIOThread
import io.github.vulka.business.crypto.decryptCredentials
import io.github.vulka.business.sync.sync
import io.github.vulka.core.api.Platform
import io.github.vulka.database.Credentials
import io.github.vulka.ui.R
import io.github.vulka.ui.VulkaViewModel
import io.github.vulka.ui.common.Avatar
import io.github.vulka.ui.common.ErrorDialog
import io.github.vulka.ui.screens.dashboard.more.AccountManager
import io.github.vulka.ui.utils.getInitials
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
class Home(
    val platform: Platform,
    val userId: String,
    val credentials: String,
    val firstSync: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    args: Home,
    navController: NavController,
    viewModel: VulkaViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val pullToRefreshState = rememberPullToRefreshState()

    val credentials = decryptCredentials(args.credentials)

    var bottomSelected: Any by rememberMutable(Start)
    val dbCredentials = viewModel.credentialRepository.getById(UUID.fromString(args.userId))!!
    val student = dbCredentials.student

    val dialogState = rememberDialogState()

    // If data was synchronized when open app
    var wasRefreshed by rememberMutable(false)

    // If data was saved in DB and now can be safely loaded
    var refreshed by rememberMutable(!args.firstSync)

    val snackBarState by rememberMutable(SnackbarHostState())
    val errorDialogState = rememberDialogState()
    var exception: Exception? by rememberMutable(null)

    LaunchedEffect(Unit) {
        if (!wasRefreshed) {
            pullToRefreshState.startRefresh()
            wasRefreshed = true
        }
    }

    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(Unit) {
            runOnIOThread {
                // Sync database
                refreshed = false

                try {
                    sync(
                        context = context,
                        platform = args.platform,
                        userId = UUID.fromString(args.userId),
                        credentials = credentials,
                        student = student
                    )
                } catch (e: Exception) {
                    refreshed = true
                    runOnIOThread {
                        val snackBarResult = snackBarState.showSnackbar(
                            message = "${context.getText(R.string.Error)}: ${e.message}",
                            actionLabel = context.getText(R.string.Details).toString(),
                            duration = SnackbarDuration.Long
                        )
                        when (snackBarResult) {
                            SnackbarResult.ActionPerformed -> {
                                exception = e
                                errorDialogState.show()
                            }
                            else -> {}
                        }
                    }
                }

                pullToRefreshState.endRefresh()
                refreshed = true
            }
        }
    }


    @Composable
    fun BoxScope.pullToRefresh() {
        PullToRefreshContainer(
            modifier = Modifier.align(alignment = Alignment.TopCenter),
            state = pullToRefreshState,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (bottomSelected) {
                            Start -> stringResource(R.string.Home)
                            Grades -> stringResource(R.string.Grades)
                            Attendance -> stringResource(R.string.Grades)
                            Timetable -> stringResource(R.string.Timetable)
                            More -> stringResource(R.string.More)
                            else -> ""
                        }
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Avatar(
                            modifier = Modifier.padding(),
                            text = student.getInitials(),
                            onClick = {
                                dialogState.show()
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackBarState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    actionColor = MaterialTheme.colorScheme.primary
                )
            }
        },
        bottomBar = {
            @Composable
            fun NavigationBarItemLabel(@StringRes resId: Int) {
                Text(
                    text = stringResource(resId),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            NavigationBar {
                NavigationBarItem(
                    icon = {
                        IconBox(Icons.Default.Dashboard)
                    },
                    label = { NavigationBarItemLabel(R.string.Home) },
                    selected = bottomSelected == Start ,
                    onClick = {
                        bottomSelected = Start
                    },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    icon = {
                        IconBox(Icons.Default.Looks6)
                    },
                    label = { NavigationBarItemLabel(R.string.Grades) },
                    selected = bottomSelected == Grades,
                    onClick = {
                        bottomSelected = Grades
                    },
                    alwaysShowLabel = false
                )
                NavigationBarItem(
                    icon = {
                        IconBox(Icons.AutoMirrored.Filled.EventNote)
                    },
                    label = { NavigationBarItemLabel(R.string.Attendance) },
                    selected = bottomSelected == Attendance,
                    onClick = {
                        bottomSelected = Attendance
                    },
                    alwaysShowLabel = false,
                )
                NavigationBarItem(
                    icon = {
                        IconBox(Icons.Default.Backpack)
                    },
                    label = { NavigationBarItemLabel(R.string.Timetable) },
                    selected = bottomSelected == Timetable,
                    onClick = {
                        bottomSelected = Timetable
                    },
                    alwaysShowLabel = false,
                )
                NavigationBarItem(
                    icon = {
                        IconBox(Icons.AutoMirrored.Filled.Notes)
                    },
                    label = { NavigationBarItemLabel(R.string.More) },
                    selected = bottomSelected == More,
                    onClick = {
                        bottomSelected = More
                    },
                    alwaysShowLabel = false
                )
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            modifier = Modifier.padding(innerPadding),
            targetState = bottomSelected,
            label = "bottom navigation"
        ) { target ->
            when (target) {
                Start -> {
                    StartScreen(
                        args = Start(
                            platform = args.platform,
                            userId = args.userId
                        ),
                        pullToRefreshState = pullToRefreshState,
                        pullRefresh = {
                            pullToRefresh()
                        },
                        refreshed = refreshed
                    )
                }
                Grades -> {
                    GradesScreen(
                        args = Grades(
                            platform = args.platform,
                            userId = args.userId
                        ),
                        pullToRefreshState = pullToRefreshState,
                        pullRefresh = {
                            pullToRefresh()
                        },
                        refreshed = refreshed
                    )
                }
                Attendance -> {
                    AttendanceScreen(
                        args = Attendance(
                            platform = args.platform,
                            userId = args.userId,
                            credentials = args.credentials
                        )
                    )
                }
                Timetable -> {
                    TimetableScreen(
                        args = Timetable(
                            platform = args.platform,
                            userId = args.userId,
                            credentials = args.credentials
                        )
                    )
                }
                More -> {
                    MoreScreen(
                        args = More(
                            platform = args.platform,
                            userId = args.userId,
                            credentials = args.credentials
                        )
                    )
                }
            }
        }
    }

    SelectAccount(
        state = dialogState,
        credentials = dbCredentials,
        navController = navController,
        args = args
    )

    ErrorDialog(
        dialogState = errorDialogState,
        error = exception
    )
}

@Composable
fun SelectAccount(
    args: Home,
    state: DialogState,
    credentials: Credentials,
    navController: NavController,
    viewModel: VulkaViewModel = hiltViewModel()
) {
    val students = viewModel.credentialRepository.getAll()

    PickerDialog(
        state = state,
        title = stringResource(R.string.SelectAccount),
        items = students,
        onSelected = {
            navController.navigate(
                Home(
                    userId = it.id.toString(),
                    platform = it.platform,
                    credentials = it.data,
                    firstSync = false
                )
            ) {
                popUpTo(navController.graph.findStartDestination().id) {
                    inclusive = true
                }
            }
        },
        trailing = {
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    state.hide()
                    navController.navigate(AccountManager(
                        userId = args.userId,
                        platform = args.platform
                    ))
                }
            ) {
                Text(text = stringResource(R.string.ManageAccounts))
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(10.dp),
        ) {
            Avatar(
                modifier = if (it.id == credentials.id)
                    Modifier.border(1.dp,MaterialTheme.colorScheme.primary, RoundedCornerShape(50.dp))
                else
                    Modifier,
                text = it.student.getInitials()
            )
            Column(
                modifier = Modifier.padding(horizontal = 10.dp)
            ) {
                if (it.student.isParent) {
                    Text(it.student.parent!!.fullName)
                    Text(
                        text = "${it.student.fullName} - ${stringResource(R.string.Parent)}",
                        fontSize = 12.sp
                    )
                } else {
                    Text(it.student.fullName)
                    Text(
                        text = stringResource(R.string.Student),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}