package club.hackbook.domain;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.UUID;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;

import club.hackbook.util.HibernateUtil;

import com.firebase.client.DataSnapshot;

public class FirebaseChangeProcessor extends java.lang.Thread {

	private DataSnapshot snapshot;
	 
	public void initialize()
	{
		
	}
	
	public FirebaseChangeProcessor(DataSnapshot inc_snapshot)
	{
		this.initialize();
		snapshot = inc_snapshot;
	}
		
	@SuppressWarnings("unchecked")
	public void run()
	{
		long entry = System.currentTimeMillis();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
		System.out.println("=== " + super.getId() +  " Fired a FirebaseChangeProcessor thread at " + sdf.format(entry));
		
		// always start this. Let PeriodicCalculator figure out if it's been > wait time
		PeriodicCalculator pc = new PeriodicCalculator();
		pc.start();
		 
		System.out.println("2Data changed " + snapshot.getChildrenCount());
		ArrayList<String> str_value_al = null;
		ArrayList<Integer> int_value_al = null;
		JSONObject new_jo;
		  
			  
		HashMap<String, Long> score_changes = new HashMap<String,Long>(); 
		//HashMap<String, Integer> karma_changes = new HashMap<String,Integer>(); 
		  
		for (DataSnapshot child : snapshot.getChildren())
		{
			  /***
			   *     _____ _____ ________  ___ _____ 
			   *    |_   _|_   _|  ___|  \/  |/  ___|
			   *      | |   | | | |__ | .  . |\ `--. 
			   *      | |   | | |  __|| |\/| | `--. \
			   *     _| |_  | | | |___| |  | |/\__/ /
			   *     \___/  \_/ \____/\_|  |_/\____/ 
			   *                                     
			   */
			  if(child.getKey().equals("items"))
			  {
				  int_value_al = child.getValue(ArrayList.class);
				  System.out.println(child.getKey() + " " + int_value_al.toString());
				  Item hnii = null;
				  String result = null;
				  Iterator<Integer> it = int_value_al.iterator();
				  Integer item = null;
				  while(it.hasNext())
				  {
					  item = it.next();
					  if(item != null) // strangely, this item in the array CAN be null. FIXME do this below too?
					  {  
						  Transaction item_tx = null;
						  Session session = HibernateUtil.getSessionFactory().openSession();
						  int indentval = (new Random()).nextInt(10);
						  Global.printThreadHeader(indentval, session.hashCode(), "FirebaseChangeProcessor", "opening");
						  try
						  {
							  item_tx = session.beginTransaction();
							  Response r = Jsoup
									 .connect("https://hacker-news.firebaseio.com/v0/item/" + item.longValue()  + ".json")
									 .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
									 .ignoreContentType(true).execute();
							  result = r.body();
							  hnii = (Item)session.get(Item.class, item.longValue());
							  if(hnii == null)
							  {
								  /*** NEW ITEM ***/
								  System.out.println("item: " + item.longValue() + " does not exist. Creating.");
								  hnii = createItemFromHNAPIResult(result, true, session);
								  if(hnii != null)
								  {
									  session.save(hnii);
									  System.out.println("Done creating item: " + item.longValue());
								  }
							  }
							  else
							  {
								  /*** hnii is already an EXISTING ITEM ***/
								  Set<Long> oldkids = null;
								  HashSet<Long> newkids = null;
								  if(hnii.getKids() == null)
									  oldkids = new HashSet<Long>(); 
								  else
									  oldkids = (Set<Long>)hnii.getKids();
								  
								  Item new_hnii = createItemFromHNAPIResult(result, false, session);
								  if(new_hnii != null) // creation successful
								  {
									  // does the Item, as just read from HN API, have any kids?
									  if(new_hnii.getKids() != null) // if so, process them.
									  {
										  newkids = (HashSet<Long>)new_hnii.getKids();
											  
										  Iterator<Long> it2 = oldkids.iterator();
										  while(it2.hasNext())
										  {
											  newkids.remove(it2.next());
										  }
											  
										  Iterator<Long> newminusoldit = newkids.iterator();
										  Long currentnewkid = 0L;
										  while(newminusoldit.hasNext())
										  {
											  currentnewkid = newminusoldit.next();
											  System.out.print("kid: " + currentnewkid + " ");
											  Item hnitemitem = (Item)session.get(Item.class, currentnewkid);
											  if(hnitemitem == null)
											  {
												  System.out.print("adding! ");
												  Response r2 = Jsoup
														  .connect("https://hacker-news.firebaseio.com/v0/item/" + currentnewkid  + ".json")
														  .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
														  .ignoreContentType(true).execute();
												  String kid_result = r2.body();
												  hnitemitem = createItemFromHNAPIResult(kid_result, true, session);
												  if(hnitemitem != null)
												  {
													  session.save(hnitemitem);
												  }
											  }
											  else
											  {
												  // kid already exists. Do nothing.
											  }
										  }
									  }
									  if(new_hnii.getKids() != null && new_hnii.getKids().size() == 0)
										  new_hnii.setKids(null);
									  
									  session.merge(new_hnii);
									  
									  if(hnii.getType().equals("story") && hnii.getScore() != new_hnii.getScore())
									  {
										  long old_score = hnii.getScore();
										  long new_score = new_hnii.getScore();
										  if(old_score != new_score)
										  {
											  if(new_score > old_score)
												  System.out.println("New score: +" + (new_score - old_score));
											  else
												  System.out.println("New score: -" + (old_score - new_score));
											  
											  User author = (User) session.get(User.class, hnii.getBy());
											  if(author == null)
											  {
												  System.out.print(" Author of this item is NOT in the database.");
											  }
											  else
											  {
												  System.out.print(" Author of this item IS in the database");
												  if(author.getRegistered())
												  {
													  System.out.print(" and registered!");
													  // add this score change to the parent's notification feed
													  if(new_score > old_score)
													  {
														  createNotification(author, "3", hnii.getId(), hnii.getTime()*1000, null, 0, session);		 // feedable event 3, a story you wrote was upvoted
														  if(score_changes.containsKey(author.getId()))
															  score_changes.put(author.getId(), (new_score-old_score)+score_changes.get(author.getId()));
													  }
													  else
													  {
														  createNotification(author, "4", hnii.getId(), hnii.getTime()*1000, null, 0, session);	 // feedable event 4, a story you wrote was downvoted
														  if(score_changes.containsKey(author.getId()))
															  score_changes.put(author.getId(), (old_score-new_score)+score_changes.get(author.getId()));
													  }
												  }
												  else
												  {
													  System.out.print(" but NOT a registered user. Ignore.");
												  }
											  }
											  System.out.println();
										  }
									  }
								  }
							  }
							  item_tx.commit();
						  }
						  catch(IOException ioe)
						  {
							  System.out.println("*** IOException getting item " + item.longValue() + ", but execution should continue.");
						  }
						  catch (Exception e) {
							  if (item_tx!=null) item_tx.rollback();
							  e.printStackTrace();
						  }
						  finally {
							  Global.printThreadHeader(indentval, session.hashCode(), "FirebaseChangeProcessor", "closing");
							  session.close();
						  }
					  }
				  }
			  }
			  /***
			   *    ____________ ___________ _____ _      _____ _____ 
			   *    | ___ \ ___ \  _  |  ___|_   _| |    |  ___/  ___|
			   *    | |_/ / |_/ / | | | |_    | | | |    | |__ \ `--. 
			   *    |  __/|    /| | | |  _|   | | | |    |  __| `--. \
			   *    | |   | |\ \\ \_/ / |    _| |_| |____| |___/\__/ /
			   *    \_|   \_| \_|\___/\_|    \___/\_____/\____/\____/ 
			   *                                                      
			   *                                                      
			   */
			  else if(child.getKey().equals("profiles"))
			  {	  
				  str_value_al = child.getValue(ArrayList.class);
				  System.out.println(child.getKey() + " " + str_value_al.toString());
				  User useritem = null;
				  String result = null;
				  Iterator<String> it = str_value_al.iterator();
				  String screenname = null;
				  while(it.hasNext())
				  {
					 screenname = it.next();	
					 
					 Transaction user_tx = null; 
					 Session session = HibernateUtil.getSessionFactory().openSession();
					 int indentval = (new Random()).nextInt(10);
					 Global.printThreadHeader(indentval, session.hashCode(), "FirebaseChangeProcessor", "opening");
					 try
					 {
						 user_tx = session.beginTransaction();	
						 result = Jsoup
								 .connect("https://hacker-news.firebaseio.com/v0/user/" + screenname  + ".json")
								 .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
								 .ignoreContentType(true).execute().body();
						 useritem = (User)session.get(User.class, screenname);
						 if(useritem == null)
						 {
							 System.out.println("Creating " + screenname + ". ");
							 useritem = createUserFromHNAPIResult(result);
							 if(useritem != null)
								 session.save(useritem);
						 }
						 else
						 {
							  System.out.println("Updating " + screenname + ". ");
							  if(!(result == null || result.isEmpty())) // 
							  {
									  new_jo = new JSONObject(result);
									  boolean saveuseritem = false;
									 
									  long new_karma = new_jo.getLong("karma");
									  long old_karma = useritem.getHNKarma();
									  if(old_karma != new_karma)
									  {
										  long ttl = useritem.getKarmaPoolTTLMins();
										  if(ttl > 1440 || ttl < 5) // one day to five minutes is the acceptable range. If not, reset it to 15 mins.
										  {
											  ttl = 15;
											  useritem.setKarmaPoolTTLMins(ttl);
										  }
										  
										  if(useritem.getLastKarmaPoolDrain() == null || (useritem.getLastKarmaPoolDrain() < (System.currentTimeMillis() - (ttl*60000L)))) // it's been more than ttl minutes
										  {
											  long change = new_karma - old_karma;
											  System.out.print("\treporting change " + change + " if the user is registered");
											  if(useritem.getRegistered()) // this change in karma for this firebase increment (30 sec) + the total change in the karma pool (10 mins) cancel each other out.
											  {											  // create notification only if registered
												  System.out.print("...which they ARE.");
												  if(change > 0L)
												  {
													  System.out.println("\tcalling createNotification for positive change.");
													  createNotification(useritem, "1", 0L, System.currentTimeMillis(), null, change, session); // feedable event 1, positive karma change
												  }
												  else if(change < 0L)				
												  {
													  System.out.println("\tcalling createNotification for negative change.");
													  createNotification(useritem, "2", 0L, System.currentTimeMillis(), null, change, session); // feedable event 2, negative karma change
												  }
											  }
											  else
											  {
												  System.out.println("...which they are not.");
											  }
											  // regardless, empty the pool and set new timestamp
											  useritem.setHNKarma(new_karma);
											  //useritem.setKarmaPool(0L);
											  useritem.setLastKarmaPoolDrain(System.currentTimeMillis());
										  }
										  else
										  {
											  System.out.println("\tWAITING to report change.");
											 // useritem.setKarmaPool(useritem.getKarmaPool() + (new_karma - old_karma));
										  }
										  saveuseritem = true;
									  }
									  else // the user's karma as reported by HN API right now is exactly the same as what we've got on file. Do nothing.
									  {
										  
									  }	 
									  
									  // keep track of "about" changes? I guess. Why not.
									  String old_about = useritem.getHNAbout();
									  String new_about = "";
									  if(new_jo.has("about"))
										  new_about = new_jo.getString("about");
									  if(old_about == null || new_about == null) // one or both was null
									  {
										  if(old_about == null && new_about == null)
										  { 
											  // both null, no change, do nothing
										  }
										  else if(old_about == null && new_about != null)
										  {
											  System.out.println("ABOUT: old_about was null. new_about is not. Saving.");
											  useritem.setHNAbout(new_about);
											  saveuseritem = true; 
										  }
										  else if(old_about != null && new_about == null)
										  {
											  System.out.println("ABOUT: old_about was not null. new_about is. Saving.");
											  useritem.setHNAbout(null);
											  saveuseritem = true; 
										  }
									  }
									  else
									  {
										  if(!old_about.equals(new_about))
										  {
											  System.out.println("ABOUT: about string changed. Saving.");
											  useritem.setHNAbout(new_about);
											  saveuseritem = true; 
										  }
										  // else neither null, both equal, no change. do nothing.
									  }
									
									  
									  if(saveuseritem)
									  {
										  session.update(useritem);
									  }
									  
									  /*
									  // no reason to keep track of this user's submitted items because any new items will appear in the ITEMS block above.
									  JSONArray old_submitted_ja = new JSONArray();
									  if(old_jo.has("submitted"))
										  old_submitted_ja = old_jo.getJSONArray("submitted");
									  List<String> old_submitted_list = new ArrayList<String>();
									  for(int i = 0; i < old_submitted_ja.length(); i++){
										  old_submitted_list.add(old_submitted_ja.getString(i));
									  }
									  
									  JSONArray new_submitted_ja = new JSONArray();
									  if(new_jo.has("submitted"))
										  new_submitted_ja = new_jo.getJSONArray("submitted");
									  List<String> new_submitted_list = new ArrayList<String>();
									  for(int i = 0; i < new_submitted_ja.length(); i++){
										  new_submitted_list.add(new_submitted_ja.getString(i));
									  }
									  
									  Iterator<String> it2 = old_submitted_list.iterator();
									  while(it2.hasNext())
									  {
										  new_submitted_list.remove(it2.next());
									  }
									  
									  Iterator<String> newminusoldit = new_submitted_list.iterator();
									  while(newminusoldit.hasNext())
									  {
										  System.out.println("Found new submitted: " + newminusoldit.next());
									  }								  
									   */
							  }
						 }
						 user_tx.commit();
					 }
					 catch(JSONException jsone)
					 {
						 jsone.printStackTrace();
					 }
					 catch(IOException ioe)
					 {
						 System.out.println("*** IOException getting user " + useritem.getId() + ", but execution should continue.");
					 }
					 catch (Exception e) 
					 {
						 if (user_tx!=null) user_tx.rollback();
						 e.printStackTrace();
					 }
					 finally 
					 {
						 Global.printThreadHeader(indentval, session.hashCode(), "FirebaseChangeProcessor", "closing");
						 session.close();
					 }
				  }
			  }
			  else 
			  {
				  System.err.println("child.getKey() was something other than \"items\" or \"profiles\"");
			  }
		}
		 
		long exit = System.currentTimeMillis();
		System.out.println("=== " + super.getId() +  " FirebaseChangeProcessor Done. elapsed=" + ((exit - entry)/1000L) + "s at " + sdf.format(exit));
		return;
	}
	

