package dbPost;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

public class DbPosts implements Runnable{

	private HashMap<String,LinkedList<Post>> posts  = new HashMap<>();
	private AtomicInteger id;
	private boolean paying = false;
	private transient long frequenza = 0;  //frequenza di backup
	private boolean stopDb = false;
	private String pathDb;
	private LinkedList<String> queue = new LinkedList<>();
	private boolean modifyingJson = false;
	DbPosts() {
		super();
	}
	public DbPosts(String path) {
		pathDb = path;
		initializeDb(path);
	}

	public AtomicInteger getId() {
		return this.id;
	}
	public void setFrequenza(long frequenza){
		this.frequenza = frequenza;
	}
	public long frequenza() {
		return this.frequenza;
	}
	public void stopDb(){
		stopDb = true;
	}
	public HashMap<String, LinkedList<Post>> getdbPost() {
		return this.posts;
	}
	public java.util.Set<String> publishers() {
		return this.posts.keySet();
	}
	public void paying() {
		synchronized (queue) {
			this.paying = !this.paying;
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
			//System.out.println("Backup Posts Aggiornato");
		}
	}

	public void initializeDb (String path) {

		// Create File
		File file = new File(path);
		ObjectMapper objectMapper = new ObjectMapper();

		try{
			//deserializzo
			if(file.exists()) {

				DbPosts db = objectMapper.readValue(file, new TypeReference<DbPosts>(){});
				this.id = db.getId();
				this.posts = db.getdbPost();
			} else{
				this.posts = new HashMap<>();
				this.id = new AtomicInteger();
			}
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
					objectMapper.writeValue(fileWriter, this);


				} catch (IOException e) {
					e.printStackTrace();
				}
				this.modifyingJson = false;
				this.queue.notifyAll();


		}
	}


	// aggiunge un post alla lista dei post, se esiste una lista lo aggiungo altrimenti ne creo una nuova
	public void addPost(String titolo, String contenuto, String autore) throws NullPointerException {
		 if(titolo == null || contenuto ==  null || autore == null) {
			 throw new NullPointerException();
		 }
		LinkedList<Post> temp;
		Post p = new Post(titolo,contenuto,autore,this.id.getAndIncrement());
		if(this.posts.containsKey(p.getAutore())) {
			temp = this.posts.get(p.getAutore());
		} else {
			temp = new LinkedList<>();
		}

		synchronized (temp) {
			temp.add(p);
			this.posts.put(p.getAutore(), temp);
		}
	}

	//usata in rewin
	public boolean addPost(String user,Post p) throws NullPointerException {
		if(p == null) {
			throw new NullPointerException();
		}
		LinkedList<Post> temp;
		if(this.posts.containsKey(user)) {
			temp = this.posts.get(user);
		} else {
			temp = new LinkedList<>();
		}
		synchronized (temp) {
			for (Post post : temp) {
				if (post.getRewinId() == p.getRewinId() || p.getIdPost() == post.getRewinId())
					return false;            //non può fare due volte il rewin dello stesso post
			}
			Post post;
			if(p.getRewinId() == -1)
				//rewin di un post originale
				 post = new Post(p.getTitolo(), p.getContenuto(), p.getAutore(), this.id.getAndIncrement(),p.getIterazioni(),p.getIdPost());
			else
				//rewin del rewin
				post = new Post(p.getTitolo(), p.getContenuto(), p.getAutore(), this.id.getAndIncrement(),p.getIterazioni(),p.getRewinId());
			temp.add(post);
			this.posts.put(user, temp);
		}
		return true;
	}
	
	public Post deletePost(String autore, int id) {
		LinkedList<Post> temp;
		
		//se l'autore non ha mai pubblicato restituisco null
		if(this.posts.containsKey(autore)) {

			synchronized (posts.get(autore)) {
				temp = posts.get(autore);
				for (Post post : temp) {
					if (post.getIdPost() == id) {

						//rimuovo il post
						temp.remove(post);

						return post;
					}
				}
			}
		}
		return null;
	}


	public void deletePost(int id) {
		for(String user: this.posts.keySet()) {

			//temporaneamente l'utente user non può cancellare post
			synchronized (this.posts.get(user)) {
				for (Post temp : this.posts.get(user)) {
					if(temp.getRewinId() == id) {
						this.posts.get(user).remove(temp);
						break;
					}
				}
			}
		}
	}

	public Post getPost(int id,LinkedList<String> following) {
		for (String username: following) {
			synchronized (this.posts.get(username)) {
				for (Post p: this.posts.get(username)) {
					if (p.getIdPost() == id) {
						return p;
					}
				}
			}
		}
		return null;
	}

	//usato nel comando blog
	public String toString(String autore) {
		StringBuilder temp = new StringBuilder();

		//se l'utente non ha ancora pubblicato nulla ritorno stringa vuota
		if (this.posts.get(autore) == null || this.posts.get(autore).isEmpty())
			return temp.toString();
		temp.append(String.format("\t%5s |%10s \t| %8s ","id","autore","titolo\n"));
		temp.append( "-----------------------------------------------------------\n");
		synchronized (this.posts.get(autore)) {
			for (Post post : this.posts.get(autore))
				temp.append(String.format("\t%5d |%10s \t| %s \n", post.getIdPost(), post.getAutore(), post.getTitolo()));
		}
		return temp.toString();
	}

	public String showFeed(LinkedList<String> followed) {
		StringBuilder temp = new StringBuilder();

		//se l'utente non ha ancora pubblicato nulla ritorno stringa vuota
		if (followed == null || followed.isEmpty())
			return temp.toString();
		temp.append(String.format("\t%5s |%10s \t| %8s ","id","autore","titolo\n"));
		temp.append( "-----------------------------------------------------------\n");

		//per ogni utente seguito stampa id autore titolo post
		for (String user : followed) {

			if(this.posts.get(user) != null) {
				synchronized (this.posts.get(user)) {
					for (Post post : this.posts.get(user)) {
						temp.append(String.format("\t%5d |%10s \t| %s \n", post.getIdPost(), post.getAutore(), post.getTitolo()));
					}
				}
			}

		}
		return temp.toString();
	}

	public HashMap<Integer,LinkedList<Comment>>  calculateRewards(String autore) {
		/*guadagno = log (max (sommatoria di tutti i nuovi like(+1 like, -1 dislike) +1) + log(sommatoria 2/1+e^-(numero commenti di una persona -1)) +1)/n iterazioni
		 *
		 */

		//associo a un post tutti i commenti/like da remunerare
		HashMap<Integer, LinkedList<Comment>> pagamenti = new HashMap<>();

		//scorro tutti i post di un autore
		synchronized (queue) {

			while (this.queue.size() != 0) {
				try {
					this.queue.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			for (Post post : this.posts.get(autore)) {

				//inizializzo la lista di curatori con tutti i commenti recentemente fatti al post
				LinkedList<Comment> curators = post.commentCurators();

				//associo a un username quanti commenti per post ha fatto
				HashMap<String, Integer> numeroCommenti = new HashMap<>();

				for (Comment comment : curators) {
					//se il commento appartiene a un utente di cui abbiamo una entry, aggiorniamo il numero di commenti
					if (numeroCommenti.containsKey(comment.getUsername())) {
						//l'utente ha già commentato almeno una volta
						Integer n = numeroCommenti.get(comment.getUsername());
						n++;
						numeroCommenti.put(comment.getUsername(), n);
					} else {
						numeroCommenti.put(comment.getUsername(), 1);
					}
				}
				int numrecentUpvote = curators.size();
				//aggiorno la lista dei curators aggiungendo anche chi ha messo recentemente like
				curators = post.recentUpvote(curators);

				//calcolo il numero degli upvote
				numrecentUpvote = curators.size() - numrecentUpvote;

				//calcolo Lp
				int lp = numrecentUpvote - post.recentDownvote();

				//max (lp,0)
				if (lp < 0)
					lp = 0;

				double rewardvote = Math.log(lp + 1);
				double rewardcommment = 0;
				for (String s : numeroCommenti.keySet()) {
					//1+ E^-(Numero commenti fatti da s -1)
					rewardcommment += (2 / (1 + Math.pow(Math.E, -(numeroCommenti.get(s) - 1))));
				}

				//logaritmo(risultato della sommatoria +1)
				rewardcommment = Math.log(rewardcommment + 1);
				rewardcommment = (rewardvote + rewardcommment) / post.getIterazioni();

				curators.addFirst(new Comment(String.valueOf(rewardcommment), -1));
				post.incIterazioni();
				pagamenti.put(post.getIdPost(), curators);
			}
		}
		return pagamenti;
	}

	public void safeRemove(String user){
		synchronized (this.queue){
			if(this.queue.contains(user)){
				this.queue.remove(user);
				this.queue.notify();
			}
		}
	}
	public void registerThreadPost(String user){
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
	public void unregisterThreadPost(String user){
		synchronized (this.queue){
			this.queue.remove(user);
			this.queue.notify();
		}
	}

	public Post showPost(int id) {
		for (String username: this.posts.keySet()) {
			synchronized (this.posts.get(username)) {
				for (Post p: this.posts.get(username)) {
					if (p.getIdPost() == id) {
						return p;
					}
				}
			}
		}
		return null;
	}
}
