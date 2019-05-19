package main.responses;

public class ResponseFactory {	
	public static Response create(String action) {
		if (action == null)
			return new UnknownResponse();
		
		Response response;
		
		switch (action) {			
		case "logon":
			response = new LogonResponse();
			break;
		case "logoff":
			response = new LogoffResponse();
			break;
		case "move":
			response = new MoveResponse();
			break;
		case "message":
			response = new MessageResponse();
			break;
		case "addexp":
			response = new AddExpResponse();
			break;
		case "duel":
			response = new DuelResponse();
			break;
		case "trade":
			response = new TradeResponse();
			break;
		case "follow":
			response = new FollowResponse();
			break;
		case "inv":// fall through
		case "invmove":// fall through
		case "invadd":// fall through
		case "invdrop":// fall through
		case "invupdate":
			response = new InventoryUpdateResponse();
			break;
		case "equip":
			response = new EquipResponse();
			break;
		case "drop":
			response = new DropResponse();
			break;
		case "take":
			response = new TakeResponse();
			break;
		case "playerLeave":
			response = new PlayerLeaveResponse();
			break;
		case "playerEnter":
			response = new PlayerEnterResponse();
			break;
		case "player_update":
			response = new PlayerUpdateResponse();
			break;
		case "examine":
			response = new ExamineResponse();
			break;
		case "mine":
			response = new MineResponse();
			break;
		case "start_mining":
			response = new StartMiningResponse();
			break;
		case "finish_mining":
			response = new FinishMiningResponse();
			break;
		default:
			response = new UnknownResponse();
			break;
		}
		
		response.setAction(action);
		return response;
	}
}
