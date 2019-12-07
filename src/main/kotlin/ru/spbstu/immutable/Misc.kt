package ru.spbstu.immutable

inline fun <T> repeat(times: Int, initial: T, transform: (T) -> T): T {
    var current = initial
    repeat(times) {
        current = transform(current)
    }
    return current
}
