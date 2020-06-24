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
 ** This class sets up the auction server using RMI.
 **/

import java.rmi.Naming;
import java.util.*;

/**
 **   @author  Ben Goldsworthy (rumps) <me+auctionprog@bengoldsworthy.net>
 **   @version 2.0
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
      displayIntro();
      
      new AuctionServer();
   }
   
   /*
    *    Displays the intro preamble to the user.
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
}
