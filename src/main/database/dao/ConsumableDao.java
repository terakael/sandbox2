package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import main.database.DbConnection;
import main.database.dto.ConsumableEffectsDto;

public class ConsumableDao {
	private static HashMap<Integer, Integer> consumables;
	private static HashMap<Integer, ArrayList<ConsumableEffectsDto>> consumableEffects;
	
	public static void cacheConsumables() {
		final String query = "select item_id, becomes_id from consumable";
		
		consumables = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					consumables.put(rs.getInt("item_id"), rs.getInt("becomes_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void cacheConsumableEffects() {
		final String query = "select item_id, stat_id, amount, pct from consumable_effects";
		
		consumableEffects = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int itemId = rs.getInt("item_id");
					if (!consumableEffects.containsKey(itemId))
						consumableEffects.put(itemId, new ArrayList<>());
					consumableEffects.get(itemId).add(new ConsumableEffectsDto(itemId, rs.getInt("stat_id"), rs.getInt("amount"), rs.getInt("pct")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean isConsumable(int itemId) {
		return consumables.containsKey(itemId);
	}
	
	public static int getBecomesItemId(int itemId) {
		if (consumables.containsKey(itemId))
			return consumables.get(itemId);
		return 0;
	}
	
	public static ArrayList<ConsumableEffectsDto> getConsumableEffects(int itemId) {
		if (consumableEffects.containsKey(itemId))
			return consumableEffects.get(itemId);
		return new ArrayList<>();
	}
}
