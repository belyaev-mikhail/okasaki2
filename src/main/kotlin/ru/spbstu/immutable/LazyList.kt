package ru.spbstu.immutable

sealed class LazyList<out E>: Sequence<E> {

    @Suppress("NOTHING_TO_INLINE", "UNCHECKED_CAST")
    internal inline fun asCons(): Cons<E>? = if(this === Nil) null else this as Cons<E>

    data class Cons<out E>(val head: E, val lazyTail: Lazy<LazyList<E>>): LazyList<E>() {
        val tail
            inline get() = lazyTail.value

        override fun iterator(): Iterator<E> = iterator builder@ {
            yield(head)
            var current = tail.asCons() ?: return@builder
            while(true) {
                yield(current.head)
                current = current.tail.asCons() ?: return@builder
            }
        }
    }
    object Nil : LazyList<Nothing>() {
        override fun iterator(): Iterator<Nothing> = iterator {}
    }

    fun isEmpty() = this === Nil

    companion object {
        fun <E> empty(): LazyList<E> = Nil

        fun <E> generate(next: () -> E): LazyList<E> =
                LazyList(next()) { generate(next) }
        fun <E> generate(start: E, next: (E) -> E): LazyList<E> =
                LazyList(start) { generate(next(start), next) }
    }
}

fun <E> LazyList(head: E, tailGen: () -> LazyList<E>) = LazyList.Cons<E>(head, lazy(tailGen))

operator fun <E> LazyList<E>.plus(that: LazyList<E>): LazyList<E> = when(val cons = this.asCons()) {
    null -> that
    else -> LazyList(cons.head) { cons.tail + that }
}
operator fun <E> E.plus(that: LazyList<E>): LazyList<E> = LazyList(this) { that }
operator fun <E> LazyList<E>.plus(that: E): LazyList<E> = when(val cons = this.asCons()) {
    null -> LazyList(that) { LazyList.Nil }
    else -> LazyList(cons.head) { cons.tail + that }
}
