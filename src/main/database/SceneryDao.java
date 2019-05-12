package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SceneryDao {
	private SceneryDao() {};
	
	public static SceneryDto getSceneryById(int id) {
		final String query = "select id, name, sprite_map_id, x, y, w, h, anchor_x, anchor_y, framecount, framerate, attributes from scenery where id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new SceneryDto(
						rs.getString("name"),
						rs.getInt("sprite_map_id"),
						rs.getInt("x"),
						rs.getInt("y"),
						rs.getInt("w"),
						rs.getInt("h"),
						rs.getFloat("anchor_x"),
						rs.getFloat("anchor_y"),
						rs.getInt("framecount"),
						rs.getInt("framerate"),
						rs.getInt("attributes")
					);
			} 
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static List<SceneryDto> getAllSceneryByRoom(int roomId) {
		// TODO query is wrong
		final String query = 
				"select name, sprite_map_id, x, y, w, h, anchor_x, anchor_y, framecount, framerate, attributes from scenery " +
				" where id in (select distinct scenery_id from room_scenery where room_id=?)";
		
		List<SceneryDto> sceneryList = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, roomId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					sceneryList.add(new SceneryDto(
							rs.getString("name"),
							rs.getInt("sprite_map_id"),
							rs.getInt("x"),
							rs.getInt("y"),
							rs.getInt("w"),
							rs.getInt("h"),
							rs.getFloat("anchor_x"),
							rs.getFloat("anchor_y"),
							rs.getInt("framecount"),
							rs.getInt("framerate"),
							rs.getInt("attributes")
						));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return sceneryList;
	}
}
