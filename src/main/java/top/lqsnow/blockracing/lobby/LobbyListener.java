package top.lqsnow.blockracing.lobby;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

/** Keeps the lobby world safe: no building, damage, hunger, explosions or drops. */
public final class LobbyListener implements Listener {

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (Lobby.isLobbyWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (Lobby.isLobbyWorld(event.getBlock().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player
                && Lobby.isLobbyWorld(player.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player
                && Lobby.isLobbyWorld(player.getWorld())) {
            event.setCancelled(true);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        if (Lobby.isLobbyWorld(event.getLocation().getWorld())) {
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (Lobby.isLobbyWorld(event.getPlayer().getWorld())) {
            event.setCancelled(true);
        }
    }
}
