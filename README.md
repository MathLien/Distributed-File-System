# Project - Distributed File System
## Setting up the servers

### In Linux or Ubuntu
In a terminal,
```bash
./launcher.sh
```

In another terminal, type in the commands.

### In Windows
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

## Writing a file

```bash
touch file.txt
echo "This is text" >> file.txt
```
```bash
java CLI put 127.0.0.1 9000 file.txt
```

## Reading a file
To read _file.txt_
```bash
java CLI read 127.0.0.1 9000 file.txt
```