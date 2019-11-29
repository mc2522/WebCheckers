package com.webcheckers.model;

import com.webcheckers.ui.PostValidateMoveRoute;
import com.webcheckers.util.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Logger;

public class Match {
    // Attributes
    private static final Logger LOG = Logger.getLogger(Match.class.getName());
    private BoardView redBoardView = new BoardView(Piece.Color.RED);
    private BoardView whiteBoardView = new BoardView(Piece.Color.WHITE);
    private Player redPlayer;
    private Player whitePlayer;
    private Piece.Color activeColor;
    private Player winner = null;
    private ArrayList<Position> redPieces = initializePieces(redBoardView);
    private ArrayList<Position> whitePieces = initializePieces(whiteBoardView);
    private Stack<Piece> piecesRemoved = new Stack<>();
    private final Map<String, Object> modeOptions;
    private boolean isGameOver = false;
    public enum STATE {resigned, finished, running}
    private STATE state;
    private ArrayList<Move> moves = new ArrayList<>();
    private boolean hasNextJump = false;
    private Piece.Type currentType = Piece.Type.SINGLE;
    private ArrayList<Position> possibleMoves = new ArrayList<>();
    private ArrayList<Position> possibleJumps = new ArrayList<>();
    private boolean possibleJump = false;
    private boolean help = false;

    /**
     * Create a new match between 2 players.
     * @param redPlayer     - red player
     * @param whitePlayer   - white player
     */
    public Match(Player redPlayer, Player whitePlayer){
        this.redPlayer = redPlayer;
        this.whitePlayer = whitePlayer;
        this.activeColor = Piece.Color.RED;
        this.state = STATE.running;
        this.modeOptions = new HashMap<>(2);
        this.modeOptions.put("isGameOver", false);
        this.modeOptions.put("gameOverMessage", null);
    }

    /**
     * Initialize all the pieces that the player has
     * @param board the board
     * @return an array list of positions
     */
    public ArrayList<Position> initializePieces(BoardView board){ // remove the color
        ArrayList<Position> pieces = new ArrayList<>();
        for (int y = 5; y < BoardView.NUM_ROW; y++){ // change back to 5
            Row row = board.getRow(y);
            for (int x = 0; x < BoardView.NUM_COL; x++){
                Space col = row.getCol(x);
                if (col.getPiece() != null) { // make sure there is piece
                    Position temp = new Position(y, x);
                    pieces.add(temp);
                }
            }
        }
        return pieces;
    }

    /**
     * Getter function for the red player.
     * @return  - the red player object
     */
    public Player getRedPlayer() {
        return redPlayer;
    }

    /**
     * Getter function for the white player.
     * @return  - the white player
     */
    public Player getWhitePlayer() {
        return whitePlayer;
    }

    /**
     * Getter function for the current active color
     * @return
     */
    public Piece.Color getActiveColor() {
        return activeColor;
    }

    /**
     * Getter function for all the pieces red player has
     * @return red player's pieces as space
     */
    public ArrayList<Position> getRedPieces() {
        return redPieces;
    }

    /**
     * Getter function for all the pieces white player has
     * @return white player's pieces as space
     */
    public ArrayList<Position> getWhitePieces() {
        return whitePieces;
    }

    /**
     * Getter function for pieces removed
     * @return pieces removed
     */
    public Stack<Piece> getPiecesRemoved(){
        return piecesRemoved;
    }

    /**
     * Alternates the current turn of the match, between the red and white player.
     */
    public void changeActiveColor(){
        if(activeColor == Piece.Color.RED)
            activeColor = Piece.Color.WHITE;
        else
            activeColor = Piece.Color.RED;
    }

    /**
     * Getter function for current player's turn.
     * @return  - the player whose turn it is.
     */
    public Player getCurrentPlayer() {
        if(activeColor.equals(Piece.Color.RED))
            return redPlayer;
        return whitePlayer;
    }

    /**
     * Getter function for red player's board view
     * @return red board view
     */
    public BoardView getRedBoardView(){ return redBoardView; }

    /**
     * Getter function for white player's board view
     * @return white board view
     */
    public BoardView getWhiteBoardView(){ return whiteBoardView; }

    /**
     * Getter function for the winner of the match.
     * @return  - the winner of the match.
     */
    public Player getWinner() {
        return winner;
    }

