package club.hackbook.domain;

import java.beans.Transient;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONException;
import org.json.JSONObject;

@Entity
@Table(name="USERS")
public class User implements java.lang.Comparable<User> {

	@Id
	private String id;
	
	@Lob
	@Column(columnDefinition="LONGTEXT")
	private String hn_about;
	private String hn_topcolor;
	private String url_checking_mode;
	private String notification_mode;
	private String hn_authtoken;
	private String since_hr;
	private String seen_hr;
	private String this_access_token;
	private String ext_version;
	
	@ElementCollection
	@CollectionTable(uniqueConstraints = {@UniqueConstraint(columnNames={"User_id", "notification_ids"})})
	private Set<String> notification_ids;
	@ElementCollection
	@CollectionTable(uniqueConstraints = {@UniqueConstraint(columnNames={"User_id", "newsfeed_ids"})})
	private Set<String> newsfeed_ids;
	@ElementCollection
	@CollectionTable(uniqueConstraints = {@UniqueConstraint(columnNames={"User_id", "followers"})})
	private Set<String> followers;
	@ElementCollection
	@CollectionTable(uniqueConstraints = {@UniqueConstraint(columnNames={"User_id", "following"})})
	private Set<String> following;
	
	private Long since = 0L;
	private Long seen = 0L;
	private Long notification_count = 0L;
	private Long newsfeed_count = 0L;
	private Long this_access_token_expires = 0L;
	private Long hn_karma = 0L;   // this is set on login and every 20 minutes by getUserSelf
	//private Long karma_pool = 0L;			// rather than produce a NotificationItem every 30 seconds, let's wait some period of time
	private Long last_karma_pool_drain = 0L;// pool the karma changes together, then unload it all at once.
	private Long karma_pool_ttl_mins = 0L;
	private Long hn_since = 0L;
	private Integer UTC_offset = -8;
	
	private boolean registered = false;
	private boolean hide_embedded_counts = true;
	private boolean hide_inline_follow = false;
	private boolean hide_deep_reply_notifications = false;
	private boolean hide_promo_links = false;
	
	/////////////////////////////
	
	public String getId() {return id; }
	public void setId(String id) { this.id = id; }

	public Long getSince() {return since; }
	public void setSince(Long since) { this.since = since; }
	
	public Long getHNSince() {return hn_since; }
	public void setHNSince(Long hn_since) { this.hn_since = hn_since; }
	
	public Long getSeen() {return seen; }
	public void setSeen(Long seen) { this.seen = seen; }
	
	public String getThisAccessToken() {return this_access_token; }
	public void setThisAccessToken(String this_access_token) { this.this_access_token = this_access_token; }
	
	public Long getThisAccessTokenExpires() {return this_access_token_expires; }
	public void setThisAccessTokenExpires(Long this_access_token_expires) { this.this_access_token_expires = this_access_token_expires; }

	public Long getNotificationCount() { return notification_count; }
	public void setNotificationCount(Long notification_count) { this.notification_count = notification_count; }
	
	public Set<String> getNotificationIds() { return notification_ids; }
	public void setNotificationIds(Set<String> notification_ids) { this.notification_ids = notification_ids; }
	
	public Long getNewsfeedCount() { return newsfeed_count; }
	public void setNewsfeedCount(Long newsfeed_count) { this.newsfeed_count = newsfeed_count; }
	
	public Set<String> getNewsfeedIds() { return  newsfeed_ids; }
	public void setNewsfeedIds(Set<String>  newsfeed_ids) { this. newsfeed_ids =  newsfeed_ids; }
	
	public String getSinceHumanReadable() {return since_hr; } // note this should not be used. Always format and return the msfe value instead.
	public void setSinceHumanReadable(String since_hr) { this.since_hr = since_hr; }
	
	public String getSeenHumanReadable() {return seen_hr; } // note this should not be used. Always format and return the msfe value instead.
	public void setSeenHumanReadable(String seen_hr) { this.seen_hr = seen_hr; }
		
	public Long getHNKarma() { return hn_karma; }
	public void setHNKarma(Long hn_karma) { this.hn_karma = hn_karma; }
		
	public String getURLCheckingMode() {return url_checking_mode; }  
	public void setURLCheckingMode(String url_checking_mode) { this.url_checking_mode = url_checking_mode; }
	
