package ru.km.sudoku

class TextBoard(difficulty: Difficulty) : Board(difficulty) {
    override fun toString(): String {
        val map = StringBuilder("")
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
        for (position in cells.keys.sortedWith(Position.getStandardComparator())) {
            if (position.index % LINE_SIZE_IN_CELL == 1) mapLine.append("${position.index / LINE_SIZE_IN_CELL + 1}|")
            if (cells[position]?.isVisible == true || versions[cells[position]] == 0) {
                mapLine.append(" ").append(cells[position].toString())
            } else mapLine.append("?").append(versions[cells[position]].toString())
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
        mapLine.clear()

        return map.toString()
    }

    private fun getNumFromChar(char: Char) = char - '0'

    fun setVersionForField(userInput: String) {
        val input = userInput.filter { it.isDigit() }

        if (input.isEmpty() || input.isBlank()) return

        if (input.length >= 3) {
            if (input == "000") setVersion(0, 0)
            else {
                val row = getNumFromChar(input[0])
                val col = getNumFromChar(input[1])
                val value = getNumFromChar(input[2])

                if (row in (1..9) || col in (1..9) || value in (1..9)) {
                    setVersion(Position.getIndexByRC(row, col), value)
                }
            }
        }
    }
}