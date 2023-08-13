package processing.scenery.constructable;

import database.dao.ShipDao;
import database.dto.ConstructableDto;
import responses.ResponseMaps;

public class CrudeHull extends Hull {

	public CrudeHull(int playerId, int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile, ResponseMaps responseMaps) {
		super(playerId, floor, tileId, lifetimeTicks, dto, onHousingTile, ShipDao.getDtoById(184), responseMaps);
	}

}
