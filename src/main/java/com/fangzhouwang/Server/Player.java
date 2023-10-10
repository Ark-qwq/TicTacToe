package com.fangzhouwang.Server;

import com.fangzhouwang.Remote.ClientInterface;

import java.rmi.RemoteException;

/**
*    @Author Fangzhou Wang
*    @Date 2023/10/6 17:05
**/
public class Player {
    private String username;
    private int  score;
    private ClientInterface client;

    public Player(String username, ClientInterface client) throws RemoteException {
        this.username = username;
        this.client = client;
    }

    public ClientInterface getClient() {
        return client;
    }

    public Player(String username) {
        this.username = username;
        this.score = 0;
    }

    public String getName() {
        return username;
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public void subtractScore(int points) {
        this.score -= points;
    }
    public void setClient(ClientInterface client){
        this.client = client;
    }
}

