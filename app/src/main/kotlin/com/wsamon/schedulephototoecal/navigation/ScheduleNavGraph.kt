package com.wsamon.schedulephototoecal.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wsamon.schedulephototoecal.ScheduleImportViewModel
import com.wsamon.schedulephototoecal.ui.calendars.ManageCalendarsScreen
import com.wsamon.schedulephototoecal.ui.capture.CaptureScreen
import com.wsamon.schedulephototoecal.ui.confirmdate.ConfirmDateScreen
import com.wsamon.schedulephototoecal.ui.processing.ProcessingScreen
import com.wsamon.schedulephototoecal.ui.result.ResultScreen
import com.wsamon.schedulephototoecal.ui.review.ReviewScreen

@Composable
fun ScheduleNavGraph(viewModel: ScheduleImportViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CAPTURE) {
        composable(Routes.CAPTURE) {
            CaptureScreen(
                viewModel = viewModel,
                onImageReady = { navController.navigate(Routes.PROCESSING) },
            )
        }
        composable(Routes.PROCESSING) {
            ProcessingScreen(
                viewModel = viewModel,
                onParsed = {
                    navController.navigate(Routes.REVIEW) {
                        popUpTo(Routes.CAPTURE)
                    }
                },
                onDateGuessed = {
                    navController.navigate(Routes.CONFIRM_DATE) {
                        popUpTo(Routes.CAPTURE)
                    }
                },
                onRetake = {
                    navController.popBackStack(Routes.CAPTURE, inclusive = false)
                },
            )
        }
        composable(Routes.CONFIRM_DATE) {
            ConfirmDateScreen(
                viewModel = viewModel,
                onConfirmed = {
                    navController.navigate(Routes.REVIEW) {
                        popUpTo(Routes.CAPTURE)
                    }
                },
                onCancel = {
                    viewModel.startOver()
                    navController.popBackStack(Routes.CAPTURE, inclusive = false)
                },
            )
        }
        composable(Routes.REVIEW) {
            ReviewScreen(
                viewModel = viewModel,
                onImported = {
                    navController.navigate(Routes.RESULT) {
                        popUpTo(Routes.CAPTURE)
                    }
                },
                onManageCalendars = { navController.navigate(Routes.MANAGE_CALENDARS) },
            )
        }
        composable(Routes.MANAGE_CALENDARS) {
            ManageCalendarsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.RESULT) {
            ResultScreen(
                viewModel = viewModel,
                onImportAnotherWeek = {
                    viewModel.startOver()
                    navController.popBackStack(Routes.CAPTURE, inclusive = false)
                },
            )
        }
    }
}
