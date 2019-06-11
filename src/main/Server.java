package main;

import java.util.HashSet;
import java.util.Scanner;

import javax.websocket.DeploymentException;

import main.database.ConsumableDao;
import main.database.EquipmentDao;
import main.database.ItemDao;
import main.database.MineableDao;
import main.database.NPCDao;
import main.database.NpcMessageDao;
import main.database.SceneryDao;
import main.processing.NPCManager;
import main.processing.PathFinder;
import main.processing.WorldProcessor;
import main.responses.CachedResourcesResponse;
import main.responses.ExamineResponse;
import main.utils.RandomUtil;

public class Server {

	public static void main(String[] args) {
		org.glassfish.tyrus.server.Server server = new org.glassfish.tyrus.server.Server("localhost", 45555, "/ws", null, Endpoint.class);
		org.glassfish.tyrus.server.Server resourceServer = new org.glassfish.tyrus.server.Server("localhost", 45556, "/ws", null, ResourceEndpoint.class);
		
		try {
			resourceServer.start();
			server.start();
			
		//			
		//
		//
		//1. help they took my babby
		//2. can you help me get my babby
		//a. yes i will help find your babby
		//b. fuck you i wont help find your babby
		//3. omg thanks plz be fast finding my babby
		//4. fuck u i want my babby
		//5. did you find my babby yet
		//c. no i didnt find your babby yet
		//6. pls be fast finding my babby
		//d. i found your babby
		//7. omg you find my babby
		//8. thank you for finding my babby
		//e. your babby is gone sorry
		//9. omg my babby gone
		//10. thanks for trying tho
		//
		//
		//npc_dialogue
		//npc_id, point_id, dialogue_id, dialogue
		//
		//npc_dialogue_options
		//npc_id, dialogue_option_id, dialogue_option, dialogue_from, dialogue_to, next_point_id, required_items_to_show
		//
		//npc_dialogue_entry_point
		//player_id, npc_id, point_id
		//
		//
		//TalkToRequest: {
		//	objectId
		//}
		//TalkToResponse.process()
		//- select point_id p from npc_dialogue_entry_point where player_id=? and npc_id=objectId
		//- if p exists
		//	- set player.dialogue.dialogue_id=1, player.dialogue.pointId=p
		//	- select * from npc_dialogue where point_id=p and dialogue_id=1
		//	- send ShowDialogueResponse with npc_dialogue.dialogue
		//
		//
		//ShowDialogueRequest: empty req (dialogue data is saved in player from the TalkToResponse)
		//
		//ShowDialogueResponse.process()
		//- if player.dialogue == null
		//  - return;
		//
		//- increment dialogue_id
		//- select * from npc_dialogue_options where dialogue_from=dialogue_id and other criteria (required items in inv exist etc) are met
		//- if records exist
		//  - send ShowDialogueOptionsResponse with array of dialogue_options
		//  - return;
		//
		//- no records exist; select * from npc_dialogue where npc_dialogue.dialogue_id=dialogue_id
		//- if record exists
		//  - send ShowDialogueResponse with npc_dialogue.dialogue
		//  - return;
		//
		//- send CloseDialogueResponse
		//
		//SelectDialogueOptionRequest: {
		//	optionId
		//}
		//SelectDialogueOptionResponse
		//- if player.dialogue == null
		//  - return;
		//
		//- select * from npc_dialogue_option where dialogue_option_id=? and npc_id=player.dialogue.npc_id
		//- if record exists
		//  - set npc_dialogue_entry_point.point_id=npc_dialogue_option.next_point_id where player.id=? and npc_id=player.dialogue.npc_id
		//  - ShowDialogueResponse.process()
		//			
			
			
			
			
			
//			HashSet<Integer> treeLocations = SceneryDao.getImpassableTileIdsByRoomId(1);
//			HashSet<Integer> npcInstances = NPCDao.getNpcInstanceIds();
//			
//			for (int i = 0; i < 50; ++i) {
//				
//				int potentialTileId = -1;
//				while (true) {
//					potentialTileId = RandomUtil.getRandom(0, PathFinder.LENGTH * PathFinder.LENGTH - 1);
//					
//					if (treeLocations.contains(potentialTileId))
//						continue;
//					
//					if (npcInstances.contains(potentialTileId))
//						continue;
//					
//					break;
//				}
//				
//				if (potentialTileId != -1) {
//					NPCDao.addNpcInstance(1, 3, potentialTileId);
//					npcInstances.add(potentialTileId);
//				}
//			}
			
			PathFinder.get();// init the path nodes and relationships
			ExamineResponse.initializeExamineMap();// all the scenery examine
			MineableDao.setupCaches();// mineable tiles, mineable objects
			ItemDao.setupCaches();
			NPCManager.get().loadNpcs();
			NpcMessageDao.setupCaches();
			ConsumableDao.cacheConsumables();
			EquipmentDao.setupCaches();
			
			// should be last after all the other caches are set up
			CachedResourcesResponse.get();// loads/caches all the sprite maps, scenery etc and gets sent on client load
			
			WorldProcessor processor = new WorldProcessor();
			processor.start();
			
			System.out.println("press any key to quit");
			new Scanner(System.in).nextLine();
		} catch (DeploymentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			server.stop();
		}
	}
}
