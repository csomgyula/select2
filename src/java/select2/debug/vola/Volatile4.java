package select2.debug.vola;
 
import java.util.concurrent.locks.ReentrantLock;
 
public class Volatile4{
	
	protected static class TestVolatile4{
		private final long[] threadIds;
		private volatile State state0;
		private volatile State state1;
		private volatile State state;
		private final ReentrantLock lock;
		
		public TestVolatile4(Thread[] threads){	
			threadIds = new long[2];
			for (int i = 0; i<2;i++){
				threadIds[i] = threads[i].getId();
			}
			state0 = State.IDLE;
			state1 = State.IDLE;
			state = State.OK;
			lock = new ReentrantLock();
		}			
		
		public void test(){
			assert state == State.OK;
			
			// get the internal thread number
			int i =  getInternalThreadId();
			
			// lock
			lock.lock();
			
			// activate
			setState(i, State.ACTIVE);
			try{Thread.sleep(1+i);}catch(Throwable ignore){};
			
			// assert: one thread must not be ACTIVE
			if( state0 == State.ACTIVE && state1 == State.ACTIVE ){
				state = State.ERROR;
			}
			assert state0 != State.ACTIVE || state1 != State.ACTIVE;
			
			// deactivate
			setState(i, State.IDLE);
			try{Thread.sleep(1+i);}catch(Throwable ignore){};

			// unlock
			lock.unlock();
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
	
	protected static class TestVolatile4Thread extends Thread{
		private TestVolatile4 test;
		
		public void setTest(TestVolatile4 test){ this.test = test; }
		
		public void run(){
			int step = 0;
			while(true){
				step++;
				test.test();
				// System.out.println("THREAD-" + Thread.currentThread().getId() + ":\tround " + step);
			}
		}
	}
	
	
	public static void main(String[] args){
		TestVolatile4Thread[] threads = new TestVolatile4Thread[2];
		threads[0] = new TestVolatile4Thread();
		threads[1] = new TestVolatile4Thread();
		
		TestVolatile4 test = new TestVolatile4(threads);
		
		threads[0].setTest(test);
		threads[1].setTest(test);
		
		threads[0].start();
		threads[1].start();
	}
}