package database.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import database.DbConnection;
import database.dto.SmeltableDto;
import types.Items;

public class SmeltableDao {
	private static List<SmeltableDto> smeltables = new ArrayList<>(); // by ore_id
	private static Set<Integer> barIds = new HashSet<>();
	
	public static void setupCaches() {
		cacheSmeltables();
	}
	
	private static void cacheSmeltables() {
		final String query = "select bar_id, level, ore_id, coal_count, ore_count, exp from smeltable";
		DbConnection.load(query, rs -> {
			barIds.add(rs.getInt("bar_id"));
			smeltables.add(new SmeltableDto(
					rs.getInt("bar_id"),
					rs.getInt("level"),
					rs.getInt("ore_id"),
					rs.getInt("coal_count"),
					rs.getInt("ore_count"),
					rs.getInt("exp")
			));
		});
	}
	
	public static SmeltableDto getSmeltableByOreId(int oreId, boolean playerIsHoldingCoal) {
		if (oreId == Items.IRON_ORE.getValue()) { // iron ore
			return smeltables.stream().filter(e -> {
				return e.getOreId() == oreId && (playerIsHoldingCoal ? (e.getRequiredCoal() > 0) : (e.getRequiredCoal() == 0));
			}).findFirst().orElse(null);
		}
		return smeltables.stream().filter(e -> e.getOreId() == oreId).findFirst().orElse(null);
	}
	
	public static SmeltableDto getSmeltableByBarId(int barId) {
		return smeltables.stream().filter(e -> e.getBarId() == barId).findFirst().orElse(null);
	}
	
	public static boolean itemIsBar(int itemId) {
		return barIds.contains(itemId);
	}
	
	public static Set<SmeltableDto> getAllSmeltables() {
		return new HashSet<>(smeltables);
	}
}
