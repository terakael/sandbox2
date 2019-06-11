package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import lombok.Getter;

public class NPCDao {
	@Getter private static ArrayList<NPCDto> npcInstanceList = null;
	@Getter private static ArrayList<NPCDto> npcList = null;
	private static HashMap<Integer, ArrayList<Integer>> npcDrops = null;
	
	public static void setupCaches() {
		npcList = getNpcs();
		npcInstanceList = getAllNpcsByRoom(1);// TODO multiple rooms
		cacheNpcDrops();
	}
	
	private static ArrayList<NPCDto> getNpcs() {
		final String query = "select * from npcs";
		
		ArrayList<NPCDto> npcList = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
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
						rs.getFloat("scale_x"),
						rs.getFloat("scale_y"),
						0,// tileId
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
						rs.getInt("agil_bonus"),
						rs.getInt("attack_speed"),
						rs.getInt("roam_radius")
					));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return npcList;
		
	}
	
	public static ArrayList<NPCDto> getAllNpcsByRoom(int roomId) {
		final String query = 
			"select id, name, up_id, down_id, left_id, right_id, attack_id, scale_x, scale_y, acc, str, def, agil, hp, magic, acc_bonus, str_bonus, def_bonus, agil_bonus, attack_speed, tile_id, leftclick_option, other_options, roam_radius from npcs" + 
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
						rs.getFloat("scale_x"),
						rs.getFloat("scale_y"),
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
						rs.getInt("agil_bonus"),
						rs.getInt("attack_speed"),
						rs.getInt("roam_radius")
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
		for (NPCDto dto : npcInstanceList) {
			if (dto.getTileId() == instanceId)
				return dto.getId();
		}
		return -1;
	}
	
	public static HashSet<Integer> getNpcInstanceIds() {
		final String query = "select tile_id from room_npcs";
		
		HashSet<Integer> set = new HashSet<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					set.add(rs.getInt("tile_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return set;
	}
	
	public static void addNpcInstance(int roomId, int npcId, int instanceId) {
		final String query = "insert into room_npcs values (?,?,?)";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, roomId);
			ps.setInt(2, npcId);
			ps.setInt(3, instanceId);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void cacheNpcDrops() {
		final String query = "select npc_id, item_id from npc_drops";
		npcDrops = new HashMap<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					int npcId = rs.getInt("npc_id");
					if (!npcDrops.containsKey(npcId))
						npcDrops.put(npcId, new ArrayList<>());
					npcDrops.get(npcId).add(rs.getInt("item_id"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<Integer> getDropsByNpcId(int npcId) {
		if (npcDrops.containsKey(npcId))
			return npcDrops.get(npcId);
		return null;
	}
}
