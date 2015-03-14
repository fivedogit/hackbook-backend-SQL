package club.hackbook.domain;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.TimeZone;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import club.hackbook.util.HibernateUtil;

// The sole purpose of PeriodicCalculator is to figure out which users have the highest follower counts. 
// It's expensive in the sense that it has to query the whole user table (not just registered users, mind you) 
// and calculate the follower count for each one.

public class PeriodicCalculator extends java.lang.Thread {

	public void initialize()
	{
	}
	
	public PeriodicCalculator()
	{
		this.initialize();
	}
	
	long nextLong(Random rng, long n) {
		   // error checking and 2^x checking removed for simplicity.
		   long bits, val;
		   do {
		      bits = (rng.nextLong() << 1) >>> 1;
		      val = bits % n;
		   } while (bits-val+(n-1) < 0L);
		   return val;
		}
	
	public void run()
	{
		long entry = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		System.out.println("=== " + super.getId() +  " Fired a PeriodicCalculator thread at " + sdf.format(entry));
		
		Session session = HibernateUtil.getSessionFactory().openSession();
		int indentval = (new Random()).nextInt(10);
		Global.printThreadHeader(indentval, session.hashCode(), "PeriodicCalculator", "opening");
		Transaction tx = null; 
		try{
			tx = session.beginTransaction();
			Globalvar periodic_last_msfe_gvi = (Globalvar)session.get(Globalvar.class, "periodic_last_msfe");
			if(periodic_last_msfe_gvi == null)
			{
				periodic_last_msfe_gvi = new Globalvar("periodic_last_msfe", "some hr date", 0L);
				session.save(periodic_last_msfe_gvi);
			}
			long periodic_last_msfe_long = periodic_last_msfe_gvi.getNumberValue();
			
			if((entry - periodic_last_msfe_long) > 1200000) // 20 minutes
			{
				// first set last timestamp to prevent others from firing
				periodic_last_msfe_gvi.setStringValue(sdf.format(entry));
				periodic_last_msfe_gvi.setNumberValue(entry);
				session.update(periodic_last_msfe_gvi);
				
				int limit = 42;
				// not sure which of these is "proper". The second is from the hibernate docs, so that's what we'll go with.
				//long tablesize = ((Number) session.createCriteria("Book").setProjection(Projections.rowCount()).uniqueResult()).longValue();
				//long tablesize = ((Number) session.createQuery("select count(*) from ....").iterate().next() ).longValue();
				
				String hql = "FROM User";
				Query query = session.createQuery(hql);
				@SuppressWarnings("unchecked")
				List<User> userScanResult = query.list();
				
				//DynamoDBScanExpression userScanExpression = new DynamoDBScanExpression();
				//List<User> userScanResult = mapper.scan(User.class, userScanExpression);
		
				Comparator<User> comparator = new FollowerCountComparator();
				PriorityQueue<User> queue = new PriorityQueue<User>(limit, comparator);
				//HashSet<User> random_users = new HashSet<User>();
				Long x = 0L;
				for (User useritem : userScanResult) { 
					
					// add the user to the priorityqueue
					queue.add(useritem);
					// if the queue is too big, remove one
					if(queue.size() > limit)
						queue.remove();
					x++;
				}
			
				System.out.println("Most followers: (size=" + queue.size() + ")");
				User currentuseritem = null;
				JSONArray most_followed_users_ja = new JSONArray();
				JSONObject temp_jo = new JSONObject();
				try
				{
					// loop through the queue of most followed users, create a jo for each and add to the master ja
					while (queue.size() != 0)
					{
						currentuseritem = queue.remove();
						if(currentuseritem.getFollowers() != null)
							System.out.println("id=" + currentuseritem.getId() + " follower_count=" + currentuseritem.getFollowers().size());
						else
							System.out.println("id=" + currentuseritem.getId() + " follower_count=" + 0);
						temp_jo = new JSONObject();
						temp_jo.put("id", currentuseritem.getId());
						if(currentuseritem.getFollowers() == null)
							temp_jo.put("num_followers", 0);
						else
							temp_jo.put("num_followers", currentuseritem.getFollowers().size());
						most_followed_users_ja.put(temp_jo);
					}
				}
				catch(JSONException jsone)
				{
					System.err.println("JSONException trying to set global vars for random users and most followed users");
				}
			
				System.out.println(most_followed_users_ja);
				//System.out.println(random_users_ja);
				
				// now save to the database
				Globalvar most_followed_users_gvi = (Globalvar)session.get(Globalvar.class, "most_followed_users");
				if(most_followed_users_gvi == null)
					most_followed_users_gvi = new Globalvar("most_followed_users", "a string", 0L);
				most_followed_users_gvi.setStringValue(most_followed_users_ja.toString());
				session.save(most_followed_users_gvi);
			}
			tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		}
		finally {
			Global.printThreadHeader(indentval, session.hashCode(), "PeriodicCalculator", "closing");
			session.close();
		}
		
		long exit = System.currentTimeMillis();
		System.out.println("=== " + super.getId() +  " PeriodicCalculator Done. elapsed=" + ((exit - entry)/1000) + "s at " + sdf.format(exit));
		return;
	}
	
	public static void main(String [] args)
	{
	}
}