package com.github.werehuman.tetris

import com.github.werehuman.tetris.impl.CurrentAction
import com.github.werehuman.tetris.impl.TetrisColor
import com.github.werehuman.tetris.impl.TetrisController
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.math.abs
import kotlin.math.min

const val borderSize: Int = 2
const val keyDown = 40
const val keyLeft = 37
const val keyRight = 39
const val keySpace = 32
const val keyUp = 38


private data class Clipping(val cellSize: Int, val cellPadding: Int)

private fun calculateClipping(controller: TetrisController, win: Window): Clipping {
    val width = win.innerWidth as Int
    val height = win.innerHeight as Int
    val cellSize = kotlin.math.min(
        (width - borderSize * 2) / controller.width,
        (height - borderSize * 2) / controller.height
    )
    return Clipping(
        cellSize = cellSize,
        cellPadding = (cellSize * 0.1).toInt()
    )
}

private fun resizeCanvas(controller: TetrisController, clipping: Clipping, canvas: HTMLCanvasElement) {
    canvas.width = clipping.cellSize * controller.width
    canvas.height = clipping.cellSize * controller.height
}

private fun milliTime(): Long {
    return (js("+new Date()") as Number).toLong()
}

private fun repaint(controller: TetrisController, clipping: Clipping, canvasCtx: CanvasRenderingContext2D) {
    val cellSize = clipping.cellSize.toDouble()
    val cellPadding = clipping.cellPadding.toDouble()
    val pxWidth = controller.width * cellSize
    val pxHeight = controller.height * cellSize

    canvasCtx.fillStyle = "black"
    canvasCtx.fillRect(0.0, 0.0, pxWidth, pxHeight)

    canvasCtx.strokeStyle = "gray"
    canvasCtx.lineWidth = borderSize.toDouble()

    canvasCtx.strokeRect(borderSize / 2.0, borderSize / 2.0, pxWidth - borderSize, pxHeight - borderSize)

    canvasCtx.lineWidth = 1.0

    var cellPosition = 0
    for (y in 0 until controller.height) {
        for (x in 0 until controller.width) {
            controller.getCell(cellPosition++)?.let {
                canvasCtx.fillStyle = when (it) {
                    TetrisColor.RED -> "red"
                    TetrisColor.YELLOW -> "yellow"
                    TetrisColor.GREEN -> "green"
                    TetrisColor.BLUE -> "blue"
                    TetrisColor.MAGENTA -> "magenta"
                    TetrisColor.CYAN -> "cyan"
                    TetrisColor.WHITE -> "white"
                }
                canvasCtx.fillRect(
                    x * cellSize + cellPadding + borderSize,
                    y * cellSize + cellPadding + borderSize,
                    cellSize - cellPadding,
                    cellSize - cellPadding
                )
            }
        }
    }
}

private class MouseTracker constructor(
    event: MouseEvent,
    private val controller: TetrisController,
    clipping: Clipping
) {
    private val pxThreshold = min(controller.width, controller.height) * clipping.cellSize / 5
    private val millisThreshold = 500
    private val startX: Int = event.clientX
    private val startY: Int = event.clientY
    private val startMillis = milliTime()

    fun onMove(event: MouseEvent): CurrentAction? {
        return when {
            event.clientX <= startX - pxThreshold -> CurrentAction.Left
            event.clientX >= startX + pxThreshold -> CurrentAction.Right
            event.clientY >= startY + pxThreshold -> CurrentAction.Down
            else -> null
        }
    }

    fun onUp(event: MouseEvent): CurrentAction? {
        if (milliTime() - startMillis < millisThreshold) {
            if (controller.pressedAction == null && abs(event.clientX - startX) < pxThreshold) {
                return CurrentAction.Rotate
            }
            if (event.clientY >= startY + pxThreshold * 2) {
                return CurrentAction.DownUntilEnd
            }
        }
        return null
    }
}

actual object TetrisMain {
    actual fun play(width: Int, height: Int) {
        val controller = TetrisController(width, height, milliTime())

        val win = js("window") as Window
        val doc = js("document") as Document
        val body = doc.body ?: throw IllegalStateException("No body in html")

        doc.title = "Kotlin Tetris"

        val style = doc.createElement("style") as HTMLStyleElement
        style.innerHTML = """
        body {
            background: black;
            margin: 0;
            padding: 0;
        }
        #tetris_canvas {
            padding: 0;
            border: 0;
            margin: 0 auto;
            display: block;
        }
        """.trimIndent()

        body.appendChild(style)

        val canvas = doc.createElement("canvas") as HTMLCanvasElement
        canvas.id = "tetris_canvas"
        val canvasCtx = (canvas.getContext("2d") as CanvasRenderingContext2D?)
            ?: throw IllegalStateException("Can't create canvas context")

        body.appendChild(canvas)

        var clipping = calculateClipping(controller, win)

        win.onresize = {
            clipping = calculateClipping(controller, win)
            resizeCanvas(controller, clipping, canvas)
            repaint(controller, clipping, canvasCtx)
        }

        win.onkeydown = { e: Event ->
            check(e is KeyboardEvent)
            when (e.keyCode) {
                keyDown -> controller.pressedAction = CurrentAction.Down
                keyLeft -> controller.pressedAction = CurrentAction.Left
                keyRight -> controller.pressedAction = CurrentAction.Right
                keySpace -> controller.pressedAction = CurrentAction.DownUntilEnd
                keyUp -> controller.pressedAction = CurrentAction.Rotate
            }
        }

        win.onkeyup = { e: Event ->
            check(e is KeyboardEvent)
            when (e.keyCode) {
                keyDown,
                keyLeft,
                keyRight,
                keySpace,
                keyUp
                -> controller.pressedAction = null
            }
        }

        var mouseTracker: MouseTracker? = null

        win.onmousedown = { e: Event ->
            mouseTracker = MouseTracker(e as MouseEvent, controller, clipping)
            controller.pressedAction = null
            null.asDynamic()
        }

        win.onmousemove = { e: Event ->
            mouseTracker?.let {
                controller.pressedAction = it.onMove(e as MouseEvent)
            }
            null.asDynamic()
        }

        win.onmouseup = { e: Event ->
            mouseTracker?.let {
                controller.pressedAction = it.onUp(e as MouseEvent)
            }
            mouseTracker = null
            null.asDynamic()
        }

        resizeCanvas(controller, clipping, canvas)
        repaint(controller, clipping, canvasCtx)

        fun worker() {
            controller.proceed(milliTime())?.let { (durationMillis, changed) ->
                if (changed) {
                    repaint(controller, calculateClipping(controller, win), canvasCtx)
                }
                win.setTimeout({ worker() }, durationMillis.toInt())
            }
        }

        worker()
    }
}
