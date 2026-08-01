int PORT = 7777;
int PACKET_SIZE = 1024;

String CLEAR_SCREEN = "\033[H\033[2J";
String BOLD_CYAN = "\033[1;36m";
String RESET = "\033[0m";

String HEADER = BOLD_CYAN + "| Service |  CPU |       RAM |       HDD |" + RESET;
String ROW = "| %-7s | %3d%% | %4d/%4d | %4d/%4d |";

/** Latest reading per service, replaced fully on every datagram. */
ConcurrentHashMap<String, Map<MetricType, Integer>> serviceMonitor = new ConcurrentHashMap<>();

void main() {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        executor.submit(this::consumeMetrics);
        executor.submit(this::printMetrics);
    }
}

/** One socket serves every client: UDP has no connections, the sender is read off each packet. */
void consumeMetrics() {
    try (var socket = new DatagramSocket(PORT)) {
        while (true) {
            var request = new DatagramPacket(new byte[PACKET_SIZE], PACKET_SIZE);
            socket.receive(request);
            var serviceMetrics = ServiceMetrics.fromProtocol(request);
            serviceMonitor.put(serviceMetrics.serviceId, serviceMetrics.metrics);
        }
    } catch (Exception ex) {
        IO.println("ERROR: Application failed." + ex);
    }
}

void printMetrics() {
    try {
        while (true) {
            IO.print(CLEAR_SCREEN);
            IO.println(HEADER);
            serviceMonitor.forEach(
                    (service, metrics) ->
                            IO.println(
                                    ROW.formatted(
                                            service,
                                            metrics.get(MetricType.CPU),
                                            metrics.get(MetricType.RAM),
                                            MetricType.RAM.maxValue,
                                            metrics.get(MetricType.HDD),
                                            MetricType.HDD.maxValue)));
            Thread.sleep(2500);
        }
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
    }
}

enum MetricType {
    CPU(100),
    RAM(4096),
    HDD(4096);

    final int maxValue;

    MetricType(int maxValue) {
        this.maxValue = maxValue;
    }
}

record ServiceMetrics(String serviceId, Map<MetricType, Integer> metrics) {
    /**
     * Protocol format: "a1b2/CPU:37;RAM:912;HDD:2048"
     *
     * <p>For simplicity we trust the incoming datagram. In the real world you would validate it
     * first, e.g. with a magic byte and/or a version prefix.
     */
    static ServiceMetrics fromProtocol(DatagramPacket packet) {
        var payload =
                new String(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength(),
                        StandardCharsets.UTF_8);
        var splitContent = payload.split("/");

        var metrics = new EnumMap<MetricType, Integer>(MetricType.class);
        for (String metricLine : splitContent[1].split(";")) {
            var values = metricLine.split(":");
            metrics.put(MetricType.valueOf(values[0]), Integer.valueOf(values[1]));
        }

        return new ServiceMetrics(splitContent[0], metrics);
    }
}
