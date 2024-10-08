package ru.km.sudoku

enum class Difficulty {
    EASY, INTERMEDIATE, HARD, INCREDIBLE;

    companion object {
        fun getClues(difficulty: Difficulty) = when (difficulty) {
            EASY -> (36..45)
            INTERMEDIATE -> (27..35)
            HARD -> (19..26)
            INCREDIBLE -> (16..18)
        }.random()
    }
}
