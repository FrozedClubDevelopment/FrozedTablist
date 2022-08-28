package club.frozed.tablist.runnable;

import club.frozed.tablist.adapter.TabAdapter;
import club.frozed.tablist.entry.TabEntry;
import club.frozed.tablist.latency.TabLatency;
import club.frozed.tablist.layout.TabLayout;
import club.frozed.tablist.skin.Skin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Created by Ryzeon
 * Project: TabAPI
 * Date: 11/12/2020 @ 10:08
 */
public class TabRunnable implements Runnable {

	private final TabAdapter adapter;

	public TabRunnable(TabAdapter adapter) {
		this.adapter = adapter;
	}

	@Override
	public void run() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (TabLayout.getLayoutMapping().containsKey(player.getUniqueId())) {
				TabLayout layout = TabLayout.getLayoutMapping().get(player.getUniqueId());

				for (TabEntry entry : adapter.getLines(player)) {
					layout.update(entry.getColumn(), entry.getRow(), entry.getText(), entry.getPing(), entry.getSkin());
				}

				for (int row = 0; row < 20; row++) {
					for (int column = 0; column < 3; column++) {
						if (layout.getByLocation(adapter.getLines(player), column, row) == null) {
							layout.update(column, row, "", TabLatency.NO_BAR.getValue(), Skin.DEFAULT_SKIN);
						}
					}
				}
			}
		}
	}
}
