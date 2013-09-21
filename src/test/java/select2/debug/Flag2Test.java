package select2.debug;

import java.util.Date;

public class Flag2Test{

    public static void main(String[] args){
		// init test
		int rounds = args.length > 0 ? Integer.parseInt( args[0] ) : 1000;
				
		Flag2 flag2 = new Flag2();
		Set2 set2 = new Set2(flag2);
		Wait2 wait2 = new Wait2(flag2);
		
		set2.setWait2(wait2);
		set2.setRounds(rounds);
		set2.setPriority(Thread.MIN_PRIORITY);
		
		// run test
		long start = (new Date()).getTime();
		
		set2.start();
		wait2.start();
		
		try{set2.join();} catch(Throwable ignored){}
		try{wait2.join();} catch(Throwable ignored){}

		long finish = (new Date()).getTime();

		// test result
		int set2Count = set2.getCount();
		int set2SyncRate = (int) ((100 * flag2.g_getSyncSets()) / (long) set2Count);
		int wait2Count = wait2.getCount();
		int wait2SyncRate = wait2Count > 0 ? (int) ((100 * flag2.g_getSyncWaits()) / (long) wait2Count) : 0;
		
		String msg = "flag2 test result: ";
		msg += set2Count + " sets with " +  set2SyncRate + "% sync sets";
		msg += " and ";
		msg += wait2Count + " waits with " + wait2SyncRate + "% sync waits";
		msg += " in " + rounds + " rounds";
		msg += " and ";
		msg += (finish-start) + " msec";
		System.out.println( msg );
	}

	protected static class Set2 extends Thread{
		private final Flag2 flag2;
		private int count;
		private Wait2 wait2;
		private int rounds;
		
		public void setWait2(Wait2 wait2){
			this.wait2 = wait2;
		}
		
		public Set2(Flag2 flag2){
			this.flag2 = flag2;
		}
		
		public void setRounds(int rounds){
			this.rounds = rounds;
		}
		
		public int getCount(){
			return count;
		}
		
		public void run(){
			count = 0;
			while (count < rounds){
				flag2.set(true);
				count++;
			}
		    flag2.terminateWait();
		}
	}

	protected static class Wait2 extends Thread{
		private final Flag2 flag2;
		private int count;
		
		public Wait2(Flag2 flag2){
			this.flag2 = flag2;
		}
		
		public int getCount(){
			return count;
		}
		
		public void run(){
			count = 0;
			try{
				while( !flag2.isWaitTerminated() ){
					flag2.waitUntil(true);
					flag2.set(false);
					count++;
				}
			}
			catch(Throwable ignored){}
		}
	}
}