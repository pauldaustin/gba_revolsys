package com.revolsys.swing.map.layer;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.slf4j.LoggerFactory;

import com.revolsys.collection.Parent;
import com.revolsys.collection.list.Lists;
import com.revolsys.geometry.model.BoundingBox;
import com.revolsys.geometry.model.GeometryFactory;
import com.revolsys.geometry.model.impl.BoundingBoxDoubleGf;
import com.revolsys.io.FileUtil;
import com.revolsys.io.IoFactory;
import com.revolsys.io.PathUtil;
import com.revolsys.io.PathName;
import com.revolsys.io.file.Paths;
import com.revolsys.io.map.MapObjectFactory;
import com.revolsys.raster.GeoreferencedImageFactory;
import com.revolsys.record.io.RecordReaderFactory;
import com.revolsys.record.io.format.json.Json;
import com.revolsys.spring.resource.FileSystemResource;
import com.revolsys.spring.resource.Resource;
import com.revolsys.spring.resource.SpringUtil;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.map.action.AddFileLayerAction;
import com.revolsys.swing.map.layer.raster.GeoreferencedImageLayer;
import com.revolsys.swing.map.layer.record.FileRecordLayer;
import com.revolsys.swing.map.layer.record.renderer.GeometryStyleRenderer;
import com.revolsys.swing.map.layer.record.style.GeometryStyle;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.menu.Menus;
import com.revolsys.util.Property;

public class LayerGroup extends AbstractLayer implements Parent<Layer>, Iterable<Layer> {

  static {
    final MenuFactory menu = MenuFactory.getMenu(LayerGroup.class);
    menu.addGroup(0, "group");
    Menus.<LayerGroup> addMenuItem(menu, "group", "Add Group", "folder_add",
      LayerGroup::addLayerGroup);
    menu.addMenuItem("group", new AddFileLayerAction());
  }

  private static Layer getLayer(LayerGroup group, final String name) {
    for (final String path : PathUtil.getPathElements(PathUtil.getPath(name))) {
      final Layer layer = getLayerByName(group, path);
      if (layer instanceof LayerGroup) {
        group = (LayerGroup)layer;
      } else {
        return null;
      }
    }

    if (group != null) {
      final String layerName = PathUtil.getName(name);

      return getLayerByName(group, layerName);
    }
    return null;
  }

  private static Layer getLayerByName(final LayerGroup group, final String layerName) {
    for (final Layer layer : group.getLayers()) {
      if (layer.getName().equals(layerName)) {

        return layer;
      }
    }
    return null;
  }

  public static LayerGroup newLayer(final Map<String, Object> properties) {
    final LayerGroup layerGroup = new LayerGroup();
    layerGroup.loadLayers(properties);
    return layerGroup;
  }

  private boolean singleLayerVisible = false;

  private boolean deleted = false;

  private List<Layer> layers = new ArrayList<Layer>();

  public LayerGroup() {
    this(null);
  }

  public LayerGroup(final String name) {
    super(name);
    setType("layerGroup");
    setRenderer(new LayerGroupRenderer(this));
    setInitialized(true);
    setOpen(true);
  }

  protected <V extends Layer> void addDescendants(final List<V> layers, final Class<V> layerClass) {
    addLayers(layers, layerClass);
    for (final LayerGroup layerGroup : getLayerGroups()) {
      layerGroup.addDescendants(layers, layerClass);
    }
  }

  public void addLayer(final int index, final Layer layer) {
    synchronized (this.layers) {
      if (layer != null && !this.layers.contains(layer)) {
        final String name = layer.getName();
        String newName = name;
        int i = 1;
        while (hasLayerWithSameName(layer, newName)) {
          newName = name + i;
          i++;
        }
        layer.setName(newName);
        this.layers.add(index, layer);
        layer.setLayerGroup(this);
        initialize(layer);
        fireIndexedPropertyChange("layers", index, null, layer);
      }
    }
  }

  public boolean addLayer(final Layer layer) {
    synchronized (this.layers) {
      if (layer == null || this.layers.contains(layer)) {
        return false;
      } else {
        final int index = this.layers.size();
        addLayer(index, layer);
        return true;
      }
    }
  }

