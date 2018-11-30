package org.github.werehuman.tetris.impl

enum class TetrisColor {
    RED, YELLOW, GREEN, BLUE, MAGENTA, CYAN, WHITE
}

data class Figure(val width: Int, val height: Int, val color: TetrisColor, val cells: BooleanArray) {
    init {
        check(width > 0)
        check(height > 0)
        check(cells.size == width * height)
    }

    internal fun rotateClockwise(): Figure {
        val newCells = BooleanArray(cells.size) { false }
        var newCellsIndex = 0
        for (offset in 0 until width) {
            for (position in (cells.size - width) downTo 0 step width) {
                newCells[newCellsIndex] = cells[position + offset]
                ++newCellsIndex
            }
        }
        return Figure(width = height, height = width, color = color, cells = newCells)
    }
}

data class FigureWithPosition(val figure: Figure, val top: Int, val left: Int) {
    internal fun rotateClockwise(): FigureWithPosition {
        val newFigure = figure.rotateClockwise()
        return FigureWithPosition(
                newFigure,
                top = top + (figure.height - newFigure.height) / 2,
                left = left + (figure.width - newFigure.width) / 2)
    }
}
