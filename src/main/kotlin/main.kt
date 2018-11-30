package org.github.werehuman.tetris

import org.github.werehuman.tetris.impl.TetrisArea
import org.github.werehuman.tetris.impl.TetrisColor
import java.awt.Color
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants


class SwingTetrisAreaDrawer(private val area: TetrisArea, private val cellSize: Int, private val cellPadding: Int) {
    init {
        check(cellPadding > 0)
        check(cellSize > cellPadding)
    }

    val pxWidth = area.width * cellSize + cellPadding
    val pxHeight = area.height * cellSize + cellPadding

    fun draw(graphics: Graphics) {
        graphics.color = Color.black
        graphics.fillRect(0, 0, pxWidth, pxHeight)

        var cellPosition = 0
        for (y in 0 until area.height) {
            for (x in 0 until area.width) {
                area.getCell(cellPosition++)?.let {
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
        var stepsBetweenProceeding = 10
        var stepFromLastProceed = 0
        var totalProceeds = 0
        val proceedsBetweenSpeedUp = 20
        var running = true
        while (running) {
            var somethingHappened: Boolean
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
