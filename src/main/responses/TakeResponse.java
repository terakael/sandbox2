package main.responses;

import java.util.ArrayList;
import java.util.List;

import lombok.Setter;
import main.FightManager;
import main.GroundItemManager;
import main.database.ItemDao;
import main.database.PlayerStorageDao;
import main.processing.PathFinder;
import main.processing.Player;
import main.processing.Player.PlayerState;
import main.requests.Request;
import main.requests.TakeRequest;
import main.types.ItemAttributes;

public class TakeResponse extends Response {
	//@Setter private List<GroundItemManager.GroundItem> groundItems;

	public TakeResponse() {
		setAction("take");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		if (!(req instanceof TakeRequest)) {
			setRecoAndResponseText(0, "funny business");
			return;
		}
		
		if (FightManager.fightWithFighterExists(player)) {
			setRecoAndResponseText(0, "you can't do that during combat.");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		TakeRequest takeReq = (TakeRequest)req;
		
		GroundItemManager.GroundItem groundItem = GroundItemManager.getItemAtTileId(player.getId(), takeReq.getItemId(), takeReq.getTileId());
		if (groundItem == null) {
			setRecoAndResponseText(0, "too late - it's gone!");
			responseMaps.addClientOnlyResponse(player, this);
			return;
		}
		
		if (takeReq.getTileId() != player.getTileId()) {
			// walk over to the item before picking it up
			player.setPath(PathFinder.findPath(player.getTileId(), takeReq.getTileId(), true));
			player.setState(PlayerState.walking);
			
			// save the request so the player reprocesses it when they arrive at their destination
			player.setSavedRequest(req);
			return;
		}
		
		if (ItemDao.itemHasAttribute(groundItem.getId(), ItemAttributes.STACKABLE)) {
			ArrayList<Integer> invItems = PlayerStorageDao.getInventoryListByPlayerId(player.getId());
			int invItemIndex = invItems.indexOf(groundItem.getId());
			if (invItemIndex >= 0) {
				PlayerStorageDao.addCountToInventoryItemSlot(player.getId(), invItemIndex, groundItem.getCount());
			} else {
				PlayerStorageDao.addItemByPlayerIdItemId(player.getId(), takeReq.getItemId(), groundItem.getCount());
			}
		} else {
			PlayerStorageDao.addItemByPlayerIdItemId(player.getId(), takeReq.getItemId(), groundItem.getCount());
		}
		
		GroundItemManager.remove(player.getId(), takeReq.getTileId(), takeReq.getItemId(), groundItem.getCount());
		
		// update the player inventory/equipped items and only send it to the player
		Response resp = ResponseFactory.create("invupdate");
		resp.process(takeReq, player, responseMaps);// adds itself to the appropriate responseMap
		player.setState(PlayerState.idle);
	}

}
