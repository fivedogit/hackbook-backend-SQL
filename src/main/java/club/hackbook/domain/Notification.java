package club.hackbook.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.json.JSONException;
import org.json.JSONObject;

// TYPES
// 0. ** Someone followed user
// 1. ** a comment user wrote was upvoted
// 2. ** a comment user wrote was downvoted
// 3. ** a story user wrote was upvoted
// 4. ** a story user wrote was downvoted
// 5. ** a comment user wrote was commented on
// 6. ** a story user wrote was commented on
// 7. a user this user is following posted a story
// 8. a user this user is following commented
// 9. *** a user deep-replied to your *comment*
// A. unused
// B. unused
// C. *** user was mentioned in chat

@Entity
@Table(name="NOTIFICATIONS")
public class Notification implements java.lang.Comparable<Notification> {
	
	@Id
	private String id; 
	
	// ALWAYS REQUIRED
	@Column(nullable = false) private String user_id;
	@Column(nullable = false) private String type;
	@Column(nullable = false) private Long action_msfe = 0L;
	@Column(nullable = false) private Long msfe = 0L;
	
	// NOT NECESSARILY REQUIRED 
	@Column(nullable = true) private String triggerer;
	@Column(nullable = true) private Long hn_target_id = 0L; // both 0 and NULL are equal, as far as this backend is concerned
	@Column(nullable = true) private Long hn_root_story_id = 0L; // both 0 and NULL are equal, as far as this backend is concerned // also, "story" means "story or poll"
	@Column(nullable = true) private Long hn_root_comment_id = 0L; // both 0 and NULL are equal, as far as this backend is concerned
	@Column(nullable = true) private Long karma_change = 0L; // both 0 and NULL are equal, as far as this backend is concerned
	
	//////////////////////////////////
	
	public String getId() {return id; }
	public void setId(String id) { this.id = id; }
	
	public String getUserId() {return user_id; }
	public void setUserId(String user_id) { this.user_id = user_id; }
	
	public Long getActionMSFE() {return action_msfe; }
	public void setActionMSFE(Long action_msfe) { this.action_msfe = action_msfe; }
	
	public Long getMSFE() {return msfe; }
	public void setMSFE(Long msfe) { this.msfe = msfe; }
	
	public String getType() {return type; }
	public void setType(String type) { this.type = type; }
	
	public Long getHNTargetId() {return hn_target_id; }
	public void setHNTargetId(Long hn_target_id) { this.hn_target_id = hn_target_id; }
	
	public String getTriggerer() {return triggerer; }
	public void setTriggerer(String triggerer) { this.triggerer = triggerer; }
	
	public Long getHNRootStoryOrPollId() {return hn_root_story_id; }
	public void setHNRootStoryOrPollId(Long hn_root_story_id) { this.hn_root_story_id = hn_root_story_id; }
	
	public Long getHNRootCommentId() {return hn_root_comment_id; }
	public void setHNRootCommentId(Long hn_root_comment_id) { this.hn_root_comment_id = hn_root_comment_id; }
	
	public Long getKarmaChange() {return karma_change; }
	public void setKarmaChange(Long karma_change) { this.karma_change = karma_change; }
	
	public JSONObject getJSON()
	{
		JSONObject jo = null;
		try {
			jo = new JSONObject();
			jo.put("id", getId());
			jo.put("user_id", getUserId());
			jo.put("action_msfe", getActionMSFE());
			jo.put("msfe", getMSFE());
			jo.put("type", getType());
			jo.put("hn_target_id", getHNTargetId());
			jo.put("triggerer", getTriggerer());
			jo.put("hn_root_story_id", getHNRootStoryOrPollId());
			jo.put("hn_root_comment_id", getHNRootCommentId());
			jo.put("karma_change", getKarmaChange());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jo;
	}
	
	public int compareTo(Notification o) // this makes more recent comments come first
	{
	    Long otheractionmsfe = ((Notification)o).getActionMSFE();
	    if(otheractionmsfe < getActionMSFE()) // this is to prevent equals
	    	return 1;
	    else if (otheractionmsfe > getActionMSFE())
	    	return -1;
	    else
	    	return 0;
	}
	
}