    /**
     * Set the winner and change the stats
     * @param winner the winner
     */
    public void setWinner(Player winner) {
        this.winner = winner;
        state = STATE.finished;
        modeOptions.put("isGameOver", true);
        modeOptions.put("gameOverMessage", winner.getName() + "captured all of the pieces.");
    }

    /**
     * returns the state of the game
     * @return
     */
    public STATE isGameResigned() {
        return state;
    }

    /**
     * sets the state of the game to resigned
     */
    public void resignGame(Player winner, Player loser) {
        this.winner = winner;
        state = STATE.resigned;
        modeOptions.put("isGameOver", true);
        modeOptions.put("gameOverMessage", loser.getName() + " has resigned.");
    }

    /**
     * Set the current type to SINGLE
     */
    public void typeSingle() { this.currentType = Piece.Type.SINGLE; }

    /**
     * Check if the game ended or not
     * @return boolean
     */
    public boolean isGameOver(){
        return isGameOver;
    }

    /**
     * Getter function for the help
     * @return boolean
     */
    public boolean getHelp() {
        return help;
    }

    /**
     * Getter functions for the moves made
     * @return an array list of moves made
     */
    public ArrayList<Move> getMoves(){ return this.moves; }

    /**
     * Add the move to the array list of moves made as stack
     * @param move the move made
     */
    public void pushMove(Move move) { this.moves.add(move); }

    /**
     * Remove the move from the array list of moves made as stack
     * @return the most recent move made
     */
    public Move popMove() { return this.moves.remove(moves.size() - 1); }

    /**
     * Empty the array list of moves made
     */
    public void emptyMoves() { this.moves = new ArrayList<>(); }

    /**
     * Check if the piece at pos is not a king
     * @param board the player's board view
     * @param pos the position of the piece
     * @return boolean
     */
    public boolean notKing(BoardView board, Position pos) {
        Space space = board.getSpace(pos.getRow(), pos.getCell());
        Piece.Type type;
        // need this so help can check all the pieces
        if (space.getPiece() != null) {
            type = space.getPiece().getType();
            return type != Piece.Type.KING;
        }
        // this for possible multiple jump
        else {
            return currentType != Piece.Type.KING;
        }
    }

    /**
     * Check if there a top left jump
     * @param board the board
     * @param pos the starting position
     * @return boolean
     */
    public boolean topLeftJump(BoardView board, Position pos) {
        int row = pos.getRow();
        int col = pos.getCell();
        Piece.Color color = activeColor;
        Space topLeft = board.getSpace(row - 1, col - 1);
        Space topLeftJump = board.getSpace(row - 2, col - 2);
        Position target = new Position(row - 2, col - 2);
        Move moveTopLeft = new Move(pos, target);
        Move backward = new Move(target, pos);
        return (! moves.contains(moveTopLeft)) &&
                (! moves.contains(backward)) &&
                spaceForJump(topLeft, topLeftJump, color);
    }

    /**
     * Check if there a top right jump
     * @param board the board
     * @param pos the starting position
     * @return boolean
     */
    public boolean topRightJump(BoardView board, Position pos) {
        int row = pos.getRow();
        int col = pos.getCell();
        Piece.Color color = activeColor;
        Space topRight = board.getSpace(row - 1, col + 1);
        Space topRightJump = board.getSpace(row - 2, col + 2);
        Position target = new Position(row - 2, col + 2);
        Move moveTopRight = new Move(pos, target);
        Move backward = new Move(target, pos);
        return (! moves.contains(moveTopRight)) &&
                (! moves.contains(backward)) &&
                spaceForJump(topRight, topRightJump, color);
    }

    /**
     * Check if there a bottom left jump
     * @param board the board
     * @param pos the starting position
     * @return boolean
     */
    public boolean botLeftJump(BoardView board, Position pos) {
        // if it is not a king
        if (notKing(board, pos)) {
            return false;
        }
        Piece.Color color = activeColor;
        int row = pos.getRow();
        int col = pos.getCell();
        Space bottomLeft = board.getSpace(row + 1, col - 1);
        Space bottomLeftJump = board.getSpace(row + 2, col - 2);
        Position target = new Position(row + 2, col - 2);
        Move moveBotLeft = new Move(pos, target);
        Move backward = new Move(target, pos);
        return (! moves.contains(moveBotLeft)) &&
                (! moves.contains(backward)) &&
                spaceForJump(bottomLeft, bottomLeftJump, color);
    }

