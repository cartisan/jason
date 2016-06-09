{namespace(priv,local)} // Forward definition of private namespace (identified by 'priv')

//prefix :: forces a term to be considered in the abstract namespace

priv::all_proposals_received
    :-  .count(::introduction(participant)[source(_)], NP) &
        .count(::propose(_)[source(_)], NO) &
        .count(::refuse[source(_)], NR) &
        NP = NO + NR. // participants = proposals + refusals
        
// starts a CNP
@p1 
+!startCNP(Task)
    <-  .broadcast(tell, ::cnp_started); // tell everyone a CNP has started
        // this_ns is a reference to the namespace where this module is loaded
        // in this example is the namespace where the CNP is being performed
        .print("Waiting participants for task ",Task," in ",this_ns ," ... ");
        .wait(3000); // wait participants introduction
        +priv::state(propose); // remember the state of the CNP
        .findall(A,::introduction(participant)[source(A)],LP);
        .print("Sending CFP for ",Task," to ",LP);
        .send(LP,tell,::cfp(Task)); // send call for proposals to participants
        // wait until all proposals are received for a maximum of 5 secs
        .wait(priv::all_proposals_received, 5000, _);
        //.findall(offer(Price,A), ::propose(Price)[source(A)],LL); .print(LL);
        !priv::contract(this_ns).

// to let the agent to query the current state of the CNP
@p2 +?cnp_state(S) <- ?priv::state(S).
@p3 +?cnp_state(none).

{begin namespace(priv)}
    // .intend (g) is true if the agent is currently intending !g
    +!contract(Ns) : state(propose) & not .intend(::contract(_))
        <-  -+state(contract); // updates the state of CNP
            .findall(offer(Price,A), Ns::propose(Price)[source(A)], L);
            .print("Offers in CNP taking place in ", Ns," are ",L);
            L \== []; // constraint the plan execution to at least one offer
            .min(L,offer(WOf,WAg)); // sort offers , the first is the best
            +Ns::winner(WAg);
            !announce_result(Ns,L);
            -+state(finished).

    // nothing todo, the current phase is not propose
    +!contract(_).
    -!contract(Ns)
        <- .print("CNP taking place in ",Ns," has failed! No proposals");
           -+state(finished).

    +!announce_result(_,[]).
    // announce to the winner
    +!announce_result (Ns,[offer(_,Ag)|T]) : Ns::winner(Ag)
       <- .send(Ag,tell,Ns::accept_proposal ); // notify the winner
    !announce_result(Ns,T).
    // announce to others
    +!announce_result(Ns,[offer(_,Ag)|T])
       <- .send(Ag,tell,Ns::reject_proposal );
          !announce_result(Ns,T).
{end}
