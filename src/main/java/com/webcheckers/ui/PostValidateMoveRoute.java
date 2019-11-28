package com.webcheckers.ui;

import com.google.gson.Gson;
import com.webcheckers.appl.GameCenter;
import com.webcheckers.appl.PlayerServices;
import com.webcheckers.model.*;
import com.webcheckers.util.Message;
import spark.*;

import java.util.*;

public class PostValidateMoveRoute implements Route {
    // attributes
    private PlayerServices playerServices;
    private GameCenter gameCenter;
    private TemplateEngine templateEngine;
    private final Gson gson;

    // Values used in the view-model map for rendering the game view.
    // move
    public static final Message VALID_MOVE_MESSAGE = Message.info("This is a valid move.");
    public static final Message ADJACENT_MOVE_ERROR = Message.error("Your move must be adjacent to you.");
    public static final Message FORWARD_MOVE_ERROR = Message.error("Normal piece can only move forward.");
    public static final Message JUMP_OPTION_ERROR = Message.error("You must jump instead of move.");
    // jump
    public static final Message VALID_JUMP_MESSAGE = Message.info("This is a valid jump.");
    public static final Message ADJACENT_JUMP_ERROR = Message.error("Your jump must be adjacent to you.");
    public static final Message FORWARD_JUMP_ERROR = Message.error("Normal piece can only jump forward.");
    public static final Message OPPONENT_JUMP_ERROR = Message.error("You can only jump over your opponent.");
    public static final Message EMPTY_JUMP_ERROR = Message.error("You cannot jump over nothing.");
    // neither
    public static final Message MAX_ROW_MESSAGE = Message.error("The maximum number of rows you can move is 2");
    // multiple jump
    public static final Message MOVE_ERROR = Message.error("After a jump, you can only jump.");
    public static final Message DIFFERENT_ERROR = Message.error("After a jump, you can only move the previous piece");
    public static final Message END_ERROR = Message.error("There is no more jump can be made from this piece.\n" +
                                                            "You have to either submit or backup.");
    public static final Message MULTIPLE_ERROR = Message.error("You cannot jump if you just moved.");
    public static final Message REPEAT_ERROR = Message.error("You cannot jump jump over the piece you just jumped over.");



    // param name
    public static final String ACTION_DATA = "actionData";

    public PostValidateMoveRoute(PlayerServices playerServices,
                                 GameCenter gameCenter,
                                 TemplateEngine templateEngine, Gson gson){
        this.gson = gson;
        Objects.requireNonNull(playerServices, "playerServices must not be null");
        Objects.requireNonNull(gameCenter, "gameCenter must not be null");
        Objects.requireNonNull(templateEngine, "templateEngine must not be null");
        this.playerServices = playerServices;
        this.gameCenter = gameCenter;
        this.templateEngine = templateEngine;

    }

    /**
     * Validate the most recent move.
     *
     * @param request
     *   the HTTP request
     * @param response
     *   the HTTP response
     *
     * @return
     *   the message displayed after a move being made.
     */
    @Override
    public Object handle(Request request, Response response) {
        final Session httpSession = request.session();
        final PlayerServices playerServices = httpSession.attribute(GetHomeRoute.PLAYERSERVICES_KEY);

        if (playerServices != null) {
            // get the information of the current user
            String currentPlayerName = httpSession.attribute(GetHomeRoute.CURRENT_USERNAME_KEY);
            Player currentPlayer = playerServices.getPlayer(currentPlayerName);
            Match currentMatch = gameCenter.getMatch(currentPlayer);

            //check if the help button is clicked
            if (currentMatch.getHelp()){
                return gson.toJson(Message.error("You must click help again in order to make a move."));
            }

            Move move = gson.fromJson(request.queryParams(ACTION_DATA), Move.class);

            Message message = currentMatch.validateMove(move);
            return gson.toJson(message);

        }
        return null;
    }
}
