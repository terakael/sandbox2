package responses;

import database.dao.ConstructableDao;
import database.dao.ItemDao;
import database.dao.SceneryDao;
import database.dto.ConstructableDto;
import processing.attackable.Player;
import requests.Request;
import requests.ShowConstructionMaterialsRequest;

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
			message += String.format("%dx %s, ", dto.getPlankAmount(), ItemDao.getNameFromId(dto.getPlankId(), dto.getPlankAmount() != 1));
		if (dto.getBarAmount() > 0)
			message += String.format("%dx %s, ", dto.getBarAmount(), ItemDao.getNameFromId(dto.getBarId(), dto.getBarAmount() != 1));
		if (dto.getTertiaryAmount() > 0)
			message += String.format("%dx %s, ", dto.getTertiaryAmount(), ItemDao.getNameFromId(dto.getTertiaryId(), dto.getTertiaryAmount() != 1));
		
		return message.substring(0, message.length() - 2);
	}

}
