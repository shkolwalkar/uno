import java.util.*;


public class Main {
   // main class to execute the game multiple times (no need to restart the program)
   public static void main(String[] args) {
      int endCode = 1;
      while (endCode != 0) {
         Uno u = new Uno();
         endCode = u.playUno(); // this plays the game one time
      }
      System.exit(0);
   }
}

class Uno {
   private ArrayList<Card> deck = new ArrayList<Card>(); // deck of cards
   private Stack<Card> trash = new Stack<Card>(); // pile of cards already played
   private ArrayList<Player> players = new ArrayList<Player>(); // list of all players
   private ArrayList<Player> finished = new ArrayList<Player>(); // list of players that dealt all their cards
   private Scanner userInput = new Scanner(System.in); // user input via scanner
   private int numPlayers; // number of players in the game
   private int numPlayersOut = 1; // number of players that have dealt all their cards (set to 1 to signify 1st place)
   private int startHand; // size of each player's starting hand
   private boolean reversed = false; // is the order of players reversed?
   
   private transient int playersHand; // size of the user's hand (not any of the CPUs)
   private transient int counter = 0; // incrementer important for looping through players
   
   private volatile String response; // for uno caller, isn't mutable if non-static
   private volatile boolean timeUp = false; // is time up (multithread)
   private volatile boolean uno = false; // has uno been called (multithreading part)
   
   // method that plays 1 game of Uno
   public int playUno() {
      initialize(); // initializes game
      
      // until player deals all their cards or ends up in last place, play turns
      while (playersHand != 0 && players.size() > 1) {
         if (counter == 0) { // first player the list is the user
            playAsUser();
         }
         else { // play based on the counter's index (i.e. the second player in the list has index 1 in players)
            playAsCPU(counter);
         }
         if (!reversed) { // if not reversed
            counter++; // increase index
            if (counter >= players.size()) { // if above the size of the ArrayList, bring it back to an interval within (0,size)
               counter = counter - players.size();
            }
         }
         else {
            counter--; // decrease index if reversed
            if (counter <= -1) { // if above the below of the ArrayList, bring it back to an interval within (0,size)
               counter = counter + players.size();
            }
         }
      }
      System.out.println("Congratulations! You're Rank " + numPlayersOut + "for this game!"); // congratulates player of breaking the loop
      finished.add(players.remove(0));
      return endGame(); // finish the game
   }
   
