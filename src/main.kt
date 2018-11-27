
import java.awt.Color
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.Timer
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
    public var cells = Array<TetrisColor?>(width * height, { _ -> null })

    fun clear() {
        for (i in 0 until cells.size) {
            cells[i] = null
        }
    }

    fun tryAddFigure(figure: Figure): Boolean {
        var noOverlaps = true
        var offset = (width - figure.width) / 2  // when figure.width is even then shift to one cell left
        var figureOffset = 0
        for (y in 0 until figure.height) {
            for (x in 0 until figure.width) {
                noOverlaps = noOverlaps and (cells[offset] == null)
                cells[offset] = if (figure.cells[figureOffset]) figure.color else null
                ++offset
                ++figureOffset
            }
            offset += width - figure.width
        }
        return noOverlaps
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

    JFrame.setDefaultLookAndFeelDecorated(true)
    val frame = JFrame("Tetris")
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.isResizable = false
    val drawer = SwingTetrisAreaDrawer(area, 30, 2)
    frame.setSize(drawer.pxWidth, drawer.pxHeight)

    frame.add(object : JPanel() {
        override fun paintComponent(g: Graphics) = drawer.draw(g)
    })

    var figureIndex = 0
    val ticker = Timer(1000, {
        area.clear()
        println(area.tryAddFigure(COMMON_FIGURES[figureIndex]))
        figureIndex = (figureIndex + 1) % COMMON_FIGURES.size
        frame.repaint()
    })
    ticker.start()

    frame.isVisible = true
}
