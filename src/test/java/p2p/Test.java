package p2p;

public class Test {

	public static void main(String[] args) {
	/*	System.out.println((int)'\r');
		System.out.println((int)'\n');*/
		String result="{\"msg\":\"exist\",\"ips\":\"zjwTest:180.168.91.190:60993_zjwTest:180.168.91.190:3201\"}";
		String ipSp="\"ips\":";
		int ipsIndex = result.indexOf(ipSp);
		if(ipsIndex!=-1){
			String ips=result.substring(ipsIndex+ipSp.length());
			ips=ips.substring(1, ips.length()-2);
			String[] clients = ips.split("_");
			if(clients[0].trim().length()>0&&clients[1].trim().length()>0){
				String[] ups = clients[0].split(":");
				if(ups.length==3){
					System.out.println("user:"+ups[0]);
					System.out.println("ip:"+ups[1]);
					System.out.println("port:"+ups[2]);
				}
			}
			System.out.println(ips);
			
		}
	}

}
