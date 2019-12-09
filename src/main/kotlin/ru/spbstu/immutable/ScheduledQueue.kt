package ru.spbstu.immutable

import kotlinx.warnings.Warnings

// rotate is essentially a scheduled reverse() of `inputs`
// precondition: outputs.size == (inputs.size - 1)
// accumulator is the result of the rotation of `input`
// logically, it returns outputs ++ inputs.reverse(), but using smart lazy unwrapping of things
// start with the contents of outputs, then, when it runs out, continue with inputs.head
// (that is guaranteed to be the only item in inputs left if any),
// then the accumulator
internal fun<E> rotate(outputs: LazyList<E>, inputs: SList.Cons<E>, accumulator: LazyList<E> = LazyList.empty()): LazyList<E> =
        @Suppress(Warnings.NAME_SHADOWING)
        when(val outputs = outputs.asCons()) {
            null -> LazyList(inputs.head) { accumulator }
            else -> LazyList(outputs.head) {
                rotate(outputs.tail, inputs.tail as SList.Cons<E>, inputs.head + accumulator)
            }
        }

class ScheduledQueue<E> private constructor(
        override val size: Int,
        private val inputs: SList<E>,
        private val outputs: LazyList<E>,
        // schedule serves two purposes:
        // first, on every operation we need to force a single element of output
        // but we can't, cause it may not be first element.
        // schedule initially contains all the output elements, but we gradually force-remove them one by one
        // and force them _inside_ output not even touching it.
        // second, the size is (genius!) always equal to (outputs.size - inputs.size), so, when it reaches zero,
        // it's `rotate` time!
        private val schedule: LazyList<E> = outputs): ImmutableQueue<E> {

    constructor() : this(0, SList.Nil, LazyList.empty(), LazyList.empty())

    internal fun copy(
            size: Int = this.size,
            inputs: SList<E> = this.inputs,
            outputs: LazyList<E> = this.outputs,
            schedule: LazyList<E> = this.schedule
    ) = ScheduledQueue(size, inputs, outputs, schedule)

    // exec is the workhorse that is invoked on every operation.
    // it force-removes one element from schedule (tick-tock), and, when it runs empty, invokes `rotate`
    // to do the reversing
    internal fun exec(): ScheduledQueue<E> = when {
        !schedule.isEmpty() -> copy(schedule = schedule.asCons()!!.tail)
        inputs.isEmpty() -> this
        else -> {
            val rotated = rotate(outputs, inputs as SList.Cons<E>)
            ScheduledQueue(size, SList.Nil, rotated, rotated)
        }
    }

    override fun push(value: E) = copy(size = size + 1, inputs = SList.Cons(value, inputs)).exec()

    override fun pop() = when(val outputs = outputs.asCons()) {
        null -> this
        else -> copy(size = size - 1, outputs = outputs.tail).exec()
    }

    override fun isEmpty() = outputs.isEmpty()
    override val top: E?
        get() = outputs.asCons()?.head

    override fun toString() = "ScheduledQueue($inputs><$outputs)"
}