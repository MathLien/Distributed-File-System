import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import networkMessages.CloseFileMessage;
import networkMessages.CreateFileMessage;
import networkMessages.GetMetadataMessage;
import networkMessages.MetadataResponse;
import networkMessages.OkMessage;

public class Client {
    private final String nameServerIPAddress;
    private final int nameServerPort;

    public Client(String nameServerIPAddress, int nameServerPort) {
        this.nameServerIPAddress = nameServerIPAddress;
        this.nameServerPort = nameServerPort;
    }

    public void putFile(String path) throws Exception {
        String fileName = Path.of(path).getFileName().toString();
        int chunkSize = (int) FileMetadata.ChunkSize;
        int chunkIndex = 0;

        try (Socket socket = new Socket(nameServerIPAddress, nameServerPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             InputStream in = Files.newInputStream(Path.of(path))) {

            oos.writeObject(new CreateFileMessage(fileName));
            oos.flush();
            Object ack = ois.readObject();
            if (!(ack instanceof OkMessage)) {
                throw new IOException("CreateFile not acknowledged by NameServer");
            }

            oos.writeObject(new GetMetadataMessage(fileName));
            oos.flush();
            Object metaObj = ois.readObject();
            if (!(metaObj instanceof MetadataResponse)) {
                throw new IOException("GetMetadata did not return metadata");
            }
            MetadataResponse meta = (MetadataResponse) metaObj;
            if (!meta.exists) {
                throw new IOException("File metadata not found after creation");
            }
            long fileId = meta.fileId;

            byte[] buffer = new byte[chunkSize];
            int read;
            while ((read = in.read(buffer)) != -1) {
                byte[] data;
                if (read == buffer.length) {
                    data = buffer;
                } else {
                    data = new byte[read];
                    System.arraycopy(buffer, 0, data, 0, read);
                }
                Chunk chunk = new Chunk(fileId, fileName, chunkIndex, data);
                oos.writeObject(chunk);
                oos.flush();
                Object chunkAck = ois.readObject();
                if (!(chunkAck instanceof OkMessage)) {
                    throw new IOException("Chunk " + chunkIndex + " not acknowledged by NameServer");
                }
                chunkIndex++;
                if (data == buffer) {
                    buffer = new byte[chunkSize];
                }
            }
            oos.writeObject(new CloseFileMessage(fileName));
            oos.flush();
            Object closeAck = ois.readObject();
            if (!(closeAck instanceof OkMessage)) {
                throw new IOException("CloseFile not acknowledged by NameServer");
            }
        }
    }
}


