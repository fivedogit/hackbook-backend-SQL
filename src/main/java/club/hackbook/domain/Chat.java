package club.hackbook.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

import org.json.JSONException;
import org.json.JSONObject;

@Entity
@Table(name="CHATS")
public class Chat implements java.lang.Comparable<Chat> {
	
	@Id
	private String id;
	@Column(nullable = false)
	private String user_id;
	@Lob
	@Column(nullable = false)
	private String text;
	@Column(nullable = false)
	private String hostname;
	@Column(nullable = false)
	private Long msfe = 0L;

	////////////////////////////////
	
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return user_id; }
    public void setUserId(String user_id) { this.user_id = user_id; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public Long getMSFE() { return msfe; }
    public void setMSFE(Long msfe) { this.msfe = msfe; }
    
    public JSONObject getJSON()
	{
		JSONObject jo = null;
		try {
			jo = new JSONObject();
			jo.put("id", getId());
			jo.put("user_id", getUserId());
			jo.put("msfe", getMSFE());
			jo.put("text", getText());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return jo;
	}
	
	public int compareTo(Chat o) // this makes more recent comments come first
	{
	    long othermsfe = ((Chat)o).getMSFE();
	    if(othermsfe < getMSFE()) // this is to prevent equals
	    	return 1;
	    else if (othermsfe > getMSFE())
	    	return -1;
	    else
	    	return 0;
	}
}