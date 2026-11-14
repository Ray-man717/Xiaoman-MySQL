package com.github.rayman717.Server;

public class NetworkMain {
    Thread serverThread = new Thread(new NetworkHttpThread());
    public NetworkMain() {
        serverThread.start();
    }
}
