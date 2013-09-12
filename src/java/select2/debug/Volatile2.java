package select2.debug;

public class Volatile2{
	
	protected static class TestVolatile2{
		private final long[] threadIds;
		private State state0;
		private State state1;
		private volatile State state;
		
		public TestVolatile2(Thread[] threads){	
			threadIds = new long[2];
			for (int i = 0; i<2;i++){
				threadIds[i] = threads[i].getId();
			}
			state0 = State.IDLE;
			state1 = State.IDLE;
			state = State.OK;
		}			
		
		public void test(){
			assert state == State.OK;
			
			// get the internal thread number
			int i =  getInternalThreadId();
			
			// activate
			setState(i, State.GUARD);
			try{Thread.sleep(1+i);}catch(Throwable ignore){};
			
			// guard
			if( getState(i+1) != State.IDLE ){ setState(i, State.WAIT); }
			else{ setState(i, State.ACTIVE); }
			System.out.println("THREAD-" + i + ":\tstate " + getState(i) );
			try{Thread.sleep(1+i);}catch(Throwable ignore){};
			
			// assert: one thread must not be ACTIVE
			if( state0 == State.ACTIVE && state1 == State.ACTIVE ){
				state = State.ERROR;
			}
			assert state0 != State.ACTIVE || state1 != State.ACTIVE;
			
			// deactivate
			setState(i, State.IDLE);
			try{Thread.sleep(1+i);}catch(Throwable ignore){};
		}
		
		protected int getInternalThreadId(){
			if (threadIds[0] == Thread.currentThread().getId()){ return 0; }
			else{ return 1; }
		}		
	
		protected synchronized void setState(int i, State state){
			if( (i % 2) == 0 ){ state0 = state; }
			else{ state1 = state; }
		}

		protected synchronized State getState(int i){
			if( (i % 2) == 0 ){ return state0; }
			else{ return state1; }
		}	
		
	}
	
	protected enum State{
		IDLE, GUARD, WAIT, ACTIVE, OK, ERROR
	}
	
	protected static class TestVolatile2Thread extends Thread{
		private TestVolatile2 test;
		
		public void setTest(TestVolatile2 test){ this.test = test; }
		
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
		TestVolatile2Thread[] threads = new TestVolatile2Thread[2];
		threads[0] = new TestVolatile2Thread();
		threads[1] = new TestVolatile2Thread();
		
		TestVolatile2 test = new TestVolatile2(threads);
		
		threads[0].setTest(test);
		threads[1].setTest(test);
		
		threads[0].start();
		threads[1].start();
	}
}