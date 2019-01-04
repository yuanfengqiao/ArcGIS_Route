package com.b202.arcgis_route;

/**
 * @author YuanFengQiqo
 * @date 2019/1/2 14:53
 */
/*
 * Copyright 2018 Esri.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.TransportationNetworkDataset;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.loadable.LoadStatus;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.MobileMapPackage;
import com.esri.arcgisruntime.mapping.Viewpoint;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;
import com.esri.arcgisruntime.symbology.SimpleMarkerSymbol;
import com.esri.arcgisruntime.tasks.networkanalysis.Route;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteParameters;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteResult;
import com.esri.arcgisruntime.tasks.networkanalysis.RouteTask;
import com.esri.arcgisruntime.tasks.networkanalysis.Stop;

public class App extends Application {

    private Point startPoint;
    private Point endPoint;
    private final int hexRed = 0xFFFF0000;
    private final int hexBlue = 0xFF0000FF;

    private GraphicsOverlay graphicsOverlay;
    private RouteTask solveRouteTask;

    private RouteParameters routeParameters;
    private TransportationNetworkDataset transportationNetwork;
    private MapView mapView;

    private List<Point> list = new ArrayList();
    private List<Stop> stopList = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        StackPane stackPane = new StackPane();
        Scene scene = new Scene(stackPane);
        stage.setTitle("测试台");
        stage.setWidth(600);
        stage.setHeight(350);
        stage.setScene(scene);
        stage.show();
        mapView = new MapView();
        setupMobileMap();
        setupGraphicsOverlay();
        stackPane.getChildren().add(mapView);
    }

    private void setupMap() {
        if (mapView != null) {
            Basemap.Type basemapType = Basemap.Type.STREETS_VECTOR;
            double latitude = 34.05293;
            double longitude = -118.24368;
            int levelOfDetail = 11;
            ArcGISMap map = new ArcGISMap(basemapType, latitude, longitude, levelOfDetail);
            mapView.setMap(map);
        }
    }





    private void setupGraphicsOverlay() {
        if (mapView != null) {
            graphicsOverlay = new GraphicsOverlay();
            mapView.getGraphicsOverlays().add(graphicsOverlay);
        }
    }

    private void setupMobileMap() {
        if (mapView != null) {
//      String mmpkFile = "./California.mmpk";
            String mmpkFile = "D:\\IDEAWorkSpace\\get-a-route-and-directions-offline\\display-a-route-with-streetmap-premium\\src\\California.mmpk";
            final MobileMapPackage mapPackage = new MobileMapPackage(mmpkFile);
            mapPackage.addDoneLoadingListener(() -> {
                if (mapPackage.getLoadStatus() == LoadStatus.LOADED && mapPackage.getMaps().size() > 0) {
                    double latitude = 34.05293;
                    double longitude = -118.24368;
                    double scale = 220000;
                    ArcGISMap map = mapPackage.getMaps().get(0);
                    transportationNetwork = map.getTransportationNetworks().get(0);
                    mapView.setMap(map);
                    map.setInitialViewpoint(new Viewpoint(latitude, longitude, scale));
                    setupRouteTask();
                } else {
                    setupMap();
                    new Alert(Alert.AlertType.ERROR, "MMPK failed to load: " + mapPackage.getLoadError().getMessage())
                            .show();
                }
            });
            mapPackage.loadAsync();
        }
    }

    /*
     * setupRouteTask()是使用transportationNetwork初始化并加solveRouteTask，将使用详细信息transportationNetwork计算两点之间的路线㿿
     * */
    private void setupRouteTask() {
        solveRouteTask = new RouteTask(transportationNetwork);
        //mRouteParameters = solveRouteTask.createDefaultParametersAsync().get();
        solveRouteTask.loadAsync();
        //solveRouteTask加载完毕后，创建默认的参数
        solveRouteTask.addDoneLoadingListener(() -> {
            if (solveRouteTask.getLoadStatus() == LoadStatus.LOADED) {
                final ListenableFuture<RouteParameters> routeParamsFuture = solveRouteTask.createDefaultParametersAsync();
                routeParamsFuture.addDoneListener(() -> {

                    try {
                        routeParameters = routeParamsFuture.get();
                        createRouteAndDisplay();
                    } catch (InterruptedException | ExecutionException e) {
                        new Alert(Alert.AlertType.ERROR, "Cannot create RouteTask parameters " + e.getMessage()).show();
                    }
                });
            } else {
                new Alert(Alert.AlertType.ERROR, "Unable to load RouteTask " + solveRouteTask.getLoadStatus().toString())
                        .show();
            }
        });
    }

    //该方法是在地图上指定位置符号化，包括符号的颜色，大小，形状，外观颜色
    private void setMapMarker(Point location, SimpleMarkerSymbol.Style style, int markerColor, int outlineColor) {
        final float markerSize = 15;//符号外形的大小
        final float markerOutlineThickness = 2;
        SimpleMarkerSymbol pointSymbol = new SimpleMarkerSymbol(style, markerColor, markerSize);
        pointSymbol.setOutline(new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, outlineColor, markerOutlineThickness));//设置外形轮廓的大尿
        Graphic pointGraphic = new Graphic(location, pointSymbol);//Graphic几何形状
        graphicsOverlay.getGraphics().add(pointGraphic);
    }

    //setStartMarker(Point location)，该方法用于显示和存在起始的位罿
    private void setStartMarker(Point location) {
        if (endPoint != null) {
            graphicsOverlay.getGraphics().clear();
        }
        setMapMarker(location, SimpleMarkerSymbol.Style.DIAMOND, hexRed, hexBlue);
        startPoint = location;
        endPoint = null;
        //调用该方法一次，就将鼠标点击的点加入集合丿
        list.add(startPoint);
    }

    //setEndMarker(Point location)，该方法用于显示和存傿 结束炿 的位罿
    private void setEndMarker(Point location) {
        setMapMarker(location, SimpleMarkerSymbol.Style.SQUARE, hexBlue, hexRed);
        endPoint = location;
        list.add(endPoint);
        solveForRoute();
//    graphicsOverlay.getGraphics().clear();


    }

    /*createRouteAndDisplay(),该方法提供一个监听地图的监听，当用户在地图上点击鼠标时，该方法就会调甿
      setStartMarker(mapPoint)，setEndMarker(mapPoint)，将点击的点可视化在地图丿
      */
    private void createRouteAndDisplay() {
        mapView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.isStillSincePress()) {
                Point2D point = new Point2D(e.getX(), e.getY());
                Point mapPoint = mapView.screenToLocation(point);
                if (startPoint == null) {
                    setStartMarker(mapPoint);
                }

                //判断list集合中的点，当点的个数大亿3时，再加入的点就是终点，之前的点都是起始炿
                else if (endPoint == null && list.size() >= 3) {
                    setEndMarker(mapPoint);
                    stopList.clear();
                } else {
                    setStartMarker(mapPoint);
                }
            }
        });
    }

    //solveForRoute()该方法用于解决两个位置之间的路线，并将路线显示在地图丿
    private void solveForRoute() {
        if (startPoint != null && endPoint != null) {
//      routeParameters.setStops(Arrays.asList(new Stop(startPoint), new Stop(endPoint)));
            //遍历list<Point>集合中存储的点，将该点加到List<Stop>集合中，设置为停靠点
            for (Point p : list) {
                Stop stop = new Stop(p);
                stopList.add(stop);
            }
            routeParameters.setStops(stopList);

            final ListenableFuture<RouteResult> routeResultFuture = solveRouteTask.solveRouteAsync(routeParameters);
            routeResultFuture.addDoneListener(() -> {
                try {
                    RouteResult routeResult = routeResultFuture.get();
                    if (routeResult.getRoutes().size() > 0) {
                        Route firstRoute = routeResult.getRoutes().get(0);
                        Polyline routePolyline = firstRoute.getRouteGeometry();
                        SimpleLineSymbol routeSymbol = new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, hexBlue, 4.0f);
                        Graphic routeGraphic = new Graphic(routePolyline, routeSymbol);
                        graphicsOverlay.getGraphics().add(routeGraphic);
                        list.clear();
                    } else {
                        new Alert(Alert.AlertType.WARNING, "没有路径").show();
                    }
                } catch (InterruptedException | ExecutionException e) {
                    new Alert(Alert.AlertType.ERROR, "Solve RouteTask failed " + e.getMessage() + e.getMessage()).show();
                }
            });
        }
    }

    @Override
    public void stop() {
        if (mapView != null) {
            mapView.dispose();
        }
    }
    public static void main(String[] args) {
        Application.launch(args);
    }

}

