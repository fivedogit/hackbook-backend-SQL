package club.hackbook.domain;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.Calendar;
import java.util.TreeSet;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;

import club.hackbook.util.HibernateUtil;


public class Endpoint extends HttpServlet {

	// static variables:
	private static final long serialVersionUID = 1L; 
	
	public void init(ServletConfig servlet_config) throws ServletException {
		System.out.println("Endpoint.init()");
		super.init(servlet_config);
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		response.setContentType("application/json; charset=UTF-8;");
		response.setHeader("Access-Control-Allow-Origin", "*"); // FIXME
		PrintWriter out = response.getWriter();
		JSONObject jsonresponse = new JSONObject();
		try {
			jsonresponse.put("response_status", "error");
			jsonresponse.put("message", "This endpoint doesn't speak POST.");
			out.println(jsonresponse);
		} catch (JSONException jsone) {
			out.println("{ \"response_status\": \"error\", \"message\": \"JSONException caught in Endpoint POST\"}");
			System.err.println("endpoint: JSONException thrown in doPost(). "
					+ jsone.getMessage());
		}
		return;
	}

	// this method queries for all 4 equal permutations of the incoming url
	private HashSet<Item> getAllHNItemsFromURL(String url_str, Session session) 
	{
		HashSet<Item> tempset = null;
		HashSet<Item> combinedset = new HashSet<Item>();
		tempset = getHNItemsFromURL(url_str, session);
		if (tempset != null) {
			combinedset.addAll(tempset); // as-is
			tempset = null;
		}

		// ideally, we'd try with/without slash on all the permutations below.
		// But that turns 4 lookups into 8, so let's just do the as-is +/- slash
		if (!url_str.endsWith("/")) {
			tempset = getHNItemsFromURL(url_str + "/", session); // as-is + trailing "/"
			if (tempset != null) {
				combinedset.addAll(tempset);
				tempset = null;
			}
		} else {
			String url_str_minus_trailing_slash = url_str.substring(0,
					url_str.length() - 1);
			tempset = getHNItemsFromURL(url_str_minus_trailing_slash, session); // as-is minus trailing "/"
			if (tempset != null) {
				combinedset.addAll(tempset);
				tempset = null;
			}
		}
		if (url_str.startsWith("https://")) {
			if (url_str.startsWith("https://www.")) // https & www.
			{
				tempset = getHNItemsFromURL("http://" + url_str.substring(12), session); // try http && !www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
				tempset = getHNItemsFromURL("https://" + url_str.substring(12), session); // try https && !www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
				tempset = getHNItemsFromURL(
						"http://www." + url_str.substring(12), session); // try http && www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
			} else if (!url_str.startsWith("https://www.")) // https & !www.
			{
				tempset = getHNItemsFromURL("http://" + url_str.substring(8), session); // try http && !www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
				tempset = getHNItemsFromURL(
						"http://www." + url_str.substring(8), session); // try http && www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
				tempset = getHNItemsFromURL(
						"https://www." + url_str.substring(8), session); // try https && www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
			}
		} else if (url_str.startsWith("http://")) {
			if (url_str.startsWith("http://www.")) // http & www.
			{
				tempset = getHNItemsFromURL("http://" + url_str.substring(11), session); // try http && !www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
				tempset = getHNItemsFromURL("https://" + url_str.substring(11), session); // try https && !www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
				tempset = getHNItemsFromURL(
						"https://www." + url_str.substring(11), session); // try https && www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
			} else if (!url_str.startsWith("http://www.")) // http & !www.
			{
				tempset = getHNItemsFromURL(
						"http://www." + url_str.substring(7), session); // try http && www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
				tempset = getHNItemsFromURL(
						"https://www." + url_str.substring(7), session); // try https && www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
				tempset = getHNItemsFromURL("https://" + url_str.substring(7), session);// try https && !www
				if (tempset != null) {
					combinedset.addAll(tempset); // try http && !www
					tempset = null;
				}
			}
		}
		return combinedset;
	}

	
	private HashSet<Item> getHNItemsFromURL(String url_str, Session session) 
	{
		String hql = "FROM Item I WHERE I.url='" + url_str + "'";
		Query query = session.createQuery(hql);
		@SuppressWarnings("unchecked")
		List<Item> items = query.list();
		
		// why convert this to a set? To get rid of duplicates?
		if (items != null && items.size() > 0) {
			HashSet<Item> returnset = new HashSet<Item>();
			for (Item item : items) {
				returnset.add(item);
			}
			return returnset;
		} else {
			return null;
		}
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
	{
		response.setContentType("application/json; charset=UTF-8;");
		response.setHeader("Access-Control-Allow-Origin","*");
		PrintWriter out = response.getWriter();
		JSONObject jsonresponse = new JSONObject();
		long timestamp_at_entry = System.currentTimeMillis();
		String method = request.getParameter("method");
		if(!request.isSecure() && !(Global.devel == true && request.getRemoteAddr().equals("127.0.0.1")))
		{
			try 
			{
				jsonresponse.put("message", "This API endpoint must be communicated with securely.");
				jsonresponse.put("response_status", "error");
			}
			catch(JSONException jsone)
			{
				out.println("{ \"response_status\": \"error\", \"message\": \"JSONException caught in Endpoint GET secure connection check. method=" + method + "\"}");
				System.err.println("endpoint: JSONException thrown in Endpoint GET secure connection check. " + jsone.getMessage());
				jsone.printStackTrace();
				return;
			}	
		}
		else if(method == null)
		{
			try 
			{
				jsonresponse.put("message", "Method not specified. This should probably produce HTML output reference information at some point.");
				jsonresponse.put("response_status", "error");
			}
			catch(JSONException jsone)
			{
				out.println("{ \"response_status\": \"error\", \"message\": \"JSONException caught in Endpoint GET method value sanity check.\"}");
				System.err.println("endpoint: JSONException thrown in Endpoint GET method value sanity check. " + jsone.getMessage());
				jsone.printStackTrace();
				return;
			}	
		}
		else
		{
			/***
			 *     _   _ _____ _   _         ___  _   _ _____ _   _  ___  ___ _____ _____ _   _ ___________  _____ 
			 *    | \ | |  _  | \ | |       / _ \| | | |_   _| | | | |  \/  ||  ___|_   _| | | |  _  |  _  \/  ___|
			 *    |  \| | | | |  \| |______/ /_\ \ | | | | | | |_| | | .  . || |__   | | | |_| | | | | | | |\ `--. 
			 *    | . ` | | | | . ` |______|  _  | | | | | | |  _  | | |\/| ||  __|  | | |  _  | | | | | | | `--. \
			 *    | |\  \ \_/ / |\  |      | | | | |_| | | | | | | | | |  | || |___  | | | | | \ \_/ / |/ / /\__/ /
			 *    \_| \_/\___/\_| \_/      \_| |_/\___/  \_/ \_| |_/ \_|  |_/\____/  \_/ \_| |_/\___/|___/  \____/ 
			 */
			if(method.equals("searchForHNItem") || method.equals("getHNAuthToken") || method.equals("verifyHNUser") || method.equals("getMostFollowedUsers") || method.equals("getItem"))
			{
					try 
					{
						if(method.equals("searchForHNItem"))
						{
							// default to unknown error message
							jsonresponse.put("response_status", "error");
							jsonresponse.put("message", "Unknown error");
							
							String url_str = request.getParameter("url");
							if(url_str != null && !url_str.isEmpty())
							{
								Session session = HibernateUtil.getSessionFactory().openSession();
								int indentval = (new Random()).nextInt(10);
								Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.searchForHNItem", "opening");
								Transaction tx = null; 
								try
								{
									 tx = session.beginTransaction();
									 HashSet<Item> hnitems = getAllHNItemsFromURL(url_str, session);
									 Item hnii = null;
									 if(hnitems == null)
										 hnii = null;
									 else if(hnitems.size() == 1)
										 hnii = hnitems.iterator().next();
									 else if(hnitems.size() > 1)
									 {
											System.out.println("There are multiple items matching this URL. Selecting the one with the highest score.");
											Iterator<Item> it = hnitems.iterator();
											long max = 0; 
											Item current = null;
											while(it.hasNext())
											{
												current = it.next();
												if(current.getScore() > max)
												{
													hnii = current;
													max = current.getScore();
												}
											}
									 }
																
									 if(hnii != null)
									 {
										 jsonresponse.put("response_status", "success");
										 jsonresponse.remove("message");
										 jsonresponse.put("objectID", hnii.getId());
									 }
									 else
									 {
										 jsonresponse.put("response_status", "success");
										 jsonresponse.remove("message");
										 jsonresponse.put("objectID", "-1");
									 }
									 tx.commit();
								}
								catch (Exception e) {
									if (tx!=null) tx.rollback();
									e.printStackTrace();
								}
								finally {
									Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.searchForHNItem", "closing");
									session.close();
								}
							}
							else
							{
								jsonresponse.put("response_status", "error");
								jsonresponse.put("message", "Invalid \"url\" parameter.");	
							}
						}
						else if(method.equals("getHNAuthToken")) // user has just chosen to log in with HN. Generate auth token for this screenname, save it, return it.
						{
							// FIXME, user could create arbitrary user names in the database with this. They wouldn't be able to register, and there would be no squatting effect, and people can already create arbitrary usernames on HN
							// but still, this doesn't feel right. There should probably be a check against the HN API right here.
							String screenname = request.getParameter("screenname");
							if(screenname == null || screenname.isEmpty())
							{
								jsonresponse.put("message", "Screenname was null or empty.");
								jsonresponse.put("response_status", "error");
							}
							else
							{
								Session session = HibernateUtil.getSessionFactory().openSession();
								int indentval = (new Random()).nextInt(10);
								Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.getHNAuthToken", "opening");
								Transaction tx = null; 
								try
								{
									// Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.getHNAuthToken", "begintx");
									 tx = session.beginTransaction();
									 User useritem = (User)session.get(User.class, screenname);
									 if(useritem == null)
									 {
										 useritem = new User();
										 useritem.setRegistered(false); 
										 useritem.setHideEmbeddedCounts(true);
									 }
									 useritem.setId(screenname);
									 String uuid = UUID.randomUUID().toString().replaceAll("-","");
									 useritem.setHNAuthToken(uuid);
									// Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.getHNAuthToken", "saveobj");
									 session.save(useritem);
									 jsonresponse.put("response_status", "success");
									 jsonresponse.put("token", uuid);
									 // Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.getHNAuthToken", "commit!");
									 tx.commit();	
								}
								catch (Exception e) {
									if (tx!=null) tx.rollback();
									e.printStackTrace();
								}
								finally {
									Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.getHNAuthToken", "closing");
									session.close();
								}
							}
						}
						else if(method.equals("verifyHNUser")) // Using the generated auth token above, user has changed their "about" page to include the token. Verify it independently.
						{										// This should probably be triggered by FirebaseListener for optimal performance.
							String screenname = request.getParameter("screenname");
							String topcolor = request.getParameter("topcolor");
							if(screenname == null || screenname.isEmpty())
							{
								jsonresponse.put("message", "Screenname was null or empty.");
								jsonresponse.put("response_status", "error");
							}
							else 
							{
								Session session = HibernateUtil.getSessionFactory().openSession();
								int indentval = (new Random()).nextInt(10);
								Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.verifyHNUser", "opening");
								Transaction tx = null; 
								try
								{
									 tx = session.beginTransaction();
									 User useritem = (User)session.get(User.class, screenname);
									 if(useritem == null)	// if the user has gotten to verifyHNUser, then the useritem stub should have just been created at getHNAuthToken. Fail if not.
									 {
										 jsonresponse.put("message", "No user by that screenname was found in the database.");
										 jsonresponse.put("response_status", "error");
									 }
									 else
									 {	
										 String stored_uuid = useritem.getHNAuthToken();
										 int x = 0;
										 String result = "";
										 String about = "";
										 String checked_uuid = "";
										 int bi = 0;
										 int ei = 0;
										 int limit = 7;
										 String hn_karma_str = "0";
										 String hn_since_str = "0";
										 JSONObject hn_user_jo = null;
											
										 // wait 11 seconds to do first try. This helps prevent read or socket timeout errors on tries 0, 1 and 2 which are unlikely to work anyway.
										 try {
											 java.lang.Thread.sleep(11000);
										 } catch (InterruptedException e) {
											 // TODO Auto-generated catch block
											 e.printStackTrace();
										 }
										 x=2;
										 while(x < limit) // 2 (11 sec), 3 (16 sec), 4 (21 sec), 5 (26 sec), 6 (31 sec)
										 {
											 try
											 {
												 result = Jsoup
															 .connect("https://hacker-news.firebaseio.com/v0/user/" + screenname  + ".json")
															 .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
															 .ignoreContentType(true).execute().body();
													
												 //System.out.println("Endpoint.verifyHNUser():" + result);
												 if(result == null || result.equals("null") || result.isEmpty())
												 {
													 jsonresponse.put("response_status", "error");
													 jsonresponse.put("message", "Hackbook encountered an error attempting to validate you with the HN API. If you are a new user or one with low karma, the HN API does not recognize you and Hackbook will not be able to verify your account. Sorry.");
													 break;
												 }
												 else
												 {	
													 hn_user_jo = new JSONObject(result);
													 about = hn_user_jo.getString("about");
													 bi = about.indexOf("BEGIN|");
													 if(bi != -1)                                   // entering here means the loop WILL break 1 of 3 ways: No |ENDTOKEN, match or no match.
													 {
														 ei = about.indexOf("|END");
														 if(ei == -1)
														 {
															 jsonresponse.put("response_status", "error");
															 jsonresponse.put("message", "Found \"BEGIN|\" but not \"|END\"");
															 break;
														 }
														 else
														 {
															 checked_uuid = about.substring(bi + 6, ei);
															 if(checked_uuid.equals(stored_uuid))
															 {	
																 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
																 sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
																 
																 String uuid_str = UUID.randomUUID().toString().replaceAll("-","");
																 Calendar cal = Calendar.getInstance();
																 long now = cal.getTimeInMillis();
																 cal.add(Calendar.YEAR, 1);
																 long future = cal.getTimeInMillis();
																 if(!useritem.getRegistered()) // if user is not yet registered, populate default values
																 {
																	 useritem.setNotificationCount(0L);
																	 useritem.setId(screenname);
																	 useritem.setSince(now);
																	 useritem.setSinceHumanReadable(sdf.format(now));
																	 useritem.setRegistered(true);
																	 useritem.setURLCheckingMode("stealth");
																	 useritem.setHideEmbeddedCounts(true);
																 }
																 if(topcolor != null && isValidTopcolor(topcolor))
																	 useritem.setHNTopcolor(topcolor);
																 else
																	 useritem.setHNTopcolor("ff6600");
																 useritem.setSeen(now);
																 useritem.setSeenHumanReadable(sdf.format(now));
																 useritem.setThisAccessToken(uuid_str);
																 useritem.setThisAccessTokenExpires(future);
																 useritem.setHNAuthToken(null);
																	
																 if(hn_user_jo.has("karma")) 
																 {	
																	 hn_karma_str = hn_user_jo.getString("karma");
																	 if(Global.isWholeNumeric(hn_karma_str))
																		 useritem.setHNKarma(Long.parseLong(hn_karma_str));
																	 else
																		 useritem.setHNKarma(0L); // if "karma" is somehow not a whole integer, set to 0
																 }
																 else
																	 useritem.setHNKarma(0L); // if "karma" is somehow missing, set to 0

																 if(hn_user_jo.has("created")) 
																 {	
																	 hn_since_str = hn_user_jo.getString("created");
																	 if(Global.isWholeNumeric(hn_since_str))
																		 useritem.setHNSince(Long.parseLong(hn_since_str));
																	 else
																		 useritem.setHNSince(0L); // if "karma" is somehow not a whole integer, set to 0
																 }
																 else
																	 useritem.setHNSince(0L); // if "karma" is somehow missing, set to 0
																	
																 session.save(useritem);
																	
																 //System.out.println("Endpoint.loginWithGoogleOrShowRegistration() user already registered, logging in");
																 jsonresponse.put("response_status", "success");
																 jsonresponse.put("verified", true);
																 jsonresponse.put("this_access_token", uuid_str);
																 jsonresponse.put("screenname", useritem.getId());
																 break;
															 }
															 else
															 {
																 System.out.println("Loop " + x + ", Found BEGIN| and |END, but the string didn't match the DB. Trying again in 5 seconds.");
																 try {
																	 java.lang.Thread.sleep(5000);
																 } catch (InterruptedException e) {
																	 // TODO Auto-generated catch block
																	 e.printStackTrace();
																 }
																 x++;
															 }
														 }
													 }
													 else
													 {
														 System.out.println("Loop " + x + ", Did not find BEGIN| or |END. Trying again in 5 seconds.");
														 try {
															 java.lang.Thread.sleep(5000);
														 } catch (InterruptedException e) {
															 // TODO Auto-generated catch block
															 e.printStackTrace();
														 }
														 x++;
													 }
												 }
											 }
											 catch(IOException ioe)
											 {
												 System.err.println("IOException attempting to verifyHNUser. Ignore and continue.");
											 }
											}
										 if(x == limit)
										 {
											 System.out.println("Checked " + limit + " times and failed. Returning response_status = error.");
											 jsonresponse.put("response_status", "error");
											 jsonresponse.put("message", "Checked " + limit + " times and didn't find \"BEGIN|\"");
										 }
									 }
									 tx.commit();	
								}
								catch (Exception e) {
									if (tx!=null) tx.rollback();
									e.printStackTrace();
								}
								finally {
									Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.verifyHNUser", "closing");
									session.close();
								}
							}
						}
						else if(method.equals("getMostFollowedUsers"))
						{
							Session session = HibernateUtil.getSessionFactory().openSession();
							int indentval = (new Random()).nextInt(10);
							Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.getMostFollowedUsers", "opening");
							Transaction tx = null; 
							try
							{
								 tx = session.beginTransaction();
								 Globalvar gvi = (Globalvar)session.get(Globalvar.class, "most_followed_users");
								 if(gvi != null)
								 {
									 jsonresponse.put("response_status", "success");
									 jsonresponse.put("most_followed_users", new JSONArray(gvi.getStringValue()));
								 }
								 else
								 {
									 jsonresponse.put("response_status", "error");
									 jsonresponse.put("message", "Couldn't get most_followed_users value from DB.");
								 }
								 tx.commit();	
							}
							catch (Exception e) {
								if (tx!=null) tx.rollback();
								e.printStackTrace();
							}
							finally {
								Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.getMostFollowedUsers", "closing");
								session.close();
							}		
						}
						else if(method.equals("getItem"))
						{
							String id = request.getParameter("id");
							if(id == null || id.isEmpty() || !Global.isWholeNumeric(id))
							{
								jsonresponse.put("message", "id value was null, empty or invalid");
								jsonresponse.put("response_status", "error");
							}
							else
							{
								Session session = HibernateUtil.getSessionFactory().openSession();
								int indentval = (new Random()).nextInt(10);
								Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.getItem", "opening");
								Transaction tx = null; 
								try
								{
									 tx = session.beginTransaction();
									Item item = (Item)session.get(Item.class, Long.parseLong(id));
									if(item != null)
									{
										jsonresponse.put("response_status", "success");
										jsonresponse.put("item_jo", item.getJSON());
									}
									else
									{
										jsonresponse.put("response_status", "error");
										jsonresponse.put("message", "Item does not exist in the DB.");
									}
									tx.commit();
								}
								catch (Exception e) {
									if (tx!=null) tx.rollback();
									e.printStackTrace();
								}
								finally {
									Global.printThreadHeader(indentval, session.hashCode(), "Endpoint.getItem", "closing");
									session.close();
								}		
							}
						}
					}
					catch(JSONException jsone)
					{
						out.println("{ \"response_status\": \"error\", \"message\": \"JSONException caught in Endpoint GET non-auth methods. method=" + method + "\"}");
						System.err.println("endpoint: JSONException thrown in Endpoint GET non-auth methods. " + jsone.getMessage());
						jsone.printStackTrace();
						return;
					}	
			}
			 /***
			  *    ___  ___ _____ _____ _   _ ___________  _____  ______ _____ _____     _   _ _____ ___________    ___  _   _ _____ _   _ 
			  *    |  \/  ||  ___|_   _| | | |  _  |  _  \/  ___| | ___ \  ___|  _  |   | | | /  ___|  ___| ___ \  / _ \| | | |_   _| | | |
			  *    | .  . || |__   | | | |_| | | | | | | |\ `--.  | |_/ / |__ | | | |   | | | \ `--.| |__ | |_/ / / /_\ \ | | | | | | |_| |
			  *    | |\/| ||  __|  | | |  _  | | | | | | | `--. \ |    /|  __|| | | |   | | | |`--. \  __||    /  |  _  | | | | | | |  _  |
			  *    | |  | || |___  | | | | | \ \_/ / |/ / /\__/ / | |\ \| |___\ \/' /_  | |_| /\__/ / |___| |\ \  | | | | |_| | | | | | | |
			  *    \_|  |_/\____/  \_/ \_| |_/\___/|___/  \____/  \_| \_\____/ \_/\_(_)  \___/\____/\____/\_| \_| \_| |_/\___/  \_/ \_| |_/
			  *                                                                                                                            
			  *                                                                                                                            
			  */
			 else if (method.equals("getUserSelf") || method.equals("setUserPreference") ||
					 method.equals("followUser") || method.equals("unfollowUser") ||
					 method.equals("resetNotificationCount") || method.equals("deleteNotification") || method.equals("removeItemFromNotificationIds") || method.equals("getNotificationItem") ||
					 method.equals("resetNewsfeedCount") || method.equals("getChat") || method.equals("submitChatMessage"))
			 {
				 try
				 {
					 // for all of these methods, check email/this_access_token. Weak check first (to avoid database hits). Then check database.
					 String screenname = request.getParameter("screenname"); // the requester's email
					 String this_access_token = request.getParameter("this_access_token"); // the requester's auth
					 if(!(screenname == null || screenname.isEmpty()) && !(this_access_token == null || this_access_token.isEmpty())) 
					 {
							// both weren't null or empty
							// if only email is null or empty respond with code "0000" to clear out the malformed credentials
							if(!(screenname == null || screenname.isEmpty()))
							{
								// otherwise, continue to user retrieval
								Session session = HibernateUtil.getSessionFactory().openSession();
								int indentval = (new Random()).nextInt(10);
								Global.printThreadHeader(indentval, session.hashCode(), "Endpoint." + method, "opening");
								Transaction tx = null; 
								try
								{
									tx = session.beginTransaction();
									User useritem = (User)session.get(User.class, screenname);
									if(useritem != null)
									{	
										if(useritem.isValid(this_access_token)) 
										{	
											if (method.equals("getUserSelf")) // I think this might be redundant (or maybe the one below is)
											{
												boolean something_needs_updating = false;
												
												// check ext version as reported by user.
												String ext_version = request.getParameter("ext_version");
												if(ext_version != null && !ext_version.isEmpty()) // covering the bases
												{
													if(ext_version.length() == 5 && Global.isWholeNumeric(ext_version.substring(0,1)) && ext_version.substring(1,2).equals(".") && Global.isWholeNumeric(ext_version.substring(2,5))) // is of the form "X.YYY"
													{
														if(useritem.getExtVersion() == null || !useritem.getExtVersion().equals(ext_version)) // if the existing value is null, or the verions don't match, update
														{
															useritem.setExtVersion(ext_version);
															something_needs_updating = true;
														}
													}
												}
												JSONObject user_jo = null;
												long now = System.currentTimeMillis();
												
												if(now - useritem.getSeen() > 600000) // if it's been > 10 mins since last "seen" update, update it
												{
													something_needs_updating = true;
													useritem.setSeen(now);
													SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
													sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
													useritem.setSeenHumanReadable(sdf.format(timestamp_at_entry));
													
													// Also update karma here, although FirebaseListener should be keeping track of all changes.
													// so this is not entirely necessary. It's only necessary if FBL isn't doing its job (i.e. missing karma changes)
													try{
														String result = Jsoup
															 .connect("https://hacker-news.firebaseio.com/v0/user/" + screenname  + ".json")
															 .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
															 .ignoreContentType(true).execute().body();
														JSONObject hn_user_jo = new JSONObject(result);
														if(hn_user_jo.has("karma"))
															useritem.setHNKarma(hn_user_jo.getLong("karma"));
													}
													catch(IOException ioe){
														//
													}
													catch(JSONException jsone){
														//
													}									
												}
												
												// check if there is lingering karma in the user's karma pool and report it, if so
												if(useritem.getKarmaPool() != 0L && (useritem.getLastKarmaPoolDrain() < (System.currentTimeMillis() - (useritem.getKarmaPoolTTLMins()*60000)))) 
												{
													  long change = useritem.getKarmaPool();
													  System.out.print("*** Endpoint.getUserSelf() found lingering karma in pool outside of karma pooling ttl. Reporting change " + change + " for " + useritem.getId());
													  FirebaseChangeProcessor fcp = new FirebaseChangeProcessor(null);
													  if(change > 0L)
													  {
														  System.out.println("\tcalling createNotification for positive change.");
														  fcp.createNotification(useritem, "1", 0L, System.currentTimeMillis(), null, change, session); // feedable event 1, positive karma change
													  }
													  else if(change < 0L)				
													  {
														  System.out.println("\tcalling createNotification for negative change.");
														  fcp.createNotification(useritem, "2", 0L, System.currentTimeMillis(), null, change, session); // feedable event 2, negative karma change
													  }
													  // regardless, empty the pool and set new timestamp
													  useritem.setHNKarma(useritem.getHNKarma() + change);
													  useritem.setKarmaPool(0L);
													  useritem.setLastKarmaPoolDrain(System.currentTimeMillis());
													  something_needs_updating = true;
												}

												if(something_needs_updating)
													session.save(useritem);
												
												user_jo = useritem.getJSON();
												
												Globalvar gvi = (Globalvar)session.get(Globalvar.class, "latest_ext_version");
												if(gvi != null)
													jsonresponse.put("latest_ext_version", gvi.getStringValue());
												jsonresponse.put("response_status", "success");
												jsonresponse.put("user_jo", user_jo);	
											}
											else if (method.equals("setUserPreference")) // email, this_access_token, target_email (of user to get) // also an admin method
											{
													 String which = request.getParameter("which");
													 String value = request.getParameter("value");
													 if(which == null || value == null)
													 {
														 jsonresponse.put("message", "Invalid parameters.");
														 jsonresponse.put("response_status", "error");
													 }
													 else
													 {	 
														 System.out.println("Endpoint setUserPreference() begin: which=" + which + " and value=" + value);
														 jsonresponse.put("response_status", "success"); // default to success, then overwrite with error if necessary
														 if(which.equals("url_checking_mode")) 
														 {
															 if(value.equals("notifications_only"))
																 useritem.setURLCheckingMode("notifications_only");
															 else // this is an error, default to 450
																 useritem.setURLCheckingMode("stealth");
															 session.save(useritem);
															 jsonresponse.put("response_status", "success"); 
														 }
														 else if(which.equals("notification_mode")) 
														 {
															 if(value.equals("notifications_only"))
																 useritem.setNotificationMode("notifications_only");
															 else if(value.equals("newsfeed_and_notifications"))
																 useritem.setNotificationMode("newsfeed_and_notifications");
															 session.save(useritem);
															 jsonresponse.put("response_status", "success"); 
														 }
														 else if(which.equals("karma_pool_ttl")) 
														 {
															 if(!Global.isWholeNumeric(value))
															 {
																 jsonresponse.put("message", "Must be an int between 5 and 1440.");
																 jsonresponse.put("response_status", "error");
															 }
															 else
															 {
																 long val = Long.parseLong(value);
																 if(val > 1440L || val < 5L)
																 {
																	 jsonresponse.put("message", "Must be an int between 5 and 1440.");
																	 jsonresponse.put("response_status", "error");
																 }
																 else
																 {
																	 useritem.setKarmaPoolTTLMins(val);
																	 session.save(useritem);
																	 jsonresponse.put("response_status", "success"); 
																 }
															 }
														 }
														 else if(which.equals("hide_embedded_counts") || which.equals("hide_inline_follow") || which.equals("hide_deep_reply_notifications") || which.equals("hide_promo_links")) 
														 {
															 if(value.equals("show") || value.equals("hide"))
															 {
																 if(which.equals("hide_embedded_counts"))
																 {
																	 if(value.equals("show"))
																		 useritem.setHideEmbeddedCounts(false);
																	 else
																		 useritem.setHideEmbeddedCounts(true);
																 }
																 
																 else if(which.equals("hide_inline_follow"))
																 {
																	 if(value.equals("show"))
																		 useritem.setHideInlineFollow(false);
																	 else
																		 useritem.setHideInlineFollow(true);
																 }
																 else if(which.equals("hide_deep_reply_notifications"))
																 {
																	 if(value.equals("show"))
																		 useritem.setHideDeepReplyNotifications(false);
																	 else
																		 useritem.setHideDeepReplyNotifications(true);
																 }
																 else if(which.equals("hide_promo_links"))
																 {
																	 if(value.equals("show"))
																		 useritem.setHidePromoLinks(false);
																	 else
																		 useritem.setHidePromoLinks(true);
																 }
																 session.save(useritem);
															 }
															 else
															 {
																 jsonresponse.put("message", "Must be \"show\" or \"hide\".");
																 jsonresponse.put("response_status", "error");
															 }
														 }
														 else
														 {
															 jsonresponse.put("message", "Invalid which value.");
															 jsonresponse.put("response_status", "error");
														 }
													 }
											}
											else if (method.equals("resetNotificationCount"))
											{
													 //System.out.println("Endpoint resetNotificationCount() begin);
													 useritem.setNotificationCount(0L);
													 session.save(useritem);
													 jsonresponse.put("message", "Notification count successfully reset."); 
													 jsonresponse.put("response_status", "success");
													//System.out.println("Endpoint resetNotificationCount() end);
											}
											else if (method.equals("resetNewsfeedCount"))
											{
													 //System.out.println("Endpoint resetNewsfeedCount() begin);
													 useritem.setNewsfeedCount(0L);
													 session.save(useritem);
													 jsonresponse.put("message", "Newsfeed count successfully reset."); 
													 jsonresponse.put("response_status", "success");
													//System.out.println("Endpoint resetNewsfeedCount() end);
											}
											else if (method.equals("deleteNotification"))
											{
													 String notification_id = request.getParameter("notification_id");
													 if(notification_id == null || notification_id.isEmpty())
													 {
														 jsonresponse.put("message", "notification_id value was null or empty");
														 jsonresponse.put("response_status", "error");
													 }
													 else
													 {
														 Notification ni = (Notification)session.get(Notification.class, notification_id);
														 if(ni != null)
														 {
															 if(ni.getUserId().equals(useritem.getId()))
															 {
																 session.delete(ni);
																 
																 // also remove from user's notification set
																 Set<String> notificationset = useritem.getNotificationIds();
																 if(notificationset != null)
																 {
																	 notificationset.remove(request.getParameter("id"));
																	 if(notificationset.isEmpty())
																		 notificationset = null;
																	 useritem.setNotificationIds(notificationset);
																	 session.save(useritem);
																 }
																 // else the notification set was already null, so it wasn't there to begin with
																 jsonresponse.put("response_status", "success");
															 }
															 else
															 {
																 jsonresponse.put("message", "You don't own that notification item.");
																 jsonresponse.put("response_status", "error");
															 }
														 }
														 else
														 {
															 jsonresponse.put("response_status", "success"); // if not found, it was never there to begin with, so return success as it is definitely gone
														 }
													 }
											}
											else if (method.equals("removeItemFromNotificationIds"))
											{
													 System.out.println("Endpoint.removeItemFromNotificationIds() begin");
													 String notification_id = request.getParameter("notification_id");
													 if(notification_id == null || notification_id.isEmpty())
													 {
														 jsonresponse.put("message", "notification_id value was null or empty");
														 jsonresponse.put("response_status", "error");
													 }
													 else
													 {
														 Set<String> notificationset = useritem.getNotificationIds();
														 if(notificationset != null)
														 {
															 System.out.println("notificationset was not null, size=" + notificationset.size() + " removing "+ notification_id);
															 Iterator<String> it = notificationset.iterator();
															 while(it.hasNext())
															 {
																 System.out.println(it.next());
															 }
															 boolean successful = notificationset.remove(notification_id);
															 System.out.println("successful=" + successful);
															 if(notificationset.isEmpty())
																 notificationset = null;
															 useritem.setNotificationIds(notificationset);
															 session.save(useritem);
														 }
														 // else notification set was already null, no need to return an error.
														 jsonresponse.put("response_status", "success");
													 }
													 System.out.println("Endpoint.removeItemFromNotificationIds() end");
											}
											else if (method.equals("getNotificationItem"))
											{
													 //System.out.println("Endpoint.getNotificationItem() begin");
													 String notification_id = request.getParameter("notification_id");
													 if(notification_id == null)
													 {
														 jsonresponse.put("message", "This method requires a notification_id value != null");
														 jsonresponse.put("response_status", "error"); 
													 }
													 else if(notification_id.isEmpty())
													 {
														 jsonresponse.put("message", "This method requires a non-empty notification_id value");
														 jsonresponse.put("response_status", "error"); 
													 }
													 else
													 {
														 Notification ai = (Notification)session.get(Notification.class, notification_id);
														 if(ai == null)
														 {
															 // No notification with the specified ID exists. 
															 // If that id is in the user's notification or newsfeed sets, remove it and update user.
															 boolean saveuser = false;
															 Set<String> notificationset = useritem.getNotificationIds();
															 if(notificationset != null && notificationset.contains(notification_id))
															 {
																 notificationset.remove(notification_id);
																 if(notificationset.isEmpty())
																	 notificationset = null;
																 useritem.setNotificationIds(notificationset);
																 saveuser = true;
															 }
															 Set<String> newsfeedset = useritem.getNewsfeedIds();
															 if(newsfeedset != null && newsfeedset.contains(notification_id))
															 {
																 newsfeedset.remove(notification_id);
																 if(newsfeedset.isEmpty())
																	 newsfeedset = null;
																 useritem.setNewsfeedIds(newsfeedset);
																 saveuser = true;
															 }
															 if(saveuser)
																 session.save(useritem);
															 jsonresponse.put("message", "No notification with that ID exists.");
															 jsonresponse.put("response_status", "error"); 
														 }
														 else 
														 {
															 if(!ai.getUserId().equals(screenname))
															 {
																 jsonresponse.put("message", "Permission denied. You're not the owner of this notification.");
																 jsonresponse.put("response_status", "error"); 
															 }
															 else
															 {
																 jsonresponse.put("response_status", "success");
																 jsonresponse.put("notification_jo", ai.getJSON());
															 }
														 }
													 }
													 //System.out.println("Endpoint.getNotificationItem() end");
											}
											else if (method.equals("followUser"))
											{
												System.out.println("Endpoint.followUser() begin");
													 String target_screenname = request.getParameter("target_screenname");
													 if(target_screenname == null)
													 {
														 System.out.println("Endpoint.followUser() 0");
														 jsonresponse.put("message", "This method requires a target_screenname value != null");
														 jsonresponse.put("response_status", "error"); 
													 }
													 else if(target_screenname.isEmpty())
													 {
														 System.out.println("Endpoint.followUser() 1");
														 jsonresponse.put("message", "This method requires a non-empty target_screenname value");
														 jsonresponse.put("response_status", "error"); 
													 }
													 else if(target_screenname.equals(screenname))
													 {
														 System.out.println("Endpoint.followUser() 2");
														 jsonresponse.put("message", "You can't follow yourself.");
														 jsonresponse.put("response_status", "error"); 
													 }
													 else if(useritem.getFollowing() != null && useritem.getFollowing().contains(target_screenname))
													 {
														 System.out.println("Endpoint.followUser() 3");
														 jsonresponse.put("message", "You are already following that user.");
														 jsonresponse.put("response_status", "error"); 
													 }
													 else
													 {
														 System.out.println("Endpoint.followUser() 4");
														 User target_useritem = getUserInDBCreatingIfNotAndFoundWithinHNAPI(target_screenname, session);
														 if(target_useritem != null)
														 { 
															 System.out.println("Endpoint.followUser() 5");
															 System.out.println(useritem.getId() + " has chosen to follow " + target_useritem.getId());
															 // create "someone followed you" notification item and add to the user being followed, but only if he/she 
															 // (a) is registered and (b) has not already notified of this follow
															 if(target_useritem.getRegistered()) // (a) registered
															 {	
																 System.out.println(target_useritem.getId() + " was found in the DB and is registered with Hackbook.");
																 boolean already_notified = false;
																 Set<String> notification_item_ts = (Set<String>)target_useritem.getNotificationIds();
																 if(notification_item_ts == null || notification_item_ts.isEmpty())
																	 already_notified = false;
																 else
																 {	 
																	 Iterator<String> it = notification_item_ts.iterator();
																	 Notification ni = null;
																	 while(it.hasNext())
																	 {
																		 ni = (Notification)session.get(Notification.class, it.next());
																		 if(ni.getType().equals("0") && ni.getTriggerer().equals(useritem.getId()))
																		 {
																			 System.out.println("***" + target_useritem.getId() + " has already been notified that " + useritem.getId() + " is following them!");
																			 already_notified = true;
																			 break;
																		 }
																	 }
																 }
																 
																 if(!already_notified) // (b) not already notified
																 { 
																	 long now = System.currentTimeMillis();
																	 String now_str = Global.fromDecimalToBase62(7,now);
																	 Random generator = new Random(); 
																	 int r = generator.nextInt(238327); // this will produce numbers that can be represented by 3 base62 digits
																	 String randompart_str = Global.fromDecimalToBase62(3,r);
																	 String notification_id = now_str + randompart_str + "0"; 
																		
																	 Notification ai = new Notification(); 
																	 ai.setId(notification_id);
																	 ai.setActionMSFE(now);
																	 ai.setMSFE(now);
																	 ai.setUserId(target_useritem.getId());
																	 ai.setType("0");
																	 //ai.setHNTargetId(null);
																	 ai.setTriggerer(useritem.getId());
																	 //ai.setHNRootStoryId();
																	 //ai.setHNRootCommentId();
																	 session.save(ai);
																	 System.out.println("notification item " + notification_id + " has been saved in the db.");
																	 
																	 TreeSet<String> notificationset = new TreeSet<String>();
																	 if(target_useritem.getNotificationIds() != null)
																		 notificationset.addAll(target_useritem.getNotificationIds());
																	 notificationset.add(notification_id);
																	 while(notificationset.size() > Global.NOTIFICATIONS_SIZE_LIMIT)
																    		notificationset.remove(notificationset.first());
																	 target_useritem.setNotificationIds(notificationset);
																	 target_useritem.setNotificationCount(target_useritem.getNotificationCount()+1);
																 }
															 }
															 else
															 {
																 System.out.println(target_useritem.getId() + " was found in the DB but is NOT registered with Hackbook.");
															 }
															 
															 Set<String> followersset = target_useritem.getFollowers();
															 if(followersset == null)
																 followersset = new HashSet<String>();
															 followersset.add(useritem.getId()); // add useritem to the target_useritem's followers list
															 target_useritem.setFollowers(followersset);
															 session.save(target_useritem);
															 System.out.println(target_useritem.getId() + " has been saved with new followers list and notification id (if registered)");
															 
															 Set<String> followingset = useritem.getFollowing();
															 if(followingset == null)
																 followingset = new HashSet<String>();
															 followingset.add(target_useritem.getId()); // add target_useritem to the useritem's following list
															 useritem.setFollowing(followingset);
															 session.save(useritem);
															 System.out.println(useritem.getId() + " has been saved with a new following list");
															 
															 NewFollowNewsfeedAdjuster nfnfa = new NewFollowNewsfeedAdjuster(useritem.getId(), target_useritem.getId());
															 System.out.println("Doing it ASYNCHRONOUSLY");
															 nfnfa.start(); // ASYNCHRONOUSLY put new feed items (triggered by target_useritem) into useritem's feed

															 jsonresponse.put("response_status", "success");
														 }
														 else
														 {
															 System.out.println("Endpoint.followUser() 6");
															 jsonresponse.put("message", "Invalid user.");
															 jsonresponse.put("response_status", "error");  
														 }
													 }
												System.out.println("Endpoint.followUser() end");
											}
											else if (method.equals("unfollowUser"))
											{
													 System.out.println("Endpoint.unfollowUser() begin");
													 String target_screenname = request.getParameter("target_screenname");
													 User target_useritem = (User)session.get(User.class, target_screenname);
													 if(target_useritem == null)
													 {
														 jsonresponse.put("message", "Can't unfollow user bc they don't exist in the DB.");
														 jsonresponse.put("response_status", "error");
													 }
													 else if(target_screenname.isEmpty())
													 {
														 jsonresponse.put("message", "This method requires a non-empty target_screenname value");
														 jsonresponse.put("response_status", "error"); 
													 }
													 else if(target_screenname.equals(screenname))
													 {
														 jsonresponse.put("message", "You can't unfollow yourself.");
														 jsonresponse.put("response_status", "error"); 
													 }
													 else
													 {
														 System.out.println("inc values ok");
														 Set<String> followingset = useritem.getFollowing();
														 if(followingset == null || !followingset.contains(target_screenname))
														 {
															 jsonresponse.put("message", "You aren't following that user.");
															 jsonresponse.put("response_status", "error"); 
														 }
														 else
														 {
															
															 System.out.println("You are following that user, so it can be removed right now.");
															 followingset.remove(target_useritem.getId());
															 if(followingset.isEmpty())
																 followingset = null;
															 useritem.setFollowing(followingset);
															 
															 // Go through the user's newsfeed (the user doing the unfollowing), and remove any items by the user they just unfollowed
															 Set<String> newsfeedset = useritem.getNewsfeedIds();
															 TreeSet<String> new_newsfeedset = new TreeSet<String>();
															 if(newsfeedset != null && !newsfeedset.isEmpty())
															 {
																 Iterator<String> newsfeed_it = newsfeedset.iterator();
																 Notification ni = null;
																 while(newsfeed_it.hasNext())
																 {	 
																	 ni = (Notification)session.get(Notification.class, newsfeed_it.next());
																	 if(!ni.getTriggerer().equals(target_screenname))
																		 new_newsfeedset.add(ni.getId()); 
																 }
																 if(new_newsfeedset.isEmpty())
																	 new_newsfeedset = null;
																 useritem.setNewsfeedIds(new_newsfeedset);
																 
																 /*String table1Name = "hackbook_notifications2";
																 HashMap<String, KeysAndAttributes> requestItems = new HashMap<String, KeysAndAttributes>();
																 ArrayList<Map<String, AttributeValue>> keys1 = new ArrayList<Map<String, AttributeValue>>();
																 HashMap<String, AttributeValue> table1key1 = null;
																 while(newsfeed_it.hasNext())
																 {	 
																	 table1key1 = new HashMap<String, AttributeValue>();
																	 table1key1.put("id", new AttributeValue().withS(newsfeed_it.next()));
																	 keys1.add(table1key1);
																 }
																 requestItems.put(table1Name, new KeysAndAttributes().withKeys(keys1));    	
																 BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest().withRequestItems(requestItems);
																 BatchGetItemResult result = client.batchGetItem(batchGetItemRequest);
																 List<Map<String,AttributeValue>> table1Results = result.getResponses().get(table1Name);
																 System.out.println("Items in table " + table1Name);
																 //Notification ni = null;
																 for (Map<String,AttributeValue> item : table1Results) {
																	 if(!item.get("triggerer").getS().equals(target_screenname)) // only keep the ones that weren't triggered by the user that just got unfollowed
																	 {	 
																		 // don't have to actually create the Notification here, but leave this for cut paste later.
																		 // ni = new Notification();
																		 //ni.setId(item.get("id").getS());
																		 //ni.setUserId(item.get("user_id").getS());
																		 //ni.setType(item.get("type").getS());
																		 //ni.setTriggerer(item.get("triggerer").getS());
																		 //ni.setActionMSFE(Long.parseLong(item.get("action_msfe").getN()));
																		 //ni.setMSFE(Long.parseLong(item.get("msfe").getN()));
																		 //ni.setHNTargetId(Long.parseLong(item.get("hn_target_id").getN()));
																		 //ni.setHNRootStoryId(Long.parseLong(item.get("hn_root_story_id").getN()));
																		 //ni.setHNRootCommentId(Long.parseLong(item.get("hn_root_comment_id").getN()));
																		 //ni.setKarmaChange(Integer.parseInt(item.get("karma_change").getN()));
																		 new_newsfeedset.add(item.get("id").getS());
																	 }
																 }
																 if(new_newsfeedset.isEmpty())
																	 new_newsfeedset = null;
																 useritem.setNewsfeedIds(new_newsfeedset);*/
															 }
															 session.save(useritem);
															 // 1. we don't have to check NotificationIds because this is an unfollow which only affects newsfeed items.
															 // 2. We don't need to check newsfeed size limit because we're unfollowing. At worst, the size stays the same.
															
															 // remove useritem from target_useritem's followers set
															 Set<String> followersset = target_useritem.getFollowers();
															 if(followersset != null)
															 {
																 followersset.remove(useritem.getId());
																 if(followersset.isEmpty())
																	 followersset = null;
																 target_useritem.setFollowers(followersset);
																 session.save(target_useritem);
															 }
															 // don't need to remove anything from notifications because notifications are based on 
															 // stuff that is done to the user, not who the user is following
															 jsonresponse.put("response_status", "success");
														 }
													 }
													 System.out.println("Endpoint.unfollowUser() end");
											}
											else if (method.equals("submitChatMessage"))
											{
													 String message = request.getParameter("message");
													 if(message != null && message.isEmpty())
													 {
														 jsonresponse.put("message", "Message was empty.");
														 jsonresponse.put("response_status", "error"); 
													 }
													 else
													 {
														 Chat ci = new Chat();
														 long now0 = System.currentTimeMillis();
														 String now_str = Global.fromDecimalToBase62(7,now0);
														 Random generator = new Random(); 
														 int r = generator.nextInt(238327); // this will produce numbers that can be represented by 3 base62 digits
														 String randompart_str = Global.fromDecimalToBase62(3,r);
														 String message_id = now_str + randompart_str;
														 ci.setId(message_id);
														 ci.setUserId(useritem.getId());
														 ci.setHostname("news.ycombinator.com");
														 ci.setMSFE(now0);
														 ci.setText(message);
														 session.save(ci);
														 jsonresponse.put("response_status", "success");
														 
														 // if there is valid chat, attach it to the response
														 HashSet<Chat> chat = getChat(session); // 3 days in minutes
														 if(!(chat == null || chat.isEmpty()))
														 { 
															 Iterator<Chat> chat_it = chat.iterator();
															 Chat currentitem = null;
															 JSONArray chat_ja = new JSONArray();
															 while(chat_it.hasNext())
															 {
																 currentitem = chat_it.next();
																 chat_ja.put(currentitem.getJSON());
															 }
															 jsonresponse.put("chat_ja", chat_ja);
														 }
														 
														 Pattern pattern = Pattern.compile(".*@[A-Za-z_\\-].*");
														 Matcher matcher = pattern.matcher(message);
														 
														 if(matcher.matches())
														 {
															 System.out.println("Chat message from " + useritem.getId() + " contains an @ followed by a [A-Za-z\\-_] char.");
															 TreeSet<User> mentioned_registered_users = getMentionedUsers(message, session);
															 Iterator<User> mentionedusers_it = mentioned_registered_users.iterator();
															 User mentioneduser = null;
															 long now1 = 0L;
															 while(mentionedusers_it.hasNext())
															 {
																 mentioneduser = mentionedusers_it.next();
																 now1 = System.currentTimeMillis();
																 now_str = Global.fromDecimalToBase62(7,now1);
																 r = generator.nextInt(238327); // this will produce numbers that can be represented by 3 base62 digits
																 randompart_str = Global.fromDecimalToBase62(3,r);
																 String notification_id = now_str + randompart_str + "C"; 
																	
																 Notification ai = new Notification(); 
																 ai.setId(notification_id);
																 ai.setActionMSFE(now0); // the chat item got added slightly before this notification item.
																 ai.setMSFE(now1);
																 ai.setUserId(mentioneduser.getId());
																 ai.setType("C");
																 //ai.setHNTargetId(null);
																 ai.setTriggerer(useritem.getId());
																 //ai.setHNRootStoryId();
																 //ai.setHNRootCommentId();
																 session.save(ai);
																 
																 TreeSet<String> notificationset = new TreeSet<String>();
																 if(mentioneduser.getNotificationIds() != null)
																	 notificationset.addAll(mentioneduser.getNotificationIds());
																 notificationset.add(notification_id);
																 while(notificationset.size() > Global.NOTIFICATIONS_SIZE_LIMIT)
															    		notificationset.remove(notificationset.first());
																 mentioneduser.setNotificationIds(notificationset);
																 mentioneduser.setNotificationCount(mentioneduser.getNotificationCount()+1);
																 session.save(mentioneduser);
															 }
														 }
													 }
											}
											else if (method.equals("getChat"))
											{
												HashSet<Chat> chat = getChat(session); // 3 days in minutes
												if(chat == null || chat.isEmpty())
												{
													jsonresponse.put("response_status", "success");
													// successful... just empty. don't put a chat_ja object
												}
												else
												{ 
													Iterator<Chat> chat_it = chat.iterator();
													Chat currentitem = null;
													JSONArray chat_ja = new JSONArray();
													while(chat_it.hasNext())
													{
														currentitem = chat_it.next();
														chat_ja.put(currentitem.getJSON());
													}
													jsonresponse.put("response_status", "success");
													if(chat_ja.length() > 0)
														jsonresponse.put("chat_ja", chat_ja);
												}
											}
										}
										else // user had an screenname and this_access_token, but they were not valid. Let the frontend know to get rid of them
										{
											System.out.println("Endpoint: screenname (" + screenname + ") + access token present, but not valid. method=" + method);
											jsonresponse.put("response_status", "error");
											jsonresponse.put("message", "screenname + access token present, but not valid. Please try again.");
											jsonresponse.put("error_code", "0000");
										}
											
									}
									else // couldn't get useritem from provided screenname
									{
										System.out.println("Endpoint: No user was found for that screenname (" + screenname + "). method=" + method);
										jsonresponse.put("response_status", "error");
										jsonresponse.put("message", "No user was found for that screenname. Please try again.");
										jsonresponse.put("error_code", "0000");
									}
									tx.commit();	
								}
								catch (Exception e) {
									if (tx!=null) tx.rollback();
									e.printStackTrace();
								}
								finally {
									Global.printThreadHeader(indentval, session.hashCode(), "Endpoint." + method, "closing");
									session.close();
								}		
						 	}
						 	else // either screenname or tat was null, but not both
						 	{
						 		System.out.println("Endpoint: screenname or access token was null. method=" + method);
						 		jsonresponse.put("response_status", "error");
						 		jsonresponse.put("message", "screenname or access token was null. Please try again.");
						 		jsonresponse.put("error_code", "0000");
						 	}
					 }	
					 else // email and tat were both null
					 {
						 System.out.println("Endpoint: screenname and access token were both null. method=" + method);
						 jsonresponse.put("response_status", "error");
						 jsonresponse.put("message", "You must be logged in to do that.");
					 }
				 }
				 catch(JSONException jsone)
				 {
					 out.println("{ \"response_status\": \"error\", \"message\": \"JSONException caught in Endpoint GET methods requiring user auth.\"}");
					 System.err.println("endpoint: JSONException thrown in Endpoint GET methods requiring user auth. " + jsone.getMessage());
					 jsone.printStackTrace();
					 return;
				 }	
			 }
			 else
			 {
				 try
				 {
					 System.out.println("Endpoint: Unsupported method. method=" + method);
					 jsonresponse.put("response_status", "error");
					 jsonresponse.put("message", "Unsupported method. method=" + method);
				 }
				 catch(JSONException jsone)
				 {
					 out.println("{ \"response_status\": \"error\", \"message\": \"JSONException caught in Endpoint GET unsupported method response generation.\"}");
					 System.err.println("endpoint: JSONException thrown in Endpoint GET unsupported method response generation. " + jsone.getMessage());
					 jsone.printStackTrace();
					 return;
				 }	
			 }
		}
		long timestamp_at_exit = System.currentTimeMillis();
		long elapsed = timestamp_at_exit - timestamp_at_entry;
		try{
			jsonresponse.put("elapsed", elapsed);
			jsonresponse.put("msfe", timestamp_at_exit);
			if(method != null)
				jsonresponse.put("method", method);
		}
		catch(JSONException jsone)
		{
			out.println("{ \"response_status\": \"error\", \"message\": \"JSONException caught in Endpoint GET elapsed and msfe generation.\"}");
			System.err.println("endpoint: JSONException thrown in Endpoint GET elapsed and msfe generation. " + jsone.getMessage());
			jsone.printStackTrace();
			return;
		}	
		if(true)//Global.devel == true) // getUserSelf is too noisy, so ignore it
			System.out.println("response=" + jsonresponse);	// respond with object, success response, or error 
		out.println(jsonresponse);	
		return; 	
	}

	// can't create new session here because we need the returned User to stay alive back (and connected to the DB) in the calling function.
	public User getUserInDBCreatingIfNotAndFoundWithinHNAPI(String target_screenname, Session session) 
	{
		 User target_useritem = (User)session.get(User.class, target_screenname);
		 if (target_useritem == null) // not in local db, try hacker news API
		 {
			 try 
			 {
				 Response r = Jsoup
						 .connect("https://hacker-news.firebaseio.com/v0/user/" + target_screenname + ".json")
						 .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.104 Safari/537.36")
						 .ignoreContentType(true).execute();
				 // if r failed, we skip to the catch rather than creating a user
				 String result = r.body();
				 if (result == null || result.trim().isEmpty() || result.trim().equals("null")) 
				 {
					 return null;
				 } 
				 else 
				 {
					 target_useritem = new User();
					 JSONObject profile_jo = new JSONObject(result);
					 target_useritem.setHNKarma(profile_jo.getLong("karma"));
					 target_useritem.setHNSince(profile_jo.getLong("created"));
					 target_useritem.setId(profile_jo.getString("id"));
					 target_useritem.setRegistered(false); 
					 target_useritem.setHideEmbeddedCounts(true);
					 target_useritem.setURLCheckingMode("stealth");
					 if (profile_jo.has("about"))
						 target_useritem.setHNAbout(profile_jo.getString("about"));
					 else
						 target_useritem.setHNAbout("");
					 session.save(target_useritem);
				 }
			 } 
			 catch (IOException ioe) 
			 {
				 target_useritem = null;
			 } 
			 catch (JSONException jsone) 
			 {
				 target_useritem = null;
			 }
		 } 
		 return target_useritem;
	}
	
	// as of right now, chat items have no elementcollections, and therefore a continuing DB connection is not necessary for them. So it's safe to create a session and return a HashSet<Chat> here.
	private HashSet<Chat> getChat(Session session) 
	{
		long now = System.currentTimeMillis();
		long then = now - 604800000; // 7 days
		HashSet<Chat> returnset = null;
		String hql = "FROM Chat C WHERE C.hostname='news.ycombinator.com' AND C.msfe > " + then + " AND C.msfe < " + now;
		Query query = session.createQuery(hql);
		@SuppressWarnings("unchecked")
		List<Chat> chatitems = query.list();
		 
		if (chatitems != null && chatitems.size() > 0) {
			returnset = new HashSet<Chat>();
			for (Chat chatitem : chatitems) {
				returnset.add(chatitem);
			}
		} else {
			returnset = null;
		}
		return returnset;
	}
	
	public boolean isValidTopcolor(String color)
	{
		// 3color "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
		Pattern pattern = Pattern.compile("^[A-Fa-f0-9]{6}$");
		Matcher matcher = pattern.matcher(color);
		return matcher.matches();
	}

	public TreeSet<User> getMentionedUsers(String text, Session session)
	{
		TreeSet<User> mentioned_users = null;
		 if(text.indexOf("@") != -1) // there might be an @ mention here
		 {
			// screenname must be 2-15 characters, all letters and numbers
			 TreeSet<String> possiblematches = new TreeSet<String>();
				
			 Pattern p_withtrailer = Pattern.compile("[@]([\\w-]){2,15}"); // match for mentions preceeded by a space where trailed by an acceptable char
			 Matcher matcher1 = p_withtrailer.matcher(text); 
			 while (matcher1.find()) {
				 possiblematches.add(matcher1.group().substring(1,matcher1.group().length()));
			 }
				
			 System.out.println(possiblematches.size());
				
			 if(possiblematches.size() > 0)
			 {
				 Iterator<String> it = possiblematches.iterator();
				 String currentmatch = null;
				 while(it.hasNext())
				 {
					 currentmatch = it.next();
					 User mentioned_user = (User)session.get(User.class, currentmatch);
					 if(mentioned_user != null && mentioned_user.getRegistered())
					 {
						 if(mentioned_users == null)
							 mentioned_users = new TreeSet<User>();
						 mentioned_users.add(mentioned_user);
					 }
					 else
					 {
						 //System.out.println(" ... not found. Dud.");
					 }
				 }
			 }
			 else
			 {
				 //System.out.print("Possible mentions == 0");
			 }
		 }
		 return mentioned_users;
	}
}