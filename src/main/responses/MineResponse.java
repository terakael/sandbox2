package main.responses;

import main.database.MineableDao;
import main.database.MineableDto;
import main.database.PlayerInventoryDao;
import main.database.StatsDao;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.MineRequest;
import main.requests.Request;

public class MineResponse extends Response {
	public MineResponse() {
		setAction("mine");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof MineRequest))
			return;
		
		MineRequest request = (MineRequest)req;
		
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
			if (StatsDao.getStatLevelByStatIdPlayerId(6, player.getId()) < mineable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d mining to mine this.", mineable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// TODO does player have inventory space
			if (PlayerInventoryDao.getFreeSlotByPlayerId(player.getId()) == -1) {
				setRecoAndResponseText(0, "your inventory is too full to mine anymore.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			//setRecoAndResponseText(0, "you got this far in the mineable, nice");
			StartMiningResponse miningStart = (StartMiningResponse)ResponseFactory.create("start_mining");
			miningStart.process(request, player, responseMaps);
			
			player.setState(PlayerState.mining);
			player.setSavedRequest(req);
			player.setTickCounter(5);
			
//			responseMaps.addClientOnlyResponse(player, this);
		}
	}

}
