final String YELLOW = "\033[33m";
final String GREEN = "\033[32m";
final String END = "\033[0m";
final String WELCOME_MESSAGE = YELLOW + "Welcome to TCP Chat!" + END;

void main() throws IOException {
    // One writer per live connection, keyed so a sender can be skipped when broadcasting incomming
    // message.
    var connections = new ConcurrentHashMap<UUID, PrintWriter>();

    try (var server = new ServerSocket(6666);
            var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        while (true) {
            var socket = server.accept();
            executor.submit(() -> handleConnection(socket, connections));
        }
    }
}

/** Every client gets a dedicated socket, so a virtual thread can block on it for free. */
void handleConnection(Socket socket, ConcurrentHashMap<UUID, PrintWriter> connections) {
    var connectionKey = UUID.randomUUID();

    try (var writer = new PrintWriter(socket.getOutputStream(), true);
            var reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            socket) {

        // Protocol: the first line is the username, every line after it is a message.
        var username = reader.readLine();
        if (username == null) {
            return;
        }

        connections.put(connectionKey, writer);

        writer.println(WELCOME_MESSAGE);

        String line;
        while ((line = reader.readLine()) != null) {
            IO.println("DEBUG:" + username + ": " + line);
            var message = GREEN + username + END + ": " + line;
            connections.forEach(
                    (key, out) -> {
                        if (!key.equals(connectionKey)) {
                            out.println(message);
                        }
                    });
        }
    } catch (Exception ex) {
        IO.println("ERROR: Connection failed, " + ex);
    } finally {
        connections.remove(connectionKey);
    }
}
