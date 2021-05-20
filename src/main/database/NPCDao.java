package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import main.types.NpcAttributes;

public class NPCDao {
	@Getter private static Map<Integer, List<NPCDto>> npcInstanceList = null; // floor, npcList
	@Getter private static List<NPCDto> npcList = null;
	private static Map<Integer, List<NpcDropDto>> npcDrops = null;
	
	
	public static void setupCaches() {
		npcList = getNpcs();
		npcInstanceList = new HashMap<>();
		for (int floor : GroundTextureDao.getDistinctFloors())
			npcInstanceList.put(floor, getAllNpcsByFloor(floor));
		cacheNpcDrops();
	}
	
	private static List<NPCDto> getNpcs() {
		final String query = "select * from npcs";
		
		List<NPCDto> npcList = new ArrayList<>();
		
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
						0,// tileId (not used in this map as this is not the instance list, just all npc types)
						rs.getInt("hp"),
						StatsDao.getCombatLevelByStats(rs.getInt("str"), rs.getInt("acc"), rs.getInt("def"), rs.getInt("pray"), rs.getInt("hp"), 0),
						rs.getInt("leftclick_option"),
						rs.getInt("other_options"),
						0,// floor (not used in this map as this is not the instance list, just all npc types)
						rs.getInt("acc"),
						rs.getInt("str"),
						rs.getInt("def"),
						rs.getInt("pray"),
						rs.getInt("magic"),
						rs.getInt("acc_bonus"),
						rs.getInt("str_bonus"),
						rs.getInt("def_bonus"),
						rs.getInt("pray_bonus"),
						rs.getInt("attack_speed"),
						rs.getInt("roam_radius"),
						rs.getInt("attributes"),
						rs.getInt("respawn_ticks")
					));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return npcList;
		
	}
	
	public static List<NPCDto> getAllNpcsByFloor(int floor) {
		final String query = 
			"select id, name, up_id, down_id, left_id, right_id, attack_id, scale_x, scale_y, acc, str, def, pray, hp, magic, acc_bonus, str_bonus, def_bonus, pray_bonus, attack_speed, tile_id, leftclick_option, other_options, roam_radius, attributes, respawn_ticks from npcs" + 
			" inner join room_npcs on room_npcs.npc_id = id" + 
			" where floor=?";
		
		List<NPCDto> npcList = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, floor);
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
						StatsDao.getCombatLevelByStats(rs.getInt("str"), rs.getInt("acc"), rs.getInt("def"), rs.getInt("pray"), rs.getInt("hp"), 0),
						rs.getInt("leftclick_option"),
						rs.getInt("other_options"),
						floor,
						rs.getInt("acc"),
						rs.getInt("str"),
						rs.getInt("def"),
						rs.getInt("pray"),
						rs.getInt("magic"),
						rs.getInt("acc_bonus"),
						rs.getInt("str_bonus"),
						rs.getInt("def_bonus"),
						rs.getInt("pray_bonus"),
						rs.getInt("attack_speed"),
						rs.getInt("roam_radius"),
						rs.getInt("attributes"),
						rs.getInt("respawn_ticks")
					));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return npcList;
	}
	
	public static Map<Integer, String> getExamineMap() {
		final String query = "select id, description from npcs";
		Map<Integer, String> examineMap = new HashMap<>();
		
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
	
	public static int getNpcIdFromInstanceId(int floor, int instanceId) {
		if (!npcInstanceList.containsKey(floor))
			return -1;
		
		for (NPCDto dto : npcInstanceList.get(floor)) {
			if (dto.getTileId() == instanceId)
				return dto.getId();
		}
		return -1;
	}
	
	public static void cacheNpcDrops() {
		final String query = "select npc_id, item_id, count, rate from npc_drops";
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
					npcDrops.get(npcId).add(new NpcDropDto(rs.getInt("npc_id"), rs.getInt("item_id"), rs.getInt("count"), rs.getInt("rate")));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static List<NpcDropDto> getDropsByNpcId(int npcId) {
		if (npcDrops.containsKey(npcId))
			return npcDrops.get(npcId);
		return new ArrayList<>();
	}
	
	public static String getNpcNameById(int npcId) {
		for (NPCDto dto : npcList) {
			if (dto.getId() == npcId)
				return dto.getName();
		}
		return null;
	}
	
	public static boolean npcHasAttribute(int npcId, NpcAttributes attribute) {
		for (NPCDto npc : npcList) {
			if (npc.getId() == npcId) {
				return (npc.getAttributes() & attribute.getValue()) == attribute.getValue();
			}
		}
		return false;
	}
}
