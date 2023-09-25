package de.tgx03;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A list wrapper supposed to enable a list to be read from in parallel,
 * but still ensures only one write-operation occurs at the same time and also no reads are executed during writing.
 *
 * @param <E> The type of elements in this list
 */
public class SynchronizedList<E> implements List<E> {

	/**
	 * The list made synchronized by this class.
	 */
	private final List<E> list;
	/**
	 * The counter of how many threads are currently reading from this list in some way.
	 */
	private final AtomicInteger readers = new AtomicInteger(0);
	/**
	 * Whether a writer thread is currently trying to access this list.
	 */
	private volatile boolean writer = false;

	/**
	 * Creates a new synchronized list from another list.
	 *
	 * @param list The list to make synchronized.
	 */
	public SynchronizedList(List<E> list) {
		this.list = list;
	}

	@Override
	public int size() {
		readerWait();
		int result = list.size();
		readerLeave();
		return result;
	}

	@Override
	public boolean isEmpty() {
		readerWait();
		boolean result = list.isEmpty();
		readerLeave();
		return result;
	}

	@Override
	public boolean contains(Object o) {
		readerWait();
		boolean result = list.contains(o);
		readerLeave();
		return result;
	}

	@Override
	public java.util.Iterator<E> iterator() {
		return new Iterator();
	}

	@Override
	public Object[] toArray() {
		readerWait();
		Object[] result = list.toArray();
		readerLeave();
		return result;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		readerWait();
		T[] result = list.toArray(a);
		readerLeave();
		return result;
	}

	@Override
	public synchronized boolean add(E e) {
		writerWait();
		boolean result = list.add(e);
		writerLeave();
		return result;
	}

	@Override
	public synchronized boolean remove(Object o) {
		writerWait();
		boolean result = list.remove(o);
		writerLeave();
		return result;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		readerWait();
		//noinspection SlowListContainsAll It's likely the underlying implementation is already as performant as possible, making any additional tricks rather pointless.
		boolean result = list.containsAll(c);
		readerLeave();
		return result;
	}

	@Override
	public synchronized boolean addAll(Collection<? extends E> c) {
		writerWait();
		boolean result = list.addAll(c);
		writerLeave();
		return result;
	}

	@Override
	public synchronized boolean addAll(int index, Collection<? extends E> c) {
		writerWait();
		boolean result = list.addAll(index, c);
		writerLeave();
		return result;
	}

	@Override
	public synchronized boolean removeAll(Collection<?> c) {
		writerWait();
		boolean result = list.removeAll(c);
		writerLeave();
		return result;
	}

	@Override
	public synchronized boolean retainAll(Collection<?> c) {
		writerWait();
		boolean result = list.retainAll(c);
		writerLeave();
		return result;
	}

	@Override
	public synchronized void clear() {
		writerWait();
		list.clear();
		writerLeave();
	}

	@Override
	public E get(int index) {
		readerWait();
		E result = list.get(index);
		readerLeave();
		return result;
	}

	@Override
	public synchronized E set(int index, E element) {
		writerWait();
		E result = list.set(index, element);
		writerLeave();
		return result;
	}

	@Override
	public synchronized void add(int index, E element) {
		writerWait();
		list.add(index, element);
		writerLeave();
	}

	@Override
	public synchronized E remove(int index) {
		writerWait();
		E result = list.remove(index);
		writerLeave();
		return result;
	}

	@Override
	public int indexOf(Object o) {
		readerWait();
		int result = list.indexOf(o);
		readerLeave();
		return result;
	}

	@Override
	public int lastIndexOf(Object o) {
		readerWait();
		int result = list.lastIndexOf(o);
		readerLeave();
		return result;
	}

	@Override
	public ListIterator<E> listIterator() {
		return new Iterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		Iterator iterator = new Iterator();
		iterator.current.lazySet(index);
		return iterator;
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return new SynchronizedList<>(list.subList(fromIndex, toIndex));
	}

	/**
	 * This method makes this thread wait if another thread is currently writing to this list
	 * or other threads are waiting to write
	 */
	private void readerWait() {
		while (writer) {
			try {
				synchronized (this) {
					if (writer) wait();
				}
			} catch (InterruptedException ignored) {
			}
		}
		readers.incrementAndGet();
	}

	/**
	 * This method decrements the counter and also informs any potential waiting writer thread
	 * in case the counter reached zero.
	 */
	private void readerLeave() {
		int count = readers.decrementAndGet();
		if (count == 0 && writer) synchronized (this) {
			notifyAll();
		}
	}

	/**
	 * Makes a writing thread wait until no more threads are reading from this list.
	 */
	private synchronized void writerWait() {
		writer = true;
		while (readers.get() > 0) {
			try {
				wait();
			} catch (InterruptedException ignored) {
			}
		}
	}

	/**
	 * Allows reading threads to access this list again.
	 */
	private synchronized void writerLeave() {
		writer = false;
		notifyAll();
	}

	private class Iterator implements ListIterator<E> {

		private final AtomicInteger current = new AtomicInteger(0);

		@Override
		public boolean hasNext() {
			readerWait();
			boolean result = list.size() > current.get();
			readerLeave();
			return result;
		}

		@Override
		public E next() {
			readerWait();
			int position = current.getAndIncrement();
			if (list.size() > position) {
				E result = list.get(position);
				readerLeave();
				return result;
			} else {
				readerLeave();
				throw new NoSuchElementException("No next element");
			}
		}

		@Override
		public boolean hasPrevious() {
			readerWait();
			boolean result = !list.isEmpty() && current.get() > 0;
			readerLeave();
			return result;
		}

		@Override
		public E previous() {
			readerWait();
			int position = current.getAndDecrement();
			E result;
			if (!list.isEmpty() && position >= 0 && position < list.size()) {
				result = list.get(position);
			} else if (list.isEmpty() || position < 0) {
				readers.decrementAndGet();
				throw new NoSuchElementException("No previous element");
			} else {
				int last = list.size() - 1;
				result = list.get(last);
			}
			readerLeave();
			return result;
		}

		@Override
		public int nextIndex() {
			return current.get() + 1;
		}

		@Override
		public int previousIndex() {
			return current.get() - 1;
		}

		@Override
		public synchronized void remove() {
			writerWait();
			list.remove(current.get());
			writerLeave();
		}

		@Override
		public synchronized void set(E e) {
			writerWait();
			list.set(current.get(), e);
			writerLeave();
		}

		@Override
		public synchronized void add(E e) {
			writerWait();
			list.add(current.get() + 1, e);
			writerLeave();
		}
	}
}