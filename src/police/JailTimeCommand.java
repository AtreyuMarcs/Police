package police;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

public class JailTimeCommand implements CommandExecutor {

	private Police police;
	
	public JailTimeCommand(Police police) {
		this.police = police;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,	String label, String[] args) {
		if (args.length == 0) {
			if (!(sender instanceof Player)) {
				return false;
			}
			
			if (!police.isAuthorized((Player)sender, "police.jailtime.self")) {
				sender.sendMessage(ChatColor.RED + "You are not allowed to use this command.");
				return true;
			} else {
				Essentials ess = (Essentials)police.getServer().getPluginManager().getPlugin("Essentials");
				if (ess == null) {
					sender.sendMessage("Essentials not found on Server! Please contact your Admin!");
					return true;
				}
				
				final User player = ess.getUser((Player)sender);
				if (player == null) {
					sender.sendMessage("Internal error! Please contact your Admin!");
					return true;
				}
				
				if (!player.isJailed()) {
					sender.sendMessage(ChatColor.GREEN + "You are not jailed!");
					return true;
				}
				
				police.jailTime((Player)sender, player, false);
				return true;
			}
			
		}
		
		if (args.length >= 1) {
			final User player = police.getPlayer(args, 0, true);
			if (player == null) {
				String message = "User " + args[0] + " not found.";
				
				if (sender instanceof Player)
					message = ChatColor.DARK_RED + message;
				
				sender.sendMessage(message);
				return true;
			}
	
			if (player.getName().equalsIgnoreCase(sender.getName())) {
				if (!(sender instanceof Player))
					return false;
				
				if (!police.isAuthorized((Player)sender, "police.jailinfo.self")) {
					sender.sendMessage(ChatColor.RED + "You are not allowed to use this command.");
					return true;
				} else {
					if (!player.isJailed()) {
						sender.sendMessage(ChatColor.GREEN + "You are not jailed!");
						return true;
					}
					
					police.jailTime((Player)sender, player, false);
					return true;
				}
			} else {
				if (sender instanceof Player) {
					if (!police.isAuthorized((Player)sender, "police.jailinfo.other")) {
						sender.sendMessage(ChatColor.RED + "You are not allowed to use this command.");
						return true;
					}
				}
				
				if (!player.isJailed()) {
					String message = "Player " + player.getName() + " is not jailed.";
					
					if (sender instanceof Player)
						message = ChatColor.GREEN + message;
					
					sender.sendMessage(message);
					return true;
				}
				
				police.jailTime((Player)sender, player, true);
				
				return true;
				
			}
		}
		
		return false;
	}

}
