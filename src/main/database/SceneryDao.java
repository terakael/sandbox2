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
				if (rs.next()) {
					SceneryDto dto = new SceneryDto();
					dto.setId(rs.getInt("id"));
					dto.setName(rs.getString("name"));
					dto.setSpriteMapId(rs.getInt("sprite_map_id"));
					dto.setX(rs.getInt("x"));
					dto.setY(rs.getInt("y"));
					dto.setW(rs.getInt("w"));
					dto.setH(rs.getInt("h"));
					dto.setAnchorX(rs.getFloat("anchor_x"));
					dto.setAnchorY(rs.getFloat("anchor_y"));
					dto.setFramecount(rs.getInt("framecount"));
					dto.setFramerate(rs.getInt("framerate"));
					dto.setAttributes(rs.getInt("attributes"));
					//dto.setInstances(getInstanceListBySceneryId(id));
					
					return dto;
				}
			} 
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static List<SceneryDto> getAllSceneryByRoom(int roomId) {
		// TODO query is wrong
		final String query = 
				"select id, name, sprite_map_id, x, y, w, h, anchor_x, anchor_y, framecount, framerate, attributes from scenery " +
				" where id in (select distinct scenery_id from room_scenery where room_id=?)";
		
		List<SceneryDto> sceneryList = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, roomId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					SceneryDto dto = new SceneryDto();
					dto.setId(rs.getInt("id"));
					dto.setName(rs.getString("name"));
					dto.setSpriteMapId(rs.getInt("sprite_map_id"));
					dto.setX(rs.getInt("x"));
					dto.setY(rs.getInt("y"));
					dto.setW(rs.getInt("w"));
					dto.setH(rs.getInt("h"));
					dto.setAnchorX(rs.getFloat("anchor_x"));
					dto.setAnchorY(rs.getFloat("anchor_y"));
					dto.setFramecount(rs.getInt("framecount"));
					dto.setFramerate(rs.getInt("framerate"));
					dto.setAttributes(rs.getInt("attributes"));
					dto.setInstances(getInstanceListByRoomIdAndSceneryId(roomId, rs.getInt("id")));
					sceneryList.add(dto);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return sceneryList;
	}
	
	public static ArrayList<Integer> getInstanceListByRoomIdAndSceneryId(int roomId, int sceneryId) {
		String query = "select tile_id from room_scenery where room_id=? and scenery_id=?";
		
		ArrayList<Integer> instanceList = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, roomId);
			ps.setInt(2, sceneryId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					instanceList.add(rs.getInt("tile_id"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return instanceList;
	}
}
