package club.hackbook.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.json.JSONException;
import org.json.JSONObject;

@Entity
@Table(name="ITEMS")
public class Item implements java.lang.Comparable<Item> {
	
	@Id
	@Column (nullable = false) 					private Long id;
	
	// ALWAYS REQUIRED
	@Column (nullable = false) 					private String type;
	@Column (nullable = false) 					private Long time = 0L; // stored in seconds, just like the HN API, not milliseconds
	@Column(name="by_custom", nullable = false) private String by;
	@Column (nullable = false)					private boolean dead = false;
	@Column (nullable = false)					private boolean deleted = false;
	
	// NOT NECESSARILY REQUIRED
	@Column (nullable = true)								private String title;
	@Column (nullable = true)								private String url;
	@Column (nullable = true)								private Long parent = null; // 0L == NULL
	@Column (nullable = true)								private Long score = null; // 0L != NULL, it reall means score = 0
	@Lob
	@Column(columnDefinition ="LONGTEXT", nullable = true) 	private String original_text = null;
	
	@ElementCollection
	@CollectionTable(uniqueConstraints = {@UniqueConstraint(columnNames={"Item_id", "kids"})})
	private Set<Long> kids = new HashSet<Long>();
	
	@ElementCollection
	@CollectionTable(uniqueConstraints = {@UniqueConstraint(columnNames={"Item_id", "urlhashes"})})
	private Set<String> urlhashes = new HashSet<String>();

	//////////////////////////////////////////////////////
	
	public Long getId() {return id; }
	public void setId(Long id) { this.id = id; }
	
	public String getBy() {return by; }
	public void setBy(String by) { this.by = by; }
	
	public Long getTime() {return time; }
	public void setTime(Long time) { this.time = time; } // stored in seconds, just like the HN API, not milliseconds
	
	public String getType() {return type; }
	public void setType(String type) { this.type = type; }
	
	public boolean getDead() {return dead; }
	public void setDead(boolean dead) { this.dead = dead; }
	
	public boolean getDeleted() {return deleted; }
	public void setDeleted(boolean deleted) { this.deleted = deleted; }
	
	public Long getParent() {return parent; }
	public void setParent(Long parent) { this.parent = parent; }
	
	public Long getScore() {return score; }
	public void setScore(Long score) { this.score = score; }
		
	public String getTitle() {return title; }
	public void setTitle(String title) { this.title = title; }	
	
	public String getURL() {return url; }
	public void setURL(String url) { this.url = url; }
	
	public Set<Long> getKids() { return kids; }
	public void setKids(Set<Long> kids) { this.kids = kids; }
	
	public Set<String> getURLHashes() { return urlhashes; }
	public void setURLHashes(Set<String> urlhashes) { this.urlhashes = urlhashes; }
	
	public String getOriginalText() {return original_text; }
	public void setOriginalText(String original_text) { this.original_text = original_text; }
	
	public JSONObject getJSON()
	{
		JSONObject jo = null;
		try
		{
			jo = new JSONObject();
			jo.put("id", this.getId());
			jo.put("by", this.getBy());
			jo.put("time", this.getTime());
			jo.put("type", this.getType());
			jo.put("dead", this.getDead());
			jo.put("deleted", this.getDeleted());
			
			if(this.getTitle() != null)
				jo.put("title", this.getTitle());
			if(this.getURL() != null)
				jo.put("url", this.getURL());
			if(this.getParent() != null && this.getParent() != 0L)
				jo.put("parent", this.getParent());
			if(this.getScore() != null)
				jo.put("score", this.getScore());
			if(this.getOriginalText() != null)
				jo.put("original_text", this.getOriginalText());
			if(this.getKids() != null && !this.getKids().isEmpty())
				jo.put("kids", this.getKids());
		}
		catch(JSONException jsone)
		{
			jsone.printStackTrace();
		}
		return jo;
	}
	
	public int compareTo(Item o) // this makes more recent comments come first
	{
	    long othertime = ((Item)o).getTime();
	    if(othertime < getTime()) // this is to prevent equals
	    	return 1;
	    else if(othertime > getTime())
	    	return -1;
	    else
	    	return 0;
	}
	
}