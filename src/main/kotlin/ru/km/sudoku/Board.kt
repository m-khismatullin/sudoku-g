package ru.km.sudoku

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import ru.km.sudoku.Board.Companion.BLOCK_SIZE_IN_CELL
import ru.km.sudoku.Board.Companion.LINE_SIZE_IN_CELL
import ru.km.sudoku.Position.Companion.getWithDiagonallyOppositeIndexList

open class Board {
    private lateinit var _versions: MutableMap<Cell, Int>
    val versions: Map<Cell, Int>
        get() = _versions.toMap()
    private lateinit var _cells: Map<Position, Cell>
    val cells: Map<Position, Cell>
        get() = _cells.toMap()
    private val traverseList = (1..LINE_SIZE_IN_CELL * LINE_SIZE_IN_CELL)
        .map { Position(it) }
        .sortedWith(Position.getRandomComparator())
    private val lastPosition: Position
        get() = traverseList.last()
    private val nodeChannel = Channel<Node>()

    companion object {
        const val LINE_SIZE_IN_CELL = 9
        const val BLOCK_SIZE_IN_CELL = 3
        const val BLOCKS_IN_LINE = LINE_SIZE_IN_CELL / BLOCK_SIZE_IN_CELL
    }

    fun noMoreMoves(): Boolean =
        cells.values.all { (versions[it] ?: 0) > 0 } && isAllVersionsConsistent()

    fun setVersion(index: Int, version: Int) = when (index) {
        0 -> cells
            .forEach { _versions[it.value] = 0 }

        else -> cells
            .filter { it.key.index == index && !it.value.isVisible }
            .firstNotNullOfOrNull { _versions[it.value] = version }
    }

    suspend operator fun invoke(difficulty: Difficulty) {
        _cells = generateCells(difficulty)
        _versions = cells
            .map { it.value to if (it.value.isVisible) it.value.number else 0 }
            .associateBy(keySelector = { it.first }, valueTransform = { it.second })
            .toMutableMap()
    }

    private suspend fun generateCells(difficulty: Difficulty): Map<Position, Cell> {
        val node = calcNode()
        val state = node.getValuesFromNodeChain()
        val visiblePositions = generateVisiblePositions(state, Difficulty.getClues(difficulty))

        return state.map {
            it.key to Cell(
                isVisible = visiblePositions.contains(it.key.index),
                number = it.value
            )
        }.toMap()
    }

    private suspend fun calcNode(): Node {
        val job = CoroutineScope(Dispatchers.Default).launch {
            calcNextNode(Node(parent = null, Position(0), 0))
            println("***********************")
        }

        return CoroutineScope(Dispatchers.Default).async {
            lateinit var result: Node

            for (node in nodeChannel) {
                result = node
                nodeChannel.close()
            }

            job.cancel()

            println("qqqqqqqqqqqqqqqqqq")

            return@async result
        }.await()
    }

    private suspend fun calcNextNode(parent: Node) {
        val index = (parent.parent?.let { traverseList.indexOf(parent.position) + 1 } ?: 0)
        val position = traverseList[index]
        val state = parent.getValuesFromNodeChain()
        val possibles = state.leftInPos(position).toList().shuffled()

        println("${parent.position}:${possibles.size}")

        possibles.forEach {
            coroutineScope {
                launch {
                    val node = Node(parent, position, it)
                    if (node.position == lastPosition) {
                        println("${nodeChannel.isClosedForSend}:${nodeChannel.isClosedForReceive}")
                        nodeChannel.send(node)
                        throw CancellationException()
                    } else calcNextNode(node)
                }
            }
        }
    }

    private fun generateVisiblePositions(state: Map<Position, Int>, needToOpen: Int): Set<Int> {
        val positionList = state.keys.toMutableList()
        val opened = mutableSetOf<Int>()
        var possibleState = state

        while (opened.size - needToOpen <= 0 && positionList.size != 0) {
            val position = positionList.random()
            setOf(
                positionList.firstOrNull { getWithDiagonallyOppositeIndexList(position).random() == it.index }
                    ?: position,
                position,
            ).forEach {
                if (isStateAdequate(possibleState, it)) {
                    possibleState = possibleState.minus(position)
                    opened += it.index
                }
                positionList -= position
            }
        }

        return opened.take(needToOpen).toSet()
    }

    private fun isStateAdequate(state: Map<Position, Int>, position: Position): Boolean {
        val possibleState = state.minus(position)
        return possibleState.leftInPos(position).size == 1
    }

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

private fun Map<Position, Int>.leftInPos(position: Position) =
    leftInCol(position).intersect(leftInRow(position).intersect(leftInBlk(position)))