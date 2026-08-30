package top.lqsnow.blockracing.managers;

import java.util.HashSet;
import java.util.List;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.TeamColor;

import static top.lqsnow.blockracing.managers.Scoreboard.scoreboard;
import static top.lqsnow.blockracing.utils.CommandUtil.sendAll;

/**
 * Transitional facade: team membership lives in the instance's
 * {@link top.lqsnow.blockracing.game.TeamState}; scoreboard registration
 * stays here.
 */
@Deprecated
public class Team {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
    public static org.bukkit.scoreboard.Team redTeam = scoreboard.registerNewTeam("red");
    public static org.bukkit.scoreboard.Team blueTeam = scoreboard.registerNewTeam("blue");

    private Team() {
    }

    private static GameInstance arena() {
        return GameManager.first();
    }

    public static List<String> redTeamPlayers() {
        return arena().getTeams().players(TeamColor.RED);
    }

    public static List<String> blueTeamPlayers() {
        return arena().getTeams().players(TeamColor.BLUE);
    }

    public static void createTeam() {
        redTeam.displayName(LEGACY_SERIALIZER.deserialize(Message.TEAM_RED_NAME.getString()));
        redTeam.prefix(LEGACY_SERIALIZER.deserialize(Message.TEAM_RED_PREFIX.getString()));
        redTeam.color(NamedTextColor.RED);
        blueTeam.displayName(LEGACY_SERIALIZER.deserialize(Message.TEAM_BLUE_NAME.getString()));
        blueTeam.prefix(LEGACY_SERIALIZER.deserialize(Message.TEAM_BLUE_PREFIX.getString()));
        blueTeam.color(NamedTextColor.BLUE);
        Scoreboard.syncPlayerTeams();
    }

    public static boolean joinTeam(Player player, org.bukkit.scoreboard.Team team, boolean sendMessage) {
        TeamColor color = team.equals(redTeam) ? TeamColor.RED : TeamColor.BLUE;
        List<String> members = arena().getTeams().players(color);
        List<String> otherMembers = arena().getTeams().players(color.opponent());
        if (members.contains(player.getName())) {
            if (sendMessage) {
                player.sendMessage(color == TeamColor.RED
                        ? Message.NOTICE_ALREADY_IN_RED.getString(player)
                        : Message.NOTICE_ALREADY_IN_BLUE.getString(player));
            }
            return false;
        }
        if (otherMembers.contains(player.getName())) {
            org.bukkit.scoreboard.Team otherTeam = color == TeamColor.RED ? blueTeam : redTeam;
            otherTeam.removeEntry(player.getName());
            otherMembers.remove(player.getName());
        }
        team.addEntry(player.getName());
        members.add(player.getName());
        Scoreboard.syncPlayerTeams();
        if (sendMessage) {
            sendAll(color == TeamColor.RED ? Message.NOTICE_JOIN_RED : Message.NOTICE_JOIN_BLUE,
                    (viewer, text) -> text.replace("%player%", player.getName()));
        }
        return true;
    }

    public static boolean isPlayerInRedTeam(Player player) {
        return redTeamPlayers().contains(player.getName());
    }

    public static boolean isPlayerInBlueTeam(Player player) {
        return blueTeamPlayers().contains(player.getName());
    }

    public static void clearTeams() {
        new HashSet<>(redTeam.getEntries()).forEach(redTeam::removeEntry);
        new HashSet<>(blueTeam.getEntries()).forEach(blueTeam::removeEntry);
        arena().getTeams().clear();
        Scoreboard.syncPlayerTeams();
    }

    public static void restoreTeams(List<String> redPlayers, List<String> bluePlayers) {
        new HashSet<>(redTeam.getEntries()).forEach(redTeam::removeEntry);
        new HashSet<>(blueTeam.getEntries()).forEach(blueTeam::removeEntry);
        arena().getTeams().restore(redPlayers, bluePlayers);
        redPlayers.forEach(redTeam::addEntry);
        bluePlayers.forEach(blueTeam::addEntry);
        Scoreboard.syncPlayerTeams();
    }
}
