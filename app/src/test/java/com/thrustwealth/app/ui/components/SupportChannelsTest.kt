package com.thrustwealth.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SupportChannelsTest {
    @Test
    fun newlyConfiguredChannelsAppearWithoutDisabledOnes() {
        assertEquals(
            listOf("chat", "whatsapp", "telegram", "custom_42"),
            enabledSupportChannels(
                ordered = listOf("chat", "whatsapp", "email"),
                configured = listOf("whatsapp", "chat", "telegram", "custom_42", "telegram")
            )
        )
    }

    @Test
    fun noConfigurationShowsNoChannel() {
        assertEquals(emptyList<String>(), enabledSupportChannels(emptyList(), emptyList()))
    }
}
