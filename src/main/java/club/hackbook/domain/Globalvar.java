package club.hackbook.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name="GLOBALVARS")
public class Globalvar {
	
	@Id
	private String name; 
	@Lob
	@Column(columnDefinition="LONGTEXT")
	private String stringval;
	private Long numberval = 0L;

    public Globalvar() {}
    public Globalvar(String inc_name, String inc_stringval, Long inc_numberval) {
    	this.name = inc_name;
    	this.stringval = inc_stringval;
    	this.numberval = inc_numberval;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
   
    public String getStringValue() { return stringval; }
    public void setStringValue(String stringval) { this.stringval = stringval; }

    public Long getNumberValue() { return numberval; }
    public void setNumberValue(Long numberval) { this.numberval = numberval; }
}