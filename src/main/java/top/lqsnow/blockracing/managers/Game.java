package top.lqsnow.blockracing.managers;

import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.core.TeamColor;

/**
 * Transitional facade over the game instance. Every member delegates to
 * {@link GameManager#first()}; this class is removed once all call sites
 * address the instance directly.
 */
@Deprecated
public final class Game {
    public enum GameState {PREGAME, INGAME, END}

    private Game() {
    }

    private static GameInstance arena() {
        return GameManager.first();
    }

    public static GameState getCurrentGameState() {
        return switch (arena().getState()) {
            case WAITING -> GameState.PREGAME;
            case RUNNING -> GameState.INGAME;
            case ENDING, DISPOSED -> GameState.END;
        };
    }

    public static void setCurrentGameState(GameState state) {
        arena().setState(switch (state) {
            case PREGAME -> RoomState.WAITING;
            case INGAME -> RoomState.RUNNING;
            case END -> RoomState.ENDING;
        });
    }

    // ------------------------------------------------------------------
    // State accessors (former public static fields)
    // ------------------------------------------------------------------

    public static int redTeamScore() {
        return arena().getScore(TeamColor.RED);
    }

    public static void setRedTeamScore(int score) {
        arena().setScore(TeamColor.RED, score);
    }

    public static int blueTeamScore() {
        return arena().getScore(TeamColor.BLUE);
    }

    public static void setBlueTeamScore(int score) {
        arena().setScore(TeamColor.BLUE, score);
    }

    public static int redTeamCurrentBlockAmount() {
        return arena().getCurrentAmount(TeamColor.RED);
    }

    public static void setRedTeamCurrentBlockAmount(int amount) {
        arena().setCurrentAmount(TeamColor.RED, amount);
    }

    public static int blueTeamCurrentBlockAmount() {
        return arena().getCurrentAmount(TeamColor.BLUE);
    }

    public static void setBlueTeamCurrentBlockAmount(int amount) {
        arena().setCurrentAmount(TeamColor.BLUE, amount);
    }

    public static int redTeamTotalBlockAmount() {
        return arena().getTotalAmount(TeamColor.RED);
    }

    public static void setRedTeamTotalBlockAmount(int amount) {
        arena().setTotalAmount(TeamColor.RED, amount);
    }

    public static int blueTeamTotalBlockAmount() {
        return arena().getTotalAmount(TeamColor.BLUE);
    }

    public static void setBlueTeamTotalBlockAmount(int amount) {
        arena().setTotalAmount(TeamColor.BLUE, amount);
    }

    public static int redTeamRollCount() {
        return arena().getRollCount(TeamColor.RED);
    }

    public static void setRedTeamRollCount(int count) {
        arena().setRollCount(TeamColor.RED, count);
    }

    public static int blueTeamRollCount() {
        return arena().getRollCount(TeamColor.BLUE);
    }

    public static void setBlueTeamRollCount(int count) {
        arena().setRollCount(TeamColor.BLUE, count);
    }

    public static int locateCost() {
        return arena().getLocateCost();
    }

    public static void setLocateCost(int cost) {
        arena().setLocateCost(cost);
    }

    public static List<Inventory> redTeamChest() {
        return arena().chests(TeamColor.RED);
    }

    public static List<Inventory> blueTeamChest() {
        return arena().chests(TeamColor.BLUE);
    }

    public static Map<Integer, Location> redWaypoint() {
        return arena().waypoints(TeamColor.RED);
    }

    public static Map<Integer, Location> blueWaypoint() {
        return arena().waypoints(TeamColor.BLUE);
    }

    public static Map<Integer, Material> redWaypointIconCache() {
        return arena().waypointIcons(TeamColor.RED);
    }

    public static Map<Integer, Material> blueWaypointIconCache() {
        return arena().waypointIcons(TeamColor.BLUE);
    }

    public static List<String> freeRandomTPList() {
        return arena().getFreeRandomTp();
    }

    public static List<String> locateCommandPermission() {
        return arena().getLocatePermission();
    }

    public static List<String> inGamePlayers() {
        return arena().getInGamePlayers();
    }

    public static Map<String, Integer> collectAmount() {
        return arena().getCollectAmount();
    }

    public static List<String> redTeamRollPlayers() {
        return arena().rollPlayers(TeamColor.RED);
    }

    public static List<String> blueRollPlayers() {
        return arena().rollPlayers(TeamColor.BLUE);
    }

    // ------------------------------------------------------------------
    // Behaviour
    // ------------------------------------------------------------------

    public static void playerLogin(Player player) {
        arena().playerLogin(player);
    }

    public static void playerQuit(Player player) {
        arena().playerQuit(player);
    }

    public static void playerReady(Player player) {
        arena().playerReady(player);
    }

    public static void checkStartDemands(Player player) {
        arena().checkStartDemands(player);
    }

    public static void startGame() {
        arena().start();
    }

    public static void resumeRecoveredGame() {
        arena().resumeRecovered();
    }

    public static void initPlayer(Player player) {
        arena().initPlayer(player);
    }

    public static void roll(Player player) {
        arena().roll(player);
    }

    public static void locate(Player player) {
        arena().locate(player);
    }

    public static void buySupply(Player buyer) {
        arena().buySupply(buyer);
    }

    public static void randomTeleport(Player player, boolean avoidOcean) {
        arena().randomTeleport(player, avoidOcean);
    }

    public static boolean waypoint(Player player, int index, ClickType clickType) {
        return arena().waypoint(player, index, clickType);
    }

    public static void resetPregameInventory(Player player) {
        arena().resetPregameInventory(player);
    }

    public static List<String> getCurrentBlocks(String team) {
        TeamColor color = TeamColor.fromName(team);
        if (color == null) {
            throw new IllegalStateException("Unexpected value: " + team);
        }
        return arena().currentBlocks(color);
    }

    public static List<String> getOnlinePlayersString() {
        return arena().getOnlinePlayerNames();
    }

    public static List<String> getOnlineTeamPlayers(String team) {
        TeamColor color = TeamColor.fromName(team);
        if (color == null) {
            throw new IllegalStateException("Unexpected value: " + team);
        }
        return arena().getTeams().onlinePlayers(color);
    }

    public static String getCoords(Location location) {
        return arena().getCoords(location);
    }

    public static void redTaskComplete(String block, String player) {
        arena().collectBlock(TeamColor.RED, block, player);
    }

    public static void blueTaskComplete(String block, String player) {
        arena().collectBlock(TeamColor.BLUE, block, player);
    }

    public static void redWin() {
        arena().end(TeamColor.RED);
    }

    public static void blueWin() {
        arena().end(TeamColor.BLUE);
    }

    public static void playSound(Sound sound) {
        arena().playSound(sound);
    }

    public static void addRandomTeleportCandidate(Location location) {
        top.lqsnow.blockracing.game.RandomTeleportService.addCandidate(location);
    }

    public static Location pollRandomTeleportCandidate() {
        return top.lqsnow.blockracing.game.RandomTeleportService.pollCandidate();
    }

    public static int getRandomTeleportPoolSize() {
        return top.lqsnow.blockracing.game.RandomTeleportService.poolSize();
    }

    public static List<Location> getRandomTeleportPoolSnapshot() {
        return top.lqsnow.blockracing.game.RandomTeleportService.snapshot();
    }
}
