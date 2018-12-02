package com.github.werehuman.tetris.impl

import kotlin.jvm.Volatile
import kotlin.math.max

internal class DelayDurationGenerator(counts: Iterable<Pair<Long, Int>>, private val lastDurationMillis: Long) {
    private val countsIterator = counts.iterator()
    private var durationMillis: Long = 0
    private var count: Int = 0

    fun getMillis(): Long {
        if (count-- == 0) {
            if (countsIterator.hasNext()) {
                val tmp = countsIterator.next()
                durationMillis = tmp.component1()
                count = tmp.component2()
            } else {
                durationMillis = lastDurationMillis
            }
        }
        return durationMillis
    }
}

enum class CurrentAction(internal val debounce: Boolean) {
    Down(false),
    DownUntilEnd(true),
    Left(false),
    Right(false),
    Rotate(true),
}

class TetrisController constructor(val width: Int, val height: Int, nowMillis: Long) {
    private val area = TetrisArea(10, 20)
    private val cycleDurationMillis = 10L
    private val bouncingActionCycleMillis = 100L
    private val durationGenerator = DelayDurationGenerator(
            arrayListOf(
                    800L to 20,
                    700L to 40,
                    600L to 80,
                    500L to 160,
                    400L to 320),
            300L)

    private var nextCycle = 0L

    @Volatile
    var pressedAction: CurrentAction? = null

    private var debouncingAction: CurrentAction? = null
    private var nextBouncingActionMillis = 0L

    init {
        area.start()
        updateDelayDuration(nowMillis)
    }

    private fun getDelayDuration(nowMillis: Long): Long {
        val result = max(0, nextCycle - nowMillis)
        return when {
            result > cycleDurationMillis -> cycleDurationMillis
            result < 0 -> 0
            else -> result
        }
    }

    private fun updateDelayDuration(nowMillis: Long) {
        nextCycle = nowMillis + durationGenerator.getMillis()
    }

    fun getCell(offset: Int): TetrisColor? = area.getCell(offset)

    data class ProceedResult(val delayMillis: Long, val changed: Boolean)

    fun proceed(nowMillis: Long): ProceedResult? {
        val action = pressedAction
        var resultChanged = false
        if (action != null) {
            val canCheck = if (action.debounce) {
                debouncingAction != action
            } else {
                nextBouncingActionMillis <= nowMillis
            }
            if (canCheck) {
                resultChanged = when (action) {
                    CurrentAction.Left -> area.moveFigureLeft()
                    CurrentAction.Right -> area.moveFigureRight()
                    CurrentAction.Down -> {
                        if (!area.tryProceed()) {
                            return null
                        }
                        true
                    }
                    CurrentAction.Rotate -> area.rotateClockwise()
                    CurrentAction.DownUntilEnd -> {
                        updateDelayDuration(nowMillis)
                        if (!area.moveFigureDownUntilEnd()) {
                            return null
                        }
                        true
                    }
                }
            }
            if (action.debounce) {
                debouncingAction = action
                nextBouncingActionMillis = 0
            } else if (resultChanged) {
                debouncingAction = null
                nextBouncingActionMillis = nowMillis + bouncingActionCycleMillis
            }
        } else {
            debouncingAction = null
            nextBouncingActionMillis = 0
        }
        if (nextCycle <= nowMillis) {
            resultChanged = true
            if (area.tryProceed()) {
                updateDelayDuration(nowMillis)
            } else {
                return null
            }
        }
        return ProceedResult(getDelayDuration(nowMillis), resultChanged)
    }
}
