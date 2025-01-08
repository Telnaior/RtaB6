package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class TribeTotalsCommand extends Command
{
	public TribeTotalsCommand()
	{
		this.name = "tribes";
		this.help = "displays the current tribe rankings, and a list of who is on what tribe";
	}
	
	@Override
	protected void execute(CommandEvent event) {
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.getId().equals(event.getChannel().getId()))
			{
				game.displayTribeTotals();
				//We found the right channel, so
				return;
			}
		}
	}

}
