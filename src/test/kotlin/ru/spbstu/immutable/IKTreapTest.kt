package ru.spbstu.immutable

import kotlin.test.Test

class IKTreapTest {
    @Test
    fun smokeTest() {
        val kk = ikTreapOf(1,2,3,4,5,6)
        println(kk)
        println(kk + kk)
    }
}