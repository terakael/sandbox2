package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import main.database.DbConnection;
import main.database.dto.SmeltableDto;

public class SmeltableDao {
	private static List<SmeltableDto> smeltables; // by ore_id
	private static Set<Integer> barIds;
	
	public static void setupCaches() {
		cacheSmeltables();
	}
	
	private static void cacheSmeltables() {
		smeltables = new ArrayList<>();
		barIds = new HashSet<>();
		
		final String query = "select bar_id, level, ore_id, coal_count, ore_count, exp from smeltable";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					barIds.add(rs.getInt("bar_id"));
					smeltables.add(new SmeltableDto(
							rs.getInt("bar_id"),
							rs.getInt("level"),
							rs.getInt("ore_id"),
							rs.getInt("coal_count"),
							rs.getInt("ore_count"),
							rs.getInt("exp")
					));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static SmeltableDto getSmeltableByOreId(int oreId, boolean playerIsHoldingCoal) {
		if (oreId == 4) { // iron ore
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
}
