package main.responses;

import java.util.List;

import main.database.dao.ChoppableDao;
import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SceneryDao;
import main.database.dao.StatsDao;
import main.database.dto.ChoppableDto;
import main.processing.ConstructableManager;
import main.processing.DepletionManager;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.ChopRequest;
import main.requests.Request;
import main.types.Stats;
import main.types.StorageTypes;

public class ChopResponse extends Response {
	private static final int hatchetId = ItemDao.getIdFromName("hatchet");
	private static final int goldenHatchetId = ItemDao.getIdFromName("golden hatchet");

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (FightManager.fightWithFighterIsBattleLocked(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		FightManager.cancelFight(player, responseMaps);
		
		ChopRequest request = (ChopRequest)req;
		final int sceneryId = SceneryDao.getSceneryIdByTileId(player.getFloor(), request.getTileId());
		if (sceneryId == -1) {
			// tileId doesn't have scenery, this is client fuckery
			return;
		}
		
		if (!PathFinder.isNextTo(player.getFloor(), player.getTileId(), request.getTileId())) {
			player.setPath(PathFinder.findPath(player.getFloor(), player.getTileId(), request.getTileId(), false));
			player.setState(PlayerState.walking);
			player.setSavedRequest(req);
			return;
		} else {
			player.faceDirection(request.getTileId(), responseMaps);
			
			ChoppableDto choppable = ChoppableDao.getChoppableBySceneryId(sceneryId);
			if (choppable == null) {
				// this scenery isn't choppable
				setRecoAndResponseText(0, "you can't chop that.");
				responseMaps.addClientOnlyResponse(player, this);
				return;
			}
			
			if (DepletionManager.isDepleted(DepletionManager.DepletionType.tree, player.getFloor(), request.getTileId())) {
				if (player.getState() != PlayerState.woodcutting) {
					setRecoAndResponseText(0, "the tree has already been cut down.");
					responseMaps.addClientOnlyResponse(player, this);
				}
				player.setState(PlayerState.idle);
				return;
			}
				
			List<Integer> inventoryItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			if (!inventoryItemIds.contains(hatchetId) && !inventoryItemIds.contains(goldenHatchetId)) {
				setRecoAndResponseText(0, "you need a hatchet in order to chop the tree.");
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			// does the player have the level to chop this?
			if (StatsDao.getStatLevelByStatIdPlayerId(Stats.WOODCUTTING, player.getId()) < choppable.getLevel()) {
				setRecoAndResponseText(0, String.format("you need %d woodcutting to chop this.", choppable.getLevel()));
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			// magic tree special case
			if (choppable.getSceneryId() == SceneryDao.getIdByName("magic tree") && !inventoryItemIds.contains(goldenHatchetId)) {
				setRecoAndResponseText(0, "this hatchet doesn't seem powerful enough to chop this tree.");
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			// does player have inventory space
			if (PlayerStorageDao.getFreeSlotByPlayerId(player.getId()) == -1) {
				final String message = player.getState() == PlayerState.woodcutting
						? "your inventory is too full to chop anymore."
						: "you don't have any free inventory space.";
				
				setRecoAndResponseText(0, message);
				responseMaps.addClientOnlyResponse(player, this);
				player.setState(PlayerState.idle);
				return;
			}
			
			if (player.getState() != PlayerState.woodcutting) {
				setRecoAndResponseText(1, "you start chopping the tree...");
				responseMaps.addClientOnlyResponse(player, this);
				
				player.setState(PlayerState.woodcutting);
				player.setSavedRequest(req);
			}
			
			int usedHatchetId = hatchetId;
			if (inventoryItemIds.contains(goldenHatchetId))
				usedHatchetId = goldenHatchetId;
			
			int tickCounter = usedHatchetId == hatchetId ? 5 : 3;
			if (ConstructableManager.constructableIsInRadius(player.getFloor(), player.getTileId(), 138, 3))
				tickCounter -= 1;
			player.setTickCounter(tickCounter);
			
			responseMaps.addLocalResponse(player.getFloor(), player.getTileId(), 
					new ActionBubbleResponse(player, ItemDao.getItem(usedHatchetId)));
		}
	}
}