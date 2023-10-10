package com.fangzhouwang.Client;

import com.fangzhouwang.Remote.TicTacToeInterface;
import com.fangzhouwang.Server.TicTacToeServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;
import java.util.Arrays;

/**
 * @Author Fangzhou Wang
 * @Date 2023/10/4 16:09
 **/
public class ClientGUI{

    private JFrame frame;
    private Timer timer;
    private int secondsElapsed = 17;
    private JLabel timerLabel;
    private JLabel timeDisplayLabel;
    private JTextArea chatArea;
    private String username;
    private JButton[] boardButtons = new JButton[9];
    private TicTacToeClient client;
    private boolean hasDisplayedResult = false;
    private JDialog pauseDialog;

    public ClientGUI(String username , TicTacToeClient client) throws RemoteException {
        this.username = username;
        this.client = client;
//        this.gameActionListener = gameActionListener;
        frame = new JFrame("Tic Tac Toe Game");
        frame.setSize(500, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    String response = client.quitSystem(username);
                    if ("Success".equals(response)) {
                        System.exit(0);  // 退出程序
                    } else {
                        // 可以在这里处理其他的响应，例如显示一个错误消息
                        JOptionPane.showMessageDialog(frame, "退出失败！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "连接失败", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Left Panel with Game Title, Timer, and Quit Button

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
//        leftPanel.setBackground(Color.BLACK);

        timerLabel = new JLabel("Timer");
        timerLabel.setForeground(Color.BLACK);
        timerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Distributed Tic-Tac-Toe");
        titleLabel.setForeground(Color.BLACK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        timeDisplayLabel = new JLabel(secondsElapsed + "s");
        timeDisplayLabel.setForeground(Color.BLACK);
        timeDisplayLabel.setFont(new Font("Arial", Font.BOLD, 24));
        timeDisplayLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton quitButton = new JButton("QUIT");
        quitButton.setAlignmentX(Component.CENTER_ALIGNMENT);



        leftPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        leftPanel.add(timerLabel);
        leftPanel.add(timeDisplayLabel);
        leftPanel.add(titleLabel);
        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(quitButton);

        frame.add(leftPanel, BorderLayout.WEST);

        // Center Panel with Tic Tac Toe Board
        JPanel centerPanel = new JPanel(new GridLayout(3, 3));
        centerPanel.setBorder(BorderFactory.createTitledBorder("Next: Raj (X)"));
        for (int i = 0; i < 9; i++) {
            JButton button = new JButton("");
            final int index = i;
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int x = index % 3;
                    int y = index / 3;
                    String result = client.makeMove(y, x); // Adjusted this line

                }
            });
            boardButtons[i] = button;
            centerPanel.add(button);
        }
        frame.add(centerPanel, BorderLayout.CENTER);

        // Right Panel with Player Messages, Input Field and Send Button
        JPanel rightPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();
        chatArea.setEditable(false);
//        chatArea.append("Rank#1 Tawfiq: Hi Raj!\n");
//        chatArea.append("Rank#50 Raj: Hi Tawfiq!\n");
        rightPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField inputField = new JTextField();
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = inputField.getText();
                if (!message.isEmpty()) {
                    try {
                        client.sendChatMessage(username, message);
                    } catch (RemoteException ex) {
                        throw new RuntimeException(ex);
                    }
                    inputField.setText("");  // 清空输入框
                }
            }
        });

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        rightPanel.add(inputPanel, BorderLayout.SOUTH);
        frame.add(rightPanel, BorderLayout.EAST);

        // Timer to update the elapsed time

        JButton quitButton1 = new JButton("Quit");
        JButton newGameButton = new JButton("Start a new game");

        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String response = client.quitSystem(username);
                    if ("Success".equals(response)) {
                        System.exit(0);  // 退出程序
                    } else {
                        // 可以在这里处理其他的响应，例如显示一个错误消息
                        JOptionPane.showMessageDialog(frame, "Fall！", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(frame, "Connection Fall", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });


        newGameButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String response = null;
                try {
                    response = client.startNewGame(username);
                    for (JButton button : boardButtons) {
                        button.setText("");
                    }
                } catch (RemoteException ex) {
                    throw new RuntimeException(ex);
                }
                // 根据服务器的响应显示相应的消息
                if ("Waiting...".equals(response)) {
                    JOptionPane.showMessageDialog(frame, "Waiting...", "Message", JOptionPane.INFORMATION_MESSAGE);
                } else if ("You already in a game!".equals(response)) {
                    JOptionPane.showMessageDialog(frame, "You already in a game", "Error", JOptionPane.ERROR_MESSAGE);
                } else if ("No User!".equals(response)) {
                    JOptionPane.showMessageDialog(frame, "No user", "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(frame, response, "Message", JOptionPane.INFORMATION_MESSAGE);
                }

            }
        });


        JPanel gameEndPanel = new JPanel();
