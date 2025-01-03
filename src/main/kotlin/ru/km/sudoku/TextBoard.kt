package ru.km.sudoku

import ru.km.sudoku.Board.Companion.BLOCKS_IN_LINE
import ru.km.sudoku.Board.Companion.BLOCK_SIZE_IN_CELL
import ru.km.sudoku.Board.Companion.LINE_SIZE_IN_CELL

class TextBoard(val board: Board) {
    override fun toString(): String {
        val map = StringBuilder("\n")
        val mapLine = StringBuilder(" ")

        (1..LINE_SIZE_IN_CELL).forEach {
            mapLine.append("  ").append(it)
            if (it % 3 == 0) mapLine.append(" ")
        }
        mapLine.append(" ")
        map.append(mapLine.append("\n"))
        mapLine.clear().append(" ")

        (1..BLOCK_SIZE_IN_CELL * 3 * BLOCKS_IN_LINE + 4).forEach { mapLine.append("-") }
        map.append(mapLine.append("\n"))

        mapLine.clear()
        for (position in board.cells.keys.sortedWith(Position.getStandardComparator())) {
            if (position.index % LINE_SIZE_IN_CELL == 1) mapLine.append("${position.index / LINE_SIZE_IN_CELL + 1}|")
            if (board.cells[position]?.isVisible == true || board.versions[board.cells[position]] == 0) {
                mapLine.append(" ").append(board.cells[position].toString())
            } else mapLine.append("?").append(board.versions[board.cells[position]].toString())
            mapLine.append(" ")
            if (position.index % BLOCK_SIZE_IN_CELL == 0) {
                mapLine.append("|")
                if (position.index % LINE_SIZE_IN_CELL == 0) {
                    mapLine.append("${position.index / LINE_SIZE_IN_CELL}")
                    map.append(mapLine).append("\n")
                    if (position.row % BLOCK_SIZE_IN_CELL == 0) {
                        mapLine.clear()
                        mapLine.append(" ")
                        (1..BLOCK_SIZE_IN_CELL * 3 * BLOCKS_IN_LINE + 4).forEach { mapLine.append("-") }
                        map.append(mapLine.append("\n"))
                    }
                    mapLine.clear()
                }
            }
        }

        mapLine.clear()
        mapLine.append(" ")
        (1..LINE_SIZE_IN_CELL).forEach {
            mapLine.append("  ").append(it)
            if (it % 3 == 0) mapLine.append(" ")
        }
        mapLine.append(" ")
        map.append(mapLine.append("\n"))

        return map.toString()
    }

    private fun getNumFromChar(char: Char) = char - '0'

    fun setVersionForField(rowColValue: String) {
        if (rowColValue == "000") board.setVersion(0, 0)
        else {
            val row = getNumFromChar(rowColValue[0])
            val col = getNumFromChar(rowColValue[1])
            val value = getNumFromChar(rowColValue[2])

            if (row in (1..9) || col in (1..9) || value in (1..9)) {
                board.setVersion(Position.getIndexByRC(row, col), value)
            }
        }
    }

    fun noMoreMoves(): Boolean = board.noMoreMoves()
}