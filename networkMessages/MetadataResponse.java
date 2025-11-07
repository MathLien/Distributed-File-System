package networkMessages;
import java.io.Serializable;

public class MetadataResponse implements Serializable {
    public final boolean exists;
    public final long fileId;

    public MetadataResponse(boolean exists, long fileId) {
        this.exists = exists;
        this.fileId = fileId;
    }
}


