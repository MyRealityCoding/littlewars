/**
 * This file was written by Miguel Gonzalez and is part of the
 * game "LittleWars". For more information mail to info@my-reality.de
 * or visit the game page: http://dev.my-reality.de/littlewars
 * 
 * Contains game information like version and author
 * 
 * @version 	0.1.3
 * @author 		Miguel Gonzalez		
 */

package de.myreality.dev.littlewars.objects;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.newdawn.slick.Animation;
import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.Sound;
import org.newdawn.slick.particles.ConfigurableEmitter;
import org.newdawn.slick.particles.ParticleIO;
import org.newdawn.slick.particles.ParticleSystem;
import org.newdawn.slick.util.pathfinding.Path;

import de.myreality.dev.littlewars.components.Debugger;
import de.myreality.dev.littlewars.components.FadeInfoSetting;
import de.myreality.dev.littlewars.components.MovementCalculator;
import de.myreality.dev.littlewars.components.UnitEmitter;
import de.myreality.dev.littlewars.components.helpers.UnitInfoHelper;
import de.myreality.dev.littlewars.components.resources.ResourceManager;
import de.myreality.dev.littlewars.components.resources.SpriteAnimationData;
import de.myreality.dev.littlewars.components.resources.UnitResource;
import de.myreality.dev.littlewars.game.IngameState;
import de.myreality.dev.littlewars.ki.CPU;
import de.myreality.dev.littlewars.ki.Player;

public abstract class ArmyUnit extends TileObject {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	// Percent of life loosing / Experience changing
	protected static float changeFactor = 0.05f;
	
	// Basic Damage
	protected static final int BASIC_DAMAGE = 100;
	
	// Current player that the object belongs to
	protected Player player;
	
	// Game
	protected IngameState game;
	
	protected Sound expSound;
	
	// Life of the unit
	protected int currentLife;
	
	// Current speed
	protected int remainingSpeed;
	
	// Experience of the unit
	protected int currentExperience;
	
	// Life and experience sequence
	protected int lifeSeq, expSeq;
	
	// Rank of the unit
	protected int rank;	
	
	// Element name
	protected String name;
	
	protected int id;	
	
	// Sounds of the unit
	protected List<Sound> dyingSounds;	
	protected List<Sound> clickSounds;
	protected List<Sound> attackSounds;
	
	protected boolean dead, deadFirst;	
	
	// Emitters
	Map<Integer, UnitEmitter> emitters;
	
	protected MovementCalculator movementCalculator;	
	
	// Additional values to strengthen the unit
	private int lifeAdd, strengthAdd, defenseAdd, speedAdd;	
	
	// Static members for game control
	static protected boolean unitMoving, unitDying, unitLoosingLife, unitBusy;
	
	// General particle system
	static ParticleSystem unitSystem;
	
	// Directly attacking enemy
	private ArmyUnit attackingEnemy;
	
	public static boolean emitterAdded;
	
