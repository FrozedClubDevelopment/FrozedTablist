package club.frozed.tablist.packet;

import club.frozed.tablist.util.Reflection;
import club.frozed.tablist.util.TinyProtocol;
import io.netty.channel.Channel;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Created by Ryzeon
 * Project: TabAPI
 * Date: 11/12/2020 @ 09:58
 */
public class TabPacket extends TinyProtocol {

	public TabPacket(Plugin plugin) {
		super(plugin);
	}

	@Override
	public Object onPacketOutAsync(Player reciever, Channel channel, Object object) {
		if (object instanceof PacketPlayOutScoreboardTeam) {
			PacketPlayOutScoreboardTeam scoreboardTeam = (PacketPlayOutScoreboardTeam) object;

			int value = Reflection.getField(scoreboardTeam.getClass(), "h", int.class).get(scoreboardTeam);
			String teamName = Reflection.getField(scoreboardTeam.getClass(), "a", String.class).get(scoreboardTeam);
			if (value == 4 && !teamName.equalsIgnoreCase("tab")) {
				Reflection.getField(scoreboardTeam.getClass(), "a", String.class).set(scoreboardTeam, "tab");
				Reflection.getField(scoreboardTeam.getClass(), "b", String.class).set(scoreboardTeam, "tab");
				Reflection.getField(scoreboardTeam.getClass(), "h", int.class).set(scoreboardTeam, 3);
			}
		}

		return super.onPacketOutAsync(reciever, channel, object);
	}
}
