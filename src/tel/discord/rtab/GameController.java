package tel.discord.rtab;

import net.dv8tion.jda.api.entities.TextChannel;

public class GameController
{
	
	public TextChannel gameChannel;
	
	public GameController(TextChannel gameChannel)
	{
		this.gameChannel = gameChannel;
		gameChannel.sendMessage("OMG IT'S A GAME CHANNEL but i don't know how to run a game yet sorry").queue();
	}
	
}
