package org.mbmg.tcp.server;

public class Server { 
	private final int port;
    private final String graphiteHost;
    private final int graphitePort;

    public Server(int port, String graphiteHost, int graphitePort) {
        this.graphiteHost = graphiteHost;
        this.graphitePort = graphitePort;
        this.port = port;
    }
	
	public void run() {
		new ServerHandler(port, graphiteHost, graphitePort);
	}
		
	public static void main(String[] args) {
		int port;
        int graphitePort;
        String graphiteHost;
        // Retrieve port specified in Sytem properties. Used port 6001 as default
        port = Integer.parseInt(System.getProperty("org.mbmg.tcp.server.port","6002"));
        graphiteHost = System.getProperty("org.mbmg.graphite.server.host","localhost");
        graphitePort = Integer.parseInt(System.getProperty("org.mbmg.graphite.server.port","2003"));
        new Server(port, graphiteHost, graphitePort).run();
	}
}