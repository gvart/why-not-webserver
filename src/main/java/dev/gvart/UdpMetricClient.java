InetSocketAddress serverAddress = new InetSocketAddress(InetAddress.getLoopbackAddress(), 7777);
Random random = new Random();

void main() throws IOException {
    var serviceId = UUID.randomUUID().toString().substring(0, 4);
    IO.println("INFO: Application started, serviceId:" + serviceId);

    // No connect(): the destination lives on the packet.
    try (var socket = new DatagramSocket()) {
        while (true) {
            pushMetrics(socket, new ServiceMetrics(serviceId, sampleMetrics()));
            Thread.sleep(2500);
        }
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
    }
}

private void pushMetrics(DatagramSocket socket, ServiceMetrics serviceMetrics) throws IOException {
    var message = serviceMetrics.toProtocol();
    socket.send(new DatagramPacket(message, message.length, serverAddress));
}

/** Stand-in for real readings: every tick reports a fresh random sample. */
private Map<MetricType, Integer> sampleMetrics() {
    var metrics = new EnumMap<MetricType, Integer>(MetricType.class);
    for (MetricType metric : MetricType.values()) {
        metrics.put(metric, random.nextInt(metric.maxValue + 1));
    }
    return metrics;
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
    /** Protocol format: "a1b2/CPU:37;RAM:912;HDD:2048" */
    byte[] toProtocol() {
        var formattedMetrics =
                metrics.entrySet().stream()
                        .map(it -> it.getKey().name() + ":" + it.getValue())
                        .collect(Collectors.joining(";"));

        return (serviceId + "/" + formattedMetrics).getBytes(StandardCharsets.UTF_8);
    }
}
