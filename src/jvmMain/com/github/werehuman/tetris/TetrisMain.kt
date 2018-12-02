package com.github.werehuman.tetris

import com.github.werehuman.tetris.impl.CurrentAction
import com.github.werehuman.tetris.impl.TetrisColor
import com.github.werehuman.tetris.impl.TetrisController
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import kotlin.system.exitProcess

class SwingTetrisAreaDrawer(private val controller: TetrisController) {
    private val borderSize: Int = 2

    private data class Clipping(val cellSize: Int, val cellPadding: Int, val marginLeft: Int, val marginTop: Int)

    private fun calculateClipping(bounds: Rectangle): Clipping {
        val cellSize = Math.min((bounds.width - borderSize * 2) / controller.width, bounds.height / controller.height)
        return Clipping(
                cellSize = cellSize,
                cellPadding = (cellSize * 0.1).toInt(),
                marginLeft = bounds.x + Math.max(0, (bounds.width - controller.width * cellSize) / 2 - borderSize),
                marginTop = bounds.y + Math.max(0, (bounds.height - controller.height * cellSize) / 2 - borderSize))
    }

    fun draw(graphics: Graphics2D) {
        val bounds = graphics.clipBounds
        val (cellSize, cellPadding, marginLeft, marginTop) = calculateClipping(bounds)
        val pxWidth = controller.width * cellSize + cellPadding
        val pxHeight = controller.height * cellSize + cellPadding

        graphics.color = Color.black
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)

        graphics.stroke = BasicStroke(borderSize.toFloat())
        graphics.color = Color.gray
        graphics.drawRect(marginLeft + borderSize / 2, marginTop + borderSize / 2, pxWidth + borderSize, pxHeight + borderSize)

        graphics.stroke = BasicStroke(1.0f)

        var cellPosition = 0
        for (y in 0 until controller.height) {
            for (x in 0 until controller.width) {
                controller.getCell(cellPosition++)?.let {
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
                            x * cellSize + cellPadding + marginLeft + borderSize,
                            y * cellSize + cellPadding + marginTop + borderSize,
                            cellSize - cellPadding,
                            cellSize - cellPadding)
                }
            }
        }
    }
}


actual object TetrisMain {
    actual fun play(width: Int, height: Int) {
        JFrame.setDefaultLookAndFeelDecorated(true)
        val frame = JFrame("Tetris")
        frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        frame.setSize(400, 800)

        val controller = TetrisController(width, height, System.nanoTime() / 1_000_000)
        val drawer = SwingTetrisAreaDrawer(controller)
        frame.addKeyListener(object : KeyListener {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_DOWN -> controller.pressedAction = CurrentAction.Down
                    KeyEvent.VK_LEFT -> controller.pressedAction = CurrentAction.Left
                    KeyEvent.VK_RIGHT -> controller.pressedAction = CurrentAction.Right
                    KeyEvent.VK_SPACE -> controller.pressedAction = CurrentAction.DownUntilEnd
                    KeyEvent.VK_UP -> controller.pressedAction = CurrentAction.Rotate
                }
            }

            override fun keyReleased(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_DOWN,
                    KeyEvent.VK_LEFT,
                    KeyEvent.VK_RIGHT,
                    KeyEvent.VK_SPACE,
                    KeyEvent.VK_UP
                    -> controller.pressedAction = null
                    KeyEvent.VK_Q -> {
                        frame.isVisible = false
                        exitProcess(0)
                    }
                }
            }

            override fun keyTyped(e: KeyEvent) {}
        })
        frame.add(object : JPanel() {
            override fun paintComponent(g: Graphics) {
                check(g is Graphics2D)
                // TODO return clone of area instead of reference. It may cause data races.
                drawer.draw(g)
            }
        })

        frame.isVisible = true

        frame.repaint()
        while (true) {
            val (durationMillis, changed) = controller.proceed(System.nanoTime() / 1_000_000) ?: break
            if (changed) {
                frame.repaint()
            }
            Thread.sleep(durationMillis)
        }
        println("Game over!")
    }
}
