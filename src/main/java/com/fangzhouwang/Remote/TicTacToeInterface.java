package com.fangzhouwang.Remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface TicTacToeInterface extends Remote {
    String registerPlayer(String username,ClientInterface client) throws RemoteException;
    String makeMove(String username, int row, int col) throws RemoteException;
    void sendMessage(String username, String message) throws RemoteException;
    List<String> getChatMessages(String username) throws RemoteException;
    String quitSystem(String username) throws RemoteException;
    String startNewGame(String username) throws  RemoteException;
    int getRank(String username) throws RemoteException;
    void rejoinGame(String username,ClientInterface clientInterface) throws RemoteException;
    String updateGameStatue(String username)throws RemoteException;
    String updateNextMove(String username)throws RemoteException;
    String getStepTime(String username)throws RemoteException;
    boolean isAlive() throws RemoteException;
    boolean isLogIn(String username) throws RemoteException;
    boolean isPaused(String username) throws RemoteException;
    boolean isDuplicate(String username) throws RemoteException;
}


