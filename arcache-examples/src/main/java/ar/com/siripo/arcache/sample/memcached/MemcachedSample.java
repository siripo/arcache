package ar.com.siripo.arcache.sample.memcached;

import java.net.InetSocketAddress;
import java.util.HashMap;

import net.spy.memcached.MemcachedClient;

public class MemcachedSample {
	
	public static void main( String[] args ) throws Exception
    {
		HashMap<String, String> hm=new HashMap<String, String>();
		hm.put("k1","val1");
		hm.put("k2","val2");
		
        MemcachedClient mcli=new MemcachedClient(new InetSocketAddress("localhost",11211));
        
        mcli.set("lakey",3600,hm).get();
        
        Object theobj=mcli.asyncGet("lakey").get();
        System.out.println("ret: "+theobj);
        
        mcli.shutdown();
    }

}
