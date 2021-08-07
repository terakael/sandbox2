package database.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import database.DbConnection;
import database.dto.NpcMessageDto;

public class NpcMessageDao {
	private static Map<Integer, List<NpcMessageDto>> npcMessages = new HashMap<>(); // npcId, <messages>
	
	public static void setupCaches() {
		loadNpcMessages();
	}
	
	private static void loadNpcMessages() {
		DbConnection.load("select npc_id, message_id, message from npc_messages", rs -> {
			npcMessages.putIfAbsent(rs.getInt("npc_id"), new ArrayList<>());
			npcMessages.get(rs.getInt("npc_id")).add(new NpcMessageDto(rs.getInt("npc_id"), rs.getInt("message_id"), rs.getString("message")));
		});
	}
	
	public static List<String> getMessagesByNpcId(int npcId) {
		if (!npcMessages.containsKey(npcId))
			return new ArrayList<>();
		
		return npcMessages.get(npcId).stream()
				.map(NpcMessageDto::getMessage)
				.collect(Collectors.toList());
	}
}
