import kotlinx.coroutines.*
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.text.SimpleDateFormat
import java.util.*

class Server {
    //TODO IOException
    private var publicSocket: ServerSocket = ServerSocket(9876) //autoallocated
    private val clientSockets = mutableMapOf<String, CustomSocket>()
    private val clientScope = CoroutineScope(Dispatchers.IO)
    private val sdf = SimpleDateFormat("hh.mm.ss")
    suspend fun run() = coroutineScope {
        launch (Dispatchers.Default) { getInfo() }
        launch (Dispatchers.IO) { publicSocketListener() }

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
                if (!nickname.matches(Regex("""[\w\d]+"""))) {
                    writer.write("Sorry, this nickname is incorrect. It can consist only of any combination of letters and digits.")
                    writer.newLine()
                    writer.flush()

                    reader.close()
                    writer.close()
                    socket.socket.close()
                }
                if (clientSockets.containsKey(nickname)) {
                    writer.write("Sorry, this nickname is already taken. Choose another one.")
                    writer.newLine()
                    writer.flush()

                    reader.close()
                    writer.close()
                    socket.socket.close()
                } else {
                    val date = Date()
                    val timeStr = sdf.format(date)
                    val customMsg = CustomMsg(timeStr, "Server", "Hello, $nickname, you are connected!", "", "")
                    writer.write(customMsg.toString())
                    writer.newLine()
                    writer.flush()
                    clientSockets[nickname] = socket
                    clientScope.launch { clientSocketListener(nickname, socket) }
                    println("coroutine launched for $nickname")
                }
            } catch (e:IOException) {
                println("Someone tried to connect, but unsuccessfully.")
            }
        }
    }

    private fun clientSocketListener(nickname: String, socket: CustomSocket) {
        println("listening for $nickname")

        val regex = Regex("""^time: "\d\d\.\d\d\.\d\d";name: "[\w\d]+";msg: ".+";attname: ".*";att: ".*"$""")
        while (socket.socket.isConnected) {
            val splittedMsg = mutableListOf<String>()
            for (i in 0 until 3) {
                try {
                    val msg = socket.reader.readLine()
                    splittedMsg.add(msg)
                } catch (e: SocketException) {
                    println("$nickname disconnected.")
                    break
                }
            }

            val date = Date()
            val timeStr = sdf.format(date)
            val customMsg = CustomMsg(timeStr, nickname, "", "", "")

            for (attr in splittedMsg) {
                val splitted = attr.split(":\\s".toRegex()).toTypedArray()
                val type = splitted.first().toString()
                val value = splitted.last().toString().substring(1, splitted.last().length - 1) //removing the ""

                when (type) {
                    "msg" -> customMsg.msg = value
                    "attname" -> customMsg.attname = value
                    "att" -> customMsg.att = value
                }
            }

            val resString = customMsg.toString()
            for (pair in clientSockets) {
                val writer = pair.value.writer
                println("wrote:\n$resString")
                writer.write(resString)
                writer.newLine()
                writer.flush()
            }


        }
        clientSockets.remove(nickname) //out of loop - socket closed
    }

    private fun getInfo() {
        println("This is your port, let the clients connect to it: "
                +  publicSocket.localPort.toString())
    }

    data class CustomSocket constructor (val socket: Socket) {
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val writer= BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
    }

    data class CustomMsg constructor(var time: String, var name: String, var msg: String, var attname: String, var att: String) {
        override fun toString(): String {
            return "time: \"$time\"\nname: \"$name\"\nmsg: \"$msg\"\nattname: \"$attname\"\natt: \"$att\""
        }
    }
}