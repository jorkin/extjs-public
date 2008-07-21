/*
 * Ext GWT - Ext for GWT
 * Copyright(c) 2007, 2008, Ext JS, LLC.
 * licensing@extjs.com
 * 
 * http://extjs.com/license
 */
package com.extjs.gxt.ui.client.store;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.extjs.gxt.ui.client.data.ChangeEvent;
import com.extjs.gxt.ui.client.data.ChangeEventSource;
import com.extjs.gxt.ui.client.data.ChangeListener;
import com.extjs.gxt.ui.client.data.DefaultModelComparer;
import com.extjs.gxt.ui.client.data.ModelComparer;
import com.extjs.gxt.ui.client.data.ModelData;
import com.extjs.gxt.ui.client.data.SortInfo;
import com.extjs.gxt.ui.client.event.BaseObservable;
import com.extjs.gxt.ui.client.store.Record.RecordUpdate;
import com.extjs.gxt.ui.client.widget.DataView;
import com.extjs.gxt.ui.client.widget.form.ComboBox;

/**
 * The store class encapsulates a client side cache of {@link ModelData} objects
 * which provide input data for components such as the {@link ComboBox} and
 * {@link DataView DataView}.
 * 
 * <dl>
 * <dt><b>Events:</b></dt>
 * 
 * <dd><b>Filter</b> : StoreEvent(store)<br>
 * <div>Fires when filters are applied and removed from the store.</div>
 * <ul>
 * <li>store : this</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>Update</b> : StoreEvent(store, model, record)<br>
 * <div>Fires when a model has been updated via its record.</div>
 * <ul>
 * <li>store : this</li>
 * <li>model : the model that was updated</li>
 * <li>record : the record that was updated</li>
 * <li>operation : the update operation being performed.</li>
 * </ul>
 * </dd>
 * 
 * <dd><b>Clear</b> : StoreEvent(store)<br>
 * <div>Fires when the data cache has been cleared.</div>
 * <ul>
 * <li>store : this</li>
 * </ul>
 * </dd>
 * 
 * </dl>
 * 
 * @param <M> the model data type
 */
public abstract class Store<M extends ModelData> extends BaseObservable {

  /**
   * BeforeDataChanged event type (value is 1100).
   */
  public static final int BeforeDataChanged = 1100;

  /**
   * DataChanged event type (value is 1102).
   */
  public static final int DataChanged = 1102;

  /**
   * Filter event type (value is 1105).
   */
  public static final int Filter = 1105;

  /**
   * Filter event type (value is 1107).
   */
  public static final int Sort = 1107;

  /**
   * Add event type (value is 1110).
   */
  public static final int Add = 1110;

  /**
   * Remove event type (value is 1120).
   */
  public static final int Remove = 1120;

  /**
   * Update event type (value is 1130).
   */
  public static final int Update = 1130;

  /**
   * Clear event type (value is 1140).
   */
  public static final int Clear = 1140;

  protected List<M> all = new ArrayList<M>();
  protected Map<M, Record> recordMap = new HashMap<M, Record>();
  protected List<M> filtered;
  protected List<Record> modified = new ArrayList<Record>();
  protected SortInfo sortInfo = new SortInfo();
  protected StoreSorter storeSorter;
  protected String filterProperty;

  private List<M> snapshot;
  private List<StoreFilter> filters;
  protected boolean filtersEnabled;
  private ModelComparer<M> comparer = DefaultModelComparer.DFFAULT;
  private ChangeListener changeListener;
  private boolean monitorChanges;

  /**
   * Adds a filter to the store.
   * 
   * @param filter the store filter to add
   */
  public void addFilter(StoreFilter filter) {
    if (filters == null) {
      filters = new ArrayList<StoreFilter>();
    }
    filters.add(filter);
  }

  /**
   * Adds a store listener.
   * 
   * @param listener the listener to add
   */
  public void addStoreListener(StoreListener listener) {
    StoreTypedListener tl = new StoreTypedListener(listener);
    addListener(Filter, tl);
    addListener(Sort, tl);
    addListener(BeforeDataChanged, tl);
    addListener(DataChanged, tl);
    addListener(Add, tl);
    addListener(Remove, tl);
    addListener(Update, tl);
    addListener(Clear, tl);
  }

