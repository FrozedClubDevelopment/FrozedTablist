package club.frozed.tablist.layout;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import club.frozed.tablist.Tab;
import club.frozed.tablist.entry.TabEntry;
import club.frozed.tablist.skin.Skin;
import club.frozed.tablist.util.Reflection;
import org.apache.commons.lang.StringEscapeUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.spigotmc.ProtocolInjector;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import lombok.Getter;
import net.minecraft.server.v1_7_R4.ChatSerializer;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.IChatBaseComponent;
import net.minecraft.server.v1_7_R4.MinecraftServer;
import net.minecraft.server.v1_7_R4.Packet;
import net.minecraft.server.v1_7_R4.PacketPlayOutPlayerInfo;
import net.minecraft.server.v1_7_R4.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_7_R4.PlayerInteractManager;
import net.minecraft.server.v1_7_R4.WorldServer;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import net.minecraft.util.com.mojang.authlib.properties.Property;

import static club.frozed.tablist.skin.Skin.TEXTURE_KEY;

public class TabLayout_v1_7 {

    private final MinecraftServer minecraftServer = MinecraftServer.getServer();
    private final WorldServer worldServer = minecraftServer.getWorldServer(0);
    private final PlayerInteractManager playerInteractManager = new PlayerInteractManager(worldServer);

    private final Map<Integer, Integer> pingMapping = Maps.newHashMap();
    private final Map<Integer, GameProfile> profileMapping = Maps.newHashMap();
    private final Map<Integer, Skin> skinMapping = Maps.newHashMap();

    @Getter
    private static final Map<UUID, TabLayout_v1_7> layoutMapping = Maps.newHashMap();

    private final Tab instance;
    private final Player player;

    public TabLayout_v1_7(Tab instance, Player player) {
        this.instance = instance;
        this.player = player;
    }

    public void setHeaderAndFooter() {
        int playerVersion = ((CraftPlayer) player).getHandle().playerConnection.networkManager.getVersion();
        if (playerVersion < 47) {
            return;
        }

        String header = (ChatColor.translateAlternateColorCodes('&', instance.getAdapter().getHeader(player)));
        String footer = (ChatColor.translateAlternateColorCodes('&', instance.getAdapter().getFooter(player)));

        IChatBaseComponent headerComponent = ChatSerializer.a("{text:\"" + StringEscapeUtils.escapeJava(header) + "\"}");
        IChatBaseComponent footerComponent = ChatSerializer.a("{text:\"" + StringEscapeUtils.escapeJava(footer) + "\"}");

        ProtocolInjector.PacketTabHeader packetTabHeader = new ProtocolInjector.PacketTabHeader(headerComponent, footerComponent);
        sendPacket(packetTabHeader);
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

        Reflection.getField(packet.getClass(), "f", int.class).set(packet, 2);
        Reflection.getField(packet.getClass(), "g", int.class).set(packet, -1);

        //TODO: send player tab
        sendPacket(packet);

        //TODO: updating tab ping
        fetchPing(row + column * 20, ping);
        //TODO: updating tab skin
        fetchSkin(row + column * 20, skin);
    }

    private void fetchPing(int index, int ping) {
        GameProfile gameProfile = profileMapping.get(index);

        int lastConnection = pingMapping.get(index);
        if (Objects.equals(lastConnection, ping)) {
            return;
        }

        EntityPlayer entityPlayer = new EntityPlayer(minecraftServer, worldServer, gameProfile, playerInteractManager);
        entityPlayer.ping = ping;

        //TODO: putting a new ping
        pingMapping.put(index, ping);

        PacketPlayOutPlayerInfo packetPlayOutPlayerInfo = PacketPlayOutPlayerInfo.updatePing(entityPlayer);
        sendPacket(packetPlayOutPlayerInfo);
    }

