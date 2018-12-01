package org.github.werehuman.tetris.impl

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test


fun areaFromString(view: String, figureFactory: () -> Figure = TODO()): TetrisArea {
    val lines = view.lines()
    val width = lines[0].length
    val height = lines.size
    val cells = Array<TetrisColor?>(width * height) { null }
    var offset = 0
    for (line in lines) {
        for (char in line) {
            cells[offset++] = when (char) {
                'R' -> TetrisColor.RED
                'Y' -> TetrisColor.YELLOW
                'G' -> TetrisColor.GREEN
                'B' -> TetrisColor.BLUE
                'M' -> TetrisColor.MAGENTA
                'C' -> TetrisColor.CYAN
                'W' -> TetrisColor.WHITE
                '.' -> null
                else -> throw IllegalArgumentException("Unexpected symbol $char")
            }
        }
    }
    return TetrisArea(width, height, cells, figureFactory)
}


fun stringFromArea(area: TetrisArea): String {
    val result = StringBuilder(area.height * (area.width + 1))
    var offset = 0
    for (y in 0 until area.height) {
        for (x in 0 until area.width) {
            result.append(when (area.getCell(offset++)) {
                TetrisColor.RED -> 'R'
                TetrisColor.YELLOW -> 'Y'
                TetrisColor.GREEN -> 'G'
                TetrisColor.BLUE -> 'B'
                TetrisColor.MAGENTA -> 'M'
                TetrisColor.CYAN -> 'C'
                TetrisColor.WHITE -> 'W'
                null -> '.'
            })
        }
        result.append('\n')
    }
    return result.toString().trim()
}

val TEST_FIGURE = Figure(3, 2, TetrisColor.RED, booleanArrayOf(
        false, true, true,
        true, true, false))


class TetrisAreaTest {
    @Test
    fun removeLines1() {
        val initial = """
            |.....
            |.....
            |RRRR.
            |YYYYY
            |GG.GG
            |BBBBB
            |MMMMM
            |C.C.C
        """.trimMargin()

        val expected = """
            |.....
            |.....
            |.....
            |.....
            |.....
            |RRRR.
            |GG.GG
            |C.C.C
        """.trimMargin()

        val area = areaFromString(initial)
        area.removeLines()
        assertEquals(expected, stringFromArea(area))
    }

    @Test
    fun proceedUntilEnd() {
        val area = areaFromString(
                """
                |.....
                |.....
                |.....
                |.....
                """.trimMargin(),
                { TEST_FIGURE })

        assertTrue(area.trySpawnFigure())

        assertEquals(
                """
                |..RR.
                |.RR..
                |.....
                |.....
                """.trimMargin(),
                stringFromArea(area))

        assertTrue(area.tryProceed())

        assertEquals(
                """
                |.....
                |..RR.
                |.RR..
                |.....
                """.trimMargin(),
                stringFromArea(area))

        assertTrue(area.tryProceed())

        assertEquals(
                """
                |.....
                |.....
                |..RR.
                |.RR..
                """.trimMargin(),
                stringFromArea(area))

        assertTrue(area.tryProceed())

        assertEquals(
                """
                |..RR.
                |.RR..
                |..RR.
                |.RR..
                """.trimMargin(),
                stringFromArea(area))

        assertFalse(area.tryProceed())
    }
}
