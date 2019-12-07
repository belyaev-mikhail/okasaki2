package ru.spbstu.immutable

data class SListZipper<out E>(val left: SList<E>, val element: @UnsafeVariance E, val right: SList<E>) {
    constructor(list: SList.Cons<E>): this(SList.Nil, list.head, list.tail)

    fun goLeft() = when(left) {
        is SList.Cons -> copy(left.tail, left.head, SList.Cons(element, right))
        else -> this
    }
    fun hasLeft() = left !== SList.Nil
    fun goRight() = when(right) {
        is SList.Cons -> copy(SList.Cons(element, left), right.head, right.tail)
        else -> this
    }
    fun hasRight() = right !== SList.Nil
}

fun <E> SListZipper<E>.goLeft(ix: Int) = repeat(ix, this) { it.goLeft() }
fun <E> SListZipper<E>.goRight(ix: Int) = repeat(ix, this) { it.goRight() }
fun <E> SListZipper<E>.addLeft(element: @UnsafeVariance E) =
        copy(left = SList.Cons(element, left))
fun <E> SListZipper<E>.addRight(element: @UnsafeVariance E) =
        copy(right = SList.Cons(element, right))
fun <E> SListZipper<E>.removeShiftingLeft() = when(left) {
    is SList.Cons -> copy(element = left.head, left = left.tail)
    else -> throw IllegalArgumentException()
}
fun <E> SListZipper<E>.removeShiftingRight() = when(right) {
    is SList.Cons -> copy(element = right.head, left = right.tail)
    else -> throw IllegalArgumentException()
}
fun <E> SListZipper<E>.remove(): SListZipper<E> = when(right) {
    is SList.Cons -> removeShiftingRight()
    else -> removeShiftingLeft()
}
fun <E> SListZipper<E>.rewind() = goLeft(left.size).let { SList.Cons(it.element, it.right) }
fun <E> SListZipper<E>.set(element: E) = copy(element = element)

sealed class SList<out E>(override val size: Int): AbstractImmutableList<E>() {
    data class Cons<out E>(val head: E, val tail: SList<E>): SList<E>(tail.size + 1) {
        override fun toString(): String = super.toString()
    }
    object Nil: SList<Nothing>(0)

    private fun asCons(): Cons<E> = when(this){ is Cons -> this; else -> throw IllegalStateException() }

    fun zipper() = SListZipper(asCons())

    override fun add(index: Int, element: @UnsafeVariance E): SList.Cons<E> = when(index) {
        0 -> Cons(element, this)
        else -> zipper().goRight(index).addLeft(element).rewind()
    }
    override fun addAll(index: Int, elements: Collection<@UnsafeVariance E>): SList<E> {
        var startingPoint = zipper().goRight(index)
        for(e in elements) startingPoint = startingPoint.addLeft(e)
        return startingPoint.rewind()
    }

    override fun get(index: Int): E = zipper().goRight(index).element
    override fun removeAt(index: Int): SList<E> = zipper().goRight(index).remove().rewind()
    override fun remove(element: @UnsafeVariance E): SList<E> = when(this) {
        is Nil -> this
        is Cons -> if(head == element) tail.remove(element) else Cons(head, tail.remove(element))
    }
    override fun set(index: kotlin.Int, element: @UnsafeVariance E): SList<E> =
            zipper().goRight(index).set(element).rewind()

    override fun subList(fromIndex: kotlin.Int, toIndex: kotlin.Int): ImmutableList<E> =
            zipper()
                    .goRight(fromIndex)
                    .copy(left = Nil)
                    .goRight(toIndex - fromIndex)
                    .copy(right = Nil)
                    .rewind()
}

operator fun <E> E.plus(that: SList<E>): SList<E> = SList.Cons(this, that)
