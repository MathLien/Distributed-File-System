public class CLI {
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            printHelp();
            return;
        }

        String cmd = args[0].toLowerCase();
        switch (cmd) {
            case "nameserver":
                // Usage: nameserver <port>
                if (args.length != 2) {
                    System.err.println("Usage: nameserver <port>");
                    System.exit(1);
                }
                int port = Integer.parseInt(args[1]);
                NameServer ns = new NameServer();
                System.out.println("Starting NameServer on port " + port + "...");
                ns.start(port);
                break;
            case "put":
                // Usage: put <serverIp> <port> <path>
                if (args.length != 4) {
                    System.err.println("Usage: put <serverIp> <port> <path>");
                    System.exit(1);
                }
                String ip = args[1];
                int p = Integer.parseInt(args[2]);
                String path = args[3];
                Client client = new Client(ip, p);
                System.out.println("Uploading " + path + " to " + ip + ":" + p + "...");
                client.putFile(path);
                System.out.println("Done.");
                break;
            default:
                System.err.println("Unknown command: " + cmd);
                printHelp();
        }
    }

    private static void printHelp() {
        System.out.println("Distributed-File-System CLI");
        System.out.println("Commands:");
        System.out.println("  nameserver <port>           Start the NameServer on port");
        System.out.println("  put <serverIp> <port> <path>  Upload file to NameServer");
        System.out.println("  help                        Show this help");
    }
}


