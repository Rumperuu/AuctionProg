set /p id="Username (leave blank for new user): "
java -Djava.net.preferIPv4Stack=true AuctionClient %id%
pause