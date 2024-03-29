package processing.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import database.dao.ArtisanMasterDao;
import database.dao.ArtisanTaskItemReplacementDao;
import database.dao.BrewableDao;
import database.dao.ChoppableDao;
import database.dao.ConstructableDao;
import database.dao.CookableDao;
import database.dao.FishableDao;
import database.dao.ItemDao;
import database.dao.MineableDao;
import database.dao.NPCDao;
import database.dao.PickableDao;
import database.dao.PlayerArtisanBlockedTaskDao;
import database.dao.PlayerArtisanTaskBreakdownDao;
import database.dao.PlayerArtisanTaskDao;
import database.dao.PlayerStorageDao;
import database.dao.SawmillableDao;
import database.dao.SmeltableDao;
import database.dao.SmithableDao;
import database.dao.StatsDao;
import database.dao.UseItemOnItemDao;
import database.dto.ArtisanMasterDto;
import database.dto.ArtisanMaterialChainDto;
import database.dto.PlayerArtisanTaskBreakdownDto;
import database.dto.PlayerArtisanTaskDto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import processing.attackable.Player;
import processing.tybaltstasks.updates.CompleteArtisanTaskUpdate;
import requests.AddExpRequest;
import responses.AddExpResponse;
import responses.InventoryUpdateResponse;
import responses.MessageResponse;
import responses.ResponseMaps;
import types.Items;
import types.Stats;
import types.StorageTypes;
import utils.RandomUtil;

public class ArtisanManager {
	// we can use this container to recursively run through an entire material chain to find what we need.
	// for example: steel helmet.
	// search steel helmet id (21), which exists as a key in the map
	// check the value, which holds the next item (<steel bar, 3>)
	// search steel bar id (329), which exists as a key in the map
	// check the value, which holds the next items (<iron ore, 1>, <coal, 2>)
	// search iron ore id (4), which exists as a key in the map
	// check the value, which is null, meaning it's not made from anything
	// search coal id (5), which exists as a key in teh map
	// check the value, which is null, meaning it is not made from anything
	// from here, we can reverse back down the stack creating the list:
	// - 6 coal
	// - 3 iron ore
	//   - 3 steel bar
	//     - 1 steel helmet
	private static final Map<Integer, Map<Integer, Integer>> itemMaterials = new HashMap<>(); // itemId, <materialItemId, count>
	private static final Map<Integer, String> itemActions = new HashMap<>(); // just used to say "mine" x coal, "smelt" x bars etc
	private static final Map<Integer, Map<Stats, Integer>> itemLevels = new HashMap<>(); // itemId, <stat, level> - so we can filter out the tasks the player can't do
	private static final Map<Integer, Integer> itemExp = new HashMap<>();
	private static final Map<Integer, Integer> itemPoints = new HashMap<>(); // itemId, points
	
	
	@Getter
	@RequiredArgsConstructor
	private static class TaskUpdate {
		private final int itemId;
		private final int parentItemId;
		private final int count;
	}
	
	// general rule:
	// tertiary tasks (fishing, mining, woodcutting etc) have an anchor of 100
	// secondary tasks (smelting, sawmilling) have an anchor of 75
	// primary tasks (smithing, brewing) have an anchor of 50
	// construction has an anchor of 20 because it has so many materials
	private static final Map<Integer, Integer> itemAmounts = new HashMap<>(); // the value is the anchor amount; the offset can be +/- 30% (100 would be 70-130; 10 would be 7-13)
	
	public static void setupCaches() {
		setupItemMaterials();
	}
	
	private static void setupItemMaterials() {
		SmithableDao.getAllSmithables().forEach(smithable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(smithable.getBarId(), smithable.getRequiredBars());
			itemMaterials.put(smithable.getItemId(), materials);
			itemActions.put(smithable.getItemId(), "smith");
			itemLevels.put(smithable.getItemId(), Map.<Stats, Integer>of(Stats.SMITHING, smithable.getLevel()));
			itemAmounts.put(smithable.getItemId(), 50);
			itemExp.put(smithable.getItemId(), SmeltableDao.getSmeltableByBarId(smithable.getBarId()).getExp() * smithable.getRequiredBars());
		});
		
