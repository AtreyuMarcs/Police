package police;

import java.util.Date;

public class BanRecord {

	private String reason;
	private String bannedby;
	private Date datetime;
	
	public BanRecord(String reason, String bannedby, Date datetime) {
		this.setReason(reason);
		this.setBannedby(bannedby);
		this.setDatetime(datetime);
	}

	public Date getDatetime() {
		return datetime;
	}

	public void setDatetime(Date datetime) {
		this.datetime = datetime;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getBannedby() {
		return bannedby;
	}

	public void setBannedby(String bannedby) {
		this.bannedby = bannedby;
	}
}
