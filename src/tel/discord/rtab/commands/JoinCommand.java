package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class JoinCommand extends Command
{
	public JoinCommand()
	{
		this.name = "join";
		this.aliases = new String[]{"in","enter","start","play","participate","embark","undertake","venture","engage","partake",
				"proceed","commence","begin"};
		this.help = "join the game (or start one if no game is running)";
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				game.addPlayer(event.getMember());
				return;
			}
		}
		//This isn't any of the game channels? Welp.
		event.reply("Cannot join game: This is not a game channel.");
	}
}