    /**
     * Check if there a bottom right jump
     * @param board the board
     * @param pos the starting position
     * @return boolean
     */
    public boolean botRightJump(BoardView board, Position pos) {
        // if it's not a king
        if (notKing(board, pos)) {
            return false;
        }
        int row = pos.getRow();
        int col = pos.getCell();
        Piece.Color color = activeColor;
        Space bottomRight = board.getSpace(row + 1, col + 1);
        Space bottomRightJump = board.getSpace(row + 2, col + 2);
        Position target = new Position(row + 2, col + 2);
        Move moveBotRight = new Move(pos, target);
        Move backward = new Move(target, pos);
        return (! moves.contains(moveBotRight)) &&
                (! moves.contains(backward)) &&
                spaceForJump(bottomRight, bottomRightJump, color);
    }

    /**
     * Check if there is available jump for this piece
     * @param board the board
     * @param pos the position of the piece
     * @return boolean
     */
    public boolean checkFourDirections(BoardView board, Position pos) {
        if (topLeftJump(board, pos)) {
            return true;
        }
        else if (topRightJump(board, pos)) {
            return true;
        }
        else if (botLeftJump(board, pos)) {
            return true;
        }
        else {
            return botRightJump(board, pos);
        }
    }

    /**
     * Check if there is an option to jump. American rule states you have to jump
     * if you can jump.
     * @return boolean
     */
    public boolean optionToJump(BoardView board, ArrayList<Position> pieces){
        for (Position piece : pieces) {
            int row = piece.getRow();
            int col = piece.getCell();
            Position pos = new Position(row, col);

            if (checkFourDirections(board, pos))
                return true;
        }
        return false;
    }

    /**
     * Check if you can jump to target piece
     * @param space space that will be jump over
     * @param target space that will end up if can jump
     * @param color color of the current player
     * @return boolean
     */
    public boolean spaceForJump(Space space, Space target, Piece.Color color){
        if (target != null && space != null){
            // there is a space to jump to
            // and there is space to jump over
            if (space.getPiece() == null)
                return false; // there is no opponent piece there
            // that space is empty
            return space.getPiece().getColor() != color && // not ally
                    target.getPiece() == null; // there is option for a jump
        }
        return false;
    }

    /**
     * Add the move to the array list of possible moves
     * @param pos the end position of the possible move
     */
    public void addPossibleMove(Position pos){
        if (! possibleMoves.contains(pos)) {
            possibleMoves.add(pos);
        }
    }

    /**
     * Add the possible jump to the array list of possible jumps
     * @param pos the end position of the possible jump
     */
    public void addPossibleJump(Position pos) {
        if (! possibleJumps.contains(pos)) {
            possibleJumps.add(pos);
        }
    }

    /**
     * Check if the target position if empty
     * @param board the board
     * @param row the row
     * @param col the col
     * @return boolean
     */
    public boolean isEmpty(BoardView board, int row, int col){
        Space target = board.getSpace(row, col);
        if (target != null) {
            return target.getPiece() == null;
        }
        return false;
    }

    /**
     * Check for possible moves/jumps. If there is, add
     * them to the corresponding array list
     */
    public void possibleMoves() {
        possibleMoves = new ArrayList<>();
        possibleJumps = new ArrayList<>();
        possibleJump = false;
        ArrayList<Position> pieces;
        BoardView board;
        if (activeColor == Piece.Color.RED){
            pieces = redPieces;
            board = redBoardView;
        }
        else {
            pieces = whitePieces;
            board = whiteBoardView;
        }

        for (Position piece : pieces) {
            int row = piece.getRow();
            int col = piece.getCell();
            Piece.Type type = board.getSpace(row, col).getPiece().getType();
            if (topLeftJump(board, piece)) {
                possibleJump = true;
                addPossibleJump(new Position(row - 2, col - 2));
            }
            if (topRightJump(board, piece)) {
                possibleJump = true;
                addPossibleJump(new Position(row - 2, col + 2));
            }
            if (botLeftJump(board, piece)) {
                possibleJump = true;
                addPossibleJump(new Position(row + 2, col - 2));
            }
            if (botRightJump(board, piece)) {
                possibleJump = true;
                addPossibleJump(new Position(row + 2, col + 2));
            }
            // only waste time check move if there is no possible jump
            if (!possibleJump) {
                // move top left
                if (isEmpty(board, row - 1, col - 1)) {
                    addPossibleMove(new Position(row - 1, col - 1));
                }
                // move top right
                if (isEmpty(board, row - 1, col + 1)) {
                    addPossibleMove(new Position(row - 1, col + 1));
                }
                if (type == Piece.Type.KING) {
                    // move bot left
                    if (isEmpty(board, row + 1, col - 1)) {
                        addPossibleMove(new Position(row + 1, col - 1));
                    }
                    // move bot right
                    if (isEmpty(board, row + 1, col + 1)) {
                        addPossibleMove(new Position(row + 1, col + 1));
                    }
                }
            }
        }
    }

