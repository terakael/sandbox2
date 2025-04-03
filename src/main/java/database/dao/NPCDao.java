package database.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import database.DbConnection;
import database.dto.NPCDto;
import database.dto.NpcDropDto;
import database.entity.delete.DeleteRoomNpcEntity;
import database.entity.insert.InsertRoomNpcEntity;
import processing.PathFinder;
import processing.attackable.NPC;
import processing.managers.DatabaseUpdater;
import processing.managers.LocationManager;
import types.NpcAttributes;

public class NPCDao {
	private static Map<Integer, NPCDto> allNpcs = new HashMap<>();
	private static Map<Integer, Map<Integer, Set<Integer>>> instances = new HashMap<>(); // floor, <npcId, <tileIds>>
	private static Map<Integer, List<NpcDropDto>> npcDrops = new HashMap<>();

	public static void setupCaches() {
		cacheNpcs();
		cacheNpcDrops();
		cacheInstances();
		populateLocationManager();
	}

	private static void cacheNpcs() {
		DbConnection.load("select * from npcs", rs -> {
			allNpcs.put(rs.getInt("id"), new NPCDto(
					rs.getInt("id"),
					rs.getString("name"),
					rs.getInt("up_id"),
					rs.getInt("down_id"),
					rs.getInt("left_id"),
					rs.getInt("right_id"),
					rs.getInt("attack_id"),
					rs.getFloat("scale_x"),
					rs.getFloat("scale_y"),
					rs.getInt("hp"),
					StatsDao.getCombatLevelByStats(rs.getInt("str"), rs.getInt("acc"), rs.getInt("def"),
							rs.getInt("pray"), rs.getInt("hp"), rs.getInt("magic")),
					rs.getInt("leftclick_option"),
					rs.getInt("other_options"),
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
					rs.getInt("respawn_ticks")));
		});
	}

	private static void cacheInstances() {
		DbConnection.load("select floor, tile_id, npc_id from room_npcs", rs -> {
			if (!GroundTextureDao.hasCustomTile(rs.getInt("floor"), rs.getInt("tile_id"))) {
				instances.putIfAbsent(rs.getInt("floor"), new HashMap<>());
				instances.get(rs.getInt("floor")).putIfAbsent(rs.getInt("npc_id"), new HashSet<>());

				instances.get(rs.getInt("floor")).get(rs.getInt("npc_id"))
						.add(PathFinder.getClosestWalkableTile(rs.getInt("floor"), rs.getInt("tile_id"), false));
			}
		});

		String customNpcQuery = "SELECT " +
				" ca.floor + crn.offset_floor AS floor, " +
				" ca.tile_id + (? * crn.offset_y) + crn.offset_x AS tile_id, " +
				" npc_id " +
				" FROM custom_room_npcs crn " +
				" JOIN custom_area ca ON crn.custom_area_id = ca.id";

		DbConnection.load(customNpcQuery, rs -> {
			instances.putIfAbsent(rs.getInt("floor"), new HashMap<>());
			instances.get(rs.getInt("floor")).putIfAbsent(rs.getInt("npc_id"), new HashSet<>());

			instances.get(rs.getInt("floor")).get(rs.getInt("npc_id"))
					.add(PathFinder.getClosestWalkableTile(rs.getInt("floor"), rs.getInt("tile_id"), false));
		}, PathFinder.LENGTH);
	}

	private static void populateLocationManager() {
		instances.forEach((floor, instanceMap) -> {
			List<NPC> npcs = new ArrayList<>();
			instanceMap.forEach((npcId, tileIds) -> tileIds
					.forEach(tileId -> npcs.add(new NPC(allNpcs.get(npcId), floor, tileId))));
			LocationManager.addNpcs(npcs);
		});
	}

	public static Map<Integer, String> getExamineMap() {
		final String query = "select id, description from npcs";
		Map<Integer, String> examineMap = new HashMap<>();
		DbConnection.load(query, rs -> examineMap.put(rs.getInt("id"), rs.getString("description")));
		return examineMap;
	}

	public static Set<NPCDto> getNpcList() {
		return new LinkedHashSet<>(allNpcs.values());
	}

	public static int getNpcIdFromInstanceId(int floor, int instanceId) {
		if (!instances.containsKey(floor))
			return -1;

		return instances.get(floor).entrySet().stream()
				.filter(entry -> entry.getValue().contains(instanceId))
				.map(Entry::getKey)
				.findFirst()
				.orElse(-1);
	}

	public static void cacheNpcDrops() {
		final String query = "select npc_id, item_id, count, rate from npc_drops";
		DbConnection.load(query, rs -> {
			int npcId = rs.getInt("npc_id");
			if (!npcDrops.containsKey(npcId))
				npcDrops.put(npcId, new ArrayList<>());
			npcDrops.get(npcId).add(
					new NpcDropDto(rs.getInt("npc_id"), rs.getInt("item_id"), rs.getInt("count"), rs.getInt("rate")));
		});
	}

	public static List<NpcDropDto> getDropsByNpcId(int npcId) {
		if (npcDrops.containsKey(npcId))
			return npcDrops.get(npcId);
		return new ArrayList<>();
	}

	public static String getNpcNameById(int npcId) {
		NPCDto dto = getNpcById(npcId);
		if (dto == null)
			return null;
		return dto.getName();
	}

	public static NPCDto getNpcById(int npcId) {
		return allNpcs.get(npcId);
	}

	public static boolean npcHasAttribute(int npcId, NpcAttributes attribute) {
		final NPCDto dto = getNpcById(npcId);
		if (dto == null)
			return false;
		return (dto.getAttributes() & attribute.getValue()) == attribute.getValue();
	}

	public static void upsertRoomNpc(int floor, int tileId, int npcId) {
		final NPCDto dto = getNpcById(npcId);
		if (dto == null)
			return;

		deleteRoomNpc(floor, tileId);

		instances.putIfAbsent(floor, new HashMap<>());
		instances.get(floor).putIfAbsent(npcId, new HashSet<>());
		instances.get(floor).get(npcId).add(tileId);

		LocationManager.addNpcs(Arrays.asList(new NPC(dto, floor, tileId)));
		DatabaseUpdater.enqueue(new InsertRoomNpcEntity(floor, tileId, npcId));
	}

	public static boolean deleteRoomNpc(int floor, int tileId) {
		// quite the mission to delete the npc from the LocationManager
		// because it's not designed to grab an NPC by instance id.
		final Set<NPC> localNpcs = new HashSet<>();
		localNpcs.addAll(LocationManager.getLocalNpcs(floor, tileId, 12, true)); // diurnal
		localNpcs.addAll(LocationManager.getLocalNpcs(floor, tileId, 12, false));// nocturnal
		final NPC npcToRemove = localNpcs.stream()
				.filter(e -> e.getInstanceId() == tileId)
				.findFirst()
				.orElse(null);
		if (npcToRemove != null)
			LocationManager.removeNpc(npcToRemove);

		if (!instances.containsKey(floor))
			return false;

		final Set<Integer> containedNpcs = instances.get(floor).values().stream()
				.filter(e -> e.contains(tileId))
				.findFirst()
				.orElse(null);

		if (containedNpcs != null) {
			if (containedNpcs.remove(tileId)) {
				DatabaseUpdater.enqueue(new DeleteRoomNpcEntity(floor, tileId, null));
				return true;
			}
		}
		return false;
	}
}
