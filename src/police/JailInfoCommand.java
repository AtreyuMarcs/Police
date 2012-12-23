package police;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

public class JailInfoCommand implements CommandExecutor {

	private Police police;
	
	public JailInfoCommand(Police police) {
		this.police = police;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 0) {
			if (!(sender instanceof Player)) {
				return false;
			}
			
			if (!police.isAuthorized((Player)sender, "police.jailinfo.self")) {
				sender.sendMessage(ChatColor.RED + "You are not allowed to use this command.");
				return true;
			} else {
				Essentials ess = (Essentials)police.getServer().getPluginManager().getPlugin("Essentials");
				if (ess == null) {
					sender.sendMessage("Essentials not found on Server! Please contact your Admin!");
					return true;
				}
				
				final User player = ess.getUser(sender);
				police.jailInfo(sender, player, false);
				return true;
			}
			
		}
		
		if (args.length == 1) {
			final User player = police.getPlayer(args, 0, true);
			if (player == null) {
				String message = "User " + args[0] + " not found.";
				if (sender instanceof Player)
					message = ChatColor.DARK_RED + message;
			
				sender.sendMessage(message);
				return true;
			}
			
			if (!(sender instanceof Player)) {
				police.jailInfo(sender, player, true);
				return true;
			}
			
			if (player.getName().equalsIgnoreCase(sender.getName())) {
				if (!police.isAuthorized((Player)sender, "police.jailinfo.self")) {
					sender.sendMessage(ChatColor.RED + "You are not allowed to use this command.");
					return true;
				} else {
					police.jailInfo(sender, player, false);
					return true;
				}
			} else {
				if (!police.isAuthorized((Player)sender, "police.jailinfo.other")) {
					sender.sendMessage(ChatColor.RED + "You are not allowed to use this command.");
					return true;
				} else {
					police.jailInfo(sender, player, true);
					return true;
				}
			}
		}
		
		if (args.length == 3) {
			if (!args[0].equalsIgnoreCase("delete"))
				return false;
		
			final User player = police.getPlayer(args, 1, true);
			
			if (player == null) {
				String message = "User " + args[1] + " not found.";
				if (sender instanceof Player)
					message = ChatColor.DARK_RED + message;
			
				sender.sendMessage(message);
				return true;
			}
					
			String playerName = player.getName().toLowerCase();
			
			if (!police.getRecords().containsKey(playerName))
				police.loadPlayer(playerName);
			
			if (!police.getRecords().containsKey(playerName)) {
				String message = "Player " + playerName +" not found.";
				
				if (sender instanceof Player)
					message = ChatColor.DARK_RED + message;
				
				sender.sendMessage(message);
				
				return true;
			}
			
			for (JailRecord jr : police.getRecords().get(playerName).getJailRecords()) {
				if (String.valueOf(jr.getId()).equalsIgnoreCase(args[2])) {
					police.deleteJailInfo(playerName, jr);
					
					String message = "Jail record number " + jr.getId() + " for player " + playerName + " expunged.";
					
					if (sender instanceof Player)
						message = ChatColor.GREEN + message;
					
					sender.sendMessage(message);
					
					return true;
				}
			}
			
			String message = "ID " + args[2] + " does not match player " + playerName;
			
			if (sender instanceof Player)
				message = ChatColor.DARK_RED + message;
			
			sender.sendMessage(message);
			
			return true;
		}
		
		return false;
	}

}
