package police;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BanInfoCommand implements CommandExecutor {

	private Police police;
	
	public BanInfoCommand(Police police) {
		this.police = police;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,	String label, String[] args) {
		// TODO Auto-generated method stub
		if (args.length < 1) {
			return false;
		}
		
		if (sender instanceof Player) {
			if (!police.isAuthorized((Player)sender, "police.baninfo")) {
				sender.sendMessage(ChatColor.RED + "You are not allowed to use this command.");
				return true;
			}
		}
		
		String playerName = args[0].toLowerCase();
		
		if (!police.getRecords().containsKey(playerName))
			police.loadPlayer(playerName);
		
		if (!police.getRecords().containsKey(playerName)) {
			String message = "No ban info for " + playerName +" found.";
					
			if (sender instanceof Player)
				message = ChatColor.GREEN + message;
			
			sender.sendMessage(message);
			
			return true;
		}
			
		int x = 1;
		
		for (BanRecord br : police.getRecords().get(playerName).getBanRecords()) {
			if (x == 1) {
				String message = "Ban history for player " + playerName;
				
				if (sender instanceof Player)
					message = ChatColor.GREEN + message;
				
				sender.sendMessage(message);
			}
			
			String message = "[" + x + "] " + br.getDatetime().toString() + " by " + br.getBannedby() + ": " + br.getReason();
			
			if (sender instanceof Player)
				message = ChatColor.GREEN + message;
			
			sender.sendMessage(message);
				
			x++;
		}
		
		if (x == 1) {
			String message = "No ban info for " + playerName +" found.";
			
			if (sender instanceof Player)
				message = ChatColor.GREEN + message;
			
			sender.sendMessage(message);
		}
		
		return true;
	}

}
