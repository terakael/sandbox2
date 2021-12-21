package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import database.DbConnection;
import database.dto.CustomBoundingBoxDto;
import database.dto.SpriteFrameDto;

public class SpriteFrameDao {
	private static Map<Integer, SpriteFrameDto> allSpriteFrames = new HashMap<>();
	
	private SpriteFrameDao() {};
	
	public static void setupCaches() {
		cacheAllSpriteFrames();
		cacheCustomBoundingBoxes();
	}
	
	public static List<SpriteFrameDto> getAllSpriteFrames() {
		return new ArrayList<>(allSpriteFrames.values());
	}
	
	private static void cacheAllSpriteFrames() {
		final String query = "select id, sprite_map_id, x, y, w, h, anchor_x, anchor_y, scale_x, scale_y, margin, frame_count, framerate, animation_type_id, color from sprite_frames";
		DbConnection.load(query, rs -> {
			allSpriteFrames.put(rs.getInt("id"), new SpriteFrameDto(
					rs.getInt("id"),
					rs.getInt("sprite_map_id"),
					rs.getInt("x"),
					rs.getInt("y"),
					rs.getInt("w"),
					rs.getInt("h"),
					rs.getFloat("anchor_x"),
					rs.getFloat("anchor_y"),
					rs.getFloat("scale_x"),
					rs.getFloat("scale_y"),
					rs.getInt("margin"),
					rs.getInt("frame_count"),
					rs.getInt("framerate"),
					rs.getInt("animation_type_id"),
					new HashMap<>(),
					rs.getInt("color")
				));
		});
	}
	
	private static void cacheCustomBoundingBoxes() {
		final String query = "select sprite_frame_id, frame_id, x_pct, y_pct, w_pct, h_pct from custom_bounding_boxes";
		DbConnection.load(query, rs -> {
			final int spriteFrameId = rs.getInt("sprite_frame_id");
			final float xPct = rs.getFloat("x_pct");
			final float yPct = rs.getFloat("y_pct");
			final float wPct = rs.getFloat("w_pct");
			final float hPct = rs.getFloat("h_pct");
			if (allSpriteFrames.containsKey(spriteFrameId)) {
				allSpriteFrames.get(spriteFrameId).getCustomBoundingBoxes().put(rs.getInt("frame_id"), new CustomBoundingBoxDto(xPct, yPct, wPct, hPct));
			}
		});
	}
	
	public static SpriteFrameDto getSpriteFrameDtoById(int id) {
		return allSpriteFrames.get(id);
	}
}
