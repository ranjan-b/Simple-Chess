package main;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;

import javax.swing.JPanel;

import piece.Bishop;
import piece.King;
import piece.Knight;
import piece.Pawn;
import piece.Piece;
import piece.Queen;
import piece.Rook;

public class GamePanel extends JPanel implements Runnable{
	
	public static final int WIDTH = 1100;
	public static final int HEIGHT = 800;
	final int FPS = 60;
	Thread gameThread;
	Board board = new Board();
	Mouse mouse = new Mouse(); 
	
	//PIECES
	public static ArrayList<Piece> pieces = new ArrayList<>();
	public static ArrayList<Piece> simPieces = new ArrayList<>();
	ArrayList<Piece> promoPieces = new ArrayList<>();
	Piece activeP, checkingP;
	public static Piece castlingP;

	
	// COLOR
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	int currentColor = WHITE;
	
	// TIMER
	long whiteTimeLeft;
	long blackTimeLeft;
	long turnStartTime;
	
	// BOOLEANS
	boolean canMove;
	boolean validSquare;
	boolean promotion;
	boolean gameover;
	boolean stalemate;
	boolean whiteTimerRunning;
	boolean blackTimerRunning;
	
	final long TOTAL_TIME = 100 * 6 * 1000;

	
	public GamePanel() {
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setBackground(Color.BLACK);
		addMouseMotionListener(mouse);
		addMouseListener(mouse);
		
		setPieces();
	//	testPromotion();
	//	testCheckmate();
		copyPieces(pieces, simPieces);
		
		// Initializing timers
		whiteTimeLeft = TOTAL_TIME;
		blackTimeLeft = TOTAL_TIME;
		whiteTimerRunning = true;
		blackTimerRunning = false;
		turnStartTime = System.currentTimeMillis();
		
	}
	
	public void launchGame() {
		gameThread = new Thread(this);
		gameThread.start();
	}
	
	public void setPieces() {
		
		// WHITE TEAM
		pieces.add(new Pawn(WHITE,0,6));
		pieces.add(new Pawn(WHITE,1,6));
		pieces.add(new Pawn(WHITE,2,6));
		pieces.add(new Pawn(WHITE,3,6));
		pieces.add(new Pawn(WHITE,4,6));
		pieces.add(new Pawn(WHITE,5,6));
		pieces.add(new Pawn(WHITE,6,6));
		pieces.add(new Pawn(WHITE,7,6));
		pieces.add(new Rook(WHITE,0,7));
		pieces.add(new Rook(WHITE,7,7));
		pieces.add(new Knight(WHITE,1,7));
		pieces.add(new Knight(WHITE,6,7));
		pieces.add(new Bishop(WHITE,2,7));
		pieces.add(new Bishop(WHITE,5,7));
		pieces.add(new Queen(WHITE,3,7));
		pieces.add(new King(WHITE,4,7));

		
		// BLACK TEAM
		pieces.add(new Pawn(BLACK,0,1));
		pieces.add(new Pawn(BLACK,1,1));
		pieces.add(new Pawn(BLACK,2,1));
		pieces.add(new Pawn(BLACK,3,1));
		pieces.add(new Pawn(BLACK,4,1));
		pieces.add(new Pawn(BLACK,5,1));
		pieces.add(new Pawn(BLACK,6,1));
		pieces.add(new Pawn(BLACK,7,1));
		pieces.add(new Rook(BLACK,0,0));
		pieces.add(new Rook(BLACK,7,0));
		pieces.add(new Knight(BLACK,1,0));
		pieces.add(new Knight(BLACK,6,0));
		pieces.add(new Bishop(BLACK,2,0));
		pieces.add(new Bishop(BLACK,5,0));
		pieces.add(new Queen(BLACK,3,0));
		pieces.add(new King(BLACK,4,0));
		
	}
	
	public void testPromotion() {
		pieces.add(new Pawn(WHITE,0,4));
		pieces.add(new Pawn(BLACK,5,4));
	}
	
