package dbUtenti;

import java.util.Calendar;
import java.util.Date;

public class Transazione {
	private int idPost;
	private int idCommento;			// vale -1 se è un like, 0 se è l'autore del post
	private double amountWnc; 		// totale wincoin ricevuti
	private Date data;
	Transazione(){
		super();
	}
	Transazione(int idPost, int idCommento, double wnc){
		this.idPost = idPost;
		this.idCommento = idCommento;
		this.amountWnc = wnc;
		this.data = Calendar.getInstance().getTime();
	}

	public double getAmountWnc() {
		return amountWnc;
	}

	public int getIdCommento() {
		return idCommento;
	}

	public int getIdPost() {
		return idPost;
	}

	public Date getData() { return  this.data;}
}
