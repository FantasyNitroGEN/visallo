package org.visallo.core.model;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.visallo.core.config.Configuration;
import org.visallo.core.exception.VisalloException;
import org.visallo.core.ingest.WorkerSpout;
import org.visallo.core.model.workQueue.WorkQueueRepository;
import org.visallo.core.status.StatusServer;
import org.visallo.core.util.VisalloLogger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkerBaseTest {
    private boolean stopOnNextTupleException;
    private int nextTupleExceptionCount;

    @Mock
    private WorkQueueRepository workQueueRepository;
    @Mock
    private Configuration configuration;
    @Mock
    private WorkerSpout workerSpout;

    @Before
    public void before() {
        nextTupleExceptionCount = 0;
    }

    @Test
    public void testExitOnNextTupleFailure_exitOnNextTupleFailure_true() throws Exception {
        stopOnNextTupleException = false;
        when(configuration.getBoolean(eq(TestWorker.class.getName() + ".exitOnNextTupleFailure"), anyBoolean())).thenReturn(true);
        when(configuration.getBoolean(eq(Configuration.STATUS_ENABLED), anyBoolean())).thenReturn(false);
        when(workQueueRepository.createWorkerSpout(eq("test"))).thenReturn(workerSpout);
        when(workerSpout.nextTuple()).thenThrow(new VisalloException("could not get nextTuple"));

        TestWorker testWorker = new TestWorker(workQueueRepository, configuration);
        try {
            testWorker.run();
            fail("should throw");
        } catch (VisalloException ex) {
            assertEquals(1, nextTupleExceptionCount);
        }
    }

    @Test
    public void testExitOnNextTupleFailure_exitOnNextTupleFailure_false() throws Exception {
        stopOnNextTupleException = true;
        when(configuration.getBoolean(eq(TestWorker.class.getName() + ".exitOnNextTupleFailure"), anyBoolean())).thenReturn(false);
        when(configuration.getBoolean(eq(Configuration.STATUS_ENABLED), anyBoolean())).thenReturn(false);
        when(workQueueRepository.createWorkerSpout(eq("test"))).thenReturn(workerSpout);
        when(workerSpout.nextTuple()).thenThrow(new VisalloException("could not get nextTuple"));

        TestWorker testWorker = new TestWorker(workQueueRepository, configuration);
        testWorker.run();
        assertEquals(1, nextTupleExceptionCount);
    }

    private class TestWorker extends WorkerBase {
        protected TestWorker(WorkQueueRepository workQueueRepository, Configuration configuration) {
            super(workQueueRepository, configuration);
        }

        @Override
        protected StatusServer createStatusServer() throws Exception {
            throw new VisalloException("not implemented");
        }

        @Override
        protected void process(Object messageId, JSONObject json) throws Exception {
            stop();
        }

        @Override
        protected String getQueueName() {
            return "test";
        }

        @Override
        protected void handleNextTupleException(VisalloLogger logger, Exception ex) throws InterruptedException {
            nextTupleExceptionCount++;
            if (stopOnNextTupleException) {
                stop();
                return;
            }
            super.handleNextTupleException(logger, ex);
        }
    }
}
