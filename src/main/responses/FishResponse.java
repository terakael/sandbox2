package main.responses;

import main.database.FishableDao;
import main.database.FishableDto;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.database.StatsDao;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.FishRequest;
import main.requests.Request;
import main.types.Items;
import main.types.Stats;

public class FishResponse extends Response {

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		FishRequest request = (FishRequest)req;
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), true));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(request.getTileId(), responseMaps);
			FishableDto fishable = FishableDao.getFishableDtoByTileId(request.getTileId());
			if (fishable == null) {
				setRecoAndResponseText(0, "you can't fish this.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// does the player have the level to fish this?
			if (StatsDao.getStatLevelByStatIdPlayerId(Stats.FISHING, player.getId()) < fishable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d fishing to fish here.", fishable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			// does player have inventory space
			if (PlayerStorageDao.getFreeSlotByPlayerId(player.getId()) == -1) {
				setRecoAndResponseText(0, "your inventory is too full to fish anymore.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			new StartFishingResponse().process(request, player, responseMaps);
			
			// the action bubble will be the fish you're trying to catch
			ActionBubbleResponse actionBubble = new ActionBubbleResponse(player.getId(), ItemDao.getItem(fishable.getItemId()).getSpriteFrameId());
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), actionBubble);
			
			player.setState(PlayerState.fishing);
			player.setSavedRequest(req);
			player.setTickCounter(5);
		}
	}

}
