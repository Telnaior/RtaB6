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
				//Github breaks the emoji if I don't escape it lol
				event.getMessage().addReaction("\u0039\uFE0F\u20E3").queue();
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
		//0.05% chance you "win"
		//And no, source-code readers, you can't cheat. :P (But you can grind it out in private if you really want)
		else if(Math.random() < 0.0025)
		{
			int secretCode = (int) (Math.random() * 1000000);
			event.reply("Congratulations, you win! "
					+ String.format("Quote %06d to Telna#2084 to add a new lucky number to the bot!",secretCode));
			System.out.println(String.format("Secret Lucky Number Code: %06d",secretCode));
		}
		//Otherwise pick one of the extra texts
		//Here be spoilers!
		else
		{
			int chosenText = (int) (Math.random() * 10);
			switch(chosenText)
			{
			case 0:
				event.reply("Your lucky number is -1.");
				break;
			case 1:
				event.reply("Your lucky number is \u03C0.");
				break;
			case 2:
				event.reply("Your lucky number is _\\*flips a coin*_ "+(Math.random()<0.5 ? "heads" : "tails")+".");
				break;
			case 3:
				event.reply("Your lucky number is a **BOMB**.");
				event.reply("It goes "+(Math.random() < 0.04 ? "_\\*fizzle*_." : "**BOOM**. $250,000 penalty."));
				break;
			case 4:
				event.reply("This fortune cookie is delicious! Unfortunately, you ate the lucky number.");
				break;
			case -1:
				event.reply("You found the triforce! Go here to claim it: https://www.youtube.com/watch?v=3KANI2dpXLw");
				break;
			case 5:
				event.reply("Your lucky number is "+Math.random()+".");
				break;
			case 6:
				event.reply("Come on, lucky seven! Oops, wrong game.");
				break;
			case 7:
				event.reply("Your lucky number is ~~9~~ SEVEN IT'S 7 YOUR LUCKY NUMBER IS 7 #TEAM7");
				break;
			case 8:
				event.reply("Your lucky number is C4.");
				break;
			case 9: //Added by aug
				event.reply("Your lucky number is 69420. :smirk:");
				break;
			}
		}
	}
}
