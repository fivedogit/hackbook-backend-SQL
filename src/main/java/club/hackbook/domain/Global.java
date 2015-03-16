
package club.hackbook.domain;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;

import org.hibernate.Session;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

public class Global {

	public static final Long NEWSFEED_SIZE_LIMIT = 40L;
	public static final Long NOTIFICATIONS_SIZE_LIMIT = 30L;
	public static boolean devel = false;
	
	public static boolean isWholeNumeric(String incoming_string) // only works for whole numbers, positive and negative
	{
		  int x=0;
		  while(x < incoming_string.length())
		  {
			  if((x==0 && incoming_string.substring(0,1).equals("-")) ||  // OK if first element is "-"
					  incoming_string.substring(x,x+1).equals("0") || incoming_string.substring(x,x+1).equals("1") ||
					  incoming_string.substring(x,x+1).equals("2") || incoming_string.substring(x,x+1).equals("3") ||
					  incoming_string.substring(x,x+1).equals("4") || incoming_string.substring(x,x+1).equals("5") ||
					  incoming_string.substring(x,x+1).equals("6") || incoming_string.substring(x,x+1).equals("7") ||
					  incoming_string.substring(x,x+1).equals("8") || incoming_string.substring(x,x+1).equals("9"))
			  {
				  // ok
			  }
			  else
			  {
				  return false;
			  }
			  x++;
		  }
		  return true;
	}	

	public static void printThreadHeader(int indent, int hashcode, String location_identifier, String opening_or_closing)
	{
		if(devel == true)
		{	
			int ohboy = 0;
			System.out.print(opening_or_closing + " session ");
			while(ohboy < indent)
			{
				System.out.print("---");
				ohboy++;
			}
			System.out.print(" " + hashcode);
			System.out.println(" (" + location_identifier + ")");
		}
	}
	
	public static String agoIt(Timestamp incoming_timestamp)
	{
	  Calendar now = Calendar.getInstance();
	  Calendar then = Calendar.getInstance();
	  String creationdate = incoming_timestamp.toString();
	  then.set((new Integer(creationdate.substring(0,4))).intValue(),(new Integer(creationdate.substring(5,7))).intValue()-1,(new Integer(creationdate.substring(8,10))).intValue(),(new Integer(creationdate.substring(11,13))).intValue(),(new Integer(creationdate.substring(14,16))).intValue(),(new Integer(creationdate.substring(17,19))).intValue());
	  long millis_ago = (new Long((now.getTimeInMillis() - then.getTimeInMillis()))).longValue();
	  int minutes_ago = (new Long(millis_ago/60000)).intValue();
	  int time_ago = 0;
	  String time_ago_units = "";
	  if(minutes_ago < 60)
	  {
		  time_ago = (new Long((now.getTimeInMillis() - then.getTimeInMillis()))).intValue()/60000;
		  time_ago_units = "mins";
	  }
	  else if ((minutes_ago > 60) && (minutes_ago < 1440))
	  {
		  time_ago = minutes_ago / 60;
		  time_ago_units = "hrs";
	  }
	  else
	  {	
		  time_ago = minutes_ago / 1440;
		  time_ago_units = "days";
	  }
	  if(time_ago == 1)
		  return (time_ago + " " + time_ago_units.substring(0,time_ago_units.length() -1) + " ago");
	  else
		  return (time_ago + " " + time_ago_units + " ago");
	}

	 private static final String baseDigits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	    
	 public static String fromDecimalToBase62 (int desiredlength, long decimalNumber) {
			long base = 62;
			String tempVal = decimalNumber == 0 ? "0" : "";
	        int mod = 0;
	        Long modLong = 0L;
	        while( decimalNumber != 0 ) {
	        	try
	        	{
	        		modLong = new Long(decimalNumber % base);
	        	}
	        	catch(Exception e)
	        	{
	        		System.err.println("Global.fromDecimalToBase62. Error doing decimalNumber % base. returning null");
	        		return null;
	        	}
	            mod = modLong.intValue();
	            tempVal = baseDigits.substring( mod, mod + 1 ) + tempVal;
	            decimalNumber = decimalNumber / base;
	        }

	        if(tempVal.length() > desiredlength)
	        	return null;
	        while(tempVal.length() < desiredlength)
	        {
	        	tempVal = "0" + tempVal;
	        }
	        return tempVal;
	    }	
	 
	/* public static long findRootItemLocal(long hn_target_id, DynamoDBMapper mapper, DynamoDBMapperConfig dynamo_config)
	 {
		 boolean foundroot = false;
		 int x = 0;
		 int limit = 15; // limit the number of possible loops to 15, just in case
		 while(!foundroot && x < limit)
		 {
			// System.out.print("Getting item https://hacker-news.firebaseio.com/v0/item/" + currentid  + ".json ");
			HNItem hnii = session.get(HNItem.class, hn_target_id);
			if(hnii == null)
			{
				return -1;
			}
			else if(hnii.getType().equals("story"))
			{
				return hnii.getId();
			}
			else if(hnii.getType().equals("comment") && hnii.getParent() != 0L)
			{
				hn_target_id = hnii.getParent();
			}
			else
			{
				return -1;
			}
			x++;
		 }
		 return -1; 
	 }*/
	 
