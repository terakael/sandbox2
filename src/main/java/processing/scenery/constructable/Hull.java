package processing.scenery.constructable;

import database.dto.ConstructableDto;
import database.dto.ShipDto;
import processing.managers.ShipManager;
import responses.ResponseMaps;

public abstract class Hull extends Constructable {	
	public Hull(int playerId, int floor, int tileId, int lifetimeTicks, ConstructableDto constructableDto, boolean onHousingTile, ShipDto shipDto, ResponseMaps responseMaps) {
		super(playerId, floor, tileId, lifetimeTicks, constructableDto, onHousingTile, responseMaps);
		ShipManager.addHull(floor, tileId, playerId, shipDto, responseMaps);
	}
	
	@Override
	public void onDestroy(ResponseMaps responseMaps) {
		ShipManager.removeHull(floor, tileId, responseMaps);
	}
}
