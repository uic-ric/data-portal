/**
 * 
 */
package edu.uic.rrc.arvados;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

/**
 * @author George Chlipala
 * @param <E>
 *
 */
public class ArvadosList<E> implements List<E> {

	private List<E> items = new ArrayList<E>();
	int totalSize = 0;
	int limit = 0;
	int offset = 0;
	
	/**
	 * 
	 */
	public ArvadosList() {

	}
	
	public ArvadosList(List<E> items, int totalSize, int limit, int offset) {
		this.items = items;
		this.totalSize = totalSize;
		this.limit = limit;
		this.offset = offset;
	}
	
	public int getTotalSize() { 
		return this.totalSize;
	}
	
	public int getOffset() { 
		return this.offset;
	}
	
	public int getLimit() { 
		return this.limit;
	}

	public List<E> getItems() { 
		return this.items;
	}
	
	void handleValue(String key, Event event, JsonParser parser) {
		switch (event) {
		case VALUE_NUMBER:
			if ( key.equalsIgnoreCase("limit") )
				this.limit = parser.getInt();
			else if ( key.equalsIgnoreCase("offset") )
				this.offset = parser.getInt();
			else if ( key.equalsIgnoreCase("items_available") ) 
				this.totalSize = parser.getInt();
			break;
		default:
			break;
		}
	}
	
	@Override
	public boolean add(E e) {
		return this.items.add(e);
	}

	@Override
	public void add(int index, E element) {
		this.items.add(index, element);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return this.items.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return this.items.addAll(index, c);
	}

	@Override
	public void clear() {
		this.items.clear();
	}

	@Override
	public boolean contains(Object o) {
		return this.items.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.items.containsAll(c);
	}

	@Override
	public E get(int index) {
		return this.items.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return this.items.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return this.items.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return this.items.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return this.items.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return this.items.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return this.items.listIterator(index);
	}

	@Override
	public boolean remove(Object o) {
		return this.items.remove(o);
	}

	@Override
	public E remove(int index) {
		return this.items.remove(index);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return this.items.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.items.retainAll(c);
	}

	@Override
	public E set(int index, E element) {
		return this.items.set(index, element);
	}

	@Override
	public int size() {
		return this.items.size();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return this.items.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return this.items.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.items.toArray(a);
	}
}
