package com.bbn.marti.util.spring;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

public class ThreadLocalRequestHolder {
	
	private static ThreadLocal<HttpServletRequest> requestThreadLocal = new ThreadLocal<>();
	
	public static void setRequest(ServletRequest request) {
		if (request != null && request instanceof HttpServletRequest) {
			requestThreadLocal.set((HttpServletRequest) request);
		} 
	}
	
	public static HttpServletRequest getRequest() {
		return requestThreadLocal.get();
	}

}
