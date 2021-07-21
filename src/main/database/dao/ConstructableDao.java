package main.database.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import main.database.DbConnection;
import main.database.dto.ConstructableDto;

public class ConstructableDao {
	private static List<ConstructableDto> constructables = new ArrayList<>();
	private static Set<Integer> constructionToolIds; // used for the UseResponse to trigger special construction handling
	
	public static void setupCaches() {
		cacheConstructables();
	}
	
	private static void cacheConstructables() {
		final String query = "select resulting_scenery_id, level, exp, tool_id, plank_id, plank_amount, bar_id, bar_amount, tertiary_id, tertiary_amount, lifetime_ticks, flatpack_item_id from constructable";
		DbConnection.load(query, rs -> {
			constructables.add(new ConstructableDto(
					rs.getInt("resulting_scenery_id"),
					rs.getInt("level"),
					rs.getInt("exp"),
					rs.getInt("tool_id"),
					rs.getInt("plank_id"),
					rs.getInt("plank_amount"),
					rs.getInt("bar_id"),
					rs.getInt("bar_amount"),
					rs.getInt("tertiary_id"),
					rs.getInt("tertiary_amount"),
					rs.getInt("lifetime_ticks"),
					rs.getInt("flatpack_item_id")));
		});
		
		constructionToolIds = constructables.stream().map(ConstructableDto::getToolId).distinct().collect(Collectors.toSet());
	}
	
	public static boolean itemIsConstructionTool(int itemId) {
		return constructionToolIds.contains(itemId);
	}
	
	public static Set<ConstructableDto> getAllConstructablesWithMaterials(int toolId, int materialId) {
		return constructables.stream()
			.filter(e -> {
				return e.getToolId() == toolId && (
					e.getPlankId() == materialId ||
					e.getBarId() == materialId ||
					e.getTertiaryId() == materialId
				);
			}).collect(Collectors.toSet());
	}
	
	public static Set<ConstructableDto> getAllConstructablesWithMaterials(int materialId) {
		return constructables.stream()
			.filter(e -> {
				return (
					e.getPlankId() == materialId ||
					e.getBarId() == materialId ||
					e.getTertiaryId() == materialId
				);
			}).collect(Collectors.toSet());
	}
	
	public static ConstructableDto getConstructableBySceneryId(int sceneryId) {
		return constructables.stream().filter(e -> e.getResultingSceneryId() == sceneryId).findFirst().orElse(null);
	}
	
	public static ConstructableDto getConstructableByFlatpackItemId(int itemId) {
		return constructables.stream().filter(e -> e.getFlatpackItemId() == itemId).findFirst().orElse(null);
	}
	
	public static Set<ConstructableDto> getAllConstructables() {
		return new HashSet<>(constructables);
	}
}