  public LayerGroup addLayerGroup() {
    final String name = JOptionPane.showInputDialog(SwingUtil.getActiveWindow(),
      "Enter the name of the new Layer Group.", "Add Layer Group", JOptionPane.PLAIN_MESSAGE);
    if (Property.hasValue(name)) {
      final LayerGroup newGroup = new LayerGroup(name);
      addLayer(newGroup);
      return newGroup;
    } else {
      return null;
    }
  }

  public LayerGroup addLayerGroup(final int index, final String name) {
    synchronized (this.layers) {
      final Layer layer = getLayer(name);
      if (layer == null) {
        final LayerGroup group = new LayerGroup(name);
        addLayer(index, group);
        return group;
      }
      if (layer instanceof LayerGroup) {
        return (LayerGroup)layer;
      } else {
        throw new IllegalArgumentException("Layer exists with name " + name);
      }
    }
  }

  public LayerGroup addLayerGroup(final String name) {
    synchronized (this.layers) {
      final Layer layer = getLayer(name);
      if (layer == null) {
        final LayerGroup group = new LayerGroup(name);
        addLayer(group);
        return group;
      }
      if (layer instanceof LayerGroup) {
        return (LayerGroup)layer;
      } else {
        throw new IllegalArgumentException("Layer exists with name " + name);
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected <V extends Layer> void addLayers(final List<V> layers, final Class<V> layerClass) {
    for (final Layer layer : this.layers) {
      if (layerClass.isAssignableFrom(layer.getClass())) {
        layers.add((V)layer);
      }
    }
  }

  public void addPath(final List<Layer> path) {
    final LayerGroup layerGroup = getLayerGroup();
    if (layerGroup != null) {
      layerGroup.addPath(path);
    }
    path.add(this);
  }

  protected <V extends Layer> void addVisibleDescendants(final List<V> layers,
    final Class<V> layerClass, final double scale) {
    if (isVisible(scale)) {
      addVisibleLayers(layers, layerClass, scale);
      for (final LayerGroup layerGroup : getLayerGroups()) {
        layerGroup.addVisibleDescendants(layers, layerClass, scale);
      }
    }
  }

  @SuppressWarnings("unchecked")
  protected <V extends Layer> void addVisibleLayers(final List<V> layers, final Class<V> layerClass,
    final double scale) {
    for (final Layer layer : this.layers) {
      if (layer.isVisible(scale)) {
        if (layerClass.isAssignableFrom(layer.getClass())) {
          layers.add((V)layer);
        }
      }
    }
  }

  public void clear() {
    for (final Layer layer : new ArrayList<Layer>(this.layers)) {
      layer.delete();
    }
    this.layers = new ArrayList<Layer>();

  }

  public boolean containsLayer(final Layer layer) {
    return this.layers.contains(layer);
  }

  @Override
  public void delete() {
    this.deleted = true;
    synchronized (this.layers) {
      for (final Iterator<Layer> iterator = this.layers.iterator(); iterator.hasNext();) {
        final Layer layer = iterator.next();
        iterator.remove();
        layer.setLayerGroup(null);
        Property.removeListener(layer, this);
        layer.delete();
      }
      super.delete();
      this.layers.clear();
    }
  }

  @Override
  public BoundingBox getBoundingBox() {
    final GeometryFactory geometryFactory = getGeometryFactory();
    BoundingBox boudingBox = new BoundingBoxDoubleGf(geometryFactory);
    for (final Layer layer : this) {
      final BoundingBox layerBoundingBox = layer.getBoundingBox();
      if (!layerBoundingBox.isEmpty()) {
        boudingBox = boudingBox.expandToInclude(layerBoundingBox);
      }
    }
    return boudingBox;
  }

  @Override
  public BoundingBox getBoundingBox(final boolean visibleLayersOnly) {
    final GeometryFactory geometryFactory = getGeometryFactory();
    BoundingBox boudingBox = new BoundingBoxDoubleGf(geometryFactory);
    if (isExists() && (!visibleLayersOnly || isVisible())) {
      for (final Layer layer : this) {
        if (layer.isExists() && (!visibleLayersOnly || layer.isVisible())) {
          final BoundingBox layerBoundingBox = layer.getBoundingBox(visibleLayersOnly);
          if (!layerBoundingBox.isEmpty()) {
            boudingBox = boudingBox.expandToInclude(layerBoundingBox);
          }
        }
      }
    }
    return boudingBox;
  }

  @Override
  public List<Layer> getChildren() {
    return getLayers();
  }

  public <V extends Layer> List<V> getDescenants(final Class<V> layerClass) {
    final List<V> layers = new ArrayList<V>();
    addDescendants(layers, layerClass);
    return layers;
  }

  @Override
  public java.nio.file.Path getDirectory() {
    final LayerGroup layerGroup = getLayerGroup();
    if (layerGroup != null) {
      final java.nio.file.Path parentDirectory = layerGroup.getDirectory();
      if (parentDirectory != null) {
        final java.nio.file.Path layerDirectory = getGroupSettingsDirectory(parentDirectory);
        return layerDirectory;
      }
    }
    return null;
  }

  @Override
  public GeometryFactory getGeometryFactory() {
    final LayerGroup layerGroup = getLayerGroup();
    final GeometryFactory geometryFactory = super.getGeometryFactory();
    if (geometryFactory == null && layerGroup != null) {
      return layerGroup.getGeometryFactory();
    } else {
      return geometryFactory;
    }
  }

  protected java.nio.file.Path getGroupSettingsDirectory(final java.nio.file.Path directory) {
    final String name = getName();
    final String groupDirectoryName = FileUtil.getSafeFileName(name);
    final java.nio.file.Path groupDirectory = Paths.getDirectoryPath(directory, groupDirectoryName);
    return groupDirectory;
  }

  @Override
  public long getId() {
    // TODO Auto-generated method stub
    return 0;
  }

  @SuppressWarnings("unchecked")
  public <V extends Layer> V getLayer(final int i) {
    if (i < this.layers.size()) {
      return (V)this.layers.get(i);
    } else {
      return null;
    }
  }

  public <V extends Layer> V getLayer(final PathName name) {
    return getLayer(name.getPath());
  }

  @SuppressWarnings("unchecked")
  public <V extends Layer> V getLayer(final String name) {
    return (V)getLayer(this, name);
  }

  public Layer getLayerByPath(final List<String> path) {
    if (path.isEmpty()) {
      return this;
    } else {
      final Layer layer = getLayer(path.get(0));
      if (path.size() == 1) {
        return layer;
      } else {
        if (layer instanceof LayerGroup) {
          final LayerGroup layerGroup = (LayerGroup)layer;
          return layerGroup.getLayerByPath(path.subList(1, path.size()));
        }
        return null;
      }
    }
  }

  public Layer getLayerByPath(final String layerPath) {
    final List<String> path = Lists.split(layerPath.replaceAll("^\\s*/+\\s*", ""), "(\\s*/+\\s*)+");
    return getLayerByPath(path);
  }

  public int getLayerCount() {
    return this.layers.size();
  }

  public List<LayerGroup> getLayerGroups() {
    final List<LayerGroup> layerGroups = new ArrayList<LayerGroup>();
    for (final Layer layer : this.layers) {
      if (layer instanceof LayerGroup) {
        final LayerGroup layerGroup = (LayerGroup)layer;
        layerGroups.add(layerGroup);
      }
    }
    return layerGroups;
  }

  public List<Layer> getLayers() {
    return new ArrayList<>(this.layers);
  }

  public <V extends Layer> List<V> getLayers(final Class<V> layerClass) {
    final List<V> layers = new ArrayList<V>();
    addLayers(layers, layerClass);
    return layers;
  }

  public <V extends Layer> List<V> getLayers(final List<PathName> names) {
    final List<V> layers = new ArrayList<V>();
    for (final PathName name : names) {
      final V layer = getLayer(name);
      if (layer != null) {
        layers.add(layer);
      }
    }
    return layers;
  }

  public <V extends Layer> List<V> getLayers(final PathName... names) {
    return getLayers(Arrays.asList(names));
  }

  @Override
  public BoundingBox getSelectedBoundingBox() {
    BoundingBox boundingBox = super.getSelectedBoundingBox();
    if (isExists() && isVisible()) {
      for (final Layer layer : this) {
        final BoundingBox layerBoundingBox = layer.getSelectedBoundingBox();
        boundingBox = boundingBox.expandToInclude(layerBoundingBox);
      }
    }
    return boundingBox;
  }

  @Override
  protected String getSettingsFileName() {
    return "rgLayerGroup.rgobject";
  }

  public <V extends Layer> List<V> getVisibleDescendants(final Class<V> layerClass,
    final double scale) {
    final List<V> layers = new ArrayList<V>();
    addVisibleDescendants(layers, layerClass, scale);
    return layers;
  }

  public boolean hasLayerWithSameName(final Layer layer, final String name) {
    for (final Layer otherLayer : this.layers) {
      if (layer != otherLayer) {
        final String layerName = otherLayer.getName();
        if (name.equals(layerName)) {
          return true;
        }
      }
    }
    return false;
  }

  public int indexOf(final Layer layer) {
    return this.layers.indexOf(layer);
  }

  public void initialize(final Layer layer) {
    if (getProject() != null) {
      LayerInitializer.initialize(layer);
      if (layer instanceof LayerGroup) {
        final LayerGroup layerGroup = (LayerGroup)layer;
        for (final Layer child : layerGroup) {
          initialize(child);
        }
      }
    }
  }

  @Override
  public boolean isDeleted() {
    return this.deleted;
  }

  public boolean isEmpty() {
    return this.layers.isEmpty();
  }

  @Override
  public boolean isHasSelectedRecords() {
    for (final Layer layer : getLayers()) {
      if (layer.isHasSelectedRecords()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean isQueryable() {
    return false;
  }

  @Override
  public boolean isQuerySupported() {
    return false;
  }

  public boolean isSingleLayerVisible() {
    return this.singleLayerVisible;
  }

  @Override
  public boolean isZoomToLayerEnabled() {
    if (!getBoundingBox().isEmpty()) {
      return true;
    }
    return false;
  }

  @Override
  public Iterator<Layer> iterator() {
    return getLayers().iterator();
  }

  protected Layer loadLayer(final File file) {
    final Resource oldResource = SpringUtil
      .setBaseResource(new FileSystemResource(file.getParentFile()));

    try {
      final Map<String, Object> properties = Json.toMap(file);
      final Layer layer = MapObjectFactory.toObject(properties);
      if (layer != null) {
        addLayer(layer);
      }
      return layer;
    } catch (final Throwable t) {
      LoggerFactory.getLogger(getClass()).error("Cannot load layer from " + file, t);
      return null;
    } finally {
      SpringUtil.setBaseResource(oldResource);
    }
  }

  @SuppressWarnings("unchecked")
  protected void loadLayers(final Map<String, Object> properties) {
    final List<String> layerFiles = (List<String>)properties.remove("layers");
    setProperties(properties);
    if (layerFiles != null) {
      for (String fileName : layerFiles) {
        if (!fileName.endsWith("rgobject")) {
          fileName += "/rgLayerGroup.rgobject";
        }
        final Resource childResource = SpringUtil.getBaseResource(fileName);
        if (childResource.exists()) {
          final Object object = MapObjectFactory.toObject(childResource);
          if (object instanceof Layer) {
            final Layer layer = (Layer)object;
            addLayer(layer);
          } else if (object != null) {
            LoggerFactory.getLogger(LayerGroup.class)
              .error("Unexpected object type " + object.getClass() + " in " + childResource);
          }
        } else {
          LoggerFactory.getLogger(LayerGroup.class).error("Cannot find " + childResource);
        }
      }
    }
  }

  public void openFile(final File file) {
    final String extension = FileUtil.getFileNameExtension(file);
    if ("rgobject".equals(extension)) {
      loadLayer(file);
    } else {
      final URL url = FileUtil.toUrl(file);
      openFile(url);
    }
  }

  public int openFile(final int index, final File file) {
    final String extension = FileUtil.getFileNameExtension(file);
    if ("rgobject".equals(extension)) {
      loadLayer(file);
      // TODO index
      return index;
    } else {
      final URL url = FileUtil.toUrl(file);
      return openFile(index, url);
    }
  }

  public int openFile(int index, final URL url) {
    final String urlString = url.toString();
    final Map<String, Object> properties = new HashMap<>();
    properties.put("url", urlString);
    String name = FileUtil.getFileName(urlString);
    name = FileUtil.fromSafeName(name);
    properties.put("name", name);
    Layer layer;
    if (IoFactory.hasFactory(GeoreferencedImageFactory.class, url)) {
      layer = new GeoreferencedImageLayer(properties);
    } else if (IoFactory.hasFactory(RecordReaderFactory.class, url)) {
      final FileRecordLayer recordLayer = new FileRecordLayer(properties);
      final GeometryStyleRenderer renderer = recordLayer.getRenderer();
      renderer.setStyle(GeometryStyle.newStyle());
      layer = recordLayer;
    } else {
      layer = null;
    }
    if (layer != null) {
      layer.setProperty("showTableView", true);
      if (index == -1) {
        addLayer(layer);
      } else {
        addLayer(index++, layer);
      }
    }
    return index;
  }

  public void openFile(final URL url) {
    openFile(-1, url);
  }

  public void openFiles(int index, final List<File> files) {
    for (final File file : files) {
      index = openFile(index, file);
    }
  }

  public void openFiles(final List<File> files) {
    openFiles(-1, files);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    super.propertyChange(event);
    final Object source = event.getSource();
    final String propertyName = event.getPropertyName();
    if ("visible".equals(propertyName)) {
      if (isSingleLayerVisible()) {
        final boolean visible = (Boolean)event.getNewValue();
        if (visible) {
          for (final Layer layer : getLayers()) {
            if (layer != source) {
              layer.setVisible(false);
            }
          }
        }
      }
    }
  }

  @Override
  protected void refreshAllDo() {
    for (final Layer layer : this.layers) {
      layer.refreshAll();
    }
  }

  @Override
  protected void refreshDo() {
  }

  public Layer removeLayer(final int index) {
    synchronized (this.layers) {
      final Layer layer = this.layers.remove(index);
      fireIndexedPropertyChange("layers", index, layer, null);
      Property.removeListener(layer, this);
      if (layer.getLayerGroup() == this) {
        layer.setLayerGroup(null);
      }
      return layer;
    }
  }

  public boolean removeLayer(final Object o) {
    synchronized (this.layers) {
      final int index = this.layers.indexOf(o);
      if (index < 0) {
        return false;
      } else {
        removeLayer(index);
        return true;
      }
    }
  }

  public boolean saveAllSettings(final java.nio.file.Path directory) {
    if (directory == null) {
      return false;
    } else {
      boolean saved = true;
      final java.nio.file.Path groupDirectory = getGroupSettingsDirectory(directory);
      if (canSaveSettings(directory)) {
        saved &= saveSettings(directory);
        for (final Layer layer : this) {
          if (layer instanceof LayerGroup) {
            final LayerGroup layerGroup = (LayerGroup)layer;
            saved &= layerGroup.saveAllSettings(groupDirectory);
          } else {
            saved &= layer.saveSettings(groupDirectory);
          }
        }
      } else {
        saved = false;
      }
      return saved;
    }
  }

  @Override
  public boolean saveChanges() {
    boolean saved = super.saveChanges();
    for (final Layer layer : this) {
      saved &= layer.saveChanges();
    }
    return saved;
  }

  @Override
  protected boolean saveSettingsDo(final java.nio.file.Path directory) {
    final java.nio.file.Path groupDirectory = getGroupSettingsDirectory(directory);
    return super.saveSettingsDo(groupDirectory);
  }

  public void setSingleLayerVisible(final boolean singleLayerVisible) {
    this.singleLayerVisible = singleLayerVisible;
  }

  public void sort() {
    synchronized (this.layers) {
      Collections.sort(this.layers);
    }
  }

  @Override
  public Map<String, Object> toMap() {
    final Map<String, Object> map = super.toMap();
    map.remove("querySupported");
    map.remove("queryable");
    map.remove("editable");
    map.remove("selectable");
    map.remove("selectSupported");
    map.put("singleLayerVisible", isSingleLayerVisible());

    final List<String> layerFiles = new ArrayList<String>();
    final List<Layer> layers = getLayers();
    for (final Layer layer : layers) {
      final String layerName = layer.getName();
      String layerFileName = FileUtil.getSafeFileName(layerName);
      if (layer instanceof LayerGroup) {
      } else {
        layerFileName += ".rgobject";
      }
      layerFiles.add(layerFileName);

    }
    map.put("layers", layerFiles);
    return map;
  }
}
