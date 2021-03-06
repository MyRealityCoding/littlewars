package de.myreality.dev.littlewars.gui.bottommenu;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;

import de.myreality.dev.littlewars.components.resources.ResourceManager;
import de.myreality.dev.littlewars.gui.GUIObject;
import de.myreality.dev.littlewars.ki.Player;
import de.myreality.dev.littlewars.objects.ArmyUnit;
import de.myreality.dev.littlewars.world.GameWorld;

public class UnitOverview extends GUIObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Player player;
	private GameWorld targetWorld;
	private List<UnitShortcut> shortcuts;
	private Image background;

	public UnitOverview(GUIObject left, GUIObject right, GUIObject parent, GameContainer gc) {
		super((int) (left.getX() + left.getWidth()) + 10, 12, gc);
		attachTo(parent);
		width = (int) (right.getX() - left.getX() - getX());
		height = right.getHeight();
		shortcuts = new ArrayList<UnitShortcut>();		
		background = ResourceManager.getInstance().getImage("GUI_BOTTOM_SEPERAT_BACKGROUND_DARK");
	}
	
	private void reAlignList(int rows) {
		
		for (UnitShortcut shortcut : shortcuts) {			
			removeChild(shortcut);
		}
		
		shortcuts.clear();		
		
		int padding = 10;
		int posX = padding; int posY = padding;

		for (ArmyUnit unit : player.getUnits()) {			
			UnitShortcut shortcut = new UnitShortcut((GUIObject) getParent(), targetWorld, unit, posX, posY, (int) Math.ceil(getHeight() / rows), (int) Math.ceil(getHeight() / rows), gc);			
			shortcut.attachTo(this);
			shortcuts.add(shortcut);
			posX += shortcut.getWidth() + padding;			
						
			if (getWidth() - posX < shortcut.getWidth()) {
				posY += shortcut.getHeight() + padding;
				posX = padding;
			}
			
			if (posY + shortcut.getHeight() > getHeight()) {				
				reAlignList(++rows);
				break;
			}
		}
	}

	@Override
	public void draw(Graphics g) {
		background.draw(getX(), getY(), getWidth(), getHeight());
		for (UnitShortcut shortcut : shortcuts) {
			shortcut.draw(g);
		}
	}

	@Override
	public void update(int delta) {		
		super.update(delta);
		if (shortcuts.size() != player.getUnits().size()) {
			reAlign();
		}
		
		for (UnitShortcut shortcut : shortcuts) {
			shortcut.update(delta);
		}
	}

	public Player getPlayer() {
		return player;
	}

	public void setPlayer(Player player) {
		this.player = player;
		if (player != null) {
			reAlign();
		}
	}
	
	
	public void reAlign() {
		reAlignList(2);
	}

	public GameWorld getTargetWorld() {
		return targetWorld;
	}

	public void setTargetWorld(GameWorld targetWorld) {
		this.targetWorld = targetWorld;
	}

}
