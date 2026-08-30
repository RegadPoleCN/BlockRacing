package top.lqsnow.blockracing.game;

import org.junit.jupiter.api.Test;

import top.lqsnow.blockracing.core.TeamColor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TeamStateTest {

    @Test
    void addPutsPlayerOnTeam() {
        TeamState teams = new TeamState();
        assertTrue(teams.add("Alice", TeamColor.RED));
        assertEquals(Optional.of(TeamColor.RED), teams.teamOf("Alice"));
    }

    @Test
    void addSwitchesTeamAutomatically() {
        TeamState teams = new TeamState();
        teams.add("Alice", TeamColor.RED);
        assertTrue(teams.add("Alice", TeamColor.BLUE));
        assertEquals(Optional.of(TeamColor.BLUE), teams.teamOf("Alice"));
        assertTrue(teams.players(TeamColor.RED).isEmpty());
    }

    @Test
    void addSameTeamTwiceIsRejected() {
        TeamState teams = new TeamState();
        teams.add("Alice", TeamColor.RED);
        assertFalse(teams.add("Alice", TeamColor.RED));
        assertEquals(1, teams.players(TeamColor.RED).size());
    }

    @Test
    void removeClearsMembership() {
        TeamState teams = new TeamState();
        teams.add("Alice", TeamColor.BLUE);
        assertTrue(teams.remove("Alice"));
        assertEquals(Optional.empty(), teams.teamOf("Alice"));
    }

    @Test
    void hasEmptyTeamDetectsMissingSide() {
        TeamState teams = new TeamState();
        teams.add("Alice", TeamColor.RED);
        assertTrue(teams.hasEmptyTeam());
        teams.add("Bob", TeamColor.BLUE);
        assertFalse(teams.hasEmptyTeam());
    }

    @Test
    void restoreReplacesMembership() {
        TeamState teams = new TeamState();
        teams.add("Alice", TeamColor.RED);
        teams.restore(List.of("X"), List.of("Y", "Z"));
        assertEquals(List.of("X"), teams.players(TeamColor.RED));
        assertEquals(List.of("Y", "Z"), teams.players(TeamColor.BLUE));
    }
}
