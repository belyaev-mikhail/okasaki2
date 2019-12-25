package ru.spbstu.immutable

internal data class Collider(val data: Int) {
    override fun hashCode(): Int = data % 3 // 0, 1 or 2
}
