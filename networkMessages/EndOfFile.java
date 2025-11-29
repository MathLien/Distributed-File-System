package networkMessages;
import java.io.Serializable;

public class EndOfFile implements Serializable {
    public final String fileName;

    public EndOfFile(String fileName) {
        this.fileName = fileName;
    }
}