  /**
   * Applies the current filters to the store.
   * 
   * @param property the optional active property
   */
  public void applyFilters(String property) {
    if (filters != null && filters.size() == 0) {
      return;
    }
    if (!filtersEnabled) {
      snapshot = all;
    }
    filtersEnabled = true;
    filtered = new ArrayList<M>();
    for (M items : snapshot) {
      if (!isFiltered(items, property)) {
        filtered.add(items);
      }
    }
    all = filtered;

    fireEvent(Filter, createStoreEvent());

    if (storeSorter != null) {
      applySort(false);
    }
  }

  /**
   * Revert to a view of this store with no filtering applied.
   */
  public void clearFilters() {
    if (isFiltered()) {
      filtersEnabled = false;
      all = snapshot;
      fireEvent(Filter, createStoreEvent());
    }
  }

  /**
   * Commit all items with outstanding changes. To handle updates for changes,
   * subscribe to the Store's <i>Update</i> event, and perform updating when
   * the operation parameter is {@link RecordUpdate#COMMIT}.
   */
  public void commitChanges() {
    for (Record r : modified) {
      r.commit(false);
    }
    modified = new ArrayList<Record>();
  }

  /**
   * Returns true if the item is in this store.
   * 
   * @param item the item
   * @return true if container
   */
  public boolean contains(ModelData item) {
    return all.contains(item);
  }

  /**
   * Filters the store using the given property.
   * 
   * @param property the property to filter by
   */
  public void filter(String property) {
    filterProperty = property;
    applyFilters(property);
  }

  /**
   * Returns the matching model in the cache using the model comparer to test
   * for equality.
   * 
   * @param model the model
   * @return the matching model or null if no match
   */
  public M findModel(M model) {
    for (M m : all) {
      if (comparer.equals(m, model)) {
        return m;
      }
    }
    return null;
  }

  /**
   * Returns the store's filters.
   * 
   * @return the filters
   */
  public List<StoreFilter> getFilters() {
    return filters;
  }

  /**
   * Returns the comparer used to comapare model instances.
   * 
   * @return the comparer
   */
  public ModelComparer<M> getModelComparer() {
    return comparer;
  }

  /**
   * Returns the store's models.
   * 
   * @return the items
   */
  public List<M> getModels() {
    return new ArrayList<M>(all);
  }

  /**
   * Gets all records modified since the last commit. Modified records are
   * persisted across load operations (e.g., during paging).
   * 
   * @return a list of modified records
   */
  public List<Record> getModifiedRecords() {
    return new ArrayList<Record>(modified);
  }

  /**
   * Returns the record instance for the item. Records are created on-demand and
   * are cleared after a stores modifications are accepted or rejected.
   * 
   * @param item the item
   * @return the record for the item
   */
  public Record getRecord(M item) {
    Record record = recordMap.get(item);
    if (record == null) {
      record = new Record(item);
      record.join(this);
      recordMap.put(item, record);
    }
    return record;
  }

  /**
   * Returns the store sorter.
   * 
   * @return the store storter
   */
  public StoreSorter getStoreSorter() {
    return storeSorter;
  }

  /**
   * Returns true if this store is currently filtered.
   * 
   * @return true if the store is filtered
   */
  public boolean isFiltered() {
    return filtersEnabled;
  }

  /**
   * Returns true if the store is monitoring changes.
   * 
   * @return the montitro changes state
   */
  public boolean isMonitorChanges() {
    return monitorChanges;
  }

  /**
   * Cancel outstanding changes on all changed records.
   */
  public void rejectChanges() {
    for (Record r : modified) {
      r.reject(false);
    }
    modified.clear();
  }

  /**
   * Remove all items from the store and fires the <i>Clear</i> event.
   */
  public void removeAll() {
    for (M m : all) {
      unregisterModel(m);
    }
    all.clear();
    modified.clear();
    recordMap.clear();
    fireEvent(Clear, createStoreEvent());
  }

  /**
   * Removes a previously added filter.
   * 
   * @param filter the filter to remove
   */
  public void removeFilter(StoreFilter filter) {
    if (filters != null) {
      filters.remove(filter);
    }
  }

