/*
 *                             AuctionProg 2.0                        
 *                  Copyright © 2016 Ben Goldsworthy (rumps)        
 *                                                                      
 * A program to facilitate a networked auction system.             
 *                                                                           
 * This file is part of AuctionProg.                                         
 *                                                                            
 * AuctionProg is free software: you can redistribute it and/or modify        
 * it under the terms of the GNU General Public License as published by       
 * the Free Software Foundation, either version 3 of the License, or          
 * (at your option) any later version.                                        
 *                                                                            
 * AuctionProg is distributed in the hope that it will be useful,             
 * but WITHOUT ANY WARRANTY; without even the implied warranty of             
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the              
 * GNU General Public License for more details.                               
 *                                                                            
 * You should have received a copy of the GNU General Public License          
 * along with AuctionProg.  If not, see <http://www.gnu.org/licenses/>.       
 */

/**
 ** This class presents the client view for interacting with the program.
 **/

import java.io.*;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.util.*;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 **   @author  Ben Goldsworthy (rumps) <me+auctionprog@bengoldsworthy.net>
 **   @version 2.0
 **/
public class AuctionClient {
   // This regex is used to ensure the email address entered is a valid
   // RFC 2822 email address format
   // (Source: http://stackoverflow.com/a/153751/4580273)
   private static final Pattern rfc2822 = Pattern.compile("^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$");
   private static Auction a;
   private static ArrayList<String> options, debugOptions;
   private static boolean debugMode;
    
   /**
    **   Sets up the program and runs the event loop.
    **   
    **   @param args Command-line arguments.
    **/
   public static void main(String[] args) {
      debugMode = true;
      
      displayIntro();
      
      // Create the reference to the remote object through the 
      // remiregistry. This comes first because if it fails we can 
      // skip doing everything else.
      try {
         a = (Auction) Naming.lookup("rmi://localhost/AuctionService");
         
         setOptions();
         
         // Authenticates the user, if the program is run with a username
         // as an argument. Otherwise, prompts the user to create a new
         // user.
         UserWrapper currentUser = login((args.length > 0) ? args[0] : "server");
      
         eventLoop(currentUser);
      } catch (Exception e) {
         System.out.println(e);
      }
   }
   
   /*
    *    Loops indefinitely, taking user input for various options.
    */
   private static void eventLoop(UserWrapper currentUser) throws Exception {
      Scanner in = new Scanner(System.in);
      
      while(true) {         
         printOptions();
         try {
            int enteredOption = Integer.parseInt(in.nextLine());
            System.out.println();
            
            if(enteredOption < options.size()) {
               switch(options.get(enteredOption)) {
               // This case takes auction details from the user and then
               // creates a new auction with those.
               case "Create auction":
                  createNewAuction(currentUser);
                  break;
               // This case removes the auction with the given ID, provided it
               // is owned by the current user.
               case "Close auction":
                  closeAuction(currentUser);
                  break;
               // This case displays all the currently-available auctions.
               case "View all auctions":
                  displayAuctions();
                  break;
               // This case places a bid on an auction.
               case "Bid on auction":
                  placeBid(currentUser);
                  break; 
               case "Quit":
                  System.exit(1);
                  break;
               default: break;
               }
            } else if(debugMode) {
               switch(debugOptions.get(enteredOption-options.size())) {
               case "Replicate server":
                  System.out.println("Server replicating...");
                  a.replicate();
                  System.out.println(a.getStatusofLast());
                  break;
               case "Close server":
                  a.close();
                  break;
               default: break;
               }
            }
         } catch (NumberFormatException e) {
            System.out.println("\nError: That is not a valid option.\n");
         }
      }
   }
   
