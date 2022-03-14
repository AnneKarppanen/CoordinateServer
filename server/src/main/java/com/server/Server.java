package com.server;

import com.sun.net.httpserver.*;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;

import java.io.FileInputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executors;
import java.util.Scanner;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class Server {

    public static void main(String[] args) throws Exception {
        try {
            // create the http server to port 8001 with default logger
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);
            SSLContext sslContext = coordinateServerSSLContext(args[0], args[1]);
            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            UserAuthenticator authenticator = new UserAuthenticator("/coordinates");
            CoordinateDatabase coordinateDB = CoordinateDatabase.getInstance();

            HttpContext regContext = server.createContext("/registration", new RegistrationHandler(authenticator));

            // create context that defines path for the resource, in this case "coordinates"
            HttpContext context = server.createContext("/coordinates", new CoordinatesHandler());
            context.setAuthenticator(authenticator);
            // creates a default executor
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            boolean running = true;
            Scanner scanner = new Scanner(System.in);

            while (running) {
                String userInput = scanner.nextLine();
                if (userInput.equals("/quit")) {
                    running = false;
                    server.stop(3);
                    coordinateDB.close();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static SSLContext coordinateServerSSLContext(String path, String passwd) throws Exception {
        char[] passphrase = passwd.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(path), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;

    }

}
