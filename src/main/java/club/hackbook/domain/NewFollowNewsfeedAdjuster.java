package club.hackbook.domain;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.HashSet;
import java.util.TreeSet;

import org.hibernate.Session;
import org.hibernate.Transaction;

import club.hackbook.util.HibernateUtil;

public class NewFollowNewsfeedAdjuster extends java.lang.Thread {

	private String screenname;
	private String target_screenname;
	
	public void initialize()
	{
	}
	
	public NewFollowNewsfeedAdjuster(String inc_screenname, String inc_target_screenname)
	{
		this.initialize();
		screenname = inc_screenname;
		target_screenname = inc_target_screenname;
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
		System.out.println("=== " + super.getId() +  " Fired a NewFollowNewsFeedAdjuster thread.");
		long entry = System.currentTimeMillis();
		Session session = HibernateUtil.getSessionFactory().openSession();
		int indentval = (new Random()).nextInt(10);
		Global.printThreadHeader(indentval, session.hashCode(), "NewFollowNewsFeedAdjuster", "opening");
		Transaction tx = null; 
		try{
			 tx = session.beginTransaction();

			 User useritem = (User)session.get(User.class, screenname);
			 User target_useritem = (User)session.get(User.class, target_screenname);
			 
			 HashSet<Item> hnitems = target_useritem.getHNItemsByd(2880L, session); // 2 days
			 Set<String> newsfeed_ids = useritem.getNewsfeedIds();
			 
			 System.out.println("Found " + hnitems.size() + " items by " + target_useritem.getId() + " in the past 2 days.");
			 Iterator<Item> hnitem_it = hnitems.iterator();
			 Item current = null;
			 Notification ni = null;
			 long now = 0L;
			 String now_str = "";
			 Random generator = new Random(); 
			 int r = 0; // this will produce numbers that can be represented by 3 base62 digits
			 String randompart_str = "";
			 String notification_id = ""; 
			 if(newsfeed_ids == null)
				 newsfeed_ids = new HashSet<String>();
			 while(hnitem_it.hasNext())
			 {
				 current = hnitem_it.next();
				 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
				 sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
				 Calendar cal = Calendar.getInstance();
				 cal.add(Calendar.MINUTE, -2880);
				 System.out.println("Found a " + current.getType() + " by " + current.getBy() + " at " + current.getTime() * 1000 + " hr=" + sdf.format(current.getTime() * 1000) + " cutoff=" + sdf.format(cal.getTimeInMillis()));
				 if(current.getType().equals("comment") || current.getType().equals("story")) // If this is a comment or story, then it fits. ignore everything else
				 { 
					 r = generator.nextInt(238327); // this will produce numbers that can be represented by 3 base62 digits
					 now = System.currentTimeMillis();
					 now_str = Global.fromDecimalToBase62(7,(current.getTime()*1000));
					 randompart_str = Global.fromDecimalToBase62(3,r);
					 ni = new Notification();
					 if(current.getType().equals("comment")) // if it's a comment, step back and find root
					 {
						 notification_id = now_str + randompart_str + "8";
						 ni.setType("8");
						 HashMap<String,Long> roots = Global.findRootStoryAndCommentLocal(current.getId(), session);
						 if(roots == null)
						 {
							 System.out.println("Couldn't find root, so skipping to next.");
							 continue; // couldn't find roots, so skipping to next
						 }
						 else
						 {
							 ni.setHNRootStoryOrPollId(roots.get("story_or_poll"));
							 ni.setHNRootCommentId(roots.get("comment")); // for #6, this should always be the same as hn_target_id
						 }
					 }
					 else if(current.getType().equals("story")) // // if it's a story, root is the same as the item id
					 {
						 notification_id = now_str + randompart_str + "7";
						 ni.setType("7");
						 ni.setHNRootStoryOrPollId(current.getId());
						 ni.setHNRootCommentId(0L);
					 }
					 ni.setId(notification_id);
					 ni.setActionMSFE(current.getTime()*1000);
					 ni.setMSFE(now);
					 ni.setUserId(useritem.getId());
					 ni.setHNTargetId(current.getId());
					 ni.setTriggerer(target_useritem.getId());
					 session.save(ni);				
					 newsfeed_ids.add(notification_id);
				 }
			 }
			 // if empty, set to null. If more than max, get most recent Global.NEWSFEED_SIZE_LIMIT items
			 if(newsfeed_ids.isEmpty())
				 newsfeed_ids = null;
			 else if(newsfeed_ids.size() > Global.NEWSFEED_SIZE_LIMIT)
			 {
				 TreeSet<String> temp_ids = new TreeSet<String>();
				 temp_ids.addAll(newsfeed_ids);
				 newsfeed_ids = new TreeSet<String>(); // empty out the existing ids;
				 Iterator<String> it = temp_ids.descendingIterator();
				 int x = 0;
				 String currentstr = "";
				 while(x < Global.NEWSFEED_SIZE_LIMIT)
				 {
					 currentstr = it.next();
					 newsfeed_ids.add(currentstr);
					 x++;
				 }
				 if(useritem.getNewsfeedCount() > Global.NEWSFEED_SIZE_LIMIT) // count can't be more than the limit
					 useritem.setNewsfeedCount(Global.NEWSFEED_SIZE_LIMIT); // so set it to the limit
			 }
			 useritem.setNewsfeedIds(newsfeed_ids);
			 session.save(useritem);
			 tx.commit();
		}
		catch (Exception e) {
			if (tx!=null) tx.rollback();
			e.printStackTrace();
		}
		finally {
			Global.printThreadHeader(indentval, session.hashCode(), "NewFollowNewsFeedAdjuster", "closing");
			session.close();
		}
		long exit = System.currentTimeMillis();
		System.out.println("=== " + super.getId() +  " NewFollowNewsFeedAdjuster Done. elapsed=" + ((exit - entry)/1000) + "s");
		return;
	}
	
	public static void main(String [] args)
	{
	}
}