    private void fetchSkin(int index, Skin skin) {
        GameProfile gameProfile = profileMapping.get(index);

        int playerVersion = ((CraftPlayer) player).getHandle().playerConnection.networkManager.getVersion();
        if (playerVersion < 47) {
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

        PacketPlayOutPlayerInfo removedPacket = PacketPlayOutPlayerInfo.removePlayer(entityPlayer);
        sendPacket(removedPacket);

        PacketPlayOutPlayerInfo addPacket = PacketPlayOutPlayerInfo.addPlayer(entityPlayer);
        sendPacket(addPacket);

        //TODO: add new game profile modified
        profileMapping.put(index, newGameProfile);
        //TODO: add skin to bars
        skinMapping.put(index, skin);
    }

    public Property getSkinProperty(Skin skin) {
        return new Property(TEXTURE_KEY, skin.getValue(), skin.getSignature());
    }

    public void create() {
        for (int row = 0; row < 20; row++) {
            for (int column = 0; column < 3l; column++) {
                int index = row + column * 20;

                Skin defualtSkin = Skin.DEFAULT_SKIN;
                //TODO: default skin bars
                skinMapping.put(index, defualtSkin);

                Property property = getSkinProperty(skinMapping.get(index));
                GameProfile fakeGameProfile = new GameProfile(UUID.randomUUID(), getTeamAt(row, column));
                fakeGameProfile.getProperties().put(TEXTURE_KEY, property);

                EntityPlayer entityPlayer = new EntityPlayer(minecraftServer, worldServer, fakeGameProfile, playerInteractManager);
                entityPlayer.ping = 0;

                //TODO: default skin bars
                pingMapping.put(index, 0);
                //TODO: defualt game profile to bars
                profileMapping.put(index, fakeGameProfile);

                PacketPlayOutPlayerInfo playerInfo = PacketPlayOutPlayerInfo.addPlayer(entityPlayer);
                sendPacket(playerInfo);
            }
        }

        for (int index = 60; index < 80; index++) {
            Skin defualtSkin = Skin.DEFAULT_SKIN;
            //TODO: default skin bars
            skinMapping.put(index, defualtSkin);

            Property property = getSkinProperty(skinMapping.get(index));
            GameProfile fakeGameProfile = new GameProfile(UUID.randomUUID(), getTeamAt(index));
            fakeGameProfile.getProperties().put(TEXTURE_KEY, property);

            EntityPlayer entityPlayer = new EntityPlayer(minecraftServer, worldServer, fakeGameProfile, playerInteractManager);
            entityPlayer.ping = 0;

            //TODO: default skin bars
            pingMapping.put(index, 0);
            //TODO: defualt game profile to bars
            profileMapping.put(index, fakeGameProfile);

            PacketPlayOutPlayerInfo playerInfo = PacketPlayOutPlayerInfo.addPlayer(entityPlayer);
            sendPacket(playerInfo);
        }

        PacketPlayOutScoreboardTeam packet = new PacketPlayOutScoreboardTeam();
        // TODO: modify values of class @{link PacketPlayOutScoreboardTeam}
        Reflection.getField(packet.getClass(), "a", String.class).set(packet, "tab");
        Reflection.getField(packet.getClass(), "b", String.class).set(packet, "tab");

        Reflection.getField(packet.getClass(), "f", int.class).set(packet, 0);
        Reflection.getField(packet.getClass(), "g", int.class).set(packet, -1);

        Reflection.getField(packet.getClass(), "e", Object.class).set(packet, getAllPlayerNames());

        //TODO: first packet to send
        sendPacket(packet);

        //TODO: to column + 1.8 rows 80
        for (int row = 0; row < 20; row++) {
            for (int column = 0; column < 4; column++) {
                String teamName = "$" + getTeamAt(row, column);

                packet = new PacketPlayOutScoreboardTeam();

                Reflection.getField(packet.getClass(), "a", String.class).set(packet, teamName);
                Reflection.getField(packet.getClass(), "b", String.class).set(packet, teamName);
                ;

                Reflection.getField(packet.getClass(), "f", int.class).set(packet, 0);
                Reflection.getField(packet.getClass(), "g", int.class).set(packet, -1);

                Reflection.getField(packet.getClass(), "e", Object.class).set(packet, Arrays.asList(getTeamAt(row, column)));

                //TODO: final packet to send
                sendPacket(packet);
            }
        }

        PacketPlayOutScoreboardTeam scoreboardTeam = new PacketPlayOutScoreboardTeam();
        Reflection.getField(scoreboardTeam.getClass(), "a", String.class).set(scoreboardTeam, "tab");
        Reflection.getField(scoreboardTeam.getClass(), "b", String.class).set(scoreboardTeam, "tab");

        Reflection.getField(scoreboardTeam.getClass(), "f", int.class).set(scoreboardTeam, 3);
        Reflection.getField(scoreboardTeam.getClass(), "g", int.class).set(scoreboardTeam, -1);

        Reflection.getField(scoreboardTeam.getClass(), "e", Object.class).set(scoreboardTeam, Arrays.asList(player.getName()));

        for (Player target : Bukkit.getOnlinePlayers()) {
            EntityPlayer targetEntity = ((CraftPlayer) target).getHandle();
            targetEntity.playerConnection.sendPacket(scoreboardTeam);
        }
    }

    public void sendPacket(Packet packet) {
        ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    public TabEntry getByLocation(List<TabEntry> entries, int column, int row) {
        for (TabEntry entry : entries) {
            if (entry.getColumn() == column && entry.getRow() == row) {
                return (entry);
            }
        }

        return (null);
    }

    private List<String> getAllPlayerNames() {
        List<String> names = Lists.newArrayList();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }

        return (names);
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
