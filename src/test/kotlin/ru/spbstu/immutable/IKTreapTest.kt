package ru.spbstu.immutable

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class IKTreapTest {
    @Test
    fun smokeTest() {
        fun doIt(seed: Int) = run {
            val kk = ikTreapOf(1, 2, 3, 4, 5, 6, random = Random(seed))
            assertEquals(listOf<String>(), ikTreapOf<String>(random = Random(seed)))
            assertEquals((1..6).toList(), kk)
            assertEquals((1..6).toList(), kk.toList())
            assertEquals((1..7).toList(), kk + 7)
            assertEquals((1..6) + (1..6), kk + kk)

            assertEquals((1..6).toList(), kk - 7)
            assertEquals((1..5).toList(), kk - 6)
            assertEquals((2..6).toList(), kk - 1)
            assertEquals((1..3) + (5..6), kk - 4)

            assertEquals((2..6).toList(), kk.removeAt(0))
            assertEquals((1..5).toList(), kk.removeAt(kk.lastIndex))
            assertEquals((1..3) + (5..6), kk.removeAt(3))

            assertEquals(listOf(1, 2, 3, 7, 4, 5, 6), kk.add(3, 7))
            assertEquals(listOf(1, 2, 3, 7, 8, 9, 4, 5, 6), kk.addAll(3, listOf(7, 8, 9)))
            assertEquals(listOf(1, 2, 1, 2, 3, 4, 5, 6, 3, 4, 5, 6), kk.addAll(2, kk))

            assertEquals((1..9).toList(), kk.addAll(listOf(7, 8, 9)))

            assertEquals(ikTreapOf(1, 2, 3, 99, 5, 6), kk.set(3, 99))
        }
        doIt(0)
        doIt(42)
        doIt(Int.MAX_VALUE - 1)
    }

    @Test
    fun emptiesTest() {
        fun doIt(seed: Int) {
            val e = ikTreapOf<Int>(random = Random(seed))
            val kk = ikTreapOf(1,2,3, random = Random(seed))

            assertEquals(kk, kk + e)
            assertEquals(kk, e + kk)
            assertEquals(e, e - 2)
            assertEquals(e, e + e + e)
        }

        doIt(0)
        doIt(42)
        doIt(Int.MAX_VALUE - 1)
    }
}