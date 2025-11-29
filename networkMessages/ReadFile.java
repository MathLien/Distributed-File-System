package networkMessages;

import java.io.Serializable;

public class ReadFile implements Serializable {
    public final String pathName;
    public final long offset;

    public ReadFile(String pathName, long offset) {
        this.pathName = pathName;
        this.offset = offset;
    }
}
