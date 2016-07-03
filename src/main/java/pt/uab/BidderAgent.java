package pt.uab;

import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Bruno Vitorino on 03/07/16.
 */
public class BidderAgent extends Agent {
    private int wallet;

    @Override
    protected void setup() {

        setRandomWallet();

        addBehaviour(new BidRequestsServer());

        // Register the auction-seller service in the yellow pages
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("auction-bidder");
        sd.setName("MultiAgentSystem-auctions");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
        System.out.println(getAID().getName() + " ready to buy some stuff. My wallet is " + wallet);
    }

    private void setRandomWallet() {
        int min = 10000;
        int max = Integer.MAX_VALUE;

        wallet = ThreadLocalRandom.current().nextInt(min, max);
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        System.out.println("Bidder " + getAID().getName() + " terminating");
    }

    private class BidRequestsServer extends Behaviour {
        private String itemName;
        private Integer itemPrice;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive();

            if (msg != null) {
                parseContent(msg.getContent());

            } else {
                block();
            }
        }

        private void parseContent(String content) {
            String[] split = content.split("\\|\\|");

            itemName = split[0];
            itemPrice = Integer.parseInt(split[1]);
        }

        @Override
        public boolean done() {
            return false;
        }
    }
}
