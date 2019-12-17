package ru.spbstu.immutable

import kotlinx.warnings.Warnings
import ru.spbstu.wheels.*

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
        value < POW_32_1 -> 0
        value < POW_32_2 -> 1
        else -> 2
    }
    value < POW_32_6 -> when {
        value < POW_32_4 -> 3
        value < POW_32_5 -> 4
        else -> 5
    }
    else -> 6
}

private fun log32ceil(value: Int) = when {
    value > POW_32_3 -> when {
        value > POW_32_5 -> 6
        value > POW_32_4 -> 5
        else -> 4
    }
    value > POW_32_0 -> when {
        value > POW_32_2 -> 3
        value > POW_32_1 -> 2
        else -> 1
    }
    else -> 0
}

private fun pow32(value: Int) = 1 shl (value * 5)

private inline infix fun Int.divCeil(that: Int) = (this / that) + if(this % that == 0) 0 else 1

private inline fun boundCheck(condition: Boolean, message: () -> String) {
    if (!condition) throw IndexOutOfBoundsException(message())
}

typealias Buffer = Array<Any?>

private fun Buffer.immPush(element: Any?) = copyOf(size + 1).also { it[size] = element }

private data class Amt<out T, UnionType> internal constructor(
        val size: Int,
        val data: UnionType?
) {
    constructor() : this(0, null)

    private val depth get() = log32ceil(size) - 1 // do this better

    @Suppress(Warnings.UNCHECKED_CAST, Warnings.NOTHING_TO_INLINE)
    private inline fun <R> UnionType?.visit(onArray: (Array<UnionType?>) -> R,
                                            onValue: (T) -> R): R = when (this) {
        is Array<*> -> onArray(this as Array<UnionType?>)
        else -> onValue(this as T)
    }

    @Suppress(Warnings.UNCHECKED_CAST, Warnings.NOTHING_TO_INLINE)
    private inline fun UnionType?.visit(onArray: (Array<UnionType?>) -> T): T = when (this) {
        is Array<*> -> onArray(this as Array<UnionType?>)
        else -> this as T
    }

    @Suppress(Warnings.UNCHECKED_CAST, Warnings.NOTHING_TO_INLINE)
    private inline fun uarray(vararg elements: UnionType) = elements as Array<UnionType?> as UnionType

    @Suppress(Warnings.UNCHECKED_CAST, Warnings.NOTHING_TO_INLINE)
    private inline fun T.asUnion() = this as UnionType

    @Suppress(Warnings.UNCHECKED_CAST, Warnings.NOTHING_TO_INLINE)
    private inline fun Array<UnionType?>.asUnion() = this as UnionType

    private fun get(data: Array<UnionType?>, index: IntBits, depth: Int): T = run {
        val actual = index.wordAt(depth, DIGITS).asInt()
        val next = data[actual]
        next.visit(
                onArray = { get(it, index, depth - 1) },
                onValue = { if (depth == 0) it else get(data, index, 0) }
        )
    }

    operator fun get(index: Int): T = run {
        boundCheck(0 <= index && index < size) { "get($index)" }
        data.visit { get(it, index.asBits(), depth) }
    }

    private fun set(data: Array<UnionType?>, index: IntBits, value: T, depth: Int): UnionType = run {
        val actual = index.wordAt(depth, DIGITS).asInt()
        val sub = data[actual]
        val next = sub.visit({ set(it, index, value, depth - 1) }, { it.asUnion() })
        data.copyOf().also { it[actual] = next }.asUnion()
    }

    fun set(index: Int, value: @UnsafeVariance T): Amt<T, UnionType> {
        boundCheck(0 <= index && index < size) { "set($index)" }
        val newData = data.visit(
                onArray = { set(it, index.asBits(), value, depth) },
                onValue = { it.asUnion() }
        )
        return copy(data = newData)
    }

    private fun add(size: Int, data: Array<UnionType?>, value: T, depth: Int): UnionType = run {
        if (isPow32(size)) return uarray(data.asUnion(), value.asUnion()) // nothing else we can do

        val lastChildSize = size % pow32(log32floor(size))
        if (lastChildSize == 0) {
            return data.copyOf(data.size + 1).also { it[data.size] = value.asUnion() }.asUnion()
        }

        val lastChild = data[data.lastIndex]
        val sub = lastChild.visit(
                onArray = { add(lastChildSize, it, value, depth - 1) },
                onValue = { uarray(it.asUnion(), value.asUnion()) }
        )
        return data.copyOf().also { it[data.lastIndex] = sub }.asUnion()
    }

    fun add(value: @UnsafeVariance T): Amt<T, UnionType> {
        val newData = data.visit(
                onArray = { add(size, it, value, depth) },
                onValue = { uarray(it.asUnion(), value.asUnion()) }
        )
        return copy(data = newData, size = size + 1);
    }

    fun allocateEmpty(newSize: Int): UnionType? = when {
        newSize == 1 -> null
        isPow32(newSize) -> Array<Any?>(32) { allocateEmpty(newSize / 32) } as UnionType
        else -> {
            val chunkSize = pow32(log32floor(size))
            val fullChunks = newSize / chunkSize
            val tail = newSize % chunkSize
            val arr = arrayOfNulls<Any?>(fullChunks + if(tail != 0) 1 else 0)
            for (i in 0 until fullChunks) arr[i] = allocateEmpty(chunkSize)
            if(tail != 0) arr[arr.lastIndex] = allocateEmpty(tail)
            arr as UnionType
        }
    }

    fun allocate(newSize: Int, size: Int, data: Array<UnionType?>, depth: Int): UnionType = run {
        if(newSize == size) return data.asUnion()

        val upper = pow32(log32ceil(size))
        if(newSize >= upper) {
            val adaptedSize = minOf(upper * 32, newSize)
            val fullChunks = adaptedSize / upper
            val tail = adaptedSize % upper
            val roundup = allocate(upper, size, data, depth)
            val up = arrayOfNulls<Any?>(fullChunks + if(tail != 0) 1 else 0)
            up[0] = roundup
            for(i in 1 until fullChunks) up[i] = allocateEmpty(upper)
            if(tail != 0) up[up.lastIndex] = allocateEmpty(tail)
            allocate(newSize, adaptedSize, up as Array<UnionType?>, depth)
        } else {
            for(i in 0..data.lastIndex) {
                TODO()
            }
            TODO()
        }

    }

    private suspend fun SequenceScope<T>.forEach(value: Array<UnionType?>, body: suspend SequenceScope<@UnsafeVariance T>.(T) -> Unit) {
        for (e in value) e.visit(
                { forEach(it, body) },
                { body(it) }
        )
    }

    private suspend fun SequenceScope<T>.forEach(body: suspend SequenceScope<@UnsafeVariance T>.(T) -> Unit) {
        if (size == 0) return
        data.visit(
                { forEach(it, body) },
                { body(it) }
        )
    }

    operator fun iterator() = iterator<T> {
        forEach { yield(it) }
    }

}

private typealias Amtt<T> = Amt<T, Any?>

fun main() {
    val a = Amt<Int, Any?>(1, 2)

    val b = a.add(3)

    val c = b.add(4)

    println(c)
    var d = c
    for (i in 5..34) d = d.add(i)

    val e = d.add(35)

    var f = e
    for (i in 36..1028) f = f.add(i)

    println(f)

    check(f.iterator().asSequence().toList() == (2..1028).toList())
    check((0 until f.size).map { f[it] } == (2..1028).toList())
}