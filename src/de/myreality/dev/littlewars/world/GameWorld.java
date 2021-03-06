/**
 * This file was written by Miguel Gonzalez and is part of the
 * game "LittleWars". For more information mail to info@my-reality.de
 * or visit the game page: http://dev.my-reality.de/littlewars
 * 
 * Game world tiled map with daytime and weather support
 * 
 * @version 	0.2
 * @author 		Miguel Gonzalez		
 */

package de.myreality.dev.littlewars.world;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Shape;
import org.newdawn.slick.tiled.TiledMap;
import org.newdawn.slick.util.pathfinding.AStarPathFinder;
import org.newdawn.slick.util.pathfinding.Path;
import org.newdawn.slick.util.pathfinding.PathFindingContext;
import org.newdawn.slick.util.pathfinding.TileBasedMap;

import de.myreality.dev.littlewars.components.Debugger;
import de.myreality.dev.littlewars.components.MovementCalculator;
import de.myreality.dev.littlewars.components.SpawnArea;
import de.myreality.dev.littlewars.components.helpers.CursorHelper;
import de.myreality.dev.littlewars.components.resources.ResourceManager;
import de.myreality.dev.littlewars.game.IngameState;
import de.myreality.dev.littlewars.gui.GUIObject;
import de.myreality.dev.littlewars.ki.Player;
import de.myreality.dev.littlewars.objects.ArmyUnit;
import de.myreality.dev.littlewars.objects.Camera;
import de.myreality.dev.littlewars.objects.CommandoCenter;
import de.myreality.dev.littlewars.objects.Movable;
import de.myreality.dev.littlewars.objects.TileObject;
import de.myreality.dev.littlewars.objects.cyborg.CyborgCommandoCenter;

