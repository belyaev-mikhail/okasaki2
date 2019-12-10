package ru.spbstu.immutable

import kotlinx.warnings.Warnings
import ru.spbstu.wheels.*
import kotlin.math.ceil
import kotlin.math.log2

private const val DIGITS = 5
private const val MAX_DEPTH = 6

@UseExperimental(ExperimentalStdlibApi::class)
private fun log2floor(value: Int) = 31 - value.countLeadingZeroBits()
@UseExperimental(ExperimentalStdlibApi::class)
private fun log2ceil(value: Int) = 32 - (value - 1).countLeadingZeroBits()
private fun isPow32(value: Int) = value.asBits().run { popCount == 1 && numberOfTrailingZeros % 5 == 0 }
private fun log32floor(value: Int) = log2floor(value) / 5
private fun log32ceil(value: Int) = log32floor(value) + if(isPow32(value)) 0 else 1
private fun pow32(value: Int) = 1 shl (value * 5)

private infix fun Int.divCeil(rhv: Int) = (this / rhv) + if(this % rhv != 0) 1 else 0

internal sealed class AmtElement<out E> {
    abstract operator fun iterator(): Iterator<E>

    class Node<out E>(size: Int = Int.SIZE_BITS): AmtElement<E>() {
        private val data: TArray<AmtElement<E>> = TArray(size)

        override fun iterator(): Iterator<E> = iterator<E> {
            for(sub in data) {
                if(null === sub) break
                yieldAll(sub.iterator())
            }
        }
    }
    class Leaf<out E>(size: Int = Int.SIZE_BITS): AmtElement<E>() {
        private val data: TArray<E> = TArray(size)

        override fun iterator(): Iterator<E> = iterator<E> {
            for(e in data) {
                if (null === e) break
                yield(e)
            }
        }
    }
}

typealias Buffer = Array<Any?>

private fun Buffer.immPush(element: Any?) = copyOf(size + 1).also { it[size] = element }

data class Amt<out T>(
        val size: Int = 0,
        val data: Any? = null
) {
    val depth get() = log32ceil(size) - 1 // do this better

    @Suppress(Warnings.UNCHECKED_CAST)
    private inline fun Any?.asEither(): Either<Buffer, T> = when(this) {
        is Array<*> -> Either.left(this as Buffer)
        else -> Either.right(this as T)
    }

    private fun get(data: Buffer, index: IntBits, depth: Int): T = run {
        val actual = index.wordAt(depth, DIGITS).asInt()
        val next = data[actual]
        when (next.asEither()) {

            !is Array<*> -> next as T
            else -> get(next as Buffer, index, depth - 1)
        }
    }

    operator fun get(index: Int): T {
        require(0 <= index && index < size)
        return when(data) {
            is Array<*> -> get(data as Buffer, index.asBits(), depth)
            else -> data as T
        }
    }

    private fun set(data: Buffer, index: IntBits, value: T, depth: Int): Buffer = run {
        val actual = index.wordAt(depth, DIGITS).asInt()
        val sub = data[actual]
        val next: Any? = when (sub) {
            !is Array<*> -> value
            else -> set(sub as Buffer, index, value, depth - 1)
        }
        data.copyOf().also { it[actual] = next }
    }

    fun set(index: Int, value: @UnsafeVariance T): Amt<T> {
        require(0 <= index && index < size)
        return when(data) {
            is Array<*> -> copy(data = set(data as Buffer, index.asBits(), value, depth))
            else -> copy(data = value)
        }
    }

    private fun add(size: Int, data: Buffer, value: T, depth: Int): Buffer = run {
        if(isPow32(size)) return arrayOf<Any?>(data, value) // nothing else we can do

        val lastChildSize = size % pow32(log32floor(size))
        if(lastChildSize == 0) {
            return data.copyOf(data.size + 1).also { it[data.size] = value }
        }

        val lastChild = data[data.lastIndex]
        val sub = when (lastChild) {
            is Array<*> -> {
                add(lastChildSize, lastChild as Buffer, value, depth - 1)
            }
            else -> {
                arrayOf<Any?>(lastChild, value)
            }
        }

        return data.copyOf().also { it[data.lastIndex] = sub }
    }

    fun add(value: @UnsafeVariance T): Amt<T> {
        return when(data) {
            is Array<*> -> copy(data = add(size, data as Buffer, value, depth), size = size + 1)
            else -> copy(data = arrayOf(data, value), size = size + 1)
        }
    }

    private suspend fun SequenceScope<T>.forEach(value: Buffer, body: suspend SequenceScope<@UnsafeVariance T>.(T) -> Unit) {
        for(e in value) when(e) {
            is Array<*> -> forEach(e as Buffer, body)
            else -> body(e as T)
        }
    }

    private suspend fun SequenceScope<T>.forEach(body: suspend SequenceScope<@UnsafeVariance T>.(T) -> Unit) {
        when {
            size == 0 -> return
            data is Array<*> -> forEach(data as Buffer, body)
            else -> body(data as T)
        }
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