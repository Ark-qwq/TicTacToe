package com.fangzhouwang.Server;

//package com.fangzhouwang;

import java.awt.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @Author Fangzhou Wang
 * @Date 2023/9/28 15:12
 **/
public class Game {
    private final Player player1;
    private final Player player2;
    private final char[][] board;
    private List<String> chatMessages = new ArrayList<>();
    private final char player1Symbol;
    private final char player2Symbol;
    private Player currentPlayer;
    private static final int TOTAL_STEP_TIME = 20; // 总时间为30秒
    private int remainingStepTime = TOTAL_STEP_TIME; // 剩余时间初始化为30秒
    private boolean pauseFlag = false;

    private String status;

    private ScheduledExecutorService stepExecutor;


    public Game(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        board = new char[3][3];

        // 随机分配符号
        Random random = new Random();
        if (random.nextBoolean()) {
            player1Symbol = 'X';
            player2Symbol = 'O';
            currentPlayer = player1;
        } else {
            player1Symbol = 'O';
            player2Symbol = 'X';
            currentPlayer = player2;
        }

        status= "In Progress";
        stepExecutor = Executors.newSingleThreadScheduledExecutor();
        stepExecutor.scheduleAtFixedRate(() -> {
            try {
                decreaseStepTime();
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        }, 0, 1, TimeUnit.SECONDS); // 每秒执行一次
    }
    private synchronized void decreaseStepTime() throws RemoteException {
        if(pauseFlag){return;}
        if (!"In Progress".equals(status)) {
            // 如果游戏状态不是"In Progress"，则重置步数时间并返回时间为99
            remainingStepTime = 99;
        } else {
            if (remainingStepTime > 0) {
                remainingStepTime--;
            } else {
                randomMove();
            }
        }
    }
    public synchronized void randomMove() throws RemoteException {
        if(pauseFlag){return;}
        // 获取所有尚未被占用的位置
        List<Point> availableMoves = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == '\0') {  // 如果位置为空
                    availableMoves.add(new Point(i, j));
                }
            }
        }

        // 如果没有可用的位置，直接返回
        if (availableMoves.isEmpty()) {
            return;
        }

        // 随机选择一个位置
        Random random = new Random();
        Point randomMove = availableMoves.get(random.nextInt(availableMoves.size()));

        // 为当前玩家放置一个符号
        char currentSymbol = (currentPlayer == player1) ? player1Symbol : player2Symbol;
        board[randomMove.x][randomMove.y] = currentSymbol;
        remainingStepTime = TOTAL_STEP_TIME;
        updateClientsBoard();
        // 更新游戏状态
        status = checkStatus();

        // 切换玩家
        switchPlayer();
    }


    public synchronized int getRemainingStepTime() {
        return remainingStepTime;
    }

    public synchronized String makeMove(String username, int row, int col) throws RemoteException {
        // 验证username是否与currentPlayer匹配
        if(pauseFlag){return "";}
        if(!(status.equals( "In Progress"))){
            return "Game is Over";
        }
        if (!currentPlayer.getName().equals(username)) {
            return "It's not your turn!";
        }

        char currentSymbol = (currentPlayer == player1) ? player1Symbol : player2Symbol;
        if (board[row][col] == '\0') {
            board[row][col] = currentSymbol;
            updateClientsBoard();
        } else {
            return "Invalid move!";
        }

        remainingStepTime = TOTAL_STEP_TIME;
        status = checkStatus();
        switchPlayer();
        return "Move made by " + currentPlayer.getName();
    }
    public String checkStatus() throws RemoteException {
        if (isWinner()) {
            if (currentPlayer == player1) {
                player1.addScore(5);
                player2.subtractScore(5);
//                player1.getClient().updateGameResult("GM: Win");
//                player2.getClient().updateGameResult("GM: Lose");
                return "Player " + player1.getName() + " wins!";
            } else {
                player2.addScore(5);
                player1.subtractScore(5);
//                player2.getClient().updateGameResult("GM: Win");
//                player1.getClient().updateGameResult("GM: Lose");
                return "Player " + player2.getName() + " wins!";
            }
        }
        if (isDraw()) {
            player1.addScore(2);
            player2.addScore(2);
//            player1.getClient().updateGameResult("Draw");
//            player1.getClient().updateGameResult("Draw");
            return "It's a draw!";
        }

        return "In Progress";
    }



    private synchronized boolean isDraw() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == '\0') {
                    return false;
                }
            }
        }
        return true;
    }

    private synchronized boolean isWinner() {
        char currentSymbol = (currentPlayer == player1) ? player1Symbol : player2Symbol;

        // 检查每一行
        for (int i = 0; i < 3; i++) {
            if (board[i][0] == currentSymbol && board[i][1] == currentSymbol && board[i][2] == currentSymbol) {
                return true;
            }
        }
        // 检查每一列
        for (int i = 0; i < 3; i++) {
            if (board[0][i] == currentSymbol && board[1][i] == currentSymbol && board[2][i] == currentSymbol) {
                return true;
            }
        }
        // 检查对角线
        if (board[0][0] == currentSymbol && board[1][1] == currentSymbol && board[2][2] == currentSymbol) {
            return true;
        }
        if (board[0][2] == currentSymbol && board[1][1] == currentSymbol && board[2][0] == currentSymbol) {
            return true;
        }

        return false;
    }

    private void switchPlayer() {
        if(pauseFlag){return;}
        currentPlayer = (currentPlayer == player1) ? player2 : player1;
    }
    public void addChatMessage(String message) throws RemoteException {
        chatMessages.add(message);
        player1.getClient().updateChat(message);
        player2.getClient().updateChat(message);
    }

    public List<String> getChatMessages() {
        return chatMessages;
    }
    public void updateClientsBoard() throws RemoteException {
        if(pauseFlag){return;}
        player1.getClient().updateBoard(board);
        player2.getClient().updateBoard(board);
    }

    public String getStatus() {
        return status;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public Player getPlayer1() {
        return player1;
    }
    public Player getPlayer2() {
        return player2;
    }
    public char getPlayer1Symbol(){
        return player1Symbol;
    }
    public char getPlayer2Symbol(){
        return player2Symbol;
    }
    public void makeOpponentPlayerWin(String quittingPlayerName) {
        Player opponent;
        if (player1.getName().equals(quittingPlayerName)) {
            opponent = player2;
        } else {
            opponent = player1;
        }

        status = "Player " + opponent.getName() + " wins due to opponent's forfeit!";
    }
    public void makeDraw(){
        status = "It's a draw!";
    }

    public void stopForWait(){
        status = "Pause";
    }
    public void setPauseFlag(Boolean flag){
        this.pauseFlag = flag;
    }
    public boolean getPauseFlag(){
        return this.pauseFlag;
    }


    public List<String> getAllChatHistory(){
        return chatMessages;
    }
}

