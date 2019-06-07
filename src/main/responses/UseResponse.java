package main.responses;

import main.FightManager;
import main.database.SceneryDao;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.Request;
import main.requests.UseRequest;
import main.scenery.Scenery;
import main.scenery.SceneryManager;

public class UseResponse extends Response {

	public UseResponse() {
		setAction("use");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof UseRequest))
			return;
		
		UseRequest request = (UseRequest)req;
		switch (request.getType()) {
		case "scenery":
			if (handleUseOnScenery(request, player, responseMaps))
				return;
			break;
		default:
			break;
		}
		
		setRecoAndResponseText(0, "nothing interesting happens.");
		responseMaps.addClientOnlyResponse(player, this);
	}
	
	private boolean handleUseOnScenery(UseRequest request, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return true;
		}
		
		if (!PathFinder.isNextTo(player.getTileId(), request.getDest())) {
			player.setPath(PathFinder.findPath(player.getTileId(), request.getDest(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(request);
			return true;
		}
		
		int sceneryId = SceneryDao.getSceneryIdByTileId(request.getDest());
		Scenery scenery = SceneryManager.getScenery(sceneryId);
		if (scenery == null)
			return false;
		
		if (!scenery.use(request.getSrc(), player, responseMaps))
			return false;
		
		return true;
	}

}
