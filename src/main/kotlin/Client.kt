import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.*
import java.lang.Thread.sleep
import java.net.Socket
import java.util.*

class Client constructor(hostAddress: String, hostPort: Int, private var nickname: String) {
    private var socket: Socket = Socket(hostAddress, hostPort)
    private var reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
    private var writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
    private val scanner = Scanner(System.`in`)
    private var isOpened = true

    suspend fun run() = coroutineScope {
        val startJob = launch(Dispatchers.IO) { start() }
        val inJob = launch(Dispatchers.IO) { handleIncoming() }
        val outJob = launch(Dispatchers.IO) { handleSent() }
    }

    private fun start() {
        println(socket.toString())
        writer.write(nickname)
        writer.newLine()
        writer.flush()
    }

    private fun handleIncoming() {
        while (isOpened and !socket.isClosed) {
            println(reader.readLine())
        }
    }

    private fun handleSent() {
        while (isOpened and !socket.isClosed) {
            val msg = scanner.next()
            if (msg.toLowerCase() == "quit") isOpened = false
            writer.write(msg)
            writer.newLine()
            writer.flush()
            println("sent")
        }

        // closed - closing the sockets
        reader.close()
        writer.close()
        socket.close()
    }
}