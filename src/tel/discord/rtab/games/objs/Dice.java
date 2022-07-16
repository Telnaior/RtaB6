package tel.discord.rtab.games.objs;

public class Dice {
    private final int[] dice;
    private final int numFaces;

    public Dice() {
        this(2,6);
    }

    public Dice(int numDice) {
        this(numDice, 6);
    }

    public Dice(int numDice, int numFaces) {
        dice = new int[numDice];
        this.numFaces = numFaces;
    }

    public int[] getDice() {
        return dice;
    }

	public int getNumFaces() {
        return numFaces;
    }
	
    public int getDiceTotal() {
        int sum = 0;

        for (int die : dice) sum += die;
        
            return sum;
    }

    public void rollDice() {
        for (int i = 0; i < dice.length; i++)
            dice[i] = (int)(Math.random() * numFaces) + 1;
    }

    public String toString() {
        String output = "";

        for (int die : dice) output += (die + " ");

        return output;
    }
}