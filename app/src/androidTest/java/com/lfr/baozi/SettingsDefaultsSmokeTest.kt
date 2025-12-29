package com.lfr.baozi

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.fetchSemanticsNodes
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDefaultsSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun defaultsShownInSettings() {
        // Login if needed (device may already be logged in from previous runs).
        if (composeRule.onAllNodesWithTag("login_invite").fetchSemanticsNodes().isNotEmpty()) {
            composeRule.onNodeWithTag("login_invite").performTextInput("test")
            composeRule.onNodeWithTag("login_button").performClick()
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithTag("drawer_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_settings").performClick()

        // Model settings is now a sub-page under Settings
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_item_model").performClick()

        composeRule.onNodeWithTag("settings_default_baseUrl").assertTextContains("http://47.99.92.117:28100/v1")
        composeRule.onNodeWithTag("settings_modelName").assertTextContains("autoglm-phone-9b")

        composeRule.onNodeWithTag("settings_item_backend").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_custom_baseUrl").assertTextContains("")
        composeRule.onNodeWithTag("settings_custom_apiKey").assertTextContains("")
    }
}
