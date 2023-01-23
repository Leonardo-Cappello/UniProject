package server;
import dbPost.DbPosts;
import dbPost.Post;
import dbUtenti.DbUser;
import dbUtenti.User;

import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;

import static server.WinsomeMainServer.str_terminator;


public class ClientHandler implements Runnable{

	private final Socket sock;
	private final DbPosts dbPost;
	private final DbUser dbUsers;
	private final ServerImpl callbackServer;
	private final PostDeleter postCollector;
	private final RandomOrg randomorg;
	private final int port;
	ClientHandler(Socket client, DbPosts dbPost, DbUser dbUsers, ServerImpl cbServer, RandomOrg randomorg, PostDeleter postCollector, int port){
		this.sock = client;
		this.dbPost = dbPost;
		this.dbUsers = dbUsers;
		this.randomorg = randomorg;
		this.callbackServer = cbServer;
		this.postCollector = postCollector;
		this.port = port;
	}
	public void run() {
		char[] buff = new char[1024];                                    //buffer per la comunicazione
        String[] parameters;                                            //vettore di stringhe per l'interpretazione dei comandi

        String comando;                                                 //stringa contenente il comando passato dall'utente
        User logged = null;
        int ris;                                                        //usata per contare il numero di caratteri letti
		try	{
	            InputStreamReader reader = new InputStreamReader(sock.getInputStream());        				//lettore stream
				PrintWriter writer = new PrintWriter( sock.getOutputStream(),true);            		//scrittore stream

	            //leggo comando
	            ris = reader.read(buff);

	            //e l'utente termina la sessione senza usare la exit(ctrl-C o chiudendo il terminale)
	            if(ris == -1)
	                comando = "exit";
	            else
	            //creo una stringa usando il buffer dalla posizione 0 al numero di caratteri letti
	                comando = new String(buff, 0, ris);

			System.out.println("Ricevuto comando: " + comando);
			parameters = comando.split(" ");
			if(comando.contains("login")) {
				if(checkParameters(parameters, 3))
				{
					if((logged = this.dbUsers.checkPwd(parameters[1], parameters[2])) != null) {
						sendOK(writer);

						//aspetto che il client si registri al servizio di callback
						ris = reader.read(buff);

						//controlliamo se la registrazione al servizio di callback è andata a buon fine
						comando = new String(buff, 0, ris);
						if(comando.equals(WinsomeMainServer.str_ok)) {

							//se è andata a buon fine aggiorniamo la lista dei followers dell'utente
							//poi si aggiornerà la lista con il meccanismo di callback

							this.callbackServer.loggingOp(logged.getUsername(), this.dbUsers.getFollowers(logged.getUsername()));
							writer.write(Integer.toString(port));
							writer.flush();
						} else {
							//c'è stato qualche problema, il client termina e terminiamo anche il thread
							logged = null;
						}

					}
					else{
						sendERR(writer);
					}
				}
				else
					sendParametersErr(writer);

			}
			while(!comando.contains("exit") && logged != null)
			{
	                //divido il comando letto nelle sue parti
	                parameters = comando.split(" ");
	


	                if(comando.contains("list users")) {

						if(checkParameters(parameters,2)) {
							//invio di utenti con un tag in comune all'utente
							try {
								String temp = this.dbUsers.toString(logged);
								if(temp.isBlank()){
									sendERR(writer);
								}else{
									writer.write(temp+str_terminator);
									writer.flush();
								}
							}
							catch(NullPointerException e){
								sendParametersErr(writer);
							}
						}else
							sendParametersErr(writer);
	                }

	                if(comando.contains("list following")){

						String temp = logged.toString();
						if(temp.isBlank())
							sendERR(writer);
						else {
							writer.write(temp+str_terminator);
							writer.flush();
						}
	                }
	                if(comando.contains("follow ")) {
	                	if(checkParameters(parameters, 2)) {
							 if(comando.contains("unfollow")) {

								 if(!this.dbUsers.existingUsername(parameters[1])){
									 sendParametersErr(writer);
								 }else{
									 this.dbUsers.registerThreadUsers(logged.getUsername());
									 if(logged.unfollow(parameters[1])){
										 try {
											 this.callbackServer.notifyUser(parameters[1], logged.getUsername(), false);
										 }catch (RemoteException e){
											 //do nothing
										 }
										 sendOK(writer);
									 }
									 else
										 sendERR(writer);
									 this.dbUsers.unregisterThreadUsers(logged.getUsername());
								 }
							 }else{
								 //follow
								 if(!this.dbUsers.existingUsername(parameters[1])){
									 sendParametersErr(writer);
								 }else{
									 this.dbUsers.registerThreadUsers(logged.getUsername());
									 if(logged.follow(parameters[1])) {
										 try {
											 this.callbackServer.notifyUser(parameters[1], logged.getUsername(), true);
										 }catch (RemoteException e){
											 //do nothing
										 }
										 sendOK(writer);
									 }
									 else
										 sendERR(writer);
									 this.dbUsers.unregisterThreadUsers(logged.getUsername());
								 }
							 }
	                     }else
							sendParametersErr(writer);

	                }
	                if(comando.contains("blog")) {


						//invio dei post dell'utente all'utente, capire se da mandare come stringa o come rmi
						String temp = this.dbPost.toString(logged.getUsername());
						if(temp.isBlank())
							sendERR(writer);
						else {
							writer.write(temp+str_terminator);
							writer.flush();
						}
					}
	                if(comando.contains("post \"")) {

						parameters = comando.split("\"");
						if(checkParameters(parameters, 4)) {
							try{
								this.dbPost.registerThreadPost(logged.getUsername());
								this.dbPost.addPost(parameters[1],parameters[3],logged.getUsername());
								this.dbPost.unregisterThreadPost(logged.getUsername());
								//comunico che il comando è stato eseguito
								sendOK(writer);
							}
							catch (NullPointerException e){
								sendParametersErr(writer);
							}
						}else{
							sendParametersErr(writer);
						}

	                }
	                if(comando.contains("show feed")) {

						if(checkParameters(parameters, 2)) {

							String temp = this.dbPost.showFeed(logged.getFollowing());
							if (temp.isBlank()) {
								sendERR(writer);
							}else{
								writer.write(temp+str_terminator);
								writer.flush();
							}


						}else{
							sendParametersErr(writer);
						}
	                }
	                if(comando.contains("show post")) {

						if(checkParameters(parameters, 3)) {

							Post temp;
							try{

								temp = this.dbPost.showPost(Integer.parseInt(parameters[2]));

								if (temp != null) {
									writer.write(temp+str_terminator);
									writer.flush();
								}else{
									sendERR(writer);
								}
							}
							catch (NullPointerException | NumberFormatException e){
								sendParametersErr(writer);
							}

						}else{
							sendParametersErr(writer);
						}

	                }
	                if(comando.contains("delete post")) {

						if(checkParameters(parameters, 3)) {
								Post temp;
								this.dbPost.registerThreadPost(logged.getUsername());
								if((temp = this.dbPost.deletePost(logged.getUsername(), Integer.parseInt(parameters[2]))) != null ){
									//possiamo cancellare tutti i rewin solo se è l'autore originale del post
									if(temp.getAutore().equals(logged.getUsername()))
										this.postCollector.addPost(Integer.parseInt(parameters[2]));
									sendOK(writer);
								}else{
									sendERR(writer);
								}
								this.dbPost.unregisterThreadPost(logged.getUsername());
						}else{
							sendParametersErr(writer);
						}
	                }
	                if(comando.contains("rewin")) {

						if(checkParameters(parameters, 2)) {

							Post temp;
							try{
								//recupero il post, se il valore restituito è null allora il post non esiste o non appartiene al feed
								temp = this.dbPost.getPost( Integer.parseInt(parameters[1]),logged.getFollowing());
								if (temp != null){

										//effettuo il rewin, se ritorna false allora ho già fatto il rewin del post
										this.dbPost.registerThreadPost(logged.getUsername());
										if(this.dbPost.addPost(logged.getUsername(),temp)){
											sendOK(writer);
										}else{

											//l'utente ha provato a fare il rewin dello stesso post due volte
											//rispondo con 555
											sendERR(writer);
										}
										this.dbPost.unregisterThreadPost(logged.getUsername());
								}else{

									//l'utente ha passato un id non valido, rispondo con 556
									writer.write("556");
									writer.flush();
								}
							}
							catch (NullPointerException | NumberFormatException e){
								sendParametersErr(writer);
							}
						}else{
							sendParametersErr(writer);
						}
	                }
	                if(comando.contains("rate")) {
						if(checkParameters(parameters, 3)) {
							Post temp;
							try {
								temp = this.dbPost.getPost(Integer.parseInt(parameters[1]), logged.getFollowing());
							}catch (NumberFormatException e){
								temp = null;
								sendParametersErr(writer);
							}
							//verifico che il post esista, altrimenti do errore
							if(temp == null){
								sendERR(writer);
							}
							else{

									//se è positivo aggiorno la lista e invio OK
									if (parameters[2].contains("+1")) {
										this.dbPost.registerThreadPost(logged.getUsername());
										if (temp.likePost(logged.getUsername()))
											sendOK(writer);
										else{
											writer.write("557");
											writer.flush();
										}
										this.dbPost.unregisterThreadPost(logged.getUsername());
									} else {
										// se è negativo aggiorno la lista e invio ok
										this.dbPost.registerThreadPost(logged.getUsername());
										if (parameters[2].contains("-1") && temp.dislikePost(logged.getUsername())) {
											sendOK(writer);
										} else {
											//se il post è stato votato dall'utente corrente restituisco errore
											writer.write("557");
											writer.flush();
										}
										this.dbPost.unregisterThreadPost(logged.getUsername());
									}
							}
						}else{
							sendParametersErr(writer);
						}
	                }
	                if(comando.contains("comment")) {
						String[] parameters1 = comando.split("\"");
						if(checkParameters(parameters1, 2) && parameters.length > 2) {
							Post temp;
							try{
								temp = this.dbPost.getPost( Integer.parseInt(parameters[1]),logged.getFollowing());
								//se il post esiste
								if (temp != null) {
									//aggiungo il commento e restituisco ok
									this.dbPost.registerThreadPost(logged.getUsername());
									temp.addComment(logged.getUsername(), parameters1[1]);
									this.dbPost.unregisterThreadPost(logged.getUsername());
									writer.write(WinsomeMainServer.str_ok);
									writer.flush();
								}else{
									sendERR(writer);
								}
							}
							catch (NullPointerException | NumberFormatException e){
								sendParametersErr(writer);
							}

						}else{
							sendParametersErr(writer);
						}
	                }
	                if(comando.contains("wallet")) {
	                	if(comando.contains("btc")) {
							if(checkParameters(parameters, 2)) {
								Double value = logged.totalWalletValue();
								writer.write(randomorg.convertValue(value));
								writer.flush();

							}else{
								sendParametersErr(writer);
							}
	                    }else {
	                    	//get wallet
							if(checkParameters(parameters, 1)) {
								String temp = logged.walletToString();
								if (temp.isBlank()) {
									sendERR(writer);
								}else{
									writer.write(temp + str_terminator);
									writer.flush();
								}
							}else{
								sendParametersErr(writer);
							}

	                    }
	                }
					if(comando.contains("logout")) {
						sendOK(writer);
						comando = "exit";
					}else{

						ris = reader.read(buff);

						//se l'utente termina la sessione senza usare la exit(ctrl-C o chiudendo il terminale)
						if(ris == -1)
							comando = "exit";
						else
							//creo una stringa usando il buffer dalla posizione 0 al numero di caratteri letti
							comando = new String(buff, 0, ris);
					}
					//System.out.println("Ricevuto comando: " + comando);
	            }
				this.sock.close();
		}catch (IOException e) {
			System.out.println("il client ha chiuso la connessione in modo inaspettato" );
			if(logged != null) {
				this.callbackServer.safeUnregister(logged.getUsername());
				this.dbPost.safeRemove(logged.getUsername());
				this.dbUsers.safeRemove(logged.getUsername());
			}
		}
		if(logged != null)
			System.out.println("utente " + logged.getUsername() + " disconnesso");

	}
	
	//controlla se sono stati passati esattamente i parametri
    private static boolean checkParameters(String[] p, int i)
    {
        if(p.length != i)
        {
            System.out.println("Syntax error: help per maggiori informazioni");
            return false;
        }
        else
            return true;
    }

  //usato per informare l'utente che è andato tutto a buon fine
    private void sendOK(PrintWriter writer){

        //System.out.println(WinsomeMainServer.str_ok);
        writer.write(WinsomeMainServer.str_ok);
        writer.flush();
    }
    //usato per informare l'utente di un errore
    private void sendERR(PrintWriter writer){

        //System.out.println(WinsomeMainServer.str_err);
        writer.write(WinsomeMainServer.str_err);
        writer.flush();
    }

    //usato per informare l'utente che ha usato un comando con sintassi sbagliata
    private void sendParametersErr(PrintWriter writer){

        //System.out.println(WinsomeMainServer.str_err_parameters);
        writer.write(WinsomeMainServer.str_err_parameters);
        writer.flush();
    }
}
