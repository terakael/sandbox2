package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SpriteFrameDao {
	private SpriteFrameDao() {};
	
	public static SpriteFrameDto getSpriteFrameById(int id) {
		final String query = "select id, sprite_map_id, x, y, w, h, anchor_x, anchor_y, margin, frame_count, animation_type_id from sprite_frames where id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new SpriteFrameDto(
						rs.getInt("id"),
						rs.getInt("sprite_map_id"),
						rs.getInt("x"),
						rs.getInt("y"),
						rs.getInt("w"),
						rs.getInt("h"),
						rs.getFloat("anchor_x"),
						rs.getFloat("anchor_y"),
						rs.getInt("margin"),
						rs.getInt("frame_count"),
						rs.getInt("animation_type_id")
					);
			} 
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static List<SpriteFrameDto> getAllSpriteFrames() {
		final String query = "select id, sprite_map_id, x, y, w, h, anchor_x, anchor_y, margin, frame_count, animation_type_id from sprite_frames";
		List<SpriteFrameDto> spriteFrames = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
		) {
			while (rs.next()) {
				spriteFrames.add(new SpriteFrameDto(
						rs.getInt("id"),
						rs.getInt("sprite_map_id"),
						rs.getInt("x"),
						rs.getInt("y"),
						rs.getInt("w"),
						rs.getInt("h"),
						rs.getFloat("anchor_x"),
						rs.getFloat("anchor_y"),
						rs.getInt("margin"),
						rs.getInt("frame_count"),
						rs.getInt("animation_type_id")
					));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return spriteFrames;
	}
}
