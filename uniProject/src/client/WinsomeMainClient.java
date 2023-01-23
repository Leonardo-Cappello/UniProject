package client;
import java.io.*;
import java.net.*;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedList;
import java.util.Scanner;
import dbUtenti.User;
import server.RegistrationInterface;
import server.ServerInterface;
import server.WinsomeMainServer;

import static server.WinsomeMainServer.*;

public class WinsomeMainClient extends Thread{

    private int tcpPort;
    private String multicastAddr;
    private int multicastPort = 0;
    private String regHost;
    private int regPort;

    private int callbackPort;
    private static boolean connectionError = false;
    private boolean terminato;

    //attributi tcp
    private Socket socket = null;
    private InputStreamReader  reader = null;
    private PrintWriter writer = null;

    private NotifyInt stub = null;
    private NotifyInt callbackObj = null;
    private String tcpAddr;


    WinsomeMainClient(String filename){
        configParametri(filename);
    }


    public static void main(String[] args)  {
        String filepath = null;
        if(args.length == 0){
           System.exit(-1);
        }else{
            filepath = args[0];
        }
        WinsomeMainClient wmc = new WinsomeMainClient(filepath);
        ServerInterface server = null;
        String loggedUsername = null;
        try {

            Registry registry = LocateRegistry.getRegistry(wmc.getRegHost(),wmc.getCallbackPort());
            String name = "Server-callback";
            server = (ServerInterface) registry.lookup(name);
        }
        catch (Exception e) {
            System.out.println("Il server e' attualmente offline, riprova piu' tardi");
            System.exit(-1);
        }

        Scanner sc = new Scanner(System.in);
        String comando;


        do {
            System.out.println("Inserire l'operazione da svolgere: ");
            comando = sc.nextLine();
           if(      !comando.contains("register")    && !comando.contains("login")          && !comando.contains("logout")         &&
                    !comando.contains("list users")  && !comando.contains("list followers") && !comando.contains("list following") &&
                    !comando.contains("follow ")     && !comando.contains("unfollow ")      && !comando.contains("blog")           &&
                    !comando.contains("post \"")     && !comando.contains("show feed")      && !comando.contains("show post")      &&
                    !comando.contains("delete post") && !comando.contains("rewin")          && !comando.contains("rate")           &&
                    !comando.contains("wallet")      && !comando.contains("wallet btc")     && !comando.contains("help")           &&
                    !comando.contains("exit")        && !comando.contains("comment")){
                System.out.println("Comando non riconosciuto, digita help per maggiori informazioni");
           }
           if(comando.equals("help"))
                help();

           if(comando.contains("register")) {
                if(loggedUsername == null) {
                    comando = wmc.register(comando);
                }else{
                    System.out.println("Prima di avviare una nuova sessione devi terminare quella corrente tramite logout!");
                }
           }
           if(comando.contains("login")) {
                if(loggedUsername == null) {
                    loggedUsername = wmc.login(comando,server);
                }else{
                    System.out.println("Prima di avviare una nuova sessione devi terminare quella corrente tramite logout!");
                }
           }
           if(comando.contains("logout")) {
                //divido il comando nelle sue parti
               String[] parameters = comando.split(" ");
               if(checkParameters(parameters, 1) ) {
                    if (wmc.isOnline())
                        //effettuo logout
                       loggedUsername = wmc.logout(comando,server,loggedUsername);
                }
            }

           if(comando.contains("list users")) {
                if (wmc.isOnline())
                   wmc.listUsers(comando);
           }

           if(comando.contains("list followers")) {
                if(wmc.isOnline())
                    wmc.listFollowers();
           }


           if(comando.contains("list following")){
                //divido il comando nelle sue parti
                String[] parameters = comando.split(" ");

                if(wmc.isOnline()){
                    if (checkParameters(parameters, 2)) {

                        String s = wmc.sendAndReceive(comando);
                        switch (s) {
                            case str_err ->            System.out.println("Non segui nessuno al momento");
                            case str_err_parameters -> System.out.println("Sintassi comando errata, help per maggiori informazioni");
                            default ->                 wmc.readAnswer( s);
                        }
                    }
                }
           }

           if(comando.contains("follow ") && !comando.contains("unfollow")) {
                if(wmc.isOnline())
                    wmc.follow(comando,loggedUsername);
           }

           if(comando.contains("unfollow ")) {
                if(wmc.isOnline())
                    wmc.unfollow(comando,loggedUsername);
           }

           if(comando.contains("blog")) {
                if(wmc.isOnline()){
                    String s = wmc.sendAndReceive(comando);
                    if (str_err.equals(s)) {
                        System.out.println("Prova a pubblicare qualcosa con il comando post. Ecco un esempio");
                        System.out.println("post \"Titolo del mio primo post \" \"Il mio primo post\"");
                    } else
                        //stampo la lista di post
                        wmc.readAnswer( s);

                }
           }


           if(comando.contains("post \"")) {
                if(wmc.isOnline())
                    wmc.publishPost(comando);
           }

           if(comando.contains("show feed")) {
                if(wmc.isOnline()){
                    String s = wmc.sendAndReceive(comando);
                    if (str_err.equals(s)) {
                        System.out.println("Nessun post trovato, prova a seguire qualcuno prima.");
                        System.out.println("Usa il comando <list users> per vedere le persone con i tuoi interessi.");
                    } else
                        //stampo la lista di post
                        wmc.readAnswer(s);

                }
           }

           if(comando.contains("show post")) {
                 if(wmc.isOnline())
                    wmc.showPost(comando);
           }

           if(comando.contains("delete post")) {
                if(wmc.isOnline())
                    wmc.deletePost(comando);
           }

           if(comando.contains("rewin")) {
                if(wmc.isOnline())
                    wmc.rewin(comando);
           }

           if(comando.contains("rate")) {
                if(wmc.isOnline())
                    wmc.rate(comando);
           }

           if(comando.contains("comment")) {

                if(wmc.isOnline())
                    wmc.comment(comando);

           }

           if(comando.contains("wallet")) {

                if(wmc.isOnline()){
                    String s = wmc.sendAndReceive(comando);
                    if(comando.contains("btc")) {
                        //get wallet btc
                        if (str_err_parameters.equals(s))
                            System.out.println("Nessuna transazione trovata");
                        else
                            System.out.println("Controvalore in bitcoin: " + s);

                    }else {
                        //get wallet
                        if (str_err.equals(s))
                            System.out.println("Nessuna transazione trovata");
                        else
                            //stampo la lista di transazioni
                            wmc.readAnswer(s);

                    }
                }
           }

        } while(!comando.equals("exit") && !connectionError);

        //chiusura client

        wmc.closing(loggedUsername,server);

       /* if(wmc.getMulticastPort() != 0)
            try {
                //fermo il thread
                wmc.terminato = true;
                InetAddress ia = InetAddress.getByName(wmc.getMulticastAddr());
                byte[] data = "hai finito".getBytes();
                DatagramPacket dp = new DatagramPacket(data, data.length, ia, wmc.getMulticastPort());
                MulticastSocket ms = new MulticastSocket();

                ms.send(dp);
            }
            catch(IOException e){
                e.printStackTrace();
            }*/
        sc.close();

    }

