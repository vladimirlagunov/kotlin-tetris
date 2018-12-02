package com.github.werehuman.tetris

expect object TetrisMain {
    fun play(width: Int, height: Int)
}

fun main(args: Array<String>) {
    TetrisMain.play(10, 20)
}
