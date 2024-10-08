package ru.km.sudoku

class Cell(
    val isVisible: Boolean = false,
    val number: Int = 0,
) {
    override fun toString() = when {
        !isVisible -> " "
        else -> number.toString()
    }
}
