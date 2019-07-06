package com.dafy.ana;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;





public class DubboSim {

	/**
	 * 1）在那台机上暴露什么方法。例如：在当地址：localhost，端口：8899，上暴露方法：UserService.getUserName()
	 * 2）使用什么序列化协议。例如使用dubbo或thrift序列化
	 * 3）通信框架是什么。如netty
	 * 4）将暴露的方法要注册到哪个地方去。如zookeeper
	 */
	public static void run() {
		//定义要暴露的方法使用的dubbo作为序列化协议，以及方法所在的服务器地址和端口号
		URL methodUrl = new URL("dubbo","localhost",8899)
				.addParameter("interface", "com.dafy.ana.IUserService")//暴露的服务
				.addParameter("methods", "getUserName")//暴露的方法
				.addParameter("scope", "remote")
				.addParameter("timeout", 1000)
				.addParameter("retries", 2);
		
		//定义注册中心的地址和端口号
		URL registryURL = new URL("registry","127.0.0.1",2181,"com.alibaba.dubbo.registry.zookeeper.ZookeeperRegistry");
		
		//配置文件的路径 new File("dubbo-2.4.8/META-INF/dubbo/internal/")
		//从配置中加载暴露服务时可以使用的代理实现，如：JdkProxy，JavassistProxy他们都间接实现了ProxyFactory
		ProxyFactory  proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
		//从配置中加载可以使用的序列化协议，如：DubboProtocol，JsonRpcProtocol他们都间接实现了接口Protocol
		Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
		//从配置中加载可以使用的注册中心的实现，如：ZookeeperRegistryFactory，RedisRegistryFactory
		RegistryFactory registryFactory = ExtensionLoader.getExtensionLoader(RegistryFactory.class).getAdaptiveExtension();
		
		URL url = registryURL.addParameter(org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY, methodUrl.toFullString());
		//真正提供服务的实例
		UserService userServiceIns = new UserService();
        Invoker<?> invoker = proxyFactory.getInvoker(userServiceIns,UserService.class,url);
        Exporter exporter = protocol.export(invoker);
	}
	
	
	public static void main(String[] args) {
		run();
		try {
			Thread.currentThread().sleep(300*1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
