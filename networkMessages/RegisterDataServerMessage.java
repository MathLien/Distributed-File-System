package networkMessages;
import java.io.Serializable;

public class RegisterDataServerMessage implements Serializable {
    public final String uuid;
    public final long freeSpace;
    public final long occupiedSpace;
    public final String ipAddress;
    public final int port;

    public RegisterDataServerMessage(String uuid, String ipAddress, int port, long freeSpace, long occupiedSpace) {
        this.uuid = uuid;
        this.ipAddress = ipAddress;
        this.port = port;
        this.freeSpace = freeSpace;
        this.occupiedSpace = occupiedSpace;
    }
}

