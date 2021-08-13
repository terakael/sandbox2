package database.dao;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import database.dto.ArtisanMasterDto;

public class ArtisanMasterDao {
	private static final Map<Integer, ArtisanMasterDto> artisanMasters = new HashMap<>(); // npcId, dto
	private static final Map<Integer, Integer> taskCompletionMultipliers = new LinkedHashMap<>(); // taskCount, multiplier
	
	public static void setupCaches() {
		setupMasters();
		setupTaskCompletionMultipliers();
	}
	
	private static void setupMasters() {
		DbConnection.load("select npc_id, artisan_requirement, assignment_level_range_min, assignment_level_range_max, completion_points from artisan_masters", rs -> {
			artisanMasters.put(rs.getInt("npc_id"), new ArtisanMasterDto(
							rs.getInt("npc_id"), 
							rs.getInt("artisan_requirement"), 
							rs.getInt("assignment_level_range_min"), 
							rs.getInt("assignment_level_range_max"),
							rs.getInt("completion_points")));
		});
	}
	
	private static void setupTaskCompletionMultipliers() {
		taskCompletionMultipliers.put(1000, 21);
		taskCompletionMultipliers.put(500, 18);
		taskCompletionMultipliers.put(250, 15);
		taskCompletionMultipliers.put(100, 12);
		taskCompletionMultipliers.put(50, 9);
		taskCompletionMultipliers.put(25, 6);
		taskCompletionMultipliers.put(10, 3);
	}
	
	public static ArtisanMasterDto getArtisanMasterByNpcId(int npcId) {
		return artisanMasters.get(npcId);
	}
	
	public static boolean npcIsArtisanMaster(int npcId) {
		return getArtisanMasterByNpcId(npcId) != null;
	}
	
	public static int getCompletionPointsByArtisanMasterId(int masterId, int totalTasks) {
		if (!artisanMasters.containsKey(masterId))
			return 0;
		
		final int multiplier = taskCompletionMultipliers.entrySet().stream()
				.filter(e -> totalTasks % e.getKey() == 0)
				.mapToInt(Map.Entry::getValue)
				.sum() + 1;
		
		return artisanMasters.get(masterId).getCompletionPoints() * multiplier;
	}
	
	public static Set<Integer> getAllArtisanMasterNpcIds() {
		return artisanMasters.keySet();
	}
}