    /**
     * Activate the help
     */
    public void activateHelp(){
        BoardView board;
        Piece piece;
        Space space;
        // only do the calculation if there was no help
        if (! help){
            possibleMoves();
        }

        // get the board of the current player
        if (activeColor == Piece.Color.RED) {
            board = redBoardView;
        }
        else {
            board = whiteBoardView;
        }

        // if there is only moves available
        if (! possibleJump) {
            // display all possible moves
            for (Position pos : possibleMoves) {
                piece = new Piece(Piece.Type.SINGLE, Piece.Color.HELP);
                space = board.getSpace(pos.getRow(), pos.getCell());
                space.setPiece(piece);
                space.changeValid(false);
            }
        }
        // if there is even one possible jump
        else {
            // display all the possible jumps
            for (Position pos : possibleJumps) {
                piece = new Piece(Piece.Type.SINGLE, Piece.Color.HELP);
                space = board.getSpace(pos.getRow(), pos.getCell());
                space.setPiece(piece);
                space.changeValid(false);
            }
        }
        help = true;
    }

    /**
     * Deactivate the help
     */
    public void deactivateHelp(){
        BoardView board;
        Space space;
        // get the board for the current player
        if (activeColor == Piece.Color.RED) {
            board = redBoardView;
        }
        else {
            board = whiteBoardView;
        }
        // if there is only possible moves
        if (! possibleJump) {
            // display all the possible moves
            for (Position pos : possibleMoves) {
                space = board.getSpace(pos.getRow(), pos.getCell());
                space.setPiece(null);
                space.changeValid(true);
            }
        }
        // if there is even one possible jump
        else {
            // display all the possible jumps
            for (Position pos : possibleJumps) {
                space = board.getSpace(pos.getRow(), pos.getCell());
                space.setPiece(null);
                space.changeValid(true);
            }
        }
        help = false;
    }

    /**
     * After validating the move, move the piece
     * @param move the move made
     */
    public void move(Move move){
        // update the position of player's pieces
        Position start = move.getStart();
        Position end = move.getEnd();
        ArrayList<Position> pieces;
        BoardView board, opp;
        if (activeColor == Piece.Color.RED) {
            pieces = redPieces;
            board = redBoardView;
            opp = whiteBoardView;
        }
        else {
            pieces = whitePieces;
            board = whiteBoardView;
            opp = redBoardView;
        }
        pieces.remove(start);
        pieces.add(end);
        // update current player's board
        // remove the piece at the start position
        Space myStart = board.getSpace(start.getRow(), start.getCell());
        Piece myPiece = myStart.getPiece();
        myStart.setPiece(null);
        myStart.changeValid(true);
        // adding a piece to the end position
        Space myEnd = board.getSpace(end.getRow(), end.getCell());
        myEnd.setPiece(myPiece);
        myEnd.changeValid(false);

        // updating opponent's board
        // remove the piece at the start position
        Space oppStart = opp.getSpace(7 - start.getRow(), 7 - start.getCell());
        Piece oppPiece = oppStart.getPiece();
        oppStart.setPiece(null);
        oppStart.changeValid(true);
        // adding a piece to the end position
        Space oppEnd = opp.getSpace(7 - end.getRow(), 7 - end.getCell());
        oppEnd.setPiece(oppPiece);
        oppEnd.changeValid(false);

        if (end.getRow() == 0){ // means the piece become a king
            myEnd.getPiece().setType(Piece.Type.KING);
            oppEnd.getPiece().setType(Piece.Type.KING);
        }
    }

