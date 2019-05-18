package main.responses;

import javax.websocket.Session;

import main.database.MineableDao;
import main.database.MineableDto;
import main.database.PlayerInventoryDao;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.WorldProcessor;
import main.processing.Player.PlayerState;
import main.requests.MineRequest;
import main.requests.Request;

public class MineResponse extends Response {
	public MineResponse(String action) {
		super(action);
	}

	@Override
	public void process(Request req, Session client, ResponseMaps responseMaps) {
		if (!(req instanceof MineRequest))
			return;
		
		MineRequest request = (MineRequest)req;
		
		Player player = WorldProcessor.playerSessions.get(client);
		
		if (!PathFinder.isNextTo(player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			// does the tile have something mineable on it?
			MineableDto mineable = MineableDao.getMineableDtoByTileId(request.getTileId());
			if (mineable == null) {
				setRecoAndResponseText(0, "you can't mine this.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// TODO does the player have the level to mine this?
			
			// TODO does player have inventory space
			if (PlayerInventoryDao.getFreeSlotByPlayerId(player.getId()) == -1) {
				setRecoAndResponseText(0, "your inventory is too full to mine anymore.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			//setRecoAndResponseText(0, "you got this far in the mineable, nice");
			StartMiningResponse miningStart = new StartMiningResponse("start_mining");
			miningStart.process(request, client, responseMaps);
			
			player.setState(PlayerState.mining);
			player.setSavedRequest(req);
			player.setTickCounter(5);
			
//			responseMaps.addClientOnlyResponse(player, this);
		}
	}

}
