package club.hackbook.hibernate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.HashSet;
import java.util.TimeZone;

import club.hackbook.domain.User;
import club.hackbook.util.HibernateUtil;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

public class MySQLDBTools {

	private HashSet<User> registered_users;
	
	public MySQLDBTools()
	{
	}

	 private User createUserFromHNAPIResult(String result)
	 {
	    	if(result == null || result.isEmpty())
	    		return null;
	    	try 
	    	{ 
	    		User useritem = new User();
	    		JSONObject profile_jo = new JSONObject(result);
	    		useritem.setId(profile_jo.getString("id"));
	    		useritem.setHNKarma(profile_jo.getLong("karma"));
	    		useritem.setHNSince(profile_jo.getLong("created"));
	    		useritem.setId(profile_jo.getString("id"));
	    		useritem.setRegistered(false);
	    		useritem.setURLCheckingMode("stealth");
	    		useritem.setKarmaPoolTTLMins(15L);
	    		useritem.setHideEmbeddedCounts(true);
	    		if(profile_jo.has("about"))
	    			useritem.setHNAbout(profile_jo.getString("about"));
	    		else
	    			useritem.setHNAbout("");
	    		return useritem;
	    	} catch (JSONException e) {
	    		e.printStackTrace();
	    		return null;
	    	}
	 } 
	
		
	public int getNumRegisteredUsers()
	{
		if(registered_users == null)
			registered_users = getRegisteredUsers();
		return registered_users.size();
	}
	
	public void printRegisteredUsers()
	{
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null; 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		try
		{
			 tx = session.beginTransaction();
			 String hql = "FROM User U WHERE U.registered=true ORDER BY U.ext_version";
			 Query query = session.createQuery(hql);
			 @SuppressWarnings("unchecked")
			 List<User> useritems = query.list();
			 
			 if (useritems != null && useritems.size() > 0) {
				 for (User u : useritems) {
					 if(u.getExtVersion() == null || u.getExtVersion().equals("null"))
					 { 
						 u.setExtVersion("0.231");
						 session.save(u);
					 }
					 System.out.println("since=" + u.getSinceHumanReadable() + " since=" + u.getSince() + " seen=" + u.getSeenHumanReadable() + "  seen=" + u.getSeen() + " ext_version=" + u.getExtVersion() + " id=" + u.getId());
				 }
			 } else {
				 return;
			 }
			 tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		}
		finally {
			session.close();
		}
		
		System.out.println("exiting printRegisteredUsers.");
	}
	
	public HashSet<User> getRegisteredUsers() { 
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null; 
		HashSet<User> returnset = null;
		try
		{
			 tx = session.beginTransaction();
			 String hql = "FROM User U WHERE U.registered=true";
			 Query query = session.createQuery(hql);
			 @SuppressWarnings("unchecked")
			 List<User> useritems = query.list();
			 
			 if (useritems != null && useritems.size() > 0) {
				 returnset = new HashSet<User>();
				 for (User useritem : useritems) {
					 returnset.add(useritem);
				 }
			 } else {
				 returnset = null;
			 }
			 tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		}
		finally {
			session.close();
		}		
		return returnset;
	}
	
	public void addUser(String screenname)
	{
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null; 
		HashSet<User> returnset = new HashSet<User>();
		User useritem = null;
		try
		{
			 tx = session.beginTransaction();
			 useritem = (User)session.get(User.class, screenname);
			 if(useritem == null)
			 {
				 System.out.println("Doesn't exist.");
				 try
				 {
					 String result = Jsoup
							 .connect("https://hacker-news.firebaseio.com/v0/user/" + screenname  + ".json")
							 .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
							 .ignoreContentType(true).execute().body();
					 System.out.println("Creating " + screenname + ". ");
					 useritem = createUserFromHNAPIResult(result);
					 if(useritem != null)
					 {
						 session.save(useritem);
					 }
				 }
				 catch(IOException ioe)
				 {
					 ioe.printStackTrace();
				 }
			 }
			 else
				 System.out.println("Exists");
			 tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		}
		finally {
			session.close();
		}
	}
	
	public void deleteUser(String screenname)
	{
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null; 
		User useritem = null;
		try
		{
			 tx = session.beginTransaction();
			 useritem = (User)session.get(User.class, screenname);
			 session.delete(useritem);
			 tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		}
		finally {
			session.close();
		}
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		MySQLDBTools dbt = new MySQLDBTools();
		dbt.printRegisteredUsers();
		System.out.println("Exiting main.");
	}

}
