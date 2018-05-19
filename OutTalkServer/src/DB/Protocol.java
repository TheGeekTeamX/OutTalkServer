package DB;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

@Entity(name = "Protocols")
public class Protocol implements IDBEntity {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name="Id")
	private int id;
    @OneToOne(targetEntity = Event.class)
    @JoinColumn(name = "EventId")
    private Event event;
    @Column(name = "ProtocolURL")
	private String protocolURL;
    
    
	public Protocol() {
		super();
		// TODO Auto-generated constructor stub
	}


	public Protocol(int id, Event event, String protocolURL) {
		super();
		this.id = id;
		this.event = event;
		this.protocolURL = protocolURL;
	}


	public int getId() {
		return id;
	}


	public void setId(int id) {
		this.id = id;
	}


	public Event getEvent() {
		return event;
	}


	public void setEvent(Event event) {
		this.event = event;
	}


	public String getProtocolURL() {
		return protocolURL;
	}


	public void setProtocolURL(String protocolURL) {
		this.protocolURL = protocolURL;
	}


	@Override
	public void update(IDBEntity other) {
		// TODO Auto-generated method stub

	}

}