   // initializes the game
   public void initialize() {
      // gets # of players and # of starting cards (as desired by user)
      System.out.println("Welcome to a new Uno game!");
      System.out.println("How many players would you like?");
      numPlayers = Integer.parseInt(userInput.nextLine());
      System.out.println("What size is the starting hand");
      startHand = Integer.parseInt(userInput.nextLine());
      
      
      // creation of the number cards
      // for each color
      for (Color c : Color.values()) {
         // 2 cards of values 1-9, and 1 card of value 0
         for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 2; j++) {
               if (j == 1 && i == 0) {
                  continue;
               }
               NumberCard a = new NumberCard(i,c);
               deck.add(a);
            }
         }
         // 2 draw two cards, 2 reverse cards, and 2 skip cards
         for (int k = 0; k < 2; k++) {
            DrawTwoCard d = new DrawTwoCard(c);
            deck.add(d);
            ReverseCard r = new ReverseCard(c);
            deck.add(r);
            SkipCard s = new SkipCard(c);
            deck.add(s);
         }
      }
      // create 4 total wild and wild plus four cards
      for (int l = 0; l < 4; l++) {
         WildCard w = new WildCard();
         deck.add(w);
         WildPlusFourCard x = new WildPlusFourCard();
         deck.add(x);
      }
      // size of a normal deck is 108, check that and shuffle the deck
      assert deck.size() == 108 : "Sizing error occurred";
      Collections.shuffle(deck);
      
      // get user's name
      System.out.println("What's your name?");
      String yourName = userInput.nextLine();
      Player you = new Player(yourName);
      players.add(you);
      
      // get a name for each player: if no name entered, then it will default to a random seed out of a list
      for (int m = 1; m < numPlayers; m++) {
         System.out.println("Type in the name for Player " + m + ". Leave it blank if you don't have a name idea.");
         String playerName = userInput.nextLine();
         if (playerName.equals("")) {
            Player p = new Player();
            players.add(p);
         }
         else {
            Player p = new Player(playerName);
            players.add(p);
         }
      }
      
      // distribute cards to players
      for (Player player : players) {
         for (int n = 0; n < startHand; n++) {
            player.hand.add(deck.remove(0));
         }
      }
      
      // set the size of the user's hand
      playersHand = players.get(0).hand.size();
      System.out.println("Ready to start game!");
      
      // flip over first card into "played" pile (a.k.a. trash)
      trash.push(deck.remove(0));
      // if the trash card is wild, then just draw another
      while (trash.peek().type == Type.WILD || trash.peek().type == Type.WILD_PLUS_FOUR || trash.peek().type == Type.PLUS_TWO) {
         trash.push(deck.remove(0));
      }
      
   }
   
   // play as the user (entering inputs)
   public int playAsUser() {
      Player thisPlayer = players.get(0);
      System.out.println("It's your turn!");
      // there are continued pauses set throughout this program to ensure that user has time to read the text
      try {
         Thread.sleep(2000);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      
      // check the top trash card (user may need to draw 2 or 4)
      if (trash.peek().type == Type.PLUS_TWO) {
         System.out.println("Looks like you have to draw two...");
         for (int i = 0; i < 2; i++) {
            draw(0);
         }
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
         finally {
            System.out.println("Cards drawn!");
         }
      }
      if (trash.peek().type == Type.WILD_PLUS_FOUR) {
         System.out.println("Looks like you have to draw four...");
         for (int i = 0; i < 4; i++) {
            draw(0);
         }
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
         finally {
            System.out.println("Cards drawn!");
         }
      }
      
      // lists player their cards
      System.out.println("Here are your cards:");
      int i = 0;
      for (Card c : thisPlayer.hand) {
         System.out.println(i + ". " + c.toString());
         fillUsableHand(0,c); // add cards that are usable in this turn to a separate list
         i++;
      }
      
      // uno call (if user has only 1 card left)
      if (thisPlayer.hand.size() == 1) {
         System.out.println("What do you need to say?");
         attemptUnoCall();
      }
      
      try {
         Thread.sleep(2000);
      }
      catch (InterruptedException e) {
         e.printStackTrace();
      }
      
      // show top trash card
      System.out.println("Here's the card atop the deck:");
      System.out.println(trash.peek().toString());
      try {
         Thread.sleep(2000);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      
      
      // if the player has a usable hand (a.k.a. they can play a card)
      if (thisPlayer.usableHand.size() > 0) {
         // show player their usable hand
         System.out.println("As such, this is your usable hand:");
         int j = 0;
         for (Card d : thisPlayer.usableHand) {
            System.out.println(j + ". " + d.toString());
            j++;
         }
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
         // have them choose a card
         System.out.println("From the usable hand, in the index of the card you want to place atop the deck.");
         int nextIndex = Integer.parseInt(userInput.nextLine());
         Card top = thisPlayer.usableHand.remove(nextIndex);
         // remove that card from the deck, and push it to the trash
         thisPlayer.hand.remove(top);
         trash.push(top);
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      // if the player's usable hand is empty
      else {
         System.out.println("Your usable hand is empty! Let's add cards to it.");
         System.out.println("Type in anything to add a card to your hand.");
         // keep drawing until a card drawn is usable
         String input;
         while (players.get(0).usableHand.size() == 0) {
            try {
               Thread.sleep(2000);
            }
            catch (Exception e) {
               e.printStackTrace();
            }
            input = userInput.nextLine();
            Card g = draw(0);
            fillUsableHand(0,g);
            System.out.println("Card drawn: " + g.toString());
         }
         // plays the first usable card on the top of the trash pile
         System.out.println("This card is usable! Playing it automatically...");
         Card top = players.get(0).usableHand.get(0);
         thisPlayer.hand.remove(top);
         trash.push(top);
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      
      // if the card is wild, make the user choose the color
      if (trash.peek().type == Type.WILD || trash.peek().type == Type.WILD_PLUS_FOUR) {
         System.out.println("Choose a color to switch to!");
         System.out.println("Type '0' for red, '1' for yellow, '2' for green, and '3' for blue.");
         int newColor = userInput.nextInt();
         userInput.nextLine();
         trash.peek().setColor(Color.values()[newColor]);
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      // if the card is reversed, reverse it
      else if (trash.peek().type == Type.REVERSE) {
         reversed = !reversed;
         System.out.println("Looks like we have to reverse the order of players!");
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      // if the card is skipped, change counter depending on reverse status
      else if (trash.peek().type == Type.SKIP) {
         System.out.println("Looks like someone got skipped...");
         if (reversed) {
            counter--;
         }
         else {
            counter++;
         }
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      
      thisPlayer.usableHand.clear(); // clear the usable hand
      return thisPlayer.hand.size();
   }
   
   // play as a CPU (params: the index of the CPU player)
   // this is essentially the same method as playAsUser, with less printing to the user
   // i'll note key changes in comments
   public int playAsCPU(int p) {
      Player thisPlayer = players.get(p);
      Card top;
      System.out.println("It's " + thisPlayer.name + "'s turn!");
      try {
            Thread.sleep(2000);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      
      if (trash.peek().type == Type.PLUS_TWO) {
         System.out.println("Looks like " + thisPlayer.name + " has to draw two...");
         for (int i = 0; i < 2; i++) {
            draw(p);
         }
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      if (trash.peek().type == Type.WILD_PLUS_FOUR) {
         System.out.println("Looks like " + thisPlayer.name + " has to draw four...");
         for (int i = 0; i < 4; i++) {
            draw(p);
         }
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      
      for (Card c : thisPlayer.hand) {
         fillUsableHand(p,c);
      }
      
      if (thisPlayer.usableHand.size() > 0) {
         // instead of user input, the CPU randomly chooses a card out of the usable hand
         int nextIndex = (int)(Math.random() * thisPlayer.usableHand.size());
         top = thisPlayer.usableHand.remove(nextIndex);
         thisPlayer.hand.remove(top);
      
         if (thisPlayer.hand.size() == 0) {
            // game continues until USER gets out
            System.out.println(thisPlayer.name + " is Rank " + numPlayersOut + " for this game!");
            finished.add(players.remove(p));
            try {
               Thread.sleep(2000);
            }
            catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
      else {
         System.out.println(thisPlayer.name + " is drawing!");
         while (thisPlayer.usableHand.size() == 0) {
            Card g = draw(p);
            fillUsableHand(p,g);
         }
         top = thisPlayer.usableHand.get(0);
         thisPlayer.hand.remove(top);
         try {
               Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }

      }
      trash.push(top);
      System.out.println("Card Played: " + top.toString());
      try {
            Thread.sleep(2000);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      
      // if the CPU plays a wild card, the color is randomly chosen
      // an alternative to make it harder would be to adjust the proportion of each color based on the proportion of that color in the CPU's hand
      if (trash.peek().type == Type.WILD || trash.peek().type == Type.WILD_PLUS_FOUR) {
         int randomColor = (int)(Math.random() * 4);
         trash.peek().setColor(Color.values()[randomColor]);
         System.out.println(thisPlayer.name + " selected the color " + trash.peek().color.toString() + "!");
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      else if (trash.peek().type == Type.REVERSE) {
         System.out.println("Looks like we have to reverse the order of players!");
         reversed = !reversed;
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      else if (trash.peek().type == Type.SKIP) {
         System.out.println("Looks like someone got skipped...");
         if (reversed) {
            counter--;
         }
         else {
            counter++;
         }
         try {
            Thread.sleep(2000);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      
      System.out.println(thisPlayer.name + " has " + thisPlayer.hand.size() + " cards left!");
      try {
         Thread.sleep(2000);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      thisPlayer.usableHand.clear();
      return thisPlayer.hand.size();
   }
   
   // checks conditions to fill the usableHand
   private void fillUsableHand(int pNum, Card c) {
      Card p = trash.peek();
      // if the colors are the same
      if (c.color == p.color) {
            players.get(pNum).usableHand.add(c);
      }
      // if the numbers are the same
      else if (c.type == p.type && c.type == Type.NUMBER) {
         
         if (c.value == p.value) {
            players.get(pNum).usableHand.add(c);
         }
      }
      // if the types (skip, reverse, draw two) are the same
      else if (c.type == p.type && c.type == Type.SKIP) {
         players.get(pNum).usableHand.add(c);
      }
      else if (c.type == p.type && c.type == Type.REVERSE) {
         players.get(pNum).usableHand.add(c);
      }
      else if (c.type == p.type && c.type == Type.PLUS_TWO) {
         players.get(pNum).usableHand.add(c);
      }
      // if the card is wild
      else if (c.type == Type.WILD || c.type == Type.WILD_PLUS_FOUR) {
         players.get(pNum).usableHand.add(c);
      }
      else if (p.type == Type.WILD || p.type == Type.WILD_PLUS_FOUR) {
         if (c.color == p.color) {
            players.get(pNum).usableHand.add(c);
         }
      }
   }
   
   // multithreaded uno call
   // waits a random amount of seconds, giving user time to enter 'UNO!' -> if not called in time, user has to draw two cards
   // currently, CPUs are immune to this
   private void attemptUnoCall() {
      response = ""; // resetting these variables, for multiple calls in 1 game
      uno = false;
      timeUp = false;
      
      // first thread: waiting for user input
      Thread inputThread = new Thread(() -> {
         // if time isn't up, and the response is "uno", then set the uno variable to true
         if (!timeUp) {
            response = userInput.nextLine();
            if (response.trim().equalsIgnoreCase("uno")) {
               uno = true;
            }
         }
      });
      
      // second thread: puts program to sleep
      Thread sleepThread = new Thread(() -> {
         int randomMillis = 2000 + (int)(Math.random() * 3000); // random amount of time from 2-5 seconds
         System.out.println(randomMillis); // test code
         try {
            Thread.sleep(randomMillis);
         }
         catch (Exception e) {
            return;
         }
         timeUp = true; // tells other thread that time is up
         if (!uno) {
            System.out.println("Didn't call Uno in time...");
            for (int i = 0; i < 2; i++) {
               Card c = draw(0);
               System.out.println("Card drawn: " + c.toString());
            }
         }
      });
      
      // starts both threads
      inputThread.start();
      sleepThread.start();
      
      // wait for both threads to finish before continuing with game
      try {
         inputThread.join();
         sleepThread.join();
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      
   }
   
   // draws a new card (param: player index in players ArrayList)
   public Card draw(int p) {
      Card ret;
      // if deck isn't empty, remove first card
      if (deck.size() > 0) {
         ret = deck.remove(0);
      }
      // if deck is empty, remove all cards from trash, shuffle them around, and add them to the deck
      else {
         List<Card> trashAsList = new ArrayList<Card>();
         while (trash.isEmpty()) {
            trashAsList.add(trash.pop());
            System.out.println("card");
         }
         Collections.shuffle(trashAsList);
         for (Card f : trashAsList) {
            deck.add(f);
         }
         // THEN take the first card
         ret = deck.remove(0);
      }
      players.get(p).hand.add(ret);
      return ret;
   }
   
   // ends the game (from the user's perspective)
   public strictfp int endGame() {
      // remove all remaining CPUs, in order of # cards 
      while(!players.isEmpty()) {
         Player lowest = players.get(0);
         for (Player p : players) {
            if (p.hand.size() < lowest.hand.size()) {
               lowest = p;
            }
         }
         finished.add(lowest);
         players.remove(lowest);
      } 
      // list rankings, based on (1) the order of players leaving the game, and (2) the # of cards they had remaining when the user finished
      // if the user was in last, then this isn't applicable
      for (int i = 0; i < finished.size(); i++) {
         System.out.println("Rank " + i + ": " + finished.get(i) + " -- " + finished.get(i).hand.size() + " cards left");
      }
      // option to play a new game
      System.out.println("Would you like to play again?");
      System.out.println("Type any number for yes, and type '0' for no.");
      return userInput.nextInt();
   }
   
    
}

// each object represents one Player
class Player {
   protected String name; // player's name
   protected ArrayList<Card> hand = new ArrayList<Card>(); // player's hand
   protected ArrayList<Card> usableHand = new ArrayList<Card>(); // player's usable hand (refreshed every turn)
   
   // list of random names in case player doesn't have any for their CPUs
   String[] names = {
            "Aaliyah", "Jasmine", "Malik", "Isaiah", "Imani", "Denzel","Mateo", "Valentina", "Santiago", "Isabela", 
            "Diego", "Camila", "Andres", "Lucia", "Javier", "Xochitl","Chen", "Mei", "Hiroshi", "Sakura", 
            "Jian", "Wei", "Yuna", "Minho", "Aarav", "Priya", "Linh", "Duy", "Zain","Omar", 
            "Yasmine", "Tariq", "Zainab", "Nadia", "Aisha", "Yojit", "Deacon","Tatanka",
            "Emily", "Michael", "Isabella", "William", "Sophia", "James", "Olivia", "Benjamin", "Ava", "Alexander",
            "Ethan", "Mia", "Henry", "Charlotte", "Joseph", "Amelia", "David", "Harper", "Samuel", "Evelyn",
            "Daniel", "Abigail", "Matthew", "Elizabeth", "Anthony", "Sofia", "Andrew", "Grace", "Joshua",
            "Kai", "Jordan", "River"
   };
   
   // if name given by user, use that name
   Player(String n) {
      name = n;
   }
   
   // if not, use a random name from the list
   Player() {
      List<String> namesList = new ArrayList<String>(Arrays.asList(names));
      name = namesList.remove(0); 
      Collections.shuffle(namesList);
   }
   
}

// represents the TYPE of card
enum Type {
   NUMBER,
   REVERSE,
   SKIP,
   PLUS_TWO,
   WILD,
   WILD_PLUS_FOUR;
   
   @Override
   public String toString() { // for user display
      switch (this.ordinal()) { // uses ordinal to print
         case 0:
            return "number";
         case 1:
            return "reverse";
         case 2:
            return "skip";
         case 3:
            return "draw two";
         case 4:
            return "wild";
         case 5:
            return "wild plus four";
         default:
            return null;
      }
   }
}

// represents the COLOR of card
enum Color {
   RED,
   YELLOW,
   GREEN,
   BLUE;
   
   @Override
   public String toString() { // for user display
      switch(this.ordinal()) { // uses ordinal print
         case 0:
            return "red";
         case 1:
            return "yellow";
         case 2:
            return "green";
         case 3:
            return "blue";
         default:
            return null;
      }
   }
}

// one object represents one Card
abstract class Card {
   public final Type type; // type of card
   public Color color; // color of card
   public final int value; // value of card (if card has a numeric value)
   
   // for number cards (set a type, color, and value)
   Card(Type t, Color c, int v) {
      type = t;
      color = c;
      value = v;
   }
   
   // for skip, reverse, and plus two cards (set a type and color)
   Card(Type t, Color c) {
      type = t;
      color = c;
      value = -1; // set value to -1 if it doesn't exist
   }
   
   // for wild cards
   Card(Type t) {
      type = t;
      color = null; // set color to null if it doesn't exist
      value = -1;
   } 
   
   // for user display
   public String toString() {
      return type.toString();
   }
   
   // set the color (for wild cards only)
   public void setColor(Color c) {}
}

// number cards
class NumberCard extends Card {

   NumberCard(int v, Color c) {
      super(Type.NUMBER,c,v); // type is number, color and value are given in deck creation loops in initialize()
   }
   
   public String toString() {
      return color.toString() + value; // i.e. "blue4" 
   }
}

// wild cards (NOT draw 4)
class WildCard extends Card {
  
   WildCard() {
      super(Type.WILD); // type is wild
   }
   
   public String toString() {
      return super.toString();
   }
   
   public void setColor(Color c) {
      color = c; // color is "set" based on the color the user/CPU chooses when the card is played
   }
}

// wild cards (draw 4) -- essentially the same 
class WildPlusFourCard extends Card {
   
   WildPlusFourCard() {
      super(Type.WILD_PLUS_FOUR);
   }
   
   public String toString() {
      return super.toString();
   }
   
   public void setColor(Color c) {
      color = c;
   }
}

// draw two cards
class DrawTwoCard extends Card {
   
   DrawTwoCard(Color c) {
      super(Type.PLUS_TWO, c); // type is plus two, color set by deck creation loops in initialize()
   }
   
   public String toString() {
      return super.toString() + color.toString(); // i.e. "draw 2 red"
   }
}

// reverse and skip cards work the same as draw two cards (both constructor and toString())
class ReverseCard extends Card {
   
   ReverseCard(Color c) {
      super(Type.REVERSE, c);
   }
   
   public String toString() {
      return super.toString() + color.toString(); // i.e. "reversered"
   }

}

class SkipCard extends Card {
   
   SkipCard(Color c) {
      super(Type.SKIP, c);
   }
   
   public String toString() {
      return super.toString() + color.toString(); // i.e. "skipred"
   }

}