package ru.km.sudoku

import ru.km.sudoku.Board.Companion.BLOCK_SIZE_IN_CELL
import ru.km.sudoku.Board.Companion.LINE_SIZE_IN_CELL
import ru.km.sudoku.Position.Companion.getWithDiagonallyOppositeIndexList

open class Board(difficulty: Difficulty) {
    private val mVersions: MutableMap<Cell, Int>
    val versions: Map<Cell, Int>
        get() = mVersions.toMap()
    val cells: Map<Position, Cell>

    companion object {
        const val LINE_SIZE_IN_CELL = 9
        const val BLOCK_SIZE_IN_CELL = 3
        const val BLOCKS_IN_LINE = LINE_SIZE_IN_CELL / BLOCK_SIZE_IN_CELL

        private fun randomIndex() = (1..LINE_SIZE_IN_CELL * LINE_SIZE_IN_CELL).random()
    }

    init {
        cells = generateCells(difficulty)

        mVersions = cells
            .map { it.value to if (it.value.isVisible) it.value.number else 0 }
            .associateBy(keySelector = { it.first }, valueTransform = { it.second })
            .toMutableMap()
    }

    private fun generateCells(difficulty: Difficulty): Map<Position, Cell> {
        val traverseIterator = (1..LINE_SIZE_IN_CELL * LINE_SIZE_IN_CELL)
            .map { Position(it) }
            .sortedWith(Position.getRandomComparator())
            .listIterator()

        var node = Node(parent = null, Position(0), possibles = setOf(0))
        var fromRetToParent = false

        while (traverseIterator.hasNext())
            try {
                if (!fromRetToParent) {
                    val position = traverseIterator.next()
                    val mapOfValues = getMapFromNodeChain(node)
                    node = Node(
                        parent = node, position = position, possibles = mapOfValues.leftInCol(position).intersect(
                            mapOfValues.leftInRow(position).intersect(
                                mapOfValues.leftInBlk(position)
                            )
                        )
                    )
                } else fromRetToParent = false
                node.number = node.possibles.minus(node.excluded).random()
            } catch (e: NoSuchElementException) {
                fromRetToParent = true
                node = node.retToParent()
                traverseIterator.previous()
            }

        val visiblePositions = generateVisiblePositions(Difficulty.getClues(difficulty))

        return getMapFromNodeChain(node).map {
            it.key to Cell(
                isVisible = visiblePositions.contains(it.key.index),
                number = it.value
            )
        }.toMap()
    }

    private fun generateVisiblePositions(needToOpen: Int): Set<Int> {
        val positionList = (1..LINE_SIZE_IN_CELL * LINE_SIZE_IN_CELL).map { Position(it) }
        val opened = mutableSetOf<Int>()
        while (needToOpen - opened.size > 0) {
            opened += when((0..1).random()) {
                0 -> getWithDiagonallyOppositeIndexList(positionList.random())
                else -> setOf(positionList.random().index)
            }
        }
        return opened.take(needToOpen).toSet()
    }

    private fun getMapFromNodeChain(currentNode: Node): Map<Position, Int> {
        var node = currentNode
        val result = mutableMapOf(node.position to node.number)
        while (true) {
            node.parent?.let {
                if (it.number > 0) result[it.position] = it.number
                node = it
            } ?: break
        }
        return result
    }

    fun noMoreMoves(): Boolean =
        cells.values.all { (versions[it] ?: 0) > 0 } && isAllVersionsConsistent()

    private fun isAllVersionsConsistent(): Boolean {
        if (versions.any { it.value == 0 }) return false
        val mvCells = mutableMapOf<Position, Int>()
        versions.forEach { vEntry ->
            val cell = vEntry.key
            val version = vEntry.value
            val index = cells.filter { it.value == cell }.map { it.key }.first()
            mvCells[index] = if (!cell.isVisible) version else cell.number
        }

        val vCells = mvCells.toMap()
        return vCells
            .filter { it.key.col == it.key.row }
            .none {
                with(vCells) {
                    this.leftInRow(it.key).isNotEmpty() ||
                            this.leftInCol(it.key).isNotEmpty() ||
                            this.leftInBlk(it.key).isNotEmpty()
                }
            }
    }

    fun setVersion(index: Int, version: Int) = when (index) {
        0 -> cells
            .forEach { mVersions[it.value] = 0 }

        else -> cells
            .filter { it.key.index == index && !it.value.isVisible }
            .firstNotNullOf { mVersions[it.value] = version }
    }

}


// extension functions
private fun Map<Position, Int>.leftIn(
    position: Position,
    rangeLimit: Int,
    lambda: (pos: Position) -> Int
): Set<Int> {
    val given = lambda(position)
    return (1..rangeLimit).minus(
        this
            .filter { lambda(it.key) == given }
            .values
            .toSet()
    ).toSet()
}

private fun Map<Position, Int>.leftInCol(position: Position) =
    leftIn(position, LINE_SIZE_IN_CELL) { pos: Position -> pos.col }

private fun Map<Position, Int>.leftInRow(position: Position) =
    leftIn(position, LINE_SIZE_IN_CELL) { pos: Position -> pos.row }

private fun Map<Position, Int>.leftInBlk(position: Position) =
    leftIn(position, BLOCK_SIZE_IN_CELL * BLOCK_SIZE_IN_CELL) { pos: Position -> pos.blk }