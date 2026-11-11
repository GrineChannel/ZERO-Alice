package com.zeroproject.alice.navigation

/**
 * Single source of truth for the currently visible application screen.
 *
 * This controller does not own Android views or Activity lifecycle.
 * Screen rendering remains in MainActivity until the next refactoring step.
 */
class AliceNavigation {

    enum class Screen {
        HOME,
        CHAT,
        MODEL,
        ENGINEER
    }

    var currentScreen: Screen = Screen.HOME
        private set

    val isChatOpen: Boolean
        get() = currentScreen == Screen.CHAT

    val isModelScreenOpen: Boolean
        get() = currentScreen == Screen.MODEL

    val isEngineerOpen: Boolean
        get() = currentScreen == Screen.ENGINEER

    val canNavigateBack: Boolean
        get() = currentScreen != Screen.HOME

    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }

    fun navigateHome() {
        currentScreen = Screen.HOME
    }
}
