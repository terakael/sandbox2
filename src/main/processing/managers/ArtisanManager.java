package main.processing.managers;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import main.database.dao.ChoppableDao;
import main.database.dao.ConstructableDao;
import main.database.dao.CookableDao;
import main.database.dao.FishableDao;
import main.database.dao.MineableDao;
import main.database.dao.PickableDao;
import main.database.dao.SawmillableDao;
import main.database.dao.SmeltableDao;
import main.database.dao.SmithableDao;
import main.database.dao.UseItemOnItemDao;
import main.types.Items;

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
	private static Map<Integer, Map<Integer, Integer>> itemMaterials; // itemId, <materialItemId, count>
	private static Map<Integer, String> itemActions; // just used to say "mine" x coal, "smelt" x bars etc
	
	public static void setupCaches() {
		setupItemMaterials();
	}
	
	private static void setupItemMaterials() {
		itemMaterials = new HashMap<>();
		itemActions = new HashMap<>();
		SmithableDao.getAllSmithables().forEach(smithable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(smithable.getBarId(), smithable.getRequiredBars());
			itemMaterials.put(smithable.getItemId(), materials);
			itemActions.put(smithable.getItemId(), "smith");
		});
		
		SmeltableDao.getAllSmeltables().forEach(smeltable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(smeltable.getOreId(), smeltable.getRequiredOre());
			if (smeltable.getRequiredCoal() > 0)
				materials.put(Items.COAL_ORE.getValue(), smeltable.getRequiredCoal());
			itemMaterials.put(smeltable.getBarId(), materials);
			itemActions.put(smeltable.getBarId(), "smelt");
		});
		
		// mineable is a gathering action, and therefore doesn't have child materials
		MineableDao.getAllMineables().forEach(mineable -> {
			itemMaterials.put(mineable.getItemId(), null);
			itemActions.put(mineable.getItemId(), "mine");
		});
		
		FishableDao.getAllFishables().forEach(fishable -> {
			itemMaterials.put(fishable.getItemId(), null);
			itemActions.put(fishable.getItemId(), "fish");
		});
		
		CookableDao.getAllCookables().forEach(cookable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(cookable.getRawItemId(), 1);
			itemMaterials.put(cookable.getCookedItemId(), materials);
			itemActions.put(cookable.getCookedItemId(), "cook");
		});
		
		ChoppableDao.getAllChoppables().forEach(choppable -> {
			itemMaterials.put(choppable.getLogId(), null);
			itemActions.put(choppable.getLogId(), "chop");
		});
		
		SawmillableDao.getSawmillable().forEach(sawmillable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			materials.put(sawmillable.getLogId(), 3);
			itemMaterials.put(sawmillable.getResultingPlankId(), materials);
			itemActions.put(sawmillable.getResultingPlankId(), "sawmill");
		});
		
		PickableDao.getAllPickables().forEach(pickable -> {
			itemMaterials.put(pickable.getItemId(), null);
			itemActions.put(pickable.getItemId(), "pick");
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
		
		// constructables should be done at the end because we only want to add artisan tertiary items to the list.
		// at this point the rest of the artisan items have been added, so it makes it easier to add the correct constructable materials.
		ConstructableDao.getAllConstructables().forEach(constructable -> {
			Map<Integer, Integer> materials = new HashMap<>();
			if (constructable.getPlankAmount() > 0)
				materials.put(constructable.getPlankId(), constructable.getPlankAmount());
			
			if (constructable.getBarAmount() > 0)
				materials.put(constructable.getBarId(), constructable.getBarAmount());
			
			if (itemMaterials.containsKey(constructable.getTertiaryId()))
				materials.put(constructable.getTertiaryId(), constructable.getTertiaryAmount());
			
			itemMaterials.put(constructable.getFlatpackItemId(), materials);
			itemActions.put(constructable.getFlatpackItemId(), "build");
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
}