    private static boolean checkParameters(String[] p, int i) {
        //verifica che siano stati passati la giusta quantita' di parametri
        if (p.length != i) {
            System.out.println("Syntax error: help per maggiori informazioni");
            return false;
        } else
            return true;
    }
    private void closing(String loggedUsername,ServerInterface server) {
        if (loggedUsername != null) {
            try {
                UnicastRemoteObject.unexportObject(this.callbackObj, true);

                if(!connectionError && server != null)
                    server.unregisterForCallback(loggedUsername,this.stub);

                this.socket.close();
            }  catch (IOException e) {
                //e.printStackTrace();
            }
        }
    }
    private void comment(String comando) {
        //divido il comando nelle sue parti
        String[] parameters = comando.split(" ");
        String[] parameters2 = comando.split("\"");

        if (parameters.length > 2 && parameters2.length == 2) {
            try {
                //se non ho passato un intero non invio la richiesta al server

                if(Integer.parseInt(parameters[1]) >= 0) {
                    String s = sendAndReceive(comando);
                    switch (s) {
                        case str_ok ->             System.out.println("Il tuo commento e' stato accettato");
                        case str_err ->            System.out.println("Il post non appartiene al tuo feed");
                        case str_err_parameters -> System.out.println("Comando non accettato, digitare help per maggiori informazioni");
                    }
                }else
                    System.out.println("Bisogna passare un intero positivo come id del post");
            } catch (NumberFormatException e) {
                System.out.println("Bisogna passare un intero positivo come id del post");
            }
        } else {
            System.out.println("Syntax error: help per maggiori informazioni");
        }
    }
    private void configParametri(String filename) {
        FileReader f = null;
        try {
            f = new FileReader(filename);
        } catch (FileNotFoundException e1) {

            System.out.println("File non trovato");
            System.exit(-1);
        }

        BufferedReader br  = new BufferedReader(f);
        String s;

        //lettura file di configurazione e assegnamento dei valori
        try {
            while((s=br.readLine()) !=  null) {
                if(!s.startsWith("#")) {
                    String temp = s.substring(s.indexOf('=')+1);
                    if(s.contains("TCPADDR")) {
                        this.tcpAddr = temp;
                    }
                    if(s.contains("TCPPORT")) {
                        this.tcpPort = Integer.parseInt(temp);
                    }
                    if(s.contains("MULTICAST")) {
                        this.multicastAddr = temp;
                    }
                    if(s.contains("REGHOST")) {
                        this.regHost = temp;
                    }
                    if(s.contains("REGPORT")) {
                        this.regPort = Integer.parseInt(temp);
                    }
                    if(s.contains("CALLBACK")) {
                        this.callbackPort = Integer.parseInt(temp);
                    }
                }
            }
            //System.out.println( tcpPort + " " + multicastAddr + " " + multicastPort + " "+ regHost + " " + regPort + " " + timeout);
        } catch(IOException e) {

            e.printStackTrace();
        }

    }
    private void deletePost(String comando) {
        //divido il comando nelle sue parti
        String[] parameters = comando.split(" ");

        if (checkParameters(parameters, 3)) {
            try {
                if (Integer.parseInt(parameters[2]) >= 0) {
                    String s = sendAndReceive(comando);
                    switch (s) {
                        case str_ok ->  System.out.println("post cancellato con successo");
                        case str_err -> System.out.println("post non trovato");
                        default ->      System.out.println("Errore. Digita help per maggiori informazioni");
                    }
                } else{
                    System.out.println("Bisogna passare un intero positivo come id del post");
                }
            } catch (NumberFormatException e) {
                System.out.println("Bisogna passare un intero positivo come id del post");
            }
        }
    }
    private void follow(String comando,String loggedUsername) {
        //divido il comando nelle sue parti
        String[] parameters = comando.split(" ");
        if (checkParameters(parameters, 2)) {

            if (parameters[1].equals(loggedUsername))
                System.out.println("Non puoi seguire te stesso");
            else {

                String s = sendAndReceive(comando);
                switch (s) {
                    case str_ok ->  System.out.println("Hai iniziato a seguire " + parameters[1]);
                    case str_err -> System.out.println("Segui gia' " + parameters[1]);
                    default ->      System.out.println("l'utente " + parameters[1] + " non esiste");
                }

            }
        }
    }
    public static void help() {
        //utility usata per elencare i comandi utilizzabili da CLI
        System.out.println("\n-- register username password tag1 tag2 .. tag5: permette di effettuare la registrazione al server di WINSOME. Sono consentiti al massimo 5 tags");
        System.out.println("-- login username password : permette di accedere al server di WINSOME.");
        System.out.println("-- logout : log-out per l'utente attualmente connesso.");
        System.out.println("-- list users : restituisce una lista di utenti che hanno almeno un tag in comune.");
        System.out.println("-- list followers : restituisce la lista dei tuoi followers.");
        System.out.println("-- list following : restituisce tutti gli utenti che segui.");
        System.out.println("-- follow username : inserisce l'utente username tra i tuoi seguiti.");
        System.out.println("-- unfollow username : rimuove l'utente username dai i tuoi seguiti.");
        System.out.println("-- blog : restituisce tutti i tuoi post pubblicati nel formato (id testo autore titolo).");
        System.out.println("-- post \"title\" \"content\" : permette di pubblicare un nuovo post, titolo massimo di 20 caratteri,contenuto di 500.");
        System.out.println("-- show feed : restituisce tutti i post nel proprio feed. ");
        System.out.println("-- show post id : restituisce titolo, contenuto, numero di voti positivi e negativi e commenti del post.  ");
        System.out.println("-- delete post id : cancella il post se e' l'autore ad invocarla. ");
        System.out.println("-- rewin idPost : pubblica nel propio blog un post presente nel feed e non sei l'autore. ");
        System.out.println("-- rate idPost vote : vota il post, vote = +1 o vote = -1 se presente nel feed e non sei l'autore  ");
        System.out.println("-- comment idPost \"comment\" : permette di commentare il post solo se e' nel proprio feed e non ne sei l'autore  ");
        System.out.println("-- Wallet : recupera il valore del tuo portafoglio  ");
        System.out.println("-- Wallet btc : recupera il controvalore del tuo portafoglio in btc ");
        System.out.println("-- exit per terminare la sessione");
    }
    private void listFollowers() {
        LinkedList<String> temp = null;
        try {
            assert stub != null;
            temp = stub.getFollowers();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (temp != null && !temp.isEmpty())
            for (String s : temp)
                System.out.println(s);
        else
            System.out.println("Al momento non hai followers");
    }
    private void listUsers(String comando) {
        //divido il comando nelle sue parti
        String[] parameters = comando.split(" ");

        if (checkParameters(parameters, 2)) {
            String s = sendAndReceive(comando);

            switch (s) {
                case str_err ->            System.out.println("Spiacenti, al momento non esistono utenti con tag uguali ai tuoi");
                case str_err_parameters -> System.out.println("Sintassi comando errata, help per maggiori informazioni");
                default ->                 this.readAnswer(s);
            }
        }
    }
    private String login(String comando, ServerInterface server) {
        //divido il comando nelle sue parti
        String[] parameters = comando.split(" ");
        String loggedUsername = null;
        if (checkParameters(parameters, 3)) {
            //apertura della connessione tcp con il server
            try {
                this.socket = new Socket();
                SocketAddress address = new InetSocketAddress(this.tcpAddr, this.tcpPort);
                socket.connect(address);

                // associa gli stream di input (lettura) e output (scrittura) al socket
                this.reader = new InputStreamReader(socket.getInputStream());
                this.writer = new PrintWriter(socket.getOutputStream());

                String s = sendAndReceive(comando);
                if (str_ok.equals(s)) {
                    try {
                        loggedUsername = parameters[1];

                        //registrazione callback
                        this.callbackObj = new NotifyImpl(loggedUsername);
                        this.stub = (NotifyInt) UnicastRemoteObject.exportObject(this.callbackObj, 0);
                        //stub.setUsername(loggedUsername);
                        if(server != null && server.registerForCallback( this.stub) == 0){

                            //mando l'ok al server e aspetto di ricevere la porta per la multicast
                            int temp = Integer.parseInt(sendAndReceive(str_ok));

                            //avvio il thread di ricezione delle notifiche
                            if(this.getMulticastPort() == 0){
                                this.setMulticastPort(temp);
                                this.setDaemon(true);
                                this.start();
                            }
                            System.out.println("login avvenuto con successo");
                        }else{
                            System.out.println("L'utente e' gia' loggato");
                            loggedUsername=null;
                            this.writer.write(str_err);
                            this.writer.flush();
                            this.socket.close();
                            UnicastRemoteObject.unexportObject(this.callbackObj,true);
                            this.writer=null;
                            this.reader=null;
                            this.socket=null;
                        }

                    } catch (RemoteException e) {
                        System.out.println("Non e' stato possibile registrarsi al servizio di callback, riprova piu' tardi");
                        this.writer.write(str_err);
                        this.writer.flush();
                        System.exit(-1);
                    }
                } else {
                    System.out.println("Errore, username e password incorretti");
                    this.socket.close();
                    this.writer=null;
                    this.reader=null;
                    this.socket=null;
                }
            } catch (IOException e) {
                System.out.println("Il server e' al momento offline, la sessione corrente sara' terminata");
                System.exit(-2);
            }
        }
        return loggedUsername;
    }
    private String logout(String comando, ServerInterface server, String loggedUsername) {
        String s = sendAndReceive(comando);
        if (s.equals(str_ok)) {
            try {
                socket.close();
                server.unregisterForCallback(loggedUsername,stub);
                loggedUsername = null;
                UnicastRemoteObject.unexportObject(callbackObj,true);

            } catch (IOException e) {
                e.printStackTrace();
            }

            writer=null;
            reader=null;
            socket=null;
            System.out.println("Ti sei disconnesso dal server");
        }
        return loggedUsername;
    }
    private boolean isOnline() {
        if(socket != null && writer != null && reader != null)
            return true;

        System.out.println("Prima di effettuare l'operazione richiesta devi autenticarti tramite login!");
        return false;
    }
    private void publishPost(String comando) {
        //divido il comando nelle sue parti
        String[] parameters = comando.split("\"");

        if (parameters.length != 4 || parameters[1].length()>20 || parameters[1].isBlank() || parameters[3].isBlank() || parameters[3].length() > 500) {
            System.out.println("Errore di sintassi, digitare help per maggiori informazioni");
        } else {

            String s = sendAndReceive(comando);
            if (str_ok.equals(s)) {
                System.out.print("Post creato con successo\n");
            } else {
                System.out.println("Errore. Digita help per maggiori informazioni");
            }

        }
    }
    private void rate(String comando) {
        //divido il comando nelle sue parti
        String[] parameters = comando.split(" ");

        if (checkParameters(parameters, 3)) {
            try {
                //se non ho passato un intero non invio la richiesta al server
                if(Integer.parseInt(parameters[1]) >= 0) {
                    if (parameters[2].contains("+1") || parameters[2].contains("-1")) {

                        String s = sendAndReceive(comando);
                        switch (s) {
                            case str_ok ->             System.out.println("Il tuo voto e' stato accettato");
                            case str_err ->            System.out.println("Il post non appartiene al tuo feed o non segui l'autore originale del post");
                            case str_err_parameters -> System.out.println("Comando non accettato, digitare help per maggiori informazioni");
                            case "557" ->              System.out.println("Hai gia' votato questo post");
                        }
                    } else {
                        System.out.println("Passa +1 o -1 per votare il post");
                    }
                }else{
                    System.out.println("Bisogna passare un intero positivo come id del post");
                }
            } catch (NumberFormatException e) {
                System.out.println("Bisogna passare un intero positivo come id del post");
            }
        }
    }
    private void readAnswer(String answer) {

        int num = 0;                        //contiene il numero di caratteri letti
        String str = answer;
        char[] buff =new char[1024];
        try{

            while(!str.contains(str_terminator) && num != -1){
                System.out.print(str);
                num = this.reader.read(buff);
                str = new String(buff, 0, num);
            }
            System.out.println(str.substring(0,str.indexOf(str_terminator)));

        }catch(IOException e){
            System.out.println("Il server e' crashato \nSarai disconnesso");
            connectionError = true;
        }
    }
    protected String register(String comando) {

        RegistrationInterface remoteObject;
        String[] parameters = comando.split(" ");

        if (parameters.length>3 && parameters.length <= 3+User.NMAX_TAGS && !parameters[1].isEmpty() && !parameters[2].isEmpty()) {
            try {
                //mi creo un vettore a parte per i tags
                String[] tags = new String[parameters.length-3];

                //copio i tags nel vettore di tag
                System.arraycopy(parameters, 3, tags, 0, parameters.length - 3);
                Registry r = LocateRegistry.getRegistry(this.regHost, this.getRegPort());
                remoteObject = (RegistrationInterface) r.lookup("REGISTRATION-SERVER");
                switch (remoteObject.registerUser(parameters[1], parameters[2], tags)) {
                    case 0 -> {
                        System.out.println("Registrazione avvenuta con successo");
                        comando = "login " + parameters[1] + " " + parameters[2];
                    }
                    case 1 -> System.out.println("Spiacenti, questo username e' gia' in uso");
                    case 2 -> System.out.println("Puoi passare un massimo di 5 tag");
                    case 3 -> System.out.println("Operazione di registrazione non valida");
                }
                //Access exception, NotboundException RemoteException
            } catch (Exception e) {
                System.out.println("Error in invoking object method " + e.toString() + e.getMessage());
                connectionError =  true;
            }
        } else
            System.out.println("Syntax error: help per maggiori informazioni");
        return comando;
    }
    private void rewin(String comando) {
        //divido il comando nelle sue parti
        String[] parameters = comando.split(" ");
        if (checkParameters(parameters, 2)) {
            try {
                //se non ho passato un intero non invio la richiesta al server
                if(Integer.parseInt(parameters[1]) >= 0) {

                    String s = sendAndReceive(comando);
                    switch (s) {
                        case str_ok ->             System.out.println("Il rewin e' stato effettuato correttamente");
                        case str_err_parameters -> System.out.println("Comando non accettato, digitare help per maggiori informazioni");
                        case str_err ->            System.out.println("Non puoi fare il rewin dello stesso post due volte");
                        case "556" ->              System.out.println("Puoi fare il rewin di un post solo se appartiene ad un tuo feed e non ne sei l'autore");
                    }
                }else{
                    System.out.println("Bisogna passare un intero positivo come id del post");
                }
            } catch (NumberFormatException e) {
                System.out.println("Bisogna passare un intero positivo come id del post");
            }
        }
    }
    public void run(){

        try  {
            MulticastSocket ms = new MulticastSocket(this.getMulticastPort());
            InetSocketAddress group = new InetSocketAddress(InetAddress.getByName(this.multicastAddr), this.getMulticastPort());
            NetworkInterface netIf = NetworkInterface.getByName("wlan1");
            ms.joinGroup(group, netIf);
            byte[] buffer = new byte[WinsomeMainServer.getMessaggio().length()];
            while(!this.terminato) {
                try {
                    DatagramPacket dp = new DatagramPacket(buffer,buffer.length);
                    ms.receive(dp);
                    String s = new String(dp.getData());
                    if(s.equals(WinsomeMainServer.getMessaggio()))
                        System.out.println("\n" + s);
                } catch (IOException ex){
                    ex.printStackTrace();
                }
            }
            ms.leaveGroup(group,netIf);
        }
        catch (IOException ex){
            ex.printStackTrace();
        }

    }
    private void unfollow(String comando, String loggedUsername) {
        String[] parameters = comando.split(" ");
        if (checkParameters(parameters, 2)) {
            if (parameters[1].equals(loggedUsername))
                System.out.println("Non segui te stesso");
            else {
                String s = sendAndReceive(comando);
                switch (s) {
                    case str_ok ->  System.out.println("Hai smesso di seguire " + parameters[1]);
                    case str_err -> System.out.println("Non segui l'utente " + parameters[1]);
                    default ->      System.out.println("l'utente " + parameters[1] + " non esiste");
                }
            }
        }
    }
    private String sendAndReceive(String comando) {
        //invia il comando e ne riceve la risposta
        this.writer.write(comando);
        this.writer.flush();

        //metodo usato per leggere i codici di risposta del server
        int num;                        //contiene il numero di caratteri letti
        String str;
        char[] buff =new char[1024];
        try{
            num = this.reader.read(buff);

            //creo una stringa con i bytes recuperati dallo stream
            str = new String(buff, 0, num);

        }catch(IOException e){
            System.out.println("Il server e' crashato \nSarai disconnesso");
            connectionError = true;

            return "Server error";
        }

        return str;
    }
    private void showPost(String comando) {
        //divido il comando nelle sue parti
        String[] parameters = comando.split(" ");

        if (checkParameters(parameters, 3)) {
            try {
                if(Integer.parseInt(parameters[2]) >= 0) {
                    String s = sendAndReceive(comando);
                    switch (s) {
                        case str_err_parameters -> System.out.println("Errore. Digita help per maggiori informazioni");
                        case str_err ->            System.out.println("Post non trovato");
                        default ->                 readAnswer(s);
                    }
                }else
                    System.out.println("Bisogna passare un intero positivo come id del post");
            } catch (NumberFormatException e) {
                System.out.println("Bisogna passare un intero come id del post");
            }
        }
    }

    public void setMulticastPort(int Port) {
        this.multicastPort = Port;
    }
    public String getMulticastAddr() {
        return multicastAddr;
    }
    public int getMulticastPort() {
        return multicastPort;
    }
    public String getRegHost() {
        return regHost;
    }
    public int getRegPort() {
        return regPort;
    }

    public int getCallbackPort() {
        return callbackPort;
    }


}
