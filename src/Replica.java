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
 ** This class represents a User.
 **/

import java.io.*;
import java.util.*;
import org.jgroups.JChannel;
import org.jgroups.blocks.RpcDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.util.*;

/**
 **   @author  Ben Goldsworthy (rumps) <me+auctionprog@bengoldsworthy.net>
 **   @version 2.0
 **/
public class Replica {
   JChannel channel;
   RpcDispatcher disp;
   RequestOptions opts=new RequestOptions(ResponseMode.GET_ALL, 5000);
   
   private ArrayList<AuctionWrapper> auctions;
   private ArrayList<UserWrapper> users;
   
   /**   
    **   Connects to the channel and sets itself up.
    **/
	public void start() {
      try {
         channel = new JChannel();
         disp = new RpcDispatcher(channel, this);
         channel.connect("AuctionProg");
         
         auctions = new ArrayList<AuctionWrapper>();
         users = new ArrayList<UserWrapper>();
      } catch (Exception e) {
         System.out.println(e);
      }
	}
   
   /**   
    **   Shuts down the replica.
    **/
   public void stop() {
      channel.close();
      disp.stop();
   }
   
   /**   
    **   Creates a new auction.
    **
    **   @param newAuction The new auction to create.
    **/
   public void createAuction(AuctionWrapper newAuction) {
      int id = 0;
      
      // Gives the new auction the lowest unclaimed ID.
      for(AuctionWrapper auction: auctions){
         if (id <= auction.getID()) {
            id = auction.getID();
         }
      }
      
      newAuction.setID(++id);
      auctions.add(newAuction);
   }
   
   /**   
    **   Removes an auction from the list.
    **
    **   @param auction The auction to remove.
    **   @return The highest-bidding user (if applicable).
    **/
   public UserWrapper removeAuction(AuctionWrapper auction) {
      auctions.remove(auction);
      
      if (auction.getPrice() >= auction.getReserve()) {
         return auction.getHighestBidder();
      } else {
         return new UserWrapper("err", "err", "server");
      }
   }
   
   /**   
    **   Mutator Method. Sets a new bid on an auction.
    **  
    **   @param auction The auction to bid on.
    **   @param user The user bidding.
    **   @param price The user's bid.
    **/
   public void setBid(AuctionWrapper auction, UserWrapper user, float price) {
      this.getAuction(auction.getID()).setBid(user, price);
   }
   
   /**   
    **   Accessor Method. Gets all the auctions.
    **
    **   @return The list of auctions.
    **/
   public ArrayList<AuctionWrapper> getAllAuctions() {
      return this.auctions;
   }
   
   /**   
    **   Gets an auction by ID.
    **
    **   @param id The auction ID.
    **   @return The auction requested, or `null`.
    **/
   private AuctionWrapper getAuction(int id) {
      for(AuctionWrapper auction: auctions) {
         if (auction.getID() == id) {
            return auction;
         }
      }
      return null;
   }
   
   /**   
    **   Accessor Method. Gets the list of all users.
    **
    **   @return The list of users.
    **/
   public ArrayList<UserWrapper> getAllUsers() {
      return this.users;
   }
   
   /**    
    **   Creates a new user.
    **
    **   @param The new user to create.
    **/
   public void createUser(UserWrapper newUser) {
      this.users.add(newUser);
   }   
}
