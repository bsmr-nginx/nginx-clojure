package nginx.clojure.wave;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import nginx.clojure.wave.MethodDatabase.ClassEntry;


public class SuspendMethodTracer {
	
	public static class MethodInfo {
		public String owner;
		public String method;
		public Integer suspendType = MethodDatabase.SUSPEND_NONE; 
		
		public MethodInfo() {
		}

		public MethodInfo(String owner, String method) {
			super();
			this.owner = owner;
			this.method = method;
		}
	}

	protected static ThreadLocal<ArrayList<MethodInfo> > tracerStacks = new ThreadLocal<ArrayList<MethodInfo>>();
	
	protected static AtomicBoolean dumping = new AtomicBoolean(false);
	
	protected static MethodDatabase db;
	
	public static ArrayList<MethodInfo> fetchStack() {
		ArrayList<MethodInfo> stack = tracerStacks.get();
		if (stack == null) {
			tracerStacks.set(stack = new ArrayList<MethodInfo>());
		}
		return stack;
	}
	
	public SuspendMethodTracer() {
	}
	
	protected static Map<String, Set<String>> SUSPEND_CAUSE_SET = new HashMap<String, Set<String>>();
	
	protected static ConcurrentHashMap<String, ConcurrentHashMap<String, Object>> SUSPEND_INFO_RESULTS = new ConcurrentHashMap<String, ConcurrentHashMap<String, Object>>();
	
	static {
//SocketInputStream
//		  public int read(byte[]) throws java.io.IOException;
//		    Signature: ([B)I
//
//		  public int read(byte[], int, int) throws java.io.IOException;
//		    Signature: ([BII)I
//
//		  int read(byte[], int, int, int) throws java.io.IOException;
//		    Signature: ([BIII)I
//
//		  public int read() throws java.io.IOException;
//		    Signature: ()I
//
//		  public long skip(long) throws java.io.IOException;
//		    Signature: (J)J
//
//		  public int available() throws java.io.IOException;
//		    Signature: ()I
//
//		  public void close() throws java.io.IOException;
//		    Signature: ()V
		{
		Set<String> socketInputMethods = new HashSet<String>();
		socketInputMethods.add("read([B)I");
		socketInputMethods.add("read([BII)I");
		socketInputMethods.add("read([BIII)I");
		socketInputMethods.add("read()I");
		socketInputMethods.add("skip(J)J");
		SUSPEND_CAUSE_SET.put("java/net/SocketInputStream", socketInputMethods);
		}
		
//SocketOutputStream
//		  public void write(int) throws java.io.IOException;
//		    Signature: (I)V
//
//		  public void write(byte[]) throws java.io.IOException;
//		    Signature: ([B)V
//
//		  public void write(byte[], int, int) throws java.io.IOException;
//		    Signature: ([BII)V
		{
		Set<String> socketOutputMethods = new HashSet<String>();
		socketOutputMethods.add("write(I)V");
		socketOutputMethods.add("write([B)V");
		socketOutputMethods.add("write([BII)V");
		SUSPEND_CAUSE_SET.put("java/net/SocketOutputStream", socketOutputMethods);
		}

//Socket
//		  public java.net.Socket(java.net.Proxy);
//		    Signature: (Ljava/net/Proxy;)V
//
//		  public java.net.Socket(java.lang.String, int) throws java.net.UnknownHostException, java.io.IOException;
//		    Signature: (Ljava/lang/String;I)V
//
//		  public java.net.Socket(java.net.InetAddress, int) throws java.io.IOException;
//		    Signature: (Ljava/net/InetAddress;I)V
//
//		  public java.net.Socket(java.lang.String, int, java.net.InetAddress, int) throws java.io.IOException;
//		    Signature: (Ljava/lang/String;ILjava/net/InetAddress;I)V
//
//		  public java.net.Socket(java.net.InetAddress, int, java.net.InetAddress, int) throws java.io.IOException;
//		    Signature: (Ljava/net/InetAddress;ILjava/net/InetAddress;I)V
//
//		  public java.net.Socket(java.lang.String, int, boolean) throws java.io.IOException;
//		    Signature: (Ljava/lang/String;IZ)V
//
//		  public java.net.Socket(java.net.InetAddress, int, boolean) throws java.io.IOException;
//		    Signature: (Ljava/net/InetAddress;IZ)V
//
//
//		  public void connect(java.net.SocketAddress) throws java.io.IOException;
//		    Signature: (Ljava/net/SocketAddress;)V
//
//		  public void connect(java.net.SocketAddress, int) throws java.io.IOException;
//		    Signature: (Ljava/net/SocketAddress;I)V
//
//		  public void bind(java.net.SocketAddress) throws java.io.IOException;
//		    Signature: (Ljava/net/SocketAddress;)V
		{
		Set<String> socketMethods = new HashSet<String>();
		socketMethods.add("<init>(Ljava/net/Proxy;)V");
		socketMethods.add("<init>(Ljava/lang/String;I)V");
		socketMethods.add("<init>(Ljava/net/InetAddress;I)V");
		socketMethods.add("<init>(Ljava/lang/String;ILjava/net/InetAddress;I)V");
		socketMethods.add("<init>(Ljava/net/InetAddress;ILjava/net/InetAddress;I)V");
		socketMethods.add("<init>(Ljava/lang/String;IZ)V");
		socketMethods.add("<init>(Ljava/net/InetAddress;IZ)V");
		socketMethods.add("connect(Ljava/net/SocketAddress;)V");
		socketMethods.add("connect(Ljava/net/SocketAddress;I)V");
//		socketMethods.add("bind(Ljava/net/InetAddress;IZ)V");
		
		SUSPEND_CAUSE_SET.put("java/net/Socket", socketMethods);
		}
		
		{
			Set<String> coroutineMethods = new HashSet<String>();
			coroutineMethods.add("yield()V");
			SUSPEND_CAUSE_SET.put("nginx/clojure/Coroutine", coroutineMethods);
		}
	}
	
