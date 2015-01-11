package club.hackbook.hibernate;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import club.hackbook.domain.Notification;
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
	
	public void findNotificationIdsThatDontExist()
	{
		Set<String> notifications_for_current_user = null;
		HashSet<String> notifications_that_dont_exist = new HashSet<String>();
		Notification current_notification = null;
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null; 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		try
		{
			 tx = session.beginTransaction();
			 String hql = "FROM User U WHERE U.registered=true";
			 Query query = session.createQuery(hql);
			 @SuppressWarnings("unchecked")
			 List<User> useritems = query.list();
			 
			 
			 if (useritems != null && useritems.size() > 0) {
				 for (User u : useritems) {
					 notifications_that_dont_exist = new HashSet<String>();
					 System.out.println("since=" + u.getSinceHumanReadable() + " since=" + u.getSince() + " seen=" + u.getSeenHumanReadable() + "  seen=" + u.getSeen() + " ext_version=" + u.getExtVersion() + " id=" + u.getId());
					 
					 for(String s: u.getNotificationIds())
					 {
						 System.out.print("\t" + s);
						 current_notification = (Notification)session.get(Notification.class, s);
						 if(current_notification == null)
						 {
							 System.out.println(" DOES NOT EXIST!");
							 notifications_that_dont_exist.add(s);
						 }
						 else
							 System.out.println(" EXISTS!");
					 }
					 
					 notifications_for_current_user = u.getNotificationIds();
					 for(String s: notifications_that_dont_exist)
					 {
						 System.out.println(" Removing " + s);
						 notifications_for_current_user.remove(s);
						 u.setNotificationIds(notifications_for_current_user);
						 session.save(u);
					 }
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
		
		for(String s: notifications_that_dont_exist)
		{
			System.out.println(s);
		}
		
		System.out.println("exiting findNotificationIdsThatDontExist.");
	}
	
	public void findFollowingFollowerInconsistencies()
	{
		User current_followee = null;
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null; 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		try
		{
			 tx = session.beginTransaction();
			 String hql = "FROM User U WHERE U.registered=true";
			 Query query = session.createQuery(hql);
			 @SuppressWarnings("unchecked")
			 List<User> useritems = query.list();
			 
			 
			 if (useritems != null && useritems.size() > 0) {
				 for (User u : useritems) {
					 System.out.println("since=" + u.getSinceHumanReadable() + " since=" + u.getSince() + " seen=" + u.getSeenHumanReadable() + "  seen=" + u.getSeen() + " ext_version=" + u.getExtVersion() + " id=" + u.getId());
					 
					 for(String s: u.getFollowing())
					 {
						 System.out.print("\t" + s);
						 current_followee = (User)session.get(User.class, s);
						 if(current_followee == null)
						 {
							 System.out.println(" DOES NOT EXIST!");
						 }
						 else
						 {
							 System.out.println(" EXISTS! Does u.getId()=" + u.getId() + " appear in this user's follwers list?");
							 if(current_followee.getFollowers().contains(u.getId()))
							 {
								 //System.out.println(" YES. All good.");
							 }
							 else
							 {
								 System.out.println("\t\t\tNO. That's bad.");
							 }
						 }
					 }
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
		
		System.out.println("exiting findFollowingFollowerInconsistencies.");
	}
	
	public void findFollowerFollowingInconsistencies()
	{
		User current_follower = null;
		Set<String> current_set_of_followers = null;
		Session session = HibernateUtil.getSessionFactory().openSession();
		Transaction tx = null; 
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		try
		{
			 tx = session.beginTransaction();
			 String hql = "FROM User U";
			 Query query = session.createQuery(hql);
			 @SuppressWarnings("unchecked")
			 List<User> useritems = query.list();
			 
			 int x = 0;
			 if (useritems != null && useritems.size() > 0) {
				 for (User u : useritems) {
					 if(x < 10)
					 {
						 System.out.print(u.getId());
						 x++;
					 }
					 else
					 {
						 System.out.println(u.getId());
						 x = 0;
					 }
					// System.out.println("since=" + u.getSinceHumanReadable() + " since=" + u.getSince() + " seen=" + u.getSeenHumanReadable() + "  seen=" + u.getSeen() + " ext_version=" + u.getExtVersion() + " id=" + u.getId());
					 current_set_of_followers = u.getFollowers();
					 if(current_set_of_followers == null)
					 {	 
						 System.out.print(" NULL   ");
					 }
					 else if(current_set_of_followers.isEmpty())
					 {
						 System.out.print(" EMPTY   ");
					 }
					 else
					 { 
						 System.out.println();
						 for(String s: current_set_of_followers)
						 {
							 System.out.print("\t" + s);
							 current_follower = (User)session.get(User.class, s);
							 if(current_follower == null)
							 {
								 System.out.println(" DOES NOT EXIST!");
							 }
							 else
							 {
								 System.out.println(" EXISTS! Does u.getId()=" + u.getId() + " appear in this user's following list?");
								 if(current_follower.getFollowing().contains(u.getId()))
								 {
									 System.out.println("\t\t\tYES. All good.");
								 }
								 else
								 {
									 System.out.println("\t\t\tNO. That's bad.");
								 }
							 }
						 }
					 }
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
		
		System.out.println("exiting findFollowerFollowingInconsistencies.");
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
			 String hql = "FROM User U WHERE U.registered=true ORDER BY U.seen";
			 Query query = session.createQuery(hql);
			 @SuppressWarnings("unchecked")
			 List<User> useritems = query.list();
			 
			 if (useritems != null && useritems.size() > 0) {
				 for (User u : useritems) {
					 System.out.println("since=" + u.getSinceHumanReadable() + " since=" + u.getSince() + " seen=" + u.getSeenHumanReadable() + "  seen=" + u.getSeen() + " ext_version=" + u.getExtVersion() + " hec=" + u.getHideEmbeddedCounts() + " id=" + u.getId());
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
	
	public void printUsersBeingFollowedByAtLeastOnePerson()
	{
		HashSet<String> pairs_set = new HashSet<String>();
		Connection conn = null;
		ResultSet rs = null;
		Statement stmt = null;
		try {
		    conn =
		       DriverManager.getConnection("jdbc:mysql://secure.hackbook.club/hackbook?" +
		                                   "user=&password=");

		    stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			String query = "SELECT * FROM User_followers";
			rs = stmt.executeQuery(query);
			while(rs.next())
			{
				if(pairs_set.contains(rs.getString(1) + "ASDHFH2342aASDae32gas3w3asvx" + rs.getString(2)))
				{
					// delete this row
					System.out.println("\t\t\t\t\t\tDeleting " + rs.getString(1) + "-" + rs.getString(2));
					rs.deleteRow();
				}
				else
				{
					// keep this row, add 
					System.out.println("Keeping " + rs.getString(1) + "-" + rs.getString(2));
					pairs_set.add(rs.getString(1) + "ASDHFH2342aASDae32gas3w3asvx" + rs.getString(2));
				}
			} 
			rs.close();
			stmt.close();
			conn.close();

		} catch (SQLException ex) {
		    // handle any errors
		    System.out.println("SQLException: " + ex.getMessage());
		    System.out.println("SQLState: " + ex.getSQLState());
		    System.out.println("VendorError: " + ex.getErrorCode());
		}
		finally
		{
			try
			{
				if (rs  != null)
					rs.close();
				if (stmt  != null)
					stmt.close();
				if (conn  != null)
					conn.close();
			}
			catch(SQLException sqle)
			{
				System.out.println("There was a problem closing the resultset, statement and/or connection to the database.");
			}
		}   	
						
		for(String s: pairs_set)
		{
			System.out.println(s);
		}
		
		
		System.out.println("exiting printUsersBeingFollowedByAtLeastOnePerson.");
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
