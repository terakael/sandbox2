package main.responses;

import java.util.Collections;
import java.util.List;

import main.database.dao.ItemDao;
import main.database.dao.PlayerStorageDao;
import main.database.dao.SawmillableDao;
import main.database.dto.SawmillableDto;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.processing.TybaltsTaskManager;
import main.requests.AddExpRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.requests.UseRequest;
import main.tybaltstasks.updates.SawmillTaskUpdate;
import main.types.Stats;
import main.types.StorageTypes;

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
