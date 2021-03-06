/**
 * ScatterMesh.java
 *
 * Copyright (c) 2013-2016, F(X)yz
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of F(X)yz, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL F(X)yz BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

package org.fxyz3d.shapes.primitives;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.TriangleMesh;
import org.fxyz3d.geometry.Point3D;
import org.fxyz3d.scene.paint.Palette.ColorPalette;
import org.fxyz3d.scene.paint.Patterns;
import org.fxyz3d.shapes.primitives.helper.MeshHelper;
import org.fxyz3d.shapes.primitives.helper.TextureMode;

/**
 *
 * @author José Pereda 
 */
public class ScatterMesh extends Group implements TextureMode {
    
    private final static List<Point3D> DEFAULT_SCATTER_DATA = Arrays.asList(new Point3D(0f,0f,0f),
            new Point3D(1f,1f,1f), new Point3D(2f,2f,2f));
    private final static double DEFAULT_HEIGHT = 0.1d;
    private final static int DEFAULT_LEVEL = 0;
    private final static boolean DEFAULT_JOIN_SEGMENTS = true;
    
    private ObservableList<TexturedMesh> meshes=null;
    
    public ScatterMesh(){
        this(DEFAULT_SCATTER_DATA,DEFAULT_JOIN_SEGMENTS,DEFAULT_HEIGHT,DEFAULT_LEVEL);
    }
    
    public ScatterMesh(List<Point3D> scatterData){
        this(scatterData,DEFAULT_JOIN_SEGMENTS,DEFAULT_HEIGHT,DEFAULT_LEVEL);
    }
    
    
    public ScatterMesh(List<Point3D> scatterData, double height){
        this(scatterData,DEFAULT_JOIN_SEGMENTS,height,DEFAULT_LEVEL);
    }
    
    public ScatterMesh(List<Point3D> scatterData, boolean joinSegments, double height, int level){
        setScatterData(scatterData);
        setJoinSegments(joinSegments);
        setHeight(height);
        setLevel(level);
        
        updateMesh();
    }
    private final ObjectProperty<List<Point3D>> scatterData = new SimpleObjectProperty<List<Point3D>>(DEFAULT_SCATTER_DATA){
        
        @Override
        protected void invalidated() {
            if(meshes!=null){
                updateMesh();
            }
        }
    };

    public List<Point3D> getScatterData() {
        return scatterData.get();
    }

    public final void setScatterData(List<Point3D> value) {
        scatterData.set(value);
    }

    public ObjectProperty<List<Point3D>> scatterDataProperty() {
        return scatterData;
    }
    private final ObjectProperty<List<Number>> functionData = new SimpleObjectProperty<List<Number>>(){
        @Override
        protected void invalidated() {
            if(meshes!=null){
                updateF(get());
            }
        }
    };

    public List<Number> getFunctionData() {
        return functionData.get();
    }

    public void setFunctionData(List<Number> value) {
        functionData.set(value);
    }

    public ObjectProperty<List<Number>> functionDataProperty() {
        return functionData;
    }
    
    private final DoubleProperty height = new SimpleDoubleProperty(DEFAULT_HEIGHT){
        @Override
        protected void invalidated() {
            if(meshes!=null){
                updateMesh();
            }
        }
    };

    public double getHeight() {
        return height.get();
    }

    public final void setHeight(double value) {
        height.set(value);
    }

    public DoubleProperty heightProperty() {
        return height;
    }

    private final IntegerProperty level = new SimpleIntegerProperty(DEFAULT_LEVEL){

        @Override
        protected void invalidated() {
            if(meshes!=null){
                updateMesh();
            }
        }

    };
    
    public final int getLevel() {
        return level.get();
    }

    public final void setLevel(int value) {
        level.set(value);
    }

    public final IntegerProperty levelProperty() {
        return level;
    }
    
    private final BooleanProperty joinSegments = new SimpleBooleanProperty(DEFAULT_JOIN_SEGMENTS){
        @Override
        protected void invalidated() {
            if(meshes!=null){
                updateMesh();
            }
        }
    };

    public boolean isJoinSegments() {
        return joinSegments.get();
    }

    public final void setJoinSegments(boolean value) {
        joinSegments.set(value);
    }

    public BooleanProperty joinSegmentsProperty() {
        return joinSegments;
    }
    
    protected final void updateMesh() {

        meshes=FXCollections.<TexturedMesh>observableArrayList();
        
        createDots();
        if(joinSegments.get()){
//            System.out.println("Single mesh created");
        }
        getChildren().setAll(meshes);
        updateTransforms();
    }
    
