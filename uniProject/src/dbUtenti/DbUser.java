package dbUtenti;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dbPost.Comment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Locale;


public class DbUser implements Runnable{
	private LinkedList<User> users = new LinkedList<>();
	private float curator = 0;
	private float publisher = 0;
	private boolean stopDb = false;
	private String pathDb;
	private long frequenza;  //frequenza di backup
	private boolean paying = false;
	private final LinkedList<String> queue = new LinkedList<>();
	private boolean modifyingJson = false;

	 public DbUser (String path) {
		 this.pathDb = path;
		 initializeDb(path);
	}

	public DbUser () {
		super();
	}

	public LinkedList<User> getDb () {
		return this.users;
	}
	public long getFrequenza() {
		return this.frequenza;
	}
	public void setFrequenza(long frequenza){ this.frequenza = frequenza; }
	public void stopDb(){
		stopDb = true;
	}
	public float getRewardCurators(){
		 return this.curator;
	}
	public float getRewardPublisher(){
		 return this.publisher;
	}
	public void initializeDb (String path) {

		File file = new File(path);
		ObjectMapper objectMapper = new ObjectMapper();

		try{
			//deserializzo
			if(file.exists())
				this.users = objectMapper.readValue(file, new TypeReference<LinkedList<User>>(){});
			else
				this.users = new LinkedList<>();
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	public void updateBackupDb() {
		synchronized (this.queue) {
			this.modifyingJson = true;

			while(this.queue.size() != 0  || this.paying) {
				try {
					this.queue.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				File file = new File(pathDb);
				file.createNewFile();

				FileWriter fileWriter = new FileWriter(file);

				//serializzo
				objectMapper.writeValue(fileWriter, this.getDb());
			} catch (IOException e) {
				e.printStackTrace();
			}

			this.modifyingJson = false;
			this.queue.notifyAll();


		}
	}

	public void safeRemove(String user){
		synchronized (this.queue){
			if(this.queue.contains(user)){
				this.queue.remove(user);
				this.queue.notifyAll();
			}
		}
	}
	public void registerThreadUsers(String user){
		synchronized (this.queue){
			while(modifyingJson || paying){
				try {
					this.queue.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			this.queue.add(user);
		}

	}
	public void unregisterThreadUsers(String user){
		synchronized (this.queue){
			this.queue.remove(user);
			this.queue.notifyAll();
		}

	}

	public void run(){
		while(!stopDb){
			try {
				Thread.sleep(frequenza);
			} catch (InterruptedException e) {
				System.out.println("Sleep interrotta");
			}
			this.updateBackupDb();
			//System.out.println("Backup Users Aggiornato");

		}
	}

	public String toString(User u) throws NullPointerException{
		 if (u == null){
			 throw new NullPointerException();
		 }
		StringBuilder temp = new StringBuilder();
		String[] tag;
		String[] tag2 = u.getTags();
		boolean trovato;
		synchronized (this.users) {
			for (User user : users) {
				trovato = false;
				tag = user.getTags();
				if (!u.getUsername().equals(user.getUsername())) {

					//controllo dei tag
					for (String s : tag) {
						for (String value : tag2) {
							if (s.equals(value)) {
								trovato = true;
								break;
							}
						}
						//se abbiamo trovato un tag uguale allora smettiamo di scorrere i restanti tag
						if (trovato) {
							break;
						}
					}
				}

				//aggiungiamo l'utente e i suoi tag in formato "username  | tag1,tag2,tag3 \n "
				if (trovato) {
					if (temp.isEmpty()) {
						temp.append(String.format("\t %10s  ", "Utente"));
						temp.append(String.format("\t| \t%10s  ", "Tag"));
						temp.append("\n------------------------------------------------------------------\n");
					}
					temp.append(String.format("\t %10s  ", user.getUsername()));
					temp.append(String.format("\t| \t%10s  ", tag[0]));
					for (int i = 1; i < tag.length; i++) {
						temp.append(", " + tag[i]);
					}
					temp.append("\n");
				}
			}
		}
		return temp.toString();
	}


	public int registerUser ( String username, String password, String[] tags )  {
		if (password.isEmpty() || username.isEmpty()) {
			return 3;
		}

		//se abbiamo passato più di 5 tag
		if (tags.length > User.NMAX_TAGS) {
			return 2;
		}

		//noinspection SynchronizeOnNonFinalField
		synchronized (this.users) {

		//controllo se esiste già un utente con quell'username

			boolean trovato = false;
			for (User user : users) {
				if (user.getUsername().equals(username)) {
					trovato = true;
					break;
				}
			}
			if (trovato)
				return 1;

			//converto i tag in minuscolo
			for (int i = 0; i < tags.length; i++) {
				tags[i] = tags[i].toLowerCase(Locale.ROOT);
			}

			//registro l'utente nel db mantenendo l'ordinamento alfabetico
			int index = -1;
			for (int i = 0; i < this.users.size(); i++) {


				if (username.compareTo(this.users.get(i).getUsername()) < 0) {
					index = i;
				}
			}
			this.registerThreadUsers(username);
			if (index == -1) {
				this.users.add(new User(username, password, tags));
			} else {
				this.users.add(index, new User(username, password, tags));
			}
			this.unregisterThreadUsers(username);
		}
		return 0;
	}

	public User checkPwd ( String name, String pwd ) {
		 synchronized (this.users) {
			 for (User user : users) {
				 //se abbiamo trovato l'utente
				 if (user.getUsername().equals(name)) {
					 /*se l'utente ha inserito la password corretta ritorno l'istanza che lo rappresenta
					  *ritorna null se non le password non corrispondono
					  */
					 if (user.getPassword().equals(pwd))
						 return user;
					 else
						 return null;
				 }
			 }
		 }
		return null;
	}

	public Boolean existingUsername(String username){
		 synchronized (this.users) {
			 for (User user : users) {
				 if (user.getUsername().equals(username))
					 return true;
			 }
		 }
		return false;
	}

	public LinkedList<String> getFollowers(String username) throws NullPointerException{
		if (username == null)
			throw new NullPointerException();
		LinkedList<String> temp = new LinkedList<>();

		//sincronizza con operazione di registrazione
		synchronized (this.users) {
			for (User user : this.users) {
				if (user.getFollowing().contains(username))
					temp.add(user.getUsername());
			}
		}
		return temp;
	}

	public void payUsers (LinkedList<Comment> comments, int id,String autore) {

		String str = comments.getFirst().getUsername();

		//nell' username del primo elemento della lista inserisco la reward da dividere
		double reward = Double.parseDouble(str);

		//se il post non ha generato interesse in questa iterazione(nessun commento o like)
		if(reward == 0){
			return;
		}
		comments.removeFirst();
		double rewardCurators = reward * this.curator/100;
		synchronized (this.queue) {
			//assegno la reward al publisher del post
			User u = this.getUserIstance(autore);

			//u non può essere uguale a null
			assert u != null;
			u.addTransaction(id, 0, (reward * this.publisher / 100));

			//calcolo la reward per ogni singolo curator
			rewardCurators = rewardCurators / comments.size();
			for (Comment comment : comments) {

				//prendo l'utente da retribuire
				u = this.getUserIstance(comment.getUsername());
				//aggiungo la nuova transazione
				assert u != null;
				u.addTransaction(id, comment.getId(), rewardCurators);
			}
		}
	}

	//usato solo nel metodo payuser
	private User getUserIstance(String username){

		 for (User user : this.users) {
			 if (user.getUsername().equals(username)) {
				 return user;
			 }
		 }

		return null;
	}


	public void setRewardPublisher(float publisher) {

		 if(publisher > 100){
				 System.out.println("ATTENZIONE: i valori inseriti nel file di configurazione non possono essere usati, si procederà usando: " + this.curator + " "+ this.publisher);
		 }else{
			 this.publisher = publisher;
		 }
	}

	public void setRewardCurator(float curator) {
		if(curator > 100){
			System.out.println("ATTENZIONE: i valori inseriti nel file di configurazione non possono essere usati, si procederà usando: " + this.curator + " "+ this.publisher);
		}else{
			this.curator = curator;
		}

	}


	public void paying() {
		synchronized (queue) {
			this.paying = !this.paying;
			this.queue.notifyAll();
		}
	}

}
