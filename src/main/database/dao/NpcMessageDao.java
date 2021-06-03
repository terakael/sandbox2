package main.database.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import main.database.DbConnection;
import main.database.dto.NpcMessageDto;

public class NpcMessageDao {
	private static ArrayList<NpcMessageDto> npcMessages = null;
	
	public static void setupCaches() {
		loadNpcMessages();
	}
	
	private static void loadNpcMessages() {
		final String query = "select npc_id, message_id, message from npc_messages";
		
		npcMessages = new ArrayList<>();
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next())
					npcMessages.add(new NpcMessageDto(rs.getInt("npc_id"), rs.getInt("message_id"), rs.getString("message")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
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
