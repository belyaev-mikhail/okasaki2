package ru.spbstu.immutable

abstract class AbstractImmutableList<out E> : AbstractList<E>(), ImmutableList<E> {

    abstract override fun add(index: Int, element: @UnsafeVariance E): ImmutableList<E>
    abstract override fun remove(element: @UnsafeVariance E): ImmutableList<E>
    abstract override fun removeAt(index: Int): ImmutableList<E>
    abstract override fun set(index: kotlin.Int, element: @UnsafeVariance E): ImmutableList<E>
    abstract override fun subList(fromIndex: kotlin.Int, toIndex: kotlin.Int): ImmutableList<E>

    override fun add(element: @UnsafeVariance E): ImmutableList<E> = add(size, element)
    override fun addAll(index: Int, elements: Collection<@UnsafeVariance E>): ImmutableList<E> {
        var current: ImmutableList<E> = this
        for((i, e) in elements.withIndex()) current = current.add(index + i, e)
        return current
    }
    override fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E> = addAll(size, elements)

    override fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E> {
        var current: ImmutableList<E>  = this
        for(e in elements) current = current.remove(e)
        return current
    }
}

abstract class AbstractImmutableSet<out E> : AbstractSet<E>(), ImmutableSet<E> {
    abstract override fun contains(element: @UnsafeVariance E): Boolean
    abstract override fun add(element: @UnsafeVariance E): ImmutableSet<E>
    abstract override fun remove(element: @UnsafeVariance E): ImmutableSet<E>

    override fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E> {
        var self: ImmutableSet<E> = this
        for(e in elements) self = self.add(e)
        return self
    }
    override fun removeAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E> {
        var self: ImmutableSet<E> = this
        for(e in elements) self = self.remove(e)
        return self
    }
    override fun retainAll(elements: Collection<@UnsafeVariance E>): ImmutableSet<E> {
        var self: ImmutableSet<E> = this
        for(e in this) if (e !in elements) self = self.remove(e)
        return self
    }
}

