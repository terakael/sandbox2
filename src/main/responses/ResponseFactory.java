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
		case "use":
			response = new UseResponse();
			break;
		case "smith":
			response = new SmithResponse();
			break;
		case "attack":
			response = new AttackResponse();
			break;
		case "toggle_attack_style":
			response = new ToggleAttackStyleResponse();
			break;
		case "talk to":
			response = new TalkToResponse();
			break;
		case "eat":
			response = new EatResponse();
			break;
		case "drink":
			response = new DrinkResponse();
			break;
		case "dialogue":
			response = new DialogueResponse();
			break;
		case "dialogue_option":
			response = new DialogueOptionResponse();
			break;
		case "shop":
			response = new ShopResponse();
			break;
		case "value":
			response = new ShopValueResponse();
			break;
		case "buy":
			response = new ShopBuyResponse();
			break;
		case "sell":
			response = new ShopSellResponse();
			break;
		case "close_shop":
			response = new CloseShopResponse();
			break;
		case "cancel_trade":
			response = new CancelTradeResponse();
			break;
		case "offer":
			response = new OfferResponse();
			break;
		case "rescind":
			response = new RescindResponse();
			break;
		case "accept_trade_offer":
			response = new AcceptTradeOfferResponse();
			break;
		case "catch":
			response = new CatchResponse();
			break;
		case "climb":
			response = new ClimbResponse();
			break;
		case "pick":
			response = new PickResponse();
			break;
		case "flower_deplete":
			response = new FlowerDepleteResponse();
			break;
		case "flower_respawn":
			response = new FlowerRespawnResponse();
			break;
		case "bank":
			response = new BankResponse();
			break;
		case "deposit":
			response = new BankDepositResponse();
			break;
		case "withdraw":
			response = new BankWithdrawResponse();
			break;
		case "show_stat_window":
			response = new ShowStatWindowResponse();
			break;
		case "fish":
			response = new FishResponse();
			break;
			
		default:
			response = new UnknownResponse();
			break;
		}
		
		response.setAction(action);
		return response;
	}
}
