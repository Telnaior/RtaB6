package tel.discord.rtab.board;

import tel.discord.rtab.games.*;

public enum Game implements WeightedSpace
{
	//Minigame Pool
	//Minigames awarding weird things appear first
	BOOSTER_SMASH(1)	{ public MiniGame getGame() { return new BoosterSmash(); } },	//Author: Atia
	OVERFLOW(1)			{ public MiniGame getGame() { return new Overflow(); } },		//Author: JerryEris
	//PvP games next so the opponent doesn't fall asleep waiting for them
	TIC_TAC_BOMB(1)		{ public MiniGame getGame() { return new TicTacBomb(); } },		//Author: Atia
	//Games you can win negative cash in go next
	MATH_TIME(1)		{ public MiniGame getGame() { return new MathTime(); } },		//Author: Atia
	UP_AND_DOWN(1)		{ public MiniGame getGame() { return new UpAndDown(); } },		//Author: JerryEris
	//Regular cash games
	DEAL_OR_NO_DEAL(1)	{ public MiniGame getGame() { return new DealOrNoDeal(); } },	//Author: Atia
	COINFLIP(1)			{ public MiniGame getGame() { return new CoinFlip(); } },		//Author: Amy
	CLOSE_SHAVE(1)		{ public MiniGame getGame() { return new CloseShave(); } },		//Author: JerryEris
	BOMB_ROULETTE(1)	{ public MiniGame getGame() { return new BombRoulette(); } },	//Author: StrangerCoug
	MONEY_CARDS(1)		{ public MiniGame getGame() { return new MoneyCards(); } },		//Author: StrangerCoug
	SHUT_THE_BOX(1)		{ public MiniGame getGame() { return new ShutTheBox(); } },		//Author: StrangerCoug
	BUMPER_GRAB(1)		{ public MiniGame getGame() { return new BumperGrab(); } },		//Author: Tara
	//FtRotS appearss last because it's long and hype
	FTROTS(1)			{ public MiniGame getGame() { return new FTROTS(); } },			//Author: Atia
	
	//Games rotated out
	//TESTGAME(0)			{ public MiniGame getGame() { return new TestGame(); } },	//Author: The Triforce
	GAMBLE(0)			{ public MiniGame getGame() { return new Gamble(); } },			//Author: Atia
	STRIKE_IT_RICH(0)	{ public MiniGame getGame() { return new StrikeItRich(); } },	//Author: Atia
	TRIPLE_PLAY(0)		{ public MiniGame getGame() { return new TriplePlay(); } },		//Author: Atia
	MINEFIELD_MULTI(0)	{ public MiniGame getGame() { return new MinefieldMulti(); } },	//Author: Amy
	THE_OFFER(0)		{ public MiniGame getGame() { return new TheOffer(); } },		//Author: Amy
	CALL_YOUR_SHOT(0)	{ public MiniGame getGame() { return new CallYourShot(); } },	//Author: JerryEris
	DOUBLE_TROUBLE(0)	{ public MiniGame getGame() { return new DoubleTrouble(); } },	//Author: JerryEris
	DOUBLE_ZEROES(0)	{ public MiniGame getGame() { return new DoubleZeroes(); } },	//Author: JerryEris
	OPEN_PASS(0)		{ public MiniGame getGame() { return new OpenPass(); } },		//Author: JerryEris
	DEUCES_WILD(0)		{ public MiniGame getGame() { return new DeucesWild(); } },		//Author: StrangerCoug
	HILO_DICE(0)		{ public MiniGame getGame() { return new HiLoDice(); } },		//Author: StrangerCoug
	
	//Bonus Games - not in pool but earned through other means
	SUPERCASH(0)		{ public MiniGame getGame() { return new Supercash(); } },
	DIGITAL_FORTRESS(0)	{ public MiniGame getGame() { return new DigitalFortress(); } },
	SPECTRUM(0)			{ public MiniGame getGame() { return new Spectrum(); } },
	HYPERCUBE(0)		{ public MiniGame getGame() { return new Hypercube(); } },
	SUPERBONUSROUND(0)	{ public MiniGame getGame() { return new SuperBonusRound(); } };
	
	String fullName;
	String shortName;
	boolean bonus;
	int weight;
	Game(int valueWeight)
	{
		fullName = getGame().getName();
		shortName = getGame().getShortName();
		bonus = getGame().isBonus();
		weight = valueWeight; 
	}
	@Override
	public String toString()
	{
		return fullName;
	}
	public String getName()
	{
		return toString();
	}
	public String getShortName()
	{
		return shortName;
	}
	public boolean isBonus()
	{
		return bonus;
	}
	//Returns a new instance of the requested minigame
	public abstract MiniGame getGame();
	
	@Override
	public int getWeight(int playerCount)
	{
		//Minigame types don't care about playercount
		return weight;
	}
}
