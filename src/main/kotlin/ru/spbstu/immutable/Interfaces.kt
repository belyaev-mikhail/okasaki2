package ru.spbstu.immutable

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

inline operator fun <E> ImmutableList<E>.plus(value: E) = add(value)
inline operator fun <E> ImmutableList<E>.plus(that: Collection<E>) = addAll(that)
inline operator fun <E> ImmutableList<E>.minus(value: E) = remove(value)

interface ImmutableSet<out E> : Set<E> {
    fun add(element: @UnsafeVariance E): ImmutableSet<E>
    fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>
    fun remove(element: @UnsafeVariance E): ImmutableSet<E>
    fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>
    fun retainAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E>
}

inline operator fun <E> ImmutableSet<E>.plus(value: E) = add(value)
inline operator fun <E> ImmutableSet<E>.plus(that: ImmutableSet<E>) = addAll(that)
inline operator fun <E> ImmutableSet<E>.minus(value: E) = remove(value)
inline operator fun <E> ImmutableSet<E>.minus(that: Collection<E>) = removeAll(that)

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

interface ImmutableQueue<out E> {
    val top: E?
    fun pop(): ImmutableQueue<E>
    fun push(value: @UnsafeVariance E): ImmutableQueue<E>
    val size: Int

    fun isEmpty(): Boolean = size == 0
}

inline operator fun <E> ImmutableQueue<E>.plus(value: E) = push(value)
