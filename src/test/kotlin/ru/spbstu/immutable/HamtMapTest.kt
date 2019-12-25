package ru.spbstu.immutable

import org.testng.annotations.Test
import ru.spbstu.wheels.getEntry
import ru.spbstu.wheels.ints
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HamtMapTest {
    @Test
    fun testSmoke() {
        val single = hamtMapOf(40 to "Hello")
        assertEquals(mapOf(40 to "Hello"), single)
        assertEquals(1, single.size)

        val a = hamtMapOf(1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5")

        assertEquals(mapOf(1 to "1", 2 to "2", 3 to "3", 4 to "4", 5 to "5"), a)
        assertEquals(5, a.size)
        for(e in 1..5) assertTrue(e in a)

        val b = a + hamtMapOf(6 to "6", 7 to "7", 8 to "8", 9 to "9")

        assertEquals((1..9).map { it to "$it" }.toMap(), b)
        assertEquals(9, b.size)
        for(e in 1..9) assertTrue(e in b)

        val bb = a.putAll((6..9).map { it to "$it" }.toMap())

        assertEquals((1..9).map { it to "$it" }.toMap(), bb)
        assertEquals(9, bb.size)
        for(e in 1..9) assertTrue(e in bb)

        assertEquals((1..5).map { it to "$it" }.toMap(), a)

        val c = a + hamtMapOf(4 to "4", 5 to "5", 6 to "6", 7 to "7")
        assertEquals((7 downTo 1).map { it to "$it" }.toMap(), c)
        assertEquals(7, c.size)
        for(e in 1..7) assertTrue(e in c)

        assertTrue(600 !in c)
    }

    @Test
    fun testCollisions() {
        val a = hamtMapOf(*(1..500).map { Collider(it) to "$it" }.toTypedArray())

        assertEquals(500, a.size)
        assertEquals((1..500).map { Collider(it) to "$it" }.toMap(), a)
        for(e in 1..500) assertTrue(Collider(e) in a)

        assertTrue(Collider(600) !in a)

        val b = a + hamtMapOf(*(200..600).map { Collider(it) to "$it" }.toTypedArray())

        for(e in 1..600) {
            assertTrue(Collider(e) in b)
            assertTrue(b.getEntry(Collider(e)) in b.entries)
        }

        assertEquals(600, b.size)
        assertEquals((1..600).map { Collider(it) to "$it" }.toMap(), b)

        assertEquals(((1..498) + (500..600)).map { Collider(it) to "$it" }.toMap(), b - Collider(499))
    }

    @Test
    fun testRandomized() {
        for(seed in 0..400) {
            val random = Random(seed)
            val data = random.ints().take(30).map { it to "$it" }.toList()

            val hamt = hamtMapOf(*data.toTypedArray())
            assertEquals(data.toMap(), hamt)
            for((e) in data) assert(e in hamt)
            for((e) in hamt) assertEquals(hamt, hamt - e + hamt.getEntry(e)!!)
        }

        for(seed in 0..40) {
            val random = Random(seed * Short.MAX_VALUE.toInt())
            val data1 = random.ints(100, 0, 80).map { it to "$it" }.toList()
            val data2 = random.ints(100, 0, 80).map { it to "$it" }.toList()

            val hamt1 = hamtMapOf(*data1.toTypedArray())
            val hamt2 = hamtMapOf(*data2.toTypedArray())
            assertEquals(data1.toMap(), hamt1)
            assertEquals(data2.toMap(), hamt2)
            assertEquals(data1.toMap() + data2.toMap(), hamt1 + hamt2)
            assertEquals(hamt1, hamt1 + hamt1)
            assertEquals(hamt1, hamt1 + immutableMapOf())
            assertEquals(hamt1, immutableMapOf<Int, String>() + hamt1)
        }
    }
}