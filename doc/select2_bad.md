a bad `select2` protocol
==

According to the literature 

> There is no wait-ffree solution to two-process consensus by atomic read/write registers

See: [Maurice Herlihy: Impossibility and universality results for wait-free synchronization](http://repository.cmu.edu/cgi/viewcontent.cgi?article=2796&context=compsci)

The following protocol is a bad one, it tries to implement a wait free protocol based on atomic read-write registers.

states
---

* `active0`, `active1`
* `token`
* `nto_selected`, `nto_interrupted` - handles the appropiate flag for the thread which is not the token owner

pseudo code
---

`select(closure)`:

    # activate this
    activate(i)

    # check whether this is the token owner
    token_owner = token == i

    # if this is not the token owner, then set the interrupted flag to false 
    if not token_owner: interrupted = false

    # if the other thread is active
    if active(i+1):
       # if this is the token owner
       if token_owner:
           # try to interrupt the other thread
           nto_interrupted = true

           # if the other thread is not selected, this will be selected
           if not nto_selected:

              # mark this as selected
              result = true 

              # execute the critical section
              closure()

           # if the other thread is selected even though it was interrupted then this won't be selected
           else:
              # mark this as not selected
              result = false

           # pass the token to the other thread
           token = i + 1

           # deactivate this
           deactivate(i)

           # return the selection result
           return result
        # if this is not the token owner
        else: 
           # deactivate this
           deactivate(i)

           # return negative result - not selected
           return false
    # else if the other thread is not active
    else:
       # if this is the token owner
       if token_owner:
           # execute the critical section
           closure()

           # pass the token to the other thread
           token = i + 1

           # deactive this
           deactivate(i) 

           # return positive result
           return true
       else:
           # select this only if it was not interrupted
           nto_selected = not nto_interrupted

           # if selected
           if nto_selected: 
              # mark this as selected
              result = true 

              # execute critical section
              closure()

              # unset the selection flag
              selected = false
           else:
              # mark this as selected
              result = false

           # deactivate this
           deactivate(i)

           # return selection result
           return result         

`activate(i)`: activate thread i

    if i == 0: active0 = true
    else: active1 = true

`deactivate(i)`: deactivate thread i

    if i == 0: active0 = false
    else: active1 = false


why is it wrong?
---

Because `selected = not interrupted` is not atomic. Suppose that thread 0 is the token owner (that is `token == 0`) and both thread is in the guard:

    thread 1: nto_selected = not nto_interrupted part 1: read not_interrupted

which yields false since interrupt is not yet set

    thread 0: nto_interrupted = true

which sets the interrupt flag to true

    thread 0: if not nto_selected

which evaluates to to true since the select flag is not yet set, hence thread 0 becomes selectable.

    thread 1: nto_selected = not nto_interrupted part 2: set nto_selected

which sets the selected flag to true since the previous read of the interrupt flag yielded false

conclusion
---

I guess that wait free protocol can be implemented if atomic copy is supported by the underliing platform. Ie. if `nto_selected = not nto_interrupted` is atomic the protocol might work
 
