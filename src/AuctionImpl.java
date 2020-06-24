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
 ** This class provides the impementation for the AuctionServer.
 **/

import java.io.*;
import java.util.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.View;
import org.jgroups.util.*;
import java.lang.reflect.InvocationTargetException;

/**
 **   @author  Ben Goldsworthy (rumps) <me+auctionprog@bengoldsworthy.net>
 **   @version 2.0
 **/
public class AuctionImpl extends java.rmi.server.UnicastRemoteObject implements Auction {
   private ArrayList<AuctionWrapper> auctions;
   private ArrayList<UserWrapper> users;
   String status;
   byte[] challenge = new byte[1024];
   static JChannel channel;
   static RpcDispatcher disp;
   static RequestOptions opts = new RequestOptions(ResponseMode.GET_ALL, 5000);
   
   /**   
    **   Constructor Method. Required to declare the `RemoteException`
    **   instance. Also sets up three replicas and creates a test suite of
    **   three auctions.
    **/
   public AuctionImpl() throws java.rmi.RemoteException {
      super();
      
      System.out.println("Server initilising...");
      
      auctions = new ArrayList<AuctionWrapper>();
      users = new ArrayList<UserWrapper>();
      status = "";
      generateKeys();
      
      System.out.println("Server initiliasation successful.");
      
      try {
         channel=new JChannel();
         disp = new RpcDispatcher(channel, this);
         channel.connect("AuctionProg");
         
         System.out.println("Creating replicas...");
         new Replica().start();
         new Replica().start();
         new Replica().start();
         System.out.println("Replicas created.");
         
         UserWrapper testUser = new UserWrapper("Test", "test@test.com", "test");
         this.openNewAuction(new AuctionWrapper(1, "test1", testUser, 12.0f, 14.0f));
         this.openNewAuction(new AuctionWrapper(2, "test2", testUser, 12.0f, 14.0f));
         this.openNewAuction(new AuctionWrapper(3, "test3", testUser, 12.0f, 14.0f));
      } catch (Exception e) {
         System.out.println(e);
      }
   }
   
   /*
    *    Generates the pair of keys for the server's authentication and
    *    writes them to files.
    */
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
   
   /*
    *    Methods to validate operations and then remotely invoke methods
    *    using JGroups.
    *
    */
   
   /**
    **   Invokes the creation of a new auction.
    **   
    **   @param newAuction The new auction to create.
    **/
   public void openNewAuction(AuctionWrapper newAuction) throws java.rmi.RemoteException {
      System.out.println("Opening new auction...");
      try {
         disp.callRemoteMethods(null, "createAuction", new Object[]{newAuction}, new Class[]{AuctionWrapper.class}, opts);
         status = "Auction successfully opened.";
         System.out.println("Auction successfully opened.");
         return;
      } catch (Exception e) {
         System.out.println(e);
      }
         status = "Auction could not be opened.";
      System.out.println("Auction unsuccessfully opened.");
   }
   
   /**
    **   Validates that the calling user owns the auction in question, and
    **   if invokes the closing of the auction, returning the winning
    **   bidder (if applicaable).
    **   
    **   @param id The ID of the auction to bid on.
    **   @param currentUser The user placing the bid.
    **   @return The highest bidder, or `null`.
    **/
   public void closeAuction(int id, UserWrapper currentUser) throws java.rmi.RemoteException {
      System.out.println("Closing auction #"+id+"...");
      AuctionWrapper auction = this.getAuction(id);
      UserWrapper response = null;
      
      if (auction.getOwner().getUsername().equals(currentUser.getUsername())) {
         System.out.println("Ownership rights confirmed.");
         try {
            RspList rsp_list = disp.callRemoteMethods(null, "removeAuction", new Object[]{auction}, new Class[]{AuctionWrapper.class}, opts);
            System.out.println("Auction successfully closed.");
            
            response = (UserWrapper)rsp_list.getFirst();
            if (response.getUsername().equals("server")) {
               status = "Auction closed with no winner";
            } else {
               status = "Auction won by: "+response.getName()+" <"+response.getEmail()+">";
            }
            return;
         } catch (Exception e) {
            System.out.println(e);
         }
      }
      status = "You do not own this auction.";
      System.out.println("Auction closing unsuccessful.");
   }
   
