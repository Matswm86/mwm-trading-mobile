package no.mwmai.backtest.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import no.mwmai.backtest.feature.dashboard.DashboardScreen
import no.mwmai.backtest.feature.results.ResultsScreen
import no.mwmai.backtest.feature.run.RunBacktestScreen

object Routes {
    const val DASHBOARD = "dashboard"
    const val RUN = "run"
    const val RESULTS = "results/{jobId}"
    fun results(jobId: Int) = "results/$jobId"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.DASHBOARD) {
        composable(Routes.DASHBOARD) {
            DashboardScreen(onNewBacktest = { nav.navigate(Routes.RUN) })
        }
        composable(Routes.RUN) {
            RunBacktestScreen(
                onBack = { nav.popBackStack() },
                onSubmitted = { jobId ->
                    nav.navigate(Routes.results(jobId)) {
                        popUpTo(Routes.DASHBOARD)
                    }
                },
            )
        }
        composable(
            Routes.RESULTS,
            arguments = listOf(navArgument("jobId") { type = NavType.IntType }),
        ) { entry ->
            ResultsScreen(
                jobId = entry.arguments?.getInt("jobId") ?: 0,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
