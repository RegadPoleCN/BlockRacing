package top.lqsnow.blockracing.game;

import top.lqsnow.blockracing.managers.Config;
import top.lqsnow.blockracing.pools.DifficultyPreset;

/** Settings of one game room. Values are seeded from config.yml when the room
 * is created and are written back to config.yml on change, mirroring the
 * previous global behaviour. */
public final class RoomSettings {
    private DifficultyPreset.Kind difficulty = DifficultyPreset.Kind.NORMAL;
    private boolean copperExtras;
    private boolean enableMediumBlock;
    private boolean enableHardBlock;
    private boolean enableDyedBlock;
    private boolean enableEndBlock;
    private int blockAmount;
    private int maxTeamChestNum;
    private int maxTeamWaypointNum;
    private int maxRollCount;
    private boolean speedMode;
    private GameModeType gameMode = GameModeType.NORMAL;

    public static RoomSettings fromConfig() {
        RoomSettings settings = new RoomSettings();
        settings.loadFromConfig();
        return settings;
    }

    /** Bare instance without config backing, for tests and programmatic rooms. */
    public static RoomSettings fromRaw(int blockAmount, GameModeType gameMode) {
        RoomSettings settings = new RoomSettings();
        settings.blockAmount = blockAmount;
        settings.gameMode = gameMode == null ? GameModeType.NORMAL : gameMode;
        return settings;
    }

    public void loadFromConfig() {
        try {
            difficulty = DifficultyPreset.Kind.parse(Config.DIFFICULTY.getString());
        } catch (IllegalArgumentException | NullPointerException ex) {
            difficulty = DifficultyPreset.Kind.NORMAL;
        }
        copperExtras = Config.COPPER_EXTRAS.getBoolean();
        enableMediumBlock = Config.MEDIUM_BLOCK.getBoolean();
        enableHardBlock = Config.HARD_BLOCK.getBoolean();
        enableDyedBlock = Config.DYED_BLOCK.getBoolean();
        enableEndBlock = Config.END_BLOCK.getBoolean();
        blockAmount = Math.max(10, Config.BLOCK_AMOUNT.getInt());
        speedMode = Config.SPEED_MODE.getBoolean();
        maxTeamChestNum = Math.max(1, Math.min(53, Config.MAX_TEAM_CHEST_NUM.getInt()));
        maxTeamWaypointNum = Math.max(1, Math.min(53, Config.MAX_TEAM_WAYPOINT_NUM.getInt()));
        maxRollCount = Math.max(0, Config.MAX_ROLL_COUNT.getInt());
        try {
            gameMode = GameModeType.valueOf(Config.GAME_MODE.getString().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            gameMode = GameModeType.NORMAL;
        }
    }

    public boolean isEnableMediumBlock() {
        return enableMediumBlock;
    }

    public boolean isEnableHardBlock() {
        return enableHardBlock;
    }

    public boolean isEnableDyedBlock() {
        return enableDyedBlock;
    }

    public boolean isEnableEndBlock() {
        return enableEndBlock;
    }

    public int getBlockAmount() {
        return blockAmount;
    }

    public int getMaxTeamChestNum() {
        return maxTeamChestNum;
    }

    public int getMaxTeamWaypointNum() {
        return maxTeamWaypointNum;
    }

    public int getMaxRollCount() {
        return maxRollCount;
    }

    public boolean isSpeedMode() {
        return speedMode;
    }

    public GameModeType getGameMode() {
        return gameMode;
    }

    public DifficultyPreset.Kind getDifficulty() {
        return difficulty;
    }

    public boolean isCopperExtras() {
        return copperExtras;
    }

    public void setDifficulty(DifficultyPreset.Kind value) {
        java.util.Objects.requireNonNull(value);
        difficulty = value;
        Config.DIFFICULTY.setString(value.id());
        Config.saveConfig();
    }

    public void setCopperExtras(boolean value) {
        copperExtras = value;
        Config.COPPER_EXTRAS.setBoolean(value);
        Config.saveConfig();
    }

    public void setEnableMediumBlock(boolean value) {
        enableMediumBlock = value;
        Config.MEDIUM_BLOCK.setBoolean(value);
    }

    public void setEnableHardBlock(boolean value) {
        enableHardBlock = value;
        Config.HARD_BLOCK.setBoolean(value);
    }

    public void setEnableDyedBlock(boolean value) {
        enableDyedBlock = value;
        Config.DYED_BLOCK.setBoolean(value);
    }

    public void setEnableEndBlock(boolean value) {
        enableEndBlock = value;
        Config.END_BLOCK.setBoolean(value);
    }

    public void setBlockAmount(int value) {
        blockAmount = value;
        Config.BLOCK_AMOUNT.setInt(value);
    }

    public void setSpeedMode(boolean value) {
        speedMode = value;
        Config.SPEED_MODE.setBoolean(value);
    }

    public void setGameMode(GameModeType value) {
        gameMode = value;
        Config.GAME_MODE.setString(value.name().toLowerCase());
    }

    public void toggleMediumBlock() {
        setEnableMediumBlock(!enableMediumBlock);
    }

    public void toggleHardBlock() {
        setEnableHardBlock(!enableHardBlock);
    }

    public void toggleDyedBlock() {
        setEnableDyedBlock(!enableDyedBlock);
    }

    public void toggleEndBlock() {
        setEnableEndBlock(!enableEndBlock);
    }

    public void toggleSpeedMode() {
        setSpeedMode(!speedMode);
    }
}
