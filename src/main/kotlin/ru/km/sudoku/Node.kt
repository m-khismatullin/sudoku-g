package ru.km.sudoku

class Node(
    val parent: Node?,
    val position: Position,
    val possibles: Set<Int>
) {
    var number: Int = 0
    private val mExcluded = mutableSetOf<Int>(0)
    val excluded: Set<Int>
        get() = mExcluded.toSet()

    fun retToParent() =
        parent?.let {
            it.mExcluded += it.number
            it.number = 0
            it
        } ?: throw IllegalArgumentException("Сбой генерации")


}