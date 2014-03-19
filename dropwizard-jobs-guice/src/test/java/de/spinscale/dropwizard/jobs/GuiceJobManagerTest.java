package de.spinscale.dropwizard.jobs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public class GuiceJobManagerTest {

    JobManager jobManager;

    @Before
    public void setUp() throws Exception {
    	Injector injector = Guice.createInjector();
		jobManager = new GuiceJobManager(injector);
    }
    
    @After
    public void tearDown() throws Exception {
    	jobManager = null;
    }

	@Test
	public void jobsOnStartupShouldBeExecuted() throws Exception {
		ApplicationStartTestJob.results.clear();
		jobManager.start();
		Thread.sleep(1000);
		assertThat(ApplicationStartTestJob.results, hasSize(1));
	}

	@Test
	public void jobsOnStoppingShouldBeExecuted() throws Exception {
		ApplicationStopTestJob.results.clear();
		jobManager.start();
		jobManager.stop();
		assertThat(ApplicationStopTestJob.results, hasSize(1));
	}

	@Test
	public void jobsWithOnAnnotationShouldBeExecuted() throws Exception {
		OnTestJob.results.clear();
		jobManager.start();
		Thread.sleep(5000);
		assertThat(OnTestJob.results, hasSize(greaterThan(5)));
	}

	@Test
	public void jobsWithEveryAnnotationShouldBeExecuted() throws Exception {
		EveryTestJob.results.clear();
		jobManager.start();
		Thread.sleep(5000);
		assertThat(EveryTestJob.results, hasSize(greaterThan(5)));
	}
}