    /**
     * After validating the jump, jump over
     * @param move the move made by player
     * @return
     */
    public void jump(Move move){
        // update the position of current player's pieces
        Position start = move.getStart();
        Position end = move.getEnd();
        ArrayList<Position> pieces, oppPieces;
        BoardView board, opp;
        if (activeColor == Piece.Color.RED){
            pieces = redPieces;
            board = redBoardView;
            opp = whiteBoardView;
            oppPieces = whitePieces;
        }
        else {
            pieces = whitePieces;
            board = whiteBoardView;
            opp = redBoardView;
            oppPieces = redPieces;
        }
        pieces.remove(start);
        pieces.add(end);
        // update current player's board
        // remove the piece at the start position
        Space myStart = board.getSpace(start.getRow(), start.getCell());
        Piece myPiece = myStart.getPiece();
        myStart.setPiece(null);
        myStart.changeValid(true);
        // remove the piece that was jumped over
        int xDiff = (start.getCell() - end.getCell()) / 2;
        int yDiff = (start.getRow() - end.getRow()) / 2;

        Space myKill = board.getSpace(start.getRow() - yDiff, start.getCell() - xDiff);
        this.getPiecesRemoved().push(myKill.getPiece());
        myKill.setPiece(null);
        myKill.changeValid(true);
        // adding a piece to the end position
        Space myEnd = board.getSpace(end.getRow(), end.getCell());
        myEnd.setPiece(myPiece);
        myEnd.changeValid(false);

        // updating opponent's board
        // remove the piece at the start position
        Space oppStart = opp.getSpace(7 - start.getRow(), 7 - start.getCell());
        Piece oppPiece = oppStart.getPiece();
        oppStart.setPiece(null);
        oppStart.changeValid(true);
        // remove the piece that was jumped over
        Space oppKill = opp.getSpace(7 - (start.getRow() - yDiff), 7 - (start.getCell() - xDiff));
        oppKill.setPiece(null);
        oppKill.changeValid(true);
        // adding a piece to the end position
        Space oppEnd = opp.getSpace(7 - end.getRow(), 7 - end.getCell());
        oppEnd.setPiece(oppPiece);
        oppEnd.changeValid(false);
        // remove the piece that was jumped over for the pieces array
        int deadY = 7 - (start.getRow() - yDiff);
        int deadX = 7 - (start.getCell() - xDiff);
        Position oppLocation = new Position(deadY, deadX);
        oppPieces.remove(oppLocation);

        if (end.getRow() == 0){ // means the piece become a king
            myEnd.getPiece().setType(Piece.Type.KING);
            oppEnd.getPiece().setType(Piece.Type.KING);
        }
    }

