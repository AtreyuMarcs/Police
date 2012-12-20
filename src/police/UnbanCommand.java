package police;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.earth2me.essentials.User;

public class UnbanCommand implements CommandExecutor {

	private Police police;
	
	public UnbanCommand(Police police) {
		this.police = police;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,	String label, String[] args) {
		if (args.length < 1) {
			return false;
		}
		
		if (sender instanceof Player) {
			if (!police.isAuthorized((Player)sender, "police.unban")) {
				sender.sendMessage(ChatColor.RED + "You are not allowed to unban players.");
				return true;
			}
		}
		
		final User player = police.getPlayer(args, 0, true);
		
		if (player == null) {
			sender.sendMessage(ChatColor.RED + "Player " + args[0] + " not found");
			return true;
		}
		
		player.setBanned(false);
		sender.sendMessage(ChatColor.GREEN + "Unbanned player " + player.getName());
		
		return true;
		
	}

}
