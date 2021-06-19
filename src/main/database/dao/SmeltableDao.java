package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import main.database.DbConnection;
import main.database.dto.SmeltableDto;

public class SmeltableDao {
	private static List<SmeltableDto> smeltables; // by ore_id
	
	public static void setupCaches() {
		cacheSmeltables();
	}
	
	private static void cacheSmeltables() {
		smeltables = new ArrayList<>();
		
		final String query = "select bar_id, level, ore_id, coal_count from smeltable";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					smeltables.add(new SmeltableDto(
							rs.getInt("bar_id"),
							rs.getInt("level"),
							rs.getInt("ore_id"),
							rs.getInt("coal_count")
					));
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
}
