package main.database;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class SpriteMapDao {
	private SpriteMapDao() {}
	
	public static SpriteMapDto getSpriteMapDataById(int id) {
		final String query = "select id, name, to_base64(data) data from sprite_maps where id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query); 
		) {
			ps.setInt(1,  id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new SpriteMapDto(rs.getInt("id"), rs.getString("name"), rs.getString("data"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static List<SpriteMapDto> getAllSpriteMaps() {
		final String query = "select id, name, to_base64(data) data from sprite_maps";
		
		List<SpriteMapDto> spriteMaps = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
		) {
			while (rs.next())
				spriteMaps.add(new SpriteMapDto(rs.getInt("id"), rs.getString("name"), rs.getString("data").replace("\n", "")));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return spriteMaps;
	}
	
	public static InputStream getSpriteMapImageBinaryStreamById(int id) {
		final String query = "select data from sprite_maps where id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					Blob blob = rs.getBlob("data");
					return blob.getBinaryStream();
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
}
