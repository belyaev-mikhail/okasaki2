package ru.spbstu.immutable

import kotlinx.warnings.Warnings
import ru.spbstu.wheels.*

private const val DIGITS = 5
private const val MAX_DEPTH = 6

internal sealed class HamtElement<K, out V> {
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
                          override val size: Int = nextEntry?.size?.plus(1) ?: 1): HamtElement<K, V>(), Map.Entry<K, V> {
        fun copy(key: K = this.key,
                 value: @UnsafeVariance V = this.value,
                 nextEntry: Entry<K, @UnsafeVariance V>? = this.nextEntry) = Entry(key, value, nextEntry)

        operator fun plus(that: Entry<K, @UnsafeVariance V>): Entry<K, V> =
                when {
                    that.nextEntry === null -> plusOne(that)
                    else -> {
                        var res = this
                        for(e in that) res = res.plusOne(e)
                        res
                    }
                }
        fun plusOne(that: Entry<K, @UnsafeVariance V>): Entry<K, V> {
            if(null === nextEntry) { // shortcut for most common case
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

        operator fun get(index: Int) = if(mask[index]) data[adjustIndex(mask, index)] else null
        fun set(index: Int, element: HamtElement<K, @UnsafeVariance V>): Node<K, V> {
            require(0 <= index && index < IntBits.SIZE)

            val adjustedIndex = adjustIndex(mask, index)
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

            val adjustedIndex = adjustIndex(mask, index)
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

    private fun findEntryForHash(depth: Int, hashBits: IntBits): Entry<K, V>? = when(this) {
        is Entry -> this
        is Node -> {
            val index = hashBits.wordAt(depth, DIGITS).asInt()
            this[index]?.findEntryForHash(depth + 1, hashBits)
        }
    }
    internal fun findEntry(depth: Int, key: K, hashBits: IntBits = hash(key)): Entry<K, V>? {
        val e = findEntryForHash(depth, hashBits) ?: return null
        for(entry in e) {
            if(entry.key == key) return entry
        }
        return null
    }

    fun findEntry(key: K) = findEntry(0, key)
    fun contains(key: K) = findEntry(0, key) != null
    fun getValue(key: K) = findEntry(0, key)?.value

    internal fun insert(depth: Int, entry: Entry<K, @UnsafeVariance V>,
                        hashBits: IntBits = hash(entry.key)): HamtElement<K, V> =
            when(this) {
                is Entry -> {
                    if(depth >= MAX_DEPTH) this + entry
                    else when (val existingHash = hash(this.key)) {
                        // do not recalculate all hashes down the line
                        // this covers both equal hashes and equal values
                        hashBits -> this + entry
                        else -> Node<K, V>()
                                .insert(depth, this, existingHash)
                                .insert(depth, entry, hashBits)
                    }
                }
                is Node -> {
                    val index = hashBits.wordAt(depth, DIGITS).asInt()
                    when(val sub = this[index]) {
                        null -> set(index, entry)
                        else -> set(index, sub.insert(depth + 1, entry, hashBits))
                    }
                }
            }

    fun insert(key: K, value: @UnsafeVariance V) = insert(0, Entry(key, value))

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

    internal fun union(depth: Int, element: HamtElement<K, @UnsafeVariance V>): HamtElement<K, V> {
        if(element === this) return this
        return when {
            this is Entry && element is Node -> element.union(depth, this)
            element is Entry -> insert(depth, element)
            else -> { /* smart casts not powerful enough */
                @Suppress(Warnings.UNCHECKED_CAST)
                this as Node
                @Suppress(Warnings.UNCHECKED_CAST)
                element as Node

                val totalMask = this.mask or element.mask
                val newData = TArray<HamtElement<K, V>>(totalMask.popCount)
                var size = 0
                totalMask.forEachOneBit { bit ->
                    val index = bit.numberOfLeadingZeros
                    val thisE = this[index]
                    val thatE = element[index]
                    val adjustedIndex = adjustIndex(totalMask, index)
                    newData[adjustedIndex] = when {
                        null === thisE -> thatE
                        null === thatE -> thisE
                        else -> thisE.union(depth + 1, thatE)
                    }
                    size += newData[adjustedIndex]!!.size
                }
                Node(totalMask, newData, size)
            }
        }
    }

    infix fun union(that: HamtElement<K, @UnsafeVariance V>) = union(0, that)

    internal fun intersect(depth: Int, element: HamtElement<K, @UnsafeVariance V>): HamtElement<K, V>? {
        if(element === this) return this
        return when(this) {
            is Entry -> when(element) {
                is Entry -> {
                    // XXX: optimize this
                    var res: Entry<K, V>? = null
                    for(l in this) {
                        for(r in element) {
                            if(l.key == r.key && l.value == r.value) res = if(res === null) l else l + res
                        }
                    }
                    res
                }
                is Node -> element.intersect(depth, this) // Entry x Node -> Node x Entry
            }
            is Node -> when(element) {
                is Entry -> {
                    val hashCode = hash(element.key)
                    val entry = findEntryForHash(depth, hashCode)
                    entry?.intersect(depth, element)
                }
                is Node -> {
                    val tmpMask = this.mask and element.mask
                    if(tmpMask == IntBits.Zero) return null
                    var recalculatedMask = tmpMask

                    val tmpData = TArray<HamtElement<K, V>>(tmpMask.popCount)
                    var size = 0
                    tmpMask.forEachOneBit { bit ->
                        val index = bit.numberOfLeadingZeros
                        val thisE = this[index]!!
                        val thatE = element[index]!!
                        val adjustedIndex = adjustIndex(tmpMask, index)
                        val res = thisE.intersect(depth + 1, thatE)
                        if(null === res) recalculatedMask = recalculatedMask andNot bit
                        else {
                            tmpData[adjustedIndex] = res
                            size += res.size
                        }
                    }

                    when (recalculatedMask) {
                        tmpMask -> Node(tmpMask, tmpData, size)
                        IntBits.Zero -> null
                        else -> {
                            val newData = TArray<HamtElement<K, V>>(recalculatedMask.popCount)
                            recalculatedMask.forEachOneBit { bit ->
                                val index = bit.numberOfLeadingZeros
                                newData[adjustIndex(recalculatedMask, index)] = tmpData[adjustIndex(tmpMask, index)]
                            }
                            Node(recalculatedMask, newData, size)
                        }
                    }
                }
            }
        }
    }

    infix fun intersect(that: HamtElement<K, @UnsafeVariance V>) = intersect(0, that)

    companion object {
        internal fun adjustIndex(mask: IntBits, index: Int) = mask.slice(toExclusive = index).popCount
    }
}

class HamtMap<K, out V>
internal constructor(private val root: HamtElement<K, V>? = null) : AbstractImmutableMap<K, V>() {
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
    override fun put(key: K, value: @UnsafeVariance V): HamtMap<K, V> =
            HamtMap(root?.insert(key, value) ?: HamtElement.Entry(key, value))
    override fun putAll(from: Map<K, @UnsafeVariance V>): ImmutableMap<K, V> = when(from) {
        is HamtMap<K, V> -> when {
            null === root -> from
            null === from.root -> this
            else -> HamtMap(root.union(from.root))
        }
        else -> super.putAll(from) as HamtMap<K, V>
    }

    override fun remove(key: K): HamtMap<K, V> =
            HamtMap(root?.removeKey(key))
    override fun containsKey(key: K): Boolean = root?.contains(key) ?: false
    override fun isEmpty(): Boolean = null === root
    override val size: Int
        get() = root?.size ?: 0
}

fun <K, V> hamtMapOf(): HamtMap<K, V> = HamtMap(null)
fun <K, V> hamtMapOf(entry: Pair<K, V>): HamtMap<K, V> = HamtMap(HamtElement.Entry(entry.first, entry.second))
fun <K, V> hamtMapOf(vararg entries: Pair<K, V>): HamtMap<K, V> {
    var res = hamtMapOf<K, V>()
    for(entry in entries) res = res.put(entry.first, entry.second)
    return res
}

fun <K, V> immutableMapOf(): ImmutableMap<K, V> = hamtMapOf()
fun <K, V> immutableMapOf(entry: Pair<K, V>): ImmutableMap<K, V> = hamtMapOf(entry)
fun <K, V> immutableMapOf(vararg entries: Pair<K, V>): ImmutableMap<K, V> = hamtMapOf(*entries)

class HamtSet<out E>
internal constructor(internal val root: HamtElement<@UnsafeVariance E, E>? = null) : AbstractImmutableSet<E>() {
    override fun contains(element: @UnsafeVariance E): Boolean = root?.contains(element) ?: false
    override fun add(element: @UnsafeVariance E): HamtSet<E> =
            HamtSet(root?.insert(element, element) ?: HamtElement.Entry(element, element))
    override fun addAll(elements: Collection<@UnsafeVariance E>): HamtSet<E> = when(elements) {
        is HamtSet<E> -> when {
            null === root -> elements
            null === elements.root -> this
            else -> HamtSet(root.union(elements.root))
        }
        else -> super.addAll(elements) as HamtSet<E>
    }
    override fun retainAll(elements: Collection<@UnsafeVariance E>): HamtSet<E> = when(elements) {
        is HamtSet<E> -> when {
            null === root || null === elements.root -> this
            else -> HamtSet(root.intersect(elements.root))
        }
        else -> super.retainAll(elements) as HamtSet<E>
    }

    override fun remove(element: @UnsafeVariance E): HamtSet<E> =
            HamtSet(root?.removeKey(element))
    override val size: Int
        get() = root?.size ?: 0
    override fun isEmpty(): Boolean = null === root
    override fun iterator(): Iterator<E> = iterator {
        val inner = root?.iterator() ?: return@iterator
        for((e,_) in inner) yield(e)
    }
}
fun <E> hamtSetOf(): HamtSet<E> = HamtSet(null)
fun <E> hamtSetOf(element: E): HamtSet<E> = HamtSet(HamtElement.Entry(element, element))
fun <E> hamtSetOf(vararg elements: E): HamtSet<E> {
    var res = hamtSetOf<E>()
    for(element in elements) res = res.add(element)
    return res
}

fun <E> immutableSetOf(): ImmutableSet<E> = hamtSetOf()
fun <E> immutableSetOf(element: E): ImmutableSet<E> = hamtSetOf(element)
fun <E> immutableSetOf(vararg elements: E): ImmutableSet<E> = hamtSetOf(*elements)
