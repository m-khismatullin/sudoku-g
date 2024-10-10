package ru.km.sudoku

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import ru.km.sudoku.Board.Companion.BLOCK_SIZE_IN_CELL
import ru.km.sudoku.Board.Companion.LINE_SIZE_IN_CELL
import ru.km.sudoku.Position.Companion.getWithDiagonallyOppositeIndexList
import java.util.concurrent.ConcurrentLinkedQueue

open class Board {
    private lateinit var mVersions: MutableMap<Cell, Int>
    val versions: Map<Cell, Int>
        get() = mVersions.toMap()
    private lateinit var mCells: Map<Position, Cell>
    val cells: Map<Position, Cell>
        get() = mCells.toMap()
    private val traverseList = (1..LINE_SIZE_IN_CELL * LINE_SIZE_IN_CELL)
        .map { Position(it) }
        .sortedWith(Position.getRandomComparator())
    private val lastPosition: Position
        get() = traverseList.last()
    private val allNodes = ConcurrentLinkedQueue<Node>()

    companion object {
        const val LINE_SIZE_IN_CELL = 9
        const val BLOCK_SIZE_IN_CELL = 3
        const val BLOCKS_IN_LINE = LINE_SIZE_IN_CELL / BLOCK_SIZE_IN_CELL
    }

    fun noMoreMoves(): Boolean =
        cells.values.all { (versions[it] ?: 0) > 0 } && isAllVersionsConsistent()

    fun setVersion(index: Int, version: Int) = when (index) {
        0 -> cells
            .forEach { mVersions[it.value] = 0 }

        else -> cells
            .filter { it.key.index == index && !it.value.isVisible }
            .firstNotNullOfOrNull { mVersions[it.value] = version }
    }

    suspend operator fun invoke(difficulty: Difficulty) = initGame(difficulty)

    private suspend fun initGame(difficulty: Difficulty) {
        allNodes.clear()
        mCells = generateCells(difficulty)
        mVersions = cells
            .map { it.value to if (it.value.isVisible) it.value.number else 0 }
            .associateBy(keySelector = { it.first }, valueTransform = { it.second })
            .toMutableMap()
    }

    private suspend fun generateCells(difficulty: Difficulty): Map<Position, Cell> {
        defineChildNodes(Node(parent = null, Position(0), 0))
        val node = allNodes.first { it.position == lastPosition }

        val visiblePositions = generateVisiblePositions(Difficulty.getClues(difficulty))

        return node.getValuesFromNodeChain().map {
            it.key to Cell(
                isVisible = visiblePositions.contains(it.key.index),
                number = it.value
            )
        }.toMap()
    }

    private fun isResultAchieved(): Boolean {
        return allNodes.any { it.position == lastPosition }
    }

    private suspend fun defineChildNodes(parent: Node) {
        if (isResultAchieved()) throw CancellationException("генерация судоку выполнена")

        val index = (parent.parent?.let { traverseList.indexOf(parent.position) + 1 } ?: 0)
        val position = traverseList[index]
        val state = parent.getValuesFromNodeChain()
        val possibles = state.leftInCol(position).intersect(
            state.leftInRow(position).intersect(
                state.leftInBlk(position)
            )
        ).toList().shuffled()

        possibles.forEach {
            try {
                coroutineScope {
                    launch {
                        val node = Node(parent, position, it)
                        allNodes += node
                        defineChildNodes(node)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
            }
        }
    }

    private fun generateVisiblePositions(needToOpen: Int): Set<Int> {
        val positionList = (1..LINE_SIZE_IN_CELL * LINE_SIZE_IN_CELL).map { Position(it) }
        val opened = mutableSetOf<Int>()
        while (needToOpen - opened.size > 0) {
            opened += when ((0..1).random()) {
                0 -> getWithDiagonallyOppositeIndexList(positionList.random())
                else -> setOf(positionList.random().index)
            }
        }
        return opened.take(needToOpen).toSet()
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

fun Map<Position, Int>.leftInCol(position: Position) =
    leftIn(position, LINE_SIZE_IN_CELL) { pos: Position -> pos.col }

fun Map<Position, Int>.leftInRow(position: Position) =
    leftIn(position, LINE_SIZE_IN_CELL) { pos: Position -> pos.row }

fun Map<Position, Int>.leftInBlk(position: Position) =
    leftIn(position, BLOCK_SIZE_IN_CELL * BLOCK_SIZE_IN_CELL) { pos: Position -> pos.blk }