  /**
   * Removes a store listener.
   * 
   * @param listener the store listener to remove
   */
  public void removeStoreListener(StoreListener listener) {
    removeListener(Sort, listener);
    removeListener(Filter, listener);
    removeListener(BeforeDataChanged, listener);
    removeListener(DataChanged, listener);
    removeListener(Add, listener);
    removeListener(Remove, listener);
    removeListener(Update, listener);
    removeListener(Clear, listener);
  }

  /**
   * Sets the comparer to be used when comparing model instances.
   * 
   * @param comparer the comparer
   */
  public void setModelComparer(ModelComparer<M> comparer) {
    this.comparer = comparer;
  }

  /**
   * Sets whether the store should listen to change events on its children
   * (defaults to false). This method should be called prior to any models being
   * added to the store when monitoring changes. Only model instances which
   * implement {@link ChangeEventSource} may be monitored.
   * 
   * @param monitorChanges true to monitor changes
   */
  public void setMonitorChanges(boolean monitorChanges) {
    if (changeListener == null) {
      changeListener = new ChangeListener() {

        public void modelChanged(ChangeEvent event) {
          onModelChange(event);
        }

      };
    }
    this.monitorChanges = monitorChanges;
  }

  /**
   * Sets the store's sorter.
   * 
   * @param storeSorter the sorter
   */
  public void setStoreSorter(StoreSorter storeSorter) {
    this.storeSorter = storeSorter;
  }

  /**
   * Notifies the store that the model has been updated and fires the <i>Update</i>
   * event.
   * 
   * @param model the updated model
   */
  public void update(M model) {
    M m = findModel(model);
    if (m != null) {
      if (m != model) {
        swapModelInstance(m, model);
      }
      StoreEvent<M> evt = createStoreEvent();
      evt.model = model;
      fireEvent(Update, evt);
    }
  }

  protected void afterCommit(Record record) {
    modified.remove(record);
    fireStoreEvent(Update, RecordUpdate.COMMIT, record);
  }

  protected void afterEdit(Record record) {
    if (!modified.contains(record)) {
      modified.add(record);
    }
    fireStoreEvent(Update, RecordUpdate.EDIT, record);
  }

  protected void afterReject(Record record) {
    modified.remove(record);
    fireStoreEvent(Update, RecordUpdate.REJECT, record);
  }

  protected void applySort(boolean supressEvent) {

  }

  protected StoreEvent createStoreEvent() {
    return new StoreEvent(this);
  }

  protected void fireStoreEvent(int type, RecordUpdate operation, Record record) {
    StoreEvent evt = createStoreEvent();
    evt.operation = operation;
    evt.record = record;
    evt.model = record.getModel();
    fireEvent(type, evt);
  }

  protected boolean isFiltered(ModelData record, String property) {
    if (filters != null) {
      for (StoreFilter filter : filters) {
        boolean result = filter.select(this, record, record, property);
        if (!result) {
          return true;
        }
      }
    }
    return false;
  }

  protected void onModelChange(ChangeEvent ce) {
    if (ce.type == ChangeEventSource.Update) {
      update((M) ce.source);
    }
  }

  /**
   * Subclasses must register any model instance being inserted into the store.
   * 
   * @param model the model
   */
  protected void registerModel(M model) {
    if (monitorChanges && model instanceof ChangeEventSource) {
      ((ChangeEventSource) model).addChangeListener(changeListener);
    }
  }

  /**
   * Subclasses must unregister any model instance being removed from the store.
   * 
   * @param model the model
   */
  protected void unregisterModel(M model) {
    if (monitorChanges && model instanceof ChangeEventSource) {
      ((ChangeEventSource) model).removeChangeListener(changeListener);
    }
    if (recordMap.containsKey(model)) {
      recordMap.remove(model);
    }
  }

  protected void swapModelInstance(M oldModel, M newModel) {
    int index = all.indexOf(oldModel);
    if (index != -1) {
      all.remove(oldModel);
      all.add(index, newModel);
      unregisterModel(oldModel);
      registerModel(newModel);
    }
  }

}
