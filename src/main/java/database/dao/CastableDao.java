package database.dao;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import database.DbConnection;
import database.dto.CastableDto;

public class CastableDao {
	private static HashMap<Integer, CastableDto> castables = new HashMap<>(); // itemId, dto
	
	public static void setupCaches() {
		cacheCastables();
	}
	
	private static void cacheCastables() {
		DbConnection.load("select item_id, level, exp, max_hit, sprite_frame_id from castable", 
				rs -> castables.put(rs.getInt("item_id"), new CastableDto(rs.getInt("item_id"), rs.getInt("level"), rs.getInt("exp"), rs.getInt("max_hit"), rs.getInt("sprite_frame_id"))));
	}

	
	public static boolean isCastable(int itemId) {
		return castables.containsKey(itemId);
	}
	
	public static CastableDto getCastableByItemId(int itemId) {
		return castables.get(itemId);
	}
	
	public static Set<CastableDto> getAllCastables() {
		return new HashSet<>(castables.values());
	}
}
