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
        if (arena.get().getState() == RoomState.RUNNING && MenuMemory.reopen(player)) return;
        openMainMenu(player);
    }

    public static void openMainMenu(Player player) {
        Optional<GameInstance> arena = GameManager.get().sessions().arenaOf(player);
        if (arena.isEmpty()) {
            new GameSelectorMenu().open(player);
            return;
        }
        if (arena.get().getState() == RoomState.RUNNING) new GameMenu(arena.get()).open(player);
        else new PreGameMenu(arena.get()).open(player);
    }

    public static void openTeamChestSelection(Player player) {
        Optional<GameInstance> arena = GameManager.get().sessions().arenaOf(player);
        if (arena.isEmpty() || arena.get().getState() != RoomState.RUNNING) {
            openMainMenu(player);
            return;
        }
        new GameMenu.TeamChestSelectMenu(arena.get()).open(player);
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
        var chests = arena.get().chests(color);
        if (index < 0 || index >= chests.size()) { openMainMenu(player); return; }
        TeamChestNavigation.prepare(chests.get(index));
        player.openInventory(chests.get(index));
        player.getOpenInventory().setTitle(color == TeamColor.RED
                ? Message.MENU_RED_CHEST.getString(player) + (index + 1)
                : Message.MENU_BLUE_CHEST.getString(player) + (index + 1));
        MenuMemory.recordChest(player, index);
    }

    // Update menu
    public static void updateMenu(MenuView menu) {
        MenuManager.refreshOpenMenus(menu.getClass());
    }
}
