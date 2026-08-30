package top.lqsnow.blockracing.lobby;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import top.lqsnow.blockracing.core.GameInstance;
import top.lqsnow.blockracing.core.GameManager;
import top.lqsnow.blockracing.core.RoomState;
import top.lqsnow.blockracing.core.TeamColor;
import top.lqsnow.blockracing.managers.Message;
import top.lqsnow.blockracing.toolkit.item.ItemBuilder;
import top.lqsnow.blockracing.toolkit.menu.MenuButton;
import top.lqsnow.blockracing.toolkit.menu.MenuView;

/** Hub menu listing running and waiting games, plus quick join. */
public final class GameSelectorMenu extends MenuView {

    public GameSelectorMenu() {
        super(27, player -> Message.MENU_SELECTOR_TITLE.getString(player));

        setButton(11, MenuButton.of(
                player -> ItemBuilder.of(Material.NETHER_STAR)
                        .name(Message.MENU_SELECTOR_QUICK_JOIN.getString(player))
                        .lore(Message.MENU_SELECTOR_QUICK_JOIN_LORE.getStringList(player))
                        .build(),
                (player, click) -> GameManager.get().findJoinable().join(player)
        ));

        List<GameInstance> arenas = new ArrayList<>(GameManager.get().all());
        int slot = 13;
        for (GameInstance arena : arenas) {
            if (slot > 15) {
                break;
            }
            GameInstance current = arena;
            setButton(slot, MenuButton.of(
                    player -> gameItem(player, current),
                    (player, click) -> current.join(player)
            ));
            slot++;
        }
    }

    private ItemStack gameItem(Player player, GameInstance arena) {
        String state = switch (arena.getState()) {
            case WAITING -> Message.NOTICE_GAME_NOT_START.getString(player);
            case RUNNING -> Message.NOTICE_GAME_HAS_START.getString(player);
            default -> arena.getState().name();
        };
        return ItemBuilder.of(arena.getState() == RoomState.RUNNING ? Material.GRASS_BLOCK : Material.CRAFTING_TABLE)
                .name(Message.MENU_SELECTOR_GAME_NAME.getString(player)
                        .replace("%id%", String.valueOf(arena.getId())))
                .lore(Message.MENU_SELECTOR_GAME_LORE.getStringList(player).stream()
                        .map(line -> line
                                .replace("%state%", state)
                                .replace("%players%", String.valueOf(arena.playerCount()))
                                .replace("%max%", String.valueOf(
                                        top.lqsnow.blockracing.managers.Config.MAX_PLAYERS.getInt())))
                        .toList())
                .build();
    }

    @Override
    protected ItemStack getBackgroundItem(int slot, Player player) {
        return ItemBuilder.of(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
    }
}
