package processing.scenery;

import database.dao.SawmillableDao;
import database.dto.SawmillableDto;
import processing.attackable.Player;
import requests.UseRequest;
import responses.ResponseMaps;
import responses.SawmillResponse;

public class Sawmill implements Scenery {
	@Override
	public boolean use(UseRequest request, Player player, ResponseMaps responseMaps) {
		final SawmillableDto sawmillable = SawmillableDao.getSawmillableByLogId(request.getSrc());
		if (sawmillable == null)
			return false;
		
		new SawmillResponse(false).process(request, player, responseMaps);
		return true;
	}

}
