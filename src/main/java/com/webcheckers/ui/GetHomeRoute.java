package com.webcheckers.ui;

import java.util.*;
import java.util.logging.Logger;

import com.webcheckers.appl.GameCenter;
import com.webcheckers.appl.PlayerServices;
import com.webcheckers.model.Leaderboard;
import com.webcheckers.model.Match;
import com.webcheckers.model.Player;
import spark.*;

import com.webcheckers.util.Message;

import static spark.Spark.halt;

/**
 * The UI Controller to GET the Home page.
 *
 * @author <a href='mailto:bdbvse@rit.edu'>Bryan Basham</a>
 */
public class GetHomeRoute implements Route {
    // Values used in the view-model map for rendering the game view.
    static final String TITLE_ATTR = "title";
    static final String MESSAGE_ATTR = "message";
    static final String PLAYERS_ATTR = "players";
    static final String GAME_ID_ATTR = "gameId";
    static final String NUM_PLAYERS_ATTR = "numPlayers";
    static final String TITLE = "Welcome!";
    static final String CURRENT_PLAYER_ATTR = "currentPlayer";
    static final String GAMES_ATTR = "games";
    static final String WON_ATTR = "won";
    static final String LOST_ATTR = "lost";
    static final String RATIO_ATTR = "ratio";
    static final String TAKEN_PIECES_ATTR = "piecesTaken";
    static final String LOST_PIECES_ATTR = "piecesLost";
    static final String LEADERBOARD_ATTR = "leaderboard";
    static final String NAMES_ONLY_ATTR = "namesOnly";
    static final String GAMES_ONLY_ATTR = "gamesOnly";
    static final String WON_ONLY_ATTR = "wonOnly";
    static final String LOST_ONLY_ATTR = "lostOnly";
    static final String PIECES_TAKEN_ONLY_ATTR = "piecesTakenOnly";
    static final String PIECES_LOST_ONLY_ATTR = "piecesLostOnly";

    public static final String VIEW_NAME = "home.ftl";
    private static final Message WELCOME_MSG = Message.info("Welcome to the world of online Checkers.");
    public static final Message SIGNIN_MSG = Message.info("You Have Successfully Sign In!");

    public static final String CURRENT_USERNAME_KEY = "currentPlayer";
    public static final String PLAYERSERVICES_KEY = "playerServices";
    public static final String TIMEOUT_SESSION_KEY = "timeoutWatchDog";
    private static final Logger LOG = Logger.getLogger(GetHomeRoute.class.getName());

    private final GameCenter gameCenter;
    private final PlayerServices playerServices;
    private final TemplateEngine templateEngine;

    /**
     * Create the Spark Route (UI controller) to handle all {@code GET /} HTTP requests.
     *
     * @param templateEngine
     *   the HTML template rendering engine
     */
    public GetHomeRoute(final PlayerServices playerServices, final GameCenter gameCenter, final TemplateEngine templateEngine) {
        // validation
        Objects.requireNonNull(gameCenter, "gameCenter must not be null");
        Objects.requireNonNull(templateEngine, "templateEngine must not be null");
        //
        this.gameCenter = gameCenter;
        this.templateEngine = templateEngine;
        this.playerServices = playerServices;
        //
        LOG.config("GetHomeRoute is initialized.");
    }

    /**
     * Deletes the player from the ingame list if they recently left a match. Checks if the player still has a match
     * and they are still in the ingame list before deleting them from the list. Game is not removed since we still need
     * that if another player is in the game screen, which GetGameRoute still needs the game to refresh the page without
     * crashing.
     * @param player the player to remove from ingame list
     */
    public synchronized void deletePlayerFromList(Player player) {
        //if the player just recently left a match, delete them from the ingame list
        if (gameCenter.getMatch(player) != null && gameCenter.isInMatch(player)) {
            gameCenter.removePlayer(player);
            player.changeRecentlyInGame(false);
        }
    }