	public String getNotificationMode() {return notification_mode; }  
	public void setNotificationMode(String notification_mode) { this.notification_mode = notification_mode; }
	
	public String getHNAbout() {return hn_about; }  
	public void setHNAbout(String hn_about) { this.hn_about = hn_about; }
	
	public String getHNTopcolor() {return hn_topcolor; }  
	public void setHNTopcolor(String hn_topcolor) { this.hn_topcolor = hn_topcolor; }
	
	public String getHNAuthToken() {return hn_authtoken; }  
	public void setHNAuthToken(String hn_authtoken) { this.hn_authtoken = hn_authtoken; }
	
	public boolean getRegistered() {return registered; }  
	public void setRegistered(boolean registered) { this.registered = registered; }
	
	public Set<String> getFollowers() { return followers; }
	public void setFollowers(Set<String> followers) { this.followers = followers; }
	
	public Set<String> getFollowing() { return following; }
	public void setFollowing(Set<String> following) { this.following = following; }
		
	//public Long getKarmaPool() { return karma_pool; }
	//public void setKarmaPool(Long karma_pool) { this.karma_pool = karma_pool; }
	
	public Long getLastKarmaPoolDrain() {return last_karma_pool_drain; }
	public void setLastKarmaPoolDrain(Long last_karma_pool_drain) { this.last_karma_pool_drain = last_karma_pool_drain; }
	
	public Long getKarmaPoolTTLMins() { return karma_pool_ttl_mins; }
	public void setKarmaPoolTTLMins(Long karma_pool_ttl_mins) { this.karma_pool_ttl_mins = karma_pool_ttl_mins; }
	
	public boolean getHideEmbeddedCounts() {return hide_embedded_counts; }  
	public void setHideEmbeddedCounts(boolean hide_embedded_counts) { this.hide_embedded_counts = hide_embedded_counts; }
	
	public boolean getHideInlineFollow() {return hide_inline_follow; }  
	public void setHideInlineFollow(boolean hide_inline_follow) { this.hide_inline_follow = hide_inline_follow; }
	
	public boolean getHideDeepReplyNotifications() {return hide_deep_reply_notifications; }  
	public void setHideDeepReplyNotifications(boolean hide_deep_reply_notifications) { this.hide_deep_reply_notifications = hide_deep_reply_notifications; }
	
	public boolean getHidePromoLinks() {return hide_promo_links; }  
	public void setHidePromoLinks(boolean hide_promo_links) { this.hide_promo_links = hide_promo_links; }
	
	public String getExtVersion() {return ext_version; }  
	public void setExtVersion(String ext_version) { this.ext_version = ext_version; }
	
	public Integer getUTCOffset() {return UTC_offset; }
	public void setUTCOffset(Integer UTC_offset) { this.UTC_offset = UTC_offset; }
	
	public boolean isValid(String inc_this_access_token)
	{
		if(inc_this_access_token == null || getThisAccessToken() == null)
			return false;
		Long now = System.currentTimeMillis();
		if(getThisAccessToken().equals(inc_this_access_token) && getThisAccessTokenExpires() >= now)
			return true;
		else
			return false;
	}
	