    private Item createItemFromHNAPIResult(String unchecked_result, boolean processFeeds, Session session)
    {
    	if(unchecked_result == null || unchecked_result.isEmpty())
    	{
    		System.err.println("createItemFromHNAPIResult(): Error trying to create new item in DB: result string from HN api was null or empty");
    		return null;
    	}
    	try{
    		Item hnii = null;
    		JSONObject new_jo = new JSONObject(unchecked_result);
    		// these are the required fields (as far as we're concerned)
    		// without them, we can't even make sense of what to do with it
    		if(new_jo.has("id") && new_jo.has("by") && new_jo.has("time") && new_jo.has("type")) 
    		{
				  /*** THESE FIELDS MUST MATCH HNItem EXACTLY ***/
				  
    			/*
    private long id; 
	private String by;
	private long time;
	private String type;
	private boolean dead;
	private boolean deleted;
	private long parent;
	private long score;
	private Set<Long> kids;
	private String url;
    			 */
    			
				  hnii = new Item();
				  hnii.setId(new_jo.getLong("id"));
				  // new fields
				  hnii.setBy(new_jo.getString("by"));
				  hnii.setTime(new_jo.getLong("time"));
				  hnii.setType(new_jo.getString("type"));
				 
				  if(new_jo.has("dead") && new_jo.getBoolean("dead") == true)
					  hnii.setDead(true);
				  else
					  hnii.setDead(false);
				  if(new_jo.has("deleted") && new_jo.getBoolean("deleted") == true)
					  hnii.setDeleted(true);
				  else
					  hnii.setDeleted(false);
				  
				  if(new_jo.has("parent"))
					  hnii.setParent(new_jo.getLong("parent"));
				  
				  if(new_jo.has("score"))
					  hnii.setScore(new_jo.getLong("score"));
				  
				  if(new_jo.has("kids"))
				  {
					  HashSet<Long> kids_ts = new HashSet<Long>();
					  JSONArray ja = new_jo.getJSONArray("kids");
					  if(ja != null && ja.length() > 0)
					  {	  
						  int x = 0;
						  while(x < ja.length())
						  {
							  kids_ts.add(ja.getLong(x));
							  x++;
						  }
						  if(kids_ts.size() == ja.length()) // if the number of items has changed for some reason, just skip bc something has messed up
						  {
							  System.out.println("createItemFromHNAPIResult(): setting kids=" + kids_ts.size());
							  hnii.setKids(kids_ts);
						  }
					  }
					  else
						  hnii.setKids(null);
				  }
				  
				  if(new_jo.has("title"))
					  hnii.setTitle(new_jo.getString("title"));
				  
				  if(new_jo.has("url"))
					  hnii.setURL(new_jo.getString("url"));
				  
				  if(new_jo.has("text"))
					  hnii.setOriginalText(new_jo.getString("text"));
				  
				  long now = System.currentTimeMillis();
				  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
				  sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
				  
				  if(processFeeds)
				  {
					  System.out.println("createItemFromHNAPIResult(): processFeeds == true");
					  System.out.println("createItemFromHNAPIResult(): is time=" + (hnii.getTime()*1000L) + " > " + (now-86400000L) + "? tospare=" + ((hnii.getTime()*1000L)-(now-86400000L)) + " (" + sdf.format((hnii.getTime()*1000L)) + " to " + sdf.format(now-86400000L) + ") ");
					  if(hnii.getTime()*1000L > (now - 86400000L)) // make sure this is within the past day before sending any notifications
					  {
						  System.out.println("createItemFromHNAPIResult(): Young enough. Processing if not deleted or dead.");
						  if(hnii.getType().equals("comment") && !hnii.getDead() && !hnii.getDeleted())
						  {
							  processNewCommentForFeeds(hnii, session);
						  }
						  else if(hnii.getType().equals("story") && !hnii.getDead() && !hnii.getDeleted())
						  {
							  processNewStoryForFeeds(hnii, session);
						  }
						  else
						  {
							  // deleted, dead or something other than comment/story (poll, for instance)
						  }
					  }
					  else
					  {
						  System.out.println("createItemFromHNAPIResult(): Too old.");
					  }
				  }
				  else
				  {
					  System.out.println("createItemFromHNAPIResult(): processFeeds == false");
				  }
				  return hnii;
    		}
    		else
    		{
    			System.out.println("createItemFromHNAPIResult(): WARNING: Error trying to create new item in DB: missing required id, by, time or type values. Skipping.");
    			return null;
    		}
		  }
		  catch(JSONException jsone)
		  {
			  System.out.println("createItemFromHNAPIResult(): WARNING: Error trying to create new item in DB: result string was not valid JSON. Skipping");
			  return null;
		  }
    }
    
