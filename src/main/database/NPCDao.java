package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import lombok.Getter;

public class NPCDao {
	@Getter private static ArrayList<NPCDto> npcList = null;
	
	public static void setupCaches() {
		npcList = getAllNpcsByRoom(1);// TODO multiple rooms
	}
	
	public static ArrayList<NPCDto> getAllNpcsByRoom(int roomId) {
		final String query = 
			"select id, name, up_id, down_id, left_id, right_id, attack_id, acc, str, def, agil, hp, magic, acc_bonus, str_bonus, def_bonus, agil_bonus, tile_id, leftclick_option, other_options from npcs" + 
			" inner join room_npcs on room_npcs.npc_id = id" + 
			" where room_id=?";
		
		ArrayList<NPCDto> npcList = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, roomId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					npcList.add(new NPCDto(
						rs.getInt("id"),
						rs.getString("name"),
						rs.getInt("up_id"),
						rs.getInt("down_id"),
						rs.getInt("left_id"),
						rs.getInt("right_id"),
						rs.getInt("attack_id"),
						rs.getInt("tile_id"),
						rs.getInt("hp"),
						StatsDao.getCombatLevelByStats(rs.getInt("str"), rs.getInt("acc"), rs.getInt("def"), rs.getInt("agil"), rs.getInt("hp"), 0),
						rs.getInt("leftclick_option"),
						rs.getInt("other_options"),
						rs.getInt("acc"),
						rs.getInt("str"),
						rs.getInt("def"),
						rs.getInt("agil"),
						rs.getInt("magic"),
						rs.getInt("acc_bonus"),
						rs.getInt("str_bonus"),
						rs.getInt("def_bonus"),
						rs.getInt("agil_bonus")
					));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return npcList;
	}
	
	public static HashMap<Integer, String> getExamineMap() {
		final String query = "select id, description from npcs";
		HashMap<Integer, String> examineMap = new HashMap<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					examineMap.put(rs.getInt("id"), rs.getString("description"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return examineMap;
	}
	
	public static int getNpcIdFromInstanceId(int instanceId) {
		for (NPCDto dto : npcList) {
			if (dto.getTileId() == instanceId)
				return dto.getId();
		}
		return -1;
	}
}
