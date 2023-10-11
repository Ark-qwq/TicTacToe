package com.fangzhouwang.Client;

/**
 * @Author Fangzhou Wang
 * @Date 2023/10/5 22:56
 **/
import com.fangzhouwang.Remote.TicTacToeInterface;
import com.fangzhouwang.Remote.ClientInterface;

import javax.swing.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.PublicKey;
import java.util.List;

public class TicTacToeClient extends UnicastRemoteObject implements ClientInterface {
    private TicTacToeInterface server;
    private String username;
    private Timer moveTimer;
    private JLabel timerLabel;
    private ClientGUI gui;



    public TicTacToeClient(String username, String serverIp, int serverPort) throws RemoteException {
        super();
        try {
            server = (TicTacToeInterface) Naming.lookup("rmi://" + serverIp + ":" + serverPort + "/TicTacToeServer");
            this.username = username;
//            Naming.rebind("rmi://localhost/Client" + username, this);

            // Initialize GUI components here
            moveTimer = new Timer(1000, e -> updateTimer());
            timerLabel = new JLabel("20");
            // Add timerLabel to the GUI

        } catch (Exception e) {
            System.out.println("Server Not Found!");
            System.exit(1);
            e.printStackTrace();
        }
    }


    private void updateTimer() {
        int timeLeft = Integer.parseInt(timerLabel.getText());
        if (timeLeft == 0) {
            moveTimer.stop();
        } else {
            timerLabel.setText(String.valueOf(timeLeft - 1));
        }
    }

    public String makeMove(int row, int col) {
        try {
            server.updateGameStatue(username);
            server.makeMove(username, row, col);
            return  server.updateGameStatue(username);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setGui(ClientGUI gui) {
        this.gui = gui;
    }

    @Override
    public void updateBoard(char[][] board ) throws RemoteException {
        gui.updateBoardButton(board);
    }

    @Override
    public void updateChat(String message) throws RemoteException {
        gui.appendChatArea(message);
    }

    @Override
    public void updateGameResult(String message) throws RemoteException {
//        JOptionPane.showMessageDialog(null, message, "游戏结果", JOptionPane.INFORMATION_MESSAGE);
        gui.appendChatArea(message);
    }

    @Override
    public boolean isAlive() throws RemoteException {
        return true;
    }

    public boolean checkServerStatue() {
        try {
            return server.isAlive();
        } catch (RemoteException e) {
            return false;
        }
    }



    public String updateGameStatue(String username) throws RemoteException {
        return  server.updateGameStatue(username);
    }

    public void registerToServer() {
        try {
            if (server.isDuplicate(username)) {
                // 弹出提示框
                gui.showDuplicate();
            } else {
                server.registerPlayer(username, this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void sendChatMessage(String username,String message) throws RemoteException {
        server.sendMessage(username,message);

    }

    public String updateStepTime(String username) throws RemoteException {
     return server.getStepTime(username);
    }
    public String quitSystem(String username) throws RemoteException {
        return server.quitSystem(username);
    }

    public String startNewGame(String username) throws RemoteException {
        return server.startNewGame(username);
    }
    public String updateNextMove() throws RemoteException {
        return server.updateNextMove(username);
    }
    public void connectToServer() {
        try {
            if (server.isLogIn(username)) {
                server.rejoinGame(username, this);
                List<String> chatMessages = server.getChatMessages(username);
                if (chatMessages != null) {
                    for (String message : chatMessages) {
                        gui.appendChatArea(message);
                    }
                }
            } else {
                registerToServer();
            }
        } catch (RemoteException e) {
            gui.showConnectionError();
        }
    }



    public static void main(String[] args) throws RemoteException {
        if (args.length < 3) {
            System.out.println("Please provide a username, server IP, and server port as command-line arguments.");
            return;
        }
        String username = args[0];
        String serverIp = args[1];
        int serverPort = Integer.parseInt(args[2]);

        TicTacToeClient client = new TicTacToeClient(username, serverIp, serverPort);
        ClientGUI gui = new ClientGUI(username, client);
        client.setGui(gui);
        client.connectToServer();
    }



    public boolean isPaused(String username) throws RemoteException {
        return server.isPaused(username);
    }
}