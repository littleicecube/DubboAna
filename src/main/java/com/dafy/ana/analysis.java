package com.dafy.ana;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import org.apache.dubbo.common.extension.Adaptive;
import org.apache.dubbo.common.extension.AdaptiveClassCodeGenerator;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.utils.ArrayUtils;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.rpc.Protocol;

import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;

public class analysis {

	
 /**
  * 
  * 
  * 
ExtensionLoader是某一种类型配置的容器，程序在启动过程中会将指定类型的配置加载到ExtensionLoader的实例中。
例如当获取有哪些序列化协议的实现时会将File(META-INF/dubbo/internal/org.apache.dubbo.rpc.Protocol)路径下
配置的内容加载到对应的ExtensionLoader实例中
META-INF/dubbo/internal/org.apache.dubbo.rpc.Protocol)中的内容摘录如下:
	qos=org.apache.dubbo.qos.protocol.QosProtocolWrapper
	filter=org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper
	listener=org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper
	dubbo=org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
	thrift=org.apache.dubbo.rpc.protocol.thrift.ThriftProtocol
	registry=org.apache.dubbo.registry.integration.RegistryProtocol
	
加载的代码如下：
	ExtensionLoader loader = ExtensionLoader.getExtensionLoader(Protocol.class);
	1）获取dubbo序列化协议的实现，此处是直接指定了要用dubbo作为序列化协议
	DubboProtocol dubboProtocolIns = loader.getExtension("dubbo");
	
	通过loader.getExtension("dubbo")获取的并不是原始的协议实例，还会对协议实例进行包装加强
	//内部先创建协议的实例dubboProtocolIns
	DubboProtocol dubboProtocolIns = new DubboProtocol();
	//将dubboProtocolIns包装成一个filterWrapper,ProtocolFilterWrapper中会将配置路径下的filter
	//信息加载到ProtocolFilterWrapper中并实例化
	ProtocolFilterWrapper filterWrapper = ProtocolFilterWrapper(dubboProtocolIns);
	//将filterWrapper再次包装成一个listenerWrapper，ProtocolListenerWrapper中将配置路径下的listener
	//信息加载到ProtocolListenerWrapper中并实例化
	ProtocolListenerWrapper listenerWrapper = ProtocolListenerWrapper(filterWrapper);
	//将listenerWrapper再次包装成QosProtocolWrapper实例
	QosProtocolWrapper qosWrapper = new QosProtocolWrapper(listenerWrapper);
	根据配置信息对生成协议实例包装,最终loader.getExtension("dubbo")获取的是被包装过后的qosWrapper。需要注意的是
	并不是所有的ExtensionLoader代表的类型都需要包装，路径下没有配置包装类信息的类型不需要包装
	
	2）上一步中获取类型的包装实例后就可以使用其中的功能。如：
	DubboProtocol dubboProtocolIns = loader.getExtension("dubbo");
	//将要暴露的服务通过协议暴露出去
	dubboProtocolIns.export(new Invoker("dubbo://userService.getUserNameById"));
	
	3）假如要暴露的服务是dubbo://userService.getUserNameById还可以通过获取协议的Adaptive来处理
	ExtensionLoader loader = ExtensionLoader.getExtensionLoader(Protocol.class);
	Protocol protocol = loader.getAdaptiveExtension();
	protocol.export(new invoker("dubbo"));
	
	//上面的代码并不是直接获取一个具体序列化协议的实现，而是多个协议的代理类，这个代理类可以配置在对应类型的
	//路径下，类型初始化的时候会被加载，如果没有配置，dubbo中会利用字节码技术动态生成一个。Protocol类型的信息在
	//初始化的时候就会动态生成一个Adaptive类如：
	 public static class Protocol$Adpative implements  Protocol {
		public void destroy() {
			throw new UnsupportedOperationException("method public ");
		}
		public int getDefaultPort() {
			throw new UnsupportedOperationException("method public ");
		}
		public  Invoker refer( Class arg0, org.apache.dubbo.common.URL arg1) {
			org.apache.dubbo.common.URL url = arg1;
			String extName = url.getProtocol() == null ? "dubbo" : url.getProtocol();
			//也是先通过类型获取对应配置的集合，在根据协议的名称获取具体协议的实现
			Protocol extension = ( Protocol) ExtensionLoader.getExtensionLoader( Protocol.class).getExtension(extName);
			return extension.refer(arg0, arg1);
		}
		public  Exporter export( Invoker arg0) {
			org.apache.dubbo.common.URL url = arg0.getUrl();
			String extName = url.getProtocol() == null ? "dubbo" : url.getProtocol() ;
			//也是先通过类型获取对应配置的集合，在根据协议的名称获取具体协议的实现
			Protocol extension = ( Protocol) ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(extName);
			return extension.export(arg0);
		}
	}


在描述了ExtensionLoader的基本作用后下面是其成员变量：
class ExtensionLoader{
	//表示当前ExtensionLoader实例描述的是org.apache.dubbo.rpc.Protocol类型的信息
	private final Class<?> type = org.apache.dubbo.rpc.Protocol;
	//如果配置文件中存在的class上配置有注解则保存在
	private volatile Class<?> cachedAdaptiveClass = null;
	//如果配置文件中存在的class上配置有注解实例化后则保存在
	private final Holder<Object> cachedAdaptiveInstance = new Holder<Object>();
	private final ExtensionFactory objectFactory;
	//要加载类型可能有几种实现方式，程序启动后解析配置保存在当前实例中，根据实现class获取对应的名称
	//	cachedNames:{
	//		org.apache.dubbo.rpc.protocol.thrift.ThriftProtocol:"thrift"，
	//		org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol:"dubbo"
	//	}
	private final ConcurrentMap<Class<?>， String> cachedNames = new ConcurrentHashMap<Class<?>， String>();
	//要加载类型可能有几种实现方式，程序启动后解析配置然后保存在当前实例中，可以根据名称获取对应的实现类
	//	Holder:{
	//		"thrift":org.apache.dubbo.rpc.protocol.thrift.ThriftProtocol，
	//		"dubbo":org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
	// 	}
	private final Holder<Map<String， Class<?>>> cachedClasses = new Holder<Map<String，Class<?>>>();
	private final Map<String， Activate> cachedActivates = new ConcurrentHashMap<String， Activate>();
	//要加载类型实现类，实例化后存放的地方
	//	cachedInstances:{
	//		"thrift":new org.apache.dubbo.rpc.protocol.thrift.ThriftProtocol()，
	//		"dubbo":new org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol()
	//	}
	private final ConcurrentMap<String， Holder<Object>> cachedInstances = new ConcurrentHashMap<String， Holder<Object>>();
	private String cachedDefaultName;
	//要加载类型的实现类，可能需要被装饰一下，cachedWrapperClasses中存放了包装的盒子
	//	cachedWrapperClasses:[
	//		org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper，
	//		org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper，
	 * 		org.apache.dubbo.qos.protocol.QosProtocolWrapper
	//	]
	private Set<Class<?>> cachedWrapperClasses;
	private final ExtensionFactory objectFactory;
	private final Map<String， Activate> cachedActivates = new ConcurrentHashMap<String， Activate>();
	private String cachedDefaultName;
}



ExtensionLoader loader = ExtensionLoader.getExtensionLoader(Protocol.class);
public T getExtension(String name) {
	Object 	instance = createExtension(name);
	return (T) instance;
}
对要根据名称获取的实例进行增强
1)比如获取dubbo对应的实例全限定名为org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
2)实例化DubboProtocol
3)遍历DubboProtocol中的方法，如果方法是set开头的则调用设置一些配置进去
4)获取DubboProtocol对应的包装类信息，比如对应配置的包装类信息为QosProtocolWrapper，ProtocolFilterWrapper，ProtocolListenerWrapper
则实例化QosProtocolWrapper，ProtocolFilterWrapper，ProtocolListenerWrapper然后将DubboProtocol的实例作为参数对其进行包装
5)返回最终的实例
private T createExtension(String name) {
	//根据名称获取DubboProtocol并创建实例
	Class<?> clazz = getExtensionClasses().get(name);
	T instance = clazz.newInstance();
	//先调用注入方法，同过instance中的set方法为其注入参数
	injectExtension(instance);
	//遍历所有的包装方法
	Set<Class<?>> wrapperClasses = cachedWrapperClasses;
	if (wrapperClasses != null && wrapperClasses.size() > 0) {
		for (Class<?> wrapperClass : wrapperClasses) {
			//根据配置的包装类，将instance进行嵌套包装
			instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
		}
	}
	return instance;
}


private Map<String, Class<?>> getExtensionClasses() {
    Map<String, Class<?>> classes = cachedClasses.get();
    if (classes == null) {
        synchronized (cachedClasses) {
            classes = cachedClasses.get();
            if (classes == null) {
            	//加载路径下配置的类信息
                classes = loadExtensionClasses();
                cachedClasses.set(classes);
            }
        }
    }
    return classes;
}

private Map<String, Class<?>> loadExtensionClasses() {
    cacheDefaultExtensionName();
    Map<String, Class<?>> extensionClasses = new HashMap<>();
    //加载META-INF/dubbo/internal路径下的信息
    loadDirectory(extensionClasses, DUBBO_INTERNAL_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
    //加载META-INF/dubbo/dubbo路径下的信息
    loadDirectory(extensionClasses, DUBBO_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
    //加载META-INF/dubbo/services路径下的信息
    loadDirectory(extensionClasses, SERVICES_DIRECTORY, type.getName().replace("org.apache", "com.alibaba"));
    return extensionClasses;
}


private void loadDirectory(Map<String, Class<?>> extensionClasses, String dir, String type) {
    String fileName = dir + type;
    Enumeration<java.net.URL> urls = ClassLoader.getSystemResources(fileName);
    if (urls != null) {
        while (urls.hasMoreElements()) {
            java.net.URL resourceURL = urls.nextElement();
            //加载包路径下的信息
            loadResource(extensionClasses, classLoader, resourceURL);
        }
    }
}

private void loadResource(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, java.net.URL resourceURL) {
    BufferedReader reader = new BufferedReader(new InputStreamReader(resourceURL.openStream(), StandardCharsets.UTF_8))；
    String line;
    //读取每一行信息，解析后用加载用Class描述
    while ((line = reader.readLine()) != null) {
        final int ci = line.indexOf('#');
        if (ci >= 0) {
            line = line.substring(0, ci);
        }
        line = line.trim();
        if (line.length() > 0) {
            String name = null;
            int i = line.indexOf('=');
            if (i > 0) {
                name = line.substring(0, i).trim();
                line = line.substring(i + 1).trim();
            }
            if (line.length() > 0) {
            	//创建配置类信息的Class描述，并根据类上的注解类型存在到ExtensionLoader实例的对应的变量中
            	//如line = "org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper";
                loadClass(extensionClasses, resourceURL, Class.forName(line, true, classLoader), name);
            }
        }
    }
}

private void loadClass(Map<String, Class<?>> extensionClasses, java.net.URL resourceURL, Class<?> clazz, String name) throws NoSuchMethodException {
	//如果类上存在Adaptive注解则存放到ExtensionLoader实例的对应的变量中
    if (clazz.isAnnotationPresent(Adaptive.class)) {
        cacheAdaptiveClass(clazz);
    } else if (isWrapperClass(clazz)) {
    //如果类是包装类型的则存放到ExtensionLoader实例的对应的变量中
        cacheWrapperClass(clazz);
    } else {
        clazz.getConstructor();
        if (StringUtils.isEmpty(name)) {
            name = findAnnotationName(clazz);
            if (name.length() == 0) {
                throw new IllegalStateException("No such extension name for the class " + clazz.getName() + " in the config " + resourceURL);
            }
        }

        String[] names = NAME_SEPARATOR.split(name);
        if (ArrayUtils.isNotEmpty(names)) {
        	//如果类上存在Activate注解则存放到ExtensionLoader实例的对应的变量中
            cacheActivateClass(clazz, names[0]);
            for (String n : names) {
            	//缓存配置的类信息
                cacheName(clazz, n);
                saveInExtensionClass(extensionClasses, clazz, name);
            }
        }
    }
}




获取对应的Adaptive类
private T createAdaptiveExtension() {
    try {
        return injectExtension((T) getAdaptiveExtensionClass().newInstance());
    } catch (Exception e) {
        throw new IllegalStateException("Can't create adaptive extension " + type + ", cause: " + e.getMessage(), e);
    }
}
创建类型对应的Adaptive类
private Class<?> getAdaptiveExtensionClass() {
    getExtensionClasses();
    if (cachedAdaptiveClass != null) {
        return cachedAdaptiveClass;
    }
    return cachedAdaptiveClass = createAdaptiveExtensionClass();
}
private Class<?> createAdaptiveExtensionClass() {
	//调用Adaptive类生成器，根据规则生成对应的class信息
    String code = new AdaptiveClassCodeGenerator(type, cachedDefaultName).generate();
    ClassLoader classLoader = findClassLoader();
    //加载可以用的编译工具，如JdkCompiler，JavassistCompiler将上一步生成的code编译成字节码
    org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
    return compiler.compile(code, classLoader);
}







E1)获取文件中内容new File(dubbo-2.4.8/META-INF/dubbo/internal/org.apache.dubbo.rpc.Protocol);
文件中的内容摘录如下:
	registry=org.apache.dubbo.registry.integration.RegistryProtocol
	filter=org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper
	listener=org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper
	dubbo=org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
	//不存在别名
	org.apache.dubbo.rpc.protocol.http.HttpProtocol
	thrift=org.apache.dubbo.rpc.protocol.thrift.ThriftProtocol

private void loadFile(Map<String， Class<?>> extensionClasses， String dir) {
  //读取每一行内容如:dubbo=org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
  while ((line = reader.readLine()) != null) {
	//将org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol加载到内存
	Class<?> clazz = Class.forName(line， true， classLoader);
	//如果类上配置有Adaptive注解则存放到cachedAdaptiveClass
	if (clazz.isAnnotationPresent(Adaptive.class)) {
		cachedAdaptiveClass = clazz;
	}else{
		try{
			//如果被解析的类中不存在type参数的构造方法，说明这不是个包装类则在异常中处理
			//如果存在则添加到包装类集合中，到最后被实例化时，多个包装类会形成调用链
			clazz.getConstructor(type);
			Set<Class<?>> wrappers = cachedWrapperClasses;
			wrappers.add(clazz);
		} catch (NoSuchMethodException e) {
			clazz.getConstructor();
			//如果配置信息是默认配置，则从文件名称中解析name值
			if (name == null || name.length() == 0) {
				name = clazz.getSimpleName().substring(0， clazz.getSimpleName().length() - type.getSimpleName().length()).toLowerCase();
			}
			//如果类上配置有Adaptive注解则存放到cachedAdaptiveClass
			Activate activate = clazz.getAnnotation(Activate.class);
			if (activate != null) {
				cachedActivates.put(names[0]， activate);
			}
			//缓存配置的类信息，值是类的别名
			cachedNames.put(clazz， n);
			//缓存配置的类信息
			extensionClasses.put(n， clazz);
		}
	}
  }
}




利用ExtensionLoader加载org.apache.dubbo.rpc.Protocol的过程
A)ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.Protocol.class).getAdaptiveExtension()
获取org.apache.dubbo.rpc.Protocol的ExtensionLoader实例然后获取其配置的扩展信息
B)  
public T getAdaptiveExtension() {
	Object instance = cachedAdaptiveInstance.get();
	//调用C)获取配置信息
	instance = createAdaptiveExtension();
	return (T) instance;
}
C)
private T createAdaptiveExtension() {
	try {
		//先执行C1获取编译类型(org.apache.dubbo.common.compiler.Compiler.class)的ExtensionLoader配置信息，并创建实现的实例化信息
		//在调用D)为生成的实例通过set方法注入数据信息
		return injectExtension((T) getAdaptiveExtensionClass().newInstance());
	} catch (Exception e) {
		throw new IllegalStateException("Can not create adaptive extenstion " + type + "， cause: " + e.getMessage()， e);
	}
}
C1)
private Class<?> getAdaptiveExtensionClass() {
	//调用E)加载org.apache.dubbo.rpc.Protocol的扩展配置信息到当前实例中
	getExtensionClasses();
	//执行C2)创建org.apache.dubbo.common.compiler.Compiler.class类型配置的实例信息
	return cachedAdaptiveClass = createAdaptiveExtensionClass();
}
C2)
//以字节码的方式为要加载的类型创建一个实现类，用来获取指定配置文件中的某个实现参建link#Adaptive
private Class<?> createAdaptiveExtensionClass() {
	//为org.apache.dubbo.rpc.Protocol创建实现字节码(public class Protocol$Adpative implements  Protocol) 
	String code = createAdaptiveExtensionClassCode();
	ClassLoader classLoader = findClassLoader();
	//通过ExtensionLoader扩展机制加载org.apache.dubbo.common.compiler.Compiler.class，并获取对应的实例默认是javasist
	org.apache.dubbo.common.compiler.Compiler compiler = ExtensionLoader.getExtensionLoader(org.apache.dubbo.common.compiler.Compiler.class).getAdaptiveExtension();
	//将Protocol$Adpative对应的字节码编译生成Class返回
	return compiler.compile(code， classLoader);
}
D)//遍历instance中以set开头的方法，通过反射调用为instance设置参数
private T injectExtension(T instance) {
	for (Method method : instance.getClass().getMethods()) {
		if (method.getName().startsWith("set")
				&& method.getParameterTypes().length == 1
				&& Modifier.isPublic(method.getModifiers())) {
				Class<?> pt = method.getParameterTypes()[0];
				String property = method.getName().length() > 3 ? method.getName().substring(3， 4).toLowerCase() + method.getName().substring(4) : "";
				Object object = objectFactory.getExtension(pt， property);
				if (object != null) {
					method.invoke(instance， object);
				}
		}
	}
	return instance;
}

E)
private Map<String， Class<?>> getExtensionClasses() {
	//执行D1)
	Map<String， Class<?>> classes = loadExtensionClasses();
	return classes;
}
E1)获取文件中内容new File(dubbo-2.4.8/META-INF/dubbo/internal/org.apache.dubbo.rpc.Protocol);
文件中的内容摘录如下:
	registry=org.apache.dubbo.registry.integration.RegistryProtocol
	filter=org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper
	listener=org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper
	dubbo=org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
	//不存在别名
	org.apache.dubbo.rpc.protocol.http.HttpProtocol
	thrift=org.apache.dubbo.rpc.protocol.thrift.ThriftProtocol

private void loadFile(Map<String， Class<?>> extensionClasses， String dir) {
  //读取每一行内容如:dubbo=org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
  while ((line = reader.readLine()) != null) {
	//将org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol加载到内存
	Class<?> clazz = Class.forName(line， true， classLoader);
	//如果类上配置有Adaptive注解则存放到cachedAdaptiveClass
	if (clazz.isAnnotationPresent(Adaptive.class)) {
		cachedAdaptiveClass = clazz;
	}else{
		try{
			//如果被解析的类中不存在type参数的构造方法，说明这不是个包装类则在异常中处理
			//如果存在则添加到包装类集合中，到最后被实例化时，多个包装类会形成调用链
			clazz.getConstructor(type);
			Set<Class<?>> wrappers = cachedWrapperClasses;
			wrappers.add(clazz);
		} catch (NoSuchMethodException e) {
			clazz.getConstructor();
			//如果配置信息是默认配置，则从文件名称中解析name值
			if (name == null || name.length() == 0) {
				name = clazz.getSimpleName().substring(0， clazz.getSimpleName().length() - type.getSimpleName().length()).toLowerCase();
			}
			//如果类上配置有Adaptive注解则存放到cachedAdaptiveClass
			Activate activate = clazz.getAnnotation(Activate.class);
			if (activate != null) {
				cachedActivates.put(names[0]， activate);
			}
			//缓存配置的类信息，值是类的别名
			cachedNames.put(clazz， n);
			//缓存配置的类信息
			extensionClasses.put(n， clazz);
		}
	}
  }
}


















<dubbo:service/>		服务配置		用于暴露一个服务，定义服务的元信息，一个服务可以用多个协议暴露，一个服务也可以注册到多个注册中心
<dubbo:reference/>		引用配置		用于创建一个远程服务代理，一个引用可以指向多个注册中心
<dubbo:protocol/>		协议配置		用于配置提供服务的协议信息，协议由提供方指定，消费方被动接受
<dubbo:application/>	应用配置		用于配置当前应用信息，不管该应用是提供者还是消费者
<dubbo:module/>			模块配置		用于配置当前模块信息，可选
<dubbo:registry/>		注册中心配置	用于配置连接注册中心相关信息
<dubbo:monitor/>		监控中心配置	用于配置连接监控中心相关信息，可选
<dubbo:provider/>		提供方配置		当 ProtocolConfig 和 ServiceConfig 某属性没有配置时，采用此缺省值，可选
<dubbo:consumer/>		消费方配置		当 ReferenceConfig 某属性没有配置时，采用此缺省值，可选
<dubbo:method/>			方法配置		用于 ServiceConfig 和 ReferenceConfig 指定方法级的配置信息
<dubbo:argument/>		参数配置		用于指定方法参数配置

package org.apache.dubbo.rpc;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class Protocol$Adpative implements  Protocol {
	public void destroy() {
		throw new UnsupportedOperationException("method public ");
	}
	public int getDefaultPort() {
		throw new UnsupportedOperationException("method public ");
	}
	public  Invoker refer( Class arg0， org.apache.dubbo.common.URL arg1) throws  Class {
		if (arg1 == null) 
			throw new IllegalArgumentException("url == null");
		org.apache.dubbo.common.URL url = arg1;
		String extName = url.getProtocol() == null ? "dubbo" : url.getProtocol();
		if(extName == null) 
			throw new IllegalStateException("Fail to get extension( Protocol) name from url(" + url.toString() + ") use keys([protocol])");
		 Protocol extension = ( Protocol) ExtensionLoader.getExtensionLoader( Protocol.class).getExtension(extName);
		return extension.refer(arg0， arg1);
	}
	public  Exporter export( Invoker arg0) throws  Invoker {
		if (arg0 == null) 
			throw new IllegalArgumentException(" Invoker argument == null");
		if (arg0.getUrl() == null) 
			throw new IllegalArgumentException(" Invoker argument getUrl() == null");
		org.apache.dubbo.common.URL url = arg0.getUrl();
		String extName = url.getProtocol() == null ? "dubbo" : url.getProtocol() ;
		if(extName == null) 
			throw new IllegalStateException("Fail to get extension( Protocol) name from url(" + url.toString() + ") use keys([protocol])");
		 Protocol extension = ( Protocol) ExtensionLoader.getExtensionLoader( Protocol.class).getExtension(extName);
		return extension.export(arg0);
	}
}

package org.apache.dubbo.rpc;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class ProxyFactory$Adpative implements org.apache.dubbo.rpc.ProxyFactory {
	public Invoker getInvoker( Object arg0，  Class arg1， org.apache.dubbo.common.URL arg2) throws  Object {
		if (arg2 == null) {
			throw new IllegalArgumentException("url == null");
		}
		org.apache.dubbo.common.URL url = arg2;
		String extName = url.getParameter("proxy"， "javassist");
		if(extName == null){
			throw new IllegalStateException("Fail to get extension");
		}
		ProxyFactory extension = (ProxyFactory)ExtensionLoader.getExtensionLoader( ProxyFactory.class).getExtension(extName);
		return extension.getInvoker(arg0， arg1， arg2);
	}
	public  Object getProxy( Invoker arg0) throws  Invoker {
		if (arg0 == null) {
			throw new IllegalArgumentException(" Invoker argument == null");
		}
		if (arg0.getUrl() == null) {
			throw new IllegalArgumentException(" Invoker argument getUrl() == null");
		}
		org.apache.dubbo.common.URL url = arg0.getUrl();
		String extName = url.getParameter("proxy"， "javassist");
		if(extName == null){
			throw new IllegalStateException("Fail to get extension");
		}
		 ProxyFactory extension = ( ProxyFactory)ExtensionLoader.getExtensionLoader( ProxyFactory.class).getExtension(extName);
		return extension.getProxy(arg0);
	}
}










org.apache.dubbo.config.RegistryConfig
org.apache.dubbo.config.ProtocolConfig
org.apache.dubbo.config.spring.ServiceBean
org.apache.dubbo.common.extension.ExtensionFactory



以类的全路径名称为文件名从路径下加载文件如加载class:org.apache.dubbo.rpc.Protocol
则加载路径dubbo-2.4.8/META-INF/dubbo/internal/org.apache.dubbo.rpc.Protocol
文件中的内容:
registry=org.apache.dubbo.registry.integration.RegistryProtocol
filter=org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper
listener=org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper
mock=org.apache.dubbo.rpc.support.MockProtocol
injvm=org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol
dubbo=org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol
rmi=org.apache.dubbo.rpc.protocol.rmi.RmiProtocol
hessian=org.apache.dubbo.rpc.protocol.hessian.HessianProtocol
org.apache.dubbo.rpc.protocol.http.HttpProtocol
org.apache.dubbo.rpc.protocol.webservice.WebServiceProtocol
thrift=org.apache.dubbo.rpc.protocol.thrift.ThriftProtocol
memcached=memorg.apache.dubbo.rpc.protocol.memcached.MemcachedProtocol
redis=org.apache.dubbo.rpc.protocol.redis.RedisProtocol



静态类型:
	private static final ConcurrentMap<Class<?>， ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>， ExtensionLoader<?>>();
	private static final ConcurrentMap<Class<?>， Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>， Object>();

非静态类型:
	//要加载的类型，比如org.apache.dubbo.rpc.Protocol
	private final Class<?> type;
	
	//type类型是个接口类型的，需要实现类做支撑，实现类可以在type路径下的配置文件中配置，对应的class上有注解
	//当type路径下的配置文件信息被加载时检测到则存放到cachedAdaptiveClass
	//检测代码:
	//  if (clazz.isAnnotationPresent(Adaptive.class)) {
	//     if(cachedAdaptiveClass == null) {
	//			cachedAdaptiveClass = clazz;
	//		} else if (! cachedAdaptiveClass.equals(clazz)) {
	//			throw new IllegalStateException("More than 1 adaptive class found: "
	//					+ cachedAdaptiveClass.getClass().getName()
	//					+ "， " + clazz.getClass().getName());
	//		}
	//	}
	//当配置文件中不存在含有注解的类信息时，会通过字节码技术创建一个类加载到当前线程中
	//如org.apache.dubbo.rpc.Protocol类通过字节码创建的实现类
		package org.apache.dubbo.rpc;
		import org.apache.dubbo.common.extension.ExtensionLoader;
		public class Protocol$Adpative implements org.apache.dubbo.rpc.Protocol {
			public void destroy() {
				throw new UnsupportedOperationException("method public...");
			}
			public int getDefaultPort() {
				throw new UnsupportedOperationException("method public abstract..，");
			}
			public org.apache.dubbo.rpc.Invoker refer(java.lang.Class arg0， org.apache.dubbo.common.URL arg1) throws java.lang.Class {
				if (arg1 == null) 
					throw new IllegalArgumentException("url == null");
				org.apache.dubbo.common.URL url = arg1;
				String extName = url.getProtocol() == null ? "dubbo" : url.getProtocol();
				if(extName == null) 
					throw new IllegalStateException("Fail to get extension(org.apache.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");
				org.apache.dubbo.rpc.Protocol extension = (org.apache.dubbo.rpc.Protocol) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.Protocol.class).getExtension(extName);
				return extension.refer(arg0， arg1);
			}
			public org.apache.dubbo.rpc.Exporter export(org.apache.dubbo.rpc.Invoker arg0) throws org.apache.dubbo.rpc.Invoker {
				if (arg0 == null) 
					throw new IllegalArgumentException("org.apache.dubbo.rpc.Invoker argument == null");
				if (arg0.getUrl() == null) 
					throw new IllegalArgumentException("org.apache.dubbo.rpc.Invoker argument getUrl() == null");org.apache.dubbo.common.URL url = arg0.getUrl();
				String extName = url.getProtocol() == null ? "dubbo" : url.getProtocol() ;
				if(extName == null) 
					throw new IllegalStateException("Fail to get extension(org.apache.dubbo.rpc.Protocol) name from url(" + url.toString() + ") use keys([protocol])");
				org.apache.dubbo.rpc.Protocol extension = (org.apache.dubbo.rpc.Protocol) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.Protocol.class).getExtension(extName);
				return extension.export(arg0);
			}
		}
	
	private volatile Class<?> cachedAdaptiveClass = null;

	private final Holder<Object> cachedAdaptiveInstance = new Holder<Object>();
	
	private final ExtensionFactory objectFactory;
	
	//从type代表的文件中解析出来配置信息，存放到cachedNames
	//如:[{org.apache.dubbo.rpc.protocol.thrift.ThriftProtocol:"thrift"}，{org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol:"dubbo"}]
	private final ConcurrentMap<Class<?>， String> cachedNames = new ConcurrentHashMap<Class<?>， String>();
	
	//从type代表的文件中解析出来配置信息，存放到cachedClasses
	//如:[{"thrift":org.apache.dubbo.rpc.protocol.thrift.ThriftProtocol}，{"dubbo":org.apache.dubbo.rpc.protocol.dubbo.DubboProtocol}]
	private final Holder<Map<String， Class<?>>> cachedClasses = new Holder<Map<String，Class<?>>>();
	
	//从type代表的文件中解析出来配置信息，判断其中的类上是否含有注解Activate
	private final Map<String， Activate> cachedActivates = new ConcurrentHashMap<String， Activate>();
	
	//从type代表的文件中解析出来配置信息，实例化后存放到Holder中，在存放到map中
	private final ConcurrentMap<String， Holder<Object>> cachedInstances = new ConcurrentHashMap<String， Holder<Object>>();

	private String cachedDefaultName;

	private volatile Throwable createAdaptiveInstanceError;
	//clazz.getConstructor(type); 如果从文件解析出的class含有一个构造函数，函数的入参是type类型的，则存放到cachedWrapperClasses
	private Set<Class<?>> cachedWrapperClasses;

	private Map<String， IllegalStateException> exceptions = new ConcurrentHashMap<String， IllegalStateException>();








protocol有几种可能是dubbo可能是inJvm的，而这个protocol的外围可能被包围，被包装。不论protocol是什么
stubProxyFactoryWrapper是个包装类，是对JavassistProxyFactory的包装
===================================================================================================================================
几个基本点：
    正常情况下网络服务至少要两个线程，或者一个线程一个线程组，一个线程用来接收到来的请求，一个用来处理到来的请求。
    服务的提供第一点要初始化确定要提供什么服务，服务名称.方法名称。大量的服务名称.方法名构成一个集合需要被处理和管理
    因为是网络请求，一般情况下到来的数据需要经过一系列的filter然后到达最终的处理handler，在经过filter时也需要分发事件给listener做一些监视和统计处理
        还要注意的是，filter和listener的添加在服务名.方法名在被处理化的时候和他们继承在一起，这是filter和listener的入口点，当一个service.method被调用时
        他的handler不能立即被调用，需要先调用注册在service.method实例中的filter和listener
    最后是线程池的服务（线程池本身存在一个线程中）线程池共享service.method的集合
=====================================================================================================================================
如果传入的Type类型中的方法上有@Adaptive注解，但是在实现类中并未有指定实现，则动态创建
@SPI("javassist")
public interface ProxyFactory {
    @Adaptive({Constants.PROXY_KEY})
    <T> T getProxy(Invoker<T> invoker) throws RpcException;
    @Adaptive({Constants.PROXY_KEY})
    <T> Invoker<T> getInvoker(T proxy， Class<T> type， URL url) throws RpcException;
}
动态创建的代码如下：
package org.apache.dubbo.rpc;
import org.apache.dubbo.common.extension.ExtensionLoader;
public class ProxyFactory$Adpative implements org.apache.dubbo.rpc.ProxyFactory {
    public java.lang.Object getProxy(org.apache.dubbo.rpc.Invoker arg0) throws org.apache.dubbo.rpc.Invoker {
        org.apache.dubbo.common.URL url = arg0.getUrl();
        String extName = url.getParameter("proxy"， "javassist");
        org.apache.dubbo.rpc.ProxyFactory extension = (org.apache.dubbo.rpc.ProxyFactory) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.ProxyFactory.class).getExtension(extName);
        return extension.getProxy(arg0);
    }
    public org.apache.dubbo.rpc.Invoker getInvoker(java.lang.Object arg0，java.lang.Class arg1， org.apache.dubbo.common.URL arg2)throws java.lang.Object {
        org.apache.dubbo.common.URL url = arg2;
        String extName = url.getParameter("proxy"， "javassist");
        org.apache.dubbo.rpc.ProxyFactory extension = (org.apache.dubbo.rpc.ProxyFactory) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.ProxyFactory.class).getExtension(extName);
        return extension.getInvoker(arg0， arg1， arg2);
    }
}
有此代码，调用compire后生成实例
=========================================================================================================================================
ProxyFactory
得到ExtensionLoader的实例后调用其getAdaptiveExtension()方法获取到实例后执行，ExtendsionLoader中type类型代表的实例
很绕的一个东西，我们首先由一个Type类型，是一个接口，接口中有方法的描述，然后在创建一个ExtensionLoader实例，实例中有Type类型接口，有Type类型实现的实例，ExtendsLoader相当于
Type类型的代理，又是一个对Type和Type类型实现的装饰。
    ProxyFactory的实例的主要作用：proxyFactory有个动态代码创建的ProxyFactory$Adaptive的实例，用这个实例调用StubProxyFactoryWrapper的实例stubProxyFactoryWrapper，stubProxyFactoryWrapper实例作为JavassistProxyFactory实例javassistProxyFactory的包装
        当调用stubProxyFactoryWrapper时会调用javassistProxyFactory实例中的方法getInvoker将要代理的实例如com.palace.seeds.dubbox.provider.HelloImp封装成一个Invoker返回
        总的来说ProxyFactory就是讲要代理的实例封装成一个Invoker
    Protocol的主要作用是调用export，export的主要作用是将生成的invoker进行包装。获取org.apache.dubbo.rpc.Protocol类型的ExtensionLoader的实例。获取的时候会加载两个包装如下：
        class org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper， class org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper，将生org.apache.dubbo.rpc.Protocol对应的实现类进行包装，org.apache.dubbo.rpc.Protocol的
        实现类的实例作为org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper实例的参数，然后在作为org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper的参数，包装完成后返回作为getExtension的返回去处理ProxyFactory生成的invoker实例
        然后invoker作为参数调用ProtocolFilterWrapper中的方法，ProtocolFilterWrapper是一个Filter的类型，它的ExtensionLoader实例，会加载一系列的Fileter，这些Filter是配置文件中创建的或者是动态代码创建的，然后把这些filter应用在invoker上，
        在调用ProtocolListenerWrapper中的方法，和ProtocolFilterWrapper类似，这里是为invoker添加listener，ProtocolListenerWrapper作为一种类型会创建他的ExtensionLoader实例，然后从配置文件中加载Listener或者是动态代码创建，然后应用在invoker上
        然后生成一个ListenerExporterWrapper的实例返回代码如下：
        =================================================================================================================================
        return new ListenerExporterWrapper<T>(
                protocol.export(invoker)， 获取
                Collections.unmodifiableList(
                        ExtensionLoader.getExtensionLoader(
                                                            ExporterListener.class 加载ExporterListener.class的实现，实现可能有多个
                                                        ).getActivateExtension(
                                                                                invoker.getUrl()，    调用跟当前协议有关的ExporterListener.class的实现
                                                                                Constants.EXPORTER_LISTENER_KEY
                                                                            )
                        )
                    );
        ===================================================================================================================================
    以上是Protocol.export的作用
    9）
    Exporter<?> exporter = protocol.export(
            1）
            proxyFactory.getInvoker(ref， (Class) interfaceClass， local)//返回一个invoker
       );
    2）
    ProxyFactory$Adaptive
    public org.apache.dubbo.rpc.Invoker getInvoker(java.lang.Object arg0，java.lang.Class arg1， org.apache.dubbo.common.URL arg2)throws java.lang.Object {
        org.apache.dubbo.common.URL url = arg2;
        String extName = url.getParameter("proxy"， "javassist");
                                                                                            这个地方根据org.apache.dubbo.rpc.ProxyFactory.class加载其实现类，其实现类有
                                                                                                    stub=org.apache.dubbo.rpc.proxy.wrapper.StubProxyFactoryWrapper，jdk=org.apache.dubbo.rpc.proxy.jdk.JdkProxyFactory，javassist=org.apache.dubbo.rpc.proxy.javassist.JavassistProxyFactory
                                                                                                    其中StubProxyFactoryWrapper是对JdkProxyFactory，和JavassistProxyFactory的包装，请看解析1，
                                                                                                    这里解释了为什么下马的步骤4）的实现类为StubProxyFactoryWrapper，而步骤6）中的实例为JavassistProxyFactory
        org.apache.dubbo.rpc.ProxyFactory extension = (org.apache.dubbo.rpc.ProxyFactory) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.ProxyFactory.class).getExtension(extName);
                3)
        return extension.getInvoker(arg0， arg1， arg2);
    }
    4)
    StubProxyFactoryWrapper是对JavassistProxyFactory的包装，下一步骤看到调用进入JavassistProxyFactory
    public <T> Invoker<T> getInvoker(T proxy， Class<T> type， URL url) throws RpcException {
        5)
        return proxyFactory.getInvoker(proxy， type， url);
    }
    6)
    JavassistProxyFactory
    public <T> Invoker<T> getInvoker(T proxy， Class<T> type， URL url) {
        // TODO Wrapper类不能正确处理带$的类名
        7)
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
        8)
        return new AbstractProxyInvoker<T>(proxy， type， url) {
            @Override
            protected Object doInvoke(T proxy， String methodName，
                                      Class<?>[] parameterTypes，
                                      Object[] arguments) throws Throwable {
                return wrapper.invokeMethod(proxy， methodName， parameterTypes， arguments);
            }
        };
    }
[class org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper， class org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper]
    10）这个是org.apache.dubbo.rpc.Protocol的Adpative
    public java.lang.Object getProxy(org.apache.dubbo.rpc.Invoker arg0) throws org.apache.dubbo.rpc.Invoker {
        org.apache.dubbo.common.URL url = arg0.getUrl();
        String extName = url.getParameter("proxy"， "javassist");
                                                                                                            和步骤2)中的代码一样，获取org.apache.dubbo.rpc.Proxy.class类型代表的ExtesionLoader，并且和步骤2中一样也有
                                                                                                            装饰类，org.apache.dubbo.rpc.Proxy.class的实现类的实例也被包装，而且是两个包装第一个包装类将org.apache.dubbo.rpc.Proxy.class的实例包装
                                                                                                            后生成一个实例，然后又创建了一个包装类，并将上一个包装类的实例作为参数传递调第二个包装类中，然后返回第二个包装类的实例
                                                                                                            两个包装类是： [class org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper， class org.apache.dubbo.rpc.protocol.ProtocolListenerWrapper]
        org.apache.dubbo.rpc.Proxy extension = (org.apache.dubbo.rpc.Proxy) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.Proxy.class).getExtension(extName);
        return extension.getProxy(arg0);
    }
    解析1
    ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.ProxyFactory.class).getExtension(extName);
    此name参数为javassist，从javassist=org.apache.dubbo.rpc.proxy.javassist.JavassistProxyFactory配置得知其值为org.apache.dubbo.rpc.proxy.javassist.JavassistProxyFactory
      @SuppressWarnings("unchecked")
    public T getExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        if ("true".equals(name)) {
            return getDefaultExtension();
        }
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name， new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.get();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.get();
                if (instance == null) {
                    name=javassist，这个参数代表的值被包装
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }
    @SuppressWarnings("unchecked")
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }
        try {
            根据name值javassist得到的class为org.apache.dubbo.rpc.proxy.javassist.JavassistProxyFactory，然后实例化
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                EXTENSION_INSTANCES.putIfAbsent(clazz， (T) clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            injectExtension(instance);
            调试时可知道cachedWrapperClasses的值是org.apache.dubbo.rpc.proxy.wrapper.StubProxyFactoryWrapper，然后创建org.apache.dubbo.rpc.proxy.wrapper.StubProxyFactoryWrapper的实例
            创建完成后将上面org.apache.dubbo.rpc.proxy.javassist.JavassistProxyFactory的实例作为参数传递到org.apache.dubbo.rpc.proxy.wrapper.StubProxyFactoryWrapper的实例中返回
            代码如下：
            Set<Class<?>> wrapperClasses = cachedWrapperClasses;
            if (wrapperClasses != null && wrapperClasses.size() > 0) {
                for (Class<?> wrapperClass : wrapperClasses) {
                    instance = injectExtension((T) wrapperClass.getConstructor(type).newInstance(instance));
                }
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + "， class: " +
                    type + ")  could not be instantiated: " + t.getMessage()， t);
        }
    }
    org.apache.dubbo.rpc.Protocol类型代表的实例创建ExtensionLoader中实例中的动态代码
    package org.apache.dubbo.rpc;
    import org.apache.dubbo.common.extension.ExtensionLoader;
    public class Protocol$Adpative implements org.apache.dubbo.rpc.Protocol {
    public void destroy() {
        throw new UnsupportedOperationException("method public abstract void org.apache.dubbo.rpc.Protocol.destroy() of interface org.apache.dubbo.rpc.Protocol is not adaptive method!");
    }
    public int getDefaultPort() {
        throw new UnsupportedOperationException("method public abstract int org.apache.dubbo.rpc.Protocol.getDefaultPort() of interface org.apache.dubbo.rpc.Protocol is not adaptive method!");
    }
    public org.apache.dubbo.rpc.Exporter export(org.apache.dubbo.rpc.Invoker arg0)throws org.apache.dubbo.rpc.Invoker {
        org.apache.dubbo.common.URL url = arg0.getUrl();
        String extName = ((url.getProtocol() == null) ? "dubbo" : url.getProtocol());
        org.apache.dubbo.rpc.Protocol extension = (org.apache.dubbo.rpc.Protocol) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.Protocol.class).getExtension(extName);
        return extension.export(arg0);
    }
    public org.apache.dubbo.rpc.Invoker refer(java.lang.Class arg0，org.apache.dubbo.common.URL arg1) throws java.lang.Class {
        org.apache.dubbo.common.URL url = arg1;
        String extName = ((url.getProtocol() == null) ? "dubbo": url.getProtocol());
        org.apache.dubbo.rpc.Protocol extension = (org.apache.dubbo.rpc.Protocol) ExtensionLoader.getExtensionLoader(org.apache.dubbo.rpc.Protocol.class).getExtension(extName);
        return extension.refer(arg0， arg1);
    }
}
injvm://127.0.0.1/com.palace.seeds.dubbox.api.IHello?anyhost=true&application=demo-provider&dubbo=2.4.8&interface=com.palace.seeds.dubbox.api.IHello&methods=sayHello&organization=dubbox&owner=programmer&pid=5656&side=provider&timestamp=1482029781928
{
generic=class org.apache.dubbo.rpc.filter.GenericFilter，
deprecated=class org.apache.dubbo.rpc.filter.DeprecatedFilter，
monitor=class org.apache.dubbo.monitor.support.MonitorFilter，
cache=class org.apache.dubbo.cache.filter.CacheFilter，
validation=class org.apache.dubbo.validation.filter.ValidationFilter，
activelimit=class org.apache.dubbo.rpc.filter.ActiveLimitFilter，
trace=class org.apache.dubbo.rpc.protocol.dubbo.filter.TraceFilter，
exception=class org.apache.dubbo.rpc.filter.ExceptionFilter，
consumercontext=class org.apache.dubbo.rpc.filter.ConsumerContextFilter，
genericimpl=class org.apache.dubbo.rpc.filter.GenericImplFilter，
echo=class org.apache.dubbo.rpc.filter.EchoFilter，
token=class org.apache.dubbo.rpc.filter.TokenFilter，
future=class org.apache.dubbo.rpc.protocol.dubbo.filter.FutureFilter，
compatible=class org.apache.dubbo.rpc.filter.CompatibleFilter，
classloader=class org.apache.dubbo.rpc.filter.ClassLoaderFilter，
context=class org.apache.dubbo.rpc.filter.ContextFilter，
accesslog=class org.apache.dubbo.rpc.filter.AccessLogFilter，
executelimit=class org.apache.dubbo.rpc.filter.ExecuteLimitFilter，
timeout=class org.apache.dubbo.rpc.filter.TimeoutFilter
}
{
activelimit=@org.apache.dubbo.common.extension.Activate(after=[]， value=[actives]， group=[consumer]， order=0， before=[])，
classloader=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[provider]， order=-30000， before=[])，
accesslog=@org.apache.dubbo.common.extension.Activate(after=[]， value=[accesslog]， group=[provider]， order=0， before=[])，
context=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[provider]， order=-10000， before=[])，
token=@org.apache.dubbo.common.extension.Activate(after=[]， value=[token]， group=[provider]， order=0， before=[])，
exception=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[provider]， order=0， before=[])，
validation=@org.apache.dubbo.common.extension.Activate(after=[]， value=[validation]， group=[consumer， provider]， order=10000， before=[])，
future=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[consumer]， order=0， before=[])，
timeout=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[provider]， order=0， before=[])，
executelimit=@org.apache.dubbo.common.extension.Activate(after=[]， value=[executes]， group=[provider]， order=0， before=[])，
consumercontext=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[consumer]， order=-10000， before=[])，
deprecated=@org.apache.dubbo.common.extension.Activate(after=[]， value=[deprecated]， group=[consumer]， order=0， before=[])，
generic=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[provider]， order=-20000， before=[])，
monitor=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[provider， consumer]， order=0， before=[])，
trace=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[provider]， order=0， before=[])，
cache=@org.apache.dubbo.common.extension.Activate(after=[]， value=[cache]， group=[consumer， provider]， order=0， before=[])，
echo=@org.apache.dubbo.common.extension.Activate(after=[]， value=[]， group=[provider]， order=-110000， before=[])，
genericimpl=@org.apache.dubbo.common.extension.Activate(after=[]， value=[generic]， group=[consumer]， order=20000， before=[])
}
[
org.apache.dubbo.rpc.filter.EchoFilter@201548d3，
org.apache.dubbo.rpc.filter.ClassLoaderFilter@55602519，
org.apache.dubbo.rpc.filter.GenericFilter@1b23b819，
org.apache.dubbo.rpc.filter.ContextFilter@13038e01，
org.apache.dubbo.rpc.protocol.dubbo.filter.TraceFilter@460b7f3a，
org.apache.dubbo.monitor.support.MonitorFilter@43bf0e5d，
org.apache.dubbo.rpc.filter.TimeoutFilter@1ccddcc3，
org.apache.dubbo.rpc.filter.ExceptionFilter@f5894fb
]
org.apache.dubbo.rpc.protocol.injvm.InjvmProtocol@72bc71dd
ProxyFactory-->StubProxyFactoryWrapper-->JavassistProxyFactory
public class JavassistProxyFactory extends AbstractProxyFactory {
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Invoker<T> invoker， Class<?>[] interfaces) {
        return (T) Proxy.getProxy(interfaces).newInstance(new InvokerInvocationHandler(invoker));
    }
    public <T> Invoker<T> getInvoker(T proxy， Class<T> type， URL url) {
        // TODO Wrapper类不能正确处理带$的类名
        final Wrapper wrapper = Wrapper.getWrapper(proxy.getClass().getName().indexOf('$') < 0 ? proxy.getClass() : type);
        return new AbstractProxyInvoker<T>(proxy， type， url) {
            @Override
            protected Object doInvoke(T proxy， String methodName，
                                      Class<?>[] parameterTypes，
                                      Object[] arguments) throws Throwable {
                return wrapper.invokeMethod(proxy， methodName， parameterTypes， arguments);
            }
        };
    }
}
{
spi=class org.apache.dubbo.common.extension.factory.SpiExtensionFactory，
spring=class org.apache.dubbo.config.spring.extension.SpringExtensionFactory
}
class org.apache.dubbo.common.extension.factory.AdaptiveExtensionFactory
class org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper
class org.apache.dubbo.rpc.protocol.ProtocolFilterWrapper
jar:file:/C:/Users/wzj/.m2/repository/com/alibaba/dubbo/2.4.8/dubbo-2.4.8.jar!/META-INF/dubbo/internal/org.apache.dubbo.rpc.Protocol
==============================================================================================================================
org.apache.dubbo.remoting.exchange.support.header.HeaderExchanger@11908def
{
grizzly=class org.apache.dubbo.remoting.transport.grizzly.GrizzlyTransporter，
netty=class org.apache.dubbo.remoting.transport.netty.NettyTransporter，
mina=class org.apache.dubbo.remoting.transport.mina.MinaTransporter
} 





  * 
  */
}
