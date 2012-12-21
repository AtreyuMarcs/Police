package police;





import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.OfflinePlayer;
import com.earth2me.essentials.User;
import com.earth2me.essentials.commands.EssentialsCommand;

public class BanCommand implements CommandExecutor {

	private Police police;
	
	public BanCommand(Police police) {
		this.police = police;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,	String label, String[] args) {
		
		if (args.length < 1) {
			sender.sendMessage("Not enough arguments!");
			return false;
		}
		
		User user = this.police.getPlayer(args, 0, true);
			
		Essentials ess = (Essentials)police.getServer().getPluginManager().getPlugin("Essentials");
		if (ess == null) {
			sender.sendMessage("Essentials not found on Server! Please contact your Admin!");
			return true;
		}
		
		if (user == null) {
			user = ess.getUser(new OfflinePlayer(args[0], ess));
		}
		
		if (!user.isOnline()) {
			if (sender instanceof Player) {
				if (!police.isAuthorized((Player)sender,"police.ban.offline")) {
					sender.sendMessage(ChatColor.RED + "You are not allowed to ban offline players.");
					return true;
				}
					
			}
		} else {
			if (sender instanceof Player) {
				if (!police.isAuthorized((Player)sender,"police.ban")) {
					sender.sendMessage(ChatColor.RED + "You are not allowed to ban players.");
					return true;
				}
				
				if (police.isAuthorized(user.getPlayer(), "police.ban.exempt")) {
					sender.sendMessage(ChatColor.RED + "Target player cannot be banned.");
					return true;					
				}
			}
		}
		
		final String senderName = sender instanceof Player ? ((Player)sender).getDisplayName() : "Console";
		String banReason;
		if (args.length > 1)
		{
			banReason = EssentialsCommand.getFinalArg(args, 1);
		}
		else
		{
			banReason = police.getDefaultBanReason();
		}

		user.setBanReason(banReason);
		user.setBanned(true);
		user.kickPlayer(banReason);
		
		police.getLogger().info(senderName + " has banned " + user.getName() + " for: " + banReason);
		
		for (Player onlinePlayer : police.getServer().getOnlinePlayers())
		{
			final User player = ess.getUser(onlinePlayer);
			if (onlinePlayer == sender || player.isAuthorized("essentials.ban.notify"))
			{
				onlinePlayer.sendMessage(ChatColor.RED + "Player "+senderName+" banned "+user.getName()+" for " + banReason);
			}
		}
		
		police.addBanRecord(user.getName().toLowerCase(), new BanRecord(banReason, senderName, new Date()));
		
		return true;
	}

}
