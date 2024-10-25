package ru.km.sudoku

suspend fun main() {
    print(
        """
        Укажите уровень сложности (4 - невероятный, 3 - сложный, 2 - средний, иначе - легкий): 
        """.trimIndent()
    )
    val difficulty = when (readln()) {
        "4" -> Difficulty.INCREDIBLE
        "3" -> Difficulty.HARD
        "2" -> Difficulty.INTERMEDIATE
        else -> Difficulty.EASY
    }

    val textBoard = TextBoard(Board.withDifficulty(difficulty))
    println(textBoard)

    while (!textBoard.noMoreMoves()) {
        print("Укажите ваш ход (или что-то иное для вывода правил): ")

        val input = readln()
        if (input.isValidMove()) textBoard.setVersionForField(input)
        else {
            println(
                """
            
                Правила игры:
                1. необходимо указать версии значений в пустых ячейках
                2. пример указания хода: 234 означает установку значения 4 в строке 2 и колонке 3
                3. для сброса установленной версии значения в конкретной ячейке надо указать 0 в качестве значения
                4. для сброса всех установленных версий необходимо указать 000
                """.trimIndent()
            )
        }
        println(textBoard)
    }

    println("Вы разгадали судоку!")
}

private fun String.isValidMove(): Boolean =
    this.filter { it.isDigit() }.let { it.isNotEmpty() && it.isNotBlank() && length >= 3 }
