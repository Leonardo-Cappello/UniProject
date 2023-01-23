package server;

import java.rmi.RemoteException;
import java.rmi.server.RemoteServer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import client.NotifyInt;



public class ServerImpl extends RemoteServer implements ServerInterface{
	
	

	//lista dei client registrati
	private final ConcurrentHashMap<String,NotifyInt> clients;


	public ServerImpl()throws RemoteException {
		super( );
		clients = new ConcurrentHashMap<>( );
	}
	public int registerForCallback(NotifyInt clientInterface) throws RemoteException {

		if (clients.putIfAbsent(clientInterface.getUsername(),clientInterface) == null) {
			System.out.println("Un nuovo client registrato." );
            return 0;
		}
        return 1;
	}
	
	public void unregisterForCallback(String username,NotifyInt client) throws RemoteException {
		if(username == null)
			throw new NullPointerException();

		if (clients.remove(username,client))
			System.out.println("Client cancellato dal servizio callback");
		else
			System.out.println("Non è possibile cancellare il client");

	}

	protected void safeUnregister(String username) {
		System.out.println("Safe unregister per: " + username);
		clients.remove(username);
	}
	@Override
	public void loggingOp(String username,LinkedList<String> followers) throws RemoteException {
		if(followers.isEmpty())
			return;

		//prendo l'utente al quale voglio inviare l'aggiornamento
		NotifyInt client = clients.get(username);

		client.setFollowing(followers);
	}



	//metodo usato per notificare a un singolo utente
	public synchronized void notifyUser(String user,String user2,  boolean op) throws RemoteException, NullPointerException{

		if(user == null )
			throw new NullPointerException();

		//prendo l'utente al quale voglio inviare l'aggiornamento
		NotifyInt client = clients.get(user);

		if(client != null)
		{
			client.notifyEvent(user2,op);
			System.out.println("Notifica inviata");
		}
	}



}
