package builder.responses;

import java.util.List;

import builder.requests.Request;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class Response {
	private int success = 1;
	private String responseText = "";
	@Setter private String action;

	public void setRecoAndResponseText(int success, String responseText) {
		this.success = success;
		this.responseText = responseText;
	}
	public abstract void process(Request req, List<Response> responses);
}
