package server;
import client.NotifyInt;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;


public interface ServerInterface  extends Remote{
	/* registrazione per la callback */
	int registerForCallback(NotifyInt clientInterface) throws RemoteException;
	
	/* cancella registrazione per la callback */
	void unregisterForCallback (String username,NotifyInt client) throws RemoteException;

    void loggingOp(String user,LinkedList<String> followers) throws RemoteException;
}