	public JSONObject getJSON()
	{
		JSONObject user_jo = new JSONObject();
		try {
			user_jo.put("screenname", getId());
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z"); // return msfe values formatted like this. 
			sdf.setTimeZone(TimeZone.getTimeZone("America/New_York"));
			// so we can change the formatting to whatever we want here, regardless of what is in the (meaningless) "HumanReadable" columns in the database
			if(getURLCheckingMode() == null || getURLCheckingMode().isEmpty() || getURLCheckingMode().toLowerCase().equals("null"))
				user_jo.put("url_checking_mode", "stealth");
			else
				user_jo.put("url_checking_mode", getURLCheckingMode());
			if(getNotificationMode() == null || getNotificationMode().isEmpty() || getNotificationMode().toLowerCase().equals("null"))
				user_jo.put("notification_mode", "newsfeed_and_notifications");
			else
				user_jo.put("notification_mode", getNotificationMode());
			user_jo.put("since", getSince());
			user_jo.put("since_hr", sdf.format(getSince()));
			if(getHNTopcolor() == null || getHNTopcolor().isEmpty() || getHNTopcolor().toLowerCase().equals("null"))
				user_jo.put("hn_topcolor", "ff6600");
			else
				user_jo.put("hn_topcolor", getHNTopcolor());
			user_jo.put("hn_karma", getHNKarma());
			user_jo.put("hn_since", getHNSince());
			if(getKarmaPoolTTLMins() > 1440 || getKarmaPoolTTLMins() < 5)
				user_jo.put("karma_pool_ttl_mins", 10);
			else
				user_jo.put("karma_pool_ttl_mins", getKarmaPoolTTLMins());
			if(getFollowing() != null)
				user_jo.put("following", getFollowing());
			if(getFollowers() != null)
				user_jo.put("followers", getFollowers());
			user_jo.put("this_access_token", getThisAccessToken());
			user_jo.put("seen", sdf.format(getSeen()));
			user_jo.put("notification_count", getNotificationCount());
			user_jo.put("newsfeed_count", getNewsfeedCount());
			if(getNotificationIds() != null && !getNotificationIds().isEmpty())
				user_jo.put("notification_ids", getNotificationIds());
			if(getNewsfeedIds() != null && !getNewsfeedIds().isEmpty())
				user_jo.put("newsfeed_ids", getNewsfeedIds());
			user_jo.put("hide_embedded_counts", this.getHideEmbeddedCounts());
			user_jo.put("hide_inline_follow", this.getHideInlineFollow());	
			user_jo.put("hide_deep_reply_notifications", this.getHideDeepReplyNotifications());	
			user_jo.put("hide_promo_links", this.getHidePromoLinks());
			user_jo.put("UTC_offset", this.getUTCOffset());
			
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return user_jo;
	}
	
/*
	@DynamoDBIgnore
	public HashSet<NotificationItem> getNotificationItems(Long minutes_ago, DynamoDBMapper mapper, DynamoDBMapperConfig dynamo_config) { 
		// set up an expression to query screename#id
		DynamoDBQueryExpression<NotificationItem> queryExpression = new DynamoDBQueryExpression<NotificationItem>()
				.withIndexName("user_id-action_msfe-index")
				.withScanIndexForward(true)
				.withConsistentRead(false);
	        
		// set the user_id part
		NotificationItem key = new NotificationItem();
		key.setUserId(getId());
		queryExpression.setHashKeyValues(key);
		
		// set the msfe range part
		if(minutes_ago > 0)
		{
			//System.out.println("Getting comment children with a valid cutoff time.");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, (minutes_ago * -1));
			Long msfe_cutoff = cal.getTimeInMillis();
			// set the msfe range part
			Map<String, Condition> keyConditions = new HashMap<String, Condition>();
			keyConditions.put("action_msfe",new Condition()
			.withComparisonOperator(ComparisonOperator.GT)
			.withAttributeValueList(new AttributeValue().withN(new Long(msfe_cutoff).toString())));
			queryExpression.setRangeKeyConditions(keyConditions);
		}	
		// execute
		List<NotificationItem> notificationitems = mapper.query(NotificationItem.class, queryExpression, dynamo_config);
		if(notificationitems != null && notificationitems.size() > 0)
		{	
			HashSet<NotificationItem> returnset = new HashSet<NotificationItem>();
			char c = 'X';
			for (NotificationItem notificationitem : notificationitems) {
				c = notificationitem.getId().charAt(10);
				if(!(c == '7' || c == '8')) // all but 'a user you're following did X'
					returnset.add(notificationitem);
			}
			return returnset;
		}
		else
		{
			return null;
		}
	}
	
	
	@DynamoDBIgnore
	public HashSet<NotificationItem> getNewsfeedItems(Long minutes_ago, DynamoDBMapper mapper, DynamoDBMapperConfig dynamo_config) { 
		// set up an expression to query screename#id
		DynamoDBQueryExpression<NotificationItem> queryExpression = new DynamoDBQueryExpression<NotificationItem>()
				.withIndexName("user_id-action_msfe-index")
				.withScanIndexForward(true)
				.withConsistentRead(false);
	        
		// set the user_id part
		NotificationItem key = new NotificationItem();
		key.setUserId(getId());
		queryExpression.setHashKeyValues(key);
		
		// set the msfe range part
		if(minutes_ago > 0)
		{
			//System.out.println("Getting comment children with a valid cutoff time.");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, (minutes_ago * -1));
			Long msfe_cutoff = cal.getTimeInMillis();
			// set the msfe range part
			Map<String, Condition> keyConditions = new HashMap<String, Condition>();
			keyConditions.put("action_msfe",new Condition()
			.withComparisonOperator(ComparisonOperator.GT)
			.withAttributeValueList(new AttributeValue().withN(new Long(msfe_cutoff).toString())));
			queryExpression.setRangeKeyConditions(keyConditions);
		}	
		// execute
		List<NotificationItem> notificationitems = mapper.query(NotificationItem.class, queryExpression, dynamo_config);
		if(notificationitems != null && notificationitems.size() > 0)
		{	
			HashSet<NotificationItem> returnset = new HashSet<NotificationItem>();
			char c = 'X';
			for (NotificationItem notificationitem : notificationitems) {
				c = notificationitem.getId().charAt(10);
				if(c == '7' || c == '8')
					returnset.add(notificationitem);
			}
			return returnset;
		}
		else
		{
			return null;
		}
	}
	*/
	
	@Transient
	public HashSet<Item> getHNItemsByd(Long minutes_ago, Session session)  
	{ 
		long now = System.currentTimeMillis()/1000;
		long then = now - 604800; // 7 days in seconds
		String hql = "FROM Item I WHERE I.by='" + getId() + "' AND I.time > " + then + " AND I.time < " + now;
		Query query = session.createQuery(hql);
		@SuppressWarnings("unchecked")
		List<Item> items = query.list();
		
		if(items != null && items.size() > 0)
		{	
			HashSet<Item> returnset = new HashSet<Item>();
			for (Item item : items) {
				returnset.add(item);
			}
			return returnset;
		}
		else
		{
			return null;
		}
	}
	
	/*
	// this method does both notifications and newsfeed with one query rather than 2. Only useful if it's desirable that minutes_ago is the same for both.
	@DynamoDBIgnore
	public HashMap<String,HashSet<NotificationItem>> getNotificationAndNewsfeedItems(Long minutes_ago, DynamoDBMapper mapper, DynamoDBMapperConfig dynamo_config) { 
		// set up an expression to query screename#id
		DynamoDBQueryExpression<NotificationItem> queryExpression = new DynamoDBQueryExpression<NotificationItem>()
				.withIndexName("user_id-action_msfe-index")
				.withScanIndexForward(true)
				.withConsistentRead(false);
	        
		// set the user_id part
		NotificationItem key = new NotificationItem();
		key.setUserId(getId());
		queryExpression.setHashKeyValues(key);
		
		// set the msfe range part
		if(minutes_ago > 0)
		{
			//System.out.println("Getting comment children with a valid cutoff time.");
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, (minutes_ago * -1));
			Long msfe_cutoff = cal.getTimeInMillis();
			// set the msfe range part
			Map<String, Condition> keyConditions = new HashMap<String, Condition>();
			keyConditions.put("action_msfe",new Condition()
			.withComparisonOperator(ComparisonOperator.GT)
			.withAttributeValueList(new AttributeValue().withN(new Long(msfe_cutoff).toString())));
			queryExpression.setRangeKeyConditions(keyConditions);
		}	
		// execute
		List<NotificationItem> notificationitems = mapper.query(NotificationItem.class, queryExpression, dynamo_config);
		if(notificationitems != null && notificationitems.size() > 0)
		{	
			HashSet<NotificationItem> notificationset = new HashSet<NotificationItem>();
			HashSet<NotificationItem> newsfeedset = new HashSet<NotificationItem>();
			char c = 'X';
			for (NotificationItem notificationitem : notificationitems) {
				c = notificationitem.getId().charAt(10);
				if(c == '7' || c == '8')
					newsfeedset.add(notificationitem);
				else
					notificationset.add(notificationitem);
			}
			HashMap<String,HashSet<NotificationItem>> returnmap = new HashMap<String,HashSet<NotificationItem>>();
			returnmap.put("newsfeed", newsfeedset);
			returnmap.put("notifications", notificationset);
			return returnmap;
		}
		else
		{
			return null;
		}
	}*/
	
	public int compareTo(User o) // this makes more recent comments come first
	{
	    String otherscreenname = ((User)o).getId();
	    int x = otherscreenname.compareTo(getId());
	    if(x >= 0) // this is to prevent equals
	    	return 1;
	    else
	    	return -1;
	}
}