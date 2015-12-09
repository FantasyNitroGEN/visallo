package org.visallo.core.model.lock;

public interface LeaderListener {
    void isLeader();

    void notLeader();
}
