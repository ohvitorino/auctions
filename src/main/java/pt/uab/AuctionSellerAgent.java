package pt.uab;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

/**
 * Created by Bruno Vitorino on 03/07/16.
 */
public class AuctionSellerAgent extends Agent {
    private AID[] bidderAgents;

    private String itemName;
    private Integer itemPrice;

    @Override
    protected void setup() {
        System.out.println("Auction Seller Agent ready for service!");

        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            itemName = (String) args[0];
            itemPrice = Integer.parseInt((String) args[1]);

            // Register the auction-seller service in the yellow pages
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("auction-seller");
            sd.setName("MultiAgentSystem-auctions");
            dfd.addServices(sd);

            try {
                DFService.register(this, dfd);
            } catch (FIPAException e) {
                e.printStackTrace();
            }

            System.out.println("This time I'm selling the item \"" + itemName + "\" with the starting price of " + itemPrice);
            System.out.println("Please make your bids!");


            // Add behaviour to get the buyers bids

            addBehaviour(new TickerBehaviour(this, 3000) {
                @Override
                protected void onTick() {
                    
                }
            });
        } else {
            System.out.println("No item to be auctioned this time");
            doDelete();
        }
    }
}
