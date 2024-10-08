package ru.km.sudoku

import ru.km.sudoku.Board.Companion.BLOCKS_IN_LINE
import ru.km.sudoku.Board.Companion.CELLS_IN_BLOCK
import ru.km.sudoku.Board.Companion.CELLS_IN_LINE

class Position(val index: Int) {
    val col: Int
        get() = (index - 1) % CELLS_IN_LINE + 1
    val row: Int
        get() = (index - 1) / CELLS_IN_LINE + 1
    val blk: Int
        get() = ((row - 1) / CELLS_IN_BLOCK) * BLOCKS_IN_LINE +
                (col - 1) / CELLS_IN_BLOCK + 1

    override fun toString(): String {
        return "$index"
    }

    companion object {
        private val comparatorStandard = Comparator<Position> { p1, p2 ->
            when {
                p1.index == p2.index -> 0
                p1.index < p2.index -> -1
                else -> 1
            }
        }

        private val comparatorUpDown = Comparator<Position> { p1, p2 ->
            when {
                p1.index == p2.index -> 0
                p1.col < p2.col -> -1
                p1.col == p2.col && p1.row < p2.row -> -1
                else -> 1
            }
        }

        private val comparatorBlock = Comparator<Position> { p1, p2 ->
            when {
                p1.index == p2.index -> 0
                p1.blk < p2.blk -> -1
                p1.blk == p2.blk && p1.index < p2.index -> -1
                else -> 1
            }
        }

        private val comparatorOddEvenInRow = Comparator<Position> { p1, p2 ->
            when {
                p1.index == p2.index -> 0
                p1.row < p2.row -> -1
                p1.row == p2.row && p1.index % 2 == 1 && p2.index % 2 == 0 -> -1
                p1.row == p2.row && p1.index % 2 == p2.index % 2 && p1.index < p2.index -> -1
                else -> 1
            }
        }

        private val comparators = setOf(
            comparatorStandard, comparatorUpDown, comparatorBlock, comparatorOddEvenInRow
        )

        fun getRandomComparator() = comparators.random()
        fun getStandardComparator() = comparatorStandard
    }
}