posizionarsi nella cartella del progetto (uniProject) ed eseguire i seguenti comandi, lato client digitare help per maggiori informazioni

//compile client and server
javac -d "bin" -cp ".\jars\jackson-annotiations-2.9.7.jar;.\jars\jackson-core-2.9.7.jar;.\jars\jackson-databind-2.9.7.jar" .\src\client\*.java .\src\dbPost\*.java .\src\dbUtenti\*.java .\src\server\*.java

//run server
java -classpath .\bin;.\jars\jackson-annotations-2.9.7.jar;.\jars\jackson-core-2.9.7.jar;.\jars\jackson-databind-2.9.7.jar server.WinsomeMainServer .\configserver.txt

//Run client
java -cp bin client.WinsomeMainClient ./configclient.txt