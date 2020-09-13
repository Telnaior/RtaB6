package tel.discord.rtab.commands;

import java.util.ArrayList;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.ChannelType;
import tel.discord.rtab.Player;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.board.Game;

public class TestMinigameCommand extends Command
{
	public TestMinigameCommand()
	{
		this.name = "practice";
		this.help = "practice a minigame in a private channel";
		this.guildOnly = false;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		if(event.getChannel().getType() != ChannelType.PRIVATE)
		{
			event.reply("This command must be used in a private message.");
			return;
		}
		String gameName = event.getArgs();
		//If they didn't supply a game, give them the list
		if(gameName.equals(""))
		{
			StringBuilder output = new StringBuilder().append("Games available for practice:\n");
			for(Game game : Game.values())
				output.append(game.getShortName() + " - " + game.getName() + "\n");
			event.reply(output.toString());
		}
		for(Game game : Game.values())
		{
			if(!game.isBonus() && gameName.equalsIgnoreCase(game.getShortName()))
			{
				ArrayList<Player> players = new ArrayList<Player>();
				players.add(new Player(event.getAuthor()));
				Thread dummyThread = new Thread()
				{
					public void run()
					{
						while(true)
							try
							{
								Thread.sleep(2000);
							}
							catch (InterruptedException e)
							{
								break;
							}
						RaceToABillionBot.testMinigames --;
					}
				};
				dummyThread.setName(String.format("Minigame Test - %s - %s", event.getAuthor().getName(),game.getName()));
				game.getGame().initialiseGame(event.getChannel(), true, 1, 1, 1, 
						players, 0, dummyThread);
				RaceToABillionBot.testMinigames ++;
				dummyThread.start();
			}
		}
	}
}