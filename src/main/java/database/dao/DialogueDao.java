package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import database.DbConnection;
import database.dto.NpcDialogueDto;
import database.dto.NpcDialogueOptionDto;
import database.entity.insert.InsertPlayerNpcDialogueEntryPointEntity;
import database.entity.update.UpdatePlayerNpcDialogueEntryPointEntity;
import processing.attackable.Player;
import processing.managers.DatabaseUpdater;
import responses.InventoryUpdateResponse;
import responses.MessageResponse;
import responses.ResponseMaps;
import responses.ShowBaseAnimationsWindowResponse;
import types.Items;
import types.StorageTypes;

public class DialogueDao {
	private static final Map<Integer, Map<Integer, Set<NpcDialogueDto>>> dialogueByNpcId = new HashMap<>(); // npcId, <pointId, dto>
	private static final Map<Integer, Map<Integer, Set<NpcDialogueOptionDto>>> dialogueOptionsByNpcId = new HashMap<>(); // npcId, <pointId, dto>
	private static final Map<Integer, Map<Integer, Integer>> dialogueEntryPointsByPlayerId = new HashMap<>(); // playerId, <npcId, pointId>
	private static final Map<Integer, Map<Integer, Map<Integer, Predicate<Player>>>> dialogueOptionDisplayCriteria = new HashMap<>(); // npcId, <pointId, <option, fn>>
	private static final Map<Integer, Map<Integer, Map<Integer, BiConsumer<Player, ResponseMaps>>>> dialogueSpecialHandling = new HashMap<>(); // npcId, <pointId, <option, fn>>
	
	public static void setupCaches() {
		cacheNpcDialogue();
		cacheDialogueOptions();
		cacheDialogueOptionDisplayCriteria();
		cacheDialogueEntryPoints();
		cacheDialogueSpecialHandling();
	}
	
	private static void cacheNpcDialogue() {
		// ensure the dialogue is in order by explicitly ordering it and sticking it in a linked hashset
		final String query = "select npc_id, point_id, dialogue_id, dialogue from npc_dialogue order by npc_id, point_id, dialogue_id";
		DbConnection.load(query, rs -> {
			dialogueByNpcId.putIfAbsent(rs.getInt("npc_id"), new HashMap<>());
			dialogueByNpcId.get(rs.getInt("npc_id")).putIfAbsent(rs.getInt("point_id"), new LinkedHashSet<>());
			dialogueByNpcId.get(rs.getInt("npc_id")).get(rs.getInt("point_id")).add(new NpcDialogueDto(rs.getInt("npc_id"), rs.getInt("point_id"), rs.getInt("dialogue_id"), rs.getString("dialogue")));
		});
	}
	
	private static void cacheDialogueOptions() {
		final String query = "select npc_id, point_id, option_id, option_text, dialogue_src, dialogue_dest, next_point_id from npc_dialogue_options";
		DbConnection.load(query, rs -> {
			dialogueOptionsByNpcId.putIfAbsent(rs.getInt("npc_id"), new HashMap<>());
			dialogueOptionsByNpcId.get(rs.getInt("npc_id")).putIfAbsent(rs.getInt("point_id"), new LinkedHashSet<>());
			dialogueOptionsByNpcId.get(rs.getInt("npc_id")).get(rs.getInt("point_id")).add(new NpcDialogueOptionDto(
				rs.getInt("npc_id"), 
				rs.getInt("option_id"), 
				rs.getString("option_text"), 
				rs.getInt("point_id"), 
				rs.getInt("dialogue_src"), 
				rs.getInt("dialogue_dest"), 
				rs.getInt("next_point_id")));
		});
	}
	
