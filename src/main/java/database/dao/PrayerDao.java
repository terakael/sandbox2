package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import database.DbConnection;
import database.dto.PrayerDto;

public class PrayerDao {
	@Getter private static Map<Integer, PrayerDto> prayers = new HashMap<>();
	@Getter private static Map<Integer, List<Integer>> prayerReplacements = new HashMap<>();
	
	public static void setupCaches() {
		cachePrayers();
		cachePrayerReplacements();
	}
	
	private static void cachePrayers() {
		final String query = "select id, name, description, icon_id, level, drain_rate from prayers";
		DbConnection.load(query, rs -> {
			prayers.put(rs.getInt("id"), new PrayerDto(rs.getInt("id"), rs.getString("name"), rs.getString("description"), rs.getInt("icon_id"), rs.getInt("level"), rs.getFloat("drain_rate")));
		});
	}
	
	private static void cachePrayerReplacements() {
		final String query = "select prayer_id, replacement_prayer_id from prayer_replacements";
		DbConnection.load(query, rs -> {
			prayerReplacements.putIfAbsent(rs.getInt("prayer_id"), new ArrayList<>());
			prayerReplacements.get(rs.getInt("prayer_id")).add(rs.getInt("replacement_prayer_id"));
		});
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
		return prayers.get(prayerId);
	}
}
