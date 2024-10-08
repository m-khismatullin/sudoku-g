package ru.km.sudoku

open class Board(difficulty: Difficulty) {
    private val mVersions: MutableMap<Cell, Int>
    val versions: Map<Cell, Int>
        get() = mVersions.toMap()
    val cells: Map<Position, Cell>

    companion object {
        const val LINE_SIZE_IN_CELL = 9
        const val BLOCK_SIZE_IN_CELL = 3
        const val BLOCKS_IN_LINE = LINE_SIZE_IN_CELL / BLOCK_SIZE_IN_CELL

        fun getIndexByRC(row: Int, col: Int) = col + (row - 1) * LINE_SIZE_IN_CELL
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
        val visiblePositions = mutableSetOf<Int>()
        val traverseList = (1..LINE_SIZE_IN_CELL * LINE_SIZE_IN_CELL)
            .map { Position(it) }
            .sortedWith(Position.getRandomComparator())
        val traverseIterator = traverseList.listIterator()

        var node = Node(parent = null, Position(0), possibles = setOf(0))
        var fromRetToParent = false

        while (traverseIterator.hasNext())
            try {
                if (!fromRetToParent) {
                    val position = traverseIterator.next()
                    val mapOfValues = getMapFromNodeChain(node)
                    node = Node(
                        parent = node, position = position, possibles = leftInCol(position, mapOfValues).intersect(
                            leftInRow(position, mapOfValues).intersect(
                                leftInBlk(position, mapOfValues)
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

        (1..Difficulty.getClues(difficulty)).forEach { _ ->
            visiblePositions += randomIndex()
        }

        return getMapFromNodeChain(node).map {
            it.key to Cell(
                isVisible = visiblePositions.contains(it.key.index),
                number = it.value
            )
        }.toMap()
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

    private fun leftIn(
        position: Position,
        rangeLimit: Int,
        mapOfValues: Map<Position, Int>,
        lambda: (pos: Position) -> Int
    ): Set<Int> {
        val given = lambda(position)
        return (1..rangeLimit).minus(
            mapOfValues
                .filter { lambda(it.key) == given }
                .values
                .toSet()
        ).toSet()
    }

    private fun leftInCol(position: Position, mapOfValues: Map<Position, Int>) =
        leftIn(position, LINE_SIZE_IN_CELL, mapOfValues) { pos: Position -> pos.col }

    private fun leftInRow(position: Position, mapOfValues: Map<Position, Int>) =
        leftIn(position, LINE_SIZE_IN_CELL, mapOfValues) { pos: Position -> pos.row }

    private fun leftInBlk(position: Position, mapOfValues: Map<Position, Int>) =
        leftIn(position, BLOCK_SIZE_IN_CELL * BLOCK_SIZE_IN_CELL, mapOfValues) { pos: Position -> pos.blk }

    fun noMoreMoves(): Boolean =
        cells.values.all { (versions[it] ?: 0) > 0 } && isAllVersionsConsistent()

    private fun isAllVersionsConsistent(): Boolean {
        if (versions.any { it.value == 0 }) return false
        val vCells = mutableMapOf<Position, Int>()
        versions.forEach { vEntry ->
            val cell = vEntry.key
            val version = vEntry.value
            val index = cells.filter { it.value == cell }.map { it.key }.first()
            vCells[index] = if (!cell.isVisible) version else cell.number
        }
        return vCells
            .filter { it.key.col == it.key.row }
            .none {
                leftInRow(it.key, vCells.toMap()).isNotEmpty() ||
                        leftInCol(it.key, vCells.toMap()).isNotEmpty() ||
                        leftInBlk(it.key, vCells.toMap()).isNotEmpty()
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