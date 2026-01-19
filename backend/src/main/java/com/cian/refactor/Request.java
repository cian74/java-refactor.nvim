package	com.cian.refactor;

public class Request {
	String command;
	String source;

	//fields for extractMethod
	Integer start_line;
	Integer end_line;
	String method_name;
	String highlighted;

	//getter setters
	java.util.List<String> selected_fields;
}
