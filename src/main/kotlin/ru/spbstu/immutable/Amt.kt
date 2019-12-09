package ru.spbstu.immutable

import kotlinx.warnings.Warnings
import ru.spbstu.wheels.*

private const val DIGITS = 5
private const val MAX_DEPTH = 6

internal sealed class AmtElement<out E> {
    abstract operator fun iterator(): Iterator<E>

    class Node<out E>: AmtElement<E>() {
        private val data: TArray<AmtElement<E>> = TArray(Int.SIZE_BITS)

        fun nodeAt(index: Int) = data[index] ?: Node<E>().also { data[index] = it }
        fun leafAt(index: Int) = data[index] ?: Leaf<E>().also { data[index] = it }

        override fun iterator(): Iterator<E> = iterator<E> {
            for(sub in data) {
                if(null === sub) break
                yieldAll(sub.iterator())
            }
        }
    }
    class Leaf<out E>: AmtElement<E>() {
        private val data: TArray<E> = TArray(Int.SIZE_BITS)

        override fun iterator(): Iterator<E> = iterator<E> {
            for(e in data) {
                if (null === e) break
                yield(e)
            }
        }
    }
}