package	com.cian.refactor;

public class Request {
	String command;
	String source;

	//fields for extractMethod
	Integer start_line;
	Integer end_line;
	String method_name;
	String highlighted;
	
	//fields for extractVariable
	String var_name;

	//getter setters
	java.util.List<String> selected_fields;
	
	//fields for profile_method
	String class_name;
}
