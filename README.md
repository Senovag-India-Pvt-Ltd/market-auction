# market-auction

Swagger link:
http://localhost:8002/market-auction/swagger-ui.html



TODO:
1. Logs both errors and warings to be added at criticle places
   a. issue bidding slip when we cannot issue due to various time constraint or not flex time/.
   b. When no bins are there in market, error out as bin counter master will error out. Todo code for invalid market id.
   c. Aspect for adding around the function bin, lot market auction for the farmer id, and time taken.
   d. Add error message reason for Market auction.
   c. If some bins are missed then we should have provision at the end to allocate the bins which got missed in the series.
   d. Bin statistics api needs to be built.
2. Specific validation message and error codes to be defined and shared with UI team.
3. Reeler auction validation message, pagination for highest bid  
4. Weighment API needs to be built. 
5. statuses for all the applicable tables 
6. DB Field varchars number of character validation 