	public static boolean isSuspend(String owner, String method) {
		Set<String> methodSet = SUSPEND_CAUSE_SET.get(owner);
		return methodSet != null && methodSet.contains(method);
	}
	
	public static void enter(String owner, String method) {
		if (dumping.get()) {
			return;
		}
		ArrayList<MethodInfo> stack = fetchStack();
		if (isSuspend(owner, method)) {
			for (int i = stack.size() - 1; i > -1;  i--) {
				MethodInfo mi = stack.get(i);
				if (mi.suspendType == MethodDatabase.SUSPEND_NORMAL) {
					break;
				}
				mi.suspendType = MethodDatabase.SUSPEND_NORMAL;
				ConcurrentHashMap<String, Object> omis = SUSPEND_INFO_RESULTS.get(mi.owner);
				ConcurrentHashMap<String, Object> mis = omis;
				if (omis == null) {
					omis = SUSPEND_INFO_RESULTS.putIfAbsent(mi.owner, mis = new ConcurrentHashMap<String, Object>());
					if (omis != null) {
						mis = omis;
					}
				}
				if (db != null && db.isDebug()) {
					mis.put(mi.method, new Object[] {new Exception().getStackTrace(), new ArrayList<MethodInfo>(stack)});
				}else {
					mis.put(mi.method, "");
				}
			}
		}
		stack.add(new MethodInfo(owner, method));
	}
	
	public static void leave() {
		if (dumping.get()) {
			return;
		}
		ArrayList<MethodInfo> stack = fetchStack();
		stack.remove(stack.size() - 1);
	}
	
	public static void markSuper(String clz, Set<String> methods, Map<String, TreeMap<String, String>> upperMarks) {
		ClassEntry ce = db.getClasses().get(clz);
		String[] itfs = ce.getInterfaces();
		if (itfs != null) {
			for (String itf : ce.getInterfaces()) {
				markSuper(itf, clz, methods, upperMarks);
			}
			String sclz = ce.getSuperName();
			if (sclz != null) {
				markSuper(sclz, clz, methods, upperMarks);
			}
		}
	}

	public static void markSuper(String parent, String child, Set<String> methods,
			Map<String, TreeMap<String, String>> upperMarks) {
		ClassEntry ice = db.getClasses().get(parent);
		if (ice != null) {
			Set<String> tmp = new HashSet<String>(methods);
			tmp.retainAll(ice.getMethods().keySet());
			if (!tmp.isEmpty()) {
				TreeMap<String, String> ms = upperMarks.get(parent);
				if (ms == null) {
					upperMarks.put(parent, ms = new TreeMap<String, String>());
				}
				for (String m : tmp) {
					ms.put(m, child);
				}
				markSuper(parent, tmp, upperMarks);
			}
		}
	}
	
	public static void dump(String path, boolean append) throws IOException {
		dumping.set(true);
		PrintWriter writer = new PrintWriter(new FileOutputStream(new File(path), append));
		writer.printf("############Generated By Nginx-Clojure SuspendMethodTracer %1$tY-%1$tm-%1$td ##############\r\n", new Date());
		
		Map<String, TreeMap<String, String>> upperMarks = new TreeMap<String, TreeMap<String, String>>();
		
		for (Entry<String, ConcurrentHashMap<String,Object>> en : new TreeMap<String, ConcurrentHashMap<String,Object>>(SUSPEND_INFO_RESULTS).entrySet()) {
			writer.printf("lazyclass:%s\r\n", en.getKey());
			markSuper(en.getKey(), en.getValue().keySet(), upperMarks);
			for (Entry<String, Object> me : new TreeMap<String, Object>(en.getValue()).entrySet()) {
				writer.printf("  %s:normal\r\n", me.getKey());
				
				if (db != null && db.isDebug() && "" != me.getValue()) {
					writer.printf("#from trace:---------------------------------------\r\n");
					Object[] dinfo = (Object[])me.getValue();
					for (StackTraceElement se : (StackTraceElement[])dinfo[0]) {
						writer.printf("####%s.%s(%s:%s)\r\n", se.getClassName(), se.getMethodName(), se.getFileName(), se.getLineNumber());
					}
					for (MethodInfo mi : (List<MethodInfo>) dinfo[1]) {
						writer.printf("#--->%s.%s\r\n", mi.owner, mi.method);
					}
				}
			}
			writer.printf("\r\n");
		}
		
		for (Entry<String, TreeMap<String, String>> umen : upperMarks.entrySet()) {
			writer.printf("lazyclass:%s\r\n", umen.getKey());
			for (Entry<String, String> me : umen.getValue().entrySet()) {
				writer.printf("#mark from sub %s\r\n", me.getValue());
				writer.printf("  %s:just_mark\r\n", me.getKey());
			}
			writer.printf("\r\n");
		}
		
		writer.close();
	}

}
