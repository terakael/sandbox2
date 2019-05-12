package main.state;

import lombok.Getter;
import main.database.PlayerDao;
import main.database.PlayerDto;
import main.requests.Request;
import main.requests.TargetRequest;

public class MoveAndActionRequest {
//	@Getter private TargetRequest request;
//	
//	public MoveAndActionRequest(TargetRequest request) {
//		this.request = request;
//	}
//	
//	public boolean process(float dt) {
//		Player player = StateProcessor.get().getPlayer(request.getId());
//		
//		int x = (int)player.getCurrentPos().getX();
//		int y = (int)player.getCurrentPos().getY();
//		return x == request.getTargetX() && y == request.getTargetY();
//	}
//	
//	public void updateTargetPos(int x, int y) {
//		this.request.setTargetX(x);
//		this.request.setTargetY(y);
//	}
}
