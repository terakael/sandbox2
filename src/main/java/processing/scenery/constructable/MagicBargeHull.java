package processing.scenery.constructable;

import database.dao.ShipDao;
import database.dto.ConstructableDto;
import responses.ResponseMaps;

public class MagicBargeHull extends Hull {

	public MagicBargeHull(int playerId, int floor, int tileId, int lifetimeTicks, ConstructableDto dto, boolean onHousingTile, ResponseMaps responseMaps) {
		super(playerId, floor, tileId, lifetimeTicks, dto, onHousingTile, ShipDao.getDtoById(185), responseMaps);
	}

}
