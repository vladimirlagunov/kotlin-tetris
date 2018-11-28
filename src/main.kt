import java.awt.Color
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants


enum class TetrisColor {
    RED, YELLOW, GREEN, BLUE, MAGENTA, CYAN, WHITE
}


data class Figure(public val width: Int, public val height: Int, public val color: TetrisColor, public val cells: BooleanArray) {
    init {
        check(width > 0)
        check(height > 0)
        check(cells.size == width * height)
    }

    fun rotateClockwise(): Figure {
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

data class FigureWithPosition(public val figure: Figure, public val top: Int, public val left: Int) {
    fun rotateClockwise(): FigureWithPosition {
        val newFigure = figure.rotateClockwise()
        return FigureWithPosition(
                newFigure,
                top = top + (figure.height - newFigure.height) / 2,
                left = left + (figure.width - newFigure.width) / 2)
    }
}


val COMMON_FIGURES = arrayOf(
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


class TetrisArea constructor(public val width: Int, public val height: Int) {
    init {
        check(width >= 4)
        check(height >= 4)
    }

    // TODO encapsulation
    public var cells = Array<TetrisColor?>(width * height) { null }
    private var currentFigure: FigureWithPosition? = null

    fun clear() {
        for (i in 0 until cells.size) {
            cells[i] = null
        }
    }

    fun start() {
        check(trySpawnFigure())
    }

    fun tryProceed(): Boolean {
        return if (tryMove(horizontal = 0, vertical = 1)) {
            true
        } else {
            applyFigure()
            trySpawnFigure()
        }
    }

    fun moveFigureLeft() {
        tryMove(horizontal = -1, vertical = 0)
    }

    fun moveFigureRight() {
        tryMove(horizontal = 1, vertical = 0)
    }

    fun moveFigureDown() {
        tryMove(horizontal = 0, vertical = 1)
    }

    fun rotateClockwise(): Boolean {
        val it = currentFigure
        if (it == null) {
            return false
        }
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

    private fun trySpawnFigure(): Boolean {
        check(currentFigure == null)
        val random = ThreadLocalRandom.current()
        var figure = COMMON_FIGURES[random.nextInt(0, COMMON_FIGURES.size)]
        for (i in 0 until random.nextInt(0, 4)) {
            figure = figure.rotateClockwise()
        }
        return tryAddFigure(figure)
    }

    private fun tryAddFigure(figure: Figure): Boolean {
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

    private fun canSafelyPutFigure(info: FigureWithPosition): Boolean {
        if (info.left < 0 || info.left + info.figure.width > width || info.top < 0 || info.top + info.figure.height >= height) {
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

    private fun putFigure(info: FigureWithPosition) {
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

    private fun removeFigure() {
        val it = currentFigure
        if (it == null) {
            return
        }
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

    private fun tryMove(horizontal: Int = 0, vertical: Int = 0): Boolean {
        val it = currentFigure
        if (it == null) {
            return true
        }
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

    private fun applyFigure() {
        val it = currentFigure
        if (it == null) {
            return
        }
        var offset = width * (height - 1)
        var fromOffset = offset
        while (fromOffset >= 0) {
            var shouldKeepOffset = true
            for (x in 0 until width) {
                shouldKeepOffset = shouldKeepOffset && (cells[offset] != null)
                cells[offset] = cells[fromOffset]
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
        while (offset >= 0) {
            cells[offset] = null
            --offset
        }
        currentFigure = null
    }
}


class SwingTetrisAreaDrawer(private val area: TetrisArea, private val cellSize: Int, private val cellPadding: Int) {
    init {
        check(cellPadding > 0)
        check(cellSize > cellPadding)
    }

    public val pxWidth = area.width * cellSize + cellPadding
    public val pxHeight = area.height * cellSize + cellPadding

    fun prepare(panel: JPanel) {
        panel.setSize(pxWidth, pxHeight)
    }

    fun draw(graphics: Graphics) {
        graphics.color = Color.black
        graphics.fillRect(0, 0, pxWidth, pxHeight)

        var cellPosition = 0
        for (y in 0 until area.height) {
            for (x in 0 until area.width) {
                area.cells[cellPosition++]?.let {
                    graphics.color = when (it) {
                        TetrisColor.RED -> Color.RED
                        TetrisColor.YELLOW -> Color.YELLOW
                        TetrisColor.GREEN -> Color.GREEN
                        TetrisColor.BLUE -> Color.BLUE
                        TetrisColor.MAGENTA -> Color.MAGENTA
                        TetrisColor.CYAN -> Color.CYAN
                        TetrisColor.WHITE -> Color.WHITE
                    }
                    graphics.fillRect(
                            x * cellSize + cellPadding,
                            y * cellSize + cellPadding,
                            cellSize - cellPadding,
                            cellSize - cellPadding)
                }
            }
        }
    }
}


fun main(args: Array<String>) {
    val area = TetrisArea(10, 20)
    area.start()

    JFrame.setDefaultLookAndFeelDecorated(true)
    val frame = JFrame("Tetris")
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.isResizable = false
    val drawer = SwingTetrisAreaDrawer(area, 30, 2)
    frame.setSize(drawer.pxWidth, drawer.pxHeight)

    val monitor = object {}

    val noDirection = 0;
    val leftDirection = 1;
    val rightDirection = 2;
    val downDirection = 3;
    val currentDirection = AtomicInteger(noDirection)
    val rotatePressed = AtomicBoolean(false)

    frame.addKeyListener(object : KeyListener {
        override fun keyPressed(e: KeyEvent) {
            when (e.keyCode) {
                KeyEvent.VK_LEFT -> currentDirection.set(leftDirection)
                KeyEvent.VK_RIGHT -> currentDirection.set(rightDirection)
                KeyEvent.VK_DOWN -> currentDirection.set(downDirection)
                KeyEvent.VK_SPACE, KeyEvent.VK_UP -> {
                    var somethingChanged = false
                    if (!rotatePressed.get()) synchronized(monitor) {
                        somethingChanged = area.rotateClockwise()
                        rotatePressed.set(true)
                    }
                    if (somethingChanged) {
                        frame.repaint()
                    }
                }
            }
        }

        override fun keyReleased(e: KeyEvent) {
            when (e.keyCode) {
                KeyEvent.VK_LEFT, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT -> currentDirection.set(noDirection)
                KeyEvent.VK_SPACE, KeyEvent.VK_UP -> rotatePressed.set(false)
            }
        }

        override fun keyTyped(e: KeyEvent) {}
    })
    frame.add(object : JPanel() {
        override fun paintComponent(g: Graphics) = drawer.draw(g)
    })

    val thread = Thread {
        var stepsBetweenProceeding = 20
        var stepFromLastProceed = 0
        var totalProceeds = 0
        val proceedsBetweenSpeedUp = 20
        var running = true
        while (running) {
            var somethingHappened = false
            synchronized(monitor) {
                 somethingHappened = when (currentDirection.get()) {
                    leftDirection -> {
                        area.moveFigureLeft()
                        true
                    }
                    rightDirection -> {
                        area.moveFigureRight()
                        true
                    }
                    downDirection -> {
                        area.moveFigureDown()
                        true
                    }
                    else -> false
                }
                if (++stepFromLastProceed == stepsBetweenProceeding) {
                    somethingHappened = true
                    if (area.tryProceed()) {
                        stepFromLastProceed = 0
                    } else {
                        running = false
                    }
                    ++totalProceeds
                    if (totalProceeds % proceedsBetweenSpeedUp == 0 && stepsBetweenProceeding > 1) {
                        --stepsBetweenProceeding
                    }
                }
            }
            if (somethingHappened) {
                frame.repaint()
            }
            Thread.sleep(80)
        }
    }
    thread.start()

    frame.isVisible = true
}
