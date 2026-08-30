package top.lqsnow.blockracing.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.lqsnow.blockracing.menus.GameMenu;
import top.lqsnow.blockracing.menus.PreGameMenu;
import top.lqsnow.blockracing.toolkit.menu.MenuManager;
import top.lqsnow.blockracing.toolkit.menu.MenuView;

public class Gui {
    public static void openMenu(Player player) {
        if (Game.getCurrentGameState().equals(Game.GameState.PREGAME)) new PreGameMenu().open(player);
        if (Game.getCurrentGameState().equals(Game.GameState.INGAME)) new GameMenu().open(player);
    }

    @SuppressWarnings("deprecation") // InventoryView has no component-based title setter in Paper 26.2.
    public static void openTeamChest(Player player, int index) {
        if (Team.redTeamPlayers().contains(player.getName())) {
            player.openInventory(Game.redTeamChest().get(index));
            player.getOpenInventory().setTitle(Message.MENU_RED_CHEST.getString(player) + (index + 1));
        } else if (Team.blueTeamPlayers().contains(player.getName())) {
            player.openInventory(Game.blueTeamChest().get(index));
            player.getOpenInventory().setTitle(Message.MENU_BLUE_CHEST.getString(player) + (index + 1));
        }
    }

    public static void closeAllPlayersMenu() {
        Bukkit.getOnlinePlayers().forEach((Player player) -> {
            player.closeInventory();
        });
    }

    // Update menu
    public static void updateMenu(MenuView menu) {
        MenuManager.refreshOpenMenus(menu.getClass());
    }
}
