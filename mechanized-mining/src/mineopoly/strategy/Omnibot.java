package mineopoly.strategy;

import mineopoly.game.Economy;
import mineopoly.game.TurnAction;
import mineopoly.item.InventoryItem;
import mineopoly.tiles.TileType;
import mineopoly.util.DistanceUtil;
import mineopoly.strategy.MinePlayerStrategy;
import mineopoly.strategy.PlayerBoardView;
import java.awt.*;
import java.util.*;

public class Omnibot implements MinePlayerStrategy {
	private static final int MINE_TYPE = 1;
	private static final int MOVE_ACTION = 4;
	private int boardSize;
	private int maxInventorySize;
	private Point currentLocation;
	private Random random;
	private int inventoryIterator = 0;
	private Point market0;
	private Point market1;
	/**
	 * Called at the start of every round
	 * @param boardSize         The length and width of the square game board
	 * @param maxInventorySize  The maximum number of items that your player can carry at one time
	 * @param winningScore      The first player to reach this score wins the round
	 * @param startTileLocation A Point representing your starting location in (x, y) coordinates
	 *                          (0, 0) is the bottom left and (boardSize - 1, boardSize - 1) is the top right
	 * @param isRedPlayer       True if this strategy is the red player, false otherwise
	 * @param random            A random number generator, if your strategy needs random numbers you should use this.
	 */
	@Override
	public void initialize(int boardSize, int maxInventorySize, int winningScore, Point startTileLocation, boolean isRedPlayer, Random random) {
		this.boardSize = boardSize;
		this.maxInventorySize = maxInventorySize;
		this.currentLocation = startTileLocation;
		this.random = random;
		int halfBoardSize = boardSize / 2;
		if (isRedPlayer) {
			market0 = new Point(halfBoardSize, halfBoardSize);
			market1 = new Point(halfBoardSize - 1, halfBoardSize - 1);
		} else {
			market0 = new Point(halfBoardSize - 1, halfBoardSize);
			market1 = new Point(halfBoardSize, halfBoardSize - 1);
		}
	}
	
	/**
	 * The main part of your strategy, this method returns what action your player should do on this turn
	 * @param boardView A PlayerBoardView object representing all the information about the board and the other player
	 *                  that your strategy is allowed to access
	 * @param economy   The GameEngine's economy object which holds current prices for resources
	 * @param isRedTurn For use when two players attempt to move to the same spot on the same turn
	 *                  If true: The red player will move to the spot, and the blue player will do nothing
	 *                  If false: The blue player will move to the spot, and the red player will do nothing
	 * @return The TurnAction that this strategy wants to perform on this game turn
	 */
	@Override
	public TurnAction getTurnAction(PlayerBoardView boardView, Economy economy, boolean isRedTurn) {
		currentLocation = boardView.getYourLocation();
		if (inventoryIterator >= maxInventorySize) {
			return goToMarket();
		} else if (boardView.getItemsOnGround().containsValue(boardView.getYourLocation())) {
			return TurnAction.PICK_UP;
		} else if (boardView.getTileTypeAtLocation(currentLocation).ordinal() > MINE_TYPE) {
			return TurnAction.MINE;
		} else {
			return goToNearestMine(boardView);
		}
	}
	
	/**
	 * Called when the player receives an item from performing a TurnAction that gives an item.
	 * At the moment this is only from using PICK_UP on top of a mined resource
	 * @param itemReceived The item received from the player's TurnAction on their last turn
	 */
	@Override
	public void onReceiveItem(InventoryItem itemReceived) {
		inventoryIterator++;
	}
	
	/**
	 * Called when the player steps on a market tile with items to sell. Tells your strategy how much all
	 * of the items sold for.
	 * @param totalSellPrice The combined sell price for all items in your strategy's inventory
	 */
	@Override
	public void onSoldInventory(int totalSellPrice) {
		inventoryIterator = 0;
	}
	
	/**
	 * Gets the name of this strategy. The amount of characters that can actually be displayed on a screen varies,
	 * although by default at screen size 750 it's about 16-20 characters depending on character size
	 * @return The name of your strategy for use in the competition and rendering the scoreboard on the GUI
	 */
	@Override
	public String getName() {
		return "Omnibot";
	}
	
	/**
	 * Called at the end of every round to let players reset, and tell them how they did if the strategy does not
	 * track that for itself
	 * @param pointsScored         The total number of points this strategy scored
	 * @param opponentPointsScored The total number of points the opponent's strategy scored
	 */
	@Override
	public void endRound(int pointsScored, int opponentPointsScored) {
	
	}

	private TurnAction goTo(Point p) {
		return goTo(p.x, p.y);
	}

	private TurnAction goTo(int x, int y) {
		if (currentLocation.x > x) {
			return TurnAction.MOVE_LEFT;
		} else if (currentLocation.x < x) {
			return TurnAction.MOVE_RIGHT;
		}

		if (currentLocation.y > y) {
			return TurnAction.MOVE_DOWN;
		} else if (currentLocation.y < y) {
			return TurnAction.MOVE_UP;
		}

		return null;
	}
	
	private TurnAction goToMarket() {
		Point closerMarket;
		if (DistanceUtil.getManhattanDistance(currentLocation, market0) < DistanceUtil.getManhattanDistance(currentLocation, market1)) {
			closerMarket = market0;
		} else {
			closerMarket = market1;
		}
		
		return goTo(closerMarket);
	}

	private TurnAction goToNearestMine(PlayerBoardView board) {
		TileType tileType;
		int shortestDistance = boardSize;
		int closestMineX = 0;
		int closestMineY = 0;
		for (int radius = 0; radius <= shortestDistance; radius++) {
			for (int x = currentLocation.x - radius; x <= currentLocation.x + radius; x++) {
				for (int y = currentLocation.y - radius; y <= currentLocation.y + radius; y++) {
					tileType = board.getTileTypeAtLocation(x, y);
					if (tileType != null && tileType.ordinal() > MINE_TYPE && DistanceUtil.getManhattanDistance(currentLocation.x, currentLocation.y, x, y) < shortestDistance) {
						shortestDistance = DistanceUtil.getManhattanDistance(currentLocation.x, currentLocation.y, x, y);
						closestMineX = x;
						closestMineY = y;
					}
				}
			}
		}

		if (shortestDistance < boardSize) {
			return goTo(closestMineX, closestMineY);
		}

		return TurnAction.values()[random.nextInt(MOVE_ACTION)];
	}
}
