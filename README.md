# Project - Distributed File System
## Starting up the servers
After building the java files, do the following.
### In Linux (or any bash terminal)
In a terminal,
```bash
cd ./out/production/Distributed-File-System/
./launcher.sh start
```
You can also stop all the running servers with `./launcher.sh stop`, otherwise they will keep running even after you close your terminal.

In another terminal, type in the commands.

### In Windows
As it has not been developped on Windows, there is not such convenient way.
Open 6 different terminals.
In each terminal, type in the following commands.

#### Terminal 1 - Name Server
```cmd
cd .\out\production\Distributed-File-System\
java CLI nameserver 9000
```
#### Terminal 2 - Data Server 1
```cmd
cd .\out\production\Distributed-File-System\
java CLI dataserver 1 127.0.0.1 9000 8001
```
#### Terminal 3 - Data Server 2
```cmd
cd .\out\production\Distributed-File-System\
java CLI dataserver 2 127.0.0.1 9000 8002
```
#### Terminal 4 - Data Server 3
```cmd
cd .\out\production\Distributed-File-System\
java CLI dataserver 3 127.0.0.1 9000 8003
```
#### Terminal 5 - Data Server 4
```cmd
cd .\out\production\Distributed-File-System\
java CLI dataserver 3 127.0.0.1 9000 8004
```
#### Terminal 6 - Client
Open a 6th terminal to write files (put) and read files (read).
```cmd
cd .\out\production\Distributed-File-System\
```
## Using the CLI
Even if this programm is intended to be included in other projects, we still provide a CLI to use it manually.
To run it, write `java CLI <your CLI command>`, or just `java CLI` to get a help message.
Here are the functions provided through the CLI :
```
  nameserver <port>                                                    Start the NameServer on port
  dataserver <uuid> <nameServerIp> <nameServerPort> <dataServerPort>   Start a DataServer
  put <serverIp> <port> <path>                                         Upload file to NameServer
  get <nameServerIp> <nameServerPort> <filePath> [offset]              Retrieve a file from the server
  help                                                                 Show this help
```

### Writing a file
First, create a file in the current directory. 
For example, do the following in a bash terminal:
```bash
touch file.txt
echo "This is text" >> file.txt
```
Put the file on the distributed file system:
```bash
java CLI put 127.0.0.1 9000 file.txt
```

### Reading a file
To read _file.txt_
```bash
java CLI read 127.0.0.1 9000 file.txt 1
```

