package com.lfr.baozi

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lfr.baozi.data.PreferencesRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDefaultsSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun defaultsShownInSettings() {
        // Ensure a deterministic starting state (device may already be logged in).
        runBlocking {
            val prefs = PreferencesRepository(composeRule.activity)
            prefs.setLoggedIn(false)
            prefs.saveInviteCode("")
            prefs.clearCustomBackendOverrides()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("login_invite").performTextInput("test")
        composeRule.onNodeWithTag("login_button").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("drawer_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_settings").performClick()

        // Model settings is now a sub-page under Settings
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_item_model").performClick()

        composeRule.onNodeWithTag("settings_modelName").assertTextContains("autoglm-phone-9b")

        composeRule.onNodeWithTag("settings_item_backend").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_custom_baseUrl").assertTextContains("")
        composeRule.onNodeWithTag("settings_custom_apiKey").assertTextContains("")
    }
}
