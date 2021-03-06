package com.webcheckers.appl;

import com.webcheckers.model.Player;

import java.util.ArrayList;


public class PlayerServices {

    //Attributes
    //private Match match;
    //playerList holding the names of players
    private ArrayList<Player> playerList;

    /**
     * Constructor for PlayerServices Object
     */
    public PlayerServices(){
        playerList = new ArrayList<>();
    }

    /**
     * End the current match;
     */
    public void endSession() {
        //match = null;
    }

    /**
     * Add a new player to the player list
     * @param player
     */
    public void addPlayer(Player player) {
        playerList.add(player);
    }

    /**
     * Remove an existing player
     * @param player
     */
    public void removePlayer(Player player){
        playerList.remove(player);
    }

    /**
     * Checks if the username is taken.
     * @param player  - the user
     * @return          - true if not taken and false if taken
     */
    public boolean isAvailable(Player player) {
        return playerList.contains(player);
    }

    /**
     * Get the player
     * @param username  - the username of the player
     * @return          - the player
     */
    public Player getPlayer(String username) {
        for (Player player : playerList) {
            if (player.getName().equals(username)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Gets the number of players
     * @return          - number int of players
     */
    public int numPlayers() {
        return playerList.size();
    }

    /**
     * Gets a copy of players online
     * @return a clean copy of player list online
     */
    public ArrayList<Player> getPlayerList() {
        ArrayList<Player> ret = new ArrayList<>();
        ret.addAll(playerList);
        return ret;
    }

}
