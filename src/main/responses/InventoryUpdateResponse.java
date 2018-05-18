package main.responses;

import java.util.ArrayList;
import java.util.List;

import javax.websocket.Session;

import lombok.AllArgsConstructor;
import main.database.ItemDto;
import main.database.PlayerInventoryDao;
import main.requests.InventoryMoveRequest;
import main.requests.InventoryUpdateRequest;
import main.requests.Request;

public class InventoryUpdateResponse extends Response {	
	private List<Integer> inventory = new ArrayList<>();

	public InventoryUpdateResponse(String action) {
		super(action);
	}

	@Override
	public ResponseType process(Request req, Session client) {		
		if (req instanceof InventoryMoveRequest)
			processInventoryMoveRequest((InventoryMoveRequest)req, client);
		
		inventory = PlayerInventoryDao.getInventoryListByPlayerId(req.getId());
		
		return ResponseType.client_only;
	}
	
	private void processInventoryMoveRequest(InventoryMoveRequest req, Session client) {
		// check if there's already an item in the slot we're trying to move the src item to
		ItemDto destItem = PlayerInventoryDao.getItemFromPlayerIdAndSlot(req.getId(), req.getDest());
		ItemDto srcItem = PlayerInventoryDao.getItemFromPlayerIdAndSlot(req.getId(), req.getSrc());
		if (destItem != null)
			PlayerInventoryDao.setItemFromPlayerIdAndSlot(req.getId(), req.getSrc(), destItem.getId());
		PlayerInventoryDao.setItemFromPlayerIdAndSlot(req.getId(), req.getDest(), srcItem.getId());
	}

}