    private void processNewCommentForFeeds(Item hnii, Session session) throws JSONException
    {
    	/***
    	 *    ____________ _____ _____  _____ _____ _____   _____ ________  ______  ___ _____ _   _ _____  ______ ___________  ______ _____ ___________  _____ 
    	 *    | ___ \ ___ \  _  /  __ \|  ___/  ___/  ___| /  __ \  _  |  \/  ||  \/  ||  ___| \ | |_   _| |  ___|  _  | ___ \ |  ___|  ___|  ___|  _  \/  ___|
    	 *    | |_/ / |_/ / | | | /  \/| |__ \ `--.\ `--.  | /  \/ | | | .  . || .  . || |__ |  \| | | |   | |_  | | | | |_/ / | |_  | |__ | |__ | | | |\ `--. 
    	 *    |  __/|    /| | | | |    |  __| `--. \`--. \ | |   | | | | |\/| || |\/| ||  __|| . ` | | |   |  _| | | | |    /  |  _| |  __||  __|| | | | `--. \
    	 *    | |   | |\ \\ \_/ / \__/\| |___/\__/ /\__/ / | \__/\ \_/ / |  | || |  | || |___| |\  | | |   | |   \ \_/ / |\ \  | |   | |___| |___| |/ / /\__/ /
    	 *    \_|   \_| \_|\___/ \____/\____/\____/\____/   \____/\___/\_|  |_/\_|  |_/\____/\_| \_/ \_/   \_|    \___/\_| \_| \_|   \____/\____/|___/  \____/ 
    	 *                                                                                                                                                     
    	 *                                                                                                                                                     
    	 */
    	
    	  Set<String> already_notified_users = new HashSet<String>(); 
		  if(hnii == null) // if the hnii input is invalid, return.
		  {
			  System.out.println("processNewCommentForFeeds(hnii): Cannot proceed because hnii is null. Returning.");
			  return;
		  }
		  
		  String triggerer = hnii.getBy();
		  Item current_hnii = hnii;
		  User current_author;
		  int parentlevel = 0; // 1=parents, 2=grandparents, etc
		  while(current_hnii.getParent() != null && current_hnii.getParent() != 0L)
		  {
			  current_hnii = (Item) session.get(Item.class, current_hnii.getParent()); 
			  if(current_hnii == null)
				  break;
			  parentlevel++;
			  current_author = (User) session.get(User.class, current_hnii.getBy());
			  if(current_author == null)
			  {
				  System.out.println("processNewCommentForFeeds(hnii): author of parent (" + current_hnii.getBy() + ") is not NOT in the database, skipping.");
			  }
			  else
			  {
				  System.out.println("processNewCommentForFeeds(hnii): author of parent (" + current_hnii.getBy() + ") IS in the database, checking if registered and not already notified...");
				  if(!current_author.getId().equals(triggerer) // don't notify a person of deep-replies to himself 
						  && current_author.getRegistered() 
						  && !already_notified_users.contains(current_author.getId())) // a user could have multiple parents above this incoming comment. Don't notify them multiple times.
				  {
					  System.out.println("processNewCommentForFeeds(hnii): author of parent (" + current_hnii.getBy() + ") IS in the database, registered and not already notified. Creating notification.");
					  if(parentlevel == 1)
					  {
						  if(current_hnii.getType().equals("comment"))
						  {
							  System.out.println("processNewCommentForFeeds(hnii): Adding replied-to-comment notification.");
							  createNotification(current_author, "5", hnii.getId(), hnii.getTime()*1000, hnii.getBy(), 0, session); // feedable event 5, a comment current_author wrote was replied to
							  already_notified_users.add(current_author.getId());
						  }
						  else if(current_hnii.getType().equals("story"))
						  {
							  System.out.println("processNewCommentForFeeds(hnii): Adding commented-on-story notification.");
							  createNotification(current_author, "6", hnii.getId(), hnii.getTime()*1000, hnii.getBy(), 0, session); // feedable event 6, a story current_author wrote was replied to
							  already_notified_users.add(current_author.getId());
						  }
						  // else, polls etc
					  }
					  else // parentlevel > 1
					  {
						  if(current_hnii.getType().equals("comment"))
						  {
							  System.out.println("processNewCommentForFeeds(hnii): Adding deep replied-to-comment notification.");
							  createNotification(current_author, "9", hnii.getId(), hnii.getTime()*1000, hnii.getBy(), 0, session); // feedable event 9, a comment current_author wrote was deep-replied to
							  already_notified_users.add(current_author.getId());
						  }
						  else if(current_hnii.getType().equals("story")) // this is where functionality to notify story posters on deep replies would be added
						  {
							  System.out.println("processNewCommentForFeeds(hnii): Skipping deep replied-to-story notification.");
						  }
						  // else, polls etc
					  }
				  }
				  else
				  {
					  System.out.println("processNewCommentForFeeds(hnii): author of parent (" + current_hnii.getBy() + ") IS in the database but NOT registered. Skipping notifications.");
				  }
			  }
		  }
		  
		  // check for followers of this comment's \"by\" and alert them
		  
		  User author = (User)session.get(User.class, hnii.getBy());
		  if(author == null)
		  {
			  System.out.println("processNewCommentForFeeds(hnii): Author of this comment (" + hnii.getBy() + ") is NOT in the database. Skipping notifications.");
		  }
		  else
		  {
			  System.out.println("processNewCommentForFeeds(hnii): Author of this comment (" + hnii.getBy() + ") IS in the database. Checking to see if they have any followers.");
			  Set<String> followers = (Set<String>) author.getFollowers();
			  if(followers == null)
			  {
				  System.out.println("processNewCommentForFeeds(hnii): No followers.");
			  }
			  else if(followers.isEmpty())
			  {
				  System.out.println("processNewCommentForFeeds(hnii): No followers. (getFollwers() was Empty (not null) and we saved it to null just now.)");
				  author.setFollowers(null);
				  session.update(author);
			  }
			  else
			  { 
				  System.out.println("processNewCommentForFeeds(hnii): author (" + hnii.getBy() + ") has followers.");
				  Iterator<String> followers_it = followers.iterator();
				  String currentfollower = "";
				  User followeruseritem = null;
				  while(followers_it.hasNext())
				  {
					  currentfollower = followers_it.next();
					  followeruseritem = (User) session.get(User.class, currentfollower);
					  if(followeruseritem != null && followeruseritem.getRegistered()) // if a user is following this commenter, they should be registered, but I guess this check is fine.						
					  {  
						  System.out.println("processNewCommentForFeeds(hnii): Found a valid, registered follower (" + followeruseritem.getId() + ") and creating notification.");
						  if(!already_notified_users.contains(followeruseritem.getId())) // only send notification if the user hasn't been alerted in the reply-checking block above.
							  createNotification(followeruseritem, "8", hnii.getId(), hnii.getTime()*1000, author.getId(), 0, session); // feedable event 8, a user you're following commented
					  }
				  }
			  }
		  }
    }
    
