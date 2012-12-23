package police;

import java.util.Date;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.Util;

public class ToggleJailCommand implements CommandExecutor {

	private Police police;
	
	public ToggleJailCommand(Police police) {
		this.police = police;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command,	String label, String[] args) {
		if (args.length < 1) {
			return false;
		}
		
		if (sender instanceof Player) {
			if (!police.isAuthorized((Player)sender, "police.togglejail")) {
				sender.sendMessage(ChatColor.RED + "You are not allowed to use this command.");
				return true;
			}
		}
		
		final User player = police.getPlayer(args, 0, true);
		
		if (player == null) {
			String message = "User " + args[0] + " not found";
			
			if (sender instanceof Player)
				message = ChatColor.DARK_RED + message;
			
			sender.sendMessage(message);
			return true;
		}
		
		if (args.length >= 2 && !player.isJailed())
		{		
			Essentials ess = (Essentials)police.getServer().getPluginManager().getPlugin("Essentials");
			
			if (ess == null) {
				sender.sendMessage("Essentials not found on Server! Please contact your Admin!");
				return true;
			}
			
			if (player.isOnline())
			{
				try {
					ess.getJails().sendToJail(player,  args[1]);
				} catch (Exception e) {
					String message = "Jail " + args[1] + " not found.";
					
					if (sender instanceof Player)
						message = ChatColor.DARK_RED + message;
					
					sender.sendMessage(message);
				}
			} else {
				try {
					ess.getJails().getJail(args[1]);
				} catch (Exception e) {
					String message = "Jail " + args[1] + " not found.";
					
					if (sender instanceof Player)
						message = ChatColor.DARK_RED + message;
					
					sender.sendMessage(message);
				}
			}
			
			player.setJailed(true);
			player.sendMessage(ChatColor.RED + "You have been jailed.");
			player.setJail(null);
			player.setJail(args[1]);
			
			long timeDiff = 0;
			String time = "";
			if (args.length > 2)
			{
				time = args[2];
				try {
					timeDiff = Util.parseDateDiff(time, true);
					player.setJailTimeout(timeDiff);
				} catch (Exception e) {
					sender.sendMessage("Jail Time not recognized.");
				}
				
			}
			sender.sendMessage((timeDiff > 0
								? ChatColor.GREEN + "Player " + ChatColor.RED +  player.getName() + ChatColor.GREEN + " jailed for " + Util.formatDateDiff(timeDiff)
								: ChatColor.GREEN + "Player " + ChatColor.RED +  player.getName() + ChatColor.GREEN + " jailed."));
			
			String reason = "";
			
			if (args.length > 3) {
				final StringBuilder bldr = new StringBuilder();
				for (int i = 3; i < args.length; i++)
				{
					if (i != 3)
						bldr.append(" ");
					bldr.append(args[i]);
				}
				reason = bldr.toString();
			}
			
			String senderName = sender.getName();
			Player p = (Player)sender;
			String senderPos = "";
			
			if (!p.getLocation().getWorld().getName().equalsIgnoreCase(police.getDefaultWorld()))
				senderPos = senderPos + p.getLocation().getWorld().getName() + ":";
			
			senderPos = senderPos + String.valueOf((int)p.getLocation().getX()) + "," + String.valueOf((int)p.getLocation().getY()) + "," + String.valueOf((int)p.getLocation().getZ());
			
			JailRecord jr = new JailRecord(senderName, time, reason, senderPos, new Date(), 0);
			
			police.addJailRecord(player.getName().toLowerCase(), jr, sender);			
			
			return true;
		}
		
		if (args.length > 1 && player.isJailed())
		{
			String message = "Person is already in jail: ";
			
			if (sender instanceof Player)
				message = ChatColor.DARK_RED + message + ChatColor.RED;
			
			message += player.getJail();
			
			if (sender instanceof Player)
				message += ChatColor.DARK_RED;
			
			message += ". Please unjail before jailing anew for proper jail history keeping.";
			
			sender.sendMessage(message);
			
			return true;
		}

		if (args.length == 1)
		{
			if (!player.isJailed())
			{
				return false;
			}
			player.setJailed(false);
			player.setJailTimeout(0);
			player.sendMessage(ChatColor.GREEN + "You have been released!");
			player.setJail(null);
			if (player.isOnline())
			{
				try {
					player.getTeleport().back();
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
			
			String message = "Player " + player.getName() + " unjailed.";
			
			if (sender instanceof Player)
				message = ChatColor.GREEN + message;
			
			sender.sendMessage(message);
			
			return true;
		}
		
		return false;
	}

}
