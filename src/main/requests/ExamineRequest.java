package main.requests;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ExamineRequest extends MultiRequest {
	private int objectId;
	private String objectName;
	private String type;
}
