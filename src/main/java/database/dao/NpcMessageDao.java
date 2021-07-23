package database.dao;

import java.util.ArrayList;

import database.DbConnection;
import database.dto.NpcMessageDto;

public class NpcMessageDao {
	private static ArrayList<NpcMessageDto> npcMessages = new ArrayList<>();
	
	public static void setupCaches() {
		loadNpcMessages();
	}
	
	private static void loadNpcMessages() {
		final String query = "select npc_id, message_id, message from npc_messages";
		DbConnection.load(query, rs -> npcMessages.add(new NpcMessageDto(rs.getInt("npc_id"), rs.getInt("message_id"), rs.getString("message"))));
	}
	
	public static ArrayList<String> getMessagesByNpcId(int npcId) {
		ArrayList<String> messages = new ArrayList<>();
		
		for (NpcMessageDto dto : npcMessages) {
			if (dto.getNpcId() == npcId)
				messages.add(dto.getMessage());
		}
		
		return messages;
	}
	
	public static String getMessageByNpcIdMessageId(int npcId, int messageId) {
		for (NpcMessageDto dto : npcMessages) {
			if (dto.getNpcId() == npcId && dto.getMessageId() == messageId)
				return dto.getMessage();
		}
		System.out.println(String.format("couldn't find message with npcid=%d and messageid=%d.", npcId, messageId));
		return null;
	}
}
