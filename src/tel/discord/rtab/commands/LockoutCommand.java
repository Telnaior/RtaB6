package tel.discord.rtab.commands;

import static tel.discord.rtab.RaceToABillionBot.waiter;

import java.util.concurrent.TimeUnit;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import tel.discord.rtab.GameController;
import tel.discord.rtab.RaceToABillionBot;

public class LockoutCommand extends Command
{
	public LockoutCommand()
	{
		this.name = "lockout";
		this.help = "exclude yourself from being able to play until your lives reset";
	}

	@Override
	protected void execute(CommandEvent event)
	{
		for(GameController game : RaceToABillionBot.game)
		{
			if(game.channel.getId().equals(event.getChannel().getId()))
			{
				//Found the right game controller, let's ask to add them to the lockout list
				event.reply("Are you sure? This will prevent you from playing in this channel until your lives refill! (yes/no)");
				waiter.waitForEvent(MessageReceivedEvent.class,
						//Make sure it's the one who sent the message and it's specifically "yes" or "no"
						e -> 
						{
							if(e.getAuthor().equals(event.getAuthor()))
							{
								String message = e.getMessage().getContentStripped();
								return message.equalsIgnoreCase("yes") || message.equalsIgnoreCase("no");
							}
							return false;
						},
						//If they said yes, archive the scoreboard files (also save a backup because Murphy's Law)
						e ->
						{
							if(e.getMessage().getContentStripped().equalsIgnoreCase("yes"))
							{
								event.reply("You are now locked out for the day.");
								game.lockoutList.add(event.getAuthor().getId());
							}
							else
							{
								event.reply("Very well.");
							}
						},
						30,TimeUnit.SECONDS, () ->
		                        event.reply("Lockout request expired.")
						);
			}
		}
	}

}