	public void testCheckmate() {
		pieces.add(new King(WHITE,4,5));
		pieces.add(new Pawn(WHITE,7,6));
		pieces.add(new King(BLACK,4,0));
		pieces.add(new Queen(BLACK,7,7));
		pieces.add(new Rook(BLACK,0,3));
	}
	
	private void copyPieces(ArrayList<Piece> source, ArrayList<Piece> target) {
		
		target.clear();
		for(int i = 0; i < source.size(); i++) {
			target.add(source.get(i));
		}
	}

	@Override
	public void run() {
		
		// GAME LOOP
		double drawInterval = 1000000000/FPS;
		double delta = 0;
		long lastTime = System.nanoTime();
		long currentTime;
		
		while(gameThread != null) {
			
			currentTime = System.nanoTime();
			
			delta += (currentTime - lastTime) / drawInterval;
			lastTime = currentTime;
			
			if(delta >= 1) {
				update();
				updateTimer();
				repaint();
				delta--;
			}
		}
	}
	
	public void update() {
		
		if(promotion) {
			promoting();
		}
		else  if(gameover == false && stalemate == false){
			/////////// MOUSE BUTTON PRESSED ////////////
			if(mouse.pressed) {
				if(activeP == null) {
					// If the activeP is null, check if you can pick up a piece
					for(Piece piece : simPieces) {
						// If the mouse is on an ally piece, pick it up as the activeP
						if(piece.color == currentColor &&
								piece.col == mouse.x/Board.SQUARE_SIZE &&
								piece.row == mouse.y/Board.SQUARE_SIZE) {
							
							activeP = piece;
						}
					}
				}
				else {
					// If the player is holding a piece, simulate the move
					simulate();
				}
			}
			
			////////// MOUSE BUTTON RELEASED //////////
			if(mouse.pressed == false) {
				if(activeP != null) {
					
					if(validSquare) {
						
						// MOVE CONFIRMED
						
						
						// Update the piece list in case a piece has been captured and removed during the simulation
						copyPieces(simPieces, pieces);
						activeP.updatePosition();
						if(castlingP != null && isKingInCheck() == false) {
							castlingP.updatePosition();
						}
						
						if(isKingInCheck() && isCheckmate()) {
							gameover = true;
							
						}
						else if(isStalemate() && isKingInCheck() == false) {
							stalemate = true;
						}
						else { // GAME continues
							if(canPromote()) {
								promotion = true;
							}
							else {
								changePlayer();
							}						
						}
					}
					else {
						// The move is not valid so reset everything
						copyPieces(pieces, simPieces);
						activeP.resetPosition();
						activeP = null;
					}
				}
			}					
		}
	}
	
	private void simulate() {
		
		canMove = false;
		validSquare = false;
		
		// Reset the piece list in every loop
		// This is basically for restoring the removed piece during the simulation
		copyPieces(pieces, simPieces);
		
		// Resetting the castling piece position
		if(castlingP != null) {
			castlingP.col = castlingP.preCol;
			castlingP.x = castlingP.getX(castlingP.col);
			castlingP = null;
		}
		
		// If a piece is being held, update it's position
		activeP.x = mouse.x - Board.HALF_SQUARE_SIZE;
		activeP.y = mouse.y - Board.HALF_SQUARE_SIZE;
		activeP.col = activeP.getCol(activeP.x);
		activeP.row = activeP.getRow(activeP.y);
		
		//Check if the piece is hovering over a reachable square
		if(activeP.canMove(activeP.col, activeP.row)) {
			
			canMove = true;
			
			// If hitting a piece, remove it from the list
			if(activeP.hittingP != null) {
				simPieces.remove(activeP.hittingP.getIndex());
			}
			
			checkCastling();
			
			if(isIllegal(activeP) == false && opponentCanCaptureKing() == false) {
				validSquare = true;
				
			}
		}				
	}
	
	private boolean isIllegal(Piece king) {
		
		if(king.type == Type.KING) {
			for(Piece piece : simPieces) {
				if(piece != king && piece.color != king.color && piece.canMove(king.col,  king.row)) {
					return true;
				}
			}
		}
		return false;		
	}
	
