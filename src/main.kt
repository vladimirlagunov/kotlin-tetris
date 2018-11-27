import java.awt.Color
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants


enum class TetrisColor {
    RED, YELLOW, GREEN, BLUE, MAGENTA, CYAN, WHITE
}


class TetrisArea constructor(public val width: Int, public val height: Int) {
    init {
        check(width > 0)
        check(height > 0)
    }

    // TODO encapsulation
    public var cells = Array<TetrisColor?>(width * height, { _ -> null })
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
    area.cells[1] = TetrisColor.RED
    area.cells[2] = TetrisColor.YELLOW
    area.cells[3] = TetrisColor.GREEN
    area.cells[5] = TetrisColor.BLUE
    area.cells[7] = TetrisColor.MAGENTA
    area.cells[11] = TetrisColor.WHITE
    area.cells[13] = TetrisColor.RED
    area.cells[17] = TetrisColor.YELLOW
    area.cells[19] = TetrisColor.GREEN
    area.cells[23] = TetrisColor.BLUE
    area.cells[29] = TetrisColor.MAGENTA
    area.cells[31] = TetrisColor.WHITE

    JFrame.setDefaultLookAndFeelDecorated(true)
    val frame = JFrame("Tetris")
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.isResizable = false
    val drawer = SwingTetrisAreaDrawer(area, 30, 2)
    frame.setSize(drawer.pxWidth, drawer.pxHeight)

    frame.add(object : JPanel() {
        override fun paintComponent(g: Graphics) = drawer.draw(g)
    })

    frame.isVisible = true
}
