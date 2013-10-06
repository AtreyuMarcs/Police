package police;

import java.util.ArrayList;
import java.util.List;

public class PoliceRecord {

	String playerName;
	private List<JailRecord> jailRecords;
	private List<BanRecord> banRecords;
	
	public PoliceRecord(String name) {
		this.setPlayerName(name);
		this.setJailRecords(new ArrayList<JailRecord>());
		this.setBanRecords(new ArrayList<BanRecord>());
	}

    public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public List<JailRecord> getJailRecords() {
		return jailRecords;
	}

	protected void setJailRecords(List<JailRecord> jailRecords) {
		this.jailRecords = jailRecords;
	}

	public List<BanRecord> getBanRecords() {
		return banRecords;
	}

	protected void setBanRecords(List<BanRecord> banRecords) {
		this.banRecords = banRecords;
	}
}