   /*
    *    Logs a user in if they are preexisting (after authentication), or
    *    prompts a user to create a new user if not.
    */
   private static UserWrapper login(String username) throws java.rmi.RemoteException, java.security.NoSuchAlgorithmException, java.security.InvalidKeyException, java.security.SignatureException {
      UserWrapper user = null;
      
      // If the user has run the program with a username argument,
      // authenticate that user.
      if (!username.equals("server")) {
         boolean verifies;
         byte[] challenge, serverResponse, userResponse;
         Signature signed, signing;
         
         // Sends the server a number to sign with its private key.
         System.out.println("Authenticating server...");
         
         challenge = new byte[1024];
         new Random().nextBytes(challenge);
         serverResponse = a.challengeServer(challenge);
         
         signed = Signature.getInstance("SHA1withDSA");
         signed.initVerify(readKey());
         signed.update(challenge);
         
         verifies = signed.verify(serverResponse);
         System.out.println("server signature verifies: " + verifies);
         
         // Receives a number from the server to sign with the user's
         // private key.
         System.out.println("Authenticating user '"+username+"'...");
         
         challenge = a.getChallenge();
         
         signing = Signature.getInstance("SHA1withDSA"); 
         signing.initSign(readKey(username));
         signing.update(challenge);
         userResponse = signing.sign();
         
         verifies = a.returnChallenge(userResponse, username);
         System.out.println(username+" authenticated by server: " + verifies);
         
         // Closes the program on authentication fail.
         if (!verifies) System.exit(-1);
         
         // Otherwise, load the user and move on.
         user = a.getUser(username);
         System.out.println(a.getStatusofLast());
      // If the user has run the program with no arguments, they are
      // prompted to create a new user.
      } else {  
         String name = "", email = "";
         Scanner in = new Scanner(System.in);
         
         while (user == null) {
            System.out.print("Enter name: ");
            name = in.nextLine();
            
            while (!rfc2822.matcher(email).matches()) {
               System.out.print("Enter email: ");
               email = in.nextLine();
               if (!rfc2822.matcher(email).matches()) System.out.println("\nError: invalid email address\n");
            }
            
            while (username.equals("server")) {
               System.out.print("Enter username ('server' is prohibited): ");
               username = in.nextLine();
            }
             
            try {
               user = a.registerUser(new UserWrapper(name, email, username));
               System.out.println(a.getStatusofLast());
            } catch (NullPointerException e) {
               System.out.println(a.getStatusofLast());
               System.exit(0);
            }
         }
         
         // Upon a successful user creation, new keys are generated and
         // exchanged with the server.
         KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
         SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
         keyGen.initialize(1024, random);

         KeyPair pair = keyGen.generateKeyPair();   
         writeKey(pair.getPrivate(), username);
         a.sendPublicKey(pair.getPublic(), username);
         writeKey(a.getPublicKey(), "server");
      }
      return user;
   }
   
   /*
    *    Sets the suite of options to present to the user.
    */
   private static void setOptions() {
      options = new ArrayList<String>();
      options.add("Create auction");
      options.add("Close auction");
      options.add("View all auctions");
      options.add("Bid on auction");
      options.add("Quit");
      
      debugOptions = new ArrayList<String>();
      debugOptions.add("Replicate server");  
      debugOptions.add("Close server"); 
   }
   
   /*
    *    Prints the options for the user.
    */
   private static void printOptions() {
      for(String option: options) {
         System.out.println(options.indexOf(option) +"\t"+option);
      }
      if (debugMode) {
         System.out.println("DEBUG");
         int num = options.size();
         for(String option: debugOptions) {
            System.out.println((num++)+"\t"+option);
         }
      }  
      System.out.print("Choose option: "); 
   }
   
   /*
    *    Prompts for auction details and then sends it off to be validated
    *    and created.
    */
   private static void createNewAuction(UserWrapper currentUser) throws java.rmi.RemoteException {
      float startPrice = 0.0f;
      float reservePrice = 0.0f;
      Scanner in = new Scanner(System.in);
      
      try {
         System.out.print("Enter description: ");
         String desc = in.nextLine();
         
         System.out.print("Enter starting price: \u00A3");
         startPrice = Float.parseFloat(in.nextLine());
         
         while (reservePrice <= startPrice) {
            System.out.print("Enter reserve price (must be higher than starting price): \u00A3");
            reservePrice = Float.parseFloat(in.nextLine());
         }
         
         a.openNewAuction(new AuctionWrapper(0, desc, currentUser, startPrice, reservePrice));
         System.out.println(a.getStatusofLast());
      } catch(NumberFormatException ex){
         System.out.println("\nError: not a valid price\n");
      }   
   }
   
   /*
    *    Closes an auction (after sending the request off for validation).
    */
   private static void closeAuction(UserWrapper currentUser) throws java.rmi.RemoteException {
      int id;
      UserWrapper winner;
      Scanner in = new Scanner(System.in);
      
      if (!a.showAllAuctions().isEmpty()) {
         System.out.print("Enter auction number: ");
         id = Integer.parseInt(in.nextLine());
         
         a.closeAuction(id, currentUser);
         System.out.println(a.getStatusofLast());
      } else {
         System.out.println("\nNo auctions available\n");
      }
   }
   
