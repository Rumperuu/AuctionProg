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
 ** This class represents the auction server.
 **/
 
import java.io.*;
import java.util.*;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 **   @author  Ben Goldsworthy (rumps) <bgoldsworthy96 @ gmail.com>
 **   @version 2.0
 **/
public interface Auction extends java.rmi.Remote {	
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
   public void openNewAuction(AuctionWrapper newAuction) throws java.rmi.RemoteException;

   /**
    **   Validates that the calling user owns the auction in question, and
    **   if invokes the closing of the auction, returning the winning
    **   bidder (if applicaable).
    **   
    **   @param id The ID of the auction to bid on.
    **   @param currentUser The user placing the bid.
    **   @return The highest bidder, or `null`.
    **/
   public void closeAuction(int id, UserWrapper currentUser) throws java.rmi.RemoteException;

   /**
    **   Validates a bid is higher than the given auctions current price,
    **   and if so invokes the setting the a new bid and bidder.
    **   
    **   @param id The ID of the auction in question.
    **   @param bidder The user bidding on the auction.
    **   @param price The amount bid.
    **/
   public void bidOnAuction(int id, UserWrapper bidder, float price) throws java.rmi.RemoteException;
   
   /**
    **   Invokes the returning of a list of all the current auctions.
    **   
    **   @return An `ArrayList` of `AuctionWrapper`s.
    **/
   public ArrayList<AuctionWrapper> showAllAuctions() throws java.rmi.RemoteException;
    
   /**
    **   Accessor Method. Invokes the retrieval of a preexisting user, 
    **   if it exists.
    **   
    **   @param username The user's username.
    **   @return The relevant `UserWrapper`.
    **/
   public UserWrapper getUser(String username) throws java.rmi.RemoteException;   
   
   /**
    **   Tests that the username entered is not already taken, and
    **   invokes the creation of a new user with the given details if not.
    **   
    **   @param newUser The new user to validate and create.
    **   @return The newly-created user.
    **/
   public UserWrapper registerUser(UserWrapper newUser) throws java.rmi.RemoteException;
   
   /*
    *    Methods that are called by the `AuctionClient` program via RMI.
    */
    
    /**
    **   Accessor Method. Gets the status resulting from the last action
    **   attempted.
    **   
    **   @return The status message.
    **/
   public String getStatusofLast() throws java.rmi.RemoteException;
   
   /**
    **   Accessor Method. Gets the server's public key.
    **   
    **   @return The public key.
    **/
   public PublicKey getPublicKey() throws java.rmi.RemoteException;
   
   /**
    **   Retrieves a user's public key and writes it to a file.
    **   
    **   @param key The user's public key.
    **   @param usernam The user's username.
    **/
   public void sendPublicKey(PublicKey key, String username) throws java.rmi.RemoteException;
            
   /**
    **   Sends back a user's challenge, signed with the server's private
    **   key.
    **   
    **   @param challenge The user's challenge.
    **   @return The signature.
    **/
   public byte[] challengeServer(byte[] challenge) throws java.rmi.RemoteException;
   
   /**
    **   Sends a user's a challenge.
    **   
    **   @return The challenge.
    **/
   public byte[] getChallenge() throws java.rmi.RemoteException;
   
   /**
    **   Takes a returned challenge from the user and verifies the
    **   signature.
    **   
    **   @param retChal The returned signature from the user.
    **   @param username The user's username.
    **   @return Whether the user has been verified or not.
    **/
   public boolean returnChallenge(byte[] retChal, String username) throws java.rmi.RemoteException;
   
   /**
    **   Creates a new replica of the server.
    **/
   public void replicate() throws java.rmi.RemoteException;
   
   /**
    **   Closes the server and replicas.
    **/
   public void close() throws java.rmi.RemoteException;
}

