final String GREEN = "\033[32m";
final String END = "\033[0m";

void main() throws IOException {
    var username = requestUsername();

    try (var socket = new Socket("localhost", 6666);
            var writer = new PrintWriter(socket.getOutputStream(), true);
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
        // The server reads this first line as the username, see TcpChatServer.
        writer.println(username);

        IO.println(
                "Welcome "
                        + GREEN
                        + username
                        + END
                        + ", Local port:"
                        + socket.getLocalPort()
                        + ", Remote port:"
                        + socket.getPort());

        // Socket and stdin both block, so incoming messages need a thread of their own.
        Thread.ofVirtual().start(() -> readIncomingMessages(reader));
        sendMessageFromInput(writer);
    }
}

String requestUsername() {
    return IO.readln("Username:");
}

/** Runs until stdin closes (Ctrl+D), then exits, which stops the application. */
private void sendMessageFromInput(PrintWriter writer) {
    String message;
    while ((message = IO.readln()) != null) {
        writer.println(message);
    }

    System.exit(0);
}

void readIncomingMessages(BufferedReader reader) {
    try {
        String line;
        while ((line = reader.readLine()) != null) {
            IO.println(line);
        }
    } catch (IOException ex) {
        IO.println("ERROR: Connection failed, " + ex);
    }
}
