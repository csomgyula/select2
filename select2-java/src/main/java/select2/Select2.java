package select2;

/**
 * Implementation of select2 protocol
 */
public class Select2 {
	/**
	* Holds the thread ids using the synch service.
	*/
	private final long[] threadIds;

	private volatile boolean active0, active1, selected0, selected1;
	private volatile int token;

    // conditional wait
    private final Object condMonitor;
    private volatile boolean condWait;

	/**
	 * The current implementation does not deal with open systems: one must explicitely give the threads to choose between.
	 */
	public Select2(Thread[] threads){
		threadIds = new long[2];
		for (int i = 0; i<2;i++){
			threadIds[i] = threads[i].getId();
		}

        condMonitor = new Object();
	}


    /**
     * select2 service
     */
	public boolean execute(Closure closure){
		// get the internal thread number
		int i =  getInternalThreadId();
		
		// 1. mark myself as active
        activate(i);
		
		// 2. check whether I am the token owner
		boolean token_owner = token == i;
	
		// 3. check whether the other thread already entered the selection protocol
        if ( isActive(i + 1) ){
            // 3.1. if I am not the token owner then wakeup owner, cleanup and exit
            if (!token_owner){
                nowait(i + 1);
                deactivate(i);
                return false;
            }
            // 3.2. if I am the token owner wait for the other thread till it decides what to do
            else{

//                while ( token == i && isActive(i + 1) && condWait ){
//                    Thread.yield();
//                }
                waitWhile(i);
            }
        }

		// 4. now different cases could happen:
		if (token_owner){
			// 4.1. if I was the token owner but the other thread took the ownership so far, then I am not selected, cleanup and exit
			if (token != i){
				deactivate(i);
				return false;
			}
			// 4.2. if I was and still is the token owner, then I am selected, give up the token ownership, cleanup and exit
			else{
				select(i);
				assert !isSelected(i+1);
                boolean result = closure.execute();
				deselect(i);
				releaseToken(i);
                deactivate(i);
				return result;
			} 
		}
		// 4.3. if I was not the token owner but reached this point, than I am selected, get the token ownership, cleanup and exit
		else {
			acquireToken(i);
			select(i);
            assert !isSelected(i+1);
            boolean result = closure.execute();
			deselect(i);
            deactivate(i);
			return result;
		}
	}

    // token

    /**
     * Get the token ownership
     *
     * @param i the index of thread who acquires
     */
    protected void acquireToken(int i){
        token = i;

        // may wake up the other thread
        mayNotify( (i+1) % 2 );
    }

    /**
     * Release the token ownership
     *
     * @param i the index of thread who releases
     */
    protected void releaseToken(int i){
        token = (i+1) % 2;
    }

    // active state

    /**
     * mark thread i as active
     */
    protected void activate(int i){
        setActive(i, true);
    }

    /**
     * mark thread i as inactive
     */
    protected void deactivate(int i){
        setActive(i, false);

        // may wake up the other thread
        mayNotify( (i + 1) % 2);
    }

    /**
     * set the active flag of thread i
     */
    protected void setActive(int i, boolean value){
        switch (i % 2){
            case 0: active0 = value; break;
            case 1: active1 = value; break;
        }
    }

    /**
     * get the active flag of thread i
     */
    protected boolean isActive(int i){
        if ((i % 2) == 0){ return active0; }
        else{ return active1; }
    }


    // select state

    /**
     * mark thread i as selected
     */
    protected void select(int i){
        setSelected(i, true);
    }

    /**
     * mark thread i as unselected
     */
    protected void deselect(int i){
        setSelected(i, false);
    }

    /**
     * set the selected flag of thread i
     */
    protected void setSelected(int i, boolean value){
        switch (i % 2){
            case 0: selected0 = value; break;
            case 1: selected1 = value; break;
        }
    }

    /**
     * get the selected flag of thread i
     */
    protected boolean isSelected(int i){
        if ((i % 2) == 0){ return selected0; }
        else{ return selected1; }
    }


    // conditions

    /**
     * The conditional wait for thread i
     *
     * @param i index of the thread wants to wait
     */
    protected void waitWhile(int i){
        // wait only if the condition is true
         if ( isCond(i) ){

             // mark waiting
             condWait = true;

             // recheck the condition
             if ( isCond(i) ){

                // wait while condition is true
                while ( isCond(i) ){

                    // wait is synchronized on the monitor
                    synchronized (condMonitor){

                        // recheck the condition
                        if ( isCond(i) ){

                            // wait on the monitor
                            try {
                                condMonitor.wait();
                            } catch (InterruptedException ignored) { } // TODO: handle interrupt
                        }
                    }
                }
             }
             // condition so far became false
             else{

                 // not waiting
                 condWait = false;
             }

         }
    }

    /**
     * mark thread i as not waiting
     */
    protected void nowait(int i){
        i = i % 2;

        condWait = false;

        // may wake up the thread
        mayNotify(i);
    }

    /**
     * Notify if there's a wait and the condition became false.
     * Must be called after a relevant flag is already changed.
     *
     * @param i index of the thread to notify
     */
    protected void mayNotify(int i){
       // notify only if there's a wait and the condition became false
       if ( condWait && !isCond(i) ){

           // mark no wait
           condWait = false;

           // notification is synchronized
           synchronized (condMonitor){

               // notify
               condMonitor.notify();
           }
       }
    }

    protected boolean isCond(int i){
       i = i % 2;
       return token == i && isActive(i + 1) && condWait;
    }


	/**
	 * Get the internal thread id used in the protocol: 0 or 1.
	 */
	protected int getInternalThreadId(){
		if (threadIds[0] == Thread.currentThread().getId()){ return 0; }
		else{ return 1; }
	}
}