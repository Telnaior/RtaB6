package tel.discord.rtab.commands.mod;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;

public class StartCommand extends Command
{
	public StartCommand()
	{
		this.name = "forcestart";
		this.help = "starts the game immediately";
		this.hidden = true;
		this.userPermissions = new Permission[] {Permission.BAN_MEMBERS};
	}
	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.equals(event.getChannel()))
			{
				game.startTheGameAlready();
				//We found the right channel, so
				return;
			}
		}
	}
}