   /**
    **   Validates a bid is higher than the given auctions current price,
    **   and if so invokes the setting the a new bid and bidder.
    **   
    **   @param id The ID of the auction in question.
    **   @param bidder The user bidding on the auction.
    **   @param price The amount bid.
    **/
   public void bidOnAuction(int id, UserWrapper bidder, float price) throws java.rmi.RemoteException {
      AuctionWrapper auction;
      System.out.println("Placing bid on auction #"+id+"...");
      
      try {
         if ((auction = this.getAuction(id)) != null) {
            if (price > auction.getPrice()) {
               disp.callRemoteMethods(null, "setBid", new Object[]{auction, bidder, price}, new Class[]{AuctionWrapper.class, UserWrapper.class, float.class}, opts);
               System.out.println("Bid successful.");
               status = "Bid successful";
            } else {
               System.out.println("Bid unsuccessful.");
               status = "Price less than highest bid";
            }
         } else {
            System.out.println("Bid unsuccessful.");
            status = "Invalid auction ID";
         }
      } catch (Exception e) {
         System.out.println(e);
      }
   }
   
   /**
    **   Invokes the returning of a list of all the current auctions.
    **   
    **   @return An `ArrayList` of `AuctionWrapper`s.
    **/
   public ArrayList<AuctionWrapper> showAllAuctions() throws java.rmi.RemoteException {
      try {
         RspList rsp_list=disp.callRemoteMethods(null, "getAllAuctions", null, null, opts);
         return (ArrayList<AuctionWrapper>)rsp_list.getFirst();
      } catch (Exception e) {
         System.out.println(e);
      }
      return null;
   }   
     
   /**
    **   Accessor Method. Invokes the retrieval of a preexisting user, 
    **   if it exists.
    **   
    **   @param username The user's username.
    **   @return The relevant `UserWrapper`.
    **/
   public UserWrapper getUser(String username) throws java.rmi.RemoteException {
      try {
         RspList rsp_list=disp.callRemoteMethods(null, "getAllUsers", null, null, opts);
         for(UserWrapper user: (ArrayList<UserWrapper>)rsp_list.getFirst()) {
            if (user.getUsername().equals(username)) {
               status = "Welcome back, "+user.getName()+".";
               return user;
            }
         }
      } catch (Exception e) {
         System.out.println(e);
      }
      status = "No such user.";
      return null;
   }
   
   /**
    **   Tests that the username entered is not already taken, and
    **   invokes the creation of a new user with the given details if not.
    **   
    **   @param newUser The new user to validate and create.
    **   @return The newly-created user.
    **/
   public UserWrapper registerUser(UserWrapper newUser) throws java.rmi.RemoteException {
      try {
         RspList rsp_list=disp.callRemoteMethods(null, "getAllUsers", null, null, opts);

         if (!rsp_list.isEmpty()) {
            for(UserWrapper user: (ArrayList<UserWrapper>)rsp_list.getFirst()){
               if (user.getUsername().equals(newUser.getUsername())) {
                  status = "Username taken. Either choose a new username or, if trying to login to an existing account, rerun the program as 'AuctionClient <username>'.";
                  return null;
               }
            }
         }
         
         disp.callRemoteMethods(null, "createUser", new Object[]{newUser}, new Class[]{UserWrapper.class}, opts);
         status = "New user created. Hello "+newUser.getName()+".";
         return newUser;    
      } catch (Exception e) {
         System.out.println(e);
      }   
      status = "Something went wrong";
      return null;
   }
   
