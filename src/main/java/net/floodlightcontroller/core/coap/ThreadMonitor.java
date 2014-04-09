package net.floodlightcontroller.core.coap;
import java.lang.Thread.State;
import java.util.HashMap;


public class ThreadMonitor implements Runnable {

	HashMap<Thread, String> thread_ap_map = new HashMap<Thread, String>();
	
	@Override
	public void run() {
		while (true) {
			try {
				System.out.println("Starting thread monitor loop");
				Thread.sleep(5000);
				synchronized (thread_ap_map) {
					for (Thread thread: thread_ap_map.keySet()) {
						Thread.State state = thread.getState();
						if (state == State.TERMINATED) {
							System.out.println(thread + " for thread " + thread_ap_map.get(thread));
							thread_ap_map.remove(thread);
							break;
						}
					}
				}
			} catch (InterruptedException e) {
				System.err.println(e.getMessage() + " " + Thread.currentThread().getId());
				return;
			}
		}
	}

	public synchronized void add_thread_to_monitor(Thread thread, String str) {
		thread_ap_map.put(thread, str);
	}
}
