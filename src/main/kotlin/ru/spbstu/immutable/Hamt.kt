package ru.spbstu.immutable

import kotlinx.warnings.Warnings
import ru.spbstu.wheels.*

private const val DIGITS = 5
private const val MAX_DEPTH = 6

sealed class HamtElement<K, out V> {
    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun hash(key: K): IntBits {
        val h: Int = key.hashCode()
        return (h xor h.ushr(16)).asBits()
    }

    abstract operator fun iterator(): Iterator<Entry<K, V>>
    abstract val size: Int

    class Entry<K, out V>(override val key: K,
                          override val value: V,
                          val nextEntry: Entry<K, V>? = null,
                          override val size: Int = nextEntry?.size ?: 0): HamtElement<K, V>(), Map.Entry<K, V> {
        fun copy(key: K = this.key,
                 value: @UnsafeVariance V = this.value,
                 nextEntry: Entry<K, @UnsafeVariance V>? = this.nextEntry) = Entry(key, value, nextEntry)

        operator fun plus(that: Entry<K, @UnsafeVariance V>): Entry<K, V> {
            if(nextEntry === null) { // shortcut for most common case
                return when (key) {
                    that.key -> when (value) {
                        that.value -> this
                        else -> that
                    }
                    else -> that.copy(nextEntry = this)
                }
            }

            val backPath = mutableListOf<Entry<K, V>>()
            var current = this ?: null
            while(current != null) {
                if(current.key == that.key) {
                    current = current.copy(value = that.value)
                    for(e in backPath) current = e.copy(nextEntry = current)
                    return current!!
                }
                backPath += current
                current = current.nextEntry
            }
            return that.copy(nextEntry = this)
        }

        operator fun minus(key: K): Entry<K, V>? {
            if(nextEntry === null) { // shortcut for most common case
                return when (key) {
                    this.key -> null
                    else -> this
                }
            }

            val backPath = mutableListOf<Entry<K, V>>()
            var current = this ?: null
            while(current != null) {
                if(current.key == key) {
                    current = current.nextEntry
                    break
                }
                backPath += current
                current = current.nextEntry
            }
            for(e in backPath) current = e.copy(nextEntry = current)
            return current!!
        }

        override operator fun iterator(): Iterator<Entry<K, V>> = iterator {
            var current = this@Entry ?: null
            while(current !== null) {
                yield(current!!)
                current = current.nextEntry
            }
        }

        override fun equals(other: Any?): Boolean =
                other is Entry<*, *> && key == other.key && value == other.value
        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + value.hashCode()
            return result
        }