   /*
    *    Methods that are remotely invoked via RPC on both this and the
    *    `Replica`s due to multicasting.
    */
    
   /**
    **   Creates an auction with the given details.
    **   
    **   @param newAuction An `AuctionWrapper` of the new auction.
    **/
   private void createAuction(AuctionWrapper newAuction) throws java.rmi.RemoteException {
      int id = 0;
      
      // Gives the new auction the lowest unclaimed ID.
      for(AuctionWrapper auction: auctions){
         if (id <= auction.getID()) {
            id = auction.getID();
         }
      }
      
      newAuction.setID(++id);
      auctions.add(newAuction);
      status = "Auction no. "+id+" successfully created.";
   }
   
   /**
    **   Removes an auction. If the reserve price was met, also returns 
    **   the winning bidder, otherwise an indicative `UserWrapper` with
    **   username of "server".
    **   
    **   @param auction The `AuctionWrapper` of the auction to remove.
    **   @return The `UserWrapper` of the highest bidder, or `null`.
    **/
   private UserWrapper removeAuction(AuctionWrapper auction) throws java.rmi.RemoteException {
      auctions.remove(auction);
      
      if (auction.getPrice() >= auction.getReserve()) {
         return auction.getHighestBidder();
      } else {
         return new UserWrapper("err", "err", "server");
      }
   }
   
   /**
    **   Mutator Method. Sets the bid on an auction.
    **   
    **   @param auction The `AuctionWrapper` of the auction to bid on.
    **   @param user The user bidding on the auction.
    **   @param price The amount the user has bid.
    **/
   private void setBid(AuctionWrapper auction, UserWrapper user, float price) {
      this.getAuction(auction.getID()).setBid(user, price);
   }
   
   /**
    **   Accessor Method. Retrieves a list of all the auctions.
    **   
    **   @return An `ArrayList` of `AuctionWrapper`s.
    **/
   private ArrayList<AuctionWrapper> getAllAuctions() {
      return this.auctions;
   }
   
   /**
    **   Accessor Method. Retrieves an auction by its ID.
    **   
    **   @param id The ID of the auction to retrieve.
    **   @return The `AuctionWrapper` indicated.
    **/
   private AuctionWrapper getAuction(int id) {
      try {
         RspList rsp_list=disp.callRemoteMethods(null, "getAllAuctions", null, null, opts);
         for(AuctionWrapper auction: (ArrayList<AuctionWrapper>)rsp_list.getFirst()) {
            if (auction.getID() == id) {
               return auction;
            }
         }
      } catch (Exception e) {
         System.out.println(e);
      }
      return null;
   }
   
   /**
    **   Creates a new user.
    **
    **   @param newUser The new user to create.
    **/
   private void createUser(UserWrapper newUser) {
      System.out.println("doing it");
      this.users.add(newUser);
   }
  
   /**
    **   Gets the list of all users.
    **
    **   @return An `ArrayList` of users.
    **/
   private ArrayList<UserWrapper> getAllUsers() {
      return this.users;
   }
   
   /*
    *    Methods that are called by the `AuctionClient` program via RMI.
    */
    
   /**
    **   Accessor Method. Gets the status resulting from the last action
    **   attempted.
    **   
    **   @return The status message.
    **/
   public String getStatusofLast() {
      return "\n"+status+"\n";
   }
   
   /**
    **   Accessor Method. Gets the server's public key.
    **   
    **   @return The public key.
    **/
   public PublicKey getPublicKey() throws java.rmi.RemoteException {
      return this.readKey("server");
   }
   
   /**
    **   Retrieves a user's public key and writes it to a file.
    **   
    **   @param key The user's public key.
    **   @param usernam The user's username.
    **/
   public void sendPublicKey(PublicKey key, String username) throws java.rmi.RemoteException {
      this.writeKey(key, username);
   }
   
