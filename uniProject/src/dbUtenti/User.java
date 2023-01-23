package dbUtenti;

import java.util.LinkedList;

public class User {

	private String username;
	private String password;
	private String[] tags;
	private LinkedList<String> following; 			//lista degli username seguiti
	private LinkedList<Transazione> wallet;
	public static final int NMAX_TAGS = 5;

	User(){
		super();
	}

	User(String username, String password, String[] tags){
		this.username = username;
		this.password = password;
		this.tags = tags;
		this.wallet = new LinkedList<>();
		this.following = new LinkedList<>();

	}

	public LinkedList<Transazione> getWallet(){
		return this.wallet;
	}

	public String getUsername() {
		return this.username;
	}

	public String getPassword() {
		return this.password;
	}

	public String[] getTags() {
		return this.tags;
	}

	public LinkedList<String> getFollowing() { return following; }

	public void addTransaction(int idPost, int idCommento,double wnc) {
		synchronized (wallet) {
			wallet.add(new Transazione(idPost, idCommento, wnc));
		}
	}

	public double totalWalletValue() {

		//se l'utente non ha ancora pubblicato nulla ritorno stringa vuota
		if (this.wallet.isEmpty())
			return 0;

		double tot = 0;
		synchronized (wallet) {
			for (Transazione t : wallet)
				tot += t.getAmountWnc();
		}
		return tot;
	}

	public String walletToString(){
		StringBuilder temp = new StringBuilder();

		//se l'utente non ha ancora pubblicato nulla ritorno stringa vuota
		if (this.wallet.isEmpty())
			return temp.toString();

		double tot = 0;
		temp.append(String.format("\t%9s |\t %12s \t| %10s \t | %15s\n","idPost","idCommento","Guadagno", "Data"));
		temp.append( "----------------------------------------------------------------\n");
		//per ogni transazione stampa id del post a cui era riferito, id commento e l'ammontare di wincoin
		synchronized (wallet) {
			for (Transazione t : wallet) {
				if (t.getAmountWnc() > 0) {
					switch (t.getIdCommento()) {
						case 0 -> temp.append(String.format("\t%9s |\t %12s \t| %.10f \t | %15s\n", t.getIdPost(), "Publisher", t.getAmountWnc(), t.getData()));
						case -1 -> temp.append(String.format("\t%9s |\t %12s \t| %.10f \t | %15s\n", t.getIdPost(), "Like", t.getAmountWnc(), t.getData()));
						default -> temp.append(String.format("\t%9s |\t %12s \t| %.10f \t | %15s\n", t.getIdPost(), t.getIdCommento(), t.getAmountWnc(), t.getData()));
					}
					tot += t.getAmountWnc();
				}
			}
		}

		temp.append( "----------------------------------------------------------------\n");
		temp.append(String.format("In totale hai guadagnato %.10f",tot));
		return temp.toString();
	}

	//crea una stringa contenente tutti i clienti che l'utente segue con formato
	/*ecco gli utenti che segui:
	*username1
	*username2 ecc
	 */
	public String toString(){
		StringBuilder temp = new StringBuilder();

		//se l'utente non segue nessuno
		if (this.following.isEmpty())
			return temp.toString();
		temp.append( "Ecco gli utenti che segui:\n");

		for (String s : this.following)
			temp.append(s + "\n");

		return temp.toString();
	}
	public boolean follow(String username) throws NullPointerException {

		if (username == null) {
			throw new NullPointerException();
		}
			if (!this.following.contains(username)) {
				this.following.add(username);
				return true;
			}

		return false;
	}
	
	public boolean unfollow(String username) throws NullPointerException {
		if(username == null) {
			throw new NullPointerException();
		}
			if (this.following.contains(username)) {
				this.following.remove(username);
				return true;
			} else {
				return false;
			}

	}


}