	private static void cacheDialogueOptionDisplayCriteria() {
		addDialogueOptionDisplayCriteria(12, 2, 3, player -> 
			PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), Items.LEOS_BABY.getValue(), StorageTypes.INVENTORY) > 0);
		
		addDialogueOptionDisplayCriteria(12, 2, 4, player -> 
			PlayerStorageDao.getStorageItemCountByPlayerIdItemIdStorageTypeId(player.getId(), Items.LEOS_BABY.getValue(), StorageTypes.INVENTORY) > 0);
	}
	
	private static void addDialogueOptionDisplayCriteria(int npcId, int pointId, int optionId, Predicate<Player> fn) {
		dialogueOptionDisplayCriteria.putIfAbsent(npcId, new HashMap<>());
		dialogueOptionDisplayCriteria.get(npcId).putIfAbsent(pointId, new HashMap<>());
		dialogueOptionDisplayCriteria.get(npcId).get(pointId).put(optionId, fn);
	}
	
	private static void cacheDialogueEntryPoints() {
		DbConnection.load("select player_id, npc_id, point_id from player_npc_dialogue_entry_points", rs -> {
			dialogueEntryPointsByPlayerId.putIfAbsent(rs.getInt("player_id"), new HashMap<>());
			dialogueEntryPointsByPlayerId.get(rs.getInt("player_id")).put(rs.getInt("npc_id"), rs.getInt("point_id"));
		});
	}
	
	public static NpcDialogueDto getEntryDialogueByPlayerIdNpcId(int playerId, int npcId) {
		if (!dialogueEntryPointsByPlayerId.containsKey(playerId))
			return null;
		
		if (!dialogueEntryPointsByPlayerId.get(playerId).containsKey(npcId))
			return null;
		
		if (!dialogueByNpcId.containsKey(npcId))
			return null;
		
		final int pointId = dialogueEntryPointsByPlayerId.get(playerId).get(npcId);
		if (!dialogueByNpcId.get(npcId).containsKey(pointId))
			return null;
		
		return dialogueByNpcId.get(npcId).get(pointId).iterator().next();
	}
	
	public static NpcDialogueDto getDialogue(int npcId, int pointId, int dialogueId) {
		if (!dialogueByNpcId.containsKey(npcId))
			return null;
		
		if (!dialogueByNpcId.get(npcId).containsKey(pointId))
			return null;
		
		return dialogueByNpcId.get(npcId).get(pointId).stream()
				.filter(e -> e.getDialogueId() == dialogueId)
				.findFirst()
				.orElse(null);
	}

	public static List<NpcDialogueOptionDto> getDialogueOptionsBySrcDialogueId(int npcId, int pointId, int dialogueId) {
		if (!dialogueOptionsByNpcId.containsKey(npcId))
			return new ArrayList<>();
		
		if (!dialogueOptionsByNpcId.get(npcId).containsKey(pointId))
			return new ArrayList<>();
		
		return dialogueOptionsByNpcId.get(npcId).get(pointId).stream()
				.filter(e -> e.getDialogueSrc() == dialogueId)
				.collect(Collectors.toList());
	}
	
	public static NpcDialogueOptionDto getDialogueOption(int npcId, int pointId, int optionId, int srcDialogueId) {
		if (!dialogueOptionsByNpcId.containsKey(npcId))
			return null;
		
		if (!dialogueOptionsByNpcId.get(npcId).containsKey(pointId))
			return null;
		
		return dialogueOptionsByNpcId.get(npcId).get(pointId).stream()
				.filter(e -> e.getOptionId() == optionId && e.getDialogueSrc() == srcDialogueId)
				.findFirst()
				.orElse(null);
	}
	
	public static void setPlayerNpcDialogueEntryPoint(int playerId, int npcId, int pointId) {
		dialogueEntryPointsByPlayerId.putIfAbsent(playerId, new HashMap<>());
		
		if (dialogueEntryPointsByPlayerId.get(playerId).containsKey(npcId)) {
			DatabaseUpdater.enqueue(new UpdatePlayerNpcDialogueEntryPointEntity(playerId, npcId, pointId));
		} else {
			DatabaseUpdater.enqueue(new InsertPlayerNpcDialogueEntryPointEntity(playerId, npcId, pointId));
		}
		
		dialogueEntryPointsByPlayerId.get(playerId).put(npcId, pointId);
	}
	
	public static int getPlayerNpcDialogueEntryPoint(int playerId, int npcId) {
		if (!dialogueEntryPointsByPlayerId.containsKey(playerId))
			return 0;
		
		if (!dialogueEntryPointsByPlayerId.get(playerId).containsKey(npcId))
			return 0;
		
		return dialogueEntryPointsByPlayerId.get(playerId).get(npcId);
	}
	
	public static boolean dialogueOptionMeetsDisplayCriteria(NpcDialogueOptionDto option, Player player) {
		if (!dialogueOptionDisplayCriteria.containsKey(option.getNpcId()))
			return true; // if there's no display criteria then show the message
		
		if (!dialogueOptionDisplayCriteria.get(option.getNpcId()).containsKey(option.getPointId()))
			return true;
		
		if (!dialogueOptionDisplayCriteria.get(option.getNpcId()).get(option.getPointId()).containsKey(option.getOptionId()))
			return true;
		
		return dialogueOptionDisplayCriteria.get(option.getNpcId()).get(option.getPointId()).get(option.getOptionId()).test(player);
	}
	
	public static void handleSpecialDialogueLogic(NpcDialogueDto dialogue, Player player, ResponseMaps responseMaps) {
		if (!dialogueSpecialHandling.containsKey(dialogue.getNpcId()))
			return;
		
		if (!dialogueSpecialHandling.get(dialogue.getNpcId()).containsKey(dialogue.getPointId()))
			return;
		
		if (!dialogueSpecialHandling.get(dialogue.getNpcId()).get(dialogue.getPointId()).containsKey(dialogue.getDialogueId()))
			return;
		
		dialogueSpecialHandling.get(dialogue.getNpcId()).get(dialogue.getPointId()).get(dialogue.getDialogueId()).accept(player, responseMaps);
	}
	
	private static void cacheDialogueSpecialHandling() {
		addSpecialHandling(12, 2, 13, (player, responseMaps) -> {
			// leo gives the sword to the player
			List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
			if (invItemIds.contains(Items.LEOS_BABY.getValue())) {
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, invItemIds.indexOf(Items.LEOS_BABY.getValue()), Items.STEEL_SWORD_III.getValue(), 1, ItemDao.getMaxCharges(Items.STEEL_SWORD_III.getValue()));
				InventoryUpdateResponse.sendUpdate(player, responseMaps);
			}
		});
		
		final int capeGiverNpcId = 26; // tybalt atm but will probs switch
		Map.<Integer, Integer>of(
			13, Items.BLACK_CAPE.getValue(),
			15, Items.WHITE_CAPE.getValue(),
			17, Items.RED_CAPE.getValue(),
			19, Items.BLUE_CAPE.getValue(),
			21, Items.GREEN_CAPE.getValue()
		).forEach((dialogueId, capeId) -> {
			addSpecialHandling(capeGiverNpcId, 1, dialogueId, (player, responseMaps) -> {
				PlayerStorageDao.addItemToFirstFreeSlot(player.getId(), StorageTypes.INVENTORY, capeId, 1, ItemDao.getMaxCharges(capeId));
				InventoryUpdateResponse.sendUpdate(player, responseMaps);
				responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(String.format("%s hands you a %s.", NPCDao.getNpcNameById(capeGiverNpcId), ItemDao.getNameFromId(capeId, false)), "white"));
			});
		});
		
		addSpecialHandling(58, 1, 50, (player, responseMaps) -> {
			// TODO open artisan shop
		});
		
		addSpecialHandling(59, 1, 5, (player, responseMaps) -> {
			// TODO open clothilda's shop
		});
		
		addSpecialHandling(59, 1, 7, (player, responseMaps) -> 
			new ShowBaseAnimationsWindowResponse().process(null, player, responseMaps));
	}
	
	private static void addSpecialHandling(int npcId, int pointId, int dialogueId, BiConsumer<Player, ResponseMaps> fn) {
		dialogueSpecialHandling.putIfAbsent(npcId, new HashMap<>());
		dialogueSpecialHandling.get(npcId).putIfAbsent(pointId, new HashMap<>());
		dialogueSpecialHandling.get(npcId).get(pointId).put(dialogueId, fn);
	}
}
