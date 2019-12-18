package ru.spbstu.immutable

import kotlin.random.Random

private infix fun <E> IKTreapNode<E>?.merge(that: IKTreapNode<E>?): IKTreapNode<E>? = when {
    null === this -> that
    null === that -> this
    this.priority >= that.priority ->
        this.copy(right = this.right merge that)
    else /* this.priority < that.priority */ ->
        that.copy(left = this merge that.left)
}

private data class IKTreapNode<out E> private constructor(
        val value: E,
        val priority: Int,
        val left: IKTreapNode<E>?,
        val right: IKTreapNode<E>?
) {
    val size: Int = (left?.size ?: 0) + (right?.size ?: 0) + 1
    inline val currentIndex get() = left?.size ?: 0

    constructor(element: E, left: IKTreapNode<E>? = null, right: IKTreapNode<E>? = null, random: Random = Random.Default) :
            this(element, random.nextInt(), left, right)

    fun split(index: Int): Triple<IKTreapNode<E>?, E, IKTreapNode<E>?> = when {
        index == currentIndex -> Triple(left, value, right)
        index < currentIndex -> {
            left ?: throw IndexOutOfBoundsException()
            val (ll, le, lr) = left.split(index)
            Triple(ll, le, copy(left = lr))
        }
        else -> /* index > currentIndex */ {
            right ?: throw IndexOutOfBoundsException()
            val (rl, re, rr) = right.split(index - currentIndex - 1)
            Triple(copy(right = rl), re, rr)
        }
    }
}

private fun <E> IKTreapNode<E>?.add(element: E, random: Random = Random.Default) =
        this merge IKTreapNode(element, random = random)

private fun <E> IKTreapNode<E>?.insertAt(index: Int, node: IKTreapNode<E>?,
                                         random: Random = Random.Default): IKTreapNode<E>? = when {
    null === this -> node
    null === node -> this
    index == 0 -> node merge this
    index == this.size -> this merge node
    else -> {
        val (l, e, r) = this.split(index)
        l merge node merge IKTreapNode(e, random = random) merge r
    }
}

private fun <E> IKTreapNode<E>?.removeAt(index: Int): IKTreapNode<E>? {
    if (null === this) return null
    val (l, _, r) = this.split(index)
    return l merge r
}

private fun <E> IKTreapNode<E>?.get(index: Int): E = when {
    null === this -> throw IndexOutOfBoundsException()
    index == currentIndex -> value
    index < currentIndex -> left.get(index)
    else /* index > currentIndex */ -> right.get(index - currentIndex - 1)
}

private fun <E> IKTreapNode<E>?.set(index: Int, value: E, random: Random = Random.Default): IKTreapNode<E>? = when {
    null === this -> null
    index == currentIndex -> copy(value = value)
    else -> {
        val (l, _, r) = this.split(index)
        l merge IKTreapNode(value, random = random) merge r
    }
}

private fun <E> IKTreapNode<E>?.iterator(): Iterator<E> = iterator builder@ {
    this@iterator ?: return@builder
    yieldAll(left.iterator())
    yield(value)
    yieldAll(right.iterator())
}

class IKTreap<out E> private constructor(
        private val root: IKTreapNode<E>?,
        private val random: Random = Random.Default): AbstractImmutableList<E>() {
    constructor(random: Random = Random.Default): this(null, random)

    override fun iterator(): Iterator<E> = root.iterator()

    override fun add(element: @UnsafeVariance E): IKTreap<E> =
            IKTreap(root.add(element, random = random), random = random)
    override fun add(index: Int, element: @UnsafeVariance E): IKTreap<E> =
            IKTreap(root.insertAt(index, IKTreapNode(element, random = random)), random = random)

    override fun removeAt(index: Int): IKTreap<E> =
            IKTreap(root.removeAt(index), random = random)

    override fun set(index: Int, element: @UnsafeVariance E): IKTreap<E> =
            IKTreap(root.set(index, element, random = random), random = random)

    override val size: Int
        get() = root?.size ?: 0

    override fun get(index: Int): E =
            root.get(index)

    override fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableList<E> = when(elements) {
        is IKTreap -> IKTreap(root merge elements.root, random = random)
        else -> super.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<@UnsafeVariance E>): ImmutableList<E> = when(elements) {
        is IKTreap -> IKTreap(root.insertAt(index, elements.root, random = random), random = random)
        else -> super.addAll(index, elements)
    }
}

fun <E> ikTreapOf(vararg values: E, random: Random = Random.Default): IKTreap<E> {
    var base = IKTreap<E>(random = random)
    for(value in values) base = base.add(value)
    return base
}
