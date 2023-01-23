package client;

import java.rmi.*;
import java.util.LinkedList;
public interface NotifyInt extends Remote {
	/* Metodo invocato dal server per notificare un evento ad un
	client remoto. */
	void notifyEvent(String username,boolean op) throws RemoteException;
	LinkedList<String> getFollowers() throws RemoteException;
	String getUsername() throws RemoteException;
	void setFollowing(LinkedList<String> followers) throws RemoteException;
}