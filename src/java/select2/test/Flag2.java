package select2.test;

/**
 * It implements kinda wait-free conditional wait, is similar to 
 * [wait()](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#wait%28%29) and 
 * [notify()](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#notify%28%29)
 * in Java, just wait-free.
 *
 * It handles only 2 threads. `flag2` enables one of the threads to wait until 
 * a flag becomes true or false w/o busy polling, ie. instead of a 
 * 
 *     flag = false;
 *     while(not flag){...}
 * 
 * loop you can simply put 
 *
 *     flag = new Flag();
 *     flag.waitWhile(false); 
 *
 * and the thread goes to sleep while the flag is false.
 */
public class Flag2{

	// the value of the flag
	private volatile boolean value;

	// if there is a wait on this flag then this is the condition value, 
	// ie. if for instance `until_value` is true then a thread is waiting until the flag becomes true
	private volatile boolean until_value;
	
	// shows whether a wait is going on
	private volatile boolean waiting;

	/**
	 *  get the value of the flag
	 */
	public boolean get(){
		return value;
	}
	
	/**
	 * set the value of the flag and wakes up the waiting thread if there's any and the wait condition is met
	 */
	public void set(boolean v){

		// change the value
		value = v;

		// check whether there is a wait and whether the new value is the one waited for, if yes then notify the waiting thread
		if ( waiting && until_value == v){

			// wait is synchronized on this
			synchronized(this){

				// unset the wait flag
				waiting = false;
				
				// notify the thread that is waiting
				this.notify();
			}
		}
	}
	
	/**
	 * wait until the flag becomes v, will be waken up by the thread who set the value to v
	 */
	public void waitUntil(boolean v) throws InterruptedException{
	
		 // wait only if the value is not already v
		 if (value != v){

			// set the until value (field) to v
			until_value = v;

			// mark waiting
			waiting = true;
			
			// recheck the condition
			if (value != v){
		 
				// wait while the wait flag is true (ie the value differs from the conditional value)
				while ( waiting ){ 

					 // wait is synchronized on this
					 synchronized(this){
					 
						// recheck the wait flag
						if (waiting){
					
							Thread thisThread = Thread.currentThread();
							if ( thisThread.isInterrupted() ) throw new InterruptedException();
				            else{
								// System.out.println("THREAD-" + thisThread.getId() + ":\tINTERRUPT: " + thisThread.isInterrupted() + " STATE: " + thisThread.getState() );
							}
							// wait on this
							try{ this.wait(); } 
							catch(InterruptedException interrupted){ throw interrupted; }
							// catch(Throwable ignored){} // might be better handled in production quality
						}
					}
				}
			}
			// the condition so far became true
			else{
				// mark waiting
				waiting = false;				
			}
		}
	}
	
    /**
	 * wait while the flag is  v, the opposit of wait_until, ie.:
	 * 
	 *     wait_while(v) 
	 * 
	 * is the same as
	 *
	 *     wait_until(!v);
	 */
	public void waitWhile(boolean v) throws InterruptedException{
		waitUntil(!v);
	}
	

	private volatile Coordination coordination = Coordination.WAITER_ACTIVE;
	
	protected enum Coordination{
		WAITER_WAIT, SET_AND_SETTER_WAIT, WAITER_ACTIVE;
	}

	public void waiterWaitAndSetWakeup(){
		synchronized(this){
			if (coordination == Coordination.WAITER_ACTIVE){
				System.out.println("THREAD-" + Thread.currentThread().getId() + ":\tWAITER_WAIT");
				coordination = Coordination.WAITER_WAIT;
				try{waitUntil(true);} catch(Throwable ignored){}
				set(false);
			}
		}
		
		synchronized(coordination){
			// System.out.println("THREAD-" + Thread.currentThread().getId() + ":\tWAITER_ACTIVE");
			coordination = Coordination.WAITER_ACTIVE;
			
			if (coordination == Coordination.SET_AND_SETTER_WAIT){
				coordination.notify();
			}
		}
	}
	
	public int setAndSetterWait(int count){
		synchronized(coordination){
			synchronized(this){
				if (coordination == Coordination.WAITER_WAIT){
					coordination = Coordination.SET_AND_SETTER_WAIT;
				}
			}

			if (coordination == Coordination.SET_AND_SETTER_WAIT){
				System.out.println("THREAD-" + Thread.currentThread().getId() + ":\tSET_AND_SETTER_WAIT(" + count + ")");
				set(true);
				while (coordination == Coordination.SET_AND_SETTER_WAIT){
					try{coordination.wait();} catch(Throwable ignored){}
				}		
				System.out.println("THREAD-" + Thread.currentThread().getId() + ":\tSETTER_ACTIVE(" + count + ")");
				return 1;
			}
			else{
				return 0;
			}
		}
	}		
}