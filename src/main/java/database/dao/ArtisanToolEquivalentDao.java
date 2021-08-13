package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import database.DbConnection;
import lombok.Getter;

public class ArtisanToolEquivalentDao {
	// tool equivalents is a many-to-many relationship, so we store it by originalItemId as we look up the artisan equivalent by the original.
	// many-to-many examples:
	// fishing rod -> [enhanced fishing rod, tacklebox, tinderpole]
	// [tinderbox, fishing rod] -> tinderpole
	@Getter private static Map<Integer, Set<Integer>> toolEquivalents = new HashMap<>(); // originalItemId, [artisanItemIds]
	
	public static void setupCaches() {
		DbConnection.load("select * from artisan_tool_equivalents", rs -> {
			toolEquivalents.putIfAbsent(rs.getInt("original_tool_id"), new HashSet<>());
			toolEquivalents.get(rs.getInt("original_tool_id")).add(rs.getInt("artisan_tool_id"));
		});
	}
	
	public static Set<Integer> getArtisanEquivalents(int originalItemId) {
		if (!toolEquivalents.containsKey(originalItemId))
			return new HashSet<>();
		return toolEquivalents.get(originalItemId);
	}
}
