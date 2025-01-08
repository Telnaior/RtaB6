package tel.discord.rtab.commands.mod;

import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.Permission;

public class DemoCommand extends Command {

	public DemoCommand()
	{
		this.name = "demo";
		this.help = "starts a game with four bots";
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
				if(game.runDemo != 0)
					game.demoMode.cancel(true);
				if(!event.getArgs().equals(""))
				{
					int playerCount = Integer.parseInt(event.getArgs());
					for(int i=0; i<playerCount; i++)
					{
						game.addRandomBot(-1);
					}
					game.startTheGameAlready();
				}
				else
					game.runDemo();
				//We found the right channel, so
				return;
			}
		}
	}
}