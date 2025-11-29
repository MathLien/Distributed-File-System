#!/bin/bash
##Script to start and stop the multiple servers easily
##Handwritten with love
##Use start to start them, stop to stop them
## And don't forget to stop the servers
## The id of the process are stored in a file, if lost you have to stop them manually

## WARNING !
## Please note this is a BASH SCRIPT, it will NOT WORK ON WINDOWS, except with some workarounds or WSL.

PidsFile=".PidsFile.txt"
BuildDir="out/production/Distributed-File-System"

#Selon le premier argument
case "$1" in
  'stop')
    echo "Try to stop"
    while read -r line ; do
        echo Tue $line
        kill $line
    done < $PidsFile #Par cette ligne on spécifie dans quel fichier sont stockés les PID, en mettant le fichier dans l'entrée standard de la boucle qui est lu par read
    rm  $PidsFile
    echo
    echo "Serveurs arrêté"
    exit
    ;;

  'start')
    echo "try to start"

    java -classpath $BuildDir CLI nameserver 9000 & (echo $! >> $PidsFile)
    sleep 0.5 # Pour lui laisser le temps de démarrer

    java -classpath $BuildDir CLI dataserver n1 127.0.0.1 9000 8001 & (echo $! >> $PidsFile)
    java -classpath $BuildDir CLI dataserver n2 127.0.0.1 9000 8002 & (echo $! >> $PidsFile)
    java -classpath $BuildDir CLI dataserver n3 127.0.0.1 9000 8003 & (echo $! >> $PidsFile)
    echo
    echo "Serveurs démarrés"
    echo

    exit
    ;;

  *)
    echo "Utility to start and stop servers easily"
    echo "Logs by all the servers will be displayed in this terminal"
    echo "Usage :"
    echo "   ./launcher.sh start  -  Start the nameserver and 3 dataservers"
    echo "   ./launcher.sh stop   -  Stop the servers launched by this utility"
    ;;
esac