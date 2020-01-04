package main.responses;

import java.util.ArrayList;
import java.util.Collections;

import main.database.BrewableDao;
import main.database.EquipmentDao;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.database.SceneryDao;
import main.database.StatsDao;
import main.database.UseItemOnItemDao;
import main.database.UseItemOnItemDto;
import main.processing.FightManager;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.UseRequest;
import main.scenery.Scenery;
import main.scenery.SceneryManager;
import main.types.Items;
import main.types.Stats;
import main.types.StorageTypes;

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
		case "item":
			if (handleUseOnItem(request, player, responseMaps))
				return;
			break;
		case "npc":
			if (handleUseOnNpc(request, player, responseMaps))
				return;
			break;
		case "player":
			if (handleUseOnPlayer(request, player, responseMaps))
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
		
		if (!scenery.use(request.getSrc(), request.getSlot(), player, responseMaps))
			return false;
		
		return true;
	}
	
	private boolean handleUseOnItem(UseRequest request, Player player, ResponseMaps responseMaps) {
		int src = request.getSrc();
		int dest = request.getDest();
		int srcSlot = request.getSrcSlot();
		int destSlot = request.getSlot();
		
		UseItemOnItemDto dto = UseItemOnItemDao.getEntryBySrcIdDestId(src, dest);
		if (dto == null) {
			// try switching the src and dest
			src = request.getDest();
			dest = request.getSrc();
			srcSlot = request.getSlot();
			destSlot = request.getSrcSlot();
			
			dto = UseItemOnItemDao.getEntryBySrcIdDestId(src, dest);
			if (dto == null) {
				// nope no match; nothing interesting happens when you use these two items together.
				return false;
			}
		}
		
		// slot check
		ArrayList<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY.getValue());
		if (invItemIds.get(srcSlot) != src) {
			srcSlot = invItemIds.indexOf(src);
			if (srcSlot == -1) {
				setRecoAndResponseText(0, "you don't have the materials to make that.");
				responseMaps.addClientOnlyResponse(player, this);
				return true;
			}
		}
		
		if (invItemIds.get(destSlot) != dest) {
			destSlot = invItemIds.indexOf(dest);
			if (destSlot == -1) {
				setRecoAndResponseText(0, "you don't have the materials to make that.");
				responseMaps.addClientOnlyResponse(player, this);
				return true;
			}
		}
		
		if (invItemIds.get(srcSlot) != src || invItemIds.get(destSlot) != dest) {
			// user-input slots don't match what's actually in the slots; bail with a "nothing interesting happens" message
			return false;
		}
		
		// level check by resulting item id
		if (BrewableDao.isBrewable(dto.getResultingItemId())) {
			int playerLevel = StatsDao.getStatLevelByStatIdPlayerId(Stats.HERBLORE, player.getId());
			int requiredLevel = BrewableDao.getRequiredLevelById(dto.getResultingItemId());
			if (playerLevel < requiredLevel) {
				setRecoAndResponseText(0, String.format("you need %d herblore to make that.", requiredLevel));
				responseMaps.addClientOnlyResponse(player, this);
				return true;
			}
		}
		
		// you can't use things on equipped items (or use equipped items on things)
		if (EquipmentDao.isItemEquippedByItemIdAndSlot(player.getId(), src, srcSlot) ||
				EquipmentDao.isItemEquippedByItemIdAndSlot(player.getId(), dest, destSlot)) {
			setRecoAndResponseText(0, "you need to unequip it first.");
			responseMaps.addClientOnlyResponse(player, this);
			return true;
		}
		
		new StartUseResponse().process(request, player, responseMaps);		
		player.setState(PlayerState.using);
		player.setSavedRequest(request);
		player.setTickCounter(3);
		return true;
	}

	private boolean handleUseOnNpc(UseRequest request, Player player, ResponseMaps responseMaps) {
		// for now you can only use poison on enemies, which requires you to be in combat.
		return false;
	}
	
	private boolean handleUseOnPlayer(UseRequest request, Player player, ResponseMaps responseMaps) {
		// for now you can only use poison on enemies, which requires you to be in combat.
		return false;
	}
}
