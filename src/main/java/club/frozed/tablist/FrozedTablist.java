package club.frozed.tablist;

import club.frozed.tablist.adapter.TabAdapter;
import club.frozed.tablist.layout.TabLayout;
import club.frozed.tablist.listener.TabListener;
import club.frozed.tablist.packet.TabPacket;
import club.frozed.tablist.runnable.TabRunnable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FrozedTablist {

	private final TabAdapter adapter;

	public FrozedTablist(JavaPlugin plugin, TabAdapter adapter, int delay1, int delay2) {
		this.adapter = adapter;

		new TabPacket(plugin);
		plugin.getServer().getPluginManager().registerEvents(new TabListener(this), plugin);
		plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new TabRunnable(adapter), delay1, delay2);
	}

	public TabAdapter getAdapter() {
		return adapter;
	}

	public void onDisable() {
		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			removePlayer(player);
		}
	}

	public void removePlayer(Player player) {
		boolean continueAt = TabLayout.getLayoutMapping().containsKey(player.getUniqueId());
		if (continueAt) {
			TabLayout.getLayoutMapping().remove(player.getUniqueId());
		}
	}
}