    private AtomicInteger index;
    private void createDots() {
        if(!joinSegments.get()){
            List<TexturedMesh> dots=new ArrayList<>();
            index=new AtomicInteger();
            scatterData.get().forEach(point3d->{
//                TexturedMesh dot = new CuboidMesh(height.get(), height.get(), height.get(), level.get(), point3d);
                TexturedMesh dot = new TetrahedraMesh(height.get(), level.get(), point3d);
                dot.setCullFace(CullFace.BACK);
                dot.setDrawMode(DrawMode.FILL);
                dot.setDepthTest(DepthTest.ENABLE);
                dot.setId(""+index.getAndIncrement());
                dots.add(dot);
            });
            meshes.addAll(dots);
        } else {
//            TexturedMesh dot = new CuboidMesh(height.get(), height.get(), height.get(), level.get(), scatterData.get().get(0));
            TexturedMesh dot = new TetrahedraMesh(height.get(), level.get(), scatterData.get().get(0));
            dot.setCullFace(CullFace.BACK);
            dot.setDrawMode(DrawMode.FILL);
            dot.setDepthTest(DepthTest.ENABLE);
            dot.setId("0");
            /*
            Combine new polyMesh with previous polyMesh into one single polyMesh
            */
            MeshHelper mh = new MeshHelper((TriangleMesh)dot.getMesh());
//            TexturedMesh dot1 = new CuboidMesh(height.get(), height.get(), height.get(), level.get(), null);
            TexturedMesh dot1 = new TetrahedraMesh(height.get(), level.get(), null);
            MeshHelper mh1 = new MeshHelper((TriangleMesh)dot1.getMesh());
            mh.addMesh(mh1,scatterData.get().stream().skip(1).collect(Collectors.toList()));
            dot.updateMesh(mh);
            meshes.add(dot);
        }
    }

    @Override
    public void setTextureModeNone() {
        meshes.stream().forEach(m->m.setTextureModeNone());
    }

    @Override
    public void setTextureModeNone(Color color) {
        meshes.stream().forEach(m->m.setTextureModeNone(color));
    }

    @Override
    public void setTextureModeNone(Color color, String image) {
        meshes.stream().forEach(m->m.setTextureModeNone(color,image));
    }

    @Override
    public void setTextureModeImage(String image) {
        meshes.stream().forEach(m->m.setTextureModeImage(image));
    }

    @Override
    public void setTextureModePattern(Patterns.CarbonPatterns pattern, double scale) {
        meshes.stream().forEach(m->m.setTextureModePattern(pattern, scale));
    }

    @Override
    public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens) {
        meshes.stream().forEach(m->m.setTextureModeVertices3D(colors, dens));
    }

    @Override
    public void setTextureModeVertices3D(ColorPalette palette, int colors, Function<Point3D, Number> dens) {
        meshes.stream().forEach(m->m.setTextureModeVertices3D(palette, colors, dens));
    }

    @Override
    public void setTextureModeVertices3D(int colors, Function<Point3D, Number> dens, double min, double max) {
        meshes.stream().forEach(m->m.setTextureModeVertices3D(colors, dens, min, max));
    }

    @Override
    public void setTextureModeVertices1D(int colors, Function<Number, Number> function) {
        meshes.stream().forEach(m->m.setTextureModeVertices1D(colors, function));
    }

    @Override
    public void setTextureModeVertices1D(ColorPalette palette, int colors, Function<Number, Number> function) {
        meshes.stream().forEach(m->m.setTextureModeVertices1D(palette, colors, function));
    }

    @Override
    public void setTextureModeVertices1D(int colors, Function<Number, Number> function, double min, double max) {
        meshes.stream().forEach(m->m.setTextureModeVertices1D(colors, function, min, max));
    }

    @Override
    public void setTextureModeFaces(int colors) {
        meshes.stream().forEach(m->m.setTextureModeFaces(colors));
    }
    
    @Override
    public void setTextureModeFaces(ColorPalette palette, int colors) {
        meshes.stream().forEach(m->m.setTextureModeFaces(palette, colors));
    }
    
    @Override
    public void updateF(List<Number> values) {
         meshes.stream().forEach(m->m.updateF(values));
    }
    
    public void setDrawMode(DrawMode mode) {
        meshes.stream().forEach(m->m.setDrawMode(mode));
    }
    
    private void updateTransforms() {
        meshes.stream().forEach(m->m.updateTransforms());
    }
    
    public TexturedMesh getMeshFromId(String id){
        return meshes.stream().filter(p->p.getId().equals(id)).findFirst().orElse(meshes.get(0));
    }
    
}
