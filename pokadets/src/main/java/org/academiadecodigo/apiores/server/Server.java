package org.academiadecodigo.apiores.server;

import org.academiadecodigo.apiores.controllers.PokadetController;
import org.academiadecodigo.bootcamp.Prompt;
import org.academiadecodigo.bootcamp.scanners.menu.MenuInputScanner;
import org.academiadecodigo.bootcamp.scanners.string.StringInputScanner;
import org.academiadecodigo.bootcamp.scanners.string.StringSetInputScanner;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    public static final int PORT = 8000;

    private ServerSocket serverSocket = null;
    private PokadetController pokadetController;
    private HashMap<Integer, Integer> playerOption; //currentPlayer , playerOptionAbility
    private HashMap<Socket, Integer> socketMap; //clientSocker , currentPlayer
    private int playerId = 1;
    public Server() {

        socketMap = new HashMap<>();
        playerOption = new HashMap<>();
    }


    public void setPokadetController(PokadetController pokadetController) {
        this.pokadetController = pokadetController;
    }

    public void init() {
        try {

            serverSocket = new ServerSocket(PORT);

            listen();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void serve(Socket clientSocket) {

        PrintStream out = null;
        InputStream in = null;

        MenuInputScanner menuInputScanner = new MenuInputScanner(pokadetController.getPokadetOptions());


        try {
            in = new DataInputStream(clientSocket.getInputStream());
            out = new PrintStream(clientSocket.getOutputStream(), true);
            Prompt prompt = new Prompt(in, out);

            //Player Pick
            int playerPick = prompt.getUserInput(menuInputScanner);

            playerOption.put(socketMap.get(clientSocket), playerPick);

            if(playerOption.size()<2){
                System.out.println("Waiting for player 2");
            }
            while(playerOption.size() <2){
                System.out.print("");
            }
            System.out.println(Thread.currentThread().getName());
            setPlayers();

            while (!pokadetController.isGameOver()) {
                synchronized (this) {
                    notifyAll();
                    wait();
                    MenuInputScanner abilities = new MenuInputScanner(pokadetController.getAbilitiesOptions(socketMap.get(clientSocket)));

                    int abilityPick = prompt.getUserInput(abilities);

                    pokadetController.init(socketMap.get(clientSocket), abilityPick);

                    System.out.println(abilityPick);
                    notifyAll();
                }
            }

            //gameOver

            playerId = 1;

            MenuInputScanner finalMenu = new MenuInputScanner(new String[]{"Yes","No"});

            menuInputScanner.setMessage(pokadetController.getWinner().getName() + " won! Restart?");

            prompt.getUserInput(finalMenu);



        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            out.close();
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void listen() {


        ExecutorService executorService = Executors.newFixedThreadPool(2);
        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();

                socketMap.put(clientSocket, playerId);
                playerId++;


                executorService.submit(new serverThread(clientSocket));
                System.out.println("New Player Connected");


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void setPlayers() {

        pokadetController.addPokadets(playerOption.get(1),playerOption.get(2));

    }


    private class serverThread implements Runnable {
        private Socket playerSocket;

        public serverThread(Socket playerSocket) {
            this.playerSocket = playerSocket;
        }

        @Override
        public void run() {
            serve(playerSocket);
        }
    }


}
