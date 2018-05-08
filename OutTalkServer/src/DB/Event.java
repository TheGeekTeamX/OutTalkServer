package DB;
import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity(name = "Events")
public class Event implements IDBEntity{

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="Id")
	private int id;
    @OneToOne(targetEntity = User.class)
    @JoinColumn(name = "AdminId")
    private User adminId;
	@Column(name = "Title")
	private String title;
	@Column(name = "DateCreated")
	private Date dateCreated;
	@Column(name = "IsFinished")
	private int isFinished;
	@Column(name = "IsConverted")
	private int isConverted;
	
	
	public Event() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Event(User adminId,String title, Date dateCreated, int isFinished, int isConverted) {
		super();
		this.title = title;
		this.adminId = adminId;
		this.dateCreated = dateCreated;
		this.isFinished = isFinished;
		this.isConverted = isConverted;
	}
	public 
	int getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void setId(int id) {
		this.id = id;
	}
	public User getAdminId() {
		return adminId;
	}
	public void setAdminId(User adminId) {
		this.adminId = adminId;
	}
	public Date getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	public int getIsFinished() {
		return isFinished;
	}
	public void setIsFinished(int isFinished) {
		this.isFinished = isFinished;
	}
	public int getIsConverted() {
		return isConverted;
	}
	public void setIsConverted(int isConverted) {
		this.isConverted = isConverted;
	}
	@Override
	public String toString()
	{
		return "Event [id="+id+",adminId="+adminId+",dateCreated="+dateCreated+",isFinished="+isFinished+",isConverted="+isConverted+"]";
	}
	@Override
	public void update(IDBEntity other) {
		// TODO Auto-generated method stub
		if(other.getClass() == this.getClass())
		{
			this.adminId = ((Event)other).adminId;
			this.dateCreated = ((Event)other).dateCreated;
			this.isFinished = ((Event)other).isFinished;
			this.isConverted = ((Event)other).isConverted;
		}
		
	}
}
