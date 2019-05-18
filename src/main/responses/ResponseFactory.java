package main.responses;

public class ResponseFactory {

	public static Response create(String action) {
		if (action == null)
			return new UnknownResponse("");
		
		if (action.equals("logon"))
			return new LogonResponse(action);
		
		if (action.equals("logoff"))
			return new LogoffResponse(action);
		
		if (action.equals("move"))
			return new MoveResponse(action);
		
		if (action.equals("message"))
			return new MessageResponse(action);
		
		if (action.equals("addexp"))
			return new AddExpResponse(action);
		
		if (action.equals("duel"))
			return new DuelResponse(action);
		
		if (action.equals("trade"))
			return new TradeResponse(action);
		
		if (action.equals("follow"))
			return new FollowResponse(action);
		
		if (action.equals("inv") || action.equals("invmove") || action.equals("invadd") || action.equals("invdrop"))
			return new InventoryUpdateResponse(action);
		
		if (action.equals("equip"))
			return new EquipResponse(action);
		
		if (action.equals("drop"))
			return new DropResponse(action);
		
		if (action.equals("take"))
			return new TakeResponse(action);
		
		if (action.equals("playerLeave"))
			return new PlayerLeaveResponse(action);
		
		if (action.equals("examine"))
			return new ExamineResponse(action);
		
		if (action.equals("mine"))
			return new MineResponse(action);
		
		return new UnknownResponse(action);
	}
}
