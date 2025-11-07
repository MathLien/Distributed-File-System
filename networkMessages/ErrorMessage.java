package networkMessages;
import java.io.Serializable;

public class ErrorMessage implements Serializable {
    public final String message;

    public ErrorMessage(String message) {
        this.message = message;
    }
}


