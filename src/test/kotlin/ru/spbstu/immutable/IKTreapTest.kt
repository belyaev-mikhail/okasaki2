package ru.spbstu.immutable

import kotlin.test.Test
import kotlin.test.assertEquals

class IKTreapTest {
    @Test
    fun smokeTest() {
        val kk = ikTreapOf(1,2,3,4,5,6)
        assertEquals(listOf<String>(), ikTreapOf<String>())
        assertEquals((1..6).toList(), kk)
        assertEquals((1..6).toList(), kk.toList())
        assertEquals((1..7).toList(), kk + 7)
        assertEquals((1..6) + (1..6), kk + kk)

        assertEquals((1..6).toList(), kk - 7)
        assertEquals((1..5).toList(), kk - 6)
        assertEquals((2..6).toList(), kk - 1)
        assertEquals((1..3) + (5..6), kk - 4)

    }
}