package tel.discord.rtab.commands;

import java.util.ArrayList;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
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
		boolean enhance = false;
		if(gameName.startsWith("-e "))
		{
			enhance = true;
			gameName = gameName.replaceFirst("-e ", "");
		}
		//Run through the list of games to find the one they asked for
		boolean gameFound = false;
		for(Game game : Game.values())
		{
			if((!game.isBonus() || event.isOwner()) && (gameName.equalsIgnoreCase(game.getShortName()) || gameName.equalsIgnoreCase(game.getName())))
			{
				runGame(event.getAuthor(), game, event.getChannel(), enhance);
				gameFound = true;
				break;
			}
		}
		//If they supplied an invalid game (or nothing at all), give them the list
		if(!gameFound)
		{
			StringBuilder output = new StringBuilder().append("Games available for practice:\n");
			for(Game game : Game.values())
				if(!game.isBonus())
					output.append(game.getShortName()).append(" - ").append(game.getName()).append("\n");
			event.reply(output.toString());
		}
	}
	
	public static void runGame(User player, Game game, MessageChannel channel, boolean enhance)
	{
		ArrayList<Player> players = new ArrayList<>();
		players.add(new Player(player));
		Thread dummyThread = new Thread(() -> RaceToABillionBot.testMinigames --);
		dummyThread.setName(String.format("Minigame Test - %s - %s", player.getName(),game.getName()));
		game.getGame().initialiseGame(channel, true, 1, 1, 1, players, 0, dummyThread, enhance);
		RaceToABillionBot.testMinigames ++;
	}
}