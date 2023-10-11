package com.fangzhouwang.Server;

/**
 * @Author Fangzhou Wang
 * @Date 2023/10/5 22:57
 **/
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.fangzhouwang.Remote.ClientInterface;
import com.fangzhouwang.Remote.TicTacToeInterface;

import javax.swing.*;
import javax.swing.Timer;

public class TicTacToeServer extends UnicastRemoteObject implements TicTacToeInterface {
    private volatile ConcurrentHashMap<String, Player> players = new ConcurrentHashMap<>();

    private volatile List<Player> playerRanks = Collections.synchronizedList(new ArrayList<>());
    private volatile Map<String, Game> playerToGameMap = new ConcurrentHashMap<>(); // 新增的Map
    private volatile Queue<Player> waitingPlayers = new LinkedList<>();
    private static Map<String, Integer> clientStatusMap = new ConcurrentHashMap<>();


    protected TicTacToeServer() throws RemoteException {
        super();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        executor.scheduleAtFixedRate(() -> {
            for (Map.Entry<String, Player> entry : players.entrySet()) {
                Player player = entry.getValue();
                try {
                    boolean alive = player.getClient().isAlive();
                    Game game = playerToGameMap.get(player.getName());
                    if (alive) {
                        clientStatusMap.put(player.getName(), 0);
                        if (game != null) {
                            game.setPauseFlag(false);
                        }
                    } else {
                        clientStatusMap.compute(player.getName(), (key, val) -> (val == null ? 1 : val + 1));
                        if (waitingPlayers.contains(player)) {
                            quitSystem(player.getName());
                            continue;  // Skip the rest of the loop for this player
                        }
                        if (clientStatusMap.get(player.getName()) <= 30) {
                            if (game != null) {
                                game.setPauseFlag(true);
                            }
                        } else {
                            game.setPauseFlag(false);
//                            FSystem(player.getName());
                            forceQuitSystem(player.getName());
                            clientStatusMap.remove(player.getName());
                        }
                    }
                } catch (RemoteException e) {
                    clientStatusMap.compute(player.getName(), (key, val) -> (val == null ? 1 : val + 1));
                    Game game = playerToGameMap.get(player.getName());
                    clientStatusMap.compute(player.getName(), (key, val) -> (val == null ? 1 : val + 1));
                    if (waitingPlayers.contains(player)) {
                        try {
                            quitSystem(player.getName());
                        } catch (RemoteException ex) {
                            throw new RuntimeException(ex);
                        }
                        continue;  // Skip the rest of the loop for this player
                    }
                    if (clientStatusMap.get(player.getName()) <= 30) {
                        if (game != null) {
                            game.setPauseFlag(true);  // 暂停游戏
                        }
                    } else {
                        try {
                            game.setPauseFlag(false);
//                            quitSystem(player.getName());
                            forceQuitSystem(player.getName());
                        } catch (RemoteException ex) {
                            throw new RuntimeException(ex);
                        }
                        clientStatusMap.remove(player.getName());  // 可选：从map中移除该客户端
                    }
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }


    @Override
    public String registerPlayer(String username, ClientInterface clientRef) throws RemoteException {
        if (players.containsKey(username)) {
        throw new RemoteException("User Name Exist！");
    }

        Player newPlayer = new Player(username, clientRef);
        players.put(username, newPlayer);
//        clientStatusMap.put(username,0);
        // 将新玩家加入到匹配队列中
        waitingPlayers.add(newPlayer);
        synchronized (playerRanks) {
            boolean found = false;
            for (int i = 0; i < playerRanks.size(); i++) {
                Player existingPlayer = playerRanks.get(i);
                if (existingPlayer.getName().equals(newPlayer.getName())) {
                    newPlayer.setScore(existingPlayer.getScore());
                    playerRanks.remove(i);
                    found = true;
                    break;
                }
            }
            playerRanks.add(newPlayer);
            if (found) {
                // Optionally, you can re-sort the playerRanks list if needed
            }
        }



        matchPlayers();
        updatePlayerRanks();
        return username;
    }
    private void matchPlayers() throws RemoteException {
        if (waitingPlayers.size() >= 2) {  // 当队列中至少有两个玩家时进行匹配
            Player player1 = waitingPlayers.poll();
            Player player2 = waitingPlayers.poll();

            // 创建一个新的游戏实例
            Game game = new Game(player1, player2);
            playerToGameMap.put(player1.getName(), game);
            playerToGameMap.put(player2.getName(), game);

            // 通知两个玩家游戏已经开始
            player1.getClient().updateChat("You have been matched with " + player2.getName() + ". Game starts now!");
            player2.getClient().updateChat("You have been matched with " + player1.getName() + ". Game starts now!");
        }
    }




    public String makeMove(String username, int row, int col) throws RemoteException {
        Game game = playerToGameMap.get(username);
        if (game != null) {
            game.makeMove(username, row, col);
            String result =  game.getStatus();
            return result;
        }
        return "Player not in game!";
    }


    @Override
    public void sendMessage(String username, String message) throws RemoteException {
        Game game = playerToGameMap.get(username);
        if (game != null) {
            game.addChatMessage("Rank#"+getRank(username)+" "+username + ": " + message);
        }
    }

    @Override
    public List<String> getChatMessages(String username) throws RemoteException {
        Game game = playerToGameMap.get(username);
        if (game != null) {
            return game.getChatMessages();
        }
        return null;
    }

    @Override
    public synchronized String quitSystem(String username) throws RemoteException {
        players.remove(username);
//        synchronized (playerRanks) {
//            playerRanks.removeIf(player -> player.getName().equals(username));
//        }
        // Logic to handle player quitting
        Game game = playerToGameMap.get(username);
        if (game != null) {
            if(game.getStatus().equals("In Progress")) {
                game.makeOpponentPlayerWin(username);
                game.checkStatus();
            }
            playerToGameMap.remove(username);
        }
        synchronized (waitingPlayers) {
            Iterator<Player> iterator = waitingPlayers.iterator();
            while (iterator.hasNext()) {
                Player player = iterator.next();
                if (player.getName().equals(username)) {
                    iterator.remove();
                    break;  // Assuming each username is unique, so we break after finding the first match
                }
            }
        }
        synchronized (clientStatusMap){
            clientStatusMap.remove(username);
        }
        return "Success";
    }
    public String forceQuitSystem(String username) throws RemoteException {
        players.remove(username);
//        synchronized (playerRanks) {
//            playerRanks.removeIf(player -> player.getName().equals(username));
//        }
        // Logic to handle player quitting
        Game game = playerToGameMap.get(username);
        if (game != null) {

            if(game.getStatus().equals("In Progress")) {
//                game.makeOpponentPlayerWin(username);
                game.makeDraw();
                game.checkStatus();
            }
            playerToGameMap.remove(username);
        }
        synchronized (waitingPlayers) {
            waitingPlayers.removeIf(player -> player.getName().equals(username));
        }
        synchronized (clientStatusMap){
            clientStatusMap.remove(username);
        }
        return "Success";
    }
    public synchronized void quitGame(String username){
        Game game = playerToGameMap.get(username);
        if (game != null) {
            // 销毁与该用户相关的游戏实例
            // 这里你可以添加其他的清理逻辑，例如通知另一个玩家
            playerToGameMap.remove(username);
        }
    }

    @Override
    public synchronized String startNewGame(String username) throws RemoteException {
        // 检查玩家是否已经在游戏中
        Game currentGame = playerToGameMap.get(username);
        if (currentGame != null && "In Progress".equals(currentGame.getStatus())) {
            return "You already in a game!";
        }else{
            quitGame(username);
        }
        Player player = players.get(username);
        if (player == null) {
            return "No User";
        }


        waitingPlayers.add(player);


        matchPlayers();

        return "Wating...";
    }


    @Override
    public int getRank(String username) throws RemoteException {
        updatePlayerRanks();
        Player player = players.get(username);
        if (player == null) {
            throw new RemoteException("Player not found!");
        }

        int rank = playerRanks.indexOf(player) + 1;
        if (rank == 0) { // player not in the ranking list
            return -1; // or any other indicator of an error or unranked status
        }
        return rank;
    }


    @Override
    public void rejoinGame(String username,ClientInterface client) throws RemoteException {

        Player player = players.get(username);
        player.setClient(client);
        if (player == null) {
            throw new RemoteException("No User");
        }

        Game game = playerToGameMap.get(username);
        if (game == null) {
            throw new RemoteException("Not in a Game");
        }
        game.updateClientsBoard();
    }


    @Override
    public String updateGameStatue(String username) throws RemoteException {
        Game game = playerToGameMap.get(username);
        if (game != null) {
//            String status = game.getStatus();
            return game.getStatus();
        } else {
            return "You are not in a game!";
//            throw new RemoteException("You are not in a game！");
        }
    }

    @Override
    public String updateNextMove(String username) throws RemoteException {
        Game game = playerToGameMap.get(username);


        if (game == null) {
            return "Not In a Match";
        }


        String status = game.getStatus();
        if (!"In Progress".equals(status)) {
            return "Game is Over";
        }


        Player currentPlayer = game.getCurrentPlayer();
        char currentSymbol = (currentPlayer == game.getPlayer1()) ? game.getPlayer1Symbol() : game.getPlayer2Symbol();
        return "Rank#"+getRank(currentPlayer.getName())+" "+currentPlayer.getName() + " (" + currentSymbol +
                ") should make the next move.";
    }


    @Override
    public String getStepTime(String username) throws RemoteException {
        Game game = playerToGameMap.get(username);
        if(game != null){
            return String.valueOf(game.getRemainingStepTime());
        }else {
            return  "99";
        }
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }

    @Override
    public boolean isLogIn(String username) throws RemoteException {
        Integer status = clientStatusMap.get(username);
        if (status != null && status < 28 && status > 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isPaused(String username) throws RemoteException {

        Game game = playerToGameMap.get(username);
        if (game!=null) {
            return game.getPauseFlag();
        }else {
            return false;
        }
    }

    @Override
    public boolean isDuplicate(String username) throws RemoteException {
        if(clientStatusMap.containsKey(username)){
            return true;
        }
        else {
            return false;
        }

    }


    private synchronized void updatePlayerRanks() {
        playerRanks.sort((player1, player2) -> Integer.compare(player2.getScore(), player1.getScore()));  // 从高到低排序
    }



    public static void main(String[] args) {
        try {
            int port = Integer.parseInt(args[1]);
            String serverIp = args[0];

            TicTacToeServer server = new TicTacToeServer();

            LocateRegistry.createRegistry(port);


            Naming.bind("rmi://" + serverIp + ":" + port + "/TicTacToeServer", server);

            System.out.println("TicTacToeServer is ready at " + serverIp + ":" + port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}

