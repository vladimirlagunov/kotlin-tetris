package org.github.werehuman.tetris

import org.github.werehuman.tetris.impl.TetrisArea
import org.github.werehuman.tetris.impl.TetrisColor
import java.awt.Color
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants
import kotlin.system.exitProcess


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

    val noDirection = 0
    val leftDirection = 1
    val rightDirection = 2
    val downDirection = 3

    // TODO where is volatile?
    val currentDirection = AtomicInteger(noDirection)
    val rotatePressed = AtomicBoolean(false)
    val nextCycleMillis = AtomicLong(0)
    val millisBetweenCycles = 80

    frame.addKeyListener(object : KeyListener {
        override fun keyPressed(e: KeyEvent) {
            when (e.keyCode) {
                KeyEvent.VK_LEFT -> currentDirection.set(leftDirection)
                KeyEvent.VK_RIGHT -> currentDirection.set(rightDirection)
                KeyEvent.VK_DOWN -> currentDirection.set(downDirection)
                KeyEvent.VK_SPACE -> {
                    area.moveFigureDownUntilEnd()
                    nextCycleMillis.set(System.nanoTime() / 1_000_000 + millisBetweenCycles)
                    frame.repaint()
                }
                KeyEvent.VK_UP -> {
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
                KeyEvent.VK_UP -> rotatePressed.set(false)
                KeyEvent.VK_Q -> exitProcess(0)
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
        // accumulative proceed count
        val speedUpMilestonesIter = intArrayOf(20, 40, 80, 160, 320).iterator()
        var speedUpMilestone = speedUpMilestonesIter.nextInt()
        var running = true
        while (running) {
            Thread.sleep(Math.max(0, nextCycleMillis.get() - System.nanoTime() / 1_000_000))
            nextCycleMillis.set(System.nanoTime() / 1_000_000 + millisBetweenCycles)
            var somethingHappened: Boolean
            synchronized(monitor) {
                somethingHappened = when (currentDirection.get()) {
                    leftDirection -> area.moveFigureLeft()
                    rightDirection -> area.moveFigureRight()
                    downDirection -> area.moveFigureDown()
                    else -> false
                }
                if (++stepFromLastProceed == stepsBetweenProceeding) {
                    somethingHappened = true
                    if (area.tryProceed()) {
                        stepFromLastProceed = 0
                    } else {
                        println("Game over")
                        running = false
                    }
                    ++totalProceeds
                    if (totalProceeds % speedUpMilestone == 0 && stepsBetweenProceeding > 1) {
                        --stepsBetweenProceeding
                        if (speedUpMilestonesIter.hasNext()) {
                            speedUpMilestone += speedUpMilestonesIter.nextInt()
                        }
                    }
                }
            }
            if (somethingHappened) {
                frame.repaint()
            }
        }
    }
    thread.start()

    frame.isVisible = true
}
