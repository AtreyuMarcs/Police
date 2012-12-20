package police;

import java.io.File;
import java.io.IOException;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.Util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class Police extends JavaPlugin {
	
	private int autobanNumber;
	private String db = null;
	private String username = null;
	private String password = null;
	private String host = null;
	private String port = null;
	private Connection connection;
	private PermissionManager permissionsEx;
	private GroupManager groupManager;
	private int perm = 0;
	private boolean usingSQL;
	private HashMap<String, PoliceRecord> records;
	private FileConfiguration plainFile = null;
	private int sqltimeout;
	private String defaultBanReason;
	private String defaultWorld;
	
	@Override
	public void onEnable() {
		// Plugin Enable Logic
		File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			saveDefaultConfig();
		}
		
		Essentials ess = (Essentials)getServer().getPluginManager().getPlugin("Essentials");
		if (ess == null) {
			getLogger().info("[Police] Essentials not found on Server, disabling");
			setEnabled(false);
			return;
		}
				
		this.setRecords(new HashMap<String, PoliceRecord>());
		
		setAutobanNumber(this.getConfig().getInt("jail_autoban_number", 0));
		setDefaultBanReason(this.getConfig().getString("default_ban_reason", "The Ban Hammer has spoken!"));
		setDefaultWorld(this.getConfig().getString("default_world", "world"));
		
		//SQL Data
		setDb(this.getConfig().getConfigurationSection("sql").getString("database", ""));
		setUsername(this.getConfig().getConfigurationSection("sql").getString("user", ""));
		setPassword(this.getConfig().getConfigurationSection("sql").getString("password", ""));
		setHost(this.getConfig().getConfigurationSection("sql").getString("host", ""));
		setPort(this.getConfig().getConfigurationSection("sql").getString("port", ""));
		setUsingSQL(this.getConfig().getConfigurationSection("sql").getBoolean("use", false));
		setSqltimeout(this.getConfig().getConfigurationSection("sql").getInt("sql_timeout", 5));
				
		getCommand("jailinfo").setExecutor(new JailInfoCommand(this));
		
		getCommand("baninfo").setExecutor(new BanInfoCommand(this));
		
		getCommand("jailtime").setExecutor(new JailTimeCommand(this));
		
		getCommand("togglejail").setExecutor(new ToggleJailCommand(this));
		
		getCommand("ban").setExecutor(new BanCommand(this));

		getCommand("unban").setExecutor(new UnbanCommand(this));
		
		getServer().getPluginManager().registerEvents(new PoliceListener(this), this);
		
		if (isUsingSQL())
			setUsingSQL(open(true));
		
		if (!isUsingSQL()) {
			readPlainFile();
		} else {
			readSQL();
		}
	}
	
	public void initializePermissionHandler() {
		if (getServer().getPluginManager().isPluginEnabled("bPermissions")) {
			//this.bpermissions = WorldManager.getInstance();
			this.getLogger().info("[Police] bPermissions detected as permission plugin.");
			this.perm = 1;
		}
		
		if (getServer().getPluginManager().isPluginEnabled("PermissionsEx")) {
			this.permissionsEx = PermissionsEx.getPermissionManager();
			this.getLogger().info("[Police] PermissionsEx detected as permission plugin.");
			this.perm = 2;
		}
		
		if (getServer().getPluginManager().isPluginEnabled("GroupManager")) {
			this.groupManager = (GroupManager)getServer().getPluginManager().getPlugin("GroupManager");
			this.getLogger().info("[Police] GroupManager detected as permission plugin.");
			this.perm = 3;
		}
		
		if (this.perm == 0) {
			this.getLogger().info("[Police] No Permissions plugin detected, defaulting to Ops.");
		}
	}
	
	public boolean isAuthorized(Player player, String node) {
		if (this.perm == 0)
			return player.isOp();
		
		if (this.perm == 1)
			return player.hasPermission(node);
		
		if (this.perm == 2)
			return this.permissionsEx.has(player, node);
		
		if (this.perm == 3) {
			final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
			if (handler != null)
				return handler.has(player, node);
		}
		
		return false;
	}
	
	public void onDisable()
	{
		if (isUsingSQL())
			this.close();
	}
	
	public boolean open(boolean verbose) {
		try {
			String url = new StringBuilder().append("jdbc:mysql://").append(this.host).append(":").append(this.port).append("/").append(this.db).append("?autoReconnect=true").toString();
			this.setConnection(DriverManager.getConnection(url, this.username, this.password));
			
			if (this.connection != null) {
				this.connection.setAutoCommit(true);
				if (verbose)
					this.getLogger().info("[Police] SQL Connection established");
				return true;
				
			} else {
				if (verbose)
					this.getLogger().info("[Police] SQL Connection failed, defaulting to PlainFile");
				return false;
			}
			
		} catch (SQLException ex) {
			if (verbose) {
				this.getLogger().info("[Police] Error: SQL Exception: " + ex.getMessage());
				this.getLogger().info("[Police] Defaulting to PlainFile");
			}
			return false;
		}
		
	}
	
	public void close() {
		try {
			if (this.connection != null)
				this.connection.close();
		} catch (SQLException ex) {
			this.getLogger().info("[Police] Error: SQL Exception: " + ex.getMessage());
		}
	}
	
	public void plainSave() {
		File file = new File(getDataFolder(), "policerecords.yml");
		if (file.exists() && file.canWrite()) {
			try {
				getPlainFile().save(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				getLogger().log(Level.SEVERE, "Failed to write to Plainfile. Check your folder permissions.");
			}
		}
	}
	
	private void readPlainFile() {
		File file = new File(getDataFolder(), "policerecords.yml");
		if (!file.exists()) {
			try {
				file.createNewFile();
				setPlainFile(YamlConfiguration.loadConfiguration(file));
				getPlainFile().createSection("jail");
				getPlainFile().createSection("ban");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				getLogger().log(Level.SEVERE, "Failed to create Plainfile. Check your folder permissions.");
			}
		} else {
			setPlainFile(YamlConfiguration.loadConfiguration(file));
		}
	}
	
	private void readSQL() {
		if (getConnection() == null) {
			if (!open(false)) {
				getLogger().log(Level.SEVERE, "No SQL Connection established");
				return;
			}
		}
		
		try {
			
			if (!getConnection().isValid(getSqltimeout())) {
				if (!open(false)) {
					getLogger().log(Level.SEVERE, "No SQL Connection established");
					return;
				}
			}
			
			Statement statement = getConnection().createStatement();
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `police_jail` (`jailid` MEDIUMINT NOT NULL AUTO_INCREMENT, `playername` VARCHAR(32), `jailedby` VARCHAR(32), `duration` VARCHAR(32), `reason` TEXT, `datetime` TIMESTAMP, `pos` TEXT, PRIMARY KEY (`jailid`), INDEX (`playername`))");
			statement.executeUpdate("CREATE TABLE IF NOT EXISTS `police_ban` (`banid` MEDIUMINT NOT NULL AUTO_INCREMENT, `playername` VARCHAR(32), `bannedby` VARCHAR(32), `reason` TEXT, `datetime` TIMESTAMP, PRIMARY KEY (`banid`), INDEX (`playername`))");
			
			statement.close();
			
			getLogger().info("[Police] Database tables initialized");
		}  catch (SQLException ex) {
			getLogger().log(Level.SEVERE, "Tables could not be created");
		}
	}
	
	protected void addJailRecord(String name, JailRecord jr, CommandSender sender) {
		if (!getRecords().containsKey(name)) {
			loadPlayer(name);
		}
		
		if (getRecords().containsKey(name)) {
			getRecords().get(name).getJailRecords().add(jr);
			
			 if (getRecords().get(name).getJailRecords().size() >= getAutobanNumber() && getAutobanNumber() > 0) {
				String message = "Maximum number of jail sentences (" + getAutobanNumber() + ") reached, banning player " + name;
				
				if (sender instanceof Player)
					message = ChatColor.RED + message;
				 
				sender.sendMessage(message);
				
				getServer().dispatchCommand(getServer().getConsoleSender(), "ban " + name + " Police: Maximum Jail Sentences");
			}
		}
		
		getServer().getScheduler().runTaskAsynchronously(this, new AsyncWriter(name, jr, null, this, false));
	}
	
	protected void addBanRecord(String name, BanRecord br) {
		if (getRecords().containsKey(name)) {
			getRecords().get(name).getBanRecords().add(br);
		}
		
		getServer().getScheduler().runTaskAsynchronously(this, new AsyncWriter(name, null, br, this, false));
	}
	
	public void deleteJailInfo(String playerName, JailRecord jr) {
		if (getRecords().containsKey(playerName)) {
			getRecords().get(playerName).getJailRecords().remove(jr);
		}
		
		getServer().getScheduler().runTaskAsynchronously(this,  new AsyncWriter(playerName, jr, null, this, true));
	}
	
	protected void loadPlayer(String name) {
		if (isUsingSQL()) {
			if (getConnection() != null) {
				try {
					if (!getConnection().isValid(getSqltimeout())) {
						if (!open(false)) {
							getLogger().log(Level.SEVERE, "No SQL Connection established");
							return;
						}
					}
					
					PoliceRecord pr = new PoliceRecord(name);
					
					PreparedStatement statement = getConnection().prepareStatement("Select * from `police_jail` where playername=? ORDER BY jailid");
					statement.setString(1, name);
					ResultSet resultSet = statement.executeQuery();
					
					List<JailRecord> jailRecords = new ArrayList<JailRecord>();
					
					while (resultSet.next()) {
						jailRecords.add(new JailRecord(resultSet.getString("jailedby"), resultSet.getString("duration"), resultSet.getString("reason"), resultSet.getString("pos"), resultSet.getDate("datetime"), resultSet.getInt("jailid")));
					}
					
					pr.setJailRecords(jailRecords);
					
					statement = getConnection().prepareStatement("Select * from `police_ban` where playername=? ORDER BY banid");
					statement.setString(1, name);
					resultSet = statement.executeQuery();
					
					List<BanRecord> banRecords = new ArrayList<BanRecord>();
					
					while (resultSet.next()) {
						banRecords.add(new BanRecord(resultSet.getString("reason"), resultSet.getString("bannedby"), resultSet.getDate("datetime")));
					}
					
					pr.setBanRecords(banRecords);
					
					getRecords().put(name, pr);
					
					if (resultSet != null)
						resultSet.close();
					
					if (statement != null)
						statement.close();
					
				} catch (SQLException ex) {
					getLogger().log(Level.WARNING, "Could not read police records for player " + name);
				}
			}
		} else {
			if (getPlainFile() != null) {
				PoliceRecord pr = new PoliceRecord(name);
				
				if (getPlainFile().getConfigurationSection("jail").contains(name)) {
					List<JailRecord> jailRecords = new ArrayList<JailRecord>();
					
					ConfigurationSection jailBlock = getPlainFile().getConfigurationSection("jail").getConfigurationSection(name);
					for (String s : jailBlock.getKeys(false)) {
						String jailedby = jailBlock.getString(s + ".jailedby");
						String duration = jailBlock.getString(s + ".duration");
						String reason = jailBlock.getString(s + ".reason");
						String pos = jailBlock.getString(s + ".pos");
						Date datetime = new Date(jailBlock.getLong(s + ".datetime"));
						int x = jailBlock.getInt(s + ".id");
						
						jailRecords.add(new JailRecord(jailedby, duration, reason, pos, datetime, x));
					}
					
					pr.setJailRecords(jailRecords);
					
					List<BanRecord> banRecords = new ArrayList<BanRecord>();
					
					ConfigurationSection banBlock = getPlainFile().getConfigurationSection("ban").getConfigurationSection(name);
					for (String s : banBlock.getKeys(false)) {
						String bannedby = banBlock.getString(s + ".bannedby");
						String reason = banBlock.getString(s + ".reason");
						Date datetime = new Date(banBlock.getLong(s + ".datetime"));
						
						banRecords.add(new BanRecord(reason, bannedby, datetime));
					}
					
					pr.setBanRecords(banRecords);
				}
					
				getRecords().put(name, pr);
			}
		}
	}
	

	protected User getPlayer(final String[] args, final int pos, final boolean getOffline) {
		if (args.length <= pos) {
			return null;
		}
		
		if (args[pos].isEmpty()) {
			return null;
		}
		
		Essentials ess = (Essentials)getServer().getPluginManager().getPlugin("Essentials");
		if (ess == null) {
			this.getLogger().info("[Police] Essentials not found on Server");
			return null;
		}
		
		final User user = ess.getUser(args[pos]);
		
		if (user != null) {
			if (!getOffline && (!user.isOnline() || user.isHidden())) {
				return null;
			}
			return user;
		}
		
		final List<Player> matches = getServer().matchPlayer(args[pos]);
		
		if (!matches.isEmpty()) {
			for (Player player : matches)
			{
				final User userMatch = ess.getUser(player);
				if (userMatch.getDisplayName().startsWith(args[pos]) && (getOffline || !userMatch.isHidden())) {
					return userMatch;
				}
			}
			final User userMatch = ess.getUser(matches.get(0));
			if (getOffline || !userMatch.isHidden())
			{
				return userMatch;
			}
		}
		
		return null;
	}
	
	public void jailInfo(CommandSender sender, User player, boolean other) {
		if (sender == null || player == null)
			return;
		
		if (!getRecords().containsKey(player.getName()))
			loadPlayer(player.getName());
		
		if (getRecords().containsKey(player.getName())) {
		
			int i = 1;
			
			for (JailRecord jr : getRecords().get(player.getName()).getJailRecords()) {
				if (i == 1 && other) {
					String message = "Jail history for player " + player.getName() + ":";
					if (sender instanceof Player)
						message = ChatColor.GREEN + message;
					
					sender.sendMessage(message);
				} else if (i == 1 && !other) {
					String message = "Your jail history:";
					if (sender instanceof Player)
						message = ChatColor.GREEN + message;
					
					sender.sendMessage(message);
				}
					
				String message = "[";
				
				if (other) 
					message += String.valueOf(jr.getId());
				else
					message += String.valueOf(i);
				
				message += "] " + jr.getDatetime().toString() + " " + jr.getJailor() + " for " + jr.getDuration();
				
				if (other)
					message += " at pos " + jr.getPos();
				
				message += ": " + jr.getReason();
				
				sender.sendMessage(message);				
				i++;				
			}
			
			if (i == 1) {
				sender.sendMessage("No jail info for " + player.getName() +" found.");
			}
		
		} else {
			sender.sendMessage("No jail info for " + player.getName() +" found.");
		}
	}
	
	public void jailTime(Player sender, User player, boolean other) {
		if (sender == null || player == null)
			return;
		
		String message = "Remaining time in jail: " + Util.formatDateDiff(player.getJailTimeout());
		
		if (sender instanceof Player)
			message = ChatColor.GREEN + message;
		
		sender.sendMessage(message);
		
		this.jailInfo(sender,  player, other);	
	}

	
	public int getAutobanNumber() {
		return autobanNumber;
	}

	public void setAutobanNumber(int autobanNumber) {
		this.autobanNumber = autobanNumber;
	}
	
	public boolean autobanEnabled() {
		return (autobanNumber > 0); 
	}

	public String getDb() {
		return db;
	}

	public void setDb(String db) {
		this.db = db;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public boolean isUsingSQL() {
		return usingSQL;
	}

	public void setUsingSQL(boolean usingSQL) {
		this.usingSQL = usingSQL;
	}

	public HashMap<String, PoliceRecord> getRecords() {
		return records;
	}

	public void setRecords(HashMap<String, PoliceRecord> records) {
		this.records = records;
	}

	public FileConfiguration getPlainFile() {
		return plainFile;
	}

	public void setPlainFile(FileConfiguration plainFile) {
		this.plainFile = plainFile;
	}

	public int getSqltimeout() {
		return sqltimeout;
	}

	public void setSqltimeout(int sqltimeout) {
		this.sqltimeout = sqltimeout;
	}

	public String getDefaultBanReason() {
		return defaultBanReason;
	}

	public void setDefaultBanReason(String defaultBanReason) {
		this.defaultBanReason = defaultBanReason;
	}

	public String getDefaultWorld() {
		return defaultWorld;
	}

	public void setDefaultWorld(String defaultWorld) {
		this.defaultWorld = defaultWorld;
	}
	
}
