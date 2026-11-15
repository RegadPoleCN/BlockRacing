package top.lqsnow.blockracing.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.lqsnow.blockracing.Main;
import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.managers.Block;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.managers.Scoreboard;
import top.lqsnow.blockracing.managers.TeamChat;
import top.lqsnow.blockracing.menus.LanguageMenu;
import top.lqsnow.blockracing.menus.PreGameMenu;

import java.util.Optional;

import static top.lqsnow.blockracing.managers.Gui.updateMenu;
import static top.lqsnow.blockracing.managers.Scoreboard.updateScoreboard;

/** Player lifecycle events routed through the session manager. Lobby-world
 * protection lives in {@link top.lqsnow.blockracing.lobby.LobbyListener}. */
public class ArenaListener implements Listener {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private static java.util.Set<String> editAmountPlayers() {
        return GameManager.get().sessions().getEditAmountPlayers();
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Optional<GameInstance> arena = GameManager.get().sessions().arenaOf(player);
        if (arena.isPresent()) {
            // Rejoin (also mid-game); the arena keeps the binding across disconnects.
            arena.get().playerLogin(player);
        } else {
            top.lqsnow.blockracing.lobby.Lobby.get().joinAsFirst(player);
        }
        if (top.lqsnow.blockracing.managers.LanguageManager.registerFirstJoin(player)) {
            LanguageMenu.sendFirstJoinPrompt(player);
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        GameManager.get().sessions().arenaOf(player)
                .ifPresent(a -> a.playerQuit(player));
        Scoreboard.removePlayer(player);
    }

    @EventHandler
    private void onPlayerLocaleChange(PlayerLocaleChangeEvent event) {
        Bukkit.getScheduler().runTask(Main.getInstance(),
                () -> Scoreboard.refreshPlayer(event.getPlayer()));
    }

    @EventHandler
    private void onPlayerSwapHand(PlayerSwapHandItemsEvent event) {
        // Open menu
        if (event.getPlayer().isSneaking()) {
            event.setCancelled(true);
            top.lqsnow.blockracing.managers.Gui.openMenu(event.getPlayer());
        }
    }

    @EventHandler
    private void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // Change block amount
        if (editAmountPlayers().contains(player.getName())) {
            event.setCancelled(true);
            String message = LEGACY_SERIALIZER.serialize(event.message());
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> handleBlockAmountInput(player, message));
            return;
        }

        TeamChat.handle(event);
    }

    private void handleBlockAmountInput(Player player, String message) {
        if (!editAmountPlayers().contains(player.getName())) return;
        Optional<GameInstance> arena = GameManager.get().sessions().arenaOf(player);
        if (arena.isEmpty() || arena.get().getState() != RoomState.WAITING) {
            editAmountPlayers().remove(player.getName());
            return;
        }
        if (message.equalsIgnoreCase("quit")) {
            player.sendMessage(Message.NOTICE_SET_BLOCKS_QUIT.getString(player));
            editAmountPlayers().remove(player.getName());
            return;
        }
        try {
            setBlockAmount(player, Integer.parseInt(message), true);
            editAmountPlayers().remove(player.getName());
        } catch (NumberFormatException ex) {
            player.sendMessage(Message.NOTICE_SET_BLOCKS_ERROR.getString(player));
        }
    }

    @EventHandler
    private void onPlayerRespawn(PlayerRespawnEvent event) {
        event.getPlayer().sendMessage(Message.NOTICE_SPAWN_PROTECT.getString(event.getPlayer()));
        Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
            Player player = event.getPlayer();
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 0, false, false));
            GameInstance arena = GameManager.get().sessions().arenaOf(player).orElse(null);
            if (arena != null && arena.getState() == RoomState.RUNNING && arena.getSettings().isSpeedMode()) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, -1, 4, false, false));
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 1, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, -1, 1, false, false));
        }, 10L);
    }

    public static void setBlockAmount(Player player, int blockAmount, Boolean sendMessage) {
        GameInstance arena = GameManager.get().sessions().arenaOf(player).orElse(GameManager.first());
        int capacity = Block.capacityFor(arena.getSettings());
        if (blockAmount < 10 || blockAmount > capacity) {
            // Capacity problems are reported instead of silently reducing the requested game length.
            arena.broadcast().all(Message.BLOCK_LIBRARY_AMOUNT_ERROR,
                    (viewer, text) -> text.replace("%capacity%", String.valueOf(capacity)));
            return;
        }
        if (sendMessage) {
            int finalBlockAmount = blockAmount;
            arena.broadcast().all(Message.NOTICE_SET_BLOCKS_SUCCESS,
                    (viewer, text) -> text + finalBlockAmount);
        }
        arena.getSettings().setBlockAmount(blockAmount);
        updateMenu(new PreGameMenu(arena));
        updateScoreboard();
    }
}
