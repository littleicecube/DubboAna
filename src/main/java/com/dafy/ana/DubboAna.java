package com.dafy.ana;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class DubboAna {

	
	public static void run() {
	    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"dubbo-demo-provider.xml"});
	    context.start();
	    System.out.println("Provider started.");
	}
	
	
	public static void main(String[] args) throws Exception {
		run();
		Thread.currentThread().sleep(300*1000);
	}
}
