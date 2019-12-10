package ru.spbstu.immutable

import kotlinx.warnings.Warnings
import ru.spbstu.wheels.*
import kotlin.math.ceil
import kotlin.math.log2

private const val DIGITS = 5
private const val MAX_DEPTH = 6

const val POW_32_0 = 1
const val POW_32_1 = 32
const val POW_32_2 = POW_32_1 * 32
const val POW_32_3 = POW_32_2 * 32
const val POW_32_4 = POW_32_3 * 32
const val POW_32_5 = POW_32_4 * 32
const val POW_32_6 = POW_32_5 * 32

private fun isPow32(value: Int) = value.asBits().run { popCount == 1 && numberOfTrailingZeros % 5 == 0 }
private fun log32floor(value: Int) = when {
    value < POW_32_3 -> when {
        value < POW_32_1 -> POW_32_0
        value < POW_32_2 -> POW_32_1
        else -> POW_32_2
    }
    value < POW_32_6 -> when {
        value < POW_32_4 -> POW_32_3
        value < POW_32_5 -> POW_32_4
        else -> POW_32_5
    }
    else -> POW_32_6
}
private fun log32ceil(value: Int) = when {
    value > POW_32_3 -> when {
        value > POW_32_5 -> POW_32_6
        value > POW_32_4 -> POW_32_5
        else -> POW_32_4
    }
    value > POW_32_0 -> when {
        value > POW_32_2 -> POW_32_3
        value > POW_32_1 -> POW_32_2
        else -> POW_32_1
    }
    else -> POW_32_0
}
private fun pow32(value: Int) = 1 shl (value * 5)

private inline fun boundCheck(condition: Boolean, message: () -> String) {
    if(!condition) throw IndexOutOfBoundsException(message())
}

typealias Buffer = Array<Any?>

private fun Buffer.immPush(element: Any?) = copyOf(size + 1).also { it[size] = element }

data class Amt<out T>(
        val size: Int = 0,
        val data: Any? = null
) {
    val depth get() = log32ceil(size) - 1 // do this better

    @Suppress(Warnings.UNCHECKED_CAST, Warnings.NOTHING_TO_INLINE)
    private inline fun Any?.asEither(): Either<Buffer, T> = when(this) {
        is Array<*> -> Either.left(this as Buffer)
        else -> Either.right(this as T)
    }

    private fun get(data: Buffer, index: IntBits, depth: Int): T = run {
        val actual = index.wordAt(depth, DIGITS).asInt()
        val next = data[actual]
        next.asEither().mapLeft { get(it, index, depth - 1) }.value
    }

    operator fun get(index: Int): T = run {
        boundCheck(0 <= index && index < size) { "get($index)" }
        data.asEither().mapLeft { get(it, index.asBits(), depth) }.value
    }

    private fun set(data: Buffer, index: IntBits, value: T, depth: Int): Buffer = run {
        val actual = index.wordAt(depth, DIGITS).asInt()
        val sub = data[actual]
        val next = sub.asEither().mapLeft { set(it, index, value, depth - 1) }.value
        data.copyOf().also { it[actual] = next }
    }

    fun set(index: Int, value: @UnsafeVariance T): Amt<T> {
        boundCheck(0 <= index && index < size) { "set($index)" }
        return data.asEither().visit(
                { buffer -> copy(data = set(buffer, index.asBits(), value, depth)) },
                { value -> copy(data = value) }
        )
    }

    private fun add(size: Int, data: Buffer, value: T, depth: Int): Buffer = run {
        if(isPow32(size)) return arrayOf<Any?>(data, value) // nothing else we can do

        val lastChildSize = size % pow32(log32floor(size))
        if(lastChildSize == 0) {
            return data.copyOf(data.size + 1).also { it[data.size] = value }
        }

        val lastChild = data[data.lastIndex]
        val sub = lastChild.asEither().visit(
                { add(lastChildSize, it, value, depth - 1) },
                { arrayOf<Any?>(it, value) }
        )
        return data.copyOf().also { it[data.lastIndex] = sub }
    }

    fun add(value: @UnsafeVariance T): Amt<T> {
        return data.asEither().visit(
                { copy(data = add(size, it, value, depth), size = size + 1) },
                { copy(data = arrayOf<Any?>(it, value), size = size + 1) }
        )
    }

    private suspend fun SequenceScope<T>.forEach(value: Buffer, body: suspend SequenceScope<@UnsafeVariance T>.(T) -> Unit) {
        for(e in value) e.asEither().visit(
                { forEach(it, body) },
                { body(it) }
        )
    }

    private suspend fun SequenceScope<T>.forEach(body: suspend SequenceScope<@UnsafeVariance T>.(T) -> Unit) {
        if(size == 0) return
        data.asEither().visit(
                { forEach(it, body) },
                { body(it) }
        )
    }

    operator fun iterator() = iterator<T> {
        forEach { yield(it) }
    }

}

fun main() {
    val a = Amt<Int>(1, 2)

    val b = a.add(3)

    val c = b.add(4)

    println(c)
    var d = c
    for(i in 5..34) d = d.add(i)

    val e = d.add(35)

    var f = e
    for(i in 36..1028) f = f.add(i)

    println(f)

    for(i in f) println(i)
}