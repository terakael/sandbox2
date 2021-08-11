package database.dao;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import database.DbConnection;
import database.dto.PlayerArtisanBlockedTaskDto;
import database.entity.insert.InsertPlayerArtisanBlockedTaskEntity;
import database.entity.update.UpdatePlayerArtisanBlockedTaskEntity;
import processing.managers.DatabaseUpdater;

public class PlayerArtisanBlockedTaskDao {
	private static Map<Integer, PlayerArtisanBlockedTaskDto> blockedTasks = new HashMap<>();
	
	public static void setupCaches() {
		DbConnection.load("select * from player_artisan_blocked_tasks", rs -> {
			blockedTasks.put(rs.getInt("player_id"), 
					new PlayerArtisanBlockedTaskDto(rs.getInt("player_id"), rs.getInt("item1"), rs.getInt("item2"), rs.getInt("item3"), rs.getInt("item4"), rs.getInt("item5")));
		});
	}
	
	public static PlayerArtisanBlockedTaskDto getBlockedTasksByPlayerId(int playerId) {
		return blockedTasks.get(playerId);
	}
	
	public static boolean blockTask(int playerId, int itemId) {
		if (!blockedTasks.containsKey(playerId)) {
			blockedTasks.put(playerId, new PlayerArtisanBlockedTaskDto(playerId, itemId, 0, 0, 0, 0));
			DatabaseUpdater.enqueue(InsertPlayerArtisanBlockedTaskEntity.builder()
					.playerId(playerId)
					.item1(itemId)
					.item2(0)
					.item3(0)
					.item4(0)
					.item5(0)
					.build());
		} else {			
			final PlayerArtisanBlockedTaskDto dto = blockedTasks.get(playerId);
			// you can only block something a single time; disallow multiple of the same item
			if (getBlockedItemIds(playerId).contains(itemId))
				return true;
			
			if (dto.getItem1() == 0) {
				dto.setItem1(itemId);
				DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item1(itemId).build());
			} else if (dto.getItem2() == 0) {
				dto.setItem2(itemId);
				DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item2(itemId).build());
			} else if (dto.getItem3() == 0) {
				dto.setItem3(itemId);
				DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item3(itemId).build());
			} else if (dto.getItem4() == 0) {
				dto.setItem4(itemId);
				DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item4(itemId).build());
			} else if (dto.getItem5() == 0) {
				dto.setItem5(itemId);
				DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item5(itemId).build());
			} else {
				// no free slots to block
				return false;
			}
		}
		return true;
	}
	
	public static boolean unblockTask(int playerId, int itemId) {
		if (!blockedTasks.containsKey(playerId))
			return false;
		
		final PlayerArtisanBlockedTaskDto dto = blockedTasks.get(playerId);
		
		// use ifs instead of elseifs just in case the user somehow blocked the same item multiple times - clear all occurrences
		if (dto.getItem1() == itemId) {
			dto.setItem1(0);
			DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item1(0).build());
		}
		
		if (dto.getItem2() == itemId) {
			dto.setItem2(0);
			DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item2(0).build());
		}

		if (dto.getItem3() == itemId) {
			dto.setItem3(0);
			DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item3(0).build());
		}

		if (dto.getItem4() == itemId) {
			dto.setItem4(0);
			DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item4(0).build());
		}

		if (dto.getItem5() == itemId) {
			dto.setItem5(0);
			DatabaseUpdater.enqueue(UpdatePlayerArtisanBlockedTaskEntity.builder().playerId(playerId).item5(0).build());
		}
		
		return true;
	}
	
	public static Set<Integer> getBlockedItemIds(int playerId) {
		Set<Integer> blockedItemIds = new LinkedHashSet<>();
		if (!blockedTasks.containsKey(playerId))
			return blockedItemIds;
		
		blockedItemIds.add(blockedTasks.get(playerId).getItem1());
		blockedItemIds.add(blockedTasks.get(playerId).getItem2());
		blockedItemIds.add(blockedTasks.get(playerId).getItem3());
		blockedItemIds.add(blockedTasks.get(playerId).getItem4());
		blockedItemIds.add(blockedTasks.get(playerId).getItem5());
		
		return blockedItemIds.stream().filter(e -> e > 0).collect(Collectors.toSet()); // exclude unfilled slots (i.e. 0)
	}
}
