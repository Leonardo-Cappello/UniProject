package dbPost;

import java.util.HashMap;
import java.util.LinkedList;

public class Post {

	private String titolo;
	private String contenuto;
	private String autore;
	// divido in due. più facile sapere quanti sono in totale i like e i dislike di un post
	private final LinkedList<String> votoPositivo = new LinkedList<>();
	private final LinkedList<String> votoNegativo = new LinkedList<>();
	private int iterazioni = 1;
	private int idCommenti = 1;
	private int rewinId;
	private final HashMap<String,LinkedList<Comment>> comments = new HashMap<>();
	private int idPost;
	Post(){
		super();
	}
	Post(String titolo, String contenuto, String autore,int idPost){
		this.titolo = titolo;
		this.contenuto = contenuto;
		this.autore = autore;
		this.idPost = idPost;
		this.rewinId = -1;
	}
	Post(String titolo, String contenuto, String autore,int idPost,int iterazioni, int rewinId){
		this.titolo = titolo;
		this.contenuto = contenuto;
		this.autore = autore;
		this.idPost = idPost;
		this.iterazioni = iterazioni;
		this.rewinId = rewinId;
	}
	public int getIterazioni(){
		return this.iterazioni;
	}

	public void incIterazioni(){
		this.iterazioni++;
	}
	public String getTitolo() {
		return this.titolo;
	}

	public String getContenuto() {
		return this.contenuto;
	}

	public String getAutore() {
		return this.autore;
	}

	public int getIdPost() {
		return this.idPost;
	}

	public int getRewinId() {return this.rewinId;}

	public HashMap<String,LinkedList<Comment>> getComments() {
		return this.comments;
	}

	public LinkedList<String> getLikes() {
		return this.votoPositivo;
	}

	public LinkedList<String> getDislikes() {
		return this.votoNegativo;
	}

	public int getIdCommenti(){ return this.idCommenti;}
	/*
	 * Parametri: username di chi commenta e commento relativo a this post
	 */
	public void addComment(String username, String commento) {
		synchronized (this) {
			LinkedList<Comment> temp;
			//se l'utente ha già commentato una volta recupero la lista dei commenti altrimenti ne creo una nuova
			if (this.comments.containsKey(username)) {
				temp = this.comments.get(username);
			} else {
				temp = new LinkedList<>();
			}

			//aggiungo il commento alla lista
			temp.add(new Comment(username, this.idCommenti, commento));
			//aggiorno la map
			this.comments.put(username, temp);
			//incremento l'id dei commenti
			this.idCommenti++;
		}
	}

	public boolean likePost(String username) {
		synchronized (this) {
			if (this.votoPositivo.contains(username) || this.votoPositivo.contains(username + " ")) {
				return false;
			}
			this.votoPositivo.add(username);
		}
		return true;
	}

	public boolean dislikePost(String username) {
		synchronized (this) {
			if (this.votoNegativo.contains(username) || this.votoPositivo.contains(username + " ")) {
				return false;
			}
			this.votoNegativo.add(username);
		}
		return true;
	}

	@Override
	public String toString(){
		StringBuilder temp = new StringBuilder();

		synchronized (this) {
			temp.append("\t Titolo: " + this.titolo + "\n");
			temp.append("\t Contenuto post: " + this.contenuto + "\n");
			temp.append("\t Likes: " + this.votoPositivo.size() + "\n");
			temp.append("\t Dislikes: " + this.votoNegativo.size() + "\n");

			if (this.comments.keySet().isEmpty()) {
				temp.append(" Non ci sono ancora commenti per questo post \n");
			} else {
				temp.append("\n Commenti : ");
				for (String user : this.comments.keySet()) {
					for (Comment comment : this.comments.get(user)) {
						temp.append("\n\t Username: " + comment.getUsername());
						temp.append("\t IdCommento: " + comment.getId() + "\n");
						temp.append("\t Testo commento: " + comment.getTesto() + "\n");
					}
				}
			}
		}
		return temp.toString();
	}

	public LinkedList<Comment> recentUpvote(LinkedList<Comment> vote){
		synchronized (this) {
			for (String s : this.votoPositivo) {
				if (!s.endsWith(" ")) {
					vote.add(new Comment(s, -1));
					this.votoPositivo.remove(s);
					this.votoPositivo.add(s + " ");
				}
			}
		}
		return vote;
	}

	public int recentDownvote(){
		int count = 0;
		synchronized (this) {
			for (String s : this.votoNegativo) {
				if (!s.endsWith(" ")) {
					count++;
					this.votoNegativo.remove(s);
					this.votoNegativo.add(s + " ");
				}
			}
		}
		return count;
	}

	//prendo tutti i commenti recenti e li restituisco
	public LinkedList<Comment> commentCurators() {
		LinkedList<Comment> vote = new LinkedList<>();
		synchronized (this) {
			for (String s : this.comments.keySet()) {
				for (Comment c : this.comments.get(s)) {

					// se il commento non è stato pagato
					if (!c.getPagato()) {
						vote.add(c);
						c.setPagato(true);
					}
				}
			}
		}
		return vote;
	}

}
