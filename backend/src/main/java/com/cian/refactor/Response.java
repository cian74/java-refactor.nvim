package com.cian.refactor;

public class Response {
	private String err;
	private String status;
	private String source;

	public static Response ok(String newSource){
		Response r = new Response();
		r.status = "ok";
		r.source = newSource;
		return r;
	}
}