   /*
    *    Writes the server's private key to a file.
    *   
    *    @param The private key.
    */
   private static void writeKey(PrivateKey pKey) throws java.rmi.RemoteException {
      try {
         byte[] key = pKey.getEncoded();
         FileOutputStream keyfos = new FileOutputStream("../key/server/serverpriv.key");
         keyfos.write(key);
         keyfos.close();
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
   }
   
   /*
    *    Writes the server's public key to a file.
    *   
    *    @param The public key.
    */
   private static void writeKey(PublicKey pKey) throws java.rmi.RemoteException {
      try {
         byte[] key = pKey.getEncoded();
         FileOutputStream keyfos = new FileOutputStream("../key/server/serverpub.key");
         keyfos.write(key);
         keyfos.close();
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
   }
   
   /*
    *    Writes a user's public key to a file.
    *   
    *    @param The public key.
    */
   private static void writeKey(PublicKey pKey, String username) throws java.rmi.RemoteException {
      try {
         byte[] key = pKey.getEncoded();
         FileOutputStream keyfos = new FileOutputStream("../key/server/"+username+"pub.key");
         keyfos.write(key);
         keyfos.close();
      } catch (Exception e) {
         System.out.println();
         System.out.println("Exception");
         System.out.println(e);
      }
   }
   
   /*
    *    Reads the server's private key from a file.
    *   
    *    @return The private key.
    */
   private static PrivateKey readKey() throws java.rmi.RemoteException {
      try {
         FileInputStream keyfis = new FileInputStream("../key/server/serverpriv.key");
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
    *    Reads a user's public key from a file.
    *   
    *    @param username The user's username.
    *    @return The public key.
    */
   private static PublicKey readKey(String username) throws java.rmi.RemoteException {
      try {
         FileInputStream keyfis = new FileInputStream("../key/server/"+username+"pub.key");
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

   /**
    **   Sends back a user's challenge, signed with the server's private
    **   key.
    **   
    **   @param challenge The user's challenge.
    **   @return The signature.
    **/
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
   
   /**
    **   Sends a user's a challenge.
    **   
    **   @return The challenge.
    **/
   public byte[] getChallenge() throws java.rmi.RemoteException {
      challenge = new byte[1024];
      new Random().nextBytes(this.challenge);
      return this.challenge;
   }
   
   /**
    **   Takes a returned challenge from the user and verifies the
    **   signature.
    **   
    **   @param retChal The returned signature from the user.
    **   @param username The user's username.
    **   @return Whether the user has been verified or not.
    **/
   public boolean returnChallenge(byte[] retChal, String username) throws java.rmi.RemoteException {
      System.out.println("Authenticating user '"+username+"'...");
      System.out.println(users);
      try {
         RspList rsp_list=disp.callRemoteMethods(null, "getAllUsers", null, null, opts);
         for(UserWrapper user: (ArrayList<UserWrapper>)rsp_list.getFirst()) {
            if (user.getUsername().equals(username)) {
               Signature sig = Signature.getInstance("SHA1withDSA");
               sig.initVerify(this.readKey(username));

               sig.update(this.challenge);
               
               boolean verifies = sig.verify(retChal);
               System.out.println(username+" signature verifies: " + verifies);
               return verifies;
            }
         }
      } catch (Exception e) {
         System.out.println(e);
      }
      
      System.out.println("User '"+username+"' not found.");
      return false;
   }
   
   /**
    **   Creates a new replica of the server.
    **/
   public void replicate() throws java.rmi.RemoteException {
      try {
         new Replica().start();
         status = "Server replication successful.";
      } catch (Exception e) {
         System.out.println(e);
         status = "Server replication failed.";
      }
   }
   
   /**
    **   Closes the server and replicas.
    **/
   public void close() throws java.rmi.RemoteException {
      System.out.println("Stopping replicas");
      try {
         disp.callRemoteMethods(null, "stop", null, null, opts);
      } catch (Exception e) {
         System.out.println(e);
      }
      channel.close();
      disp.stop();
      System.exit(-1);
   }
}
