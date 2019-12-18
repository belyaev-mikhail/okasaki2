package ru.spbstu.immutable

import org.testng.annotations.Test
import ru.spbstu.wheels.ints
import kotlin.random.Random
import kotlin.test.assertEquals

data class Collider(val data: Int) {
    override fun hashCode(): Int = data % 3 // 0, 1 or 2
}

class HamtSetTest {
    @Test
    fun testSmoke() {
        val a = hamtSetOf(1, 2, 3, 4, 5)

        assertEquals(setOf(1, 2, 3, 4, 5), a)
        assertEquals(5, a.size)
        for(e in 1..5) assert(e in a)

        val b = a + hamtSetOf(6, 7, 8, 9)

        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8, 9), b)
        assertEquals(9, b.size)
        for(e in 1..9) assert(e in b)

        val bb = a.addAll(listOf(6, 7, 8, 9))

        assertEquals(setOf(1, 2, 3, 4, 5, 6, 7, 8, 9), bb)
        assertEquals(9, bb.size)
        for(e in 1..9) assert(e in bb)

        assertEquals(setOf(1, 2, 3, 4, 5), a)

        val c = a + hamtSetOf(4, 5, 6, 7)
        assertEquals(setOf(7, 6, 5, 4, 3, 2, 1), c)
        assertEquals(7, c.size)
        for(e in 1..7) assert(e in c)
    }

    @Test
    fun testCollisions() {
        val a = hamtSetOf(*(1..500).map(::Collider).toTypedArray())

        assertEquals(500, a.size)
        assertEquals((1..500).mapTo(mutableSetOf(), ::Collider) as Set<Collider>, a)
        for(e in 1..500) assert(Collider(e) in a)

        val b = a + hamtSetOf(*(200..600).map(::Collider).toTypedArray())

        for(e in 1..600) assert(Collider(e) in b)

        assertEquals(600, b.size)
        assertEquals((1..600).mapTo(mutableSetOf(), ::Collider) as Set<Collider>, b)
    }

    @Test
    fun testRandomized() {
        for(seed in 0..400) {
            val random = Random(seed)
            val data = random.ints().take(300).toList()

            val hamt = hamtSetOf(*data.toTypedArray())
            assertEquals(data.toSet(), hamt)
            for(e in data) assert(e in hamt)
            for(e in hamt) assertEquals(hamt, hamt - e + e)
        }
    }
}