	 public static HashMap<String,Long> findRootStoryAndCommentLocal(long hn_target_id, Session session)
	 {
		 HashMap<String,Long> returnmap = new HashMap<String,Long>();
		 int x = 0;
		 int limit = 15; // limit the number of possible loops to 15, just in case
		 long lastcomment = 0L;
		 while(x < limit)
		 {
			// System.out.print("Getting item https://hacker-news.firebaseio.com/v0/item/" + currentid  + ".json ");
			Item hnii = (Item) session.get(Item.class, hn_target_id);
			if(hnii == null)
			{
				return null;
			}
			else if(hnii.getType().equals("story") || hnii.getType().equals("poll"))
			{
				 returnmap.put("comment", lastcomment);
				 returnmap.put("story_or_poll", hnii.getId());
				 return returnmap;
			}
			else if(hnii.getType().equals("comment") && hnii.getParent() != 0L)
			{
				lastcomment = hnii.getId();
				hn_target_id = hnii.getParent();
			}
			else
			{
				return null;
			}
			x++;
		 }
		 return null; 
	 }
	 
	 /*
	 public static long findRootItem(long hn_target_id)
	 {
		 boolean foundroot = false;
		 long currentid = hn_target_id;
		 JSONObject currentobject = null;
		 String result = null;
		 int x = 0;
		 int limit = 15; // limit the number of possible loops to 15, just in case
		 try{
			 while(!foundroot && x < limit)
			 {
				// System.out.print("Getting item https://hacker-news.firebaseio.com/v0/item/" + currentid  + ".json ");
				 result = Jsoup
						 .connect("https://hacker-news.firebaseio.com/v0/item/" + currentid  + ".json")
						 .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
						 .ignoreContentType(true).execute().body();
				 currentobject = new JSONObject(result);
				 //System.out.println(" which had type=" + currentobject.getString("type"));
				 if(!currentobject.getString("type").equals("comment"))
				 {
					 //System.out.println("FOUND NON-COMMENT! type=" + currentobject.getString("type") + " setting hn_root_id=" +currentobject.getInt("id"));
					 foundroot = true;
					 return currentobject.getInt("id");
				 }
				 else
				 {
					 if(currentobject.has("parent"))
						 currentid = currentobject.getLong("parent"); // move to previous
					 else
						 break; // this is a failsafe in case we come across a comment that has no parent object. In this case, leave the root blank and handle properly on the client side.
				 }
				 x++;
			 }
		 }
		 catch(IOException ioe)
		 {
			 return -1;
		 }
		 catch(JSONException jsone)
		 {
			 return -1;
		 }
		 return -1; 
	 }*/
	
	 public static String findRootItemTitle(long hn_target_id)
	 {
		 boolean foundroot = false;
		 long currentid = hn_target_id;
		 JSONObject currentobject = null;
		 String result = null;
		 int x = 0;
		 int limit = 15; // limit the number of possible loops to 15, just in case
		 try{
			 while(!foundroot && x < limit)
			 {
				 result = Jsoup
						 .connect("https://hacker-news.firebaseio.com/v0/item/" + currentid  + ".json")
						 .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
						 .ignoreContentType(true).execute().body();
				 currentobject = new JSONObject(result);
				 if(!currentobject.getString("type").equals("comment"))
				 {
					 foundroot = true;
					 return currentobject.getString("title");
				 }
				 else
				 {
					 if(currentobject.has("parent"))
						 currentid = currentobject.getLong("parent"); // move to previous
					 else
						 break; // this is a failsafe in case we come across a comment that has no parent object. In this case, leave the root blank and handle properly on the client side.
				 }
				 x++;
			 }
		 }
		 catch(IOException ioe)
		 {
			 return null;
		 }
		 catch(JSONException jsone)
		 {
			 return null;
		 }
		 return null; 
	 }
	 
	 public static HashMap<String,Long> findRootStoryAndComment(long hn_target_id)
	 {
		 HashMap<String,Long> returnmap = new HashMap<String,Long>();
		 long currentid = hn_target_id;
		 JSONObject currentobject = null;
		 String result = null;
		 int x = 0;
		 int limit = 15; // limit the number of possible loops to 15, just in case
		 long lastcomment = 0L;
		 try{
			 while(x < limit)
			 {
				// System.out.print("Getting item https://hacker-news.firebaseio.com/v0/item/" + currentid  + ".json ");
				 result = Jsoup
						 .connect("https://hacker-news.firebaseio.com/v0/item/" + currentid  + ".json")
						 .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
						 .ignoreContentType(true).execute().body();
				 currentobject = new JSONObject(result);
				 //System.out.println(" which had type=" + currentobject.getString("type"));
				 if(!currentobject.getString("type").equals("comment"))
				 {
					 //System.out.println("FOUND NON-COMMENT! type=" + currentobject.getString("type") + " setting hn_root_id=" +currentobject.getInt("id"));
					 returnmap.put("comment", lastcomment);
					 returnmap.put("story_or_poll", currentobject.getLong("id"));
					 return returnmap;
				 }
				 else
				 {
					 if(currentobject.has("parent"))
					 {
						 lastcomment = currentobject.getLong("id");
						 currentid = currentobject.getLong("parent"); // move to previous
					 }
					 else
						 break; // this is a failsafe in case we come across a comment that has no parent object. In this case, leave the root blank and handle properly on the client side.
				 }
				 x++;
			 }
		 }
		 catch(IOException ioe)
		 {
			 return null;
		 }
		 catch(JSONException jsone)
		 {
			 return null;
		 }
		 return null; 
	 } 
	 
	public static void main(String [] args)
	{
		//Global g = new Global();
	}


}