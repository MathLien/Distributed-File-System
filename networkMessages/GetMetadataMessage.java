package networkMessages;
import java.io.Serializable;

public class GetMetadataMessage implements Serializable {
    public final String fileName;

    public GetMetadataMessage(String fileName) {
        this.fileName = fileName;
    }
}


