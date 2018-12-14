package com.github.werehuman.tetris

import com.github.werehuman.tetris.impl.CurrentAction
import com.github.werehuman.tetris.impl.TetrisColor
import com.github.werehuman.tetris.impl.TetrisController
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

const val borderSize: Int = 3
const val paddingFraction: Double = 0.1
const val keyDown = 40
const val keyLeft = 37
const val keyRight = 39
const val keySpace = 32
const val keyUp = 38


private data class Clipping(val cellSize: Int, val cellPadding: Int)

private fun calculateClipping(controller: TetrisController, win: Window): Clipping {
    val width = win.innerWidth
    val height = win.innerHeight
    val cellSize = kotlin.math.min(
        (width - borderSize * 2) / (controller.width + paddingFraction),
        (height - borderSize * 2) / (controller.height + paddingFraction)
    ).toInt()
    val padding = (cellSize * paddingFraction).toInt()
    return Clipping(cellSize = cellSize, cellPadding = padding)
}

private fun resizeCanvas(controller: TetrisController, clipping: Clipping, canvas: HTMLCanvasElement) {
    canvas.width = clipping.cellSize * controller.width + borderSize * 2 + clipping.cellPadding
    canvas.height = clipping.cellSize * controller.height + borderSize * 2 + clipping.cellPadding
}

private fun milliTime(): Long {
    return (js("+new Date()") as Number).toLong()
}

private fun repaint(controller: TetrisController, clipping: Clipping, canvasCtx: CanvasRenderingContext2D) {
    val cellSize = clipping.cellSize.toDouble()
    val cellPadding = clipping.cellPadding.toDouble()

    canvasCtx.fillStyle = "black"
    canvasCtx.fillRect(
        0.0,
        0.0,
        controller.width * cellSize + borderSize * 2 + cellPadding,
        controller.height * cellSize + borderSize * 2 + cellPadding
    )

    canvasCtx.strokeStyle = "gray"
    canvasCtx.lineWidth = borderSize.toDouble()

    canvasCtx.strokeRect(
        borderSize / 2.0,
        borderSize / 2.0,
        controller.width * cellSize + borderSize + cellPadding,
        controller.height * cellSize + borderSize + cellPadding)

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

private class TouchTracker constructor(
    private val startX: Int, private val startY: Int,
    private val controller: TetrisController,
    clipping: Clipping
) {
    private val pxThreshold = min(controller.width, controller.height) * clipping.cellSize / 5
    private val cellSize = clipping.cellSize
    private val millisThreshold = 500
    private val startMillis = milliTime()
    private var currentCellOffsetX = 0
    private var desiredCellOffsetX = 0
    private var preventRotation = false

    fun onMove(clientX: Int, clientY: Int): CurrentAction? {
        val xOffset = abs(clientX - startX)
        val yOffset = abs(clientY - startY)
        return if (xOffset < yOffset && clientY >= startY + pxThreshold) {
            preventRotation = true
            CurrentAction.Down
        } else {
            desiredCellOffsetX = (clientX - startX) / cellSize
            moveHorizontal()
        }
    }

    private fun moveHorizontal(): CurrentAction? {
        return when {
            desiredCellOffsetX < currentCellOffsetX -> {
                --currentCellOffsetX
                preventRotation = true
                CurrentAction.Left
            }
            currentCellOffsetX < desiredCellOffsetX -> {
                ++currentCellOffsetX
                preventRotation = true
                CurrentAction.Right
            }
            else -> null
        }
    }

    fun onNothing(): CurrentAction? {
        return moveHorizontal()
    }

    fun onUp(clientX: Int, clientY: Int): CurrentAction? {
        if (milliTime() - startMillis < millisThreshold) {
            if (!preventRotation && abs(clientX - startX) < pxThreshold) {
                return CurrentAction.Rotate
            }
            if (clientY >= startY + pxThreshold) {
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
            height: 100%;
            margin: 0;
            overflow: hidden;
            padding: 0;
            position: fixed;
            touch-action: none;
            width: 100%;
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
            e.stopPropagation()
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
            e.stopPropagation()
            when (e.keyCode) {
                keyDown,
                keyLeft,
                keyRight,
                keySpace,
                keyUp
                -> controller.pressedAction = null
            }
        }

        var touchTracker: TouchTracker? = null

        win.addEventListener("touchstart", { e: dynamic ->
            e.preventDefault()
            touchTracker = TouchTracker(
                (e.layerX as Number).toInt(),
                (e.layerY as Number).toInt(),
                controller, clipping
            )
            controller.pressedAction = null
            null.asDynamic()
        })

        win.addEventListener("touchmove", { e: dynamic ->
            e.preventDefault()
            touchTracker?.let {
                controller.pressedAction = it.onMove(
                    (e.layerX as Number).toInt(),
                    (e.layerY as Number).toInt()
                )
            }
            null.asDynamic()
        })

        win.addEventListener("touchend", { e: dynamic ->
            e.preventDefault()
            touchTracker?.let {
                controller.pressedAction = it.onUp(
                    (e.layerX as Number).toInt(),
                    (e.layerY as Number).toInt()
                )
            }
            touchTracker = null
            null.asDynamic()
        })

        resizeCanvas(controller, clipping, canvas)
        repaint(controller, clipping, canvasCtx)

        fun worker() {
            controller.proceed(milliTime())?.let { (durationMillis, changed) ->
                val handleStart = milliTime()
                if (changed) {
                    repaint(controller, calculateClipping(controller, win), canvasCtx)
                }
                touchTracker?.let {
                    controller.pressedAction = it.onNothing()
                }
                win.setTimeout({ worker() }, max(0, (handleStart - milliTime() + durationMillis).toInt()))
            }
        }

        worker()
    }
}
