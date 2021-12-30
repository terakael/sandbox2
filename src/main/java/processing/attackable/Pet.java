package processing.attackable;

import java.util.Stack;

import database.dto.NPCDto;
import lombok.Setter;
import processing.PathFinder;
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
		if (master == null) {
			if (disassociatedTick == 0) {
				disassociatedTick = currentTick;
			} else if (currentTick - disassociatedTick >= 50) {
				// we've been wandering around masterless for 30 seconds; time to run away for good
				LocationManager.removePetIfExists(this);
				return;
			}
			super.process(currentTick, responseMaps);
		} else {
			if (floor != master.getFloor() || !Utils.areTileIdsWithinRadius(tileId, master.getTileId(), 12)) {
				teleportToMaster(responseMaps);
				return;
			}
			
			if (!Utils.areTileIdsWithinRadius(tileId, master.getTileId(), 1) || tileId == master.getTileId()) {
				Stack<Integer> path = PathFinder.findPath(floor, tileId, master.getTileId(), false); 
				if (path.isEmpty()) {
					// we can't get to master, so teleport
					teleportToMaster(responseMaps);
					return;
				}
				setPath(path);
			}
			
			if (popPath()) {
				NpcUpdateResponse updateResponse = new NpcUpdateResponse();
				updateResponse.setInstanceId(instanceId);
				updateResponse.setTileId(tileId);
				responseMaps.addLocalResponse(floor, tileId, updateResponse);
			}
		}
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
