import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerStatus {
    public final String ipAddress;
    public final int port;
    public final String uuid;
    public long lastSeen = 0;
    public long freeSpace;
    public long occupiedSpace;
    
    // Persistent connection to DataServer
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
    private final Object connectionLock = new Object();

    public ServerStatus(String ipAddress, int port, String uuid, long freeSpace, long occupiedSpace) {
        this.ipAddress = ipAddress;
        this.port = port;
        this.uuid = uuid;
        this.freeSpace = freeSpace;
        this.occupiedSpace = occupiedSpace;
    }
    
    public Socket getSocket() {
        synchronized (connectionLock) {
            return socket;
        }
    }
    
    public ObjectOutputStream getOos() {
        synchronized (connectionLock) {
            return oos;
        }
    }
    
    public ObjectInputStream getOis() {
        synchronized (connectionLock) {
            return ois;
        }
    }
    
    public Object getConnectionLock() {
        return connectionLock;
    }
    
    public void establishConnection() {
        synchronized (connectionLock) {
            // Close existing connection if any
            closeConnection();
            
            try {
                socket = new Socket(ipAddress, port);
                oos = new ObjectOutputStream(socket.getOutputStream());
                ois = new ObjectInputStream(socket.getInputStream());
                
                System.out.println("Established persistent connection to DataServer " + uuid);
            } catch (IOException e) {
                System.err.println("Failed to establish connection to DataServer " + uuid +" (" +ipAddress+":"+port+ ") : " + e.getMessage());
                closeConnection();
            }
        }
    }
    
    public void closeConnection() {
        synchronized (connectionLock) {
            try {
                if (ois != null) {
                    ois.close();
                    ois = null;
                }
            } catch (Exception e) {
                // Ignore
            }
            try {
                if (oos != null) {
                    oos.close();
                    oos = null;
                }
            } catch (Exception e) {
                // Ignore
            }
            try {
                if (socket != null) {
                    socket.close();
                    socket = null;
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }
}
