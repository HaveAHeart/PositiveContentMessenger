import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.Socket
import java.nio.file.Path
import java.util.*

class Client constructor(hostAddress: String, hostPort: Int, private var nickname: String) {
    private var socket: Socket = Socket(hostAddress, hostPort)
    private var reader: BufferedReader = BufferedReader(InputStreamReader(socket.getInputStream()))
    private var writer: BufferedWriter = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
    private val scanner = Scanner(System.`in`)
    private var isOpened = true

    suspend fun run() = coroutineScope {
        launch(Dispatchers.IO) { start() }
        launch(Dispatchers.IO) { handleIncoming() }
        launch(Dispatchers.IO) { handleSent() }
    }

    private fun start() {
        println(socket.toString())
        writer.write(nickname)
        writer.newLine()
        writer.flush()
    }

    private fun handleIncoming() {
        while (isOpened and !socket.isClosed) {
            val customMsg = CustomMsg("", "", "", "", "")
            for (i in 0 until 5) {
                val msg = reader.readLine()
                val splitted = msg.split(":\\s".toRegex()).toTypedArray()
                val type = splitted.first().toString()
                val value = splitted.last().toString().substring(1, splitted.last().length - 1) //removing the ""

                when (type) {
                    "time" -> customMsg.time = value
                    "name" -> customMsg.name = value
                    "msg" -> customMsg.msg = value
                    "attname" -> customMsg.attname = value
                    "att" -> customMsg.att = value
                }
            }
            if (customMsg.attname.isNotBlank()) {
                val decodedFile = Base64.getDecoder().decode(customMsg.att)

                //TODO filenames
                val file = File(customMsg.attname)
                file.createNewFile()
                file.writeBytes(decodedFile)
                println("file ${customMsg.attname} is loaded in ${file.absolutePath}")
            }
            println(customMsg.toString())
        }
    }

    private fun handleSent() {
        while (isOpened and !socket.isClosed) {
            var msg = "msg: \"" + scanner.nextLine() + "\""
            if (msg.toLowerCase() == "quit") isOpened = false
            val regexAtt = """att\(.+\)""".toRegex()
            if (msg.contains(regexAtt)) {
                var pathStr = regexAtt.find(msg)!!.value
                pathStr = pathStr.substring(4, pathStr.length - 1)
                val file = File(pathStr)
                if (file.isFile) {
                    //println("file found: ${file.name}")
                    msg = msg.replaceFirst(regexAtt, "(file ${file.name} attached)")
                    //println(msg + "after replace")
                    msg = msg.plus("\nattname: \"${file.name}\"")
                    //println(msg + "after path")
                    val byteArrayFile = file.readBytes()
                    val stringFile = Base64.getEncoder().encodeToString(byteArrayFile)

                    msg = msg.plus("\natt: \"$stringFile\"")
                    //println(msg + "after att")

                }
                else {
                    msg = msg.plus("\nattname: \"\"\natt: \"\"")
                }
            }
            else {
                msg = msg.plus("\nattname: \"\"\natt: \"\"")
            }

            //println(msg)
            writer.write(msg)
            writer.newLine()
            writer.flush()
            //println("sent")
        }

        // closed - closing the sockets
        reader.close()
        writer.close()
        socket.close()
    }

    data class CustomMsg constructor(var time: String, var name: String, var msg: String, var attname: String, var att: String) {
        override fun toString(): String {
            return "time: \"$time\"\nname: \"$name\"\nmsg: \"$msg\"\nattname: \"$attname\"\natt: \"$att\""
        }
    }
}