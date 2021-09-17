import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    for (arg in args) println(arg)
    when {
        (args.size == 1) and (args[0] == "help") -> {
            println("Help")
        }
        (args.size == 1) and (args[0] == "-s") -> {
            val server = Server()
            runBlocking { server.run() }
        }
        (args.size == 4) and (args[0] == "-c") -> {
            val addr = args[1]
            val port = args[2].toIntOrNull()
            val nickname = args[3]
            if (port == null) println("incorrect port format. Try using help.")
            else {
                if ((port > 1024) and (port < 65535)) {
                    val client = Client(addr, port, nickname)
                    runBlocking { client.run() }
                }

            }


        }
    }
}