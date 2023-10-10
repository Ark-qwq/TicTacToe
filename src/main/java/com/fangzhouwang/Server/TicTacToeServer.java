package com.fangzhouwang.Server;

/**
 * @Author Fangzhou Wang
 * @Date 2023/10/5 22:57
 **/
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
                        // 如果玩家重新连接，恢复游戏状态
                        if (game != null) {
                            game.setPauseFlag(false);  // 恢复游戏
                        }
                    } else {
                        clientStatusMap.compute(player.getName(), (key, val) -> (val == null ? 1 : val + 1));
                        if (clientStatusMap.get(player.getName()) <= 30) {
                            if (game != null) {
                                game.setPauseFlag(true);  // 暂停游戏
                            }
                        } else {
                            game.setPauseFlag(false);
//                            FSystem(player.getName());
                            forceQuitSystem(player.getName());
                            //TODO:增加中断逻辑

                            clientStatusMap.remove(player.getName());  // 可选：从map中移除该客户端
                        }
                    }
                } catch (RemoteException e) {
                    clientStatusMap.compute(player.getName(), (key, val) -> (val == null ? 1 : val + 1));
                    Game game = playerToGameMap.get(player.getName());
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
        throw new RemoteException("用户名已存在！");
    }

        Player newPlayer = new Player(username, clientRef);
        players.put(username, newPlayer);

        // 将新玩家加入到匹配队列中
        waitingPlayers.add(newPlayer);
        synchronized (playerRanks) {
//            playerRanks.add(newPlayer);
            if (!playerRanks.contains(newPlayer)) {
                playerRanks.add(newPlayer);
            }
        }


        // 尝试进行匹配
        matchPlayers();
        updatePlayerRanks();
        return username;
    }
    private void matchPlayers() throws RemoteException {
        while (waitingPlayers.size() >= 2) {  // 当队列中至少有两个玩家时进行匹配
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
        // 这里只是一个示例，你可能需要根据实际的需求来获取聊天消息
        Game game = playerToGameMap.get(username);  // 获取第一个游戏的聊天消息
        if (game != null) {
            return game.getChatMessages();
        }
        return null;
    }

    @Override
    public String quitSystem(String username) throws RemoteException {
        players.remove(username);
//        synchronized (playerRanks) {
//            playerRanks.removeIf(player -> player.getName().equals(username));
//        }
        // Logic to handle player quitting
        Game game = playerToGameMap.get(username);
        if (game != null) {
            // 销毁与该用户相关的游戏实例
            // 这里你可以添加其他的清理逻quit辑，例如通知另一个玩家

            if(game.getStatus().equals("In Progress")) {
                game.makeOpponentPlayerWin(username);
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
    public String forceQuitSystem(String username) throws RemoteException {
        players.remove(username);
//        synchronized (playerRanks) {
//            playerRanks.removeIf(player -> player.getName().equals(username));
//        }
        // Logic to handle player quitting
        Game game = playerToGameMap.get(username);
        if (game != null) {
            // 销毁与该用户相关的游戏实例
            // 这里你可以添加其他的清理逻quit辑，例如通知另一个玩家

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

        // 将当前玩家添加到等待队列中
        waitingPlayers.add(player);

        // 尝试匹配玩家
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
            // 根据status进行后续操作，例如通知客户端等
        } else {
            return "You are not in a game!";
//            throw new RemoteException("You are not in a game！");
        }
    }

    @Override
    public String updateNextMove(String username) throws RemoteException {
        Game game = playerToGameMap.get(username);

        // 检查玩家是否在游戏中
        if (game == null) {
            return "Not In a Match";
        }

        // 检查游戏状态
        String status = game.getStatus();
        if (!"In Progress".equals(status)) {
            return "Game is Over";
        }

        // 返回当前应该是谁进行下一步
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
        if (status != null && status < 28) {
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


    private synchronized void updatePlayerRanks() {
        playerRanks.sort((player1, player2) -> Integer.compare(player2.getScore(), player1.getScore()));  // 从高到低排序
    }



    public static void main(String[] args) {
        try {
            TicTacToeServer server = new TicTacToeServer();
            Registry registry = LocateRegistry.createRegistry(1099);

            // 将远程对象的stub绑定到注册表中
            registry.bind("TicTacToeServer", server);
//            java.rmi.Naming.rebind("TicTacToeServer", server);
            System.out.println("TicTacToeServer is ready");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

