# Project 4
## Setting up the servers

Open 4 different terminals.
In each terminal, type in the following commands.

#### Terminal 1
```cmd
cd .\out\production\Distributed-File-System\
java CLI nameserver 8000
```
#### Terminal 2
```cmd
cd .\out\production\Distributed-File-System\
java CLI dataserver 1 127.0.0.1 8000 8001
```
#### Terminal 3
```cmd
cd .\out\production\Distributed-File-System\
java CLI dataserver 2 127.0.0.1 8000 8002
```
#### Terminal 4
```cmd
cd .\out\production\Distributed-File-System\
java CLI dataserver 3 127.0.0.1 8000 8003
```
#### Terminal 5
Open a 5th terminal to write files (put) and read files (read).

```cmd
cd .\out\production\Distributed-File-System\
```

## Writing a file

```cmd
touch file.txt
java CLI put 127.0.0.1 8000 file.txt
```

## Reading a file