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
import java.util.Iterator;
import java.util.Map;

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

            addBehaviour(new TickerBehaviour(this, 30000) {
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
        private Map<AID, Integer> expectedProposals = new HashMap<>();

        private MessageTemplate mt;
        private AID highestBidder = null;
        private int highestBid = 0;

        @Override
        public void action() {
//            System.out.println("Step: " + step);
            switch (step) {
                case 0:

                    // Send the item being sold and the starting bidding price

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
                    ACLMessage reply = myAgent.receive(mt);

                    if (reply != null) {

                        switch (reply.getPerformative()) {
                            case ACLMessage.PROPOSE:
                                // This is a bid
                                expectedProposals.put(reply.getSender(), Integer.parseInt(reply.getContent()));

                                System.out.println(reply.getSender().getName() + " bids " + reply.getContent());

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

                    // Send an CFP to the agents with lower bids

                    Iterator<Map.Entry<AID, Integer>> iter = expectedProposals.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry<AID, Integer> item = iter.next();
                        if (highestBid < item.getValue()) {
                            highestBidder = item.getKey();
                            highestBid = item.getValue();
                        }
                    }

                    if (highestBidder != null) {
                        System.out.println(highestBid + " for " + highestBidder.getName());
                    } else {
                        System.out.println("Only received invalid bids!");
                    }

                    // Send accept proposal to the highest bidder

                    ACLMessage accept = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    accept.addReceiver(highestBidder);
                    accept.setContent(itemName + "||" + highestBid);
                    accept.setConversationId("auction");
                    accept.setReplyWith("bid-ok" + System.currentTimeMillis());

                    myAgent.send(accept);

                    // Reject the rest of the proposals

                    for (AID aid : expectedProposals.keySet()) {
                        if (aid != highestBidder) {
                            ACLMessage reject = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
                            reject.addReceiver(highestBidder);
                            reject.setContent(itemName + "||" + expectedProposals.get(aid));
                            reject.setConversationId("auction");
                            reject.setReplyWith("bid-reject" + System.currentTimeMillis());

                            myAgent.send(reject);
                        }
                    }

                    step = 3;
                    break;
                case 3:

                    System.out.println("Do I hear " + (int) highestBid * 1.5 + "??");

                    step = 4;
                    break;
            }
        }

        @Override
        public boolean done() {
            return (step == 4);
        }
    }
}
