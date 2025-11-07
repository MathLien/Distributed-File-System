public class ServerStatus {
    public final String ipAddress;
    public final int port;
    public final String uuid;
    public long lastSeen = 0;
    public long freeSpace;
    public long occupiedSpace;

    public ServerStatus(String ipAddress, int port, String uuid, long freeSpace, long occupiedSpace) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.uuid = uuid;
        this.freeSpace = freeSpace;
        this.occupiedSpace = occupiedSpace;
    }
}
