package processing.attackable;

import java.util.ArrayList;
import java.util.Stack;

import database.dto.NPCDto;
import lombok.Setter;
import processing.PathFinder;
import processing.managers.HousingManager;
import processing.managers.LocationManager;
import responses.NpcUpdateResponse;
import responses.ResponseMaps;
import utils.Utils;

public class Pet extends NPC {
	@Setter private Player master = null;
	private int disassociatedTick = 0; // the tick we were disassociated on - when the player dies with a pet (or shoos away), we become disassociated
	
	public Pet(NPCDto dto, int floor, int instanceId) {
		super(dto, floor, instanceId);
	}
	
	@Override
	protected void init() {
		// NPC does a bunch of extra init stuff that a pet doesn't need
		setCurrentHp(1); // so we don't hit the respawn checks (when hp is 0)
		
		// our instanceId is either the playerId if dropped normally, or a house tileId if dropped in a house.
		int houseId = HousingManager.getHouseIdFromFloorAndTileId(floor, instanceId);
		if (houseId > 0) {
			walkableTiles = new ArrayList<>(HousingManager.getWalkableTilesByHouseId(houseId, floor));
		}
	}
	
	@Override
	public void setFloor(int floor) {
		this.floor = floor;
		LocationManager.addPet(this);
	}
	
	@Override
	public void setTileId(int tileId) {
		this.tileId = tileId;
		LocationManager.addPet(this);
	}
	
	@Override
	public boolean isDead() {
		return false; // pets don't die
	}

	@Override
	public void process(int currentTick, ResponseMaps responseMaps) {
		if (master != null && PathFinder.tileIsSailable(master.getFloor(), master.getTileId())) {
			// master is sailing; we are on the boat too so hide from the land
			return;
		}
		
		// if the pet has walkableTiles then it means it's in a house.
		// house pets shouldn't follow their master; they should walk around the house.
		if (master == null || walkableTiles != null) {
			if (walkableTiles == null) {
				// aimlessly wandering meaning he's lost his master, should despawn after a while
				if (disassociatedTick == 0) {
					disassociatedTick = currentTick;
				} else if (currentTick - disassociatedTick >= 50) {
					// we've been wandering around masterless for 30 seconds; time to run away for good
					LocationManager.removePetIfExists(this);
					return;
				}
			}
			super.process(currentTick, responseMaps);
		} else {
			if (floor != master.getFloor() || !Utils.areTileIdsWithinRadius(tileId, master.getTileId(), 12)) {
				teleportToMaster(responseMaps);
				return;
			}
			
			if (!Utils.areTileIdsWithinRadius(tileId, master.getTileId(), 1) || tileId == master.getTileId()) {
				Stack<Integer> path = PathFinder.findPath(floor, tileId, master.getTileId(), false, false); 
				if (path.isEmpty()) {
					// we can't get to master, so teleport
					teleportToMaster(responseMaps);
					return;
				}
				setPath(path);
			}
			
			if (popPath(responseMaps)) {
				NpcUpdateResponse updateResponse = new NpcUpdateResponse();
				updateResponse.setInstanceId(instanceId);
				updateResponse.setTileId(tileId);
				responseMaps.addLocalResponse(floor, tileId, updateResponse);
			}
		}
	}
	
	@Override
	public int getOwnerId() {
		if (master != null)
			return master.getId();
		
		if (walkableTiles != null) {
			return HousingManager.getOwningPlayerId(floor, instanceId);
		}
		
		return super.getOwnerId();
	}
	
	private void teleportToMaster(ResponseMaps responseMaps) {
		setFloor(master.getFloor());
		setTileId(master.getTileId());
		
		NpcUpdateResponse updateResponse = new NpcUpdateResponse();
		updateResponse.setInstanceId(instanceId);
		updateResponse.setTileId(tileId);
		responseMaps.addLocalResponse(floor, tileId, updateResponse);
	}
}
