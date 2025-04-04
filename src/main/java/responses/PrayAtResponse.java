package responses;

import database.dao.SceneryDao;
import database.dao.StatsDao;
import processing.PathFinder;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import requests.PrayAtRequest;
import requests.Request;
import types.Stats;

public class PrayAtResponse extends WalkAndDoResponse {
//	public PrayAtResponse() {
//		setAction("pray at");
//	}

//	@Override
//	public void process(Request req, Player player, ResponseMaps responseMaps) {
//		if (!(req instanceof PrayAtRequest))
//			return;
//		
//		PrayAtRequest request = (PrayAtRequest)req;
//		int altarId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
//		if (altarId == -1) {
//			// there's no alter here, bail.
//			return;
//		}
//		
//		// if they mess with the client code and try to make them pray at some random scenery, fuck with them by making them walk to the scenery lmao whatta madlad
//		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
//			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
//			player.setState(PlayerState.walking);
//			player.setSavedRequest(req);
//			return;
//		} else if (altarId == 106) {// bit fucky but that's the sceneryId of the altar
//			player.faceDirection(request.getTileId(), responseMaps);
//			int maxPrayer = StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, player.getId());
//			
//			if (player.getFloor() == 1 && request.getTileId() == 871901629) {
//				// this is the altar in the upper floor of the monk's guild.
//				// tbh this should be a different scenery id, "monk's guild altar" or something, in case the altar is moved at some point.
//				maxPrayer += 2;
//			}
//			
//			player.setPrayerPoints((float)maxPrayer, responseMaps);
//			
//			setResponseText("you recharge your prayer points.");
//			responseMaps.addClientOnlyResponse(player, this);
//		}
//	}
	
	@Override
	protected boolean setTarget(Request request, Player player, ResponseMaps responseMaps) {
		if (SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId()) != 106) // altar TODO PrayableDao and table?
			return false;
		
		walkingTargetTileId = request.getTileId();
		return true;
	}

	@Override
	protected void doAction(Request request, Player player, ResponseMaps responseMaps) {
		// at this point we're at the altar
		int maxPrayer = StatsDao.getStatLevelByStatIdPlayerId(Stats.PRAYER, player.getId());
		
		if (player.getFloor() == 1 && request.getTileId() == 871901629) {
			// this is the altar in the upper floor of the monk's guild.
			// tbh this should be a different scenery id, "monk's guild altar" or something, in case the altar is moved at some point.
			maxPrayer += 2;
		}
		
		player.setPrayerPoints((float)maxPrayer, responseMaps);
		
		setResponseText("you recharge your prayer points.");
		responseMaps.addClientOnlyResponse(player, this);
	}

}
