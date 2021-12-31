package processing.managers;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import database.dao.NPCDao;
import database.dao.PetDao;
import database.dao.PlayerStorageDao;
import database.dao.WanderingPetDao;
import database.dto.NPCDto;
import processing.attackable.NPC;
import processing.attackable.Player;
import responses.InventoryUpdateResponse;
import responses.MessageResponse;
import responses.ResponseMaps;
import types.Items;
import types.StorageTypes;

public class WanderingPetManager {
	private static WanderingPetManager instance = null;
	private Map<Integer, Iterator<NPC>> wanderingPetIterators;
	private Map<Integer, NPC> activeWanderingPets = new HashMap<>(); // npcId, npc
	
	private Map<Integer, Set<Integer>> petItems = new HashMap<>(); // npcId, itemId TODO move to db
	
	private WanderingPetManager() {
		petItems.put(5, Set.<Integer>of(Items.BREAD_LOAF.getValue(), Items.HALF_BREAD_LOAF.getValue())); // chick likes bread
		petItems.put(6, Set.<Integer>of(Items.BONES.getValue(), Items.GIANT_BONES.getValue(), Items.WOLF_BONES.getValue())); // puppy likes bones
		petItems.put(60, Collections.singleton(Items.BUCKET_OF_MILK.getValue())); // cat likes milk
	}
	
	public static WanderingPetManager get() {
		if (instance == null)
			instance = new WanderingPetManager();
		return instance;
	}
	
	public void loadWanderingPets() {
		WanderingPetDao.setupCaches();
		
		wanderingPetIterators = WanderingPetDao.getWanderingPets().entrySet().stream()
			.map(e -> {
				final Set<NPC> npcs = new HashSet<>();
				e.getValue().forEach(loc -> npcs.add(new NPC(NPCDao.getNpcById(e.getKey()), loc.getKey(), loc.getValue())));
				return new SimpleImmutableEntry<>(e.getKey(), npcs);
			})
			.collect(Collectors.toMap(e -> e.getKey(), e -> Stream.generate(() -> e.getValue()).flatMap(Set::stream).iterator()));
	}
	
	public void rotateWanderingPets() {
		wanderingPetIterators.forEach((key, value) -> {
			// get rid of whatever is currently in there if necessary
			if (activeWanderingPets.containsKey(key))
				LocationManager.removeNpc(activeWanderingPets.get(key));
			
			// overwrite with the next npc (need to keep track as we can't access the "current" iterator's object
			activeWanderingPets.put(key, value.next());
			
			// add the "next" npc to the location manager
			LocationManager.addNpcs(Collections.singletonList(activeWanderingPets.get(key)));
		});
	}
	
	public NPC getActiveWanderingPetByFloorAndInstanceId(int floor, int instanceId) {
		for (NPC npc : activeWanderingPets.values()) {
			if (npc.getInstanceId() == instanceId && npc.getFloor() == floor)
				return npc;
		}
		return null;
	}
	
	public boolean npcIsWanderingPet(NPC npc) {
		return activeWanderingPets.values().stream()
				.anyMatch(e -> e == npc);
	}
	
	public boolean handleUseItemOnPet(Player player, NPC npc, int itemId, ResponseMaps responseMaps) {
		if (!petItems.containsKey(npc.getId()))
			return false; // nothing interesting happens
		
		if (!petItems.get(npc.getId()).contains(itemId))
			return false; // nothing interesting happens
		
		final int petItemId = PetDao.getItemIdFromNpcId(npc.getId());
		if (petItemId == -1) {
			// no corresponding item for this pet - not actually a pet then.
			return false; // nothing interesting happens.
		}
		
		if (PlayerStorageDao.itemExistsInPlayerStorage(player.getId(), petItemId)) {
			// already have one, cannot have more than one.
			final MessageResponse messageResponse = MessageResponse.newMessageResponse(String.format("the %s doesn't seem interested in that.", NPCDao.getNpcNameById(npc.getId())), "white");
			responseMaps.addClientOnlyResponse(player, messageResponse);
			return true;
		}
		
		final int slot = PlayerStorageDao.getSlotOfItemId(player.getId(), StorageTypes.INVENTORY, itemId);
		
		// pet accepts the gift
		PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, slot, 0, 0, 0);
		
		if (PlayerStorageDao.getItemIdInSlot(player.getId(), StorageTypes.PET, 0) == 0) {
			// no pet following, so make this pet follow
			PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.PET, 0, petItemId, 1, 0);
			player.setPet(petItemId);
		} else {
			// already have a pet following, so put pet in inventory
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, petItemId, 1, 0);
		}
		
		InventoryUpdateResponse.sendUpdate(player, responseMaps);
		
		// once pet has been taken, remove from location manager as nobody else should be able to get it
		LocationManager.removeNpc(npc);
		
		return true;
	}
}
