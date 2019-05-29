package main.scenery;

import main.processing.Player;
import main.responses.ResponseMaps;

public abstract class Scenery {
	abstract public boolean use(int srcItemId, Player player, ResponseMaps responseMaps);
}
