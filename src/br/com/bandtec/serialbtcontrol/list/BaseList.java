//
// FPlayAndroid is distributed under the FreeBSD License
//
// Copyright (c) 2013-2014, Carlos Rafael Gimenes das Neves
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this
//    list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
// The views and conclusions contained in the software and documentation are those
// of the authors and should not be interpreted as representing official policies,
// either expressed or implied, of the FreeBSD Project.
//
// https://github.com/carlosrafaelgn/FPlayAndroid
//
package br.com.bandtec.serialbtcontrol.list;

import java.lang.reflect.Array;
import java.util.Arrays;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import br.com.bandtec.serialbtcontrol.ui.BaseItemView;
import br.com.bandtec.serialbtcontrol.ui.BgListView;
import br.com.bandtec.serialbtcontrol.ui.UI;
import br.com.bandtec.serialbtcontrol.util.ArraySorter;
import br.com.bandtec.serialbtcontrol.util.ArraySorter.Comparer;

//
//SINCE ALL CALLS MADE BY Player ARE MADE ON THE MAIN THREAD, THERE IS NO
//NEED TO SYNCHRONIZE THE ACCESS TO THE ITEMS
//
public class BaseList<E extends BaseItem> implements ListAdapter {
	private static final int LIST_DELTA = 32;
	
	protected static final int SELECTION_CHANGED = 0;
	protected static final int CONTENTS_CHANGED = 1;
	protected static final int LIST_CLEARED = 2;
	
	protected BgListView listObserver;
	protected DataSetObserver observer;
	protected final Object sync;
	protected E[] items;
	protected int count, current, firstSel, lastSel, originalSel, lastDeleted, modificationVersion;
	
	@SuppressWarnings("unchecked")
	public BaseList(Class<E> c) {
		this.sync = new Object();
		this.items = (E[])Array.newInstance(c, LIST_DELTA);
		this.current = -1;
		this.firstSel = -1;
		this.lastSel = -1;
		this.originalSel = -1;
		this.lastDeleted = -1;
	}
	
	private void setCapacity(int capacity) {
		if (capacity < count)
			return;
		
		if (capacity > items.length ||
			capacity <= (items.length - (2 * LIST_DELTA))) {
			capacity += LIST_DELTA;
		} else {
			return;
		}
		
		items = Arrays.copyOf(items, capacity);
	}
	
	public final void add(E item, int position) {
		if (firstSel != lastSel)
			setSelection(firstSel, firstSel, false, false);
		
		if (position < 0 || position >= count)
			position = count;
		
		setCapacity(count + 1);
		
		//synchronized (sync) {
			modificationVersion++;
			if (count != position)
				System.arraycopy(items, position, items, position + 1, count - position);
			items[position] = item;
			count++;
			if (current >= position)
				current++;
			if (current >= count)
				current = count - 1;
			if (firstSel >= position)
				firstSel++;
			if (lastSel >= position)
				lastSel++;
		//}
		
		notifyDataSetChanged(-1, CONTENTS_CHANGED);
	}
	
	public final void add(E[] items, int position, int count) {
		if (count <= 0)
			return;
		
		if (position < 0 || position >= this.count)
			position = this.count;
		if (count > items.length)
			count = items.length;
		
		setCapacity(this.count + count);
		
		//synchronized (sync) {
			modificationVersion++;
			if (this.count != position)
				System.arraycopy(this.items, position, this.items, position + count, this.count - position);
			System.arraycopy(items, 0, this.items, position, count);
			this.count += count;
			if (current >= position)
				current += count;
			if (firstSel >= position)
				firstSel += count;
			if (lastSel >= position)
				lastSel += count;
			if (originalSel >= position)
				originalSel += count;
		//}
		
		notifyDataSetChanged(-1, CONTENTS_CHANGED);
	}	
	
	public final void clear() {
		//synchronized (sync) {
			modificationVersion++;
			for (int i = items.length - 1; i >= 0; i--)
				items[i] = null;
			count = 0;
			current = -1;
			firstSel = -1;
			lastSel = -1;
			originalSel = -1;
			lastDeleted = -1;
		//}
		notifyDataSetChanged(-1, LIST_CLEARED);
	}
	
	public final boolean removeSelection() {
		int position = firstSel, count = lastSel - firstSel + 1;
		if (position < 0 || position >= this.count || count <= 0)
			return false;
		
		if ((position + count) > this.count)
			count = this.count - position;
		if (count <= 0)
			return false;
		
		if (firstSel != lastSel)
			setSelection(firstSel, firstSel, false, false);
		
		//synchronized (sync) {
			modificationVersion++;
			final int tot = position + count;
			for (int i = position; i < tot; i++)
				items[i] = null;
			
			System.arraycopy(items, position + count, items, position, (this.count - position - count));
			this.count -= count;
			lastDeleted = -1;
			if (current >= position && current < (position + count)) {
				lastDeleted = position;
				current = -1;
			} else if (current > position) {
				current -= count;
			}
			if (current >= this.count)
				current = -1;
			if (firstSel >= this.count)
				firstSel = this.count - 1;
			//if (firstSel >= position && firstSel < (position + count)) {
			//	firstSel = -1;
			//	lastSel = -1;
			//} else if (firstSel > position) {
			//	firstSel -= count;
			//	lastSel = firstSel;
			//}
			lastSel = firstSel;
			originalSel = firstSel;
		//}
		setCapacity(this.count);
		
		notifyDataSetChanged(originalSel, CONTENTS_CHANGED);
		
		return true;
	}
	
