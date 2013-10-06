package police;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;

public class AsyncWriter implements Runnable {

	private String name;
	private JailRecord jr;
	private BanRecord br;
	private Police police;
	private boolean delete;

	public AsyncWriter(String name, JailRecord jr, BanRecord br, Police police, boolean delete) {
		this.name = name;
		this.jr = jr;
		this.br = br;
		this.police = police;
		this.delete = delete;
	}

	@Override
	public void run() {
		if (police.isUsingSQL()) {
			if (police.getConnection() != null) {
				try {
					if (!police.getConnection().isValid(police.getSqltimeout())) {
						if (!police.open(false)) {
							police.getLogger().log(Level.SEVERE, "No SQL Connection established");
							return;
						}
					}

					if (jr != null) {

						if (delete) {
							PreparedStatement statement = police.getConnection().prepareStatement("delete from police_jail where jailid=?");
							statement.setInt(1, jr.getId());
							statement.executeUpdate();

                            statement.close();
							
						} else {

							PreparedStatement statement = police.getConnection().prepareStatement("insert into police_jail values (default, ?, ?, ?, ?, ?, ?)");

							statement.setString(1, name);
							statement.setString(2, jr.getJailor());
							statement.setString(3, jr.getDuration());
							statement.setString(4, jr.getReason());
							statement.setTimestamp(5, new Timestamp(jr.getDatetime().getTime()));
							statement.setString(6, jr.getPos());
							statement.executeUpdate();

                            statement.close();

							PreparedStatement queryStatement = police.getConnection().prepareStatement("select * from police_jail where playername=? order by jailid ASC");
							queryStatement.setString(1, name);

							ResultSet resultSet = queryStatement.executeQuery();

							int x = 0;

							while (resultSet.next()) {
								x = resultSet.getInt("jailid");
							}

							jr.setId(x);

                            resultSet.close();

                            queryStatement.close();
						}
					}

					if (br != null) {
						PreparedStatement statement = police.getConnection().prepareStatement("insert into police_ban values (default, ?, ?, ?, ?)");

						statement.setString(1, name);
						statement.setString(2, br.getReason());
						statement.setString(3, br.getBannedby());
						statement.setTimestamp(4, new Timestamp(br.getDatetime().getTime()));
						statement.executeUpdate();

                        statement.close();
					}
				} catch (SQLException ex) {
					police.getLogger().log(Level.WARNING, "Could not write police records for player " + name);
					police.getLogger().info(ex.toString());
				}
			}
		} else {			
			if (jr != null) {
				if (!police.getPlainFile().getConfigurationSection("jail").contains(name)) {
					police.getPlainFile().getConfigurationSection("jail").createSection(name);
				}				

				ConfigurationSection jailee = police.getPlainFile().getConfigurationSection("jail").getConfigurationSection(name);

				if (jailee != null) { //should always be the case

					if (delete) {
						for (String s : jailee.getKeys(false)) {
							if (jailee.getInt(s + ".id") == jr.getId())
								jailee.set(s, null);
						}
						
						police.plainSave();

					} else {

						int nextEntry = jailee.getKeys(false).size() + 1;
						jr.setId(nextEntry);
						jailee.set(nextEntry + ".jailedby", jr.getJailor());
						jailee.set(nextEntry + ".duration", jr.getDuration());
						jailee.set(nextEntry + ".reason", jr.getReason());
						jailee.set(nextEntry + ".pos", jr.getPos());
						jailee.set(nextEntry + ".datetime", jr.getDatetime().getTime());
						jailee.set(nextEntry + ".id", jr.getId());

						police.plainSave();
					}
				}
			}

			if (br != null) {
				if (!police.getPlainFile().getConfigurationSection("ban").contains(name)) {
					police.getPlainFile().getConfigurationSection("ban").createSection(name);
				}				

				ConfigurationSection banned = police.getPlainFile().getConfigurationSection("ban").getConfigurationSection(name);

				if (banned != null) { //should always be the case
					String nextEntry = String.valueOf(banned.getKeys(false).size() + 1);
					banned.set(nextEntry + ".bannedby", br.getBannedby());
					banned.set(nextEntry + ".reason", br.getReason());
					banned.set(nextEntry + ".datetime", br.getDatetime().getTime());

					police.plainSave();
				}
			}
		}
	}

}