	static {
		unitMoving = false;
		unitDying = false;
		unitLoosingLife = false;
		unitBusy = false;
		emitterAdded = false;
		try {
			unitSystem = ParticleIO.loadConfiguredSystem("res/particles/system/default.xml");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	static public void setUnitMoving(boolean value) {
		unitMoving = value;
	}
	
	static public boolean isUnitMoving() {
		return unitMoving;
	}
	
	static public void setUnitDying(boolean value) {
		unitDying = value;
	}
	
	static public boolean isUnitDying() {
		return unitDying;
	}
	
	static public void setUnitLoosingLife(boolean value) {
		unitLoosingLife = value;
	}
	
	static public boolean isUnitLoosingLife() {
		return unitLoosingLife;
	}
	
	static public boolean isUnitBusy() {
		return isUnitLoosingLife() || isUnitDying() || isUnitMoving() || unitBusy;
	}
	
	static public void setUnitBusy(boolean value) {
		unitBusy = value;
	}


	/**
	 * Constructor of ArmyUnit
	 * 
	 * @param x x-coordinate on the screen
	 * @param y y-coordinate on the screen
	 * @param name string name of the unit
	 * @param type type of the unit
	 * @param gc GameContainer
	 * @param cam
	 * @param map
	 * @throws SlickException
	 */
	protected ArmyUnit(int id, String resourceID, int x, int y, GameContainer gc,
			Camera cam, IngameState game) throws SlickException {
		super(x, y, gc, cam, game.getWorld());
		this.id = id;
		this.game = game;
		rank = 1;
		currentLife = getLife();
		currentExperience = 0;
		updatePlayerColor();	
		loadFromResource(resourceID);
		dead = false;
		deadFirst = false;
		lifeSeq = 0;
		expSeq = 0;
		attackingEnemy = null;
		remainingSpeed = getSpeed();
		expSound = ResourceManager.getInstance().getSound("SOUND_EXP");
		//info = new UnitTileInfo(this, cam, 0, 0, gc);
		//info.attachTo(this);
		movementCalculator = new MovementCalculator(this, game);
		if (animations[SpriteAnimationData.DIE] != null) {
			for (int i = 0; i < animations[SpriteAnimationData.DIE].length; ++i) {
				animations[SpriteAnimationData.DIE][i].setLooping(false);
			}
		}
		emitters = new TreeMap<Integer, UnitEmitter>();
		
		addUnitEmitter(SpriteAnimationData.EXP, "BLUE_FLAIR");
		UnitEmitter expEmitter = getUnitEmitter(SpriteAnimationData.EXP);
		if (expEmitter != null) {
			expEmitter.setOffsetX(getWidth() / 2);
			expEmitter.setOffsetY(getHeight() / 2);
			expEmitter.setEnabled(false);
		} else {
			Debugger.getInstance().write("Exp emitter does not exist");
		}
		addUnitEmitter(SpriteAnimationData.LEVELUP, "WONDER_EXPLOSION");
		UnitEmitter levelupEmitter = getUnitEmitter(SpriteAnimationData.LEVELUP);
		if (levelupEmitter != null) {
			levelupEmitter.setOffsetX(getWidth() / 2);
			levelupEmitter.setOffsetY(getHeight() / 2);
			levelupEmitter.setEnabled(false);
		} else {
			Debugger.getInstance().write("Levelup emitter does not exist");
		}
	}
	
	public void setAttackingEnemy(ArmyUnit enemy) {
		attackingEnemy = enemy;
	}
	
	/**
	 * Change the player that it belongs to
	 * 
	 * @param player new player
	 */
	public void setPlayer(Player player) {
		this.player = player;	
		updatePlayerColor();
	}


	public Player getPlayer() {
		return player;
	}
	
	private void updatePlayerColor() {
		if (player != null) {
			color = player.getColor();
		}
		
	}
	
	
	public boolean onDead() {
		return !dead && isDead();
	}	
	
	
	@Override
	public void move(int direction, int delta) {
		
		// Return in order to fix camera bug
		if (game.getWorld().getCamera().isSmoothMoving() && game.getClientPlayer().isCurrentPlayer()) {
			return;
		}
		
		
		if (!isDead() && canMove()) {
			if (isTargetArrived()) {
				remainingSpeed--;
			}
			super.move(direction, delta);
		}
	}

	protected void loadFromResource(String resourceID) {
		
		UnitResource resource = ResourceManager.getInstance().getUnitResource(resourceID);
		
		// Load the animations
		animations = resource.getAnimationData().generateAnimations();
		
		// Load the avatar
		imgAvatar = ResourceManager.getInstance().getImage(resource.getAvatar());
		imgAvatarID = resource.getAvatar();
	
		// Set the name
		name = resource.getName();
		
		// Load the sounds
		dyingSounds = resource.generateSoundList("onDead");	
		clickSounds = resource.generateSoundList("onClick");
		attackSounds = resource.generateSoundList("onAttack");
	}
	
	
	public int getCurrentLife() {
		return currentLife;
	}
	
	public void playRandomSound(String key) {
		if (key == "onDead" && !dyingSounds.isEmpty()) {
			int index = (int) (Math.random() * dyingSounds.size());
			dyingSounds.get(index).play();			
		} else if (key == "onClick" && !clickSounds.isEmpty()) {
			int index = (int) (Math.random() * clickSounds.size());
			clickSounds.get(index).play();	
		} else if (key == "onAttack" && !attackSounds.isEmpty()) {
			int index = (int) (Math.random() * attackSounds.size());
			attackSounds.get(index).play();	
		}
	}
	
	public boolean isSoundPlaying(String key) {
		if (key == "onDead" && !dyingSounds.isEmpty()) {
			for (Sound s : dyingSounds) {
				if (s.playing()) {
					return true;
				}
			}
		} else if (key == "onClick" && !clickSounds.isEmpty()) {
			for (Sound s : clickSounds) {
				if (s.playing()) {
					return true;
				}
			}
		} 
		
		return false;
	}
	
	
	public boolean stopSound(String key) {
		if (key == "onDead" && !dyingSounds.isEmpty()) {
			for (Sound s : dyingSounds) {
				if (s.playing()) {
					s.stop();
					break;
				}
			}
		} else if (key == "onClick" && !clickSounds.isEmpty()) {
			for (Sound s : clickSounds) {
				if (s.playing()) {
					s.stop();
					break;
				}
			}
		} 
		
		return false;
	}

	public void setCurrentLife(int currentLife) {
		this.currentLife = currentLife;
	}

	public int getCurrentExperience() {
		return currentExperience;
	}

	public void setCurrentExperience(int currentExperience) {
		this.currentExperience = currentExperience;
	}
	
	public void setRemainingSpeed(int speed, boolean exception) {
		if (((Integer)speed).equals(0)) { // Fixed movement bug on click
			if (((Integer)getSpeed()).equals(getRemainingSpeed()) && !exception) {
				return;
			}
		}
		remainingSpeed = speed;
	}
	
	public void setRemainingSpeed(int speed) {
		setRemainingSpeed(speed, false);
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}	

	public void setLifeAdd(int lifeAdd) {
		this.lifeAdd = lifeAdd;
	}

	public void setStrengthAdd(int strengthAdd) {
		this.strengthAdd = strengthAdd;
	}

	public void setSpeedAdd(int speedAdd) {
		this.speedAdd = speedAdd;
	}

	// State Functions
	protected abstract int getRankStrength(int rank);
	protected abstract int getRankLife(int rank);
	protected abstract int getRankDefense(int rank);
	protected abstract int getRankSpeed(int rank);
	protected abstract int getRankExperience(int rank);
	public abstract int getPrice();
	
	
	public int getStrength() {
		return getRankStrength(rank) + getStrengthAdd();
	}
	
	
	public int getTotalExperience() {
		return getRankExperience(rank);
	}
	
	
	public int getLife() {
		return getRankLife(rank) + getLifeAdd();
	}
	
	
	public int getRemainingSpeed() {
		return remainingSpeed;
	}
	
	protected abstract int getRankExperienceValue(int rank);
	
	protected int getExperienceValue() {
		return getRankExperienceValue(rank);
	}

	@Override
	public void draw(Graphics g) {
		if (isDead()) {
			if (animations[SpriteAnimationData.DIE][currentDirection] != null) {
				animations[SpriteAnimationData.DIE][currentDirection].draw(-camera.getX() + getX(), -camera.getY() + getY(), getWidth(), getHeight(), color);
			}
		} else {
			super.draw(g);
		}
	}
	
	public boolean isDying() {
		Animation animDead = animations[SpriteAnimationData.DIE][currentDirection];
		return isDead() && !animDead.isStopped();
	}
	
	public void attackTargetEnemy() {
		ArmyUnit enemy = movementCalculator.getEnemy();
		if (enemy != null) {
			attack(enemy);
			setRemainingSpeed(0, true);
		} else {
			setRemainingSpeed(0, true);
		}
	}
	
	public int getRealPathLength() {
		return movementCalculator.getLength();
	}

	@Override
	public void update(int delta) {
		super.update(delta);

		// Update animation
		if (!isTargetArrived()) {
			if (animations[SpriteAnimationData.MOVE][currentDirection] != null) {
				animations[SpriteAnimationData.MOVE][currentDirection].update(delta);
			}
		} else if (isDead()) {			
			Animation animDead = animations[SpriteAnimationData.DIE][currentDirection];
			if (animDead.isStopped()) {
				player.removeArmyUnit(this);
			} else if (animDead != null) {
				animDead.update(delta);
			}			
		} 
	
		if (lifeSeq > 0 && !isDead()) {
			if (lifeSeq >= Math.ceil((getLife() * (changeFactor / 100) * delta))) {
				currentLife -= Math.ceil((getLife() * (changeFactor / 100) * delta));
			} else {
				currentLife -= lifeSeq;
			}
			lifeSeq -= Math.ceil((getLife() * (changeFactor / 100) * delta));
			if (lifeSeq < 0) {
				lifeSeq = 0;
			}
		}
		
		if (lifeSeq == 0 && expSeq > 0 && !isDead()) {
			if (expSeq >=  Math.ceil((getTotalExperience() * (changeFactor / 100) * delta))) {
				currentExperience += Math.ceil((getTotalExperience() * (changeFactor / 100) * delta));
			} else {
				currentExperience += expSeq;
			}
			expSound.stop();
			expSound.play(1f * (getExperiencePercent() / 100), 0.05f);
			expSeq -= Math.ceil((getTotalExperience() * (changeFactor / 100) * delta));	
			if (expSeq < 0) {
				expSeq = 0;
			}
		} 
		
		if (currentLife < 0) {
			currentLife = 0;
		}
		
		if (currentExperience >= getTotalExperience()) {
			expSeq += currentExperience - getTotalExperience();
			currentExperience = 0;
			rankUp();
		}
		
		if (isDead()) {
			expSeq = 0;
			lifeSeq = 0;
		} 
		
		if (deadFirst && isDead()) {
			dead = true;
		}
		
		if (isDead() && !deadFirst) {
			deadFirst = true;
		}
		
		if (!isDead()) {
			deadFirst = false;
			dead = false;
		}
		
		if (onDead()) {
			playRandomSound("onDead");
			FadeInfoSetting setting = new FadeInfoSetting();
			setting.setText("Kill!");
			setting.setColor(Color.red);
			UnitInfoHelper.getInstance().addInfo(this, setting, gc, game);
		}
		
		if (lifeSeq > 0 || expSeq > 0 || isDying() || onDead() || !isTargetArrived()) {
			unitBusy = true;
		}
		
		if (player.isClientPlayer() && !isDead() && (instantClick || onMouseClick())) {
			stopSound("onClick");
			playRandomSound("onClick");
		}
		
		if (instantClick) {
			instantClick = false;
		}
		
		if (getRemainingSpeed() == 0 && isTargetArrived() && !(this instanceof CommandoCenter) && player.isCurrentPlayer()) {
			color = Color.gray;
		} else {
			color = player.getColor();
		}
		
		movementCalculator.update(delta);		
		
		if (!isTargetArrived()) {
			setUnitMoving(true);
		}
		
		if (isDying()) {
			setUnitDying(true);
		}
		
		if (lifeSeq > 0) {
			setUnitLoosingLife(true);
		}
		

		if (!isUnitBusy() && movementCalculator.isDone() && isTargetArrived()) {
			ArmyUnit enemy = movementCalculator.getEnemy();
			if (enemy != null) {
				attack(enemy);			
			}			
		}		
		
		// Attack back
		if (!isUnitBusy() && attackingEnemy != null && !onDead() && !isDead() && !isDying()) {
			attackingEnemy.addExperience((getExperienceValue() * (getRank() / attackingEnemy.getRank())) / 2);
			attack(attackingEnemy, false);
			attackingEnemy = null;
		} else if (!isUnitBusy() && (onDead() || isDead() || isDying()) && attackingEnemy != null) {
			attackingEnemy.addExperience(getExperienceValue() * (getRank() / attackingEnemy.getRank()));
			attackingEnemy = null;
		}
		
		for (Entry<Integer, UnitEmitter> entry : emitters.entrySet()) {
			UnitEmitter emitter = entry.getValue();
			switch (entry.getKey()) {
				case SpriteAnimationData.DEFAULT:
					emitter.setEnabled(isTargetArrived() && !isDying() && player != null);
					break;
				case SpriteAnimationData.ATTACK:
					break;
				case SpriteAnimationData.DIE:
					emitter.setEnabled(isDying() && !emitter.getInner().completed());
					break;
				case SpriteAnimationData.MOVE:
					break;
				case SpriteAnimationData.DAMAGED:
					if (isDamageInProcess() && emitter.getInner().completed()) {
						emitter.getInner().setEnabled(true);
						emitter.getInner().replay();
					} else if (!isDamageInProcess() && emitter.getInner().completed()) {
						emitter.setEnabled(false);
					} else if (isDamageInProcess() && !emitter.getInner().completed()) {
						emitter.setEnabled(true);
					}
					break;
				case SpriteAnimationData.EXP:
					if (emitter.getInner().completed()) {
						emitter.setEnabled(false);
					}
					break;
				case SpriteAnimationData.LEVELUP:
					if (emitter.getInner().completed()) {
						emitter.setEnabled(false);
					}
					break;
			}			
			emitter.update(delta);
		}
	}

	public int getDefense() {
		return getRankDefense(rank) + defenseAdd;
	}
	
	public void finalize() {
		free();
	}
	
	
	
	public int getSpeed() {
		return getRankSpeed(rank) + getSpeedAdd();
	}
	
	public static void renderParticles(Camera camera) {
		unitSystem.render(-camera.getX(), -camera.getY());
	}
	
	public static void updateParticles(int delta) {
		try {
			if (!emitterAdded) {
				unitSystem.update(delta);
			}
		} catch (ConcurrentModificationException e) {
			Debugger.getInstance().write("Concurrent modification in particle system.");
		}
	}
	
	public boolean isDamageInProcess() {
		return lifeSeq > 0;
	}
	
	
	public void free() {
		for (Entry<Integer, UnitEmitter> entry : emitters.entrySet()) {
			entry.getValue().releaseFromSystem();
		}
		emitters.clear();
	}
	
	
	
	public int getRankExperience() {
		return getRankExperience(rank);
	}

	public int getLifeAdd() {
		return lifeAdd;
	}

	public void setLifeAdd(Integer lifeAdd) {
		this.lifeAdd = lifeAdd;
	}

	public int getStrengthAdd() {
		int add = 0;
		if (player != null && !(this instanceof CommandoCenter)) {
			for (ArmyUnit center : player.getCommandoCenters()) {
				if (center != null && !equals(center)) {
					add += center.getStrength();
				}
			}
		}
		
		return strengthAdd + add;
	}

	public void setStrengthAdd(Integer strengthAdd) {
		this.strengthAdd = strengthAdd;
	}

	public int getDefenseAdd() {
		int add = 0;
		if (player != null) {
			for (ArmyUnit center : player.getCommandoCenters()) {
				if (center != null && !equals(center)) {
					add += center.getDefense();
				}
			}
		}
		
		return defenseAdd + add;
	}

	public void setDefenseAdd(Integer defenseAdd) {
		this.defenseAdd = defenseAdd;
	}

	public int getSpeedAdd() {
		return speedAdd;
	}

	public void setSpeedAdd(Integer speedAdd) {
		this.speedAdd = speedAdd;
	}
	
	public void rankUp() {
		++rank;
		currentLife = getLife();
		FadeInfoSetting setting = new FadeInfoSetting();
		setting.setText("Level up!");
		setting.setColor(ResourceManager.getInstance().getColor("COLOR_MAIN"));
		UnitInfoHelper.getInstance().addInfo(this, setting, gc, game);
		
		// Show animation
		UnitEmitter emitter = emitters.get(SpriteAnimationData.LEVELUP);
		if (emitter != null) {
			ConfigurableEmitter inner = emitter.getInner();
			inner.replay();
			inner.setEnabled(true);
		}
	}
	
	
	public void addExperience(int exp) {
		if (!isDead() && isTargetArrived()) {
			expSeq += exp;
			FadeInfoSetting setting = new FadeInfoSetting();
			setting.setText("+" + String.valueOf(exp) + " EXP");
			setting.setColor(ResourceManager.getInstance().getColor("UNIT_EXP"));
			UnitInfoHelper.getInstance().addInfo(this, setting, gc, game);
			
			// Particle animation
			UnitEmitter emitter = emitters.get(SpriteAnimationData.EXP);
			if (emitter != null) {
				emitter.getInner().replay();
				emitter.getInner().setEnabled(true);
			}
			
		}		
	}
	
	
	public float getLifePercent() {		
		return getCurrentLife() * 100 / getLife();
	}
	
	public float getExperiencePercent() {
		return getCurrentExperience() * 100 / getTotalExperience();
	}
	
	public boolean addDamage(int value) {
		
		if (isDead() || !isTargetArrived()) {
			return false;
		}
		
		lifeSeq += value;
		
		FadeInfoSetting setting = new FadeInfoSetting();
		setting.setText(String.valueOf(value) + " " + ResourceManager.getInstance().getText("TXT_GAME_DAMAGE"));
		setting.setColor(Color.red);
		UnitInfoHelper.getInstance().addInfo(this, setting, gc, game);
		
		return true;
	}
	
	public boolean isDead() {
		return currentLife <= 0;
	}
	
	public void attack(ArmyUnit target) {
		attack(target, true);		
	}
	
	public void attack(ArmyUnit target, boolean attackingBack) {
		if (!equals(target)) {
			// Play attack sound
			playRandomSound("onAttack");
			
			// Damage calculation 
			int damage = (int) ((int) (11 * (Math.random() * 30 + 1) - target.getDefense()) * getStrength() * (float)(getRank() + getStrength()) / 256.0f);
			target.addDamage(damage);
			if (!target.isDead()) {  
				if (attackingBack) {
					if (!(target instanceof CommandoCenter)) {
					target.setAttackingEnemy(this);
					}
					// Add money to the player (10% of the price of the target unit)
					player.getMoney().addCredits(target.getPrice() / 10);
					
				} else {
					if (target.isDying() || target.isDead() || target.onDead()) {
						addExperience(getExperienceValue() * (target.getRank() / getRank()));
					} else {
						addExperience((getExperienceValue() * (target.getRank() / getRank())) / 3);
					}
				}				
			} 
			movementCalculator.reset();			
			// Set owner as new opponent
			if (target.getPlayer() instanceof CPU) {
				CPU cpu = (CPU)target.getPlayer();
				cpu.setOpponent(getPlayer());
			}
		}
		if (attackingBack) {
			setRemainingSpeed(0, true);
		}
	}
	
	
	public boolean isReady() {
		return lifeSeq == 0 && expSeq == 0;
	}
	
	
	public boolean canMove() {
		return remainingSpeed > 0;
	}
	
	public void activate() {
		remainingSpeed = getSpeed();
	}
	
	public Integer getID() {
		return id;
	}
	
	public void moveAlongPath(Path path) {
		if (!isUnitMoving()) {
			movementCalculator.setMovement(path);
		}
	}
	
	public IngameState getGame() {
		return game;
	}
	
	public float getTileVelocity(int delta) {
		return velocity * delta;
	}
	
	public void addUnitEmitter(Integer type, String ID) {
		if (emitters.get(type) == null) {
			emitters.put(type, new UnitEmitter(this, ID, unitSystem));
		} else {
			emitters.get(type).releaseFromSystem();
			emitters.remove(type);
			addUnitEmitter(type, ID);
		}
		emitterAdded = true;
	}
	
	public UnitEmitter getUnitEmitter(Integer type) {
		return emitters.get(type);
	}
	
	
	public static void freeParticleSystem() {
		unitSystem.removeAllEmitters();
	}
}
