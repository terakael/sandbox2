package processing.scenery;

import processing.attackable.Player;
import requests.UseRequest;
import responses.ResponseMaps;

public interface Scenery {
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps);
}
