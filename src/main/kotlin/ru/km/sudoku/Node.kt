package ru.km.sudoku

class Node(
    val parent: Node?,
    val position: Position,
    val number: Int,
) {
    fun getValuesFromNodeChain(): Map<Position, Int> =
        (parent?.getValuesFromNodeChain() ?: emptyMap()).plus(position to number)
}