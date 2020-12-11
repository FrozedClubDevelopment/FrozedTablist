package club.frozed.tablist.layout;

import club.frozed.tablist.Tab;
import club.frozed.tablist.entry.TabEntry;
import club.frozed.tablist.skin.Skin;
import club.frozed.tablist.util.Reflection;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import lombok.Getter;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.IChatBaseComponent.ChatSerializer;
import net.minecraft.server.v1_8_R3.MinecraftServer;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerInfo.EnumPlayerInfoAction;
import net.minecraft.server.v1_8_R3.PacketPlayOutPlayerListHeaderFooter;
import net.minecraft.server.v1_8_R3.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_8_R3.PlayerInteractManager;
import net.minecraft.server.v1_8_R3.WorldServer;
import net.minecraft.server.v1_8_R3.WorldSettings;
import protocolsupport.api.ProtocolSupportAPI;
import us.myles.ViaVersion.api.ViaVersion;

import java.util.*;

import static club.frozed.tablist.skin.Skin.TEXTURE_KEY;

public class TabLayout_v1_8 {

    private final Map<Integer, Integer> pingMapping = Maps.newHashMap();
    private final Map<Integer, GameProfile> profileMapping = Maps.newHashMap();
    private final Map<Integer, Skin> skinMapping = Maps.newHashMap();

    @Getter
    private static final Map<UUID, TabLayout_v1_8> layoutMapping = Maps.newHashMap();

    private final MinecraftServer minecraftServer = MinecraftServer.getServer();
    private final WorldServer worldServer = minecraftServer.getWorldServer(0);
    private final PlayerInteractManager playerInteractManager = new PlayerInteractManager(worldServer);

    private final Tab instance;

    private final Player player;
    private final EntityPlayer entityPlayer;

    public TabLayout_v1_8(Tab instance, Player player) {
        this.instance = instance;
        this.player = player;

        entityPlayer = ((CraftPlayer) player).getHandle();
    }

    public void setHeaderAndFooter() {
        boolean continueAt = false;
        if (Bukkit.getPluginManager().getPlugin("ProtocolSupport") != null) {
            if (ProtocolSupportAPI.getProtocolVersion(player).getId() >= 47) {
                continueAt = true;
            }
        }

        if (Bukkit.getPluginManager().getPlugin("ViaVersion") != null) {
            if (ViaVersion.getInstance().getPlayerVersion(player) >= 47) {
                continueAt = true;
            }
        }

        if (continueAt) {
            String header = (ChatColor.translateAlternateColorCodes('&', instance.getAdapter().getHeader(player)));
            String footer = (ChatColor.translateAlternateColorCodes('&', instance.getAdapter().getFooter(player)));

            IChatBaseComponent headerComponent = ChatSerializer.a("{text:\"" + StringEscapeUtils.escapeJava(header) + "\"}");
            IChatBaseComponent footerComponent = ChatSerializer.a("{text:\"" + StringEscapeUtils.escapeJava(footer) + "\"}");

            PacketPlayOutPlayerListHeaderFooter packet = new PacketPlayOutPlayerListHeaderFooter();

            Reflection.getField(packet.getClass(), "a", Object.class).set(packet, headerComponent);
            Reflection.getField(packet.getClass(), "b", Object.class).set(packet, footerComponent);

            this.entityPlayer.playerConnection.sendPacket(packet);
        }
    }

    public void update(int column, int row, String text, int ping, Skin skin) {
        if (row > 19) {
            throw new RuntimeException("Row is above 19 " + row);
        }

        if (column > 4) {
            throw new RuntimeException("Column is above 4 " + column);
        }

        text = ChatColor.translateAlternateColorCodes('&', text);

        String prefix = text;
        String suffix = "";

        //TODO: check prefix char to color code
        if (text.length() > 16) {
            prefix = text.substring(0, 16);

            if (prefix.charAt(15) == ChatColor.COLOR_CHAR) {
                prefix = prefix.substring(0, 15);
                suffix = text.substring(15, text.length());
            } else if (prefix.charAt(14) == ChatColor.COLOR_CHAR) {
                prefix = prefix.substring(0, 14);
                suffix = text.substring(14, text.length());
            } else {
                suffix = ChatColor.getLastColors(prefix) + text.substring(16, text.length());
            }
        }

        if (suffix.length() > 16) {
            suffix = suffix.substring(0, 16);
        }

        String teamName = "$" + getTeamAt(row, column); //TODO: create a new team
        PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();

        Reflection.getField(packet.getClass(), "a", String.class).set(packet, teamName);
        Reflection.getField(packet.getClass(), "b", String.class).set(packet, teamName);
        ;

        Reflection.getField(packet.getClass(), "c", String.class).set(packet, prefix);
        Reflection.getField(packet.getClass(), "d", String.class).set(packet, suffix);

        Reflection.getField(packet.getClass(), "h", int.class).set(packet, 2);
        Reflection.getField(packet.getClass(), "e", String.class).set(packet, ("always"));
        ;
        Reflection.getField(packet.getClass(), "f", int.class).set(packet, -1);

        //TODO: send player tab
        entityPlayer.playerConnection.sendPacket(packet);

        int index = row + column * 20;
        GameProfile gameProfile = profileMapping.get(index);

        //TODO: updating tab ping
        fetchPing(index, gameProfile, ping);
        //TODO: updating tab skin
        fetchSkin(index, gameProfile, skin);
    }