    /**
     * Validate the move
     * @param move
     * @return
     */
    public Message validateMove(Move move){
        // get different board from the match
        BoardView currentBoardView;

        ArrayList<Position> pieces;
        // set the information accordingly
        if (activeColor == Piece.Color.RED){
            currentBoardView = redBoardView;
            pieces = redPieces;
        }
        else{
            currentBoardView = whiteBoardView;
            pieces = whitePieces;
        }

        Position start = move.getStart();
        Position end = move.getEnd();
        int row = start.getRow();
        int col = start.getCell();
        // if there is no moves being made
        if (moves.size() == 0) {
            Piece piece = currentBoardView.getSpace(row, col).getPiece();
            currentType = piece.getType();
        }
        // check if the web page get refreshed or not
        else {
            Move temp = this.popMove();
            if (! temp.getEnd().equals(start)) {
                this.emptyMoves();
            }
            else {
                this.pushMove(temp);
            }
        }

        boolean isKing = (currentType == Piece.Type.KING);

        Message message;
        boolean isMove = false;
        boolean isJump = false;

        // dealing with multiple jump
        if (moves.size() != 0){
            // check the previous move
            Move previousMove = this.popMove();
            Position previousStart = previousMove.getStart();
            Position previousEnd = previousMove.getEnd();
            this.pushMove(previousMove);

            // you cannot jump if you just moved
            if (previousStart.getRow() - previousEnd.getRow() == 1 ||
                    previousStart.getRow() - previousEnd.getRow() == -1)
                message = PostValidateMoveRoute.MULTIPLE_ERROR;

            else if (moves.contains(new Move(end, start))) {
                message = PostValidateMoveRoute.REPEAT_ERROR;
            }

            // you can only make jump from the previous piece
            else if (row != previousEnd.getRow() &&
                    col != previousEnd.getCell())
                message = PostValidateMoveRoute.DIFFERENT_ERROR;
                // after a jump, you can only jump
            else if (row - end.getRow() == 1 ||
                    row - end.getRow() == -1)
                message = PostValidateMoveRoute.MOVE_ERROR;


            // dealing with jump
            else if (row - end.getRow() == 2 ||
                    row - end.getRow() == -2){
                // there is no more possible jump
                if (! checkFourDirections(currentBoardView, start))
                    message = PostValidateMoveRoute.END_ERROR;
                // normal piece can only jump forward
                else if (row - end.getRow() == -2 && currentType != Piece.Type.KING) {
                    message = PostValidateMoveRoute.FORWARD_JUMP_ERROR;
                }
                // can jump forward by two rows
                else {
                    int xDiff = col - end.getCell();
                    int yDiff = row - end.getRow();
                    Space space = currentBoardView.getSpace(start.getRow() - (yDiff / 2),
                            start.getCell() - (xDiff / 2));
                    if (xDiff == 2 || xDiff == -2) { // can only jump left or right by two columns
                        if (space.getPiece().getColor() != activeColor) {
                            message = PostValidateMoveRoute.VALID_JUMP_MESSAGE; // valid jump
                            isJump = true;
                        }
                        else
                            message = PostValidateMoveRoute.OPPONENT_JUMP_ERROR; // you cannot jump over your own piece
                    }
                    else
                        message = PostValidateMoveRoute.ADJACENT_JUMP_ERROR; // jump is larger than 2 cols in magnitude
                }
            }
            else
                message = PostValidateMoveRoute.MAX_ROW_MESSAGE;
        }

        // done with multiple jump check
        // do this is it is regular move
        else{
            // dealing with move
            if (row - end.getRow() == 1 ||
                    row - end.getRow() == -1) {
                // you are not suppose to move if you can jump
                if (optionToJump(currentBoardView, pieces)) {
                    message = PostValidateMoveRoute.JUMP_OPTION_ERROR;
                }
                else if (row - end.getRow() == -1 && ! isKing) {
                    message = PostValidateMoveRoute.FORWARD_MOVE_ERROR; // you can only move forward
                }
                else {
                    int diff = col - end.getCell();
                    if (diff == 1 || diff == -1) {
                        message = PostValidateMoveRoute.VALID_MOVE_MESSAGE; // valid move
                        isMove = true;
                    }
                    else
                        message = PostValidateMoveRoute.ADJACENT_MOVE_ERROR; // move is larger than one col
                }
            }
            // dealing with jump
            else if (row - end.getRow() == 2 ||
                    row - end.getRow() == -2){
                // dealing with jump
                if (row - end.getRow() == -2 && currentType != Piece.Type.KING)
                    message = PostValidateMoveRoute.FORWARD_JUMP_ERROR; // normal piece can only jump forward
                else { // can jump forward by two rows
                    int xDiff = col - end.getCell();
                    int yDiff = row - end.getRow();
                    Space space = currentBoardView.getSpace(start.getRow() - (yDiff / 2),
                            start.getCell() - (xDiff / 2));
                    if (xDiff == 2 || xDiff == -2) { // can only jump left or right by two columns
                        if (space.getPiece() == null) {
                            message = PostValidateMoveRoute.EMPTY_JUMP_ERROR;
                        }
                        else if (space.getPiece().getColor() != activeColor) {
                            message = PostValidateMoveRoute.VALID_JUMP_MESSAGE; // valid jump
                            isJump = true;
                        }
                        else
                            message = PostValidateMoveRoute.OPPONENT_JUMP_ERROR; // you cannot jump over your own piece
                    }
                    else
                        message = PostValidateMoveRoute.ADJACENT_JUMP_ERROR; // jump is larger than 2 cols in magnitude
                }
            }
            else
                message = PostValidateMoveRoute.MAX_ROW_MESSAGE;
        }

        if (isMove) {
            this.pushMove(move);
            if (end.getRow() == 0) {
                currentType = Piece.Type.KING;
            }
        }
        else if (isJump){
            // add this move to the stack of move
            // so when we implement backup, we know the exact order
            this.pushMove(move);
            if (end.getRow() == 0) {
                currentType = Piece.Type.KING;
            }
            hasNextJump = checkFourDirections(currentBoardView, end);
        }

        return message;
    }
}