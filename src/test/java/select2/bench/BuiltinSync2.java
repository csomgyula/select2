package select2.bench;

/**
 * sync2 service implemented upon the Java-builtin synchronization primitive.
 * It does not implement the protocol itself, just its features and services. Ie. it provides the same features and 
 * services as sync2 but uses simply the synchronized keyword.
 */
public class BuiltinSync2 implements Executor{

	/**
	 * Implements the sync2 application service with the synchronization keyword.
	 */
	public synchronized boolean execute(Closure closure){
	    return closure.execute();
	}
}