package processing.managers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import database.DbConnection;
import database.dao.NPCDao;
import database.dao.PetDao;
import database.dto.NPCDto;
import database.entity.delete.DeleteHousePetsEntity;
import database.entity.insert.InsertHousePetsEntity;
import processing.attackable.NPC;
import processing.attackable.Pet;
import processing.attackable.Player;
import types.NpcAttributes;

public class HousePetsManager {
	private static Map<Integer, Map<Integer, Set<Integer>>> housePetInstances = new HashMap<>(); // floor, <houseId, <tileIds>>
	
	public static void setupCaches() {
		DbConnection.load("select house_id, pet_id, floor from house_pets", rs -> {
			addPet(rs.getInt("house_id"), rs.getInt("pet_id"), rs.getInt("floor"), null);
		});
	}
	
	public static boolean addPetByPetItemId(Player player, int petItemId) {
		// player is in their house, so attach pet to house if there's room
		final int npcId = PetDao.getNpcIdFromItemId(petItemId);
		if (npcId == -1)
			return false;
		
		if (!addPet(player.getHouseId(), npcId, player.getFloor(), player.getTileId()))
			return false;
		
		DatabaseUpdater.enqueue(new InsertHousePetsEntity(player.getHouseId(), npcId, player.getFloor()));
		return true;
	}
	
	public static void removePet(NPC pet) {		
		final int houseId = HousingManager.getHouseIdFromFloorAndTileId(pet.getFloor(), pet.getInstanceId());
		
		if (housePetInstances.containsKey(pet.getFloor()) && housePetInstances.get(pet.getFloor()).containsKey(houseId)) {
			housePetInstances.get(pet.getFloor()).get(houseId).remove(pet.getInstanceId());
			
			// cleanups - if we removed the only instance in the house, remove the house entry itself
			if (housePetInstances.get(pet.getFloor()).get(houseId).isEmpty())
				housePetInstances.get(pet.getFloor()).remove(houseId);
			
			// also if we removed the only house from the floor, remove the floor as well
			if (housePetInstances.get(pet.getFloor()).isEmpty())
				housePetInstances.remove(pet.getFloor());
		}
		
		// we don't care about the floor here - a house should only be able to have one of a specific type of pet
		DatabaseUpdater.enqueue(DeleteHousePetsEntity.builder()
				.houseId(houseId)
				.petId(pet.getId())
				.build());
	}
	
	private static boolean addPet(int houseId, int petId, int floor, Integer spawnTileId) {
		// given the list of house tiles on this floor, remove any tiles already taken by pets.  Scenery is already filtered out.
		final Set<Integer> availableHouseTiles = HousingManager.getWalkableTilesByHouseId(houseId, floor);
		availableHouseTiles.removeAll(getOccupiedTileIdsByHouseId(houseId, floor));
		if (availableHouseTiles.isEmpty())
			return false; // no free tiles to accomodate pet
		
		// choose a random available tile
		final int instanceId = availableHouseTiles.stream()
				.skip(new Random().nextInt(availableHouseTiles.size()))
				.findFirst()
				.orElse(availableHouseTiles.iterator().next());
		
		housePetInstances.putIfAbsent(floor, new HashMap<>());
		housePetInstances.get(floor).putIfAbsent(houseId, new HashSet<>());
		housePetInstances.get(floor).get(houseId).add(instanceId);
		
		NPCDto deepCopy = new NPCDto(NPCDao.getNpcById(petId));
		deepCopy.setAttributes(deepCopy.getAttributes() & ~NpcAttributes.ATTACKABLE.getValue()); // pets cannot be attacked
		deepCopy.setAttributes(deepCopy.getAttributes() | NpcAttributes.DIURNAL.getValue() | NpcAttributes.NOCTURNAL.getValue()); // should show at all times of the day
		
		Pet pet = new Pet(deepCopy, floor, instanceId);
		pet.setTileId(spawnTileId == null ? instanceId : spawnTileId);
		
		return true;
	}
	
	private static Set<Integer> getOccupiedTileIdsByHouseId(int houseId, int floor) {
		if (housePetInstances.containsKey(floor) && housePetInstances.get(floor).containsKey(houseId))
			return housePetInstances.get(floor).get(houseId);
		return new HashSet<>();
	}
}