	public final void sort(Comparer<E> comparer) {
		//synchronized (sync) {
			modificationVersion++;
			ArraySorter.sort(items, 0, this.count, comparer);
			current = -1;
			firstSel = -1;
			lastSel = -1;
			originalSel = -1;
			lastDeleted = -1;
		//}
		notifyDataSetChanged(-1, CONTENTS_CHANGED);
	}
	
	public final void moveSelection(int to) {
		int from = firstSel, count = lastSel - firstSel + 1;
		if (from < 0 || to < 0 || count <= 0)
			return;
		if (count > this.count)
			count = this.count;
		if ((from + count) > this.count)
			count = this.count - from;
		if (count <= 0)
			return;
		if (to >= this.count)
			to = this.count - 1;
		if (to >= from && to < (from + count)) {
			if (originalSel != to) {
				originalSel = to;
				notifyDataSetChanged(-1, SELECTION_CHANGED);
			}
			return;
		}
		Object[] tmp = new Object[count];
		System.arraycopy(items, from, tmp, 0, count);
		//synchronized (sync) {
			modificationVersion++;
			final int delta;
			if (to < from) {
				delta = to - from;
				System.arraycopy(items, to, items, to + count, from - to);
				System.arraycopy(tmp, 0, items, to, count);
			} else {
				delta = to - (from + count) + 1;
				System.arraycopy(items, from + count, items, from, delta);
				System.arraycopy(tmp, 0, items, from + delta, count);
			}
			if (current < from && current >= to)
				current += count;
			else if (current >= from && current < (from + count))
				current += delta;
			else if (current > from && current <= to)
				current -= count;
			firstSel += delta;
			lastSel += delta;
			originalSel = to;
		//}
		notifyDataSetChanged(-1, CONTENTS_CHANGED);
	}
	
	public final int getSelection() {
		return originalSel;
	}
	
	public final int getCurrentPosition() {
		return current;
	}
	
	public final int getFirstSelectedPosition() {
		return firstSel;
	}
	
	public final int getLastSelectedPosition() {
		return lastSel;
	}
	
	public final boolean isSelected(int position) {
		return (position >= 0 && position >= firstSel && position <= lastSel);
	}
	
	public final void setSelection(int position, boolean byUserInteraction) {
		setSelection(position, position, position, true, byUserInteraction);
	}
	
	public final void setSelection(int from, int to, boolean notifyChanged, boolean byUserInteraction) {
		setSelection(from, to, -1, notifyChanged, byUserInteraction);
	}
	
	public final void setSelection(int from, int to, int original, boolean notifyChanged, boolean byUserInteraction) {
		int gotoPosition = -1;
		firstSel = -1;
		lastSel = -1;
		if (from >= 0 && to >= 0) {
			if (from >= count)
				from = count - 1;
			if (to >= count)
				to = count - 1;
			if (from > to) {
				firstSel = to;
				lastSel = from;
			} else {
				firstSel = from;
				lastSel = to;
			}
			if (from == to && ((original < 0) || (from < 0))) {
				originalSel = from;
				gotoPosition = from;
			}
		} else {
			originalSel = -1;
		}
		if (original >= 0 && original >= firstSel && original <= lastSel) {
			originalSel = original;
			gotoPosition = original;
		}
		if (notifyChanged)
			notifyDataSetChanged(byUserInteraction ? -1 : gotoPosition, SELECTION_CHANGED);
	}
	
	@Override
	public final int getCount() {
		return count;
	}
	
	@Override
	public final Object getItem(int position) {
		return items[position];
	}
	
	public final E getItemT(int position) {
		return items[position];
	}
	
	@Override
	public final long getItemId(int position) {
		return items[position].id;
	}
	
	@Override
	public final boolean areAllItemsEnabled() {
		return true;
	}
	
	@Override
	public final int getItemViewType(int position) {
		return 0;
	}
	
	@Override
	public final int getViewTypeCount() {
		return 1;
	}
	
	@Override
	public final boolean hasStableIds() {
		return true;
	}
	
	@Override
	public final boolean isEnabled(int position) {
		return true;
	}
	
	@Override
	public final boolean isEmpty() {
		return (count == 0);
	}
	
	public final void setObserver(BgListView list) {
		//if (listObserver != null)
		//	listObserver.setAdapter(null);
		listObserver = list;
		if (list != null)
			list.setAdapter(this);
	}
	
	@Override
	public final void registerDataSetObserver(DataSetObserver observer) {
		//we only need to support one observer
		this.observer = observer;
	}
	
	@Override
	public final void unregisterDataSetObserver(DataSetObserver observer) {
		//we only need to support one observer
		this.observer = null;
	}
	
	protected void notifyDataSetChanged(int gotoPosition, int whatHappened) {
		if (observer != null)
			observer.onChanged();
		if (listObserver != null && gotoPosition >= 0)
			listObserver.centerItem(gotoPosition, false);
	}
	
	protected final int getItemState(int position) {
		return ((position == current) ? UI.STATE_CURRENT : 0) | ((position == originalSel) ? UI.STATE_SELECTED :
			((position >= firstSel && position <= lastSel) ? UI.STATE_MULTISELECTED : 0));
	}
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		BaseItemView view = ((convertView == null) ? new BaseItemView(listObserver.getContext()) : (BaseItemView)convertView);
		view.setItemState(items[position].toString(), getItemState(position) | (items[position].highlight ? UI.STATE_CURRENT : 0));
		return view;
	}
}
