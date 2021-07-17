package main.responses;

import main.database.dao.ConstructableDao;
import main.database.dao.ItemDao;
import main.database.dao.SceneryDao;
import main.database.dto.ConstructableDto;
import main.processing.attackable.Player;
import main.requests.Request;
import main.requests.ShowConstructionMaterialsRequest;

public class ShowConstructionMaterialsResponse extends Response {
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		ShowConstructionMaterialsRequest request = (ShowConstructionMaterialsRequest)req;
		
		ConstructableDto constructable = ConstructableDao.getConstructableBySceneryId(request.getSceneryId());
		if (constructable == null)
			return;
		
		String message = String.format("%s requires: %s.", SceneryDao.getNameById(constructable.getResultingSceneryId()), compileMaterialList(constructable));
		
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, "white"));
	}
	
	public static String compileMaterialList(ConstructableDto dto) {
		String message = "";
		if (dto.getPlankAmount() > 0)
			message += String.format("%dx %s, ", dto.getPlankAmount(), ItemDao.getNameFromId(dto.getPlankId()));
		if (dto.getBarAmount() > 0)
			message += String.format("%dx %s, ", dto.getBarAmount(), ItemDao.getNameFromId(dto.getBarId()));
		if (dto.getTertiaryAmount() > 0)
			message += String.format("%dx %s, ", dto.getTertiaryAmount(), ItemDao.getNameFromId(dto.getTertiaryId()));
		
		return message.substring(0, message.length() - 2);
	}

}
