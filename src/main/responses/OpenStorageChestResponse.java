package main.responses;

import java.util.List;

import main.database.dto.InventoryItemDto;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.OpenRequest;
import main.requests.Request;

public class OpenStorageChestResponse extends Response {
	private List<InventoryItemDto> items = null;
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		OpenRequest request = (OpenRequest)req;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(request.getTileId(), responseMaps);
			setRecoAndResponseText(0, "TODO");
			responseMaps.addClientOnlyResponse(player, this);
		}
	}
}
