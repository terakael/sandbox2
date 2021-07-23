package responses;

import java.util.Collections;
import java.util.List;

import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.SawmillableDao;
import database.dto.SawmillableDto;
import processing.attackable.Player;
import processing.attackable.Player.PlayerState;
import processing.managers.TybaltsTaskManager;
import processing.tybaltstasks.updates.SawmillTaskUpdate;
import requests.AddExpRequest;
import requests.Request;
import requests.RequestFactory;
import requests.UseRequest;
import types.Stats;
import types.StorageTypes;

public class FinishSawmillResponse extends Response {
	private static int REQUIRED_LOGS = 3;
	
	public FinishSawmillResponse() {
		setAction("finish_sawmill");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		UseRequest request = (UseRequest)req;
		
		final SawmillableDto sawmillable = SawmillableDao.getSawmillableByLogId(request.getSrc());
		if (sawmillable == null)
			return;
		
		List<Integer> inventoryItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		final int numRequiredLogs = Collections.frequency(inventoryItemIds, sawmillable.getLogId());
		if (numRequiredLogs < REQUIRED_LOGS) {
			final String message = player.getState() != PlayerState.sawmill
						? "you need three logs to make a plank."
						: "you have run out of logs.";
				
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, null));
			return;
		}
		
		AddExpRequest addExpReq = new AddExpRequest();
		addExpReq.setStatId(Stats.CONSTRUCTION.getValue());
		addExpReq.setExp(sawmillable.getExp());
		new AddExpResponse().process(addExpReq, player, responseMaps);
		
		for (int i = 0; i < REQUIRED_LOGS; ++i) {
			int logIndex = inventoryItemIds.indexOf(sawmillable.getLogId());
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, logIndex, 0, 0, 0);
			inventoryItemIds.set(logIndex, 0);
		}
		
		PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, sawmillable.getResultingPlankId(), 1, ItemDao.getMaxCharges(sawmillable.getResultingPlankId()));
		
		new InventoryUpdateResponse().process(RequestFactory.create("dummy", player.getId()), player, responseMaps);
		
		TybaltsTaskManager.check(player, new SawmillTaskUpdate(sawmillable.getResultingPlankId()), responseMaps);
	}

}
