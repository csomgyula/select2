package select2.bench;

import select2.Cas2;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

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
public class SimpleBenchCas2 {
	public void execute(int rounds){
		int warmup = 100000;
		// executeAtomicIntTest(warmup);
		// executeMaySet2Test(warmup);
		
		for (int i = 0; i< 10; i++){
			// test Select2
			stopWatch.push();
			executeCas2Test(rounds);
			stopWatch.push();
			System.out.println("Cas2:\t\t" + rounds + " rounds in " + stopWatch.time() + " msec");
			
			// test CasSelect2
			stopWatch.push();
			executeAtomicIntTest(rounds);
			stopWatch.push();
			System.out.println("AtomicInt:\t" + rounds + " rounds in " + stopWatch.time() + " msec");

            System.out.println();
		}
	}
	
	protected void executeCas2Test(int rounds){
		Cas2Thread[] threads = new Cas2Thread[2];
		
		for (int i = 0; i<2 ;i++){
			threads[i] = new Cas2Thread();
		}	
		
		Cas2 cas2 = new Cas2(threads);
		
		for (int i = 0; i<2 ;i++){
			threads[i].setCas2(cas2);
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
	
	protected void executeAtomicIntTest(int rounds){
        AtomicIntThread[] threads = new AtomicIntThread[2];
		
		for (int i = 0; i<2 ;i++){
			threads[i] = new AtomicIntThread();
		}	
		
		AtomicInteger atomicInt = new AtomicInteger();
		
		for (int i = 0; i<2 ;i++){
			threads[i].setAtomicInt(atomicInt);
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
		SimpleBenchCas2 bench = new SimpleBenchCas2();

        int rounds = args.length == 0 ? 10000 : Integer.parseInt(args[0]);
		bench.execute(rounds);
	}
	
	/**
	 * A simple atomicInt thread for testing purposes.
	 */	
	private class Cas2Thread extends Thread{

        public Cas2Thread(){
            random = new Random();
        }
		private Cas2 cas2;
		public void setCas2(Cas2 cas2){
			this.cas2 = cas2;
		}
		
		private int rounds;
		public void setRounds(int rounds){
			this.rounds = rounds;
		}

        private Random random;

		public void run(){
			long id = Thread.currentThread().getId();
			int expected, update;
            for(int i=0; i<rounds; i++){
				// System.out.println( "DEBUG-" + id + ":\texecuting..." );
                expected = cas2.get();
                update = random.nextInt();
				boolean result = cas2.compareAndSet(expected, update);
				// System.out.println( "DEBUG-" + id + ":\texecuted? " + result);
                Thread.yield();
			}
		}			
	}

    /**
     * A simple atomic int for testing purposes.
     */
    private class AtomicIntThread extends Thread{

        public AtomicIntThread(){
            random = new Random();
        }
        private AtomicInteger atomicInt;
        public void setAtomicInt(AtomicInteger atomicInt){
            this.atomicInt = atomicInt;
        }

        private int rounds;
        public void setRounds(int rounds){
            this.rounds = rounds;
        }

        private Random random;

        public void run(){
            long id = Thread.currentThread().getId();
            int expected, update;
            for(int i=0; i<rounds; i++){
                // System.out.println( "DEBUG-" + id + ":\texecuting..." );
                expected = atomicInt.get();
                update = random.nextInt();
                boolean result = atomicInt.compareAndSet(expected, update);
                // System.out.println( "DEBUG-" + id + ":\texecuted? " + executed);
                Thread.yield();
            }
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