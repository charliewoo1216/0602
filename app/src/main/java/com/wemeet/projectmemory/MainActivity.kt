package com.wemeet.projectmemory

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.core.content.ContextCompat
import com.wemeet.projectmemory.data.model.Project
import com.wemeet.projectmemory.di.AppContainer
import com.wemeet.projectmemory.overlay.OverlayPermissionHelper
import com.wemeet.projectmemory.overlay.OverlayService
import com.wemeet.projectmemory.ui.newproject.NewProjectDialog
import com.wemeet.projectmemory.ui.projectdetail.ProjectDetailScreen
import com.wemeet.projectmemory.ui.projectdetail.ProjectDetailViewModel
import com.wemeet.projectmemory.ui.projectlist.ProjectListScreen
import com.wemeet.projectmemory.ui.projectlist.ProjectListViewModel
import com.wemeet.projectmemory.ui.projectsettings.ProjectSettingsScreen
import com.wemeet.projectmemory.ui.projectsettings.ProjectSettingsViewModel
import com.wemeet.projectmemory.ui.theme.ProjectMemoryTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as ProjectMemoryApplication).container
        setContent {
            ProjectMemoryTheme {
                AppNavHost(container = container)
            }
        }
    }
}

private object Routes {
    const val PROJECT_LIST = "projectList"
    const val PROJECT_DETAIL = "projectDetail/{projectId}"
    const val PROJECT_SETTINGS = "projectSettings/{projectId}"
    fun projectDetail(id: String) = "projectDetail/$id"
    fun projectSettings(id: String) = "projectSettings/$id"
}

@Composable
private fun AppNavHost(container: AppContainer) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showNewProjectDialog by remember { mutableStateOf(false) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    NavHost(navController = navController, startDestination = Routes.PROJECT_LIST) {
        composable(Routes.PROJECT_LIST) {
            val viewModel: ProjectListViewModel = viewModel(
                factory = viewModelFactory { initializer { ProjectListViewModel(container.projectRepository) } },
            )
            ProjectListScreen(
                viewModel = viewModel,
                onProjectClick = { project: Project -> navController.navigate(Routes.projectDetail(project.id)) },
                onCreateProjectClick = { showNewProjectDialog = true },
            )
            if (showNewProjectDialog) {
                NewProjectDialog(
                    onDismiss = { showNewProjectDialog = false },
                    onCreate = { parentUri, name, colorHex, purpose ->
                        coroutineScope.launch {
                            container.projectRepository.createProject(parentUri, name, colorHex, purpose)
                            showNewProjectDialog = false
                        }
                    },
                )
            }
        }

        composable(
            route = Routes.PROJECT_DETAIL,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
            val viewModel: ProjectDetailViewModel = viewModel(
                key = "detail-$projectId",
                factory = viewModelFactory {
                    initializer {
                        ProjectDetailViewModel(
                            projectId = projectId,
                            projectRepository = container.projectRepository,
                            storageManager = container.storageManager,
                            memoRepository = container.memoRepository,
                        )
                    }
                },
            )
            val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission(),
            ) { granted ->
                if (granted) toggleOverlayService(context, projectId, start = true)
            }
            ProjectDetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenSettings = { navController.navigate(Routes.projectSettings(projectId)) },
                onToggleOverlay = { start ->
                    if (!start) {
                        toggleOverlayService(context, projectId, start = false)
                    } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        toggleOverlayService(context, projectId, start = true)
                    } else {
                        recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            )
        }

        composable(
            route = Routes.PROJECT_SETTINGS,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId").orEmpty()
            val viewModel: ProjectSettingsViewModel = viewModel(
                key = "settings-$projectId",
                factory = viewModelFactory {
                    initializer {
                        ProjectSettingsViewModel(
                            projectId = projectId,
                            repository = container.projectRepository,
                            glossaryScanner = container.glossaryScanner,
                        )
                    }
                },
            )
            ProjectSettingsScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

private fun toggleOverlayService(context: android.content.Context, projectId: String, start: Boolean) {
    if (start && !OverlayPermissionHelper.hasOverlayPermission(context)) {
        context.startActivity(OverlayPermissionHelper.permissionRequestIntent(context))
        return
    }
    val intent = Intent(context, OverlayService::class.java).apply {
        putExtra(OverlayService.EXTRA_PROJECT_ID, projectId)
    }
    if (start) context.startForegroundService(intent) else context.stopService(intent)
}
