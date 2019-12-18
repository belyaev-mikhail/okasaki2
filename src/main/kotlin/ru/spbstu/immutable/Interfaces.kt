@file: Suppress(Warnings.NOTHING_TO_INLINE)
package ru.spbstu.immutable

import kotlinx.warnings.Warnings

interface ImmutableList<out E> : List<E> {
    fun add(index: Int, element: @UnsafeVariance E): ImmutableList<E>
    fun removeAt(index: Int): ImmutableList<E>
    fun set(index: Int, element: @UnsafeVariance E): ImmutableList<E>

    fun remove(element: @UnsafeVariance E): ImmutableList<E> = when(val index = indexOf(element)) {
        -1 -> this
        else -> removeAt(index)
    }
    fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E> {
        var current: ImmutableList<E>  = this
        for(e in elements) current = current.remove(e)
        return current
    }

    fun add(element: @UnsafeVariance E): ImmutableList<E> = add(size, element)
    fun addAll(index: Int, elements: Collection<@UnsafeVariance E>): ImmutableList<E> {
        var current: ImmutableList<E> = this
        for((i, e) in elements.withIndex()) current = current.add(index + i, e)
        return current
    }
    fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E> = addAll(size, elements)

}

inline operator fun <E> ImmutableList<E>.plus(value: E): ImmutableList<E> = add(value)
inline operator fun <E> ImmutableList<E>.plus(that: Collection<E>): ImmutableList<E> = addAll(that)
inline operator fun <E> ImmutableList<E>.minus(value: E): ImmutableList<E> = remove(value)
inline operator fun <E> ImmutableList<E>.minus(that: Collection<E>): ImmutableList<E> = removeAll(that)

interface ImmutableSet<out E> : Set<E> {
    fun add(element: @UnsafeVariance E): ImmutableSet<E>
    fun remove(element: @UnsafeVariance E): ImmutableSet<E>

    fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E> {
        var self: ImmutableSet<E> = this
        for(e in elements) self = self.add(e)
        return self
    }
    fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E> {
        var self: ImmutableSet<E> = this
        for(e in elements) self = self.remove(e)
        return self
    }
    fun retainAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E> {
        var self: ImmutableSet<E> = immutableSetOf()
        for(e in elements) if(e in this) self = self.add(e)
        return self
    }
}

inline operator fun <E> ImmutableSet<E>.plus(value: E): ImmutableSet<E> = add(value)
inline operator fun <E> ImmutableSet<E>.plus(that: ImmutableSet<E>): ImmutableSet<E> = addAll(that)
inline operator fun <E> ImmutableSet<E>.minus(value: E): ImmutableSet<E> = remove(value)
inline operator fun <E> ImmutableSet<E>.minus(that: Collection<E>): ImmutableSet<E> = removeAll(that)
inline infix fun <E> ImmutableSet<E>.intersect(that: ImmutableSet<E>): ImmutableSet<E> = retainAll(that)

interface ImmutableMap<K, out V> : Map<K, V> {
    fun put(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>
    fun remove(key: K): ImmutableMap<K, V>
    fun remove(key: K, value: @UnsafeVariance V): ImmutableMap<K, V> =
            when(get(key)) {
                value -> remove(key)
                else -> this
            }
    fun putAll(from: Map<K, @UnsafeVariance V>): ImmutableMap<K, V> {
        var res = this
        for((k, v) in from) res = res.put(k, v)
        return res
    }
}

inline operator fun <K, V> ImmutableMap<K, V>.plus(entry: Pair<K, V>): ImmutableMap<K, V> = put(entry.first, entry.second)
inline operator fun <K, V> ImmutableMap<K, V>.plus(entry: Map.Entry<K, V>): ImmutableMap<K, V> = put(entry.key, entry.value)
inline operator fun <K, V> ImmutableMap<K, V>.plus(that: Map<K, V>): ImmutableMap<K, V> = putAll(that)
inline operator fun <K, V> ImmutableMap<K, V>.minus(key: K): ImmutableMap<K, V> = remove(key)

interface ImmutableQueue<out E> {
    val top: E?
    fun pop(): ImmutableQueue<E>
    fun push(value: @UnsafeVariance E): ImmutableQueue<E>
    val size: Int

    fun isEmpty(): Boolean = size == 0
}

inline operator fun <E> ImmutableQueue<E>.plus(value: E) = push(value)
