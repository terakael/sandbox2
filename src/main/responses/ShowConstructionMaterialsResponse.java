package main.responses;

import main.database.dao.ConstructableDao;
import main.database.dao.ItemDao;
import main.database.dao.SceneryDao;
import main.database.dto.ConstructableDto;
import main.processing.Player;
import main.requests.Request;
import main.requests.ShowConstructionMaterialsRequest;

public class ShowConstructionMaterialsResponse extends Response {
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		ShowConstructionMaterialsRequest request = (ShowConstructionMaterialsRequest)req;
		
		ConstructableDto constructable = ConstructableDao.getConstructableBySceneryId(request.getSceneryId());
		if (constructable == null)
			return;
		
		String message = String.format("%s requires: ", SceneryDao.getNameById(constructable.getResultingSceneryId()));
		if (constructable.getPlankAmount() > 0)
			message += String.format("%dx %s, ", constructable.getPlankAmount(), ItemDao.getNameFromId(constructable.getPlankId()));
		if (constructable.getBarAmount() > 0)
			message += String.format("%dx %s, ", constructable.getBarAmount(), ItemDao.getNameFromId(constructable.getBarId()));
		if (constructable.getTertiaryAmount() > 0)
			message += String.format("%dx %s, ", constructable.getTertiaryAmount(), ItemDao.getNameFromId(constructable.getTertiaryId()));
		
		message = message.substring(0, message.length() - 2) + ".";
		
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, "white"));
	}

}
