package tel.discord.rtab.commands;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;
import tel.discord.rtab.GameStatus;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class NextCommand extends Command {
	public NextCommand()
	{
		this.name = "next";
		this.aliases = new String[]{"nextgame"};
		this.help = "asks the bot to ping you when the current game finishes";
		this.cooldown = 120;
	}
	@Override
	protected void execute(CommandEvent event) {
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				if(game.gameStatus != GameStatus.SIGNUPS_OPEN || (!game.playersCanJoin && game.players.size() == 0))
				{
					game.pingList.add(event.getAuthor().getAsMention());
					event.reply(String.format("Noted - will ping you when you can %s.",game.playersCanJoin?"play":"bet"));
				}
				else
				{
					if(game.playersCanJoin)
						event.reply("You can join already!");
					else
					{
						event.reply("You can bet already!");
						game.listPlayers(false);
					}
				}
				//We found the right channel, so
				return;
			}
		}
	}
}