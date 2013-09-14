package select2.test;

import java.util.Date;

public class Flag2Test{

    public static void main(String[] args){
		int rounds = args.length > 0 ? Integer.parseInt( args[0] ) : 1000;
				
		Flag2 flag2 = new Flag2();
		Set2 set2 = new Set2(flag2);
		Wait2 wait2 = new Wait2(flag2);
		
		set2.setWait2(wait2);
		set2.setRounds(rounds);
		
		long start = (new Date()).getTime();

		set2.start();
		wait2.start();

		
		try{set2.join();} catch(Throwable ignored){}
		try{wait2.join();} catch(Throwable ignored){}

		long finish = (new Date()).getTime();
		
		System.out.println("flag2 test result: " + set2.getCount() + " sets and " + wait2.getCount() + " waits in " + rounds + " rounds and " + (finish-start) + " msec");
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
				// set and wait for waiter thread to wakeup
				count += flag2.setAndSetterWait(count + 1);
			}
			
		    wait2.interrupt();
			System.out.println("THREAD-" + Thread.currentThread().getId() + ":\tINTERRUPT: " + wait2.isInterrupted() + " STATE: " + wait2.getState() );
			if (!wait2.isInterrupted()){
				wait2.interrupt();
				System.out.println("THREAD-" + Thread.currentThread().getId() + ":\tINTERRUPT: " + wait2.isInterrupted() + " STATE: " + wait2.getState() );
			}
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
				while(!interrupted()){
					// wait and wakeup setter thread
					flag2.waiterWaitAndSetWakeup();
					
					count++;
				}
			}
			catch(Throwable ignored){}
		}
		
	}
}