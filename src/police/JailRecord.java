package police;

import java.util.Date;

public class JailRecord {

	private String jailor;
	private String duration;
	private String reason;
	private String pos;
	private Date datetime;
	private int id;
	
	public JailRecord(String jailor, String duration, String reason, String pos, Date datetime, int id) {
		this.setJailor(jailor);
		this.setDuration(duration);
		this.setReason(reason);
		this.setPos(pos);
		this.setDatetime(datetime);
		this.setId(id);
	}

	public Date getDatetime() {
		return datetime;
	}

	public void setDatetime(Date datetime) {
		this.datetime = datetime;
	}

	public String getPos() {
		return pos;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getJailor() {
		return jailor;
	}

	public void setJailor(String jailor) {
		this.jailor = jailor;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
}
