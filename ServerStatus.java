public class ServerStatus {
    public final String ipAddress;
    public final String uuid;
    public long lastSeen = 0;
    public long freeSpace = 0;

    public ServerStatus(String ipAddress, String uuid, long freeSpace) {
        this.ipAddress = ipAddress;
        this.uuid = uuid;
        this.freeSpace = freeSpace;
    }
}
