package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

public class PrayerDao {
	@Getter private static Map<Integer, PrayerDto> prayers = null;
	@Getter private static Map<Integer, List<Integer>> prayerReplacements = null;
	
	public static void setupCaches() {
		cachePrayers();
		cachePrayerReplacements();
	}
	
	private static void cachePrayers() {
		final String query = "select id, name, description, icon_id, level, drain_rate from prayers";
		
		prayers = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					prayers.put(rs.getInt("id"), new PrayerDto(rs.getInt("id"), rs.getString("name"), rs.getString("description"), rs.getInt("icon_id"), rs.getInt("level"), rs.getFloat("drain_rate")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void cachePrayerReplacements() {
		final String query = "select prayer_id, replacement_prayer_id from prayer_replacements";
		
		prayerReplacements = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (!prayerReplacements.containsKey(rs.getInt("prayer_id")))
						prayerReplacements.put(rs.getInt("prayer_id"), new ArrayList<>());
					prayerReplacements.get(rs.getInt("prayer_id")).add(rs.getInt("replacement_prayer_id"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static List<PrayerDto> getReplacementPrayersByPrayerLevel(int prayerLevel) {
		List<PrayerDto> replacementPrayers = new ArrayList<>();
		for (Map.Entry<Integer, List<Integer>> entry : prayerReplacements.entrySet()) {
			int highestPrayerId = entry.getKey();
			for (Integer val : entry.getValue()) {
				if (prayerLevel < prayers.get(val).getLevel())
					break;
				highestPrayerId = val;
			}
			replacementPrayers.add(prayers.get(highestPrayerId));
		}
		return replacementPrayers;
	}
	
	public static PrayerDto getPrayerById(int prayerId) {
		if (!prayers.containsKey(prayerId))
			return null;
		return prayers.get(prayerId);
	}
}