    private void fetchPing(int index, GameProfile gameProfile, int ping) {
        int lastConnection = pingMapping.get(index);
        if (Objects.equals(lastConnection, ping)) {
            return;
        }

        EntityPlayer entityPlayer = new EntityPlayer(minecraftServer, worldServer, gameProfile, playerInteractManager);
        entityPlayer.ping = ping;

        //TODO: putting a new bars ping
        pingMapping.put(index, ping);

        PacketPlayOutPlayerInfo packetPlayOutPlayerInfo = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.UPDATE_LATENCY, entityPlayer);
        this.entityPlayer.playerConnection.sendPacket(packetPlayOutPlayerInfo);
    }

    private void fetchSkin(int index, GameProfile gameProfile, Skin skin) {
        boolean continueAt = false;
        if (Bukkit.getPluginManager().getPlugin("ProtocolSupport") != null) {
            if (ProtocolSupportAPI.getProtocolVersion(player).getId() >= 47) {
                continueAt = true;
            }
        }

        if (Bukkit.getPluginManager().getPlugin("ViaVersion") != null) {
            if (ViaVersion.getInstance().getPlayerVersion(player) >= 47) {
                continueAt = true;
            }
        }

        if (!continueAt) {
            return;
        }

        if (skin == null) {
            skin = Skin.DEFAULT_SKIN;
        }

        Skin lastSkin = skinMapping.get(index);
        if (Objects.equals(skin, lastSkin)) {
            return;
        }

        GameProfile newGameProfile = new GameProfile(gameProfile.getId(), gameProfile.getName());
        newGameProfile.getProperties().put(TEXTURE_KEY, getSkinProperty(skin));

        EntityPlayer entityPlayer = new EntityPlayer(minecraftServer, worldServer, newGameProfile, playerInteractManager);

        PacketPlayOutPlayerInfo removePacket = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.REMOVE_PLAYER, entityPlayer);
        this.entityPlayer.playerConnection.sendPacket(removePacket);

        PacketPlayOutPlayerInfo addPacket = new PacketPlayOutPlayerInfo(EnumPlayerInfoAction.ADD_PLAYER, entityPlayer);
        this.entityPlayer.playerConnection.sendPacket(addPacket);

        //TODO: add new game profile modified
        profileMapping.put(index, newGameProfile);
        //TODO: add skin to bars
        skinMapping.put(index, skin);
    }

    private Property getSkinProperty(Skin skin) {
        return new Property(TEXTURE_KEY, skin.getValue(), skin.getSignature());
    }

    public void create() {
        PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
        PacketPlayOutPlayerInfo packetInfo = new PacketPlayOutPlayerInfo();

        Reflection.getField(packetInfo.getClass(), "a", Object.class).set(packetInfo, PacketPlayOutPlayerInfo.EnumPlayerInfoAction.ADD_PLAYER);

        List<PacketPlayOutPlayerInfo.PlayerInfoData> infoDatas = (List<PacketPlayOutPlayerInfo.PlayerInfoData>) Reflection.getField(packetInfo.getClass(), "b", Object.class).get(packetInfo);

        GameProfile gameProfile;

        for (int row = 0; row < 20; row++) {
            for (int column = 0; column < 3; column++) {
                int index = row + column * 20;
                Skin defualtSkin = Skin.DEFAULT_SKIN;
                //TODO: default skin bars
                skinMapping.put(index, defualtSkin);

                Property property = getSkinProperty(skinMapping.get(index));

                gameProfile = new GameProfile(UUID.randomUUID(), getTeamAt(row, column));
                gameProfile.getProperties().put(TEXTURE_KEY, property);

                infoDatas.add(packetInfo.new PlayerInfoData(gameProfile, 0, WorldSettings.EnumGamemode.SURVIVAL, null));

                //TODO: default skin bars
                pingMapping.put(index, 0);
                //TODO: defualt game profile to bars
                profileMapping.put(index, gameProfile);
            }
        }

        for (int index = 60; index < 80; index++) {
            Skin defualtSkin = Skin.DEFAULT_SKIN;
            //TODO: default skin bars
            skinMapping.put(index, defualtSkin);

            Property property = getSkinProperty(skinMapping.get(index));

            gameProfile = new GameProfile(UUID.randomUUID(), getTeamAt(index));
            gameProfile.getProperties().put(TEXTURE_KEY, property);

            infoDatas.add(packetInfo.new PlayerInfoData(gameProfile, 0, WorldSettings.EnumGamemode.SURVIVAL, null));

            //TODO: default skin bars
            pingMapping.put(index, 0);
            //TODO: defualt game profile to bars
            profileMapping.put(index, gameProfile);
        }

        Bukkit.getLogger().info("Send info datas");
        entityPlayer.playerConnection.sendPacket(packetInfo);

        packet = new PacketPlayOutScoreboardTeam();
        // TODO: modify values of class @{link PacketPlayOutScoreboardTeam}
        Reflection.getField(packet.getClass(), "a", String.class).set(packet, "tab");
        Reflection.getField(packet.getClass(), "b", String.class).set(packet, "tab");

        Reflection.getField(packet.getClass(), "h", int.class).set(packet, 0);
        Reflection.getField(packet.getClass(), "f", int.class).set(packet, -1);

        Reflection.getField(packet.getClass(), "e", String.class).set(packet, "always");

        Collection<String> players = Lists.newArrayList();
        for (Player other : Bukkit.getOnlinePlayers()) {
            players.add(other.getName());
        }

        Reflection.getField(packet.getClass(), "g", Object.class).set(packet, players);

        //TODO: first packet to send
        this.entityPlayer.playerConnection.sendPacket(packet);

        //TODO: to column + 1.8 rows 80
        for (int row = 0; row < 20; row++) {
            for (int column = 0; column < 4; column++) {
                String teamName = "$" + getTeamAt(row, column);

                packet = new PacketPlayOutScoreboardTeam();

                Reflection.getField(packet.getClass(), "a", String.class).set(packet, teamName);
                Reflection.getField(packet.getClass(), "b", String.class).set(packet, teamName);
                ;

                Reflection.getField(packet.getClass(), "h", int.class).set(packet, 0);
                Reflection.getField(packet.getClass(), "f", int.class).set(packet, -1);

                Reflection.getField(packet.getClass(), "e", String.class).set(packet, "always");
                Reflection.getField(packet.getClass(), "g", Object.class).set(packet, Collections.singleton(getTeamAt(row, column)));

                //TODO: final packet to send
                this.entityPlayer.playerConnection.sendPacket(packet);
            }
        }


        PacketPlayOutScoreboardTeam scoreboardTeam = new PacketPlayOutScoreboardTeam();
        Reflection.getField(scoreboardTeam.getClass(), "a", String.class).set(scoreboardTeam, "tab");
        Reflection.getField(scoreboardTeam.getClass(), "b", String.class).set(scoreboardTeam, "tab");

        Reflection.getField(scoreboardTeam.getClass(), "h", int.class).set(scoreboardTeam, 3);
        Reflection.getField(scoreboardTeam.getClass(), "f", int.class).set(scoreboardTeam, -1);

        Reflection.getField(scoreboardTeam.getClass(), "g", Object.class).set(scoreboardTeam, Collections.singleton(player.getName()));

        for (Player target : Bukkit.getOnlinePlayers()) {
            ((CraftPlayer) target).getHandle().playerConnection.sendPacket(scoreboardTeam);
        }
    }

    public TabEntry getByLocation(List<TabEntry> entries, int column, int row) {
        for (TabEntry entry : entries) {
            if (entry.getColumn() == column && entry.getRow() == row) {
                return (entry);
            }
        }

        return (null);
    }

    private String getTeamAt(int row, int column) {
        return getTeamAt(row + column * 20);
    }

    private String getTeamAt(int index) {
        return (
                ChatColor.BOLD.toString() +
                        ChatColor.GREEN.toString() +
                        ChatColor.UNDERLINE.toString() +
                        ChatColor.YELLOW.toString() +
                        (index >= 10 ?
                                ChatColor.COLOR_CHAR + String.valueOf(index / 10) + ChatColor.COLOR_CHAR + String.valueOf(index % 10) :
                                ChatColor.BLACK.toString() + ChatColor.COLOR_CHAR + String.valueOf(index)) +
                        ChatColor.RESET
        );
    }
}