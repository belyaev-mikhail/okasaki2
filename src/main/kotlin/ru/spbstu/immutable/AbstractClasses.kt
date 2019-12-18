package ru.spbstu.immutable

abstract class AbstractImmutableList<out E> : AbstractList<E>(), ImmutableList<E> {
    abstract override fun get(index: Int): E
    abstract override fun set(index: Int, element: @UnsafeVariance E): ImmutableList<E>
    abstract override fun add(index: Int, element: @UnsafeVariance E): ImmutableList<E>
    abstract override fun removeAt(index: Int): ImmutableList<E>
}

abstract class AbstractImmutableSet<out E> : AbstractSet<E>(), ImmutableSet<E> {
    abstract override fun contains(element: @UnsafeVariance E): Boolean
    abstract override fun add(element: @UnsafeVariance E): ImmutableSet<E>
    abstract override fun remove(element: @UnsafeVariance E): ImmutableSet<E>
}

abstract class AbstractImmutableMap<K, out V> : AbstractMap<K, V>(), ImmutableMap<K, V> {
    abstract override fun containsKey(key: K): Boolean
    abstract override fun get(key: K): V?
    abstract override fun put(key: K, value: @UnsafeVariance V): ImmutableMap<K, V>
    abstract override fun remove(key: K): ImmutableMap<K, V>
}
