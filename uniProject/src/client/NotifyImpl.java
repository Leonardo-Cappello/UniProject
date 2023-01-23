package client;
import java.rmi.*;
import java.rmi.server.*;
import java.util.LinkedList;
public class NotifyImpl extends RemoteObject implements NotifyInt {
	
	/* crea un nuovo callback client */


	private final String username;
	private LinkedList<String> followers;
	public NotifyImpl(String username) throws RemoteException{
		this.username = username;
		followers = new LinkedList<>();
	}
	

	
	public void notifyEvent(String username, boolean op) throws RemoteException {
		String returnMessage;
		if(op) {
			 returnMessage = username + " ha iniziato a seguirti";
			 if(!this.followers.contains(username)) {
				 synchronized (this.followers) {
					 this.followers.add(username);
				 }
			 }
		}else {
			returnMessage = username + " ha smesso di seguirti ";
			synchronized (this.followers) {
				this.followers.remove(username);
			}
		}
		
		System.out.println(returnMessage);
	}

	public LinkedList<String> getFollowers(){
		return this.followers;
	}
	public String getUsername() {
		return this.username;
	}

	@Override
	public void setFollowing(LinkedList<String> followers) {
		synchronized (this.followers) {
			this.followers = followers;
		}
	}

}