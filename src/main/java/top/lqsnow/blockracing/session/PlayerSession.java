package top.lqsnow.blockracing.session;

import java.util.Collection;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

/** Snapshot of a player's state before entering a game, restored when they
 * return to the lobby. */
public final class PlayerSession {
    private final ItemStack[] inventory;
    private final ItemStack[] armor;
    private final ItemStack offHand;
    private final GameMode gameMode;
    private final double health;
    private final int food;
    private final float saturation;
    private final int level;
    private final float exp;
    private final Collection<PotionEffect> effects;

    private PlayerSession(ItemStack[] inventory, ItemStack[] armor, ItemStack offHand, GameMode gameMode,
                          double health, int food, float saturation, int level, float exp,
                          Collection<PotionEffect> effects) {
        this.inventory = inventory;
        this.armor = armor;
        this.offHand = offHand;
        this.gameMode = gameMode;
        this.health = health;
        this.food = food;
        this.saturation = saturation;
        this.level = level;
        this.exp = exp;
        this.effects = effects;
    }

    public static PlayerSession snapshot(Player player) {
        return new PlayerSession(
                player.getInventory().getContents(),
                player.getInventory().getArmorContents(),
                player.getInventory().getItemInOffHand(),
                player.getGameMode(),
                player.getHealth(),
                player.getFoodLevel(),
                player.getSaturation(),
                player.getLevel(),
                player.getExp(),
                player.getActivePotionEffects());
    }

    public void restore(Player player) {
        player.getInventory().clear();
        player.getInventory().setContents(inventory);
        player.getInventory().setArmorContents(armor);
        player.getInventory().setItemInOffHand(offHand);
        player.setGameMode(gameMode);
        player.setHealth(Math.min(health, player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue()));
        player.setFoodLevel(food);
        player.setSaturation(saturation);
        player.setLevel(level);
        player.setExp(exp);
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        for (PotionEffect effect : effects) {
            player.addPotionEffect(effect);
        }
    }
}
