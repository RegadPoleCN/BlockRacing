package top.lqsnow.blockracing.managers;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.toolkit.text.Texts;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TeamChat {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final Set<UUID> PENDING_HINTS = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> KNOWN_HINTS = ConcurrentHashMap.newKeySet();

    private TeamChat() {
    }

    public static void handle(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        GameInstance arena = GameManager.get().sessions().arenaOf(sender).orElse(null);
        if (arena == null || arena.getState() != RoomState.RUNNING) {
            return;
        }
        TeamColor senderTeam = arena.getTeams().teamOf(sender.getName()).orElse(null);
        if (senderTeam == null) {
            return;
        }
        boolean red = senderTeam == TeamColor.RED;
        boolean blue = senderTeam == TeamColor.BLUE;

        String serialized = LEGACY.serialize(event.message());
        int prefixLength = globalPrefixLength(serialized);
        if (prefixLength > 0) {
            String content = serialized.substring(prefixLength).stripLeading();
            if (content.isEmpty()) {
                event.setCancelled(true);
                return;
            }
            event.message(LEGACY.deserialize(content));
            event.renderer((source, displayName, message, viewer) ->
                    globalComponent(viewer instanceof Player player ? player : null,
                            source.getName(), LEGACY.serialize(message)));
            return;
        }

        Set<String> members = Set.copyOf(arena.getTeams().players(senderTeam));
        event.viewers().removeIf(viewer -> viewer instanceof Player player
                && !members.contains(player.getName()));
        Message format = red ? Message.TEAM_RED_CHAT : Message.TEAM_BLUE_CHAT;
        event.renderer((source, displayName, message, viewer) -> {
            String localized = viewer instanceof Player player
                    ? format.getString(player)
                    : format.getString();
            return LEGACY.deserialize(String.format(localized, source.getName(), LEGACY.serialize(message)));
        });
        sendFirstTeamHint(sender);
    }

    public static void shout(Player sender, String message) {
        if (message.isBlank()) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(viewer ->
                viewer.sendMessage(globalComponent(viewer, sender.getName(), message)));
        Bukkit.getConsoleSender().sendMessage(globalComponent(null, sender.getName(), message));
    }

    private static Component globalComponent(Player viewer, String playerName, String message) {
        String format = viewer == null
                ? Message.CHAT_GLOBAL_FORMAT.getString()
                : Message.CHAT_GLOBAL_FORMAT.getString(viewer);
        String team = globalTeamPrefix(viewer, playerName);
        return Texts.component(format
                .replace("%team%", team)
                .replace("%player%", globalPlayerName(viewer, playerName))
                .replace("%message%", message));
    }

    private static String globalTeamPrefix(Player viewer, String playerName) {
        TeamColor team = teamOf(playerName);
        if (team == null) {
            return "&7[--]";
        }
        Message prefix = team == TeamColor.RED ? Message.TEAM_RED_PREFIX : Message.TEAM_BLUE_PREFIX;
        return viewer == null ? prefix.getString() : prefix.getString(viewer);
    }

    private static String globalPlayerName(Player viewer, String playerName) {
        TeamColor team = teamOf(playerName);
        if (team == null) {
            return "&7" + playerName;
        }
        Message color = team == TeamColor.RED ? Message.TEAM_RED_COLOR : Message.TEAM_BLUE_COLOR;
        return (viewer == null ? color.getString() : color.getString(viewer)) + playerName;
    }

    private static TeamColor teamOf(String playerName) {
        return GameManager.get().sessions().arenaOf(playerName)
                .map(arena -> arena.getTeams().teamOf(playerName).orElse(null))
                .orElse(null);
    }

    static int globalPrefixLength(String message) {
        if (message.isEmpty()) {
            return 0;
        }
        int first = message.codePointAt(0);
        return first == '@' || first == '＠' || first == '!' || first == '！' || first == '﹗'
                ? Character.charCount(first)
                : 0;
    }

    private static void sendFirstTeamHint(Player player) {
        NamespacedKey key = new NamespacedKey(Main.getInstance(), "team_chat_hint_seen");
        UUID uuid = player.getUniqueId();
        if (KNOWN_HINTS.contains(uuid) || !PENDING_HINTS.add(uuid)) {
            return;
        }
        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            if (!player.getPersistentDataContainer().has(key, PersistentDataType.BYTE)) {
                player.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
                player.sendMessage(Message.CHAT_TEAM_HINT.getString(player));
            }
            KNOWN_HINTS.add(uuid);
            PENDING_HINTS.remove(uuid);
        });
    }
}
