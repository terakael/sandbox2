package main.responses;

import java.util.Collections;

import main.database.dao.FishableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.StatsDao;
import main.database.dto.FishableDto;
import main.processing.ClientResourceManager;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.FishRequest;
import main.requests.Request;
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
				player.setState(PlayerState.idle);
				return;
			}
			
			// does the player have the level to fish this?
			if (StatsDao.getStatLevelByStatIdPlayerId(Stats.FISHING, player.getId()) < fishable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d fishing to fish here.", fishable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			// does player have inventory space
			if (PlayerStorageDao.getFreeSlotByPlayerId(player.getId()) == -1) {
				setRecoAndResponseText(0, "your inventory is full.");
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			if (player.getState() != PlayerState.fishing) {
				setRecoAndResponseText(1, "you start fishing...");
				responseMaps.addClientOnlyResponse(player, this);
				
				player.setState(PlayerState.fishing);
				player.setSavedRequest(req);
			}
			player.setTickCounter(5);
			
			// the action bubble will be the fish you're trying to catch
			ActionBubbleResponse actionBubble = new ActionBubbleResponse(player.getId(), ItemDao.getItem(fishable.getItemId()).getSpriteFrameId());
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), actionBubble);
			
			ClientResourceManager.addItems(player, Collections.singleton(fishable.getItemId()));
		}
	}

}
