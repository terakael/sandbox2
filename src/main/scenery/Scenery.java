package main.scenery;

import main.processing.Player;
import main.requests.UseRequest;
import main.responses.ResponseMaps;

public interface Scenery {
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps);
}
