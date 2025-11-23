import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import networkMessages.CloseFileMessage;
import networkMessages.CreateFileMessage;
import networkMessages.ErrorMessage;
import networkMessages.GetMetadataMessage;
import networkMessages.MetadataResponse;
import networkMessages.OkMessage;

public class Client {
    private final String nameServerIPAddress;
    private final int nameServerPort;

    // Client Class constructor
    public Client(String nameServerIPAddress, int nameServerPort) {
        this.nameServerIPAddress = nameServerIPAddress;
        this.nameServerPort = nameServerPort;
    }

    // Error handling method
    private void checkForError(Object response, String operation) throws IOException {
        if (response instanceof ErrorMessage error) {
            StringBuilder errorMsg = new StringBuilder(operation);
            errorMsg.append(" failed: ").append(error.message);
            if (error.fileName != null) {
                errorMsg.append(" (fileName: ").append(error.fileName).append(")");
            }
            if (error.chunkID != null) {
                errorMsg.append(" (chunkID: ").append(error.chunkID).append(")");
            }
            throw new IOException(errorMsg.toString());
        }
    }

    // Upload files from Client to Distributed File System
    public void putFile(String path) throws Exception {
        // Initialization
        String fileName = Path.of(path).getFileName().toString();
        int chunkSize = (int) FileMetadata.ChunkSize;
        int chunkIndex = 0;

        // Establish a connection to NameServer
        try (Socket socket = new Socket(nameServerIPAddress, nameServerPort);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
             InputStream in = Files.newInputStream(Path.of(path))) {

            // Create file on a server
            oos.writeObject(new CreateFileMessage(fileName));
            oos.flush();
            Object ack = ois.readObject();
            checkForError(ack, "CreateFile");
            if (!(ack instanceof OkMessage)) {
                throw new IOException("CreateFile not acknowledged by NameServer");
            }

            // Get file metadata
            oos.writeObject(new GetMetadataMessage(fileName));
            oos.flush();
            Object metaObj = ois.readObject();
            checkForError(metaObj, "GetMetadata");
            if (!(metaObj instanceof MetadataResponse)) {
                throw new IOException("GetMetadata did not return metadata");
            }
            MetadataResponse meta = (MetadataResponse) metaObj;
            if (!meta.exists) {
                throw new IOException("File metadata not found after creation");
            }
            long fileId = meta.fileId;

            // Send file chunks to NameServer
            byte[] buffer = new byte[chunkSize];
            int read;
            while ((read = in.read(buffer)) != -1) {
                byte[] data;
                // Chunking of file
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
                checkForError(chunkAck, "Chunk " + chunkIndex);
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
            checkForError(closeAck, "CloseFile");
            if (!(closeAck instanceof OkMessage)) {
                throw new IOException("CloseFile not acknowledged by NameServer");
            }
        }
    }
}


