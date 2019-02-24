# Java Threads
## Launching threads
### Thread control

- `join()` wait for thread to finish

  ```
  Computation c = new Computation(34);
  Thread t = new Thread(c);
  t.start();
  t.join();
  System.out.println("done");
  ```

- `sleep(int n)` sleep for n ms (keep locks)
