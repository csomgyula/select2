package select2.bench;

import java.util.Date;

/**
 * A very simple benchmark. Features:
 *
 * (1) uses a simple (empty) closure
 * (2) supports simple warmup
 * (3) support one test cycle, where the round could be configured
 * (4) measure run time
 *
 * TODO: implement a deterministic test variant, where mt scenarios could be given.
 */
public class SimpleBenchSync2{
	public void execute(int rounds){
		int warmup = 100;
		//executeBuiltinSync2Test(warmup);
		//executeSync2Test(warmup);
		
		for (int i = 0; i< 10; i++){
			// test sync2
			stopWatch.push();
			executeSync2Test(rounds);
			stopWatch.push();
			System.out.println("sync2:\t\t" + rounds + " rounds in " + stopWatch.time() + " msec");
			
			// test buitin sync2
			stopWatch.push();
			executeBuiltinSync2Test(rounds);
			stopWatch.push();
			System.out.println("builtin sync2:\t" + rounds + " rounds in " + stopWatch.time() + " msec\n");
		}
	}
	
	protected void executeSync2Test(int rounds){
		TestExecutorThread[] threads = new TestExecutorThread[2];
		
		for (int i = 0; i<2 ;i++){
			threads[i] = new TestExecutorThread();
		}	
		
		Sync2 sync2 = new Sync2(threads);
		
		for (int i = 0; i<2 ;i++){
			threads[i].setExecutor(sync2);
			threads[i].setRounds(rounds);
			threads[i].start();
		}
		
		for (int i = 0; i < 2; i++) {
			try {
			   threads[i].join();
			} 
			catch (InterruptedException ignore) {}
		}
	}
	
	protected void executeBuiltinSync2Test(int rounds){
		TestExecutorThread[] threads = new TestExecutorThread[2];
		
		for (int i = 0; i<2 ;i++){
			threads[i] = new TestExecutorThread();
		}	
		
		BuiltinSync2 sync2 = new BuiltinSync2();
		
		for (int i = 0; i<2 ;i++){
			threads[i].setExecutor(sync2);
			threads[i].setRounds(rounds);
			threads[i].start();
		}
		
		for (int i = 0; i < 2; i++) {
			try {
			   threads[i].join();
			} 
			catch (InterruptedException ignore) {}
		}
	}
	
	/**
	 * Runs the given test.
	 */
	public static void main(String args[]){
		SimpleBenchSync2 bench = new SimpleBenchSync2();
		int rounds = args.length == 0 ? 100 : Integer.parseInt(args[0]);
		bench.execute(rounds);
	}
	
	/**
	 * A simple class for testing purposes.
	 */	
	private class TestExecutorThread extends Thread{
		private Executor executor;
		public void setExecutor(Executor executor){
			this.executor = executor;
		}
		
		private int rounds;
		public void setRounds(int rounds){
			this.rounds = rounds;
		}
		
		public void run(){
			long id = Thread.currentThread().getId();
			for(int i=0; i<rounds; i++){
				executor.execute(TEST);
			}
			Thread.yield();
		}			
	}
	
	private static final TestClosure TEST = new TestClosure();
	private static class TestClosure implements Closure{
		public boolean execute(){
			return true;
		}
	}

	private StopWatch stopWatch = new StopWatch();
	private static class StopWatch{
		private boolean started;
		private long start, stop;
		
		public void push(){
			if (!started){
				start = (new Date()).getTime();
				started = true;
			}
			else{
				stop = (new Date()).getTime();
				started = false;
			}
		}
		
		public long time(){
			if (!started){
				return stop - start;
			}
			else{
				return (new Date()).getTime() - start;
			}
		}
	}
}