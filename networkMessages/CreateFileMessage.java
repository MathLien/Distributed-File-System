package networkMessages;
import java.io.Serializable;

public class CreateFileMessage implements Serializable {
    public final String fileName;

    public CreateFileMessage(String fileName) {
        this.fileName = fileName;
    }
}


