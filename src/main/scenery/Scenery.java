package main.scenery;

import main.processing.Player;
import main.responses.ResponseMaps;

public abstract class Scenery {
	abstract public boolean use(int srcItemId, int slot, Player player, ResponseMaps responseMaps);
}
