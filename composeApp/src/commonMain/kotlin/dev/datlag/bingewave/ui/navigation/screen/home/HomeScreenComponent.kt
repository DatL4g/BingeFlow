package dev.datlag.bingewave.ui.navigation.screen.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import com.arkivanov.decompose.ComponentContext
import org.kodein.di.DI

class HomeScreenComponent(
    componentContext: ComponentContext,
    override val di: DI
): HomeComponent, ComponentContext by componentContext {

    @Composable
    @NonRestartableComposable
    override fun renderCommon() {
        onRender {
            HomeScreen(this)
        }
    }
}