		SmeltableDao.getAllSmeltables().forEach(smeltable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(smeltable.getOreId(), smeltable.getRequiredOre());
			if (smeltable.getRequiredCoal() > 0)
				materials.put(Items.COAL_ORE.getValue(), smeltable.getRequiredCoal());
			itemMaterials.put(smeltable.getBarId(), materials);
			itemActions.put(smeltable.getBarId(), "smelt");
			itemLevels.put(smeltable.getBarId(), Map.<Stats, Integer>of(Stats.SMITHING, smeltable.getLevel()));
			itemAmounts.put(smeltable.getBarId(), 75);
			itemExp.put(smeltable.getBarId(), smeltable.getExp());
		});
		
		// mineable is a gathering action, and therefore doesn't have child materials
		MineableDao.getAllMineables().forEach(mineable -> {
			itemMaterials.put(mineable.getItemId(), null);
			itemActions.put(mineable.getItemId(), "mine");
			itemLevels.put(mineable.getItemId(), Map.<Stats, Integer>of(Stats.MINING, mineable.getLevel()));
			itemAmounts.put(mineable.getItemId(), 100);
			itemExp.put(mineable.getItemId(), mineable.getExp());
		});
		
		FishableDao.getAllFishables().forEach(fishable -> {
			itemMaterials.put(fishable.getItemId(), null);
			itemActions.put(fishable.getItemId(), "fish");
			itemLevels.put(fishable.getItemId(), Map.<Stats, Integer>of(Stats.FISHING, fishable.getLevel()));
			itemAmounts.put(fishable.getItemId(), 100);
			itemExp.put(fishable.getItemId(), fishable.getExp());
		});
		
		CookableDao.getAllCookables().forEach(cookable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(cookable.getRawItemId(), 1);
			itemMaterials.put(cookable.getCookedItemId(), materials);
			itemActions.put(cookable.getCookedItemId(), "cook");
			itemLevels.put(cookable.getCookedItemId(), Map.<Stats, Integer>of(Stats.COOKING, cookable.getLevel()));
			itemAmounts.put(cookable.getCookedItemId(), 75);
			itemExp.put(cookable.getCookedItemId(), cookable.getExp());
		});
		
		ChoppableDao.getAllChoppables().forEach(choppable -> {
			itemMaterials.put(choppable.getLogId(), null);
			itemActions.put(choppable.getLogId(), "chop");
			itemLevels.put(choppable.getLogId(), Map.<Stats, Integer>of(Stats.WOODCUTTING, choppable.getLevel()));
			itemAmounts.put(choppable.getLogId(), 100);
			itemExp.put(choppable.getLogId(), choppable.getExp());
		});
		
		SawmillableDao.getSawmillable().forEach(sawmillable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(sawmillable.getLogId(), 3); // three logs make a plank
			itemMaterials.put(sawmillable.getResultingPlankId(), materials);
			itemActions.put(sawmillable.getResultingPlankId(), "sawmill");
			itemLevels.put(sawmillable.getResultingPlankId(), Map.<Stats, Integer>of(Stats.CONSTRUCTION, sawmillable.getRequiredLevel()));
			itemAmounts.put(sawmillable.getResultingPlankId(), 50);
			itemExp.put(sawmillable.getResultingPlankId(), sawmillable.getExp());
		});
		
		PickableDao.getAllPickables().forEach(pickable -> {
			itemMaterials.put(pickable.getItemId(), null);
			itemActions.put(pickable.getItemId(), "pick");
			itemAmounts.put(pickable.getItemId(), 100);
			itemExp.put(pickable.getItemId(), 10);
		});
		
		// brewable is handled here, as the "getResultingItemId" includes the potions.
		// only include artisan items, i.e. those defined above
		// for example, goblin stank uses dark bluebell mix, and goblin nails.
		// we want to include the dark bluebell mix (and subsequently the bluebell itself)
		// but we don't want to include the goblin nails as that's obtained through monster drop
		UseItemOnItemDao.getAllDtos().forEach(useItemOnItem -> {
			// there's an ordering issue where sometimes the following happens:
			// we got to add a potion, then check if the skyflower mix is an artisan item.
			// it hasn't been added yet as it's further in the loop, so returns false.
			// therefore we add all the resulting item ids initially, then run through
			// and overwrite them in a second loop.
			itemMaterials.put(useItemOnItem.getResultingItemId(), null);
			itemActions.put(useItemOnItem.getResultingItemId(), "make");
			itemAmounts.put(useItemOnItem.getResultingItemId(), 50);
		});
		
		// now all the resultingItemIds have been added as artisan items, 
		// we can run through and add the materials without worrying about order.
		UseItemOnItemDao.getAllDtos().forEach(useItemOnItem -> {
			Map<Integer, Integer> materials = new HashMap<>();
			if (itemMaterials.containsKey(useItemOnItem.getSrcId()))
				materials.put(useItemOnItem.getSrcId(), useItemOnItem.getRequiredSrcCount());
			
			if (itemMaterials.containsKey(useItemOnItem.getDestId()))
				materials.put(useItemOnItem.getDestId(), 1);
			
			// overwrite what was added previously
			itemMaterials.put(useItemOnItem.getResultingItemId(), materials);
			itemExp.put(useItemOnItem.getResultingItemId(), 10); // e.g. making mixes - potions will be overwritten in the next part
		});
		
		// the potionIds will have been added in the useItemOnItem insert; we can pull the levels from the brewable table
		BrewableDao.getAllBrewables().forEach(brewable -> {
			// not all potions are available for making yet (such as magic potion)
			if (itemMaterials.containsKey(brewable.getPotionId())) {
				itemLevels.put(brewable.getPotionId(), Map.<Stats, Integer>of(Stats.HERBLORE, brewable.getLevel()));
				itemExp.put(brewable.getPotionId(), brewable.getExp());
			}
		});
		
		// constructables should be done at the end because we only want to add artisan tertiary items to the list.
		// at this point the rest of the artisan items have been added, so it makes it easier to add the correct constructable materials.
		ConstructableDao.getAllConstructables().forEach(constructable -> {
			if (constructable.getFlatpackItemId() > 0) {
				Map<Integer, Integer> materials = new HashMap<>();
				if (constructable.getPlankAmount() > 0)
					materials.put(constructable.getPlankId(), constructable.getPlankAmount());
				
				if (constructable.getBarAmount() > 0)
					materials.put(constructable.getBarId(), constructable.getBarAmount());
				
				if (itemMaterials.containsKey(constructable.getTertiaryId()))
					materials.put(constructable.getTertiaryId(), constructable.getTertiaryAmount());
				
				itemMaterials.put(constructable.getFlatpackItemId(), materials);
				itemActions.put(constructable.getFlatpackItemId(), "build");
				itemLevels.put(constructable.getFlatpackItemId(), Map.<Stats, Integer>of(Stats.CONSTRUCTION, constructable.getLevel()));
				itemAmounts.put(constructable.getFlatpackItemId(), 13);
				itemExp.put(constructable.getFlatpackItemId(), constructable.getExp());
			}
		});
		
		// once everything has been added, remove the replacements from the materials list
		// replacements are, for example, flasks (you cannot be assigned flasks because they take points to buy, meaning you could get stuck with no points).
		// however, if you get assigned a brew task, making the equivalent flask counts towards the task.
		// TODO man these lists need to be grouped into some kinda object
		final Set<Integer> replacementItems = ArtisanTaskItemReplacementDao.getReplacementMap().keySet();
		itemMaterials.entrySet().removeIf(e -> replacementItems.contains(e.getKey()));
		itemActions.entrySet().removeIf(e -> replacementItems.contains(e.getKey()));
		itemLevels.entrySet().removeIf(e -> replacementItems.contains(e.getKey()));
		itemAmounts.entrySet().removeIf(e -> replacementItems.contains(e.getKey()));
		itemExp.entrySet().removeIf(e -> replacementItems.contains(e.getKey()));
		
		itemMaterials.keySet().forEach(itemId -> {
			// the number of points an item gives is the sum of all the actions it takes:
			// copper ore: 1 point for mining it
			// rune ore: 1 point for mining it
			// iron platebody: 1 point for smithing it; 5 points for smelting 5 bars; 5 points for mining 5 ores: 11 points
			// rune platebody: 1 point for smithing it, 5 points for smelting 5 bars, 5+(5*8) points for mining all rune and coal: 51 points
			itemPoints.put(itemId, getStepsFromItemId(itemId, 1).values().stream().mapToInt(Integer::intValue).sum());
		});
	}
	
	public static Map<Integer, Integer> getStepsFromItemId(int itemId, int requiredItems) {
		Map<Integer, Integer> map = new LinkedHashMap<>();
		getStepsFromItemId(itemId, requiredItems, map, 1);
		return map;
	}
	
	private static Map<Integer, Integer> getStepsFromItemId(int itemId, int requiredItems, Map<Integer, Integer> map, int depth) { // task, ${done}/${total}
		if (!itemMaterials.containsKey(itemId))
			return map;
		
//		map.putIfAbsent(depth, new LinkedHashMap<>());
		map.merge(itemId, requiredItems, Integer::sum);
		if (itemMaterials.get(itemId) == null)
			return map;
		
		itemMaterials.get(itemId).forEach((materialId, requiredCount) -> {
			Map<Integer, Integer> nextSteps = new LinkedHashMap<>();
			getStepsFromItemId(materialId, requiredCount * requiredItems, nextSteps, depth + 1);
				nextSteps.forEach((currentItemId, currentRequiredCount) ->
					map.merge(currentItemId, currentRequiredCount, Integer::sum));
		});
		
		return map;
	}
	
	private static void cascadeOnItemId(TaskUpdate taskUpdate, Consumer<TaskUpdate> fn) {
		if (!itemMaterials.containsKey(taskUpdate.getItemId()))
			return;
			
		fn.accept(taskUpdate);
		if (itemMaterials.get(taskUpdate.getItemId()) == null)
			return;
			
		itemMaterials.get(taskUpdate.getItemId()).forEach((materialId, requiredCount) -> {
			cascadeOnItemId(new TaskUpdate(materialId, taskUpdate.getItemId(), taskUpdate.getCount() * requiredCount), fn);
		});
	}
	
	public static String getActionFromItemId(int itemId) {
		if (!itemActions.containsKey(itemId))
			return "make";
		return itemActions.get(itemId);
	}
	
	// note we only check the final item they can make.
	// for example, if they are 99 smithing and 1 mining, they can still be assigned
	// a "smith rune helmet" task or whatever; they'll just need to buy the ore instead of mining it.
	// also different artisan masters have different level ranges; e.g. alaina in tyrotown only assigns things crafted between level 1-20 in a skill
	private static List<Integer> getItemsPlayerCanMake(int playerId, int minRange, int maxRange) {
		return itemLevels.entrySet().stream()
				.filter(e -> e.getValue().entrySet().stream()
					.anyMatch(entry -> {
						return entry.getValue() >= minRange && entry.getValue() <= maxRange && StatsDao.getStatLevelByStatIdPlayerId(entry.getKey(), playerId) >= entry.getValue();
					}))
		.map(e -> e.getKey())
		.collect(Collectors.toList());
	}
	
	public static void newTask(Player player, ArtisanMasterDto master, ResponseMaps responseMaps) {
		final Set<Integer> blockedItems = PlayerArtisanBlockedTaskDao.getBlockedItemIds(player.getId());
		final List<Integer> itemsPlayerCanMake = getItemsPlayerCanMake(player.getId(), master.getAssignmentLevelRangeMin(), master.getAssignmentLevelRangeMax()).stream()
				.filter(itemId -> !blockedItems.contains(itemId))
				.collect(Collectors.toList());
		
		
		final int taskItemId = itemsPlayerCanMake.get(RandomUtil.getRandom(0, itemsPlayerCanMake.size()));
		
		int numItemsToMake = 100;
		if (itemAmounts.containsKey(taskItemId)) {
			numItemsToMake = RandomUtil.getRandomInRange(itemAmounts.get(taskItemId), itemAmounts.get(taskItemId) / 3);
		} else {
			System.out.println(String.format("taskItemId %d (%s) not found in itemAmounts map", taskItemId, ItemDao.getNameFromId(taskItemId, false)));
			return;
		}
		
		// different masters give different ranges; this range is based on level range as a percentage, i.e. maxRange 20 means 20% of the total need to be made
		numItemsToMake /= (100 / master.getAssignmentLevelRangeMax());
		
		// artisan task dao holds all the artisan tasks (main and subtasks) and tracks how many remain
		PlayerArtisanTaskBreakdownDao.clearTask(player.getId());
		
		// artisan task item dao holds the main task item, and records how many finished items are handed in to the artisan master
		PlayerArtisanTaskDao.newTask(player.getId(), master.getNpcId(), taskItemId, numItemsToMake);
		
		final String taskMessage = String.format("new artisan task: %s %dx %s.", getActionFromItemId(taskItemId), numItemsToMake, ItemDao.getNameFromId(taskItemId, numItemsToMake != 1));
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(taskMessage, "#23f5b4"));
		
		ArtisanManager.getStepsFromItemId(taskItemId, numItemsToMake).forEach((itemId, requiredCount) -> {
			PlayerArtisanTaskBreakdownDao.addTaskItem(player.getId(), itemId, requiredCount);
		});
	}
	
	public static boolean playerHasTask(int playerId) {
		return PlayerArtisanTaskDao.getTask(playerId) != null && !currentTaskIsFinished(playerId);
	}
	
	public static String getTaskString(int playerId) {
		// "your current task is ${artisanTask}."
		
		final PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(playerId);
		if (task == null)
			return "not assigned";
		
		return String.format("to %s %d %s", getActionFromItemId(task.getItemId()), task.getAssignedAmount(), ItemDao.getNameFromId(task.getItemId(), task.getAssignedAmount() != 1));
	}
	
	public static boolean currentTaskIsFinished(int playerId) {
		final PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(playerId);
		return task == null || !PlayerArtisanTaskBreakdownDao.taskIsValid(playerId, task.getItemId());
	}
	
	public static void check(Player player, int itemId, ResponseMaps responseMaps) {
		// if, for example, it's a flask, then switch the item to the equivalent vial brew
		if (ArtisanTaskItemReplacementDao.getReplacementMap().containsKey(itemId))
			itemId = ArtisanTaskItemReplacementDao.getReplacementMap().get(itemId);
		
		checkInternal(player, itemId, responseMaps);
	}
	
	private static void checkInternal(Player player, int itemId, ResponseMaps responseMaps) {		
		if (!PlayerArtisanTaskBreakdownDao.taskIsValid(player.getId(), itemId))
			return;
		
		// only subtract the count from the child items when the remaining count is higher than the parents remaining count
		// e.g. task is making 10 planks (requiring 30 logs)
		// player cuts 9 logs
		// now has a remaining task of 10 planks and 21 logs
		// player sawmills three logs
		// remaining task should be 7 planks and 21 logs (logs didn't decrease)
		// however if the player gets three more logs from the bank and sawmills those into another plank
		// then the remaining task will be 6 planks and 18 logs (logs decreased)
		final Map<Integer, Integer> remainingCountsByItemId = PlayerArtisanTaskBreakdownDao.getTaskList(player.getId()).stream()
				.collect(Collectors.toMap(PlayerArtisanTaskBreakdownDto::getItemId, PlayerArtisanTaskBreakdownDto::getAmount));
				
		cascadeOnItemId(new TaskUpdate(itemId, -1, 1), taskUpdate -> {
			if (taskUpdate.getParentItemId() == -1 || remainingCountsByItemId.get(itemId) * taskUpdate.getCount() < remainingCountsByItemId.get(taskUpdate.getItemId())) {
				PlayerArtisanTaskBreakdownDao.updateTask(player.getId(), taskUpdate.getItemId(), taskUpdate.getCount());
				remainingCountsByItemId.put(taskUpdate.getItemId(), remainingCountsByItemId.get(taskUpdate.getItemId()) - taskUpdate.getCount());
			}
		});
		
		new AddExpResponse().process(new AddExpRequest(player.getId(), Stats.ARTISAN, itemExp.get(itemId)), player, responseMaps);
		
		final int playerMainTaskItemId = PlayerArtisanTaskDao.getTaskItemId(player.getId());
		if (!PlayerArtisanTaskBreakdownDao.taskIsValid(player.getId(), playerMainTaskItemId)) {
			PlayerArtisanTaskDto updatedTask = PlayerArtisanTaskDao.finishTask(player.getId());
			
			final int grantedPoints = ArtisanMasterDao.getCompletionPointsByArtisanMasterId(updatedTask.getAssignedMasterId(), updatedTask.getTotalTasks());
			
			final String message = String.format("you have completed %d artisan tasks, and receive %d points for a total of %d points.", 
									updatedTask.getTotalTasks(),
									grantedPoints,
									updatedTask.getTotalPoints());
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, "#23f5b4"));
			
			TybaltsTaskManager.check(player, new CompleteArtisanTaskUpdate(updatedTask.getAssignedMasterId()), responseMaps);
		}
	}
	
	public static List<ArtisanMaterialChainDto> getTaskList(int playerId) {
		List<ArtisanMaterialChainDto> taskCompletions = new LinkedList<>();
		
		final PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(playerId);
		if (task == null)
			return taskCompletions;
			
		final Map<Integer, Integer> remainingCountsByItemId = PlayerArtisanTaskBreakdownDao.getTaskList(playerId).stream()
			.collect(Collectors.toMap(PlayerArtisanTaskBreakdownDto::getItemId, PlayerArtisanTaskBreakdownDto::getAmount));
		
		cascadeOnItemId(new TaskUpdate(task.getItemId(), -1, task.getAssignedAmount()), taskUpdate -> {
			ArtisanMaterialChainDto dto = new ArtisanMaterialChainDto(taskUpdate.getItemId(), taskUpdate.getCount() - remainingCountsByItemId.get(taskUpdate.getItemId()), taskUpdate.getCount());
			ArtisanMaterialChainDto parentDto = taskCompletions.stream()
				.flatMap(ArtisanMaterialChainDto::flattened)
				.filter(e -> e.getItemId() == taskUpdate.getParentItemId())
				.findFirst().orElse(null);
				
			if (parentDto != null) {
				parentDto.addChild(dto);
			} else {
				taskCompletions.add(dto);
			}
		});
	
		return taskCompletions;
	}
	
	public static boolean handleUseItemOnMaster(Player player, ArtisanMasterDto master, int itemId, ResponseMaps responseMaps) {
		PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(player.getId());
		if (task == null)
			return false;
		
		int replacementItemId = itemId;
		if (ArtisanTaskItemReplacementDao.getReplacementMap().containsKey(itemId))
			replacementItemId = ArtisanTaskItemReplacementDao.getReplacementMap().get(itemId);
	
		if (task.getItemId() != replacementItemId)
			return false;	
			
		final int maxAmountMasterWillAccept = task.getAssignedAmount() - task.getHandedInAmount();
		
		final int remainingTaskCount = PlayerArtisanTaskBreakdownDao.getRemainingTaskCount(player.getId(), replacementItemId);
		
		// you can't hand in items that you haven't made yourself
		final int totalItemsDoneAndNotHandedIn = maxAmountMasterWillAccept - remainingTaskCount;
		
		// start off with total amount in inventory
		final List<Integer> invItemIds = PlayerStorageDao.getStorageListByPlayerId(player.getId(), StorageTypes.INVENTORY);
		int numTaskItemsToHandIn = Collections.frequency(invItemIds, itemId);
		
		// what's smaller, the amount in the inventory or the number you've actually made
		numTaskItemsToHandIn = Math.min(numTaskItemsToHandIn, totalItemsDoneAndNotHandedIn);
		
		// what's smaller, the previous result or the amount the master will actually accept
		numTaskItemsToHandIn = Math.min(numTaskItemsToHandIn, maxAmountMasterWillAccept);
		
		final int numItemsHandedIn = PlayerArtisanTaskDao.handInItems(player.getId(), numTaskItemsToHandIn);
		
		final String message = String.format("you have completed %d/%d %s, and handed in %d.", task.getAssignedAmount() - remainingTaskCount, task.getAssignedAmount(), ItemDao.getNameFromId(replacementItemId, true), task.getHandedInAmount());
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(message, "white"));
		
		if (numItemsHandedIn > 0) {
			for (int i = 0; i < numItemsHandedIn; ++i) {
				int idx = invItemIds.indexOf(itemId);
				PlayerStorageDao.setItemFromPlayerIdAndSlot(player.getId(), StorageTypes.INVENTORY, idx, 0, 0, 0);
				invItemIds.set(idx, 0);
			}
			InventoryUpdateResponse.sendUpdate(player, responseMaps);
			
			final int points = itemPoints.get(replacementItemId) * numItemsHandedIn;
			responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(String.format("%s grants you %d point%s.", NPCDao.getNpcNameById(master.getNpcId()), points, points == 1 ? "" : "s"), "white"));
			
			PlayerArtisanTaskDao.spendPoints(player.getId(), -points);
		}
	
		return true;
	}
	
	public static int getTotalPointsByPlayerId(int playerId) {
		final PlayerArtisanTaskDto task = PlayerArtisanTaskDao.getTask(playerId);
		if (task == null)
			return 0;
		return task.getTotalPoints();
	}
	
	public static void spendPoints(int playerId, int spentPoints) {
		PlayerArtisanTaskDao.spendPoints(playerId, spentPoints);
	}
	
	public static boolean playerIsNearMaster(Player player) {
		return LocationManager.getLocalNpcs(player.getFloor(), player.getTileId(), 5, TimeManager.isDaytime()).stream()
				.map(npc -> npc.getDto().getId())
				.anyMatch(ArtisanMasterDao.getAllArtisanMasterNpcIds()::contains);
	}
	
	public static int getPointsForItemId(int itemId) {
		if (!itemPoints.containsKey(itemId))
			return 0;
		return itemPoints.get(itemId);
	}
}
