# Unsafe API Usage

Extracts calls of methods which use `sun.misc.Unsafe`. Closely related methods are grouped according to the mapping given in the paper:
[Use at Your Own Risk: The Java Unsafe API in the Wild][1] **by Lues Mastrangelo et al.** As described in the paper, the analysis supports the following categories:

## Alloc
  The *Alloc* group contains only the `allocateInstance`
      method, which allows the developer to allocate a Java
      object without executing a constructor.

## Array
  The *Array* group contains methods and fields for computing
      relative addresses of array elements. The fields were
      added as a simpler and potentially faster alternative in a
      more recent version of Unsafe. The value of all fields in
      this group are constants initialized with the result of a call
      to either `arrayBaseOffset` or `arrayIndexScale` in the Array
      group.

## CompareAndSwap
  The *CAS* group contains methods to atomically compare-and-swap
      a Java variable. These operations are implemented
      using processor-specific atomic instructions. For
      instance, on x86 architectures, compareAndSwapInt is
      implemented using the `CMPXCHG` machine instruction.

## Class
  Methods of the *Class* group are used to dynamically
      load and check Java classes.

## Fence
  The methods of the *Fence* group provide memory fences
      to ensure loads and stores are visible to other threads.
      These methods are implemented using processor-specific instructions.

## Fetch & Add
  The *Fetch & Add* group, like the CAS group, allows the
      programmer to atomically update a Java variable. This
      group of methods was also added recently in Java 8.

## Heap, Heap Get and Heap Put
  The *Heap* group methods are used to directly access
      memory in the Java heap. The Heap Get and Heap Put
      groups allow the developer to load and store a Java variable.

## Misc
  The *Misc* group contains the method getLoadAverage, to
      get the load average in the operating system run queue
      assigned to the available processors.

## Monitor
  The *Monitor* group contains methods to explicitly manage
      Java monitors.

## Off-Heap
  The *Off-Heap* group provides access to unmanaged
      memory, enabling explicit memory management. Similarly
      to the Heap Get and Heap Put groups, the OffHeap
      Get and Off-Heap Put groups allow the developer
      to load and store values in Off-Heap memory. The usage
      of these methods is non-negligible, with getByte
      and putByte dominating the rest. The value of the
      *ADDRESS SIZE* field is the result of the method *addressSize()*.

## Offset
  Methods of the *Offset* group are used to compute the location
      of fields within Java objects. The offsets are used
      in calls to many other sun.misc.Unsafe methods, for instance
      those in the Heap Get, Heap Put, and the CAS
      groups.

## Ordered Put
  The *Ordered Put* group has methods to store to a Java
      variable without emitting any memory barrier but guaranteeing
      no reordering across the store.

## Park
  The park and unpark methods are contained in the *Park*
      group. With them, it is possible to block and unblock a
      thread's execution.

## Throw
  The throwException method is contained in the *Throw*
      group, and allows one to throw checked exceptions without
      declaring them in the throws clause.

## Volatile Get & Put
  The *Volatile Get and Volatile Put* groups allow
      the developer to store a value in a Java variable with
      volatile semantics.


[1]: http://dl.acm.org/citation.cfm?doid=2814270.2814313
