package top.lqsnow.blockracing.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import top.lqsnow.blockracing.core.TeamColor;

/** Per-game target block assignment and remaining progress. */
public final class BlockState {
    private final List<String> redAll = new ArrayList<>();
    private final List<String> blueAll = new ArrayList<>();
    private final List<String> redRemaining = new ArrayList<>();
    private final List<String> blueRemaining = new ArrayList<>();

    public void setup(RoomSettings settings, BlockCatalog catalog, Random random) {
        redAll.clear();
        blueAll.clear();
        redRemaining.clear();
        blueRemaining.clear();

        List<String> pool = catalog.pool(settings);
        if (settings.getGameMode() == GameModeType.NORMAL) {
            redAll.addAll(generateBlocks(settings, catalog, pool, random));
            blueAll.addAll(generateBlocks(settings, catalog, pool, random));
        } else {
            List<String> blocks = generateBlocks(settings, catalog, pool, random);
            redAll.addAll(blocks);
            blueAll.addAll(blocks);
        }
        redRemaining.addAll(redAll);
        blueRemaining.addAll(blueAll);
    }

    private List<String> generateBlocks(RoomSettings settings, BlockCatalog catalog,
                                        List<String> pool, Random random) {
        List<String> blocksTemp = new ArrayList<>(pool);
        List<String> easyTemp = new ArrayList<>(catalog.getEasyBlocks());
        List<String> mediumTemp = new ArrayList<>(catalog.getMediumBlocks());
        List<String> hardTemp = new ArrayList<>(catalog.getHardBlocks());
        List<String> dyedTemp = new ArrayList<>(catalog.getDyedBlocks());
        List<String> endTemp = new ArrayList<>(catalog.getEndBlocks());

        int blockAmount = settings.getBlockAmount();
        List<String> targetBlocks = new ArrayList<>();

        for (int i = 0; i < blockAmount; i++) {
            int easyWeight = 0;
            int mediumWeight = 0;
            int hardWeight = 0;
            int dyedWeight = 0;
            int endWeight = 0;

            if (easyTemp.size() != 0) easyWeight = BlockCatalog.calculateEasyBlocksWeight((float) i / blockAmount);

            if (mediumTemp.size() != 0)
                mediumWeight = settings.isEnableMediumBlock() ? BlockCatalog.calculateMediumBlocksWeight((float) i / blockAmount) : 0;

            if (hardTemp.size() != 0)
                hardWeight = settings.isEnableHardBlock() ? BlockCatalog.calculateHardBlocksWeight((float) i / blockAmount) : 0;

            if (dyedTemp.size() != 0)
                dyedWeight = settings.isEnableDyedBlock() ? BlockCatalog.calculateDyedBlocksWeight((float) i / blockAmount) : 0;

            if (endTemp.size() != 0)
                endWeight = settings.isEnableEndBlock()
                        ? BlockCatalog.calculateEndBlocksWeight((float) i / blockAmount, endTemp.size(), pool.size()) : 0;

            String difficulty = chooseDifficulty(random, easyWeight, mediumWeight, hardWeight, dyedWeight, endWeight);
            String selectedBlock = selectBlock(difficulty, easyTemp, mediumTemp, hardTemp, dyedTemp, endTemp, random);
            targetBlocks.add(selectedBlock);

            switch (difficulty) {
                case "easy" -> easyTemp.remove(selectedBlock);
                case "medium" -> mediumTemp.remove(selectedBlock);
                case "hard" -> hardTemp.remove(selectedBlock);
                case "dyed" -> dyedTemp.remove(selectedBlock);
                case "end" -> endTemp.remove(selectedBlock);
                default -> throw new IllegalArgumentException("Invalid difficulty");
            }

            blocksTemp.remove(selectedBlock);
        }

        return targetBlocks;
    }

    private String chooseDifficulty(Random random, int easyWeight, int mediumWeight, int hardWeight,
                                    int dyedWeight, int endWeight) {
        int totalWeight = easyWeight + mediumWeight + hardWeight + dyedWeight + endWeight;
        int randomNumber = random.nextInt(totalWeight);

        if (randomNumber < easyWeight) {
            return "easy";
        } else if (randomNumber < easyWeight + mediumWeight) {
            return "medium";
        } else if (randomNumber < easyWeight + mediumWeight + hardWeight) {
            return "hard";
        } else if (randomNumber < easyWeight + mediumWeight + hardWeight + dyedWeight) {
            return "dyed";
        } else {
            return "end";
        }
    }

    private String selectBlock(String difficulty, List<String> easyTemp, List<String> mediumTemp,
                               List<String> hardTemp, List<String> dyedTemp, List<String> endTemp, Random random) {
        return switch (difficulty) {
            case "easy" -> selectRandomBlockFromList(easyTemp, random);
            case "medium" -> selectRandomBlockFromList(mediumTemp, random);
            case "hard" -> selectRandomBlockFromList(hardTemp, random);
            case "dyed" -> selectRandomBlockFromList(dyedTemp, random);
            case "end" -> selectRandomBlockFromList(endTemp, random);
            default -> throw new IllegalArgumentException("Invalid difficulty");
        };
    }

    private String selectRandomBlockFromList(List<String> blockList, Random random) {
        int randomIndex = random.nextInt(blockList.size());
        return blockList.get(randomIndex);
    }

    public List<String> all(TeamColor team) {
        return team == TeamColor.RED ? redAll : blueAll;
    }

    public List<String> remaining(TeamColor team) {
        return team == TeamColor.RED ? redRemaining : blueRemaining;
    }

    /** The blocks currently hunted by the team (up to 4). */
    public List<String> current(TeamColor team) {
        List<String> remaining = remaining(team);
        return remaining.subList(0, Math.min(remaining.size(), 4));
    }

    public void complete(TeamColor team, String block) {
        remaining(team).remove(block);
    }

    public boolean hasFinished(TeamColor team) {
        return remaining(team).isEmpty();
    }
}
