package me.contaria.seedqueue.compat;

import dev.tildejustin.stateoutput.State;
import dev.tildejustin.stateoutput.StateOutputHelper;

public class StateOutputCompat {
    static void setWallState() {
        StateOutputHelper.outputState(State.WALL);
    }
}
