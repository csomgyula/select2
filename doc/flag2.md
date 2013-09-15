`flag2` synchronization primitive
==

It implements kinda wait-free conditional wait, is similar to [wait()](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#wait%28%29) and [notify()](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#notify%28%29) in Java, just wait-free.

It handles only 2 threads. `flag2` enables one of the threads to wait until a flag becomes true or false w/o busy polling, ie. instead of a `flag = false; while(not flag){...}` loop you can put `flag.waitWhile(false)` and the thread goes to sleep while the flag is false.

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

`set(v)` - set the value of the flag and wakes up the waiting thread if there's any and the wait condition is met, pseudo code:

     # chage the value
     value = v

     # check whether there is a wait and whether the new value is the one waited for, if yes, then notify the waiting thread
     if waiting and until_value == v:

         # unset the wait flag
         waiting = false;

         # wait is synchronized on this
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

     	 # recheck the condition
     	 if not (value == v):

             # wait while wait flag is true ie. the flag differs from the conditional value
         	 while waiting: 

                 # wait is synchronized on this
                 synchronized(this):
             
                     # recheck whether waiting
                     if waiting: 

                         # wait on this
                         wait

         # the condition so far became true
         else:

             # not waiting
             waiting = false

`wait_while(v)` - wait while the flag is `v`, the code is simply the negation of `wait_until`:

    wait_until(not v) 


properties
--

**Property 1: If the wait is issued when the condition is true it terminates immediately if a parallel set is not updating the condition value**

    set(v) << wait_until(v) => wait_until immediately terminates 

**Property 2: If the appropiate `set()` is invoked at any time then this wait terminates if:**

* **(i) no other wait is issued in parallel with or after this one**
* **(ii) no other `set` is issued after this one and before `wait_until` terminates**

Formally:

    set(v) => wait_until terminates 

Proof:

*Case 1 - wait is not issued*

It either means that the condition is true or the thread is blocked in the `synchronization`. The latter cannot happen due to the conditions.


*Case 2 - wait is issued but notify is not*

(1) `notify` is not issued due to either `waiting` was false or the thread blocked in the synchronized. The latter could not happen, hence `waiting` was false at the time the setter thread checked it. That is the other thread (the waiter) has not yet set the wait flag to true:

    if waiting <= waiting = true

(2) Since the setter thread already set the value before it checked the wait flag:

    value = until_value << if waiting

and the waiter thread sets the wait flag before it rechecks the condition:

    waiting = true << if not (value = until_value)

the waiter does not go into wait since the check happend after the condition became true:

    value = until_value << if not (value = until_value)

contradiction.


*Case 3 - wait is issued earlier then notify*

    wait < notify

Since they run in synchronized block it means that wait was not just issued earlier but also finished earlier.

    wait << notify

Hence it terminates due to the notification...


*Case 4 - wait is issued later then notify*

    notify < wait

(1) Again due to the `synchronized` block this means that notify finished earlier:

    notify << wait

(2) Since `wait` runs in a `synchronized` block with another stament which rechecks whether the wait flag is true, the above means that `notify` happened before the waiter rechecked the flag:

    notify << if waiting: wait

(3) Also before `notify` is issued that thread unset the wait flag, that is:

    waiting = false << notify

(4) Since `wait` is only executed if the wait flag is true, this means that the waiter thread modified the wait flag between the above two statements:

    waiting = false <= waiting = true << if waiting: wait

(5) Hence the waiter thread rechecked the condition later then the value was set,:

    value = v << if not(value = v)

since the setter thread sets the value before the wait flag:

    value = v << waiting = false

and the waiter thread rechecks the condition after it set the wait flag:

    waiting = true << if not(value = v)

This is a contradiction since the thread only goes to sleep if the recheck is true.


*Case 5 - wait and notify issued at the same time*

Could not happen due to the `synchronized` block


**Property 3: If the appropiate `set()` is invoked after the thread went into wait then the wait terminates**

Proof: evident, see Case 3 above.

Notes: 

* This version of wait supports only one wait. Otherwise if another wait is called then the previous might block. Reason: since `flag2` deals with 2 threads it does not make sense when both thread is waiting.
* If two sets are issued before wait terminates it may happen that wait does not terminate. 

Sample scenario:

    thread 1: wait_until(v)
    thread 0: set(v)
    thread 0: value = v
    thread 0: if waiting and until_value == v # evaluates to false
    thread 0: returns
    thread 0: set(not v)
    thread 0: value = not v
    thread 0: if waiting and until_value == v # evaluates to false
    thread 0: returns
    thread 1: if not (value == v) # evaluates to true
    thread 1: waiting = true
    ...

### TODO ###

* Java impl, test
* wait might throw an error if another wait is already going on