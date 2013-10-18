package tipl.util;

import java.util.LinkedList;
import java.util.List;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

/**
 * just a simple hazelcast client for listening for new connections, offers two commands to search for merlin and tomcat beamline nodes
 * @author mader
 *
 */
public class HZClient {
	public final ClientConfig config= new ClientConfig();
	protected HazelcastInstance hazelcastClient;
	
	public void startHazel() {
		hazelcastClient = HazelcastClient.newHazelcastClient(config);
	}
	public static Config setupCluster(List<String> nodeList) {
		Config cfg = new Config();
		        
		NetworkConfig network = cfg.getNetworkConfig();
		network.setPortAutoIncrement(true);
		JoinConfig join = network.getJoin();
		join.getMulticastConfig().setEnabled(false);
		join.getTcpIpConfig().setEnabled(true);
		join.getTcpIpConfig().setMembers(nodeList);
		join.getTcpIpConfig().setRequiredMember("merlinc60.psi.ch");
		network.setJoin(join);
		cfg.setNetworkConfig(network);
		
		
		MapConfig mapCfg = new MapConfig();
		
		mapCfg.setName("testMap");
		mapCfg.setBackupCount(1);
		mapCfg.getMaxSizeConfig().setSize(10000);
		mapCfg.setTimeToLiveSeconds(300);
		cfg.addMapConfig(mapCfg);
		System.out.println(cfg.toString());
		return cfg;
	}
	public static HazelcastInstance createCluster(List<String> nodeList) {
		return Hazelcast.newHazelcastInstance(setupCluster(nodeList));
	}
	public void startCluster(List<String> nodeList) {
		hazelcastClient=createCluster(nodeList);
		
	}
	public static List<String> getMerlin() {
		List<String> merlinAddresses=new LinkedList<String>();
		for(int i=1;i<=30;i++) merlinAddresses.add(String.format("merlinc%02d.psi.ch",i));
		merlinAddresses.add(String.format("merlinc%02d.psi.ch",60));
		for(int i=1;i<=3;i++) merlinAddresses.add(String.format("merlinl%02d.psi.ch",i));
		
		return merlinAddresses;
	}
	public static List<String> getTomcat() {
		List<String> merlinAddresses=new LinkedList<String>();
		for(int i=0;i<=6;i++) merlinAddresses.add(String.format("x02da-cn-%02d",i));
		return merlinAddresses;
	}
	public static void addMerlin(ClientConfig inConfig) {
		
		String outStr="";
		for(String cAdd : getMerlin()) outStr+=", "+cAdd;
		System.out.println("All addresses:"+outStr);
		inConfig.setAddresses(getMerlin());

	}
	public static void addTOMCAT(ClientConfig inConfig) {
		inConfig.setAddresses(getTomcat());

	}
	public static void main(String[] args) {
        HZClient cclient=new HZClient();
       
		cclient.startCluster(HZClient.getMerlin());
    }
	
}