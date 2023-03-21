package responses;

import java.util.List;
import java.util.stream.IntStream;

import database.dto.HouseInfoDto;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.HousingManager;
import processing.managers.LocationManager;
import requests.BuyHouseRequest;
import requests.GetHouseInfoRequest;
import requests.Request;

public class BuyHouseResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		final int houseId = ((BuyHouseRequest)req).getHouseId();
		final NPC houseSeller = LocationManager.getLocalNpcs(player.getFloor(), player.getTileId(), 10).stream()
			.filter(npc -> npc.getId() == 62) // realtor
			.findFirst()
			.orElse(null);
		
		if (houseSeller == null) {
			// there's no house sellers nearby, seems like fuckery
			return;
		}
		
		final List<HouseInfoDto> portfolio = HousingManager.getPortfolioByHouseSellerInstanceId(houseSeller.getInstanceId());
		if (portfolio == null || portfolio.isEmpty()) {
			return;
		}
		
		final int houseIndex = IntStream.range(0, portfolio.size())
				.filter(i -> portfolio.get(i).getId() == houseId)
				.findFirst()
				.orElse(-1);
		
		if (houseIndex == -1)
			return;
		
		final HouseInfoDto house = portfolio.get(houseIndex);
		if (house == null || !house.isForSale())
			return; // this house seller ain't selling this house
		
		HousingManager.unassignHouseFromPlayer(player.getId());
		HousingManager.assignHouseToPlayer(houseId, player.getId());
		
		GetHouseInfoRequest getHouseInfoRequest = new GetHouseInfoRequest();
		getHouseInfoRequest.setCurrentHouseId(houseId);
		getHouseInfoRequest.setDirection(4); // don't change from the current house
		new GetHouseInfoResponse().process(getHouseInfoRequest, player, responseMaps);
	}

}
