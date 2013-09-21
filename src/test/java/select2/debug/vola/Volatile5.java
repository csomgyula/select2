package select2.debug.vola;

public class Volatile5{
	
	protected static class TestVolatile5{
		private final long[] threadIds;
		private volatile State state0;
		private volatile State state1;
		private volatile State state;
		private final Lock lock;
		
		public TestVolatile5(Thread[] threads){	
			threadIds = new long[2];
			for (int i = 0; i<2;i++){
				threadIds[i] = threads[i].getId();
			}
			state0 = State.IDLE;
			state1 = State.IDLE;
			state = State.OK;
			lock = new Lock();
		}			
		
		public void test(int step){
			if((step % 100) == 0){
				System.out.println("THREAD-" + Thread.currentThread().getId() + ":\trounds " + step + "-" + lock);
			}
			
			assert state == State.OK;
			
			// get the internal thread number
			int i =  getInternalThreadId();
			
			// lock
			lock.lock(i);
			
			// activate
			setState(i, State.ACTIVE);
			try{Thread.sleep(1+i);}catch(Throwable ignore){};
			
			// assert: one thread must not be ACTIVE
			if( state0 == State.ACTIVE && state1 == State.ACTIVE ){
				state = State.ERROR;
				// System.out.println(lock);
			}
			assert state0 != State.ACTIVE || state1 != State.ACTIVE;
			
			// deactivate
			setState(i, State.IDLE);
			try{Thread.sleep(1+i);}catch(Throwable ignore){};
			
			// unlock
			lock.unlock(i);
		}
	
		protected int getInternalThreadId(){
			if (threadIds[0] == Thread.currentThread().getId()){ return 0; }
			else{ return 1; }
		}		
	
		protected void setState(int i, State state){
			if( (i % 2) == 0 ){ state0 = state; }
			else{ state1 = state; }
		}

		protected State getState(int i){
			if( (i % 2) == 0 ){ return state0; }
			else{ return state1; }
		}	
		
	}
	
	protected enum State{
		IDLE, ACTIVE, OK, ERROR
	}
	
	protected static class Lock{
		private volatile boolean locked;
		private volatile boolean lockRequest;
		private volatile boolean g_wait0, g_wait1;
		
		protected synchronized void lock(int i){
			if (locked){
				try{
					lockRequest = true;					
					switch(i){
						case 0: g_wait0 = true; break;
						case 1: g_wait1 = true; break;
					}
					// System.out.println(this);
					while (locked){ 
						try{ this.wait(); }
						catch(Throwable th){ throw new RuntimeException(th); }
					}
				}
				finally{
					lockRequest = false;
					switch(i){
						case 0: g_wait0 = false; break;
						case 1: g_wait1 = false; break;
					}
				}
			}
			
			locked = true;
		}
		
		protected synchronized void unlock(int i){
			locked = false;
			
			if (lockRequest){ this.notify(); }
		}
		
		public String toString(){
			String tos = "Lock:{";
			tos += "locked: " + locked;
			tos += ", ";
			tos += "lockRequest: " + lockRequest;
			tos += ", ";
			tos += "wait0: " + g_wait0;
			tos += ", ";
			tos += "wait1: " + g_wait1;
			tos += "}";
			return tos;
		}
	}
	
	protected static class TestVolatile5Thread extends Thread{
		private TestVolatile5 test;
		private volatile int step;
		
		public void setTest(TestVolatile5 test){ this.test = test; }
		
		private TestVolatile5Thread otherThread;
		public void setOtherThread(TestVolatile5Thread otherThread){ this.otherThread = otherThread; }
		
		public void run(){
			while(step < 1000){
				step++;
				test.test(step);
			}
		}
	}
	
	
	public static void main(String[] args){
		TestVolatile5Thread[] threads = new TestVolatile5Thread[2];
		threads[0] = new TestVolatile5Thread();
		threads[1] = new TestVolatile5Thread();
		
		TestVolatile5 test = new TestVolatile5(threads);
		
		threads[0].setTest(test);
		threads[0].setOtherThread(threads[1]);
		threads[1].setTest(test);
		threads[1].setOtherThread(threads[0]);
		
		threads[0].start();
		threads[1].start();
	}
}