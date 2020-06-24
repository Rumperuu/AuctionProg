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
 ** This class provides the impementation for the AuctionServer.
 **/

import java.io.*;
import java.util.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 **   @author  Ben Goldsworthy (rumps) <me+auctionprog@bengoldsworthy.net>
 **   @version 1.0
 **/
public class AuctionImpl extends java.rmi.server.UnicastRemoteObject implements Auction {
   private ArrayList<AuctionWrapper> auctions;
   private ArrayList<UserWrapper> users;
   String status;
   byte[] challenge = new byte[1024];
   
   /**   
    **   Constructor Method. Required to declare the `RemoteException`
    **   instance.
    **/
   public AuctionImpl() throws java.rmi.RemoteException {
      super();
      
      System.out.println("Server initilising...");
      
      auctions = new ArrayList<AuctionWrapper>();
      users = new ArrayList<UserWrapper>();
      status = "";
      generateKeys();
      
      System.out.println("Server start successful.");
   }
   
   private void generateKeys() {
      try {
         KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
         SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
         keyGen.initialize(1024, random);
         
         KeyPair pair = keyGen.generateKeyPair();
         writeKey(pair.getPublic());
         writeKey(pair.getPrivate());
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
   }
   
   /**
    **   Creates an auction with the given details, generating a new ID for 
    **   it.
    **   
    **   @param desc The auction description.
    **   @param owner The auction owner/creator.
    **   @param startingPrice The auction starting price.
    **   @param reserve The auction reserve price.
    **/
   public void createAuction(String desc, UserWrapper owner, float startingPrice, float reserve) throws java.rmi.RemoteException {
      int id = 0;
      String preamble = "Create new auction: ";
      
      lines();
      System.out.println(preamble + "begin.");
      
      // Gives the new auction the lowest unclaimed ID.
      for(AuctionWrapper item: auctions){
         if (id <= item.getID()) {
            id = item.getID();
         }
      }
      
      auctions.add(new AuctionWrapper(++id, desc, owner, startingPrice, reserve));
      System.out.println(preamble + "auction "+id+" successfully created.");
      status = "Auction no. "+id+" successfully created.";
      
      System.out.println(preamble + "end.");
      lines();
   }
   
   /**
    **   Removes an auction, provided the user calling the method owns it.
    **   If the reserve price was met, also returns the winning bidder.
    **   
    **   @param id The ID of the auction to remove.
    **   @param currentUser The user calling the method.
    **   @return The `UserWrapper` of the highest bidder, if applicable.
    **/
   public UserWrapper removeAuction(int id, UserWrapper currentUser) throws java.rmi.RemoteException {
      AuctionWrapper auction = this.getAuction(id);
      UserWrapper response = null;
      String preamble = "Close auction "+id+": ";
      
      lines();
      System.out.println(preamble + "begin.");
      
      if (auction.getOwner().getUsername().equals(currentUser.getUsername())) {
         System.out.println(preamble + "ownership rights confirmed.");
         auctions.remove(auction);
         System.out.println(preamble + "auction successfully closed.");
         status = "Auction no. "+id+" successfully removed";
         if (auction.getPrice() >= auction.getReserve()) {
            System.out.println(preamble + "auction closed with winner.");
            status = "Auction closed - winner";
            response = auction.getHighestBidder();
         } else {
            System.out.println(preamble + "auction closed with no winner.");
            status = "Auction closed - no winner";
         }
      } else {     
         System.out.println(preamble + "invalid ownership rights.");
         status = "Auction no. "+id+" could not be removed - you do not own this auction";
      }
      
      System.out.println(preamble + "end.");
      lines();
      return response;
   }
   
   /**
    **   Bids on a given auction, provided the bid is more than the
    **   current highest price.
    **   
    **   @param id The ID of the auction in question.
    **   @param bidder The user bidding on the auction.
    **   @param price The amount bid.
    **/
   public void bidOnAuction(int id, UserWrapper bidder, float price) 
      throws java.rmi.RemoteException {
      AuctionWrapper auction;
      String preamble = "Bid on auction "+id+": ";
      
      lines();
      System.out.println(preamble + "begin.");
      
      if ((auction = this.getAuction(id)) != null) {
         if (price > auction.getPrice()) {
            this.getAuction(id).setBid(bidder, price);
            System.out.println(preamble + "bid successful.");
            status = "Bid successful";
         } else {
            System.out.println(preamble + "bid less than current highest bid.");
            status = "Price less than highest bid";
         }
      } else {
         System.out.println(preamble + "auction ID not found.");
         status = "Invalid auction ID";
      }
      
      System.out.println(preamble + "end.");
      lines();
   }
   
   /**
    **   Accessor Method. Retrieves an auction by its ID.
    **   
    **   @param id The ID of the auction to retrieve.
    **   @return The `AuctionWrapper` indicated.
    **/
   private AuctionWrapper getAuction(int id) {
      for(AuctionWrapper auction: auctions){
         if (auction.getID() == id) {
            return auction;
         }
      }
      return null;
   }
   
   /**
    **   Accessor Method. Retrieves a list of all the auctions.
    **   
    **   @return An `ArrayList` of `AuctionWrapper`s.
    **/
   public ArrayList<AuctionWrapper> getAuctions() throws java.rmi.RemoteException {
      return auctions;
   }
   
   /**
    **   Accessor Method. Gets a user, or creates a new one if non 
    **   exists with the given details.
    **   
    **   @param name The user's name.
    **   @param email The user's email address.
    **   @return The relevant `UserWrapper`.
    **/
   public UserWrapper getUser(String username) throws java.rmi.RemoteException {
      for(UserWrapper user: users){
         if (user.getUsername().equals(username)) {
            status = "Welcome back, "+user.getName()+".";
            return user;
         }
      }
      status = "No such user.";
      return null;
   }
   
   public UserWrapper registerUser(String name, String email, String username) throws java.rmi.RemoteException {
      for(UserWrapper user: users){
         if (user.getUsername().equals(username)) {
            status = "Username taken. Either choose a new username or, if trying to login to an existing account, rerun the program as 'AuctionClient <username>'.";
            return null;
         }
      }
      
      UserWrapper user = new UserWrapper(name, email, username);
      users.add(user);
      status = "New user created. Hello "+name+".";
      return user;
   }
   
   /**
    **   Accessor Method. Gets the status resulting from the last action
    **   attempted.
    **   
    **   @return The status message.
    **/
   public String getStatusofLast() {
      return "\n"+status+"\n";
   }
   
   public PublicKey getPublicKey() throws java.rmi.RemoteException {
      return this.readKey("server");
   }
   
   public void sendPublicKey(PublicKey key, String username) throws java.rmi.RemoteException {
      this.writeKey(key, username);
   }
   
   private static void writeKey(PrivateKey pKey) throws java.rmi.RemoteException {
      try {
         byte[] key = pKey.getEncoded();
         FileOutputStream keyfos = new FileOutputStream("../key/serverpriv.key");
         keyfos.write(key);
         keyfos.close();
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
   }
   
   private static void writeKey(PublicKey pKey) throws java.rmi.RemoteException {
      try {
         byte[] key = pKey.getEncoded();
         FileOutputStream keyfos = new FileOutputStream("../key/serverpub.key");
         keyfos.write(key);
         keyfos.close();
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
   }
   
   private static void writeKey(PublicKey pKey, String username) throws java.rmi.RemoteException {
      try {
         byte[] key = pKey.getEncoded();
         FileOutputStream keyfos = new FileOutputStream("../key/"+username+"pub.key");
         keyfos.write(key);
         keyfos.close();
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
   }
   
   private static PrivateKey readKey() throws java.rmi.RemoteException {
      try {
         FileInputStream keyfis = new FileInputStream("../key/serverpriv.key");
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
   
   private static PublicKey readKey(String username) throws java.rmi.RemoteException {
      try {
         FileInputStream keyfis = new FileInputStream("../key/"+username+"pub.key");
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

   
   public byte[] challengeServer(byte[] challenge) throws java.rmi.RemoteException {   
      try {
         Signature dsa = Signature.getInstance("SHA1withDSA"); 
         dsa.initSign(this.readKey());
         dsa.update(challenge);
         byte[] realSig = dsa.sign();
         
         return realSig;
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
         return null;
      }
   }
   
   public byte[] getChallenge() throws java.rmi.RemoteException {
      challenge = new byte[1024];
      new Random().nextBytes(this.challenge);
      return this.challenge;
   }
   
   public boolean returnChallenge(byte[] retChal, String username) throws java.rmi.RemoteException {
      System.out.println("Authenticating user '"+username+"'...");
      for(UserWrapper user: users){
         if (user.getUsername().equals(username)) {
            try {
               Signature sig = Signature.getInstance("SHA1withDSA");
               sig.initVerify(this.readKey(username));

               sig.update(this.challenge);
               
               boolean verifies = sig.verify(retChal);
               System.out.println(username+" signature verifies: " + verifies);
               return verifies;
            } catch (Exception e) {
               System.out.println();
               System.out.println("Exception");
               System.out.println(e);
            }
         }
      }
      System.out.println("User '"+username+"' not found.");
      return false;
   }
   
   public void close() throws java.rmi.RemoteException {
      System.exit(-1);
   }
   
   private void lines() {
      for (int i = 0; i < 80; i++) System.out.print("-");
      System.out.print("\n");
   }
}

