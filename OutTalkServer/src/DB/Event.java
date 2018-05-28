package DB;
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
    private User admin;
	@Column(name = "Title")
	private String title;
	@Column(name = "DateCreated")
	private String dateCreated;
	@Column(name = "IsFinished")
	private int isFinished;
	@Column(name = "IsConverted")
	private int isConverted;
	@Column(name = "Description")
	private String description;
	
	
	public Event() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Event(User admin,String title, String dateCreated, int isFinished, int isConverted, String description) {
		super();
		this.title = title;
		this.admin = admin;
		this.dateCreated = dateCreated;
		this.isFinished = isFinished;
		this.isConverted = isConverted;
		this.description = description;
	}
	
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public	int getId() {
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
	public User getAdmin() {
		return admin;
	}
	public void setAdmin(User admin) {
		this.admin = admin;
	}
	public String getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(String dateCreated) {
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
		return "Event [id="+id+",admin="+admin+",dateCreated="+dateCreated+",isFinished="+isFinished+",isConverted="+isConverted+",description="+description+"]";
	}
	@Override
	public void update(IDBEntity other) {
		// TODO Auto-generated method stub
		if(other.getClass() == this.getClass())
		{
			this.admin = ((Event)other).admin;
			this.dateCreated = ((Event)other).dateCreated;
			this.isFinished = ((Event)other).isFinished;
			this.isConverted = ((Event)other).isConverted;
			this.description = ((Event)other).description;
		}
		
	}
}
