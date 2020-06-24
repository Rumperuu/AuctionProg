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
 ** This class represents an Auction.
 **/
 
import java.io.*;

/**
 **   @author  Ben Goldsworthy (rumps) <me+auctionprog@bengoldsworthy.net>
 **   @version 1.0
 **/
public class AuctionWrapper implements Serializable {
   private int id;
   private String desc;
   private UserWrapper owner;
   private UserWrapper highestBidder;
   private float price;
   private float reserve;

   /**   
    **   Constructor Method.
    **   @param id The ID of the auction.
    **   @param desc The description of the auction.
    **   @param owner The user creating the auction.
    **   @param startingPrice The starting price of the auction.
    **   @param reserve The reserve price of the auction.
    **/
	public AuctionWrapper(int id, String desc, UserWrapper owner, float startingPrice, float reserve){
		this.id = id;
		this.desc = desc;
		this.owner = owner;
      this.highestBidder = null;
      this.price = startingPrice;
      this.reserve = reserve;
	}
   
   /**   
    **   Returns whether the auction has been sold on close.
    **   @return Whether the auction has been sold or not.
    **/
   public boolean isSold() {
      return (this.price >= this.reserve) ? true : false;
   }
   
   /**   
    **   Mutator Method. Sets the current highest bid.
    **   @param bidder The user bidding.
    **   @param price The bid price.
    **/
   public void setBid(UserWrapper bidder, float price) {
      this.highestBidder = bidder;
      this.price = price;
   }
   
   /**   
    **   Accessor Method. Gets the auction ID.
    **   @return The auction ID.
    **/
	public int getID() {
		return this.id;
	}
   
   /**   
    **   Accessor Method. Gets the auction description.
    **   @return The auction description.
    **/
   public String getDesc() {
		return this.desc;
	}
   
   /**   
    **   Accessor Method. Gets the auction owner.
    **   @return The auction owner.
    **/
   public UserWrapper getOwner() {
		return this.owner;
	}
   
   /**   
    **   Accessor Method. Gets the highest bidder on the auction.
    **   @return The highest bidder on the auction.
    **/
   public UserWrapper getHighestBidder() {
		return this.highestBidder;
	}
   
   /**   
    **   Accessor Method. Gets the current auction price.
    **   @return The current auction price.
    **/
   public float getPrice() {
		return this.price;
	}
   
   /**   
    **   Accessor Method. Gets the auction reserve price.
    **   @return The auction reserve price.
    **/
   public float getReserve() {
		return this.reserve;
	}
}
