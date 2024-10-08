package ru.km.sudoku

fun main() {
    print(
        """
        Укажите уровень сложности (4 - невероятный, 3 - сложный, 2 - средний, иначе - легкий): 
        """.trimIndent()
    )
    val input = readln()
    val difficulty = when (input) {
        "4" -> Difficulty.INCREDIBLE
        "3" -> Difficulty.HARD
        "2" -> Difficulty.INTERMEDIATE
        else -> Difficulty.EASY
    }

    val board = TextBoard(difficulty)
    println(board)

    while (true) {
        if (board.noMoreMoves()) break
        else print(
            """
            Ход указывается в формате: "ряд""колонка""значение" без разделителей
            Для отмены версии укажите в качестве значения "0". Для отмены всех версий укажите для ряда и колонки значения "0"
            Укажите ваш ход: 
            """.trimIndent()
        )
        board.setVersionForField(readln())
        println(board)
    }

    println("Вы разгадали судоку!")
}