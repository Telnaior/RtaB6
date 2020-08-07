package tel.discord.rtab;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import net.dv8tion.jda.api.entities.TextChannel;

public class GameController
{

	final int MAX_PLAYERS = 16;
	public ScheduledThreadPoolExecutor timer = new ScheduledThreadPoolExecutor(1);
	public TextChannel channel;
	int baseNumerator, baseDenominator, botCount;
	public final List<Player> players = new ArrayList<>(16);
	int currentTurn, playersAlive, repeatTurn;
	
	public GameController(TextChannel gameChannel, int baseNumerator, int baseDenominator, int botCount)
	{
		channel = gameChannel;
		this.baseNumerator = baseNumerator;
		this.baseDenominator = baseDenominator;
		this.botCount = botCount;
		channel.sendMessage("OMG IT'S A GAME CHANNEL but i don't know how to run a game yet sorry").queue();
		reset();
	}
	
	public void reset()
	{
		players.clear();
		currentTurn = -1;
	}
	
}