   /*
    *    Displays the program intro preamble.
    */
   private static void displayIntro() {
      ArrayList<String> lines = new ArrayList<String>();
      lines.add("");
      lines.add("AuctionProg 1.0");
      lines.add("Copyright \u00a9 2016 Ben Goldsworthy (rumps)");
      lines.add("");
      lines.add("A program to facilitate a networked auction system");
      lines.add("This file is part of AuctionProg.");
      lines.add("");
      lines.add("AuctionProg is free software: you can redistribute it and/or modify");
      lines.add("it under the terms of the GNU General Public License as published by");
      lines.add("the Free Software Foundation, either version 3 of the License, or");
      lines.add("(at your option) any later version.");
      lines.add("");
      lines.add("AuctionProg is distributed in the hope that it will be useful,");
      lines.add("but WITHOUT ANY WARRANTY; without even the implied warranty of");
      lines.add("MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
      lines.add("GNU General Public License for more details.");
      lines.add("");
      lines.add("You should have received a copy of the GNU General Public License");
      lines.add("along with AuctionProg.  If not, see <http://www.gnu.org/licenses/>.");
      lines.add("");
      
      for(String line: lines){
         System.out.println(line);
      }
   }
  
   /*
    *    Displays all the available auctions.
    */
   private static void displayAuctions() throws java.rmi.RemoteException {
      if (!a.showAllAuctions().isEmpty()) {
         System.out.println();
         System.out.println("#\tOwner\tPrice\tDesc");
         for (int i = 0; i < 80; i++) System.out.print("-");
         System.out.println();
         for(AuctionWrapper auction: a.showAllAuctions()){
            System.out.println(auction.getID()+"\t"+auction.getOwner().getUsername()+"\t\u00A3"+String.format("%.2f", auction.getPrice())+"\t"+auction.getDesc());
         }
         System.out.println("");
      } else {
         System.out.println("\nNo auctions available\n");
      }
   }
   
   /*
    *    Takes bid details and sends the bid off.
    */
   private static void placeBid(UserWrapper currentUser) throws java.rmi.RemoteException {
      Scanner in = new Scanner(System.in);
      
      if (!a.showAllAuctions().isEmpty()) {
         try{
            int id;
            float price;
            
            System.out.print("Enter auction number: ");
            id = Integer.parseInt(in.nextLine());
            
            System.out.print("Enter bid amount: \u00A3");
            price = Float.parseFloat(in.nextLine());
            
            a.bidOnAuction(id, currentUser, price);
            System.out.println(a.getStatusofLast());
         } catch(NumberFormatException ex){
            System.out.println("\nError: not a valid price\n");
         }
      } else {
         System.out.println("\nNo auctions available\n");
      }   
   }
   
   /*
    *    Reads a user's private key from a file.
    */
   private static PrivateKey readKey(String username) {
      try {
         FileInputStream keyfis = new FileInputStream("../key/client/"+username + "priv.key");
         byte[] encKey = new byte[keyfis.available()];
         keyfis.read(encKey);
         keyfis.close();

         PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(encKey);

         KeyFactory keyFactory = KeyFactory.getInstance("DSA");
         return keyFactory.generatePrivate(privKeySpec);
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
         return null;
      }
   }
   /*
    *    Reads the server's public key from a file.
    */
   private static PublicKey readKey() {
      try {
         FileInputStream keyfis = new FileInputStream("../key/client/serverpub.key");
         byte[] encKey = new byte[keyfis.available()];
         keyfis.read(encKey);
         keyfis.close();   
         
         X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(encKey);
         
         KeyFactory keyFactory = KeyFactory.getInstance("DSA");
         return keyFactory.generatePublic(pubKeySpec);
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
         return null;
      }
   }
   
   /*
    *    Writes a private key to a file.
    */
   private static void writeKey(PrivateKey pKey, String username) {
      wK(pKey, username, "priv");
   }
   /*
    *    Writes a public key to a file.
    */
   private static void writeKey(PublicKey pKey, String username) {
      wK(pKey, username, "pub");
   }
   /*
    *    Actually writes a key to a file.
    */
   private static void wK(Key pKey, String username, String type) {
      try {
         byte[] key = pKey.getEncoded();
         FileOutputStream keyfos = new FileOutputStream("../key/client/"+username+type+".key");
         keyfos.write(key);
         keyfos.close();
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
   }
}
