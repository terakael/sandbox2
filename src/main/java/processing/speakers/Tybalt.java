package processing.speakers; // "i need some help..."

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.dao.DialogueDao;
import database.dao.ItemDao;
import database.dao.PlayerStorageDao;
import database.dao.PlayerTybaltsTaskDao;
import database.dto.InventoryItemDto;
import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import database.dto.PlayerTybaltsTaskDto;
import processing.attackable.Player;
import processing.managers.TybaltsTaskManager;
import responses.InventoryUpdateResponse;
import responses.MessageResponse;
import responses.ResponseMaps;
import types.Items;
import types.StorageTypes;

public class Tybalt implements Speaker {
	private static Map<Integer, Set<InventoryItemDto>> rewardsByTaskId = new HashMap<>();
	static {
		// log burner
		rewardsByTaskId.put(1, Set.<InventoryItemDto>of(new InventoryItemDto(Items.COINS.getValue(), 0, 50, 0)));
		
		// make copper helmet
		rewardsByTaskId.put(2, Set.<InventoryItemDto>of(
				new InventoryItemDto(Items.COINS.getValue(), 0, 100, 0),
				new InventoryItemDto(Items.COPPER_REINFORCEMENT.getValue(), 0, 1, 0)
		));
		
		// reinforce copper helmet
		rewardsByTaskId.put(3, Set.<InventoryItemDto>of(new InventoryItemDto(Items.COINS.getValue(), 0, 50, 0)));
		
		// chicken slayer
		rewardsByTaskId.put(4, Set.<InventoryItemDto>of(new InventoryItemDto(Items.IRON_DAGGERS_II.getValue(), 0, 1, 0)));
		
		// shrimp cooker
		rewardsByTaskId.put(5, Set.<InventoryItemDto>of(new InventoryItemDto(Items.COINS.getValue(), 0, 50, 0)));
		
		// bone totem
		rewardsByTaskId.put(6, Set.<InventoryItemDto>of(new InventoryItemDto(Items.COINS.getValue(), 0, 50, 0)));
		
		// brewer of stank
		rewardsByTaskId.put(7, Set.<InventoryItemDto>of(new InventoryItemDto(Items.COINS.getValue(), 0, 50, 0)));
		
		// shrine maker
		rewardsByTaskId.put(8, Set.<InventoryItemDto>of(new InventoryItemDto(Items.COINS.getValue(), 0, 50, 0)));
		
		// real artisan
		rewardsByTaskId.put(9, Set.<InventoryItemDto>of(new InventoryItemDto(Items.GOLDEN_PICKAXE.getValue(), 0, 1, ItemDao.getMaxCharges(Items.GOLDEN_PICKAXE.getValue()))));
		
		// nefarious nuisance
		rewardsByTaskId.put(10, Set.<InventoryItemDto>of(new InventoryItemDto(Items.TYBALTS_SWORD.getValue(), 0, 1, 0)));
	}

	@Override
	public void preShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		if (dialogueDto.getPointId() == 1 && dialogueDto.getDialogueId() == 14) {
			PlayerTybaltsTaskDao.setNewTask(player, 1, responseMaps);
		}
		
		if (dialogueDto.getPointId() == 100 && dialogueDto.getDialogueId() == 13) {
			// give the player a tinderbox and hatchet
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, Items.TINDERBOX.getValue(), 1, 0);
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, Items.HATCHET.getValue(), 1, 0);
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
		}
		
		if (dialogueDto.getPointId() == 500 && dialogueDto.getDialogueId() == 12) {
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, Items.NET.getValue(), 1, 0);
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
		}
	}
	
	@Override
	public void postShowDialogue(NpcDialogueDto dialogueDto, Player player, ResponseMaps responseMaps) {
		if (dialogueDto.getPointId() == 700 && dialogueDto.getDialogueId() == 24) {
			PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, Items.VIAL.getValue(), 1, 0);
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse("tybalt hands you a vial.", "white"));
		}
	}

	@Override
	public boolean dialogueOptionMeetsDisplayCriteria(NpcDialogueOptionDto option, Player player) {
		if (option.getPointId() == 2) {
			switch (option.getOptionId()) {
			case 1: // "i need some help"
				return !TybaltsTaskManager.taskIsFinished(player.getId());
				
			case 2: // i've finished it!
				return TybaltsTaskManager.taskIsFinished(player.getId());
			}
		}
		return true;
	}

	@Override
	public NpcDialogueDto switchDialogue(NpcDialogueDto previousDialogueDto, Player player, ResponseMaps responseMaps) {
		if (previousDialogueDto.getPointId() == 1 && previousDialogueDto.getDialogueId() == 14) { // before we start i have a few trials for you...
			return DialogueDao.getDialogue(26, 100, 1); // first task
		}

		if (previousDialogueDto.getPointId() == 2) {
			switch (previousDialogueDto.getDialogueId()) {
			case 5: { // some help eh?
				PlayerTybaltsTaskDto task = PlayerTybaltsTaskDao.getCurrentTaskByPlayerId(player.getId());
				if (task == null)
					return null;
				
				// pointId is taskId * 100 (e.g. LogBurner dialogue starts at pointId 100, MakeCopperHelmet dialogue starts at pointId 200 etc)
				// help dialogue starts at dialogue id 10.
				return DialogueDao.getDialogue(26, task.getTaskId() * 100, 10);
			}
			case 7: {// great job here's your reward
				PlayerTybaltsTaskDto task = PlayerTybaltsTaskDao.getCurrentTaskByPlayerId(player.getId());
				if (task == null)
					return null;
				
				if (rewardsByTaskId.containsKey(task.getTaskId())) {
					final String rewardMessage = rewardsByTaskId.get(task.getTaskId()).stream()
							.map(e -> String.format("%dx %s", e.getCount(), ItemDao.getNameFromId(e.getItemId())))
							.collect(Collectors.joining(", "));
					responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(String.format("tybalt hands you %s.", rewardMessage), "white"));
					
					rewardsByTaskId.get(task.getTaskId()).forEach(item -> 						
						PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, item.getItemId(), item.getCount(), item.getCharges()));
					
					InventoryUpdateResponse.sendUpdate(player, responseMaps);
				}
				
				final int newTaskId = task.getTaskId() + 1;
				PlayerTybaltsTaskDao.setNewTask(player, newTaskId, responseMaps);
				return DialogueDao.getDialogue(26, newTaskId * 100, 1);
			}
			
			}
		}
		
		return null;
	}
}
