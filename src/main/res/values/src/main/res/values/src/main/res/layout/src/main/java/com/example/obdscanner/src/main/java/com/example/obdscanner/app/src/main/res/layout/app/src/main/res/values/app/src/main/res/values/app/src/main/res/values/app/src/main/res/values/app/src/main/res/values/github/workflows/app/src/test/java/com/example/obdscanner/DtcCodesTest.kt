import org.junit.Assert.*
import org.junit.Test

class DtcCodesTest {

    @Test
    fun describe_known_code_case_insensitive() {
        assertEquals("Ratés d'allumage non spécifiés (plusieurs cylindres)", com.example.obdscanner.DtcCodes.describe("p0300"))
        assertTrue(com.example.obdscanner.DtcCodes.isKnown(" P0301 "))
    }

    @Test
    fun unknown_code_returns_fallback() {
        val res = com.example.obdscanner.DtcCodes.describe("P9999")
        assertTrue(res.contains("non disponible"))
    }

    @Test
    fun validation_regex() {
        assertTrue(com.example.obdscanner.DtcCodes.isValidFormat("P0301"))
        assertFalse(com.example.obdscanner.DtcCodes.isValidFormat("X123"))
        assertFalse(com.example.obdscanner.DtcCodes.isValidFormat("P301")) // moins de 4 chiffres
    }
}
