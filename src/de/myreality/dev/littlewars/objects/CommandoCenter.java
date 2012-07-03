package de.myreality.dev.littlewars.objects;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import de.myreality.dev.littlewars.components.UnitGenerator;
import de.myreality.dev.littlewars.game.IngameState;
import de.myreality.dev.littlewars.ki.Player;

public class CommandoCenter extends ArmyUnit {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected CommandoCenter(String resourceID, int x, int y,
			GameContainer gc, Camera cam, IngameState game) throws SlickException {
		super(UnitGenerator.UNIT_CENTER, resourceID, x, y, gc, cam, game);
		this.area = new Rectangle(getX(), getY(), getWidth(), getHeight());
	}
	
	

	@Override
	public void draw(Graphics g) {
		super.draw(g);
	}

	@Override
	public void setPlayer(Player player) {		
		if (!player.equals(this.player)) {
			if (this.player != null) {
				this.player.getSpawnArea().removeArea(this);
			}		
			super.setPlayer(player);
			if (this.player != null && this.player.getSpawnArea() != null) {
				this.player.getSpawnArea().writeArea(this);
			}		
		}
	}



	@Override
	public void update(int delta) {
		super.update(delta);	
		if (game.getPhase() == IngameState.BATTLE && game.getCurrentPlayer().isClientPlayer()) {
			color = Color.gray;
		} else {
			color = player.getColor();
		}
	}
	
	
	@Override
	protected int getRankStrength(int rank) {
		return 0;
	}

	@Override
	protected int getRankLife(int rank) {
		return 0;
	}

	@Override
	protected int getRankDefense(int rank) {
		return 0;
	}

	@Override
	protected int getRankSpeed(int rank) {
		return 0;
	}

	@Override
	protected int getRankExperience(int rank) {
		return 0;
	}

	@Override
	protected int getRankExperienceValue(int rank) {
		return rank * 1500;
	}
	
	public int getSpawnRange() {
		return 9;		
	}
}
