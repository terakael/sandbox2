package main.processing;

import java.util.Stack;

import javax.websocket.Session;


import lombok.Getter;
import lombok.Setter;
import main.database.MineableDao;
import main.database.MineableDto;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.database.PlayerInventoryDao;
import main.database.StatsDao;
import main.requests.AddExpRequest;
import main.requests.MineRequest;
import main.requests.Request;
import main.requests.RequestFactory;
import main.responses.AddExpResponse;
import main.responses.FinishMiningResponse;
import main.responses.InventoryUpdateResponse;
import main.responses.MineResponse;
import main.responses.PlayerUpdateResponse;
import main.responses.Response;
import main.responses.ResponseFactory;
import main.responses.ResponseMaps;

public class Player {
	public enum PlayerState {
		idle,
		walking,
		chasing,// used for walking to a moving target (following, moving to attack something etc)
		following,
		mining,
		fighting
	};
	
	@Getter private PlayerDto dto;
	@Getter private Session session;
	@Setter private Stack<Integer> path = new Stack<>();// stack of tile_ids
	@Setter private PlayerState state = PlayerState.idle;
	@Setter private Request savedRequest = null;
	@Setter private int targetPlayerId;// used for following or chasing a player
	@Setter private int tickCounter = 0;
	
	public Player(PlayerDto dto, Session session) {
		this.dto = dto;
		this.session = session;
	}
	
	public void process(ResponseMaps responseMaps) {
		// called each tick; build a response where necessary
		switch (state) {
		case walking: {
			// if the player path stack isn't empty, then pop one off and create a player_updates response entry.
			if (!path.isEmpty()) {				
				setTileId(path.pop());

				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTile(getTileId());
				responseMaps.addLocalResponse(this, playerUpdateResponse);
				
				if (path.isEmpty()) {
					if (savedRequest == null) // if not null then reprocess the saved request; this is a walkandaction.
						state = PlayerState.idle;
					else {
						Response response = ResponseFactory.create(savedRequest.getAction());
						response.process(savedRequest, this, responseMaps);
//						savedRequest = null;
					}
				}
			}
			
			break;
		}
		case following: {
			if (!path.isEmpty()) {
				setTileId(path.pop());
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTile(getTileId());
				responseMaps.addLocalResponse(this, playerUpdateResponse);
			}

			Player targetPlayer = WorldProcessor.getPlayerById(targetPlayerId);
			
			// maybe the target player logged out
			if (targetPlayer == null) {
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.isNextTo(dto.getTileId(), targetPlayer.dto.getTileId())) {
				path = PathFinder.findPath(dto.getTileId(), targetPlayer.dto.getTileId(), false);
			}
			break;
		}
		case chasing: {
			// similar to walking, but need to recalculate path each tick due to moving target
			if (!path.isEmpty()) {
				setTileId(path.pop());
				PlayerUpdateResponse playerUpdateResponse = new PlayerUpdateResponse();
				playerUpdateResponse.setId(dto.getId());
				playerUpdateResponse.setTile(getTileId());
				responseMaps.addLocalResponse(this, playerUpdateResponse);
			}

			Player targetPlayer = WorldProcessor.getPlayerById(targetPlayerId);	
			if (targetPlayer == null) {
				// player could have logged out as they were chasing
				state = PlayerState.idle;
				break;
			}
			
			if (!PathFinder.isNextTo(dto.getTileId(), targetPlayer.dto.getTileId())) {
				path = PathFinder.findPath(dto.getTileId(), targetPlayer.dto.getTileId(), false);
			} else {
				// start the fight
				state = PlayerState.fighting;
				targetPlayer.state = PlayerState.fighting;
			}
			break;
		}
		case mining:
			// waiting until the tick counter hits zero, then do the actual mining and create a finish_mining response
			if (--tickCounter <= 0) {
				// TODO do checks:
				// is player close enough to target tile?
				// is target tile a rock?
				// does player have level to mine this rock?
				// does player have inventory space for the loot?
				
				// if yes to all above:
				// add rock loot to first empty inventory space
				// create storage_update response
				// create finish_mining response
				// set state to idle.
				if (!(savedRequest instanceof MineRequest)) {
					savedRequest = null;
					state = PlayerState.idle;
					return;
				}
				
				MineRequest mineRequest = (MineRequest)savedRequest;
				MineableDto mineable = MineableDao.getMineableDtoByTileId(mineRequest.getTileId());
				if (mineable == null) {
					MineResponse mineResponse = new MineResponse();
					mineResponse.setRecoAndResponseText(0, "you can't mine that.");
					responseMaps.addClientOnlyResponse(this, mineResponse);
					return;
				}
				PlayerInventoryDao.addItemByItemIdPlayerId(getId(), mineable.getItemId());
				
				AddExpRequest addExpReq = new AddExpRequest();
				addExpReq.setId(getId());
				addExpReq.setStatId(6);// mining
				addExpReq.setExp(mineable.getExp());
				
				new AddExpResponse().process(addExpReq, this, responseMaps);
				new FinishMiningResponse().process(savedRequest, this, responseMaps);
				new InventoryUpdateResponse().process(RequestFactory.create("dummy", getId()), this, responseMaps);
				
				savedRequest = null;
				state = PlayerState.idle;
			}
			break;
		case fighting:
			if (--tickCounter <= 0) {
				// calculate the actual attack, create hitspat_update response, reset tickCounter.
			}
			break;
			
		case idle:// fall through
		default:
			break;
		}
	}
	
	public void setTileId(int tileId) {
		dto.setTileId(tileId);
		PlayerDao.updateTileId(dto.getId(), tileId);
	}
	
	public int getTileId() {
		return dto.getTileId();
	}
	
	public boolean isGod() {
		return dto.getId() == 3;// god id
	}
	
	public int getId() {
		return dto.getId();
	}
}
