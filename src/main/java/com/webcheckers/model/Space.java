package com.webcheckers.model;

public class Space {

    //Attributes
    public int cellIdx;
    private int cellIdy;
    private boolean isValid;
    private Piece piece;

    /**
     * Space Constructor, sets the x-location, y-location, validity boolean and whether or not there is a piece located on it.
     * @param cellIdx   - x-location of the space
     * @param cellIdy   - y-location of the space
     * @param isValid   - whether or not the space is valid
     * @param piece     - the current piece on the space, null if none.
     */
    public Space(int cellIdx, int cellIdy, boolean isValid, Piece piece) {
        this.cellIdx = cellIdx;
        this.cellIdy = cellIdy;
        this.isValid = isValid;
        this.piece = piece;
    }

    /**
     * X-location getter function
     * @return x-location of the space
     */
    public int getCellIdx() {
        return cellIdx;
    }

    /**
     * Validity getter function
     * @return validity of the space
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * Change the validity attribute to a set boolean.
     * @param isValid   - the new validity
     */
    public void changeValid(boolean isValid) {
        this.isValid = isValid;
    }

    /**
     * Piece getter function
     * @return  - the piece currently at the space, null if none.
     */
    public Piece getPiece() {
        return piece;
    }

    /**
     * Set the piece to be located at the space.
     * @param piece
     */
    public void setPiece(Piece piece) {
        this.piece = piece;
    }
}
