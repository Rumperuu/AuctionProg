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
 ** This class represents a User.
 **/

import java.io.*;

/**
 **   @author  Ben Goldsworthy (rumps) <me+auctionprog@bengoldsworthy.net>
 **   @version 1.0
 **/
public class UserWrapper implements Serializable {
   private String name;
   private String email;
   private String username;

   /**   
    **   Constructor Method.
    **   @param id The ID of the user.
    **   @param name The user's name.
    **   @param email The user's email address.
    **/
	public UserWrapper(String name, String email, String username){
		this.name = name;
      this.email = email;
      this.username = username;
	}
   
   /**   
    **   Accessor Method. Returns the user's name.
    **   @param The user's name.
    **/
	public String getName() {
		return this.name;
	}
   
	public String getUsername() {
		return this.username;
	}
   
   /**   
    **   Accessor Method. Returns the user's email address.
    **   @param The user's email address.
    **/
   public String getEmail() {
		return this.email;
	}
}
