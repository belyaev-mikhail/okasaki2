package ru.spbstu.immutable

import ru.spbstu.immutable.HamtElement.Entry
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
        var nd = HamtElement.Node<Int, Int>()

        nd = nd.set(1, Entry(2, 3))
        nd = nd.set(30, Entry(4, 5))

        assertEquals(Entry(2, 3), nd[1])
        assertEquals(Entry(4, 5), nd[30])

        fun seq(node: HamtElement.Node<Int, Int>) = sequence { node.forEachElement { yield(it) } }

        assertEquals(setOf(Entry(2, 3), Entry(4, 5)), seq(nd).toSet())

        nd = nd.set(30, Entry(6, 7))
        assertEquals(Entry(2, 3), nd[1])
        assertEquals(Entry(6, 7), nd[30])

        var new = HamtElement.Node<Int, Int>()
        for(i in (0..31).shuffled(Random(42))) new = new.set(i, Entry(i, i))

        for(i in 0..31) assertEquals(Entry(i, i), new[i])

        assertEquals((0..31).map { Entry(it, it) }.toSet(), seq(new).toSet())

        new = new.set(21, Entry(0, 0))

        for(i in 0..31) if(i != 21) assertEquals(Entry(i, i), new[i])
        assertEquals(Entry(0, 0), new[21])

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
        val b = Entry(3, 4)

        assert(b === b + Entry(3, 4))
        assertEquals(Entry(3, 6), b + Entry(3, 6))

        val many = Entry(4, 5) + Entry(1, 2) + Entry(4, 3) + Entry(2, 5)
        assertEquals(3, many.size)
        assertEquals(3, (many - 40)?.size)
        assertEquals(2, (many - 1)?.size)
        assertEquals(
                Entry(4, 3) + Entry(2, 5),
                many - 1
        )
        assertEquals(
                null,
                many.minus(1)?.minus(4)?.minus(2)
        )

    }

    @Test
    fun testElements() {
        var b: HamtElement<Int, Int> = Entry(2, 3)
        b = b.insert(3, 4)
        assert(b.contains(2))
        assert(b.contains(3))
        assertEquals(2, b.size)
        assertEquals(3, b.getValue(2))
        assertEquals(4, b.getValue(3))
        b = b.insert(3, 5)
        assert(b.contains(2))
        assert(b.contains(3))
        assertEquals(2, b.size)
        assertEquals(3, b.getValue(2))
        assertEquals(5, b.getValue(3))
        b = b.insert(30, 30)
        assertEquals(3, b.size)
    }
}