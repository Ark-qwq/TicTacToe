package com.fangzhouwang.Remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @Author Fangzhou Wang
 * @Date 2023/10/7 00:15
 **/
public interface ClientInterface extends Remote {
    void updateBoard(char[][] board) throws RemoteException;
    void updateChat(String message) throws  RemoteException;
    void updateGameResult(String message)throws RemoteException;
    boolean isAlive() throws RemoteException;

}