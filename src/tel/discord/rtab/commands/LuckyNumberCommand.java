package tel.discord.rtab.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;

public class LuckyNumberCommand extends Command
{
	public LuckyNumberCommand()
	{
		this.name = "luckynumber";
		this.aliases = new String[]{"lucky"};
		this.help = "gives you a random number";
		this.guildOnly = false;
		this.cooldown = 15;
	}
	@Override
	protected void execute(CommandEvent event)
	{
		//80% chance of getting the (officially designated) luckiest of all numbers
		if(Math.random() < 0.8)
		{
			//10% chance of giving it as a reaction instead of a message
			if(Math.random() < 0.1)
			{
				event.getMessage().addReaction("9️⃣").queue();
			}
			else
			{
				event.reply("Your lucky number is "+(Math.random() < 0.055 ? ":nine:" : "9")+".");
				//1% chance they get a comic
				if(Math.random() < 0.0132)
				{
					if(Math.random() < 0.5)
						event.reply("http://dilbert.com/strip/2001-10-25");
					else
						event.reply("https://xkcd.com/221");
				}
			}
		}
		//0.1% chance you "win"
		//And no, source-code readers, you can't cheat. :P (But you can grind it out in private if you really want)
		else if(Math.random() < 0.005)
		{
			int secretCode = (int) (Math.random() * 1000000);
			event.reply("Congratulations, you win! "
					+ String.format("Quote %06d to Atia#2084 to add a new lucky number to the bot!",secretCode));
			System.out.println(String.format("Secret Lucky Number Code: %06d",secretCode));
		}
		//Otherwise pick one of the extra texts
		//Here be spoilers!
		else
		{
			int chosenText = (int) (Math.random() * 11);
			switch(chosenText)
			{
			case 0:
				event.reply("Your lucky number is -1.");
				break;
			case 1:
				event.reply("Your lucky number is π.");
				break;
			case 2:
				event.reply("Your lucky number is "+(Math.random()<0.5 ? "heads" : "tails")+".");
				break;
			case 3:
				event.reply("Your lucky number is 88.");
				break;
			case 4:
				event.reply("Space 4 really is safe this time, honest!");
				break;
			case 5:
				event.reply("Your lucky number is a **BOMB**.");
				event.reply("It goes "+(Math.random() < 0.04 ? "_\\*fizzle*_." : "**BOOM**. $250,000 penalty."));
				break;
			case 6:
				event.reply("This fortune cookie is delicious! Unfortunately, you ate the lucky number.");
				break;
			case 7:
				event.reply("Your lucky number is "+Math.random()+".");
				break;
			case 8:
				event.reply("Come on, lucky seven! Oops, wrong game.");
				break;
			case 9:
				event.reply("Didn't you see the horoscope?");
				break;
			case 10:
				event.reply("Should've picked 33!");
				break;
			}
		}
	}
}