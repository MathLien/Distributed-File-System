package networkMessages;
import java.io.Serializable;

public class ErrorMessage implements Serializable {
    public final String message;
    public final String fileName;
    public final Integer chunkID;

    public ErrorMessage(String message) {
        this(message, null, null);
    }

    public ErrorMessage(String message, String fileName) {
        this(message, fileName, null);
    }

    public ErrorMessage(String message, String fileName, Integer chunkID) {
        this.message = message;
        this.fileName = fileName;
        this.chunkID = chunkID;
    }
}


