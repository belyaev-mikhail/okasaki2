package ru.spbstu.immutable

import org.testng.annotations.Test
import ru.spbstu.wheels.ints
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HamtSetTest {
    @Test
    fun testSmoke() {
        val single = hamtSetOf(40)
        assertEquals(setOf(40), single)
        assertEquals(1, single.size)

        val a = hamtSetOf(1, 2, 3, 4, 5)

        assertEquals(setOf(1, 2, 3, 4, 5), a)
        assertEquals(5, a.size)
        for(e in 1..5) assertTrue(e in a)

        val b = a + hamtSetOf(6, 7, 8, 9)

        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8, 9), b)
        assertEquals(9, b.size)
        for(e in 1..9) assertTrue(e in b)

        val bb = a.addAll(listOf(6, 7, 8, 9))

        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8, 9), bb)
        assertEquals(9, bb.size)
        for(e in 1..9) assertTrue(e in bb)

        assertEquals(setOf(1, 2, 3, 4, 5), a)

        val c = a + hamtSetOf(4, 5, 6, 7)
        assertEquals(setOf(7, 6, 5, 4, 3, 2, 1), c)
        assertEquals(7, c.size)
        for(e in 1..7) assertTrue(e in c)

        assertTrue(600 !in c)
        assertEquals(c, c - 600)
    }

    @Test
    fun testCollisions() {
        val a = hamtSetOf(*(1..500).map(::Collider).toTypedArray())

        assertEquals(500, a.size)
        assertEquals((1..500).mapTo(mutableSetOf(), ::Collider) as Set<Collider>, a)
        for(e in 1..500) assertTrue(Collider(e) in a)

        assertTrue(Collider(600) !in a)
        assertEquals(a, a - Collider(600))

        val b = a + hamtSetOf(*(200..600).map(::Collider).toTypedArray())

        for(e in 1..600) assertTrue(Collider(e) in b)

        assertEquals(600, b.size)
        assertEquals((1..600).mapTo(mutableSetOf(), ::Collider) as Set<Collider>, b)

        assertEquals(((1..498) + (500..600)).mapTo(mutableSetOf(), ::Collider) as Set<Collider>, b - Collider(499))
    }

    @Test
    fun testRandomized() {
        for(seed in 0..400) {
            val random = Random(seed)
            val data = random.ints().take(30).toList()

            val hamt = hamtSetOf(*data.toTypedArray())
            assertEquals(data.toSet(), hamt)
            for(e in data) assert(e in hamt)
            for(e in hamt) assertEquals(hamt, hamt - e + e)
        }

        for(seed in 0..40) {
            val random = Random(seed * Short.MAX_VALUE.toInt())
            val data1 = random.ints(100, 0, 80).toList()
            val data2 = random.ints(100, 0, 80).toList()

            val hamt1 = hamtSetOf(*data1.toTypedArray())
            val hamt2 = hamtSetOf(*data2.toTypedArray())
            assertEquals(data1.toSet(), hamt1)
            assertEquals(data2.toSet(), hamt2)
            assertEquals(data1.toSet() + data2.toSet(), hamt1 + hamt2)
            assertEquals(data1.toSet() intersect data2.toSet(), hamt1 intersect hamt2)
            assertEquals(hamt1, hamt1 intersect hamt1)
            assertEquals(hamt1, hamt1 + hamt1)
            assertEquals(hamt1, hamt1 + immutableSetOf())
            assertEquals(setOf<Int>(), hamt1 intersect immutableSetOf())
            assertEquals(hamt1, immutableSetOf<Int>() + hamt1)
            assertEquals(setOf<Int>(), immutableSetOf<Int>() intersect hamt1)
        }
    }


}