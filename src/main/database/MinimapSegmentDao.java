package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MinimapSegmentDao {
	private static final int segmentsPerRow = 1000;
	private static Map<Integer, Map<Integer, String>> minimapSegments; // floor, <segment, base64>
	
	private static Map<Integer, Map<Integer, Map<Integer, Set<Integer>>>> tileIdsByFloorAndSegmentAndSpriteFrameId; // floor, <segment, <iconId, <tileIds>>>
	
	public static void setupCaches() {
		cacheMinimapSegments();
		cacheMinimapIconLocations();
	}
	
	private static void cacheMinimapSegments() {
		minimapSegments = new HashMap<>();
		
		final String query = "select floor, segment, to_base64(data) data from minimap_segments";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					if (!minimapSegments.containsKey(rs.getInt("floor")))
						minimapSegments.put(rs.getInt("floor"), new HashMap<>());
					minimapSegments.get(rs.getInt("floor")).put(rs.getInt("segment"), rs.getString("data"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private static void cacheMinimapIconLocations() {
		tileIdsByFloorAndSegmentAndSpriteFrameId = new HashMap<>();
		Map<Integer, Integer> minimapIcons = loadMinimapIcons();
		
		for (int floor : GroundTextureDao.getDistinctFloors()) {
			tileIdsByFloorAndSegmentAndSpriteFrameId.put(floor, new HashMap<>());
			
			for (Map.Entry<Integer, Integer> entry : minimapIcons.entrySet()) {
				Set<Integer> tileIds = SceneryDao.getInstanceListByFloorAndSceneryId(floor, entry.getKey());
				for (int tileId : tileIds) {
					int segmentId = getSegmentIdFromTileId(tileId);
					if (!tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).containsKey(segmentId))
						tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).put(segmentId, new HashMap<>());
					
					if (!tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).get(segmentId).containsKey(entry.getValue()))
						tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).get(segmentId).put(entry.getValue(), new HashSet<>());
					
					tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).get(segmentId).get(entry.getValue()).add(tileId);
				}
			}
		}
	}
	
	private static Map<Integer, Integer> loadMinimapIcons() {
		Map<Integer, Integer> sceneryIdIconId = new HashMap<>();
		final String query = "select scenery_id, sprite_frame_id from minimap_icons";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					sceneryIdIconId.put(rs.getInt("scenery_id"), rs.getInt("sprite_frame_id"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return sceneryIdIconId;
	}
	
	public static String getMinimapSegmentDataByTileId(int floor, int tileId) {
		return getMinimapDataByFloorAndSegmentId(floor, getSegmentIdFromTileId(tileId));
	}
	
	public static String getMinimapDataByFloorAndSegmentId(int floor, int segmentId) {
		if (!minimapSegments.containsKey(floor))
			return null;
		return minimapSegments.get(floor).get(segmentId);
	}
	
	public static int getSegmentIdFromTileId(int tileId) {
		int tileX = tileId % 25000;
		int tileY = tileId / 25000;
		int segmentX = tileX / 25;
		int segmentY = tileY / 25;
		return (segmentY * segmentsPerRow) + segmentX;
	}
	
	public static Set<Integer> getSegmentIdsFromTileId(int tileId) {
		Set<Integer> segmentIds = new HashSet<>();
		
		// we take a 3x3 set of segments, where the tileId represents the central segment
		int centralSegmentId = getSegmentIdFromTileId(tileId);
		segmentIds.add(centralSegmentId);
		
		// we know that the segments are 1000x1000
		if (centralSegmentId % segmentsPerRow > 0)
			segmentIds.add(centralSegmentId - 1);// left
		
		if (centralSegmentId % segmentsPerRow < segmentsPerRow - 1)
			segmentIds.add(centralSegmentId + 1);// right
		
		if (centralSegmentId / segmentsPerRow > 0)
			segmentIds.add(centralSegmentId - segmentsPerRow);// above
		
		if (centralSegmentId / segmentsPerRow < segmentsPerRow - 1)
			segmentIds.add(centralSegmentId + segmentsPerRow);// below
		
		if (centralSegmentId % segmentsPerRow > 0 && centralSegmentId / segmentsPerRow > 0)
			segmentIds.add(centralSegmentId - segmentsPerRow - 1);// top left
		
		if (centralSegmentId % segmentsPerRow < segmentsPerRow - 1 && centralSegmentId / segmentsPerRow > 0)
			segmentIds.add(centralSegmentId - segmentsPerRow + 1);// top right
		
		if (centralSegmentId % segmentsPerRow > 0 && centralSegmentId / segmentsPerRow < segmentsPerRow - 1)
			segmentIds.add(centralSegmentId + segmentsPerRow - 1);// bottom left
		
		if (centralSegmentId % segmentsPerRow < segmentsPerRow - 1 && centralSegmentId / segmentsPerRow < segmentsPerRow - 1)
			segmentIds.add(centralSegmentId + segmentsPerRow + 1);// bottom right
		
		return segmentIds;
	}
	
	public static Map<Integer, Set<Integer>> getMinimapIconLocationsByFloorAndSegment(int floor, int segmentId) {
		if (!tileIdsByFloorAndSegmentAndSpriteFrameId.containsKey(floor))
			return new HashMap<>();
		
		if (!tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).containsKey(segmentId))
			return new HashMap<>();
		
		return tileIdsByFloorAndSegmentAndSpriteFrameId.get(floor).get(segmentId);
	}
}