    /**
     * Deletes the match from gameCenter IF AND ONLY IF both players have left the game screen, since the GetGameRoute
     * needs match. Checks by seeing if both players have been deleted from the ingame list, which means that both players
     * have left the game screen.
     * @param player the player name which identifies which game to remove from gameCenter
     */
    public synchronized void deleteMatchIfPossible(Player player) {
        // if both players of the match has left the match, and returned to the home screen, delete the match
        // since nothing about the match is needed, otherwise will crash the session still in the game screen
        if (gameCenter.getMatch(player) != null) {
            Match match = gameCenter.getMatch(player);
            if (player.equals(match.getRedPlayer())) {
                if (!gameCenter.isInMatch(match.getWhitePlayer()))
                    gameCenter.removeMatch(match);
            } else if (player.equals(match.getWhitePlayer())) {
                if (!gameCenter.isInMatch(match.getRedPlayer()))
                    gameCenter.removeMatch(match);
            }
        }
    }

    /**
     * Render the WebCheckers Home page.
     *
     * @param request
     *   the HTTP request
     * @param response
     *   the HTTP response
     *
     * @return
     *   the rendered HTML for the Home page
     */
    @Override
    public Object handle(Request request, Response response) {
        LOG.finer("GetHomeRoute is invoked.");
        //
        final Session httpSession = request.session();
        Map<String, Object> vm = new HashMap<>();

        vm.put(TITLE_ATTR, TITLE);

        //If no session is currently active
        if(httpSession.attribute(PLAYERSERVICES_KEY) == null) {
            //get object for specific services for the player
            httpSession.attribute(PLAYERSERVICES_KEY, playerServices);

            httpSession.attribute(TIMEOUT_SESSION_KEY, new SessionTimeoutWatchdog(playerServices));

            //Can be not active for 10 min before it times you out.
            httpSession.maxInactiveInterval(600);
        }

        //If user is currently logged in
        ArrayList<Player> players = playerServices.getPlayerList();
        if(httpSession.attribute(CURRENT_USERNAME_KEY) != null){
            Player player = playerServices.getPlayer(httpSession.attribute(CURRENT_USERNAME_KEY));

            // set it to false so the next time they win or lose a game their records will be modified
            if (player.getRecordsModified())
                player.setRecordsModified(false);

            // if the player just recently left a match, delete them from the ingame list
            if (player.wasRecentlyInGame()) {
                deletePlayerFromList(player);
            }

            // if both players of the match has left the match, and returned to the home screen, delete the match
            // since nothing about the match is needed, otherwise will crash the session still in the game screen
            deleteMatchIfPossible(player);

            // Grab leaderboards
            Leaderboard leaderboard = new Leaderboard();
            leaderboard.updateAllBoards();
            TreeSet<Player> gamesBoard = leaderboard.getGamesBoard();
            TreeSet<Player> wonBoard = leaderboard.getWonBoard();
            TreeSet<Player> lostBoard = leaderboard.getLostBoard();
            TreeSet<Player> piecesTakenBoard = leaderboard.getPiecesTakenBoard();
            TreeSet<Player> piecesLostBoard = leaderboard.getPiecesLostBoard();

            // redirect the challenged player to the game
            if (player.getStatus() == Player.Status.challenged ||
                player.getStatus() == Player.Status.ingame){
                response.redirect(WebServer.GAME_URL);
                halt();
                return null;
            }

            ArrayList<String> namesOnly = new ArrayList<>();
            ArrayList<Integer> gamesOnly = new ArrayList<>();
            ArrayList<Integer> wonOnly = new ArrayList<>();
            ArrayList<Integer> lostOnly = new ArrayList<>();
            ArrayList<Integer> piecesTakenOnly = new ArrayList<>();
            ArrayList<Integer> piecesLostOnly = new ArrayList<>();
            // check what leaderboard should be displayed
            String boardButton = request.queryParams("boardButton");
            if (boardButton == null) {
                // set to rank by number of games by default
                namesOnly = leaderboard.getNamesOnly(gamesBoard);
                gamesOnly = leaderboard.getGamesOnly(gamesBoard);
                wonOnly = leaderboard.getWonOnly(gamesBoard);
                lostOnly = leaderboard.getLostOnly(gamesBoard);
                piecesTakenOnly = leaderboard.getPiecesTakenOnly(gamesBoard);
                piecesLostOnly = leaderboard.getPiecesLostOnly(gamesBoard);
            } else if (boardButton.equals("Games Rankings")) {
                //System.out.println("changed to games");
                namesOnly = leaderboard.getNamesOnly(gamesBoard);
                gamesOnly = leaderboard.getGamesOnly(gamesBoard);
                wonOnly = leaderboard.getWonOnly(gamesBoard);
                lostOnly = leaderboard.getLostOnly(gamesBoard);
                piecesTakenOnly = leaderboard.getPiecesTakenOnly(gamesBoard);
                piecesLostOnly = leaderboard.getPiecesLostOnly(gamesBoard);
            } else if (boardButton.equals("Victory Rankings")) {
                //System.out.println("changed to won");
                namesOnly = leaderboard.getNamesOnly(wonBoard);
                gamesOnly = leaderboard.getGamesOnly(wonBoard);
                wonOnly = leaderboard.getWonOnly(wonBoard);
                lostOnly = leaderboard.getLostOnly(wonBoard);
                piecesTakenOnly = leaderboard.getPiecesTakenOnly(wonBoard);
                piecesLostOnly = leaderboard.getPiecesLostOnly(wonBoard);
            } else if (boardButton.equals("Loss Rankings")) {
                //System.out.println("changed to loss");
                namesOnly = leaderboard.getNamesOnly(lostBoard);
                gamesOnly = leaderboard.getGamesOnly(lostBoard);
                wonOnly = leaderboard.getWonOnly(lostBoard);
                lostOnly = leaderboard.getLostOnly(lostBoard);
                piecesTakenOnly = leaderboard.getPiecesTakenOnly(lostBoard);
                piecesLostOnly = leaderboard.getPiecesLostOnly(lostBoard);
            } else if (boardButton.equals("Pieces Taken Rankings")) {
                //System.out.println("changed to PiecesTaken");
                namesOnly = leaderboard.getNamesOnly(piecesTakenBoard);
                gamesOnly = leaderboard.getGamesOnly(piecesTakenBoard);
                wonOnly = leaderboard.getWonOnly(piecesTakenBoard);
                lostOnly = leaderboard.getLostOnly(piecesTakenBoard);
                piecesTakenOnly = leaderboard.getPiecesTakenOnly(piecesTakenBoard);
                piecesLostOnly = leaderboard.getPiecesLostOnly(piecesTakenBoard);
            } else if (boardButton.equals("Pieces Lost Rankings")) {
                //System.out.println("changed to piecesLost");
                namesOnly = leaderboard.getNamesOnly(piecesLostBoard);
                gamesOnly = leaderboard.getGamesOnly(piecesLostBoard);
                wonOnly = leaderboard.getWonOnly(piecesLostBoard);
                lostOnly = leaderboard.getLostOnly(piecesLostBoard);
                piecesTakenOnly = leaderboard.getPiecesTakenOnly(piecesLostBoard);
                piecesLostOnly = leaderboard.getPiecesLostOnly(piecesLostBoard);
            }

            vm.put(MESSAGE_ATTR, SIGNIN_MSG);
            if (httpSession.attribute("message") != null)
                vm.put(MESSAGE_ATTR, httpSession.attribute("message"));
            httpSession.removeAttribute("message");
            vm.put(GAMES_ATTR, player.getGames());
            vm.put(WON_ATTR, player.getWon());
            vm.put(LOST_ATTR, player.getLost());
            vm.put(RATIO_ATTR, player.getRatio());
            vm.put(TAKEN_PIECES_ATTR, player.getPiecesTaken());
            vm.put(LOST_PIECES_ATTR, player.getPiecesLost());
            vm.put(CURRENT_USERNAME_KEY, httpSession.attribute(CURRENT_USERNAME_KEY));
            players.remove(player);
            vm.put(PLAYERS_ATTR, players);
            vm.put(NAMES_ONLY_ATTR, namesOnly);
            vm.put(GAMES_ONLY_ATTR, gamesOnly);
            vm.put(WON_ONLY_ATTR, wonOnly);
            vm.put(LOST_ONLY_ATTR, lostOnly);
            vm.put(PIECES_TAKEN_ONLY_ATTR, piecesTakenOnly);
            vm.put(PIECES_LOST_ONLY_ATTR, piecesLostOnly);
            //TODO change home.ftl please
        } else {
            // only show the number of players online if you are not signed in
            vm.put(NUM_PLAYERS_ATTR, playerServices.getPlayerList().size());
            vm.put(MESSAGE_ATTR, WELCOME_MSG);
        }

        return templateEngine.render(new ModelAndView(vm, VIEW_NAME));
    }
}
