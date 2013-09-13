`flag2` synchronization primitive
==

It implements kinda wait-free conditional wait. It handles only 2 threads. It enables to wait until a flag becomes true or false. It is similar to [wait()](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#wait%28%29) and [notify()](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#notify%28%29) in Java, just wait-free.

assumptions 
--

    volatile
    synchronized
    wait, notify

states
--

The protocol has the following states:

`value: boolean` - the value of the flag

`until_value: boolean` - if there is a wait on this flag then this is the condition value, ie. if for instance `until_value` is true then a thread is waiting until the flag becomes true

`waiting` - shows whether a wait is going on


methods
--

`get()` - get the value of the flag

`set(v)` - set the value of the flag to the arg, pseudo code:

     # chage the value
     value = v

     # check whether there is a wait
     if waiting:

        # if the the new value is the one waited for, notify the waiting thread
        if value == until_value:

            # wait is synchronized
            synchronized(this): 

                # notify the thread that is waiting
                notify


`wait_until(v)` - wait until the flag becomes `v`, pseudo code:  

     # wait only if the value is not already v
     if not (value == v):

         # set the until value (field) to v
         until_value = v

         # mark waiting
         waiting = true

         # wait while the flag differs from the conditional value
         while not (value == until_value): 

             # wait is synchronized on the monitor
             synchronized(this):
             
                 # recheck the condition
                 if not (value == until_value): 

                     # wait on the monitor
                     wait

         # not waiting
         waiting = false


`wait_while(v)` - wait while the flag is `v`, the code is simply the negation of `wait_until`:

    wait_until(not v) 


properties
--

**if wait is issued when the condition is true it terminates immediately**

**if set is invoked after wait then wait terminates if no more set is issued**

    wait_until(v) < set(v) => wait_until terminates 


*Case 1 - wait is not issued*

It either means that the condition is true or the thread is blocked in the` synchronization`. The latter cannot happen due to the conditions.


*Case 2 - wait is issued but notify is not*

`notify` is not issued due to either `wait` was false or the thread blocked in the synchronized. The latter could not happen, hence monitor was null at the time the thread checked it. That is the other thread has not yet set the wait flag to true:

    if waiting <= waiting = true

since the value is already set (in this to case `until_value`) before the wait flag is checked:

    value = until_value << if waiting

and wait flag is set before while condition is checked:

    waiting = true <<  while not (value == until_value)

the waiter does not go into wait:

    value = until_value <<  while not (value == until_value)

contradiction.

*Case 3 - wait is issued earlier then notify*

    wait < notify

Since they run in synchronized block it means that wait was not just issued earlier but also finished earlier.

    wait << notify

Hence it terminates due to the notification...


*Case 4 - wait is issued later then notify*

    notify < wait

Again due to the `synchronized` block this means that notify finished earlier.

    notify << wait

Since `wait` runs in a `synchronized` block with another stament which rechecks the wait condition, the above means that `notify` happened before the waiter rechecked the condition:

    notify << if not (value == until_value)

Since `notify` is only called when `value` is already set to `until_value` and wait is only executed if the opposite is true (`not (value == until_value)`), this is a contradiction.


*Case 5 - wait and notify issued at the same time*

Could not happen due to the `synchronized` block