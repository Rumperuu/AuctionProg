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
 ** This class represents the auction server.
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
public interface Auction extends java.rmi.Remote {	
   /**
    **   Creates an auction with the given details, generating a new ID for 
    **   it.
    **   
    **   @param desc The auction description.
    **   @param owner The auction owner/creator.
    **   @param startingPrice The auction starting price.
    **   @param reserve The auction reserve price.
    **/
   public void createAuction(String desc, UserWrapper owner, float startingPrice, float reserve) throws java.rmi.RemoteException;

   /**
    **   Removes an auction, provided the user calling the method owns it.
    **   If the reserve price was met, also returns the winning bidder.
    **   
    **   @param id The ID of the auction to remove.
    **   @param currentUser The user calling the method.
    **   @return The `UserWrapper` of the highest bidder, if applicable.
    **/
   public UserWrapper removeAuction(int id, UserWrapper currentUser) throws java.rmi.RemoteException;

   /**
    **   Bids on a given auction, provided the bid is more than the
    **   current highest price.
    **   
    **   @param id The ID of the auction in question.
    **   @param bidder The user bidding on the auction.
    **   @param price The amount bid.
    **/
   public void bidOnAuction(int id, UserWrapper bidder, float price) throws java.rmi.RemoteException;
   
   /**
    **   Accessor Method. Retrieves a list of all the auctions.
    **   
    **   @return An `ArrayList` of `AuctionWrapper`s.
    **/
   public ArrayList<AuctionWrapper> getAuctions() throws java.rmi.RemoteException;
    
   /**
    **   Accessor Method. Gets a user, or creates a new one if non 
    **   exists with the given details.
    **   
    **   @param name The user's name.
    **   @param email The user's email address.
    **   @return The relevant `UserWrapper`.
    **/
   public UserWrapper getUser(String username) throws java.rmi.RemoteException;   
   
   public UserWrapper registerUser(String name, String email, String username) throws java.rmi.RemoteException;
   
   public void close() throws java.rmi.RemoteException;
   
   public PublicKey getPublicKey() throws java.rmi.RemoteException;
   public void sendPublicKey(PublicKey key, String username) throws java.rmi.RemoteException;
            
   /**
    **   Accessor Method. Gets the status resulting from the last action
    **   attempted.
    **   
    **   @return The status message.
    **/
   public String getStatusofLast() throws java.rmi.RemoteException;
   
   public byte[] challengeServer(byte[] challenge) throws java.rmi.RemoteException;
   
   public byte[] getChallenge() throws java.rmi.RemoteException;
   public boolean returnChallenge(byte[] retChal, String username) throws java.rmi.RemoteException;
}

