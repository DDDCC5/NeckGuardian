package com.example.neckguardian.domain
import org.junit.Assert.assertEquals
import org.junit.Test
class DisplayFormatTest {
    @Test fun angleAndGraceValuesRemainDisplayable() {
        assertEquals("0.0",oneDecimal(0.0))
        assertEquals("12.5",oneDecimal(12.5))
        assertEquals("-3.2",oneDecimal(-3.2))
    }
}
