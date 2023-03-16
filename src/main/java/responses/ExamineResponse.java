package responses;

import java.util.HashMap;
import java.util.Map;

import database.dao.ClockDao;
import database.dao.ItemDao;
import database.dao.NPCDao;
import database.dao.PlayerDao;
import database.dao.PlayerStorageDao;
import database.dao.SceneryDao;
import processing.attackable.NPC;
import processing.attackable.Player;
import processing.managers.ConstructableManager;
import processing.managers.HousingManager;
import processing.managers.LocationManager;
import processing.managers.LockedDoorManager;
import processing.managers.TimeManager;
import requests.ExamineRequest;
import requests.Request;
import types.ItemAttributes;
import types.StorageTypes;

@SuppressWarnings("unused")
public class ExamineResponse extends Response {
	private String examineText;
	
	private static Map<Integer, String> sceneryExamineMap = new HashMap<>();
	private static Map<Integer, String> itemExamineMap = new HashMap<>();
	private static Map<Integer, String> npcExamineMap = new HashMap<>();
	public static void initializeExamineMap() {
		sceneryExamineMap = SceneryDao.getExamineMap();
		itemExamineMap = ItemDao.getExamineMap();
		npcExamineMap = NPCDao.getExamineMap();
	}

	public ExamineResponse() {
		setAction("examine");
	}
	
	@Override
	protected boolean handleCombat(Request req, Player player, ResponseMaps responseMaps) {
		return true;
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof ExamineRequest))
			return;
		
		ExamineRequest request = (ExamineRequest)req;
		if (request.getType() == null)
			return;
		
		switch (request.getType()) {
		case "scenery": { 
			examineText = sceneryExamineMap.get(request.getObjectId());
			
			// constructables show their timer as well (if they aren't in a player's house; house constructables don't have a timer)
			final int remainingTicks = ConstructableManager.getRemainingTicks(player.getFloor(), request.getTileId());
			if (remainingTicks > 0 && HousingManager.getHouseIdFromFloorAndTileId(player.getFloor(), request.getTileId()) <= 0) {
				if (remainingTicks > 120) {
					final int remainingMinutes = remainingTicks / 120;
					examineText += String.format(" (%d minute%s remain%s)", remainingMinutes, remainingMinutes == 1 ? "" : "s", remainingMinutes == 1 ? "s" : "");
				} else {
					final int remainingSeconds = (int)(remainingTicks * 0.5);
					examineText += String.format(" (%d second%s remain%s)", remainingSeconds, remainingSeconds == 1 ? "" : "s", remainingSeconds == 1 ? "s" : "");
				}
			}
			
			if (ClockDao.isClock(request.getObjectId())) {
				examineText += String.format(" (%s)", TimeManager.getInGameTime());
			}
			
			// player house front doors show the house name and who owns it
			if (LockedDoorManager.isLockedDoor(player.getFloor(), request.getTileId())) {
				final int houseId = HousingManager.getHouseIdFromFloorAndTileId(player.getFloor(), request.getTileId());
				if (houseId > 0) {
					final int owningPlayerId = HousingManager.getOwningPlayerId(player.getFloor(), request.getTileId());
					final String ownershipMessage = owningPlayerId != -1
							? String.format("owned by %s", PlayerDao.getNameFromId(owningPlayerId))
							: "up for sale";
					examineText = String.format("a sign reads: %s - %s.", HousingManager.getHouseNameById(houseId), ownershipMessage);
				}
			}
			
			break;
		}
		case "item": {
			if (ItemDao.itemHasAttribute(request.getObjectId(), ItemAttributes.STACKABLE)) {
				int stackCount = PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), request.getObjectId(), StorageTypes.INVENTORY); 
				if (stackCount >= 100000) {
					examineText = String.format("%,d %s.", stackCount , ItemDao.getNameFromId(request.getObjectId(), stackCount != 1));
					break;
				}
			}
			examineText = itemExamineMap.get(request.getObjectId());
			break;
		}
		case "grounditem": {
			// you can't count a stackable item if it's on the ground, that's just silly.
			// you need to be holding it to count it.
			examineText = itemExamineMap.get(request.getObjectId());
			break;
		}
		case "npc": {
			final NPC npc = LocationManager.getNpcNearPlayerByInstanceId(player, request.getObjectId());
			if (npc != null)
				examineText = npcExamineMap.get(npc.getId());
			break;
		}
		default:
			break;
		}
		
		responseMaps.addClientOnlyResponse(player, this);
	}

}
