package com.webcheckers.ui;

import com.google.gson.Gson;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.webcheckers.appl.GameCenter;
import com.webcheckers.appl.PlayerServices;
import com.webcheckers.model.*;
import com.webcheckers.util.Message;
import spark.*;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.webcheckers.ui.WebServer.csvFile;
import static spark.Spark.halt;

public class GetGameRoute implements Route {
    // Values used in the view-model map for rendering the game view.

    public static final String TITLE = "Game";
    public static final String VIEW_NAME = "game.ftl";
    public static final String CURRENT_USER_ATTR = "currentUser";
    public static final String VIEW_MODE_ATTR = "viewMode";
    public static final String MODE_OPTION_ATTR = "modeOptionsAsJSON";
    public static final String RED_PLAYER_ATTR = "redPlayer";
    public static final String WHITE_PLAYER_ATTR = "whitePlayer";
    public static final String ACTIVE_COLOR_ATTR = "activeColor";
    public static final String BOARD_ATTR = "board";
    public enum viewMode {PLAY, SPECTATOR, REPLAY}

    public static final String MATCH_ATTR = "match";

    private final TemplateEngine templateEngine;
    private final GameCenter gameCenter;
    private final PlayerServices playerServices;
    private final CSVutility csvutility;

    private Gson gson;

    /**
     * The constructor for the {@code GET /game} route handler.
     *
     * @param templateEngine
     *    The {@link TemplateEngine} used for rendering page HTML.
     */
    public GetGameRoute(final PlayerServices playerServices,
                        final GameCenter gameCenter,
                        final TemplateEngine templateEngine){
        Objects.requireNonNull(playerServices, "playerServices must not be null");
        Objects.requireNonNull(gameCenter, "gameCenter must not be null");
        Objects.requireNonNull(templateEngine, "templateEngine must not be null");
        this.playerServices = playerServices;
        this.gameCenter = gameCenter;
        this.templateEngine = templateEngine;
        this.csvutility = new CSVutility();
    }

