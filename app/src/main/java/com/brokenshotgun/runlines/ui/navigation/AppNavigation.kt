package com.brokenshotgun.runlines.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.brokenshotgun.runlines.data.ScriptReaderDbHelper
import com.brokenshotgun.runlines.ui.screens.HomeScreen
import com.brokenshotgun.runlines.ui.screens.ReadSceneScreen
import com.brokenshotgun.runlines.ui.screens.ScriptListScreen
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute : NavKey {
    @Serializable
    data object Home : AppRoute

    @Serializable
    data object ScriptList : AppRoute

    @Serializable
    data class ReadScene(val scriptId: Long, val sceneIndex: Int) : AppRoute

}

@Composable
fun AppNavigation(
    onAddScript: () -> Unit,
    onImportScript: () -> Unit,
    dbHelper: ScriptReaderDbHelper,
    initialScriptId: Long = -1L,
    initialSceneIndex: Int = 0
) {
    val initialRoute: AppRoute = if (initialScriptId >= 0L) {
        AppRoute.ReadScene(initialScriptId, initialSceneIndex.coerceAtLeast(0))
    } else {
        AppRoute.Home
    }
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(initialRoute)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<AppRoute.Home> {
                HomeScreen(
                    onAddScript = onAddScript,
                    onImportScript = onImportScript,
                    onOpenExisting = { backStack.add(AppRoute.ScriptList) }
                )
            }

            entry<AppRoute.ScriptList> {
                ScriptListScreen(
                    dbHelper = dbHelper,
                    onScriptSelected = { script ->
                        backStack.add(AppRoute.ReadScene(script.id, 0))
                    }
                )
            }

            entry<AppRoute.ReadScene> { key ->
                val script = dbHelper.getScripts().find { it.id == key.scriptId } ?: return@entry
                ReadSceneScreen(
                    script = script,
                    sceneIndex = key.sceneIndex,
                    onBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        }
                    },
                    onSaveScript = { updatedScript ->
                        dbHelper.updateScript(updatedScript)
                    }
                )
            }

        }
    )
}
