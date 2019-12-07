package ru.spbstu.immutable

import kotlinx.warnings.Warnings
import ru.spbstu.wheels.*

const val DIGITS = 5
const val MAX_DEPTH = 6

sealed class HamtElement<K, out V> {
    @Suppress(Warnings.NOTHING_TO_INLINE)
    internal inline fun hash(key: K): IntBits {
        val h: Int = key.hashCode()
        return (h xor h.ushr(16)).asBits()
    }

    data class Entry<K, out V>(override val key: K, override val value: V): HamtElement<K, V>(), Map.Entry<K, V>
    data class Bucket<K, out V>(val elements: SList<Entry<K, V>>): HamtElement<K, V>() {
        operator fun plus(element: Entry<K, @UnsafeVariance V>): Bucket<K, V> {
            if(elements.isEmpty()) return Bucket(element + SList.Nil)
            var z = elements.zipper()

            do {
                val (k, v) = z.element
                if(k == element.key) {
                    return if(v == element.value) this else Bucket(z.set(element).rewind())
                }
                z = z.goRight()
            } while(z.hasRight())

            // no such element
            return Bucket(element + elements)
        }

        operator fun iterator() = elements.iterator()
    }
    data class Node<K, out V>(val mask: IntBits,
                              private val data: TArray<HamtElement<K, @UnsafeVariance V>>): HamtElement<K, V>() {
        constructor(): this(IntBits.Zero, TArray(0))

        private fun adjustIndex(index: Int) = mask.slice(toExclusive = index).popCount

        operator fun get(index: Int) = if(mask[index]) data[adjustIndex(index)] else null
        fun set(index: Int, element: HamtElement<K, @UnsafeVariance V>): Node<K, V> {
            require(0 <= index && index < IntBits.SIZE)

            val adjustedIndex = adjustIndex(index)
            if(mask[index])
                return copy(data = data.copyOf().apply { set(adjustedIndex, element) })

            val newMask = mask.set(index)
            val newData = TArray<HamtElement<K, @kotlin.UnsafeVariance V>>(data.size + 1)

            data.copyInto(newData, startIndex = 0, endIndex = adjustedIndex)
            data.copyInto(newData, startIndex = adjustedIndex, destinationOffset = adjustedIndex + 1)
            newData[adjustedIndex] = element
            return Node(newMask, newData)
        }
        fun remove(index: Int): Node<K, V>? {
            require(0 <= index && index < IntBits.SIZE)

            val adjustedIndex = adjustIndex(index)
            if(!mask[index]) return this

            val newMask = mask.clear(index)
            if(newMask == IntBits.Zero) return null

            val newData = TArray<HamtElement<K, @kotlin.UnsafeVariance V>>(data.size - 1)

            data.copyInto(newData, startIndex = 0, endIndex = adjustedIndex)
            data.copyInto(newData, startIndex = adjustedIndex + 1, destinationOffset = adjustedIndex)
            return Node(newMask, newData)
        }

        inline fun forEachElement(body: (HamtElement<K, V>) -> Unit) {
            mask.forEachOneBit { bit ->
                body(get(bit.numberOfLeadingZeros)!!)
            }
        }
    }

    protected fun findEntry(depth: Int, key: K, hashBits: IntBits = hash(key)): Entry<K, V>? {
        when(this) {
            is Entry -> return if(key == this.key) this else null
            is Bucket -> {
                for(entry in this) entry.findEntry(depth, key, hashBits)?.let { return it }
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

    protected fun insert(depth: Int, key: K, value: @UnsafeVariance V, hashBits: IntBits = hash(key)): HamtElement<K, V> =
            when(this) {
                is Entry -> if(key == this.key) copy(value = value) else {
                    if(depth == MAX_DEPTH) Bucket(this + (Entry(key, value) + SList.Nil))
                    else Node<K, V>()
                            .insert(depth + 1, this.key, this.value) // recalculating the hash for this.key is unfortunate
                            .insert(depth + 1, key, value, hashBits)
                }
                is Bucket -> this + Entry(key, value)
                is Node -> {
                    val index = hashBits.wordAt(depth, DIGITS).asInt()
                    val sub = this[index] ?: if(depth == MAX_DEPTH) Entry(key, value) else Node()
                    set(index, sub.insert(depth + 1, key, value, hashBits))
                }
            }

    fun insert(key: K, value: @UnsafeVariance V) = insert(0, key, value)

}