	private boolean opponentCanCaptureKing() {
		
		Piece king = getKing(false);
		
		for(Piece piece : simPieces) {
			if(piece.color != king.color && piece.canMove(king.col, king.row)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isKingInCheck() {
		
		Piece king = getKing(true);
		
		if(activeP.canMove(king.col, king.row)) {
			checkingP = activeP;
			return true;
		}
		else {
			checkingP = null;
		}	
		return false;
	}
	
	private Piece getKing(boolean opponent) {
		
		Piece king = null;
		
		for(Piece piece : simPieces) {
			if(opponent) {
				if(piece.type == Type.KING && piece.color != currentColor) {
					king = piece;
				}
			}
			else {
				if(piece.type == Type.KING && piece.color == currentColor) {
					king = piece;
				}
			}
		}
		return king;
	}
	
	private void checkCastling() {
		
		if(castlingP != null) {
			if(castlingP.col == 0) {
				castlingP.col += 3;
			}
			else if(castlingP.col == 7) {
				castlingP.col -= 2;
			}
			castlingP.x = castlingP.getX(castlingP.col);
		}
		
	}
	
	private boolean isStalemate() {
		
		int count = 0;
		// Count the number of pieces
		for(Piece piece : simPieces) {
			if(piece.color != currentColor) {
				count++;
			}
		}
		
		// If only king is left
		if(count == 1) {
			if(kingCanMove(getKing(true)) == false) {
				return true;
			}
		}	
		return false;
	}
	
	private boolean isCheckmate() {
		
		Piece king = getKing(true);
		
		if(kingCanMove(king)) {
			return false;
		}
		else {
			
			// if you still have a chance
			// Check if you can block the attack with your other piece
			
			// Check the postion of the checking piece and the king in check
			int colDiff = Math.abs(checkingP.col - king.col);
			int rowDiff = Math.abs(checkingP.row - king.row);
			
			if(colDiff == 0) {
				// The checking piece is attacking vertically
				if(checkingP.row < king.row) {
					// The checking piece is above the king
					for(int row = checkingP.row + 1; row < king.row; row++) {
						for(Piece piece : simPieces) {
							if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col,  row)) {
								return false;
							}
						}
					}
				}
				if(checkingP.row > king.row) {
					// The checking piece is below the king
					for(int row = checkingP.row - 1; row > king.row; row--) {
						for(Piece piece : simPieces) {
							if(piece != king && piece.color != currentColor && piece.canMove(checkingP.col,  row)) {
								return false;
							}
						}
					}
				}
			}
			else if(rowDiff == 0) {
				// The checking piece is attacking horizontally
				if(checkingP.col < king.col) {
					// The checking piece is to the left
					for(int col = checkingP.col + 1; col < king.col; col++) {
						for(Piece piece : simPieces) {
							if(piece != king && piece.color != currentColor && piece.canMove(col,  checkingP.row)) {
								return false;
							}
						}
					}
				}
				if(checkingP.col > king.col) {
					// The checking piece is to the right
					for(int col = checkingP.col - 1; col > king.col; col--) {
						for(Piece piece : simPieces) {
							if(piece != king && piece.color != currentColor && piece.canMove(col,  checkingP.row)) {
								return false;
							}
						}
					}
				}				
			}
			else if(colDiff == rowDiff) {
				// The checking piece is attacking diagonally
				if(checkingP.row < king.row) {
					// The checking piece is above the king
					if(checkingP.col < king.col) {
						// The checking piece is in the upper left
						for(int col = checkingP.col + 1, row = checkingP.row; col < king.col; col++, row++) {
							for(Piece piece : simPieces) {
								if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
									return true;
								}
							}
						}
					}
					if(checkingP.col > king.col) {
						// The checking piece is in the upper right
						for(int col = checkingP.col - 1, row = checkingP.row; col > king.col; col--, row++) {
							for(Piece piece : simPieces) {
								if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
									return true;
								}
							}
						}
					}
				}
				if(checkingP.row > king.row) {
					// The checking piece is below the king
					if(checkingP.col < king.col) {
						// The checking piece is in the lower left
						for(int col = checkingP.col + 1, row = checkingP.row; col < king.col; col++, row--) {
							for(Piece piece : simPieces) {
								if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
									return true;
								}
							}
						}
					}
					if(checkingP.col > king.col) {
						// The checking piece is in teh lower right
						for(int col = checkingP.col - 1, row = checkingP.row; col > king.col; col--, row++) {
							for(Piece piece : simPieces) {
								if(piece != king && piece.color != currentColor && piece.canMove(col, row)) {
									return true;
								}
							}
						}
					}
				}
			}
			else {
				// The checking piece is Knight
				for (Piece piece : simPieces) {
					if(piece != king && piece.color == king.color && piece.canMove(checkingP.col, checkingP.row)) {
						return false;
					}
				}		
			}
		}		
		return true; 
	}
	
	private boolean kingCanMove(Piece king) {
		
		// Simulate if there is any square the king can move to
		if(isValidMove(king, -1, -1)) {return true;}
		if(isValidMove(king, 0, -1)) {return true;}
		if(isValidMove(king, 1, -1)) {return true;}
		if(isValidMove(king, -1, 0)) {return true;}
		if(isValidMove(king, 1, 0)) {return true;}
		if(isValidMove(king, -1, 1)) {return true;}
		if(isValidMove(king, 0, 1)) {return true;}
		if(isValidMove(king, 1, 1)) {return true;}
		
		return false;
		
	}
	
	private boolean isValidMove(Piece king, int colPlus, int rowPlus) {
		
		boolean isValidMove = false;
		
		// Update the king's position
		king.col += colPlus;
		king.row += rowPlus;
		
		if(king.canMove(king.col, king.row)) {
			
			if(king.hittingP != null) {
				simPieces.remove(king.hittingP.getIndex());
			}
			if(isIllegal(king) == false) {
				isValidMove = true;
			}
		}
		// Reset the king's position and restore the removed piece
		king.resetPosition();
		copyPieces(pieces, simPieces);
		
		return isValidMove;
	}
	
	public void updateTimer() {
		if(gameover || stalemate) {
			return; // Stop updating timer if the game is over
		}
		long currentTime = System.currentTimeMillis();
		long elapsedTime = currentTime - turnStartTime;
		
		if(currentColor == WHITE) {
			if(whiteTimerRunning) {
				whiteTimeLeft -= elapsedTime;
				if(whiteTimeLeft <= 0) {
					whiteTimeLeft = 0;
					gameover = true; // Handles white losing due to timeout
				}
			}
		}
		else {
			if(blackTimerRunning) {
				blackTimeLeft -= elapsedTime;
				if(blackTimeLeft <= 0) {
					blackTimeLeft = 0;
					gameover = true; // Handles black losing due to timeout
				}
			}
		}
		turnStartTime = currentTime;
	}
	
	public void changePlayer() {
		
		if(currentColor == WHITE) {
			currentColor = BLACK;
			whiteTimerRunning = false;
			blackTimerRunning = true;
			// Reset black's two stepped status
			for(Piece piece : pieces) {
				piece.twoStepped = false;
			}
		}
		else {
			currentColor = WHITE;
			// Reset white's two stepped status
			blackTimerRunning = false;
			whiteTimerRunning = true;
			for(Piece piece : pieces) {
				if(piece.color == WHITE) {
					piece.twoStepped = false;
				}
			}
		}
		activeP = null;
		turnStartTime = System.currentTimeMillis();
	}
	
	private boolean canPromote() {
		
		if(activeP.type == Type.PAWN) {
			if(currentColor == WHITE && activeP.row == 0 || currentColor == BLACK && activeP.row == 7) {
				promoPieces.clear();
				promoPieces.add(new Rook(currentColor,9,2));
				promoPieces.add(new Knight(currentColor,9,3));
				promoPieces.add(new Bishop(currentColor,9,4));
				promoPieces.add(new Queen(currentColor,9,5));
				return true;
			}
		}
		
		
		return false;
	}
	
	public void promoting() {
		
		if(mouse.pressed) {
			for(Piece piece : promoPieces) {
				if(piece.col == mouse.x/Board.SQUARE_SIZE && piece.row == mouse.y/Board.SQUARE_SIZE) {
					switch(piece.type) {
						case ROOK: simPieces.add(new Rook(currentColor, activeP.col, activeP.row)); break;
						case KNIGHT: simPieces.add(new Knight(currentColor, activeP.col, activeP.row)); break;
						case BISHOP: simPieces.add(new Bishop(currentColor, activeP.col, activeP.row)); break;
						case QUEEN: simPieces.add(new Queen(currentColor, activeP.col, activeP.row)); break;
						default: break;
					}
					
					simPieces.remove(activeP.getIndex());
					copyPieces(simPieces, pieces);
					activeP = null;
					promotion = false;
					changePlayer();
				}
			}
		}	
	}
	
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		
		Graphics2D g2 = (Graphics2D)g;
		
		// BOARD
		board.draw(g2);
		
		// PIECES
		for(Piece p : simPieces) {
			p.draw(g2);
		}
		
		if(activeP != null) {
			if(canMove) {
				if(isIllegal(activeP) || opponentCanCaptureKing()) {
					g2.setColor(Color.red);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7F));
					g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE,
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}
				else {
					g2.setColor(Color.green);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7F));
					g2.fillRect(activeP.col*Board.SQUARE_SIZE, activeP.row*Board.SQUARE_SIZE,
							Board.SQUARE_SIZE, Board.SQUARE_SIZE);
					g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
				}	
			}
			
			// Draw the active piece in the end it won't be hidden by the board or the colored square
			activeP.draw(g2);
		}	
		
		// STATUS MESSAGES 
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g2.setFont(new Font("Algerian", Font.PLAIN, 40));
		g2.setColor(Color.WHITE);
		
		if(promotion) {
			
			g2.drawString("Promote to: ", 840, 150);
			for(Piece piece : promoPieces) {
				g2.drawImage(piece.image, piece.getX(piece.col), piece.getY(piece.row), 
						Board.SQUARE_SIZE, Board.SQUARE_SIZE, null);
			}
		}
		else {
			if(currentColor == WHITE) {
				g2.drawString("WHITE's TURN", 820, 550);
				if(checkingP != null && checkingP.color == BLACK) {
					g2.setColor(Color.red);
					g2.drawString("The King", 840, 400);
					g2.drawString("is in Check!", 840, 450);
				}
			}
			else {
				g2.drawString("BLACK's TURN", 820, 250);
				if(checkingP != null && checkingP.color == WHITE) {
					g2.setColor(Color.red);
					g2.drawString("The King", 840, 400);
					g2.drawString("is in Check!", 840, 450);
				}
			}	
		}
		
		if(gameover || stalemate) {
			String s = "";
			if(currentColor == WHITE) {
				s = "White Wins";
			}
			else {
				s = "Black Wins";
			}
			g2.setFont(new Font("Engravers MT", Font.PLAIN, 80));
			g2.setColor(Color.GREEN);
			//g2.drawString("CHECKMATE", 200, 460);
			g2.drawString(s, 135, 390);
			
		}
		
		//Display the remaining time for each player
		g2.setFont(new Font("Stencil", Font.PLAIN, 25));
		g2.setColor(Color.WHITE);
		g2.drawString("White Time: " + formatTime(whiteTimeLeft), 840, 30);
		g2.drawString("Black Time: " + formatTime(blackTimeLeft), 840, 60);
		
	}

	private String formatTime(long timeMillis) {
		
		long seconds = (timeMillis/1000) % 60;
		long minutes = (timeMillis/1000) / 60;
		return String.format("%02d:%02d", minutes, seconds);
	}		
}
