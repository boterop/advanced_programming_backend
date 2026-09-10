package com.project.advanced.project.domain.valueobject

import com.project.advanced.project.domain.exception.ReglaDominioException
import com.project.advanced.project.domain.valueobject.Estancia
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.time.LocalDate

@SpringBootTest
class EstanciaTest {
    private val startDate = LocalDate.now()
    private val endDate = startDate.plusDays(10)
    private val estancia = Estancia(startDate, endDate)

    @Nested
    inner class Solapa {
        @Test
        fun `should create a new estancia with valid dates`() {
            assertDoesNotThrow({ Estancia(startDate, endDate) })
        }

        @Test
        fun `shouldn't create a new estancia with same dates`() {
            assertThrows(ReglaDominioException::class.java, { Estancia(startDate, startDate) })
        }

        @Test
        fun `should throw an exception if the start date is after the end date`() {
            val endDate2 = startDate.minusDays(1)

            assertThrows(ReglaDominioException::class.java, { Estancia(startDate, endDate2) })
        }

        @Test
        fun `should return true if the end date is overlaping`() {
            val startDate2 = startDate.minusDays(3)
            val endDate2 = startDate.plusDays(1)
            val isOverlaping = estancia.solapa(startDate2, endDate2)

            assertTrue(isOverlaping)
        }

        @Test
        fun `should return true if both dates are overlaping`() {
            val startDate2 = startDate.plusDays(1)
            val endDate2 = endDate.minusDays(1)
            val isOverlaping = estancia.solapa(startDate2, endDate2)

            assertTrue(isOverlaping)
        }

        @Test
        fun `should return true if the entire first estancia is overlaping`() {
            val startDate2 = startDate.minusDays(1)
            val endDate2 = endDate.plusDays(1)
            val isOverlaping = estancia.solapa(startDate2, endDate2)

            assertTrue(isOverlaping)
        }

        @Test
        fun `shouldn't overlap two consecutive 1-2 estancias`() {
            val startDate2 = endDate
            val endDate2 = startDate2.plusDays(1)
            val estancia2 = Estancia(startDate2, endDate2)
            val isOverlaping = estancia.solapa(estancia2)

            assertFalse(isOverlaping)
        }

        @Test
        fun `shouldn't overlap two consecutive 2-1 dates`() {
            val startDate2 = startDate.minusDays(3)
            val endDate2 = startDate

            val isOverlaping = estancia.solapa(startDate2, endDate2)
            assertFalse(isOverlaping)
        }

        @Test
        fun `shouldn't overlap with two different dates`() {
            val startDate2 = endDate.plusDays(10)
            val endDate2 = startDate2.plusDays(1)
            val isOverlaping = estancia.solapa(startDate2, endDate2)

            assertFalse(isOverlaping)
        }
    }
}
