package top.lqsnow.blockracing.managers;

import java.util.Optional;

import org.bukkit.entity.Player;

import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.lobby.GameSelectorMenu;
import top.lqsnow.blockracing.menus.GameMenu;
import top.lqsnow.blockracing.menus.PreGameMenu;
import top.lqsnow.blockracing.toolkit.menu.MenuManager;
import top.lqsnow.blockracing.toolkit.menu.MenuView;

public class Gui {
    public static void openMenu(Player player) {
        Optional<GameInstance> arena = GameManager.get().sessions().arenaOf(player);
        if (arena.isEmpty()) {
            new GameSelectorMenu().open(player);
            return;
        }
        if (arena.get().getState() == RoomState.RUNNING) {
            new GameMenu(arena.get()).open(player);
        } else {
            new PreGameMenu(arena.get()).open(player);
        }
    }

    @SuppressWarnings("deprecation") // InventoryView has no component-based title setter in Paper 26.2.
    public static void openTeamChest(Player player, int index) {
        Optional<GameInstance> arena = GameManager.get().sessions().arenaOf(player);
        if (arena.isEmpty()) {
            return;
        }
        TeamColor color = arena.get().getTeams().teamOf(player.getName()).orElse(null);
        if (color == null) {
            return;
        }
        player.openInventory(arena.get().chests(color).get(index));
        player.getOpenInventory().setTitle(color == TeamColor.RED
                ? Message.MENU_RED_CHEST.getString(player) + (index + 1)
                : Message.MENU_BLUE_CHEST.getString(player) + (index + 1));
    }

    // Update menu
    public static void updateMenu(MenuView menu) {
        MenuManager.refreshOpenMenus(menu.getClass());
    }
}
