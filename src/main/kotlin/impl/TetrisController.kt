package org.github.werehuman.tetris.impl

import java.time.Duration

internal class DelayDurationGenerator(counts: Iterable<Pair<Duration, Int>>, private val lastDuration: Duration) {
    private val countsIterator = counts.iterator()
    private var duration: Duration = Duration.ZERO
    private var count: Int = 0

    fun get(): Duration {
        if (count-- == 0) {
            if (countsIterator.hasNext()) {
                val tmp = countsIterator.next()
                duration = tmp.component1()
                count = tmp.component2()
            } else {
                duration = lastDuration
            }
        }
        return duration
    }
}

enum class CurrentAction(internal val debounce: Boolean) {
    Down(false),
    DownUntilEnd(true),
    Left(false),
    Right(false),
    Rotate(true),
}

class TetrisController constructor(val width: Int, val height: Int, now: Duration) {
    private val area = TetrisArea(10, 20)
    private val cycleDuration = Duration.ofMillis(10)
    private val bouncingActionCycle = Duration.ofMillis(100)
    private val durationGenerator = DelayDurationGenerator(
            arrayListOf(
                    Duration.ofMillis(800) to 20,
                    Duration.ofMillis(700) to 40,
                    Duration.ofMillis(600) to 80,
                    Duration.ofMillis(500) to 160,
                    Duration.ofMillis(400) to 320),
            Duration.ofMillis(300))

    private var nextCycle = Duration.ZERO

    @Volatile
    var pressedAction: CurrentAction? = null

    private var debouncingAction: CurrentAction? = null
    private var nextBouncingAction = Duration.ZERO

    init {
        area.start()
        updateDelayDuration(now)
    }

    private fun getDelayDuration(now: Duration): Duration {
        val result = nextCycle.minus(now).let {
            if (it.isNegative) Duration.ZERO else it
        }
        return when {
            result > cycleDuration -> cycleDuration
            result.isNegative -> Duration.ZERO
            else -> result
        }
    }

    private fun updateDelayDuration(now: Duration) {
        nextCycle = now.plus(durationGenerator.get())
    }

    fun getCell(offset: Int): TetrisColor? = area.getCell(offset)

    data class ProceedResult(val delay: Duration, val changed: Boolean)

    fun proceed(now: Duration): ProceedResult? {
        val action = pressedAction
        var resultChanged = false
        if (action != null) {
            val canCheck = if (action.debounce) {
                debouncingAction != action
            } else {
                nextBouncingAction <= now
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
                        updateDelayDuration(now)
                        if (!area.moveFigureDownUntilEnd()) {
                            return null
                        }
                        true
                    }
                }
            }
            if (action.debounce) {
                debouncingAction = action
                nextBouncingAction = Duration.ZERO
            } else if (resultChanged) {
                debouncingAction = null
                nextBouncingAction = now.plus(bouncingActionCycle)
            }
        } else {
            debouncingAction = null
            nextBouncingAction = Duration.ZERO
        }
        if (nextCycle <= now) {
            resultChanged = true
            if (area.tryProceed()) {
                updateDelayDuration(now)
            } else {
                return null
            }
        }
        return ProceedResult(getDelayDuration(now), resultChanged)
    }
}
