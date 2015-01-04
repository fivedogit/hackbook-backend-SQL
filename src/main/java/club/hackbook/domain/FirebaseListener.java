package club.hackbook.domain;

import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.hibernate.Session;
import org.hibernate.Transaction;

import club.hackbook.util.HibernateUtil;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
 
public class FirebaseListener implements ServletContextListener {
 
    private static ExecutorService executor;
    private Firebase myFirebaseRef = null;
    private String myId = ""; 
    private Globalvar firebase_owner_id_gvi = null;
    private Globalvar firebase_last_msfe_gvi = null;
    private ValueEventListener vel = null;
    
    public void contextInitialized(ServletContextEvent cs) {
    	System.out.println("contextInitialized()");
    	myId = UUID.randomUUID().toString().replaceAll("-", "");
    	myFirebaseRef = new Firebase("https://hacker-news.firebaseio.com/v0/updates");
    	
    	createExecutor();
    	cs.getServletContext().log("Executor service started !");
    	vel = myFirebaseRef.addValueEventListener(new ValueEventListener() 
    	{
 			  public void onDataChange(DataSnapshot snapshot) 
 			  {
 				 boolean ihavethepower = false;
 				 System.out.println("Firebase data has changed.");
 				 Session session = HibernateUtil.getSessionFactory().openSession();
 				 int indentval = (new Random()).nextInt(10);
 				 Global.printThreadHeader(indentval, session.hashCode(), "FirebaseListener", "opening");
 				 Transaction tx = null; 
 				 try
 				 {
 					 tx = session.beginTransaction();
 					 // If I own the firebase lock OR the firebase lock is more than 2.5 minutes old, take it over.
 	 				 long now = System.currentTimeMillis();
 	 				 
 	 				 firebase_owner_id_gvi = (Globalvar)session.get(Globalvar.class, "firebase_owner_id");
 	 				 if(firebase_owner_id_gvi == null)
 	 				 {
 	 					firebase_owner_id_gvi = new Globalvar("firebase_owner_id", "blahblahblah", 0L);
 	 					session.save(firebase_owner_id_gvi);
 	 				 }
 	 				 
 	 				 firebase_last_msfe_gvi = (Globalvar)session.get(Globalvar.class, "firebase_last_msfe");
 	 				 if(firebase_last_msfe_gvi == null)
 	 				 {
 	 					firebase_last_msfe_gvi = new Globalvar("firebase_last_msfe", "some hr date", 0L);
 	 					session.save(firebase_last_msfe_gvi);
 	 				 }
 	 				 
 					 if(!firebase_owner_id_gvi.getStringValue().equals(myId))
 					 {
 						 if(firebase_last_msfe_gvi.getNumberValue() > System.currentTimeMillis()-150000) 
 						 {
 							 System.out.println("*** I do NOT have the power (SQL)! *** myId=" + myId + " owner=" + firebase_owner_id_gvi.getStringValue() + " firebase_last_msfe is " + (System.currentTimeMillis()-firebase_last_msfe_gvi.getNumberValue()) + " old.");
 							 ihavethepower = false;
 						 }
 						 else
 						 {
 							 System.out.println("*** I DO have the power (SQL)! (takeover) *** myId=" + myId + " owner=" + firebase_owner_id_gvi.getStringValue() + " firebase_last_msfe is " + (System.currentTimeMillis()-firebase_last_msfe_gvi.getNumberValue()) + " old.");
 							 firebase_owner_id_gvi.setStringValue(myId);
 							 firebase_last_msfe_gvi.setNumberValue(now);
 							 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
 							 sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
 							 firebase_last_msfe_gvi.setStringValue(sdf.format(now));
 							 session.save(firebase_owner_id_gvi);
 							 session.save(firebase_last_msfe_gvi);
 							 ihavethepower = true;
 						 }
 					 }
 					 else // if(firebase_owner_id_gvi.getStringValue().equals(myId))
 					 {
 						 System.out.println("*** I DO have the power! (SQL) (keep) *** myId=" + myId + " owner=" + firebase_owner_id_gvi.getStringValue() + " firebase_last_msfe is " + (System.currentTimeMillis()-firebase_last_msfe_gvi.getNumberValue()) + " old.");
 						 firebase_owner_id_gvi.setStringValue(myId);
 						 firebase_last_msfe_gvi.setNumberValue(System.currentTimeMillis());
 						 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
 						 sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
 						 firebase_last_msfe_gvi.setStringValue(sdf.format(now));
 						 session.save(firebase_owner_id_gvi);
 						 session.save(firebase_last_msfe_gvi);
 						 ihavethepower = true;
 					 }
 					 tx.commit();
 				 }
 				 catch (Exception e) {
 				     if (tx!=null) tx.rollback();
 				     e.printStackTrace();
 				 }
 				 finally {
 					 Global.printThreadHeader(indentval, session.hashCode(), "FirebaseListener", "opening");
 					 session.close();
 				 }
 				 
 				 if(ihavethepower)
 				 { 
 					 System.out.println("Inside if(ihavethepower==true) myId=" + myId);
 					 FirebaseChangeProcessor fcp = new FirebaseChangeProcessor(snapshot);
 					 fcp.start();
 				 }
 			  }

 			  public void onCancelled(FirebaseError error) {
 				  System.out.println("onCancelled called");
 			  }
 			});
    }
    public void contextDestroyed(ServletContextEvent cs) {
    	System.out.println("contextDestroyed()");
    	myFirebaseRef.removeEventListener(vel);
    	executor.shutdown();
    	cs.getServletContext().log("Executor service shutdown !");
    }
 
    public static synchronized void submitTask(Runnable runnable) {
    	System.out.println("submitTask(runnable)");
        if (executor == null) {
            createExecutor();
        }
        executor.submit(runnable);
    }
 
    public static synchronized Future<String> submitTask(Callable callable) {
    	System.out.println("submitTask(callable)");
        if (executor == null) {
            createExecutor();
        }
        return executor.submit(callable);
    }
 
    static void  createExecutor() {
    	System.out.println("createExecutor()");
        executor = new ThreadPoolExecutor(
                1,
                3,
                100L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
 
    }
    
}
