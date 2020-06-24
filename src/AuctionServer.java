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
 ** This class sets up the auction server using RMI.
 **/

import java.rmi.Naming;

/**
 **   @author  Ben Goldsworthy (rumps) <me+auctionprog@bengoldsworthy.net>
 **   @version 1.0
 **/
public class AuctionServer {
   /**   
    **   Constructor Method.
    **/
   public AuctionServer() {
      try {
       	Auction a = new AuctionImpl();
       	Naming.rebind("rmi://localhost/AuctionService", a);
      } catch (Exception e) {
         System.out.println("Server Error: " + e);
      }
   }

   /**   
    **   Runs the server.
    **
    **   @param args Command-line arguments.
    **/
   public static void main(String args[]) {
      new AuctionServer();
   }
}
