package networkMessages;
import java.io.Serializable;

public class CloseFileMessage implements Serializable {
    public final String fileName;

    public CloseFileMessage(String fileName) {
        this.fileName = fileName;
    }
}


