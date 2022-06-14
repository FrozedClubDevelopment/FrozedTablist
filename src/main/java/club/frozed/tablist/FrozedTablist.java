package club.frozed.tablist;

import club.frozed.tablist.adapter.TabAdapter;
import club.frozed.tablist.layout.TabLayout;
import club.frozed.tablist.listener.TabListener;
import club.frozed.tablist.packet.TabPacket;
import club.frozed.tablist.runnable.TabRunnable;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public class FrozedTablist {

    @Getter private static FrozedTablist instance;

    private final TabAdapter adapter;

    private Version version;

    public FrozedTablist(JavaPlugin plugin, TabAdapter adapter, int delay1, int delay2) {
        instance = this;
        this.adapter = adapter;

        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        String version = packageName.substring(packageName.lastIndexOf('.') + 1);
        try {
            this.version = Version.valueOf(version);
            plugin.getLogger().info("[Tab] Using " + this.version.name() + " version.");
        } catch (final Exception e) {
            e.printStackTrace();
            return;
        }

        handlerPacket(plugin);

        Bukkit.getServer().getPluginManager().registerEvents(new TabListener(this), plugin);
        if (this.version == Version.v1_8_R3) {
            Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(plugin, new TabRunnable(adapter), delay1, delay2); //TODO: async to run 1 millis
        }
    }

    private void handlerPacket(JavaPlugin plugin) {
        if (this.version == Version.v1_8_R3) {
            new TabPacket(plugin);
        }
    }

    public void onDisable() {
        for (Player player : Bukkit.getServer().getOnlinePlayers()) {
            removePlayer(player);
        }
    }

    public void removePlayer(Player player) {
        boolean continueAt = false;
        if (this.version == Version.v1_8_R3) {
            if (TabLayout.getLayoutMapping().containsKey(player.getUniqueId())) {
                continueAt = true;
            }

            if (continueAt) {
                TabLayout.getLayoutMapping().remove(player.getUniqueId());
            }
        }
    }

    public enum Version {
        v1_8_R3
    }
}
