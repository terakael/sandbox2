package main.responses;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Setter;
import main.database.dao.DoorDao;
import main.processing.DepletionManager;
import main.processing.LockedDoorManager;
import main.processing.Player;
import main.requests.Request;

public class AddSceneryInstancesResponse extends Response {
	@Setter private Map<Integer, Set<Integer>> instances; // <sceneryId, <tileIds>>>
	private Set<Integer> depletedScenery = null;
	private Set<Integer> openDoors = null;
	
	public AddSceneryInstancesResponse() {
		setAction("add_scenery_instances");
	}

	@Override
	public void process(Request req, Player player, ResponseMaps responseMaps) {
		// TODO Auto-generated method stub
	}
	
	public void setDepletedScenery(int floor, Set<Integer> tilesToCheck) {
		depletedScenery = DepletionManager.getDepletedSceneryTileIds(floor)
				.stream()
				.filter(tileId -> tilesToCheck.contains(tileId))
				.collect(Collectors.toSet());
	}
	
	public void setOpenDoors(int floor, Set<Integer> tilesToCheck) {
		openDoors = new HashSet<>();
		openDoors.addAll(DoorDao.getOpenDoorTileIds(floor)
				  .stream()
				  .filter(tileId -> tilesToCheck.contains(tileId))
				  .collect(Collectors.toSet()));
		
		openDoors.addAll(LockedDoorManager.getOpenLockedDoorTileIds(floor)
				  .stream()
				  .filter(tileId -> tilesToCheck.contains(tileId))
				  .collect(Collectors.toSet()));
	}
}
