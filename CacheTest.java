import java.lang.*;
import java.io.*;
import java.util.*;

public class CacheTest {
	private int counter = 0;
	private int max_count = 1000000;
	private TASlock invalid = new TASlock(), reqPending = new TASlock();
	private int number_of_threads = 2;
	private int threads_quit;

	public CacheTest(){
		threads_quit = 0;
		startThreads();
	}
	
	public int readCounter(){
		while (invalid.get());
		reqPending.unlock();
		if(counter < max_count)
			return(counter);
		else return(-1);
	}
	
	public synchronized boolean writeCounter(int value){
		counter = value;
		invalid.unlock();
		if(value < max_count) 
		    return(true);
		else return(false);
	}

	public synchronized boolean invalidTest() {
		return invalid.get();
	}
	
	public synchronized boolean reqPendingTest() {
		return reqPending.get();
	}
	
	public synchronized void invalidSet() {
		invalid.set();
	}
	
	public synchronized void reqPendingSet() {
		reqPending.set();
	}
	
	public synchronized int threads_quit_increase() {
		threads_quit = threads_quit + 1;
		if (threads_quit < number_of_threads) return (0);
		else return (-1); 
	}
	
	public void startThreads(){
		CacheReader cr = new CacheReader(this, counter);
		CacheWriter cw = new CacheWriter(this, counter);
		cr.start(); cw.start(); 
	}
	
	public static void main(String args[]){
		CacheTest a = new CacheTest();
	}
}
	
class CacheReader extends Thread {
		CacheTest parent;
		int counter;
		
		public CacheReader(CacheTest parent, int number){
			this.parent = parent;
			this.counter = number;
		}
		
		public void run(){
			boolean cont = true;
			
			while (cont) {
				if (parent.invalidTest()) {
					parent.reqPendingSet(); // Sending a pending read request to other threads.
					counter = parent.readCounter();
					if (counter == -1)
						cont = false;
					else 
						System.out.println("I am Cache Reader. The counter I hold has value: " + counter);
				}
			}
			
			System.out.println("Cache Reader is quitting!");
			if (parent.threads_quit_increase() == -1)
				System.out.println("All threads have quitted. Done!");
		}
}

class CacheWriter extends Thread {
		CacheTest parent;
		int counter;
		
		public CacheWriter(CacheTest parent, int number){
			this.parent = parent;
			this.counter = number;
		}
		
		public void run(){
			boolean cont = true;
			
			while (cont) {
				if (parent.reqPendingTest()) // check whether this is pending read request from the reader thread.
												// If so, update the counter in the main memory. 
												// Otherwise, proceed with counter increment.
				{ 
					cont = parent.writeCounter(counter);
					if (cont) System.out.println("I am Cache Writer. The counter I set has value: " + counter + "   Hello Dr. Lin");
				}
				counter = counter + 1;
				parent.invalidSet(); 	// Invalidate other cache copies and the original counter in main memory
											// after the counter increment.
			}		
			
			cont = parent.writeCounter(counter);
			System.out.println("Cache Writer is quitting!");
			if (parent.threads_quit_increase() == -1)
				System.out.println("All threads have quitted. Done!");
		}
}

class TASlock {
 AtomicBoolean state =
  new AtomicBoolean(false);

 public boolean get() {
 	return state.get();
 }
 
 public void set() {
 	state.set(true);
 }
 
 public void lock() {
  while (state.getAndSet(true)) {}
 }
 
 public void unlock() {
  state.set(false);
 }
} 

class AtomicBoolean {
 boolean value;
 
 public AtomicBoolean(boolean initial) {
 	value = initial;
 }
 
 public synchronized boolean getAndSet(boolean newValue) {
   boolean prior = value;
   value = newValue;
   return prior;
 }
 
 public synchronized void set(boolean newValue) {
 	value = newValue;
 }
 
 public synchronized boolean get() {
 	return value;
 }
}
