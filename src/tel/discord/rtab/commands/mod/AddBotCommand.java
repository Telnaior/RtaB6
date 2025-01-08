package tel.discord.rtab.commands.mod;

import tel.discord.rtab.GameController;
import tel.discord.rtab.GameStatus;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;

public class AddBotCommand extends Command {

	public AddBotCommand()
	{
		this.name = "addbot";
		this.help = "adds the specified bot";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.MANAGE_SERVER};
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.getId().equals(event.getChannel().getId()))
			{
				if(game.gameStatus != GameStatus.SIGNUPS_OPEN)
				{
					//Ignore the command
				}
				else if(event.getArgs().equals(""))
				{
					game.addRandomBot();
				}
				else
				{
					game.addBot(Integer.parseInt(event.getArgs()));
				}
				//We found the right channel, so
				return;
			}
		}
		
	}
}