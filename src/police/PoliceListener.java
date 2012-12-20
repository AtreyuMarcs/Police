package police;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;

public class PoliceListener implements Listener {

	private Police police;
	
	public PoliceListener(Police plugin) {
		this.police = plugin;
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJoin(final PlayerJoinEvent event) {
		if (police.getRecords().containsKey(event.getPlayer().getName()))
			return;
		
		police.loadPlayer(event.getPlayer().getName().toLowerCase());
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerJailtimeSign(final PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR)
			return;
		
		final Block block;
		if (event.isCancelled() && event.getAction() == Action.RIGHT_CLICK_AIR)
		{
			Block targetBlock = null;
			try
			{
				targetBlock = event.getPlayer().getTargetBlock(null, 5);
			}
			catch (IllegalStateException ex)
			{
				police.getLogger().info("Illegel state exception in JailSC");
				return;
			}
			block = targetBlock;
		}
		else
		{
			block = event.getClickedBlock();
		}
		if (block == null)
		{
			return;
		}
		
		final int mat = block.getTypeId();
		if (mat == Material.SIGN_POST.getId() || mat == Material.WALL_SIGN.getId())
		{
			final Sign csign = (Sign)block.getState();
			if (csign.getLine(0).contains("[JailTime]"))
			{
				Essentials ess = (Essentials)police.getServer().getPluginManager().getPlugin("Essentials");
				if (ess == null) {
					police.getLogger().info("Null Error in JailSCListener");
					event.setCancelled(true);
					return;
				}
				
				final User player = ess.getUser(event.getPlayer());
				if (player == null) {
					police.getLogger().info("Internal Error in JailSCListener");
					event.setCancelled(true);
					return;
				}
				
				if (!player.isJailed()) {
					event.getPlayer().sendMessage(ChatColor.GREEN + "You are not jailed!");
					event.setCancelled(true);
					return;
				}
				
				police.jailTime(event.getPlayer(), player, false);				
				event.setCancelled(true);
				return;
			}
			
		}
	}
}
