package tel.discord.rtab;

import java.util.Comparator;

public class DescendingScoreSorter implements Comparator<String> {

	@Override
	public int compare(String arg0, String arg1) {
		String[] string0 = arg0.split("#");
		String[] string1 = arg1.split("#");
		int test0 = Integer.parseInt(string0[2]);
		int test1 = Integer.parseInt(string1[2]);
		//Deliberately invert it to get descending order
		return test1 - test0;
	}

}