public class GameWorld extends TiledMap implements TileBasedMap, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Hover tileObject
	private TileObject hoverObject;

	// Clicked tileObject
	private TileObject clickedObject;
	
	// Focused tileObject
	private TileObject focusObject;
	
	// Collision array
	private boolean[][] collisions;
	
	// Unit detections
	private Map<Integer, ArmyUnit[][]> unitCollisions;
	
	// Players
	private IngameState game;
	
	// Game camera
	private Camera cam;	
	
	// Zoom factor
	private float zoom = 1f;
	
	// Weather on the map
	private Weather weather;
	
	// Daytime on the map
	private Daytime daytime;
	
	private GUIObject bottomMenu;
	
	// Name of the world
	private String name;
	
	// PathFinder
	AStarPathFinder pathFinder;
	Path unitPath;
	
	// GameContainer
	GameContainer gc;

	public GameWorld(String name, String ref, GameContainer gc, IngameState game) throws SlickException {
		super(ref);
		setWeather(new Weather(Weather.NORMAL));
		this.game = game;
		this.cam = new Camera(this, gc);
		this.name = name;
		this.daytime = new Daytime(Daytime.DAY);
		this.gc = gc;		
	
		daytime.start();
		pathFinder = new AStarPathFinder(this, 100, false);	
		buildCollisionArray();
	}
	
	public IngameState getGame() {
		return game;
	}
	
	
	public void finalize() {
		close();		
		System.out.println("World deleted.");
	}
	
	public AStarPathFinder getPathFinder() {
		return pathFinder;
	}
	
	public void close() {
		Debugger.getInstance().write("Release resources..");
		ResourceManager.getInstance().releaseAnimationSource();
	}

	public float getZoom() {
		return zoom;
	}
	
	public void addRenderTarget(ArmyUnit obj, GameContainer gc) {		
		game.addUnitInfo(obj, gc);
	}
	
	public void removeRenderTarget(ArmyUnit obj) {		
		game.removeUnitInfo(obj);
		if (focusObject != null && focusObject.equals(obj)) {
			focusObject = null;
		}
		if (hoverObject != null && hoverObject.equals(obj)) {
			hoverObject = null;
		}
		
		// Update collision arrays
		unitCollisions.get(obj.getPlayer().getId())[obj.getLastTileX()][obj.getLastTileY()] = null;
		unitCollisions.get(obj.getPlayer().getId())[obj.getTileX()][obj.getTileY()] = null;
		collisions[obj.getLastTileX()][obj.getLastTileY()] = false;
		collisions[obj.getTileX()][obj.getTileY()] = false;
		
	}
	
	public Camera getCamera() {
		return cam;
	}
	

	public void loadConfiguration(GameContainer gc) throws SlickException {		

		for(int i = 0; i < getLayerCount(); ++i) {
	    	for (int count = 0; count < getObjectCount(i); ++count) {
    		
	    		String type = getObjectType(i, count);
	    		int playerID = Player.NONE;
	    		if (type.equals("player1")) {
	    			playerID = Player.PL1;
	    		} else if (type.equals("player2")) {
	    			playerID = Player.PL2;
	    		} else if (type.equals("player3")) {
	    			playerID = Player.PL3;
	    		} else if (type.equals("player4")) {
	    			playerID = Player.PL4;
	    		}	
	    		
	    		// Add the commando center
	    		for (Player player : game.getPlayers()) {
	    			if (player.getId() == playerID) {
	    				CommandoCenter center = new CyborgCommandoCenter(getObjectX(i, count), getObjectY(i, count), gc, cam, game);
	    				player.addArmyUnit(center);
	    				player.addCommandoCenter(center);
	    				player.getMoney().addCredits(Integer.valueOf(getObjectProperty(i, count, "money", "0")));
	    				break;
	    			}
	    		}
	    	}
		}
		
		buildCollisionArray();
	}
	
	public int tileIndexX(float x) {
		return (int) (x / getTileWidth());
	}
	
	public int tileIndexY(float y) {
		return (int) (y / getTileHeight());
	}
	
	private void buildCollisionArray() {
		collisions = new boolean[getWidth()][getHeight()];
		unitCollisions = new TreeMap<Integer, ArmyUnit[][]>();
		for (int x = 0; x < getWidth(); x++) {
			for (int y = 0; y < getHeight(); y++) {
				String value = "false";
				for (int count = 0; count < getLayerCount(); count++) {
					int tileID = getTileId(x, y, count);
					if (value.equals("false")) {
						value = getTileProperty(tileID, "blocked", "false");
					} else break;
				}
				if ("true".equals(value)) {					
					collisions[x][y] = true;
				}
			}
		}
		
		for (Player player : game.getPlayers()) {
			ArmyUnit[][] tempCollisions = new ArmyUnit[getWidth()][getHeight()];
			for (int x = 0; x < getWidth(); x++) {
				for (int y = 0; y < getHeight(); y++) {
					tempCollisions[x][y] = null;
				}
			}
			// Add the data
			unitCollisions.put(player.getId(), tempCollisions);
		}
	}
	

	
	public boolean isBlocked(TileObject obj, int direction) {
	
		switch (direction) {
			case Movable.TOP:
				return collisionExists(obj.getTileX(), obj.getTileY() - 1);
			case Movable.BOTTOM:
				return collisionExists(obj.getTileX(), obj.getTileY() + 1);
			case Movable.LEFT:
				return collisionExists(obj.getTileX() - 1, obj.getTileY());
			case Movable.RIGHT:
					return collisionExists(obj.getTileX() + 1, obj.getTileY());
		}
		
		return false;		 
	}


	
	public void render(GameContainer gc, Graphics g) {			
		drawLayers((int)cam.getX(), (int)cam.getY(), cam.getWidth(), cam.getHeight(), g, gc);	
		daytime.draw(gc, g);
		if (focusObject != null && focusObject instanceof ArmyUnit) {
			ArmyUnit unit = (ArmyUnit)focusObject;
			if (unitPath != null && unit.getPlayer().isCurrentPlayer()) {
				
				if (!unit.isDead()) {
					MovementCalculator.drawUnitPath(g, unit, unitPath, game);
				}
			}
		}
		
		// Draw the render radius of the current player
		game.getCurrentPlayer().getSpawnArea().draw(g);
	}
	
	private void renderDebugInformation(Graphics g) {		
		
		g.setColor(new Color(255, 0, 0, 0.5f));
		for (int x = 0; x < collisions.length; ++x) {
			for (int y = 0; y < collisions[x].length; ++y) {
				if (collisions[x][y]) {
					g.fillRect(x * 32 - cam.getX(), y * 32 - cam.getY(), 32, 32);
				}
			}
		}
	}
	
	public boolean isEnemyUnit(Player current, int tileX, int tileY) {
		
		boolean found = false;
		
		for (Entry<Integer, ArmyUnit[][]> entry : unitCollisions.entrySet()) {
			if (current.getId() != entry.getKey()) {
				found = entry.getValue()[tileX][tileY] != null;
			}
			
			if (found) {
				break;
			}
		}
		return found;
	}
	
	public void update(GameContainer gc, int delta) throws SlickException {		
		// Unit state reset (each frame)
		ArmyUnit.setUnitMoving(false);
		ArmyUnit.setUnitDying(false);
		ArmyUnit.setUnitLoosingLife(false);
		ArmyUnit.setUnitBusy(false);
		daytime.update(gc, delta);
		Input input = gc.getInput();
		int padding = 40;	
		int bottomHeight = 0;
		int topHeight = 0;
		@SuppressWarnings("unused")
		TileObject movingObject = null;
		
		if (bottomMenu != null) {
			bottomHeight = bottomMenu.getHeight();
		}
		
		//if (cam.getTopMenu() != null) {
			//topHeight = cam.getTopMenu().getHeight();
		//}
		
		// TODO: Top Menu camera moving
		if (bottomMenu != null && !bottomMenu.isMouseOver()/* && !cam.getTopMenu().isHover()*/) {
			if (input.getMouseY() < padding) {
				cam.move(Movable.TOP, delta);			
			} else {
				if (input.getMouseY() > gc.getHeight() - padding - bottomHeight - topHeight) {
					cam.move(Movable.BOTTOM, delta);
				}
			}
		} 
		
		if (bottomMenu != null && !bottomMenu.isMouseOver()) {
			if (input.getMouseX() < padding) {
				cam.move(Movable.LEFT, delta);
			} else if (input.getMouseX() > gc.getWidth() - padding) {
				cam.move(Movable.RIGHT, delta);
			} else {
				cam.move(-1, delta);
			}
		} else if(bottomMenu == null) {
			if (input.getMouseX() < padding) {
				cam.move(Movable.LEFT, delta);
			} else if (input.getMouseX() > gc.getWidth() - padding) {
				cam.move(Movable.RIGHT, delta);
			} else {
				cam.move(-1, delta);
			}
		}
		
		int worldWidth = getWidth() * getTileWidth();
		int worldHeight = getHeight() * getTileHeight();
		
		if (cam.getY() < 0) {
			cam.setY(0);
		}
		
		if (cam.getX() < 0) {
			cam.setX(0);
		}
		
		if (cam.getX() + cam.getWidth() > worldWidth) {
			cam.setX(worldWidth - gc.getWidth());
		}
		
		
		if (cam.getY() + cam.getHeight() + topHeight > worldHeight) {
			cam.setY(worldHeight - (gc.getHeight() - bottomHeight - topHeight));
		} 	

		// Objekte updaten
		boolean hover = false;
		boolean clicked = false;
		
		for (Player player : game.getPlayers()) {
			Iterator<ArmyUnit> itr = player.getUnits().iterator();
			while (itr.hasNext()) {
				TileObject target = itr.next();
				target.update(delta); 	 
				ArmyUnit unit = (ArmyUnit) target;
				if (unit.isDead() && !unit.isDying()) {
					break;
				}
				
				if (unit.onMouseClick()) {
					//unit.rankUp();
				}
			
				if (!target.isTargetArrived()) {
					movingObject = target;
				}
				
				if (!bottomMenu.isMouseOver()) {
	    			if (target.isMouseOver()) {
	    				hoverObject = target;
	    				hover = true;
	    			}	    
	    			
	    			// Solved the attack problem: only focus, when target is NOT a potential enemy unit
	    			if (!clicked && unit.onMouseClick()
	    			    && unit.getPlayer().equals(game.getClientPlayer())) {
	    				clicked = true;
	    				clickedObject = unit;
	    				focusObject = unit;
	    				focusCameraOnObject(unit, gc);
	    			}
				}
				
				if (target.getLastTileX() < getWidth() && target.getTileX() < getWidth() &&
					target.getLastTileY() < getHeight() && target.getTileY() < getHeight()) {
					try {
						// Collision array
						collisions[target.getLastTileX()][target.getLastTileY()] = false;
						collisions[target.getTileX()][target.getTileY()] = true;
						// Player array in map
						unitCollisions.get(player.getId())[target.getLastTileX()][target.getLastTileY()] = null;
						unitCollisions.get(player.getId())[target.getTileX()][target.getTileY()] = (ArmyUnit)target;
						
					} catch (ArrayIndexOutOfBoundsException e) {
						Debugger.getInstance().write("Error: Object " + target.toString() + " is out of world");
						target.stop();
					}
				}
				
				// Set the cursor that depends on the unit state
				boolean isPlayerUnit = getFocusObject() != null && ((ArmyUnit)getFocusObject()).getPlayer().isClientPlayer() && !(getFocusObject() instanceof CommandoCenter);
				if (game.getPhaseID() != IngameState.PREPERATION && !unit.getPlayer().equals(game.getClientPlayer()) && unit.isMouseOver() && isPlayerUnit) {
					CursorHelper.getInstance().setCursor(ResourceManager.getInstance().getImage("CURSOR_ATTACK"));
				} else if (unit.isMouseOver()) {
					CursorHelper.getInstance().setCursor(ResourceManager.getInstance().getImage("CURSOR_HOVER"));
				} else if (unit.onMouseOut()) {
					CursorHelper.getInstance().setCursor(ResourceManager.getInstance().getImage("CURSOR_DEFAULT"));
				}
				
			}
		}
		
		if (!hover && !bottomMenu.isMouseOver()) {
			hoverObject = null;    				
		}	  
		
		if (clickedObject != null && !clicked && !bottomMenu.isMouseOver()) {
			clickedObject = null;	    			
    	}
		
		if (input.isKeyDown(Input.KEY_ESCAPE) && game.getCurrentPlayer().isClientPlayer()) {
			focusObject = null;
		}
		
		if (focusObject != null && !focusObject.isTargetArrived()) {
			game.getWorld().focusCameraOnObject(focusObject, gc, true);
		}
		
		cam.update(delta);
		SpawnArea spawnArea = game.getCurrentPlayer().getSpawnArea();
		
		// Update the render radius of the current player
		boolean isEnabled = (game.getPhaseID() == IngameState.PREPERATION || game.getPhaseID() == IngameState.INIT) && game.isPreviewSelected();
							 
		spawnArea.setVisible(isEnabled);
	}
	
	
	public ArmyUnit getArmyUnitAt(int x, int y) {
		for (Entry<Integer, ArmyUnit[][]> entry : unitCollisions.entrySet()) {
			if (entry.getValue()[x][y] != null) {
				return entry.getValue()[x][y];
			}
		}
		return null;
	}
	
		
	public TileObject getFocusObject() {
		return focusObject;
	}

	private void drawLayers(int x, int y, int w, int h, Graphics g, GameContainer gc) {		
	    int tileOffsetX = (-1 * x % getTileWidth());
	    int tileOffsetY = (-1 * y % getTileHeight());
	    int tileIndexX  = x / getTileWidth();
	    int tileIndexY  = y / getTileHeight();
	    //Input input = gc.getInput();   
	   
	    for(int i = 0; i < getLayerCount(); ++i) {
	    	// Objects
	    	//g.translate(cam.getX(), cam.getY());	    	
	    		    	
	    	/*for (int tileX = tileIndexX; tileX < w / getTileWidth(); tileX++) {
	    		for (int tileY =  tileIndexY; tileY < h / getTileWidth(); tileY++) {
	    			Image image = this.getTileImage(tileX, tileY, i);
	    			if (image != null) {
	    				image.setColor(Image.TOP_LEFT, lightValue[tileX][tileY][0], lightValue[tileX][tileY][1], lightValue[tileX][tileY][2], 1);
	    				image.setColor(Image.TOP_RIGHT, lightValue[tileX+1][tileY][0], lightValue[tileX+1][tileY][1], lightValue[tileX+1][tileY][2], 1);
	    				image.setColor(Image.BOTTOM_RIGHT, lightValue[tileX+1][tileY+1][0], lightValue[tileX+1][tileY+1][1], lightValue[tileX+1][tileY+1][2], 1);
	    				image.setColor(Image.BOTTOM_LEFT, lightValue[tileX][tileY+1][0], lightValue[tileX][tileY+1][1], lightValue[tileX][tileY+1][2], 1);	  

	    				image.draw(tileX * getTileWidth(), tileY + getTileHeight(), getTileWidth(), getTileHeight());
	    			}
		    	}
	    	}*/
	    	
	    	
	    	render( tileOffsetX, tileOffsetY, tileIndexX, tileIndexY,
	                    (int)(((w - tileOffsetX) / getTileWidth() + 1) * (1 / zoom)),
	                    (int)(((h - tileOffsetY)/ getTileHeight() + 1) * (1 / zoom)),
	                    i, false);
	    	
	    	
	    	if (i == 0) {
	    		for (Player player : game.getPlayers()) {
	    			for (Iterator<ArmyUnit> itr = player.getUnits().iterator(); itr.hasNext();) {  
	    				TileObject target = itr.next();
		    			Rectangle rect = (Rectangle) getCamera().getArea();
		    			rect.setX(0);
		    			rect.setY(0);
		    			Shape objArea = target.getArea();
		    			float oldX = objArea.getX(), oldY = objArea.getY();
		    			objArea.setLocation(objArea.getX() - getCamera().getX(), objArea.getY() - getCamera().getY());
		    			if (rect.intersects(objArea)) {
		    				// Draw tile background
		    				if (target.equals(focusObject)) {
		    					g.setColor(new Color(50, 150, 0, 50));
		    					g.fillRect(objArea.getX(), objArea.getY(), getTileWidth(), getTileHeight());
		    				} 
		    				
		    				target.draw(g);
		    			}
		    			objArea.setLocation(oldX, oldY);	  
	    			}
	    		}
	    	} else if (i == 1) {
	    		if (!bottomMenu.isMouseOver() && !cam.isSmoothMoving() && !cam.hasMoved()) {
	    			int curX = tileIndexX(gc.getInput().getMouseX() - tileOffsetX);
	    			int curY = tileIndexY(gc.getInput().getMouseY() - tileOffsetY);
	    			int curTotalX = tileIndexX(cam.getX() + gc.getInput().getMouseX());
	    			int curTotalY = tileIndexY(cam.getY() + gc.getInput().getMouseY());
	    			
	    			boolean foundFocusObject = false;
	    			for (ArmyUnit center : game.getClientPlayer().getCommandoCenters()) {
	    				if (focusObject != null && focusObject.equals(center)) {
	    					foundFocusObject = true;
	    					break;
	    				}
	    			}
	    			
	    			if (game.getPhaseID() == IngameState.PREPERATION || (unitPath == null && (focusObject == null)) || foundFocusObject) {
		    			if (!collisionExists(curTotalX, curTotalY)) {
			    			Color bg = new Color(0, 255, 0, 50);
			    			g.setColor(bg);
			    			g.fillRect((curX * getTileWidth()) + tileOffsetX, (curY * getTileHeight()) + tileOffsetY, getTileWidth(), getTileHeight());
			    			g.setColor(Color.green);
			    			g.drawRect((curX * getTileWidth()) + tileOffsetX, (curY * getTileHeight()) + tileOffsetY, getTileWidth(), getTileHeight());
		    			} else {
		    				Color bg = new Color(255, 0, 0, 50);
			    			g.setColor(bg);
			    			g.fillRect((curX * getTileWidth()) + tileOffsetX, (curY * getTileHeight()) + tileOffsetY, getTileWidth(), getTileHeight());
			    			g.setColor(Color.red);
			    			g.drawRect((curX * getTileWidth()) + tileOffsetX, (curY * getTileHeight()) + tileOffsetY, getTileWidth(), getTileHeight());
		    			}
	    			}
	    		}
	    		if (!bottomMenu.isMouseOver() && focusObject != null && game.getPhaseID() != IngameState.PREPERATION) {
	    			int xT = tileIndexX(gc.getInput().getMouseX() + cam.getX());
	    			int yT = tileIndexY(gc.getInput().getMouseY() + cam.getY());
	    			ArmyUnit obj = (ArmyUnit) focusObject;
	    			if (xT > 0 && yT > 0 && xT < getWidth() && yT < getHeight() && obj.getPlayer().equals(game.getClientPlayer())) {
	    				try {
	    					unitPath = pathFinder.findPath(obj, obj.getTileX(), obj.getTileY(), xT, yT);
	    					if (obj.canMove() && gc.getInput().isMousePressed(Input.MOUSE_LEFT_BUTTON)) {
		    					obj.moveAlongPath(unitPath);
		    				}
	    				} catch (ArrayIndexOutOfBoundsException e) {
	    					Debugger.getInstance().write("Error: Path is out of world");
	    					unitPath = null;
	    					obj.stop();	    					
	    				}	    				
	    			} else if (!obj.getPlayer().equals(game.getClientPlayer())) {
	    				unitPath = null;
	    			}
	    		} else unitPath = null;
	    	}
	    }
	    
	    if (Debugger.getInstance().isEnabled()) {
	    	renderDebugInformation(g);
	    	
	    }
	}
	
	
	public float getTotalMouseX(GameContainer gc) {
		int tileOffsetX = (-1 * (int)cam.getX() % getTileWidth());
		int curX = tileIndexX(gc.getInput().getMouseX() - tileOffsetX);
		return (curX * getTileWidth()) + tileOffsetX + cam.getX();
	}
	
	public float getTotalMouseY(GameContainer gc) {
		int tileOffsetY = (-1 * (int)cam.getY() % getTileHeight());
		int curY = tileIndexY(gc.getInput().getMouseY() - tileOffsetY);
		return (curY * getTileHeight()) + tileOffsetY + cam.getY();
	}

	public Weather getWeather() {
		return weather;
	}

	public void setWeather(Weather weather) {
		this.weather = weather;
	}

	public Daytime getDaytime() {
		return daytime;
	}

	public GUIObject getBottomMenu() {
		return bottomMenu;
	}

	public void setBottomMenu(GUIObject bottomMenu) {
		this.bottomMenu = bottomMenu;
	}
	
	
	public TileObject getHoverObject() {
		return hoverObject;
	}

	public TileObject getClickedObject() {
		return clickedObject;
	}
	
	public List<ArmyUnit> getAllArmyUnits() {
		List<ArmyUnit> objList = new ArrayList<ArmyUnit>();
		for (Player player : game.getPlayers()) {
			for (Iterator<ArmyUnit> itr = player.getUnits().iterator(); itr.hasNext();) {  
				ArmyUnit target = itr.next();
				objList.add(target);
			}
		}
		
		return objList;
	}
	
	public void setHoverObject(TileObject hoverObject) {
		this.hoverObject = hoverObject;
	}
	
	public void setFocusObject(TileObject focusObject) {
		this.focusObject = focusObject;
	}
	
	
	
	public void setClickedObject(TileObject clickedObject) {
		this.clickedObject = clickedObject;
	}


	public void focusCameraOnObject(TileObject obj, GameContainer gc, boolean instantly) {
		// Align camera to the first object (TEST)
	   	float camOffsetX = cam.getWidth() / 2, camOffsetY = cam.getHeight() / 2;
	    			
	   	if (camOffsetX > obj.getX()) {
	    	camOffsetX += (obj.getX() - camOffsetX);
	    }
	    			
	    if (camOffsetY > obj.getY()) {
	    	camOffsetY += (obj.getY() - camOffsetY);
	    }
	    			
	    if (camOffsetX > (getWidth() * getTileWidth()) - (obj.getX())) {
	    	camOffsetX -= (getWidth() * getTileWidth()) - (obj.getX()) - camOffsetX;
	    }
	    			
	    if (getTileHeight() * getHeight() > gc.getScreenHeight()) {
		   	if (camOffsetY > (getHeight() * getTileHeight()) - (obj.getY())) {
		   		camOffsetY -= (getHeight() * getTileHeight()) - (obj.getY()) - camOffsetY;    		
		   	}
	    }
	    
	    setFocusObject(obj);
	    if (instantly) {
	    	cam.setX(obj.getX() - camOffsetX);
	    	cam.setY(obj.getY() - camOffsetY);
	    } else {
	    	cam.moveTo(obj.getX() - camOffsetX, obj.getY() - camOffsetY);	   	 
	    }
	}
	
	public void focusCameraOnObject(TileObject obj, GameContainer gc) {
		focusCameraOnObject(obj, gc, false);
	}

	public String getName() {
		return name;
	}
	
	
	
	
	
	/**
	 * Returns a full mapped image of the entire world map
	 * 
	 * @return Image
	 */
	public Image getWorldImage() {
		Image content = null;
		try {
			content = new Image(getWidth() * getTileWidth(), getHeight() * getTileHeight());
			Graphics mapGraphics = content.getGraphics();
			
			for (int x = 0; x < getWidth(); ++x) {
				for (int y = 0; y < getHeight(); ++y) {	
					for (int count = 0; count < getLayerCount(); ++count) {
						 Image tileImage = getTileImage(x, y, count);
						 
						 if (tileImage != null) {
							 mapGraphics.drawImage(tileImage, x * 32, y * 32);
						 }
					}
				}
			}
			
			mapGraphics.flush();
		} catch (SlickException e) {
			e.printStackTrace();
		}
		
		return content;
	}
	
	
	/**
	 * Check, if collision exists
	 */
	public boolean collisionExists(int x, int y) {
		return collisionExists(x, y, false);
	}
	
	public boolean collisionExists(int x, int y, boolean battle) {
		if (x < 0 || y < 0 || x >= getWidth() || y >= getHeight()) {
			return true;
		}	
		
		if (battle) {
			if (game.getCurrentPlayer().isCPU()) {
				return collisions[x][y] && !(isEnemyUnit(game.getCurrentPlayer(), x, y));
			}
			
			return collisions[x][y] && !(isEnemyUnit(game.getCurrentPlayer(), x, y) && isTileMouseOver(x, y));
		} else {
			if (game.getCurrentPlayer().isCPU()) {
				return collisions[x][y];
			}
			
			return collisions[x][y] && isTileMouseOver(x, y);
		}
	}
	
	
	/**
	 * Returns true, if mouse is over the given cell
	 */
	public boolean isTileMouseOver(int x, int y) {
		Input input = gc.getInput();
		// Calculate world position
		float worldX = input.getMouseX() + cam.getX();
		float worldY = input.getMouseY() + cam.getY();
	    // Check tile position
		return tileIndexX(worldX) == x && tileIndexY(worldY) == y;
	}


	@Override
	public boolean blocked(PathFindingContext context, int tx, int ty) {
		return collisionExists(tx, ty, true);
	}


	@Override
	public float getCost(PathFindingContext context, int tx, int ty) {
		return 0;
	}


	@Override
	public int getHeightInTiles() {
		return getHeight();
	}


	@Override
	public int getWidthInTiles() {
		return getWidth();
	}


	@Override
	public void pathFinderVisited(int x, int y) {
		
	}
	
	public IngameState getParentGame() {
		return game;
	}


}
