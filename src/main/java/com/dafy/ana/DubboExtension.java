package com.dafy.ana;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ReferenceConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.cluster.Constants;





public class DubboExtension {

	/**
	 * 1）在那台机上暴露什么方法。例如：在当地址：localhost，端口：8899，上暴露方法：UserService.getUserNameByID()
	 * 2）暴露的服务使用哪种代理方法。如：JdkProxy，JavassistProxy
	 * 3）使用什么序列化协议。例如使用dubbo或thrift序列化
	 * 4）通信框架是什么。例如netty,mina
	 * 5）将暴露的方法要注册到哪个地方去，以便消费端获取访问地址。如zookeeper，redis
	 */
	public static void main(String[] args) {
		//定义要暴露的方法使用的dubbo作为序列化协议，以及方法所在的服务器地址和端口号
		URL methodUrl = new URL("dubbo","127.0.0.1",8899,IUserService.class.getName())
				.addParameter("interface", IUserService.class.getName())//暴露的服务
				.addParameter("methods", "getUserNameByID")//暴露的方法
				.addParameter("side", "provider")
				.addParameter("scope", "remote")
				.addParameter("register", true)
				.addParameter("timeout", 1000)
				.addParameter("retries", 2);

		//定义注册中心的地址和端口号
		URL registryURL = new URL("registry","127.0.0.1",2181).addParameter("registry", "zookeeper");
		//将注册中心的url和暴露方法定义的url进行合并，在程序启动时先根据方法定义的url将服务启动，在根据注册中心的url将暴露的方法注册到注册中心
		URL url = registryURL.addParameterAndEncoded(Constants.EXPORT_KEY, methodUrl.toFullString());
		//配置文件的路径 new File("dubbo-2.4.8/META-INF/dubbo/internal/")
		//从配置中加载暴露服务时可以使用的代理实现，如：JdkProxy，JavassistProxy他们都间接实现了ProxyFactory
		ProxyFactory  proxyFactory = ExtensionLoader.getExtensionLoader(ProxyFactory.class).getAdaptiveExtension();
		//真正提供服务的实例
		UserService userServiceIns = new UserService();
		//利用代理工具将要暴露的服务和接口封装成一个Invoker
        Invoker<?> invoker = proxyFactory.getInvoker(userServiceIns,IUserService.class,url);
		//从配置中加载可以使用的序列化协议，如：DubboProtocol，JsonRpcProtocol他们都间接实现了接口Protocol
		Protocol protocol = ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
		//将暴露的服务和通信框架进行关系绑定，并启动通信服务在相应的端口上进行监听
        //将暴露的服务注册到注册中心
        Exporter<?> exporter = protocol.export(invoker);
        System.out.println("^^^^^^^^^^服务端启动成功^^^^^^^^^^");
        
        //客户端发起调用
        ReferenceConfig<IUserService> reference = new ReferenceConfig<IUserService>(); 
        reference.setApplication(new ApplicationConfig("client"));
        reference.setRegistry(new RegistryConfig("127.0.0.1:2181","zookeeper")); 
        reference.setInterface(IUserService.class);
        String ret = reference.get().getUserNameByID(1234);
        System.out.println("调用结果："+ret);
	}
}
interface IUserService {

	public String getUserNameByID(long lUserId);
}
class UserService implements IUserService{

	public String getUserNameByID(long lUserId) {
		return "userName"+lUserId;
	}

}