    private void processNewStoryForFeeds(Item hnii, Session session) throws JSONException
    {
    	/***
    	 *    ____________ _____ _____  _____ _____ _____   _____ _____ _____________   __ ______ ___________  ______ _____ ___________  _____ 
    	 *    | ___ \ ___ \  _  /  __ \|  ___/  ___/  ___| /  ___|_   _|  _  | ___ \ \ / / |  ___|  _  | ___ \ |  ___|  ___|  ___|  _  \/  ___|
    	 *    | |_/ / |_/ / | | | /  \/| |__ \ `--.\ `--.  \ `--.  | | | | | | |_/ /\ V /  | |_  | | | | |_/ / | |_  | |__ | |__ | | | |\ `--. 
    	 *    |  __/|    /| | | | |    |  __| `--. \`--. \  `--. \ | | | | | |    /  \ /   |  _| | | | |    /  |  _| |  __||  __|| | | | `--. \
    	 *    | |   | |\ \\ \_/ / \__/\| |___/\__/ /\__/ / /\__/ / | | \ \_/ / |\ \  | |   | |   \ \_/ / |\ \  | |   | |___| |___| |/ / /\__/ /
    	 *    \_|   \_| \_|\___/ \____/\____/\____/\____/  \____/  \_/  \___/\_| \_| \_/   \_|    \___/\_| \_| \_|   \____/\____/|___/  \____/ 
    	 *                                                                                                                                     
    	 *                                                                                                                                     
    	 */
    	
    	  if(hnii == null || !hnii.getType().equals("story") || hnii.getDead() || hnii.getDeleted())
    	  {
    		  System.out.println("processNewStoryForFeeds(hnii): hnii was null, or not a \"story\" or dead or deleted. Skipping processing of feeds.");
    		  return;
    	  }

    	  if(hnii.getScore() > 1) // this is a brand new story, but already has an upvote
			  System.out.println("processNewStoryForFeeds(hnii): NOTE: This is a BRAND NEW STORY (with url) THAT ALREADY HAS AN UPVOTE. Proceeding.");
		  
    	  
		  // check for followers of this new story's \"by\" and alert them
		  User author = (User) session.get(User.class, hnii.getBy());
		  if(author == null)
		  {
			  System.out.println("processNewStoryForFeeds(hnii): Author of this story is NOT in the database. Skipping processing of feeds.");
		  }
		  else
		  {
			  System.out.println("processNewStoryForFeeds(hnii): Author of this story is in the database. See if they have any followers.");
			  Set<String> followers = (Set<String>) author.getFollowers();
			  if(followers == null)
			  {
				  System.out.println("processNewStoryForFeeds(hnii): No followers.");
			  }
			  else if(followers.isEmpty())
			  {
				  System.out.println("processNewStoryForFeeds(hnii): No followers. (getFollwers() was Empty (not null) and we saved it to null just now.)");
				  author.setFollowers(null);
				  session.update(author);
			  }
			  else
			  {
				  System.out.println("processNewStoryForFeeds(hnii): author has followers.");
				  Iterator<String> followers_it = followers.iterator();
				  String currentfollower = "";
				  User followeruseritem = null;
				  while(followers_it.hasNext())
				  {
					  currentfollower = followers_it.next();
					  followeruseritem = (User) session.get(User.class, currentfollower);
					  if(followeruseritem != null && followeruseritem.getRegistered()) // if a user is following this poster, they should be registered, but I guess this check is fine.		
					  {  
						  System.out.println("processNewStoryForFeeds(hnii): Found a valid, registered follower (" + followeruseritem + ") and creating notification.");
						  createNotification(followeruseritem, "7", hnii.getId(), hnii.getTime()*1000, author.getId(), 0, session); // feedable event 7, a user you're following posted a story with a URL
					  }
				  }
			  }
		  }
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

    
    public boolean createNotification(User useritem, String type, long hn_target_id, long action_time, String triggerer, long karma_change, Session session)
    {
    	if(!useritem.getRegistered()) // never create notification items for users that aren't registered.
    		return false;
    	
    	long now = System.currentTimeMillis();
    	String now_str = Global.fromDecimalToBase62(7,action_time);
    	Random generator = new Random(); 
		int r = generator.nextInt(238327); // this will produce numbers that can be represented by 3 base62 digits
    	String randompart_str = Global.fromDecimalToBase62(3,r);
		String notification_id = now_str + randompart_str + type; 
		
		Notification ni = new Notification();
		ni.setId(notification_id);
		ni.setActionMSFE(action_time);
		ni.setMSFE(now);
		ni.setUserId(useritem.getId());
		ni.setType(type);
		if(type.equals("1") || type.equals("2"))
		{
			ni.setHNRootStoryOrPollId(0L);
			ni.setHNRootCommentId(0L);
			ni.setHNTargetId(0L);
			ni.setTriggerer(null);
			ni.setKarmaChange(karma_change);
		}
		else if(type.equals("3") || type.equals("4") || type.equals("7")) // a story you wrote was upvoted, downvoted or a user you're following wrote a story
		{
			ni.setHNTargetId(hn_target_id);
			ni.setHNRootStoryOrPollId(hn_target_id);
			ni.setHNRootCommentId(0L);
			ni.setTriggerer(triggerer);
		}
		else if(type.equals("5") || type.equals("6") || type.equals("8") || type.equals("9")) // a comment you wrote was replied to(5) or deep-replied to(9) or story you wrote was commented on(6) or a user you're following commented(8). Crawl back to root.
		{
			ni.setHNTargetId(hn_target_id);
			System.out.println("Found an item we need to step back on to find root.");
			HashMap<String,Long> roots = Global.findRootStoryAndComment(hn_target_id);
			if(roots == null)
				return false; // couldn't find root, so bail
			ni.setHNRootStoryOrPollId(roots.get("story_or_poll"));
			ni.setHNRootCommentId(roots.get("comment")); // for #6, this should always be the same as hn_target_id
			ni.setTriggerer(triggerer);
		}
		System.out.println("createNotification(): Creating notificationItem for " + useritem.getId() + "... ");
		session.save(ni);
		
		if(type.equals("7") || type.equals("8"))// the two newsfeed types
		{
			
			TreeSet<String> newsfeedset = new TreeSet<String>();
	    	if(useritem.getNewsfeedIds() != null)
	    		newsfeedset.addAll(useritem.getNewsfeedIds());
	    	newsfeedset.add(notification_id);
	    	while(newsfeedset.size() > Global.NEWSFEED_SIZE_LIMIT)
	    		newsfeedset.remove(newsfeedset.first());
	    	useritem.setNewsfeedIds(newsfeedset);
	    	if(useritem.getNotificationMode() != null && useritem.getNotificationMode().equals("newsfeed_and_notifications")) // only do this if they want news feed notifications
	    		useritem.setNewsfeedCount(useritem.getNewsfeedCount()+1);
	    	session.update(useritem);
		}
		else // everything else, i.e. the notification types
		{	
			TreeSet<String> notificationset = new TreeSet<String>();
	    	if(useritem.getNotificationIds() != null)
	    		notificationset.addAll(useritem.getNotificationIds());
	    	notificationset.add(notification_id);
	    	while(notificationset.size() > Global.NOTIFICATIONS_SIZE_LIMIT)
	    		notificationset.remove(notificationset.first());
	    	useritem.setNotificationIds(notificationset);
	    	useritem.setNotificationCount(useritem.getNotificationCount()+1);
	    	System.out.println("createNotification(): Updating " + useritem.getId() + "'s notificationIds with item " + notification_id);
	    	session.update(useritem);
		}
    	return true;
    }
	
}