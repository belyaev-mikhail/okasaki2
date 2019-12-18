package ru.spbstu.immutable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueueTest {
    @Test
    fun smokeTest() {
        var q = immutableQueue<Int>() + 1 + 2 + 3
        assertEquals(1, q.top)
        q = q.pop()
        assertEquals(2, q.top)
        q = q.pop()
        assertEquals(3, q.top)
        q = q.pop()
        assertEquals(null, q.top)
        assertTrue(q.isEmpty())
        assertEquals(q, q.pop())
        assertTrue(q.isEmpty())
        q = q.pop()
        assertEquals(null, q.top)
    }

    @Test
    fun mixedTest() {
        var q = immutableQueue<Int>()
        for(i in 0..40) q = q.push(i)
        assertEquals(0, q.top)
        for(i in 0..25) {
            assertEquals(i, q.top)
            q = q.pop()
        }
        for(i in 41..70) {
            q = q.push(i)
        }
        for(i in 26..50) {
            assertEquals(i, q.top)
            q = q.pop()
        }
        for(i in 71..80) {
            q = q.push(i)
        }
        for(i in 51..80) {
            assertEquals(i, q.top)
            q = q.pop()
        }
        assertTrue(q.isEmpty())
    }
}