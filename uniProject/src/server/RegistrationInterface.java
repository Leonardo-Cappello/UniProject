package server;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RegistrationInterface extends Remote{
    
    //metodo usato per registrare utenti
    int registerUser(String username, String password, String[] tags) throws NullPointerException, RemoteException;
}