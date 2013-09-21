package select2.debug;

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

	private volatile long g_syncSets;
	private volatile long g_syncWaits;
	
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
		if ( waiting ){ 
			if(until_value == v){

				// unset the wait flag
				waiting = false;

				// wait is synchronized on this
				synchronized(this){
					g_syncSets++;
					
					// notify the thread that is waiting
					this.notify();
				}
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
						g_syncWaits++;
						
					    if (waitTerminated){ waiting = false; throw new InterruptedException(); }
						
						// recheck the wait flag
						if (waiting){
					
							// wait on this
							try{ this.wait(); } 
							catch(InterruptedException interrupted){ waiting = false; throw interrupted; }
							// catch(Throwable ignored){} // might be better handled in production quality
						}
						
					    if (waitTerminated){ waiting = false; throw new InterruptedException(); }
					}
				}
			}
			// the condition so far became true
			else{
				// unset the wait flag
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
	
	
	private volatile boolean waitTerminated;
	
	public void terminateWait(){
		synchronized(this){
			waitTerminated = true;
			this.notify();
		}
	}
	
	public boolean isWaitTerminated(){
		return waitTerminated;
	}
	
	public long g_getSyncSets(){ return g_syncSets; }
	public long g_getSyncWaits(){ return g_syncWaits; }
	
	
	private volatile T_WaitState t_waitState;
	public void t_Wait(){
		// wait
		synchronized(this){
			t_waitState = T_WaitState.WAIT;
            try {
                waitWhile(false);
            } catch (InterruptedException ignored) { }
        }
		
		// wakeup setter thread if necessary
	}

	private volatile Object t_setMonitor = new Object();
	private volatile T_SetState t_setState;
	public void t_Set(){
		// set to true
		t_setState = T_SetState.NOT_WAIT;
		set(true);
		
		// wait for waiter thread to wake up if necessary
		synchronized(t_setMonitor){
			synchronized(this){
				if (t_waitState == T_WaitState.WAIT){ t_setState = T_SetState.WAIT; }
			}
			
			if (t_setState == T_SetState.WAIT){
                try {
                    t_setMonitor.wait();
                } catch (InterruptedException ignored) { }
            }
		}
	}

    protected enum T_WaitState{
        WAIT;
    }

    protected enum T_SetState{
        WAIT, NOT_WAIT;
    }
}