        override fun toString(): String = "$key=$value"
        internal fun rep(): String = iterator().asSequence().joinToString()

    }
    data class Node<K, out V>(val mask: IntBits,
                              private val data: TArray<HamtElement<K, @UnsafeVariance V>>,
                              override val size: Int): HamtElement<K, V>() {
        constructor(): this(IntBits.Zero, TArray(0), 0)

        private fun adjustIndex(index: Int) = mask.slice(toExclusive = index).popCount

        operator fun get(index: Int) = if(mask[index]) data[adjustIndex(index)] else null
        fun set(index: Int, element: HamtElement<K, @UnsafeVariance V>): Node<K, V> {
            require(0 <= index && index < IntBits.SIZE)

            val adjustedIndex = adjustIndex(index)
            if(mask[index]) {
                val existingSize = data[adjustedIndex]?.size ?: 0
                val sizeAdjustment = element.size - existingSize
                return copy(
                        data = data.copyOf().apply { set(adjustedIndex, element) },
                        size = size + sizeAdjustment
                )
            }

            val newMask = mask.set(index)
            val newData = TArray<HamtElement<K, @kotlin.UnsafeVariance V>>(data.size + 1)

            data.copyInto(newData, startIndex = 0, endIndex = adjustedIndex)
            data.copyInto(newData, startIndex = adjustedIndex, destinationOffset = adjustedIndex + 1)
            newData[adjustedIndex] = element
            return Node(newMask, newData, size + element.size)
        }
        fun remove(index: Int): Node<K, V>? {
            require(0 <= index && index < IntBits.SIZE)

            val adjustedIndex = adjustIndex(index)
            if(!mask[index]) return this

            val newMask = mask.clear(index)
            if(newMask == IntBits.Zero) return null

            val removedSize = data[adjustedIndex]?.size ?: 0
            val newData = TArray<HamtElement<K, @kotlin.UnsafeVariance V>>(data.size - 1)

            data.copyInto(newData, startIndex = 0, endIndex = adjustedIndex)
            data.copyInto(newData, startIndex = adjustedIndex + 1, destinationOffset = adjustedIndex)
            return Node(newMask, newData, size - removedSize)
        }

        inline fun forEachElement(body: (HamtElement<K, V>) -> Unit) {
            mask.forEachOneBit { bit ->
                body(get(bit.numberOfLeadingZeros)!!)
            }
        }

        override operator fun iterator(): Iterator<Entry<K, V>> = iterator {
            forEachElement {
                yieldAll(it.iterator())
            }
        }
    }

    internal fun findEntry(depth: Int, key: K, hashBits: IntBits = hash(key)): Entry<K, V>? {
        when(this) {
            is Entry -> {
                for(entry in this) {
                    if(entry.key == key) return entry
                }
                return null
            }
            is Node -> {
                val index = hashBits.wordAt(depth, DIGITS).asInt()
                return this[index]?.findEntry(depth + 1, key, hashBits)
            }
        }
    }

    fun findEntry(key: K) = findEntry(0, key)
    fun contains(key: K) = findEntry(0, key) != null
    fun getValue(key: K) = findEntry(0, key)?.value

    internal fun insert(depth: Int, key: K, value: @UnsafeVariance V, hashBits: IntBits = hash(key)): HamtElement<K, V> =
            when(this) {
                is Entry -> {
                    if(depth >= MAX_DEPTH) this + Entry(key, value)
                    else when (val existingHash = hash(this.key)) {
                        // do not recalculate all hashes down the line
                        // this covers both equal hashes and equal values
                        hashBits -> this + Entry(key, value)
                        else -> Node<K, V>()
                                .insert(depth, this.key, this.value, existingHash)
                                .insert(depth, key, value, hashBits)
                    }
                }
                is Node -> {
                    val index = hashBits.wordAt(depth, DIGITS).asInt()
                    when(val sub = this[index]) {
                        null -> set(index, Entry(key, value))
                        else -> set(index, sub.insert(depth + 1, key, value, hashBits))
                    }
                }
            }

    fun insert(key: K, value: @UnsafeVariance V) = insert(0, key, value)

    internal fun remove(depth: Int, key: K, hashBits: IntBits = hash(key)): HamtElement<K, V>? {
        return when(this) {
            is Entry -> this - key
            is Node -> {
                val index = hashBits.wordAt(depth, DIGITS).asInt()
                when(val sub = this[index]) {
                    null -> this
                    else -> when(val bottom = sub.remove(depth + 1, key, hashBits)) {
                        null -> remove(index)
                        else -> set(index, bottom)
                    }
                }
            }
        }
    }

    fun removeKey(key: K) = remove(0, key)
}

class HamtMap<K, out V>(private val root: HamtElement<K, V>? = null) : AbstractImmutableMap<K, V>() {
    private inner class EntrySet : AbstractSet<Map.Entry<K, V>>() {
        override val size: Int
            get() = root?.size ?: 0
        override fun contains(element: Map.Entry<K, @UnsafeVariance V>) =
                when(val entry = root?.findEntry(element.key)) {
                    null -> false
                    else -> entry.value == element.value
                }
        override fun isEmpty(): Boolean = null === root
        override fun iterator(): Iterator<Map.Entry<K, V>> = root?.iterator() ?: iterator {}
    }

    override val entries: Set<Map.Entry<K, V>>
        get() = EntrySet()
    override fun put(key: K, value: @UnsafeVariance V): ImmutableMap<K, V> =
            HamtMap(root?.insert(key, value) ?: HamtElement.Entry(key, value))
    override fun remove(key: K): ImmutableMap<K, V> =
            HamtMap(root?.removeKey(key))
    override fun containsKey(key: K): Boolean = root?.contains(key) ?: false
    override fun isEmpty(): Boolean = null === root
    override val size: Int
        get() = root?.size ?: 0
}
