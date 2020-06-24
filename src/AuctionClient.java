/*
 *                             AuctionProg 1.0                        
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
 **   @author  Ben Goldsworthy (rumps) <bgoldsworthy96 @ gmail.com>
 **   @version 1.0
 **/
public class AuctionClient {
   // This regex is used to ensure the email address entered is a valid
   // email address format
   // (Source: http://stackoverflow.com/a/153751/4580273)
   private static final Pattern rfc2822 = Pattern.compile("^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$");
   
   /**
    **   Displays the UI.
    **   
    **   @param args Command-line arguments.
    **/
   public static void main(String[] args) {
      try {
         // Create the reference to the remote object through the 
         // remiregistry. This comes first because if it fails we can 
         // skip doing everything else.
         Auction a = (Auction) Naming.lookup("rmi://localhost/AuctionService");
       
         // Declares some variables that'll be used later on.
         Scanner in = new Scanner(System.in);
         UserWrapper currentUser = null;
         int id;      
         float price;
         
         // Authenticates the user, if the program is run with a username
         // as an argument. Otherwise, prompts the user to create a new
         // user.
         currentUser = login(a, (args.length > 0) ? args[0] : "server");
        
         while(true) {
            // Resets the variables after each run through.
            price = 0.0f;
            
            printOptions();
            
            switch(in.nextLine()) {
            // This case takes auction details from the user and then
            // creates a new auction with those.
            case "1":
               createNewAuction(a, currentUser);
               break;
            // This case removes the auction with the given ID, provided it
            // is owned by the current user.
            case "2":
               deleteAuction(a, currentUser);
               break;
            // This case displays all the currently-available auctions.
            case "3":
               displayAuctions(a);
               break;
            // This case places a bid on an auction.
            case "4":
               placeBid(a, currentUser);
               break; 
            // This case exits the program.
            case "5":
               System.exit(1);
               break;
            // The below cases are remote debug commands.
            // This case remotely shuts down the server.
            case "6":
               a.close();
               break;
            default:
               break;
            }
         }
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
      System.exit(-1);
   }
   
   private static UserWrapper login(Auction a, String username) throws java.rmi.RemoteException, java.security.NoSuchAlgorithmException, java.security.InvalidKeyException, java.security.SignatureException {
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
            
            System.out.print("Enter email: ");
            while (!rfc2822.matcher(email).matches()) {
               email = in.nextLine();
               if (!rfc2822.matcher(email).matches()) System.out.println("\nError: invalid email address\n");
            }
            
            while (username.equals("server")) {
               System.out.print("Enter username ('server' is prohibited): ");
               username = in.nextLine();
            }
                     
            user = a.registerUser(name, email, username);
            System.out.println(a.getStatusofLast());
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
   
   private static void printOptions() {
      System.out.println("1\tCreate auction\n2\tDelete auction\n3\tView auctions\n4\tBid on auction\n5\tQuit\n6\tClose server");
      System.out.print("Choose option: ");   
   }
   
   private static void createNewAuction(Auction a, UserWrapper currentUser) throws java.rmi.RemoteException {
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
         
         a.createAuction(desc, currentUser, startPrice, reservePrice);
         System.out.println(a.getStatusofLast());
      } catch(NumberFormatException ex){
         System.out.println("\nError: not a valid price\n");
      }   
   }
   
   private static void deleteAuction(Auction a, UserWrapper currentUser) throws java.rmi.RemoteException {
      int id;
      UserWrapper winner;
      Scanner in = new Scanner(System.in);
      
      if (!a.getAuctions().isEmpty()) {
         System.out.print("Enter auction number: ");
         id = Integer.parseInt(in.nextLine());
         
         try {
            winner = a.removeAuction(id, currentUser);
            System.out.println("\nAuction won by: "+winner.getName()+" <"+winner.getEmail()+">\n");
         } catch(NullPointerException e) {
            System.out.println(a.getStatusofLast());
         }
      } else {
         System.out.println("\nNo auctions available\n");
      }
   }
   
   private static void displayAuctions(Auction a) throws java.rmi.RemoteException {
      if (!a.getAuctions().isEmpty()) {
         System.out.println("#\tOwner\tPrice\tDesc");
         for (int i = 0; i < 80; i++) System.out.print("-");
         for(AuctionWrapper auction: a.getAuctions()){
            System.out.println(auction.getID()+"\t"+auction.getOwner().getUsername()+"\t\u00A3"+String.format("%.2f", auction.getPrice())+"\t"+auction.getDesc());
         }
         System.out.println("");
      } else {
         System.out.println("\nNo auctions available\n");
      }
   }
   
   private static void placeBid(Auction a, UserWrapper currentUser) throws java.rmi.RemoteException {
      Scanner in = new Scanner(System.in);
      
      if (!a.getAuctions().isEmpty()) {
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
   
   private static PrivateKey readKey(String username) {
      try {
         FileInputStream keyfis = new FileInputStream(username + "priv.key");
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
   private static PublicKey readKey() {
      try {
         FileInputStream keyfis = new FileInputStream("serverpub.key");
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
   
   private static void writeKey(PrivateKey pKey, String username) {
      wK(pKey, username, "priv");
   }
   private static void writeKey(PublicKey pKey, String username) {
      wK(pKey, username, "pub");
   }
   private static void wK(Key pKey, String username, String type) {
      try {
         byte[] key = pKey.getEncoded();
         FileOutputStream keyfos = new FileOutputStream(username+type+".key");
         keyfos.write(key);
         keyfos.close();
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
   }
}