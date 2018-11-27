
import java.awt.Color
import java.awt.Graphics
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.WindowConstants

fun main(args: Array<String>) {
    val width = 400
    val height = 800
    JFrame.setDefaultLookAndFeelDecorated(true)
    val frame = JFrame("Example")
    frame.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame.background = Color.black
    frame.setSize(width, height)
    frame.isResizable = false

    frame.add(object : JPanel() {
        override fun paintComponent(g: Graphics) {
            g.color = Color.black
            g.fillRect(0, 0, width, height)

            g.color = Color.yellow
            g.fillRect(2, 2, 36, 36)
        }
    })

    frame.isVisible = true
}
