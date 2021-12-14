Project 5, Distributed Auction
CS 351L 004

Brian Kimbrell
Laxman Adhikari
Shaswat Shukla

Working of Program:

First, All jar files are transferred to virtual machine. We transferred the files
using NoMachine interface which was just drag and drop. After all our jar is
in the virtual machine, we need to go to directory where all our jar files are
sitted. The first jar file to be run is BankServer.jar as it is going to act as
a server for both the agents and auction house. It will connect clients, give them
an id and start a new thread to communicate.

After that we can log in to another virtual machine and run either AuctionHouse.jar or
AgentClient.jar . The order doesn't matter between then two. Whenever we run either
of them, they send account initialize request to Bank. The clients get connected to
the bank, and they can now communicate with each other.

Basically, we as a user will only interact in agents program except of close request
on auction house. We are given options to interact with Bank in the beginning. The options
include A: Connect Auction House B: Get the Balance information and Q: Quit the program.
Connect Auction House will give agents list of auction houses that are available for
interaction. Agents are shown another options for interaction with Auction House. These
options are R: Request list of items listed on Auction, B: Bid Command, X: Close interaction
temporarily with Auction House. Based on user input, Agent class sends request to Bank and
Auction House and read their input to perform bidding, paying off bought items,etc. All
items are tracked if they are on bidding and if they are available to bid or not.
The program is designed to accept multiple Auction Houses and multiple Agents.

For the auction house, "Q" will close the auction house if there are not any item on
auction house on active bidding.

Auction House will have three items available for Bidding at any point of time until
there is possibility. If one item is sold on auction, another item will replace. Whenever,
the item is sold a update item request will update the amount available to bid.

Bank keeps running until user closes it.
Bid time is set to be 30 seconds.
