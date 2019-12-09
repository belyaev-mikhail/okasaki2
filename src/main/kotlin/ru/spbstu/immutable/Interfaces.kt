@file: Suppress(Warnings.NOTHING_TO_INLINE)
package ru.spbstu.immutable

import kotlinx.warnings.Warnings

interface ImmutableList<out E> : List<E> {
    fun add(element: @UnsafeVariance E): ImmutableList<E>
    fun add(index: Int, element: @UnsafeVariance E): ImmutableList<E>
    fun addAll(index: Int, elements: Collection<@UnsafeVariance E>): ImmutableList<E>
    fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E>
    fun remove(element: @UnsafeVariance E): ImmutableList<E>
    fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E>
    fun removeAt(index: Int): ImmutableList<E>
    fun set(index: Int, element: @UnsafeVariance E): ImmutableList<E>
    override fun subList(fromIndex: Int, toIndex: Int): ImmutableList<E>
}

inline operator fun <E> ImmutableList<E>.plus(value: E): ImmutableList<E> = add(value)
inline operator fun <E> ImmutableList<E>.plus(that: Collection<E>): ImmutableList<E> = addAll(that)
inline operator fun <E> ImmutableList<E>.minus(value: E): ImmutableList<E> = remove(value)
inline operator fun <E> ImmutableList<E>.minus(that: Collection<E>): ImmutableList<E> = removeAll(that)

interface ImmutableSet<out E> : Set<E> {
    fun add(element: @UnsafeVariance E): ImmutableSet<E>
    fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>
    fun remove(element: @UnsafeVariance E): ImmutableSet<E>
    fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>
    fun retainAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>
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
