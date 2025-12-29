package com.lfr.baozi

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
        composeRule.onNodeWithTag("drawer_toggle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("drawer_settings").performClick()

        // Model settings is now a sub-page under Settings
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_item_model").performClick()

        composeRule.onNodeWithTag("settings_apiKey").assertTextContains("")
        composeRule.onNodeWithTag("settings_baseUrl").assertTextContains("http://47.99.92.117:28100/v1")
        composeRule.onNodeWithTag("settings_modelName").assertTextContains("autoglm-phone-9b")
    }
}
