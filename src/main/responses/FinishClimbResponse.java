package main.responses;

import main.database.LadderConnectionDao;
import main.database.LadderConnectionDto;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.ClimbRequest;
import main.requests.Request;

public class FinishClimbResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ClimbRequest))
			return;
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		ClimbRequest request = (ClimbRequest)req;
		
		if (!PathFinder.isNextTo(player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			for (LadderConnectionDto dto : LadderConnectionDao.getLadderConnections()) {
				if (dto.getFromTileId() == request.getTileId()) {
					PlayerUpdateResponse playerUpdate = (PlayerUpdateResponse)ResponseFactory.create("player_update");
					playerUpdate.setId(player.getDto().getId());
					playerUpdate.setTile(dto.getToTileId());
					player.setTileId(dto.getToTileId());
					responseMaps.addBroadcastResponse(playerUpdate);
					break;
				}
			}
			player.setState(PlayerState.idle);
		}
	}

}
