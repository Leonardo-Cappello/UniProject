package dbPost;

public class Comment {
	private String testo;
	private int id;
	private String username;
	private boolean pagato;

	Comment(){
		super();
	}

	Comment(String username, int id, String testo){
		this.username = username;
		this.testo = testo;
		this.id = id;
		this.pagato = false;
	}
	Comment(String username, int id){
		this.username = username;
		this.testo = null;
		this.id = id;
		this.pagato = false;
	}

	

	public String getTesto() {
		return this.testo;
	}
	public int getId() {
		return this.id;
	}
	public String getUsername() {
		return  this.username;
	}
	public  boolean getPagato(){
		return this.pagato;
	}
	public void setPagato(boolean pagato){
		this.pagato = pagato;
	}
}
