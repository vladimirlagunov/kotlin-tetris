package org.github.werehuman.tetris.impl

class TetrisArea constructor(
        val width: Int,
        val height: Int,
        // TODO will default cells be shared between all instances?
        private val cells: Array<TetrisColor?> = Array(width * height) { null },
        private val figureFactory: () -> Figure = ::commonFigureFactory) {

    init {
        check(width >= 4)
        check(height >= 4)
        check(cells.size == width * height)
    }

    private var currentFigure: FigureWithPosition? = null

    fun getCell(offset: Int): TetrisColor? = cells[offset]

    fun start() {
        check(trySpawnFigure())
    }

    fun tryProceed(): Boolean {
        return if (moveFigureDown()) {
            true
        } else {
            removeLines()
            currentFigure = null
            trySpawnFigure()
        }
    }

    fun moveFigureLeft(): Boolean = tryMove(horizontal = -1, vertical = 0)

    fun moveFigureRight(): Boolean = tryMove(horizontal = 1, vertical = 0)

    private fun moveFigureDown(): Boolean = tryMove(horizontal = 0, vertical = 1)

    fun moveFigureDownUntilEnd(): Boolean {
        while (moveFigureDown()) {
        }
        removeLines()
        currentFigure = null
        return trySpawnFigure()
    }

    fun rotateClockwise(): Boolean {
        val it = currentFigure ?: return false
        val newFigure = it.rotateClockwise()
        removeFigure()
        if (canSafelyPutFigure(newFigure)) {
            putFigure(newFigure)
            return true
        } else {
            putFigure(it)
            return false
        }
    }

    internal fun trySpawnFigure(): Boolean {
        check(currentFigure == null)
        return tryAddFigure(figureFactory.invoke())
    }

    internal fun tryAddFigure(figure: Figure): Boolean {
        val candidate = FigureWithPosition(
                figure,
                top = 0,
                left = (width - figure.width) / 2  // when figure.width is even then shift to one cell left
        )

        if (canSafelyPutFigure(candidate)) {
            putFigure(candidate)
            return true
        } else {
            return false
        }
    }

    internal fun canSafelyPutFigure(info: FigureWithPosition): Boolean {
        if (info.left < 0 || info.left + info.figure.width > width || info.top < 0 || info.top + info.figure.height > height) {
            return false
        }
        var offset = info.left + info.top * width
        var figureOffset = 0
        for (y in info.top until (info.top + info.figure.height)) {
            for (x in 0 until info.figure.width) {
                if (info.figure.cells[figureOffset] && cells[offset] != null) {
                    return false
                }
                ++offset
                ++figureOffset
            }
            offset += width - info.figure.width
        }
        return true
    }

    internal fun putFigure(info: FigureWithPosition) {
        var offset = info.left + info.top * width
        var figureOffset = 0
        for (y in 0 until info.figure.height) {
            for (x in 0 until info.figure.width) {
                if (info.figure.cells[figureOffset]) {
                    cells[offset] = info.figure.color
                }
                ++offset
                ++figureOffset
            }
            offset += width - info.figure.width
        }
        currentFigure = info
    }

    internal fun removeFigure() {
        val it = currentFigure ?: return
        var offset = it.left + it.top * width
        var figureOffset = 0
        for (y in 0 until it.figure.height) {
            for (x in 0 until it.figure.width) {
                if (it.figure.cells[figureOffset]) {
                    cells[offset] = null
                }
                ++offset
                ++figureOffset
            }
            offset += width - it.figure.width
        }
        currentFigure = null
    }

    internal fun tryMove(horizontal: Int = 0, vertical: Int = 0): Boolean {
        val it = currentFigure ?: return true
        val candidate = FigureWithPosition(it.figure, left = it.left + horizontal, top = it.top + vertical)
        removeFigure()
        if (canSafelyPutFigure(candidate)) {
            putFigure(candidate)
            return true
        } else {
            putFigure(it)
            return false
        }
    }

    internal fun removeLines() {
        var offset = width * (height - 1)
        var fromOffset = offset
        while (fromOffset >= 0) {
            var shouldKeepOffset = true
            for (x in 0 until width) {
                cells[offset] = cells[fromOffset]
                shouldKeepOffset = shouldKeepOffset && (cells[offset] != null)
                ++fromOffset
                ++offset
            }
            fromOffset -= width * 2
            if (shouldKeepOffset) {
                offset -= width
            } else {
                offset -= width * 2
            }
        }
        offset += width
        while (offset > 0) {
            cells[--offset] = null
        }
    }
}
