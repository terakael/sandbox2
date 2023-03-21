package responses;

import java.util.List;
import java.util.stream.IntStream;

import database.dto.HouseInfoDto;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.HousingManager;
import processing.managers.LocationManager;
import requests.GetHouseInfoRequest;
import requests.Request;

@SuppressWarnings("unused")
public class GetHouseInfoResponse extends Response {
	private HouseInfoDto houseInfo = null;
	private boolean showNextButton = true;
	private boolean showPrevButton = false;
	
	public GetHouseInfoResponse() {
		setAction("get_house_info");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		GetHouseInfoRequest request = (GetHouseInfoRequest)req;
		
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
		
		// direction: // 0=first, 1=prev, 2=next, 3=last
		if (request.getDirection() == 0 || request.getCurrentHouseId() == null) {
			// get first house in house seller's list
			houseInfo = portfolio.get(0);
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		final int currentIndex = IntStream.range(0, portfolio.size())
				.filter(i -> portfolio.get(i).getId() == request.getCurrentHouseId())
				.findFirst()
				.orElse(-1);
		
		if (currentIndex == -1) {
			// house doesn't exist?
			return;
		}
		
		if (currentIndex == 0 && (request.getDirection() == 0 || request.getDirection() == 1))
			return; // trying to go back before the beginning
		
		if (currentIndex == portfolio.size() - 1 && (request.getDirection() == 2 || request.getDirection() == 3))
			return; // trying to go past the end
		
		int nextIndex = currentIndex;
		switch (request.getDirection()) {
		case 0: // first
			nextIndex = 0;
			break;
		case 1: // prev
			nextIndex -= 1;
			break;
		case 2: // next
			nextIndex += 1;
			break;
		case 3: // last
			nextIndex = portfolio.size() - 1;
			break;
		case 4: // same
		default:
			break;
		}
		
		houseInfo = portfolio.get(nextIndex);
		showPrevButton = nextIndex > 0;
		showNextButton = nextIndex < portfolio.size() - 1;
		responseMaps.addClientOnlyResponse(player, this);
	}

}
