package main.responses;

import main.database.dao.FishableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dao.StatsDao;
import main.database.dto.FishableDto;
import main.processing.PathFinder;
import main.processing.managers.FightManager;
import main.processing.WorldProcessor;
import main.processing.attackable.Player;
import main.processing.attackable.Player.PlayerState;
import main.requests.FishRequest;
import main.requests.Request;
import main.types.SceneryAttributes;
import main.types.Stats;
import main.types.StorageTypes;

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
			
			// sometimes scenery only appears at night or day
			final boolean isDiurnal = SceneryDao.sceneryContainsAttribute(fishable.getSceneryId(), SceneryAttributes.DIURNAL);
			final boolean isNocturnal = SceneryDao.sceneryContainsAttribute(fishable.getSceneryId(), SceneryAttributes.NOCTURNAL);
			if ((WorldProcessor.isDaytime() && !isDiurnal) || (!WorldProcessor.isDaytime() && !isNocturnal))
				return;
			
			// does the player have the level to fish this?
			if (StatsDao.getStatLevelByStatIdPlayerId(Stats.FISHING, player.getId()) < fishable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d fishing to fish here.", fishable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			if (PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), fishable.getToolId(), StorageTypes.INVENTORY) == 0) {
				setRecoAndResponseText(0, String.format("you need a %s to fish here.", ItemDao.getNameFromId(fishable.getToolId())));
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			if (fishable.getBaitId() != 0 && PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), fishable.getBaitId(), StorageTypes.INVENTORY) == 0) {
				final String baitName = ItemDao.getNameFromId(fishable.getBaitId());
				final String message = player.getState() == PlayerState.fishing
						? String.format("you have run out of %ss.", baitName)
						: String.format("you need some %ss to fish here.", baitName);
				setRecoAndResponseText(0, message);
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
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
					new ActionBubbleResponse(player, ItemDao.getItem(fishable.getToolId())));
		}
	}

}