    /**
     * Render the WebCheckers Game page.
     *
     * @param request
     *   the HTTP request
     * @param response
     *   the HTTP response
     *
     * @return
     *   the rendered HTML for the Game page
     */
    @Override
    public Object handle(Request request, Response response){
        final Session httpSession = request.session();
        final PlayerServices playerServices = httpSession.attribute(GetHomeRoute.PLAYERSERVICES_KEY);

        if(playerServices != null) {
            final Map<String, Object> vm = new HashMap<>();
            vm.put(GetHomeRoute.TITLE_ATTR, TITLE);
            String currentPlayerName = httpSession.attribute(GetHomeRoute.CURRENT_USERNAME_KEY);
            Player currentPlayer = playerServices.getPlayer(currentPlayerName);

            vm.put(CURRENT_USER_ATTR, currentPlayer);

            Player redPlayer;
            Player whitePlayer;
            Match currentMatch;
            // if the button is just triggered, initialize everything
            if (request.queryParams("button") != null) {
                String opponentName = request.queryParams("button");
                redPlayer = currentPlayer;
                whitePlayer = playerServices.getPlayer(opponentName);
                // if either player is not available
                if (! gameCenter.addMatch(redPlayer, whitePlayer)){
                    httpSession.attribute("message", Message.error(whitePlayer.getName() + " is already in game!"));
                    response.redirect(WebServer.HOME_URL);
                    halt();
                    return null;
                }
                httpSession.attribute("help", false);
                currentMatch = gameCenter.getMatch(redPlayer);
            } else { // else get the information from the match
                currentMatch = gameCenter.getMatch(currentPlayer);
                redPlayer = currentMatch.getRedPlayer();
                whitePlayer = currentMatch.getWhitePlayer();
                httpSession.attribute("help", false);
            }

            // only display the help button when it is your turn and the game is not ended
            if (currentMatch.getCurrentPlayer().equals(currentPlayer) &&
                    ! currentMatch.isGameOver() ) {
                // double check if the game ended 
                if (currentMatch.getWinner() == null) {
                    vm.put("PLAYING", true);
                }
            }

            // check if the help button is click
            if (request.queryParams("help") != null) {
                // if it is asking for help
                if (request.queryParams("help").equals("help")) {
                    currentMatch.activateHelp();
                    vm.put("HELP", 0); // 0 is a filler
                }
                // else it is asking for removing the help
                else {
                    currentMatch.deactivateHelp();
                }
            }

            // save it to session
            httpSession.attribute(MATCH_ATTR, currentMatch);
            httpSession.attribute(RED_PLAYER_ATTR, redPlayer);
            httpSession.attribute(WHITE_PLAYER_ATTR, whitePlayer);

            // send players to ftl
            vm.put(RED_PLAYER_ATTR, redPlayer);
            vm.put(WHITE_PLAYER_ATTR, whitePlayer);

            //Match match = gameCenter.getMatch(currentPlayer);
            BoardView whiteBoardView = currentMatch.getWhiteBoardView();
            BoardView redBoardView = currentMatch.getRedBoardView();

            if(currentPlayerName.equals(redPlayer.getName())) {
                vm.put(BOARD_ATTR, redBoardView);
                vm.put(CURRENT_USER_ATTR, redPlayer);
            } else {
                vm.put(BOARD_ATTR, whiteBoardView);
                vm.put(CURRENT_USER_ATTR, whitePlayer);
            }

            // for the nav-bar to display the signout option
            vm.put(GetHomeRoute.CURRENT_PLAYER_ATTR, currentPlayerName);

            vm.put(ACTIVE_COLOR_ATTR, currentMatch.getActiveColor());
            // right now there is only the option to play
            viewMode currentViewMode = viewMode.PLAY;
            vm.put(VIEW_MODE_ATTR, currentViewMode);

            // if game is in resigned state (opponent resigned)
            if (currentMatch.isGameResigned() == Match.STATE.resigned) {
                // remove the player from the ingame list after exiting the game
                currentPlayer.changeRecentlyInGame(true);
                // guard so that their records won't be modified more than once in case of mandatory refreshes
                if (! currentPlayer.getRecordsModified()) {
                    int piecesTaken, piecesLost;
                    if (currentPlayer.equals(currentMatch.getRedPlayer())) {
                        piecesTaken = 12 - currentMatch.getWhitePieces().size();
                        piecesLost = 12 - currentMatch.getRedPieces().size();
                    } else {
                        piecesTaken = 12 - currentMatch.getRedPieces().size();
                        piecesLost = 12 - currentMatch.getWhitePieces().size();
                    }
                    currentPlayer.addPiecesTaken(piecesTaken);
                    currentPlayer.addPiecesLost(piecesLost);
                    currentPlayer.addWon();
                    csvutility.editPlayerRecords(currentPlayer);
                    currentPlayer.setRecordsModified(true);
                }

                Gson gson = new Gson();
                Map <String, Object> modeOptions = new HashMap<>(2);
                modeOptions.put("isGameOver", true);
                // produce the right message
                if (currentPlayer.equals(redPlayer)) {
                    modeOptions.put("gameOverMessage", whitePlayer.getName() + " has resigned. You won!");
                } else if (currentPlayer.equals(whitePlayer)) {
                    modeOptions.put("gameOverMessage", redPlayer.getName() + " has resigned. You won!");
                }
                vm.put(MODE_OPTION_ATTR, gson.toJson(modeOptions));
                currentPlayer.changeStatus(Player.Status.waiting);
            } else if (currentMatch.getRedPieces().size() == 0) {
                // remove the player from the ingame list after exiting the game
                currentPlayer.changeRecentlyInGame(true);
                // guard so that their records won't be modified more than once in case of mandatory refreshes
                if (! currentPlayer.getRecordsModified()) {
                    int piecesTaken, piecesLost;
                    if (currentPlayer.equals(currentMatch.getRedPlayer())) {
                        piecesTaken = 12 - currentMatch.getWhitePieces().size();
                        piecesLost = 12 - currentMatch.getRedPieces().size();
                        currentPlayer.addPiecesTaken(piecesTaken);
                        currentPlayer.addPiecesLost(piecesLost);
                        currentPlayer.addLost();
                    } else {
                        piecesTaken = 12 - currentMatch.getRedPieces().size();
                        piecesLost = 12 - currentMatch.getWhitePieces().size();
                        currentPlayer.addPiecesTaken(piecesTaken);
                        currentPlayer.addPiecesLost(piecesLost);
                        currentPlayer.addWon();
                    }
                    csvutility.editPlayerRecords(currentPlayer);
                    currentPlayer.setRecordsModified(true);
                }
                Gson gson = new Gson();
                Map <String, Object> modeOptions = new HashMap<>(2);
                modeOptions.put("isGameOver", true);
                modeOptions.put("gameOverMessage", whitePlayer.getName() + " has captured all opponent pieces!");
                vm.put(MODE_OPTION_ATTR, gson.toJson(modeOptions));
                //gameCenter.removePlayer(currentPlayer);
                currentPlayer.changeStatus(Player.Status.waiting);
                //gameCenter.removeMatch(currentMatch);
            } else if (currentMatch.getWhitePieces().size() == 0) {
                // remove the player from the ingame list after exiting the game
                currentPlayer.changeRecentlyInGame(true);
                // guard so that their records won't be modified more than once in case of mandatory refreshes
                if (! currentPlayer.getRecordsModified()) {
                    int piecesTaken, piecesLost;
                    if (currentPlayer.equals(currentMatch.getRedPlayer())) {
                        piecesTaken = 12 - currentMatch.getWhitePieces().size();
                        piecesLost = 12 - currentMatch.getRedPieces().size();
                        currentPlayer.addPiecesTaken(piecesTaken);
                        currentPlayer.addPiecesLost(piecesLost);
                        currentPlayer.addWon();
                    } else {
                        piecesTaken = 12 - currentMatch.getRedPieces().size();
                        piecesLost = 12 - currentMatch.getWhitePieces().size();
                        currentPlayer.addPiecesTaken(piecesTaken);
                        currentPlayer.addPiecesLost(piecesLost);
                        currentPlayer.addLost();
                    }
                    csvutility.editPlayerRecords(currentPlayer);
                    currentPlayer.setRecordsModified(true);
                }
                Gson gson = new Gson();
                Map <String, Object> modeOptions = new HashMap<>(2);
                modeOptions.put("isGameOver", true);
                modeOptions.put("gameOverMessage", redPlayer.getName() + " has captured all opponent pieces!");
                vm.put(MODE_OPTION_ATTR, gson.toJson(modeOptions));
                currentPlayer.changeStatus(Player.Status.waiting);
            }
            return templateEngine.render(new ModelAndView(vm, VIEW_NAME));
        } else {
            response.redirect("/");
            halt();
            return null;
        }
    }
}
