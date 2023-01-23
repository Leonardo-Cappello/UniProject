package server;

import dbPost.Comment;
import dbPost.DbPosts;
import dbUtenti.DbUser;

import java.io.*;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



public class WinsomeMainServer extends Thread implements RegistrationInterface {
	//messaggi predefiniti del server
	private static final String messaggio = "Sono state assegnate le nuove reward!!";
	public static final String str_ok = "222 (OK)";
	public static final String str_err = "555 (Server Error)";
	public static final String str_terminator = "223 (END)";
	public static final String str_err_parameters = "450 (Client Error)";
	private int tcpPort = 0;
	private String multicastAddr = null;
	private int multicastPort = 0;
	private String regHost = null;
	private int regPort = 0;
	private int	timeout = 0;
	private int callbackPort;
	private final DbUser dbUsers;
	private final DbPosts dbPosts;
	private final RandomOrg randomorg;
	private final PostDeleter postCollector;
	private long intervalloRicompensa = 0;
	private long intervalloPolling = 0;
	private boolean stopServer = false;

	WinsomeMainServer(String filepath,String pathUsersdb,String pathPostdb){
		dbUsers = new DbUser(pathUsersdb);
		dbPosts = new DbPosts(pathPostdb);
		postCollector = new PostDeleter(dbPosts);
		inizializzaParametri(filepath);
		this.randomorg = new RandomOrg(this.intervalloPolling);

	}
	
	
	public static void main(String[] args) {
		String filepath = null;
		String pathUsersdb= "./src/databaseUser.json";
		final String pathPostdb= "./src/databasePost.json";
		if(args.length == 0){
			System.exit(-1);
		}else{
			filepath = args[0];
		}

		WinsomeMainServer wms = new WinsomeMainServer(filepath,pathUsersdb,pathPostdb);
		wms.registrationsetup(wms);
		ServerImpl callbackserver;

		try { /*registrazione presso il registry */
			callbackserver = new ServerImpl();

			//Esportazione dell'Oggetto in grado di ricevere richieste sulla porta specificata
			ServerInterface stub = (ServerInterface) UnicastRemoteObject.exportObject(callbackserver, 0);
			String name = "Server-callback";

			// Creazione ed esportazione di un registry in grado di ricevere richieste sulla porta specificata
			LocateRegistry.createRegistry(wms.getCallbackPort());
			Registry registry = LocateRegistry.getRegistry(wms.getRegHost(),wms.getCallbackPort());

			// Pubblicazione dello stub nel registry
			registry.bind(name,stub);

			wms.startServer(callbackserver);
		}catch(RemoteException | AlreadyBoundException e){
			e.printStackTrace();
		}
	}
	private void startServer(ServerImpl callbackServer){
		// dichiara il threadpool che si occupa della gestione delle richieste
		ExecutorService pool = Executors.newCachedThreadPool();

		try (
				// welcoming socket
				ServerSocket tcpserver = new ServerSocket(this.getTcpPort())
		) {
			pool.execute(this);
			pool.execute(this.postCollector);
			pool.execute(this.randomorg);
			pool.execute(this.dbPosts);
			pool.execute(this.dbUsers);

			System.out.println("Server operativo");

			while (true) {

				// rimane in attesa di una richiesta di connessione
				Socket client = tcpserver.accept();
				// invio al threadpool la richiesta di gestione del client
				pool.execute(new ClientHandler(client,this.getDbPosts(),this.getdbUsers(),callbackServer, this.randomorg,this.postCollector,this.multicastPort));
			}
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void stopServer(){
		this.stopServer = true;
	}

	@Override
	public void run(){
		try{
			InetAddress ia=InetAddress.getByName(this.multicastAddr);
			byte[] data = WinsomeMainServer.messaggio.getBytes();

			//pacchetto da inviare
			DatagramPacket dp = new DatagramPacket(data,data.length,ia, this.getMulticastPort());

			//multicast Socket per l'invio del messaggio
			DatagramSocket ms = new DatagramSocket(this.getMulticastPort()+2);

			while(!this.stopServer){
				try {
					Thread.sleep(intervalloRicompensa);
				} catch (InterruptedException e) {
					System.out.println("Sleep interrotta");
				}
				//se il server ha finito ma il thread era in sleep salto la fase di pagamento, per velocizzare la chiusura
				this.dbPosts.paying();
				this.dbUsers.paying();
				if(!this.stopServer) {

					for (String autore : this.dbPosts.publishers()) {
						HashMap<Integer, LinkedList<Comment>> temp = this.dbPosts.calculateRewards(autore);
						for (Integer i : temp.keySet()) {
							this.dbUsers.payUsers(temp.get(i), i, autore);
						}
					}
					ms.send(dp);
					System.out.println("Reward Calcolate");
				}

				this.dbPosts.paying();
				this.dbUsers.paying();
			}
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
	}
	private void inizializzaParametri(String filename) {
		FileReader f = null;
		try {
			f = new FileReader(filename);
		} catch (FileNotFoundException e1) {
			System.out.println("File di configurazione non trovato");
			System.exit(-1);
		}
		
		BufferedReader br;
		br = new BufferedReader(f);
		String s;

		//lettura file di configurazione e assegnamento dei valori
		try {
			while((s=br.readLine()) !=  null) {
				if(!s.startsWith("#")) {
					String temp = s.substring(s.indexOf('=')+1);

					if(s.contains("TCPPORT")) {
						tcpPort = Integer.parseInt(temp);
					}
					if(s.contains("BACKUPUSER")) {
						this.dbUsers.setFrequenza(Long.parseLong(temp));
					}
					if(s.contains("BACKUPPOST")) {
						this.dbPosts.setFrequenza(Long.parseLong(temp));
					}
					if(s.contains("MULTICAST")) {
						multicastAddr = temp;
					}
					if(s.contains("MCASTPORT")) {
						multicastPort = Integer.parseInt(temp);
					}
					if(s.contains("REGHOST")) {
						regHost = temp;
					}
					if(s.contains("REGPORT")) {
						regPort = Integer.parseInt(temp);
					}
					if(s.contains("TIMEOUT")) {
						timeout = Integer.parseInt(temp);
					}
					if(s.contains("CALLBACK")) {
						callbackPort = Integer.parseInt(temp);
					}
					if(s.contains("INTERVALLO")) {
						if(Long.MAX_VALUE > Long.parseLong(temp))
							intervalloRicompensa = Long.parseLong(temp);
					}
					if(s.contains("CURATOR")) {
						this.dbUsers.setRewardCurator(Float.parseFloat(temp));
					}
					if(s.contains("PUBLISHER")) {
						this.dbUsers.setRewardPublisher(Float.parseFloat(temp));
					}
					if(s.contains("POLLING")){
						this.intervalloPolling = (Long.parseLong(temp));
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(this.intervalloPolling == 0 || this.dbUsers.getRewardCurators() == 0 || this.dbUsers.getRewardPublisher() == 0
				|| this.intervalloRicompensa == 0 || this.callbackPort== 0 || this.regPort == 0 || this.tcpPort == 0
		|| this.dbUsers.getFrequenza() == 0 || this.dbPosts.frequenza() == 0 || this.regHost == null || this.multicastAddr == null){
			System.out.println("File di configurazione non completo");
			System.exit(-1);
		}
	}
	public void registrationsetup(WinsomeMainServer wms) {
		try {
		
			//Esportazione dell'Oggetto in grado di ricevere richieste sulla porta specificata
			RegistrationInterface stub = (RegistrationInterface)UnicastRemoteObject.exportObject(wms,wms.getregPort());
			// Creazione ed esportazione di un registry in grado di ricevere richieste sulla porta specificata

			LocateRegistry.createRegistry(wms.getregPort());
			Registry r=LocateRegistry.getRegistry(wms.getRegHost(),wms.getregPort());
			// Pubblicazione dello stub nel registry
			r.rebind("REGISTRATION-SERVER", stub);
			
		}
		// se si ha un errore di comunicazione
		catch (RemoteException e) {
			System.out.println("Errore di comunicazione " + e);
			e.printStackTrace();
		}
		
	}


	public int registerUser(String username, String password, String[] tags) throws NullPointerException, RemoteException {
		 return this.dbUsers.registerUser(username, password, tags);
	}

	public int getCallbackPort() {
		return this.callbackPort;
	}
	
	
	public DbUser getdbUsers() {
		return this.dbUsers;
	}
	
	
	public int getregPort() {
		return this.regPort;
	}

	public int getTcpPort() {
		return tcpPort;
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


	public int getTimeout() {
		return timeout;
	}

	public static String getMessaggio() {
		return messaggio;
	}
	public DbPosts getDbPosts() {
		return dbPosts;
	}
	
}
