package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class RandomOrg implements Runnable{
    private String url = "https://www.random.org/decimal-fractions/?format=plain&num=1&col=1&dec=8";
    private long time;
    private double current_value;
    private boolean stop;
    RandomOrg(long time){
        this.time = time;
        this.stop = false;
    }
    public void stopGeneration(){
        this.stop = true;
    }

    public String convertValue(Double value){
       double ris = value * this.current_value;
       return Double.toString(ris);
    }

    public void run() {
        do {
            BufferedReader in = null;
            String ris = null;
            try {
                // Open the URL for reading
                URL u = new URL(this.url);
                HttpURLConnection con = (HttpURLConnection) u.openConnection();
                con.setRequestMethod("GET");
                con.setRequestProperty("content-type", "text/plain");
                con.setRequestProperty("User-Agent", "l.cappello3@studenti.unipi.it");
                int responseCode = con.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    ris = in.readLine();
                    this.current_value = Double.parseDouble(ris);
                }
            } catch (MalformedURLException ex) {
                System.err.println(this.url + " is not a parseable URL");
            } catch (IOException ex) {
                System.out.println(ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }


            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }while(!stop);
    }
}
