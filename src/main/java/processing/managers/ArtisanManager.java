package processing.managers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import database.dao.BrewableDao;
import database.dao.ChoppableDao;
import database.dao.ConstructableDao;
import database.dao.CookableDao;
import database.dao.FishableDao;
import database.dao.ItemDao;
import database.dao.MineableDao;
import database.dao.PickableDao;
import database.dao.SawmillableDao;
import database.dao.SmeltableDao;
import database.dao.SmithableDao;
import database.dao.StatsDao;
import database.dao.UseItemOnItemDao;
import processing.attackable.Player;
import responses.MessageResponse;
import responses.ResponseMaps;
import types.Items;
import types.Stats;
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
		});
		
		// mineable is a gathering action, and therefore doesn't have child materials
		MineableDao.getAllMineables().forEach(mineable -> {
			itemMaterials.put(mineable.getItemId(), null);
			itemActions.put(mineable.getItemId(), "mine");
			itemLevels.put(mineable.getItemId(), Map.<Stats, Integer>of(Stats.MINING, mineable.getLevel()));
			itemAmounts.put(mineable.getItemId(), 100);
		});
		
		FishableDao.getAllFishables().forEach(fishable -> {
			itemMaterials.put(fishable.getItemId(), null);
			itemActions.put(fishable.getItemId(), "fish");
			itemLevels.put(fishable.getItemId(), Map.<Stats, Integer>of(Stats.FISHING, fishable.getLevel()));
			itemAmounts.put(fishable.getItemId(), 100);
		});
		
		CookableDao.getAllCookables().forEach(cookable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(cookable.getRawItemId(), 1);
			itemMaterials.put(cookable.getCookedItemId(), materials);
			itemActions.put(cookable.getCookedItemId(), "cook");
			itemLevels.put(cookable.getCookedItemId(), Map.<Stats, Integer>of(Stats.COOKING, cookable.getLevel()));
			itemAmounts.put(cookable.getCookedItemId(), 75);
		});
		
		ChoppableDao.getAllChoppables().forEach(choppable -> {
			itemMaterials.put(choppable.getLogId(), null);
			itemActions.put(choppable.getLogId(), "chop");
			itemLevels.put(choppable.getLogId(), Map.<Stats, Integer>of(Stats.WOODCUTTING, choppable.getLevel()));
			itemAmounts.put(choppable.getLogId(), 100);
		});
		
		SawmillableDao.getSawmillable().forEach(sawmillable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(sawmillable.getLogId(), 3); // three logs make a plank
			itemMaterials.put(sawmillable.getResultingPlankId(), materials);
			itemActions.put(sawmillable.getResultingPlankId(), "sawmill");
			itemLevels.put(sawmillable.getResultingPlankId(), Map.<Stats, Integer>of(Stats.CONSTRUCTION, sawmillable.getRequiredLevel()));
			itemAmounts.put(sawmillable.getResultingPlankId(), 50);
		});
		
		PickableDao.getAllPickables().forEach(pickable -> {
			itemMaterials.put(pickable.getItemId(), null);
			itemActions.put(pickable.getItemId(), "pick");
			itemAmounts.put(pickable.getItemId(), 100);
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
		});
		
		// the potionIds will have been added in the useItemOnItem insert; we can pull the levels from the brewable table
		BrewableDao.getAllBrewables().forEach(brewable -> {
			// not all potions are available for making yet (such as magic potion)
			if (itemMaterials.containsKey(brewable.getPotionId()))
				itemLevels.put(brewable.getPotionId(), Map.<Stats, Integer>of(Stats.HERBLORE, brewable.getLevel()));
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
			}
		});
	}
	
	public static Map<Integer, Map<Integer, Integer>> getStepsFromItemId(int itemId, int requiredItems) {
		Map<Integer, Map<Integer, Integer>> map = new LinkedHashMap<>();
		getStepsFromItemId(itemId, requiredItems, map, 1);
		return map;
	}
	
	private static Map<Integer, Map<Integer, Integer>> getStepsFromItemId(int itemId, int requiredItems, Map<Integer, Map<Integer, Integer>> map, int depth) { // task, ${done}/${total}
		if (!itemMaterials.containsKey(itemId))
			return map;
		
		map.putIfAbsent(depth, new HashMap<>());
		map.get(depth).merge(itemId, requiredItems, Integer::sum);
		if (itemMaterials.get(itemId) == null)
			return map;
		
		itemMaterials.get(itemId).forEach((materialId, requiredCount) -> {
			Map<Integer, Map<Integer, Integer>> nextSteps = new HashMap<>();
			getStepsFromItemId(materialId, requiredCount * requiredItems, nextSteps, depth + 1);
			nextSteps.forEach((currentDepth, currentMap) -> {
				map.putIfAbsent(currentDepth, new HashMap<>());
				
				currentMap.forEach((currentItemId, currentRequiredCount) ->
					map.get(currentDepth).merge(currentItemId, currentRequiredCount, Integer::sum));
			});
		});
		
		return map;
	}
	
	public static String getActionFromItemId(int itemId) {
		if (!itemActions.containsKey(itemId))
			return "make";
		return itemActions.get(itemId);
	}
	
	// note we only check the final item they can make.
	// for example, if they are 99 smithing and 1 mining, they can still be assigned
	// a "smith rune helmet" task or whatever; they'll just need to buy the ore instead of mining it.
	private static List<Integer> getItemsPlayerCanMake(int playerId) {
		return itemLevels.entrySet().stream()
				.filter(e -> e.getValue().entrySet().stream()
					.anyMatch(entry -> StatsDao.getStatLevelByStatIdPlayerId(entry.getKey(), playerId) >= entry.getValue()))
		.map(e -> e.getKey())
		.collect(Collectors.toList());
	}
	
	public static void newTask(Player player, ResponseMaps responseMaps) {
		final List<Integer> itemsPlayerCanMake = getItemsPlayerCanMake(player.getId());
		final int taskItemId = itemsPlayerCanMake.get(RandomUtil.getRandom(0, itemsPlayerCanMake.size()));
		
		int numItemsToMake = 100;
		if (itemAmounts.containsKey(taskItemId)) {
			numItemsToMake = RandomUtil.getRandomInRange(itemAmounts.get(taskItemId), itemAmounts.get(taskItemId) / 3);
		} else {
			System.out.println(String.format("taskItemId %d (%s) not found in itemAmounts map", taskItemId, ItemDao.getNameFromId(taskItemId)));
		}
		
		
		final String taskMessage = String.format("new artisan task: %s %dx %s.", getActionFromItemId(taskItemId), numItemsToMake, ItemDao.getNameFromId(taskItemId));
		responseMaps.addClientOnlyResponse(player, MessageResponse.newMessageResponse(taskMessage, "#23f5b4"));
		
		Map<Integer, Map<Integer, Integer>> steps = ArtisanManager.getStepsFromItemId(taskItemId, numItemsToMake);
		steps.forEach((depth, map) -> {
			map.forEach((itemId, requiredCount) ->
				System.out.println(" ".repeat(depth * 2) + ArtisanManager.getActionFromItemId(itemId) + String.format(" %dx %s", requiredCount, ItemDao.getNameFromId(itemId))));
		});
	}
}
