import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.*

class Server constructor() {
    private var publicSocket: ServerSocket = ServerSocket(9876) //autoallocated
    private val clientSockets = mutableMapOf<String, CustomSocket>()

    suspend fun run() = coroutineScope {
        launch { getInfo() }
        launch { publicSocketListener() }
        launch { clientSocketsListener() }


    }

    private fun publicSocketListener() {
        while (true) {
            try {
                val socket = CustomSocket(publicSocket.accept())
                println(socket.socket.toString())
                val reader = socket.reader
                val writer = socket.writer
                val nickname = reader.readLine()
                println("$nickname is connecting")
                if (clientSockets.containsKey(nickname)) {
                    println("Sorry, this nickname is already taken. Choose another one.")
                    writer.write("Sorry, this nickname is already taken. Choose another one.")
                    writer.newLine()
                    writer.flush()

                    reader.close()
                    writer.close()
                    socket.socket.close()
                } else {
                    println("normal connection")
                    writer.write("Connected successfully.")
                    writer.newLine()
                    writer.flush()
                    clientSockets[nickname] = socket
                }
            } catch (e:IOException) {
                println("Someone tried to connect, but unsuccessfully.")
            }
        }
    }

    private fun clientSocketsListener() {
        while (true) {
            for (pair in clientSockets) {
                val customSocket = pair.value
                if (customSocket.socket.isClosed) clientSockets.remove(pair.key)
                if (customSocket.reader.ready()) sendMessage(pair.key, customSocket.reader.readText())
            }
        }
    }

    private fun sendMessage(nickname: String, message: String) {
        val date = Date()
        val sdf = SimpleDateFormat("hh:mm:ss")
        val timeStr = sdf.format(date)
        for (pair in clientSockets) {
            val writer = pair.value.writer
            writer.write("$timeStr <$nickname>: $message")
            writer.newLine()
            writer.flush()
        }
    }

    private fun getInfo() {
        println("This is your port, let the clients connect to it: "
                +  publicSocket.localPort.toString())
    }

    data class CustomSocket constructor (val socket: Socket) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val writer= BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
    }
}