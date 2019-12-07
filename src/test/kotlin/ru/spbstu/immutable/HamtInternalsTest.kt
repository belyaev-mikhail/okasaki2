package ru.spbstu.immutable

import ru.spbstu.wheels.IntBits
import ru.spbstu.wheels.TArray
import ru.spbstu.wheels.Zero
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class HamtInternalsTest {

    @Test
    fun testNodes() {
        var nd = HamtElement.Node<Int, Int>(IntBits.Zero, TArray(0))

        nd = nd.set(1, HamtElement.Entry(2, 3))
        nd = nd.set(30, HamtElement.Entry(4, 5))

        assertEquals(HamtElement.Entry(2, 3), nd[1])
        assertEquals(HamtElement.Entry(4, 5), nd[30])

        fun seq(node: HamtElement.Node<Int, Int>) = sequence { node.forEachElement { yield(it) } }

        assertEquals(setOf(HamtElement.Entry(2, 3), HamtElement.Entry(4, 5)), seq(nd).toSet())

        nd = nd.set(30, HamtElement.Entry(6, 7))
        assertEquals(HamtElement.Entry(2, 3), nd[1])
        assertEquals(HamtElement.Entry(6, 7), nd[30])

        var new = HamtElement.Node<Int, Int>(IntBits.Zero, TArray(0))
        for(i in (0..31).shuffled(Random(42))) new = new.set(i, HamtElement.Entry(i, i))

        for(i in 0..31) assertEquals(HamtElement.Entry(i, i), new[i])

        assertEquals((0..31).map { HamtElement.Entry(it, it) }.toSet(), seq(new).toSet())

        new = new.set(21, HamtElement.Entry(0, 0))

        for(i in 0..31) if(i != 21) assertEquals(HamtElement.Entry(i, i), new[i])
        assertEquals(HamtElement.Entry(0, 0), new[21])

        var nullableNew = new ?: null
        for(i in (0..31).shuffled(Random(142))) {
            assertNotEquals(null, nullableNew)
            nullableNew = nullableNew?.remove(i)
            assertEquals(null, nullableNew?.get(i))
        }
        assertEquals(null, nullableNew)

    }

    @Test
    fun testBuckets() {
        val b = HamtElement.Bucket(HamtElement.Entry(3, 4) + SList.Nil)

        assert(b === b + HamtElement.Entry(3, 4))
        assertEquals(HamtElement.Bucket(HamtElement.Entry(3, 6) + SList.Nil), b + HamtElement.Entry(3, 6))

    }

}