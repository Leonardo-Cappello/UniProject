package server;

import dbPost.DbPosts;


import java.util.LinkedList;

//garbage collector dei post eliminati
public class PostDeleter implements Runnable{
    final LinkedList<Integer> deletedPost;
    DbPosts dbPosts;
    Boolean stop;

    PostDeleter(DbPosts posts){
        deletedPost = new LinkedList<>();
        stop = false;
        this.dbPosts = posts;
    }


    public void addPost(int id){
        synchronized (this.deletedPost) {
            deletedPost.add(id);
            deletedPost.notify();
        }
    }

    public void stopCollector(){
        this.stop = true;
    }
    public void run(){
       while(!this.stop) {
           synchronized (this.deletedPost) {
               while (this.deletedPost.isEmpty()) {
                   try {
                       this.deletedPost.wait();
                   } catch (InterruptedException e) {
                       e.printStackTrace();
                   }
               }
           }

           //cancello dal database tutti i post che mi sono stati inviati
           cancellaPost();
       }
    }
    public void cancellaPost(){
        int id;
        while(!this.deletedPost.isEmpty()){
            //applico una politica FIFO di svuotamento della lista
            id = deletedPost.removeFirst();
            this.dbPosts.registerThreadPost("PostDeleter");
            this.dbPosts.deletePost(id);
            this.dbPosts.unregisterThreadPost("PostDeleter");
        }
    }
}
