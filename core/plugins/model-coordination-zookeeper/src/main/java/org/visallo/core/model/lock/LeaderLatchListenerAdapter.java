package org.visallo.core.model.lock;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

public class LeaderLatchListenerAdapter implements LeaderLatchListener {
    private final LeaderListener leaderListener;

    public LeaderLatchListenerAdapter(LeaderListener leaderListener) {
        this.leaderListener = leaderListener;
    }

    @Override
    public void isLeader() {
        this.leaderListener.isLeader();
    }

    @Override
    public void notLeader() {
        this.leaderListener.notLeader();
    }
}
