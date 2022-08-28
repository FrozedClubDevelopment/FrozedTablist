package club.frozed.tablist.listener;

import club.frozed.tablist.FrozedTablist;
import club.frozed.tablist.layout.TabLayout;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TabListener implements Listener {

	private final FrozedTablist instance;

	public TabListener(FrozedTablist instance) {
		this.instance = instance;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		TabLayout layout;
		boolean validate = false;

		layout = new TabLayout(instance, player);
		if (TabLayout.getLayoutMapping().containsKey(player.getUniqueId())) {
			validate = true;
		}
		if (TabLayout.getLayoutMapping().get(player.getUniqueId()) != null) {
			validate = true;
		}
		if (!validate) {
			layout.create();
			layout.setHeaderAndFooter();
		}

		TabLayout.getLayoutMapping().put(player.getUniqueId(), layout);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		instance.removePlayer(player);
	}

	@EventHandler
	public void onKick(PlayerKickEvent event) {
		Player player = event.getPlayer();
		instance.removePlayer(player);
	}
}