//        gameEndPanel.add(quitButton);
//        gameEndPanel.add(newGameButton);
        frame.add(gameEndPanel, BorderLayout.SOUTH);


        pauseDialog = new JDialog(frame, "Game Paused", true);
        pauseDialog.setSize(200, 100);
        pauseDialog.setLayout(new FlowLayout());
        pauseDialog.add(new JLabel("Game is paused..."));
        pauseDialog.setLocationRelativeTo(frame);  // 使对话框居中显示


// Timer to update the elapsed time and check game status
        timer = new Timer(500, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    try {
                        try {
                            if (!client.checkServerStatue()) {
//                                timer.stop();
                                JOptionPane.showMessageDialog(frame, "Server has disconnected.", "Error", JOptionPane.ERROR_MESSAGE);
                                Thread.sleep(5000);
                                System.exit(0);
                            }
                        } catch (InterruptedException ex) {
                            throw new RuntimeException(ex);
                        }
                        boolean isPaused = client.isPaused(username);
                        if (isPaused) {
                            if (!pauseDialog.isVisible()) {
                                pauseDialog.setVisible(true);
                            }
                        } else {
                            if (pauseDialog.isVisible()) {
                                pauseDialog.setVisible(false);
                            }
                        }
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }

                    String gameState = client.updateGameStatue(username);
                    String stepTime = client.updateStepTime(username);
                    timeDisplayLabel.setText(stepTime + "s");
                    centerPanel.setBorder(BorderFactory.createTitledBorder(client.updateNextMove()));
                    if (!"In Progress".equals(gameState) && !"You are not in a game!".equals(gameState)) {
//                        timer.stop();  // Stop the timer
                        if (!hasDisplayedResult) {
                            String message = "";
                            if (gameState.contains(username)) {
                                message = gameState;
                            } else if ("It's a draw!".equals(gameState)) {
                                message = "It's a draw!";
                            } else {
                                message = gameState;
                            }

                            // Display the result and ask the user for the next action
                            Object[] options = {"Find a new match", "Quit"};
                            int option = JOptionPane.showOptionDialog(frame, message + "\nWhat would you like to do next?", "Choose an action", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                            if (option == JOptionPane.YES_OPTION) {
                                newGameButton.doClick();
                            } else {
                                quitButton.doClick();
                            }

                            hasDisplayedResult = true;
                        }
                    }
                    if ("In Progress".equals(gameState) || "You are not in a game!".equals(gameState)) {
                        hasDisplayedResult = false;
                    }
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
            }
        });
        timer.start();




        frame.setVisible(true);


    }
    public void setClient(TicTacToeClient client) {
        this.client = client;
    }

    public void updateBoardButton(char[][] board) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int index = i * 3 + j;
                boardButtons[index].setText(String.valueOf(board[i][j]));
            }
        }
    }

//    public void appendChatArea(String message){
//        chatArea.append(message+'\n');
//    }

    public void appendChatArea(String message) {
        // 添加新消息
        chatArea.append(message + '\n');

        // 按行分割内容
        String[] lines = chatArea.getText().split("\n");

        // 检查行数是否超过10
        if (lines.length > 10) {
            // 删除最早的消息
            String newText = String.join("\n", Arrays.copyOfRange(lines, 1, lines.length));
            chatArea.setText(newText+"\n");
        }

        // 将插入符号位置设置为chatArea的末尾，以确保最新消息始终可见
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }



//    public static void main(String[] args) {
//        new ClientGUI();
//    }
}

