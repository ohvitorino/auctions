package pt.uab;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashMap;

/**
 * Created by Bruno Vitorino on 03/07/16.
 */
public class AuctionerAgent extends Agent {
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

            System.out.println("This time I'm selling the item \"" + itemName + "\" with the starting price of " + itemPrice);
            System.out.println("Please make your bids!");


            // Add behaviour to get the buyers bids

            addBehaviour(new TickerBehaviour(this, 3000) {
                @Override
                protected void onTick() {
                    // Get all the BidderAgents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("auction-bidder");
                    template.addServices(sd);

                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);

                        bidderAgents = new AID[result.length];
                        for (int i = 0; i < result.length; i++) {
                            System.out.println("Found seller: " + result[i].getName());

                            bidderAgents[i] = result[i].getName();
                        }
                    } catch (FIPAException e) {
                        e.printStackTrace();
                    }

                    myAgent.addBehaviour(new AuctionPerformer());
                }
            });
        } else {
            System.out.println("No item to be auctioned this time");
            doDelete();
        }
    }

    private class AuctionPerformer extends Behaviour {
        private int step = 0;
        private HashMap<AID, Integer> expectedProposals = new HashMap();

        private MessageTemplate mt;

        public void action() {
            switch (step) {
                case 0:
                    // Send the item being sold and the start bidding price

                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                    for (int i = 0; i < bidderAgents.length; i++) {
                        cfp.addReceiver(bidderAgents[i]);
                    }

                    cfp.setContent(itemName + "||" + itemPrice);
                    cfp.setConversationId("auction");
                    cfp.setReplyWith("cfp" + System.currentTimeMillis());

                    myAgent.send(cfp);

                    // Prepare the message template to deal with the bidding proposals
                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("auction"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));

                    step = 1;
                    break;
                case 1:
                    ACLMessage reply = myAgent.receive();

                    if (reply != null) {

                        switch (reply.getPerformative()) {
                            case ACLMessage.PROPOSE:
                                // This is a bid
                                expectedProposals.put(reply.getSender(), Integer.parseInt(reply.getContent()));
                                break;
                            case ACLMessage.REFUSE:
                                // The agent is not interested in the item
                                expectedProposals.put(reply.getSender(), null);
                                break;
                        }

                        if (expectedProposals.size() == bidderAgents.length) {
                            step = 2;
                        }

                    } else {
                        block();
                    }
                    break;
                case 2:
            }
        }

        public boolean done() {
            return false;
        }
    }
}
