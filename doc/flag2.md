`flag2` synchronization primitive
==

It implements a wait-free conditional wait for 2 threads. It is similar to [wait()](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#wait%28%29) and [notify()](http://docs.oracle.com/javase/7/docs/api/java/lang/Object.html#notify%28%29) in Java, just wait-free.

assumptions 
--

    volatile
    synchronized
    wait, notify

states
--

The protocol has the following states:

`value: boolean` - the value of the flag

`until_value: boolean` - if there is wait on this flag then this is the condition value, ie. if `until_value` is true then a thread is waiting until the flag becomes true

`until_monitor: null | MONITOR_TRUE | MONITOR_FALSE` - if there is wait on this flag then this is the associated monitor, otherwise null


methods
--

`get()` - get the value of the flag

`set(v)` - change the value of the flag, pseudo code:

     # chage the value
     value = v

     # check whether there is a wait
     if is_monitor:

        # if the the new value is the one waited until, notify the waiter
        if value == until_value:

            # wait is synchronized on the monitor
            synchronized(until_monitor): 

                # notify the monitor (the thread that is waiting)
                until_monitor.notify


`wait_until(v)` - wait until the flag becomes `v`, pseudo code:  

     # wait only if the value is not already v
     if not (value == v):

         # set the until value (field) to v
         until_value = v

         # set the monitor to the one associated with the condition (value)
         until_monitor = get_until_monitor(conditional_value)

         # wait while the flag differs from the conditional value
         while not (value == until_value): 

             # wait is synchronized on the monitor
             synchronized(until_monitor):
             
                 # recheck the condition
                 if not (value == until_value): 

                     # wait on the monitor
                     until_monitor.wait

         # empty the monitor
         monitor = null


internal methods:

`is_monitor`

    not (monitor == null)

`get_until_monitor(v)`

    case if v: MONITOR_TRUE
    case if not v: MONITOR_FALSE

properties
--

**if set is invoked after wait then wait terminates if no more set is issued**

    wait_until(v) < set(v) => wait_until terminates 


*Case 1 - wait is not issued*

It either means that the condition is true or the thread is blocked in the` synchronization`. The latter cannot happen due to the conditions.


*Case 2 - notify is not issued*

`notify` is not issued due to either `until_monitor` was null or the thread blocked in the synchronized. The latter could not happen, hence monitor was null at the time the thread checked it. That is the other thread has not yet set the monitor.

    if is_monitor <= until_monitor = get_until_monitor(conditional_value)

since value is set (in this to case until_value) before monitor is checked:

    value = until_value << if is_monitor

and monitor is set before while condition is checked:

    get_until_monitor(conditional_value) <<  while not (value == until_value)

the waiter does go into wait:

    value = until_value <<  while not (value == until_value)


*Case 3 - wait is issued earlier then notify*

    until_monitor.wait < until_monitor.notify

Since they run in synchronized block it means that wait was not just issued earlier but also finished earlier.

    until_monitor.wait << until_monitor.notify

Hence it terminates due to the notification...


*Case 4 - wait is issued later then notify*

    until_monitor.notify < until_monitor.wait

Again due to the `synchronized` block this means that notify finished earlier.

    until_monitor.notify << until_monitor.wait

Since `wait` runs in a `synchronized` block with another stament which rechecks the wait condition, the above means that `notify` happened before the waiter rechecked the condition:

    until_monitor.notify < if not (value == until_value)

Since `notify` is only called when `value` is already set to `until_value` and wait is only executed if the opposite is true (`not (value == until_value)`), this is a contradiction.


*Case 5 - wait and notify issued at the same time*

Could not happen due to the `synchronized` block