package main.responses;

import java.util.Stack;

import lombok.Setter;
import main.database.DoorDao;
import main.database.DoorDto;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.OpenCloseRequest;
import main.requests.Request;

public class OpenCloseResponse extends Response {
	@Setter private int tileId;
	
	public OpenCloseResponse() {
		setAction("open");
	}
	
	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		OpenCloseRequest request = (OpenCloseRequest)req;
		
		// does the tile a door on it?
		DoorDto door = DoorDao.getDoorDtoByTileId(player.getFloor(), request.getTileId());
		if (door == null) {
			// this can technically happen when the player clicks a ladder, then right-clicks a door
			// then finally selects "open" after they have switched rooms.  Do not do anything in this case.
			return;
		}
		tileId = request.getTileId();
		
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), tileId, false)) {
			Stack<Integer> path = PathFinder.findPathToDoor(player.getFloor(), player.getTileId(), tileId);
			// empty check because it's not guaranteed that there is a path between the player and the door.
			if (!path.isEmpty()) {
				player.setPath(path);
				player.setState(PlayerState.walking);
				player.setSavedRequest(req);
			}
			return;
		} else {			
			player.faceDirection(request.getTileId(), responseMaps);
			
			DoorDao.toggleDoor(player.getFloor(), tileId);
			responseMaps.addLocalResponse(player.getFloor(), tileId, this);
		}
	}
}
