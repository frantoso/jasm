package de.franklisting.fsm

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class HistoryTest {
    @Test
    fun isNoHistory() {
        val history = History.None
        assertThat(history.isHistory).isFalse
        assertThat(history.isDeepHistory).isFalse
    }

    @Test
    fun isHistory() {
        val history = History.H
        assertThat(history.isHistory).isTrue
        assertThat(history.isDeepHistory).isFalse
    }

    @Test
    fun isDeepHistory() {
        val history = History.Hd
        assertThat(history.isHistory).isFalse
        assertThat(history.isDeepHistory).isTrue
    }
}
