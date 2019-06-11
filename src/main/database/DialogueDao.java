package main.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DialogueDao {
	public static NpcDialogueDto getEntryDialogueByPlayerIdNpcId(int playerId, int npcId) {
		final String query = 
				"select npc_dialogue.npc_id, npc_dialogue.point_id, npc_dialogue.dialogue_id, npc_dialogue.dialogue from player_npc_dialogue_entry_points" + 
				" inner join npc_dialogue on player_npc_dialogue_entry_points.npc_id=npc_dialogue.npc_id and player_npc_dialogue_entry_points.point_id=npc_dialogue.point_id" + 
				" where player_id=? and npc_dialogue.npc_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, npcId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new NpcDialogueDto(rs.getInt("npc_id"), rs.getInt("point_id"), rs.getInt("dialogue_id"), rs.getString("dialogue"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static NpcDialogueDto getDialogue(int npcId, int pointId, int dialogueId) {
		final String query = "select npc_id, point_id, dialogue_id, dialogue from npc_dialogue" +
					" where npc_id=? and point_id=? and dialogue_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, npcId);
			ps.setInt(2, pointId);
			ps.setInt(3, dialogueId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next())
					return new NpcDialogueDto(rs.getInt("npc_id"), rs.getInt("point_id"), rs.getInt("dialogue_id"), rs.getString("dialogue"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public static ArrayList<NpcDialogueOptionDto> getDialogueOptionsBySrcDialogueId(int npcId, int pointId, int dialogueId) {
		final String query = 
				"select npc_id, point_id, option_id, option_text, dialogue_src, dialogue_dest, next_point_id, required_item_1, required_item_2, required_item_3" + 
				" from npc_dialogue_options" + 
				" where npc_id=? and point_id=? and dialogue_src=?";
		
		ArrayList<NpcDialogueOptionDto> options = new ArrayList<>();
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, npcId);
			ps.setInt(2, pointId);
			ps.setInt(3, dialogueId);
			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					options.add(new NpcDialogueOptionDto(
							rs.getInt("npc_id"),
							rs.getInt("option_id"),
							rs.getString("option_text"),
							rs.getInt("point_id"),
							rs.getInt("dialogue_src"),
							rs.getInt("dialogue_dest"),
							rs.getInt("next_point_id"),
							rs.getInt("required_item_1"),
							rs.getInt("required_item_2"),
							rs.getInt("required_item_3")
					));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return options;
	}
	
	public static NpcDialogueOptionDto getDialogueOption(int npcId, int pointId, int optionId) {
		final String query = 
				"select npc_id, point_id, option_id, option_text, dialogue_src, dialogue_dest, next_point_id, required_item_1, required_item_2, required_item_3" + 
				" from npc_dialogue_options" + 
				" where npc_id=? and point_id=? and option_id=?";
		
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query);
		) {
			ps.setInt(1, npcId);
			ps.setInt(2, pointId);
			ps.setInt(3, optionId);
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return new NpcDialogueOptionDto(
							rs.getInt("npc_id"),
							rs.getInt("option_id"),
							rs.getString("option_text"),
							rs.getInt("point_id"),
							rs.getInt("dialogue_src"),
							rs.getInt("dialogue_dest"),
							rs.getInt("next_point_id"),
							rs.getInt("required_item_1"),
							rs.getInt("required_item_2"),
							rs.getInt("required_item_3"));
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static void setPlayerNpcDialogueEntryPoint(int playerId, int npcId, int pointId) {
		final String query =
				"insert into player_npc_dialogue_entry_points values (?, ?, ?)" + 
				" on duplicate key update point_id=?";
		try (
			Connection connection = DbConnection.get();
			PreparedStatement ps = connection.prepareStatement(query)
		) {
			ps.setInt(1, playerId);
			ps.setInt(2, npcId);
			ps.setInt(3, pointId);
			ps.setInt(4, pointId);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
