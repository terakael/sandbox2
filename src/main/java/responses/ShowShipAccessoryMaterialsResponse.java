package responses;

import database.dao.ItemDao;
import database.dao.ShipAccessoryDao;
import database.dto.ShipAccessoryDto;
import processing.attackable.Player;
import requests.Request;
import requests.ShowShipAccessoryMaterialsRequest;

public class ShowShipAccessoryMaterialsResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		ShowShipAccessoryMaterialsRequest request = (ShowShipAccessoryMaterialsRequest)req;
		
		final ShipAccessoryDto accessory = ShipAccessoryDao.getShipAccessories().stream()
				.filter(e -> e.getId() == request.getAccessoryId())
				.findFirst()
				.orElse(null);
		
		if (accessory == null)
			return;
		
		String message = String.format("%s requires: %s.", accessory.getName(), compileMaterialList(accessory));
		
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, "white"));
	}
	
	public static String compileMaterialList(ShipAccessoryDto dto) {
		String message = "";
		if (dto.getPrimaryMaterialCount() > 0)
			message += String.format("%dx %s, ", dto.getPrimaryMaterialCount(), ItemDao.getNameFromId(dto.getPrimaryMaterialId(), dto.getPrimaryMaterialCount() != 1));
		if (dto.getSecondaryMaterialCount() > 0)
			message += String.format("%dx %s, ", dto.getSecondaryMaterialCount(), ItemDao.getNameFromId(dto.getSecondaryMaterialId(), dto.getSecondaryMaterialCount() != 1));
		
		return message.substring(0, message.length() - 2);
	}

}
