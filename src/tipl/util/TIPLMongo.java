/**
 * 
 */
package tipl.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

/**
 * An interface to MongoDB for reading and exporting experimental information will eventually become a general interface which can then be implemented for a given database type
 * 
 * @author mader
 *
 */
public class TIPLMongo {
	protected MongoClient mc;
	protected DB mdb;
	protected TIPLMongo(List<ServerAddress> seeds,String table) {
		mc=new MongoClient(seeds);
		mdb=mc.getDB(table);
	}
	@Deprecated
	static public TIPLMongo getTable(String ipAddress, int port, String table, String username, String password) {
		try {
			return getTable(Arrays.asList(new ServerAddress(ipAddress,port)),table,username,password);
		} catch(Exception e) {
			throw new IllegalArgumentException("Cannot Connect to Client "+ipAddress +", "+e);
		}
	}
	static public TIPLMongo getTable(List<ServerAddress> hostList, String table, String username, String password) {
		TIPLMongo outObj=new TIPLMongo(hostList,table);
		
		boolean auth = outObj.mdb.authenticate(username,password.toCharArray());
		if (!auth) throw new IllegalArgumentException("Mongo could not be authenticated");
		return outObj;
	}
	
	public DBCollection getCollection(String name) {
		return mdb.getCollection(name);
	}
	static public interface ITIPLUsage {
		public void registerPlugin(String pluginName, String args);
		public void registerImage(String imageName, String dim, String info);
	}
	final static public class TIPLUsage implements ITIPLUsage {
		static final private String usIp="ds053307.mongolab.com";
		static final private int usPort=53307;
		
		static final private String tmIp="madbox.psi.ch";
		static final private int tmPort=53307;
		
		static final private String usTable="tipl_usage";
		static final private String usUN="27032014"; //"tipl_app";
		static final private String usPA="tiply";  //"cRyf7TUQjkBQio6HmN";
		
		private TIPLMongo coreMongo;
		private DBCollection imageList;
		private DBCollection plugList;
		
		final private String username;
		final private String address;
		final private String hostname;
		static public ITIPLUsage getTIPLUsage() {
			// check to see if the address can be resolved
			try {
				ServerAddress mongoLab=new ServerAddress(usIp,usPort);
			} catch (Exception e) {
				if (TIPLGlobal.isLocalOk()) return TIPLGlobal.isLocalUsage(22515);
				throw new IllegalArgumentException("TIPL Requires Access to network in order to run and download required packages");
				
			}
			// If it can be resolved but not connected (firewall), then continue in local mode
			Socket s = new Socket();
			try {
				s.connect(new InetSocketAddress(usIp, usPort), 1000);
				s.close();
			} catch (Exception e) {
				if (TIPLGlobal.isLocalOk()) return TIPLGlobal.isLocalUsage(22515);
				System.out.println("Operating in local mode...");
				throw new IllegalArgumentException("TIPL Requires Port-level Access to network in order to run and download required packages");
				
			}
			// Otherwise connect to it (if login fails stop)
			try {
				return new TIPLUsage();
				} 
			catch (Exception e) {
					throw new IllegalArgumentException("TIPL Requires Access to network in order to run and download required packages");
			}
			
		}
		protected TIPLUsage() {
			List<ServerAddress> mongoHosts=new ArrayList<ServerAddress>(2);
			
			try {
				mongoHosts.add(new ServerAddress(usIp,usPort));
			} catch (UnknownHostException e1) {
				System.out.println("External Network not available checking for TOMCAT Resources");
				
			}
			/*
			try {
				mongoHosts.add(new ServerAddress(tmIp,tmPort));
			} catch (UnknownHostException e1) {
				System.out.println("TOMCAT Resources not available");
			}
			*/
 
			
			coreMongo=TIPLMongo.getTable(mongoHosts, usTable,usUN,usPA);
			imageList=coreMongo.getCollection("images");
			plugList=coreMongo.getCollection("plugins");
			username=System.getProperty("user.name");
			Socket s;
			try {
				s = new Socket("google.com", 80);
				address=s.getLocalAddress().getHostAddress();
				s.close();
			} catch (Exception e ) {
				e.printStackTrace();
				throw new IllegalArgumentException("Cannot Connect to Google");
				
			}
			try {
				hostname=InetAddress.getLocalHost().getHostName();
			} catch (UnknownHostException e) {
				e.printStackTrace();
				throw new IllegalArgumentException("Cannot identify host! " + e);
				
			}
			
			
		}
		public void registerPlugin(String pluginName, String args) {
			BasicDBObject doc = new BasicDBObject("username", username).
                    append("address", address).
                    append("host", hostname).
                    append("tuname",usUN).
                    append("plugin",pluginName).
                    append("arguments", args);
			plugList.insert(doc);
		}
		
		public void registerImage(String imageName, String dim, String info) {
			BasicDBObject doc = new BasicDBObject("username", username).
                    append("address", address).
                    append("host", hostname).
                    append("tuname",usUN).
                    append("path",imageName).
                    append("dim",dim).
                    append("info", info);
			imageList.insert(doc);
		}
	}
	
	
}
