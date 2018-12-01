package org.github.werehuman.tetris.impl

import java.util.concurrent.ThreadLocalRandom

private val COMMON_FIGURES = arrayOf(
        // `L`
        Figure(3, 2, TetrisColor.RED, booleanArrayOf(
                true, false, false,
                true, true, true)),

        // Inverted `L`
        Figure(3, 2, TetrisColor.YELLOW, booleanArrayOf(
                true, true, true,
                true, false, false)),

        // `_r-`
        Figure(3, 2, TetrisColor.GREEN, booleanArrayOf(
                false, true, true,
                true, true, false)),

        // Inverted `_r-`
        Figure(3, 2, TetrisColor.BLUE, booleanArrayOf(
                true, true, false,
                false, true, true)),

        // `_|_`
        Figure(3, 2, TetrisColor.MAGENTA, booleanArrayOf(
                false, true, false,
                true, true, true)),

        // Square
        Figure(2, 2, TetrisColor.CYAN, booleanArrayOf(
                true, true,
                true, true)),

        // Most desired figure ever
        Figure(1, 4, TetrisColor.WHITE, booleanArrayOf(
                true, true, true, true)))


fun commonFigureFactory(): Figure {
    val random = ThreadLocalRandom.current()
    var figure = COMMON_FIGURES[random.nextInt(0, COMMON_FIGURES.size)]
    for (i in 0 until random.nextInt(0, 4)) {
        figure = figure.rotateClockwise()
    }
    return figure
}
