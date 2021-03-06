package sample;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Camera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Line;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;

public class Main extends Application {


    private static final double SCREEN_WIDTH = 1000;
    private static final double SCREEN_HEIGHT = 800;
    private static final double SUBSCENE_WIDTH = 800;
    private static final double SUBSCENE_HEIGHT = 800;
    private static final double FAR_CLIP = 20000;        // Maximum range of camera view
    private static final double NEAR_CLIP = 0.1;         // Minimum range of camera view

    // Initial conditions:
    private static final double TIMESTEP = 0.01;        // Customizable
    private static final double GM = 40000;             // Customizable, Gravitational Constant (M1 &2)
    private static final double Gm = 0;                 // Customizable, Gravitational Constant (M3)
    private static final double LARGE_RADIUS = 20;      // Radius for M1 & M2
    private static final double SMALL_RADIUS = 5;       // Radius for M3
    private double x1 = -300, y1 = 0;
    private double x2 = 300, y2 = 0;
    private double vX1 = 0, vy1 = 5;
    private double vX2 = 0, vY2 = -5;
    private final double xOfSphere3 = 0;
    private final double yOfSphere3 = 0;
    private double z3 = 0;                              // Customizable
    private double vZ3 = 3;                     // Customizable, but depends on zOfSphere, max when zOfSphere == 0


    private final Sphere sphere1 = new Sphere();
    private final Sphere sphere2 = new Sphere();
    private final Sphere sphere3 = new Sphere();


    private double lastX1, lastY1, lastX2, lastY2, lastZ3;  // Recording variables to draw tracks
    private Rotate rotateX, rotateY, rotateZ;
    public static Camera camera = new PerspectiveCamera(true); // true --> eye fixed on (0, 0, 0)
    private Translate translate;

    private final Label currentSphere1XLabel = new Label();
    private final Label currentSphere1YLabel = new Label();
    private final Label currentSphere2XLabel = new Label();
    private final Label currentSphere2YLabel = new Label();
    private final Label currentSphere3ZLabel = new Label();

    private double minX1 = x1, maxX1;
    private double minX2, maxX2 = x2;
    private double maxZ3 = z3;

    private Pane displayPane;
    private final Label minSphere1XLabel = new Label();
    private final Label maxSphere1XLabel = new Label();
    private final Label minSphere2XLabel = new Label();
    private final Label maxSphere2XLabel = new Label();
    private final Label maxSphere3ZLabel = new Label();
    private final Label cycleCountLabel = new Label();
    private int cycleCount = 0, yCount = 0;
    private boolean touchedYAxis = true;

    private XSSFWorkbook workbook;
    private int startingRowCount = 2;

    private double mouseStartX, mouseStartY;
    private boolean primaryPressed = false, secondaryPressed = false, bothPressed = false;

    public Main() {
    }

    @Override
    public void start(Stage primaryStage) throws IOException {

        // Load Excel file "Result.xlsx", create one if not found:
        File file = new File("Result.xlsx");
        if (file.exists()) {
            workbook = new XSSFWorkbook(new FileInputStream("Result.xlsx"));
        } else {
            workbook = new XSSFWorkbook();
            saveExcel(workbook);
        }

        // init:
        writeInitialConditions();
        setAppearances();
        setAnimation();
        Scene scene = setupScene();
        setCamera();

        // Camera controls by keys:
        primaryStage.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case A -> translate.setX(translate.getX() - 5);
                case D -> translate.setX(translate.getX() + 5);
                case W -> translate.setY(translate.getY() + 5);
                case S -> translate.setY(translate.getY() - 5);
                case LEFT -> rotateY.setAngle(rotateY.getAngle() + 5);
                case RIGHT -> rotateY.setAngle(rotateY.getAngle() - 5);
                case UP -> rotateX.setAngle(rotateX.getAngle() - 5);
                case DOWN -> rotateX.setAngle(rotateX.getAngle() + 5);
                case I -> translate.setZ(translate.getZ() + 5);
                case O -> translate.setZ(translate.getZ() - 5);
            }
        });

        // Camera controls by mouse:
        primaryStage.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseEvent -> {
            mouseStartX = mouseEvent.getX();
            mouseStartY = mouseEvent.getY();
            if (mouseEvent.getButton() == MouseButton.PRIMARY) {
                primaryPressed = true;
            }
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                secondaryPressed = true;
            }
            if (primaryPressed && secondaryPressed) {
                bothPressed = true;
            }
        });
        primaryStage.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseEvent -> {

            if (bothPressed) {
                System.out.println(true);
                camera.getTransforms().add(
                        translate = new Translate(0, 0,
                                mouseStartX - mouseEvent.getX()));
            } else {
                switch (mouseEvent.getButton()) {
                    case PRIMARY -> camera.getTransforms().addAll(
                            rotateX = new Rotate((mouseStartY - mouseEvent.getY()) / 5, Rotate.X_AXIS),
                            rotateY = new Rotate((mouseStartX - mouseEvent.getX()) / 5, Rotate.Y_AXIS));
                    case SECONDARY -> camera.getTransforms().addAll(
                            translate = new Translate(0,
                                    (mouseStartY - mouseEvent.getY()),
                                    (mouseStartX - mouseEvent.getX())));
                }
            }
            mouseStartX = mouseEvent.getX();
            mouseStartY = mouseEvent.getY();
        });
        primaryStage.addEventFilter(MouseEvent.MOUSE_RELEASED, mouseEvent -> {
            bothPressed = false;
            switch (mouseEvent.getButton()) {
                case PRIMARY:
                    primaryPressed = false;
                    break;

                case SECONDARY:
                    secondaryPressed = false;
                    break;
            }
        });

        primaryStage.setTitle("Sitnikov's Problem Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void writeInitialConditions() {
//        Sheet sheet = workbook.createSheet("Test");
        Sheet sheet = workbook.getSheet("Test");
        Row row1 = sheet.createRow(0);
        row1.createCell(0).setCellValue("Timestep");
        row1.createCell(1).setCellValue(TIMESTEP);
        row1.createCell(3).setCellValue("x1");
        row1.createCell(4).setCellValue("y1");
        row1.createCell(5).setCellValue("vx1");
        row1.createCell(6).setCellValue("vy1");
        row1.createCell(8).setCellValue("x2");
        row1.createCell(9).setCellValue("y2");
        row1.createCell(10).setCellValue("vx2");
        row1.createCell(11).setCellValue("vy2");
        row1.createCell(13).setCellValue("z3");
        row1.createCell(14).setCellValue("vz3");


        Row row2 = sheet.createRow(1);
        row2.createCell(0).setCellValue("GM");
        row2.createCell(1).setCellValue(GM);
        row2.createCell(3).setCellValue(x1);
        row2.createCell(4).setCellValue(y1);
        row2.createCell(5).setCellValue(vX1);
        row2.createCell(6).setCellValue(vy1);
        row2.createCell(8).setCellValue(x2);
        row2.createCell(9).setCellValue(y2);
        row2.createCell(10).setCellValue(vX2);
        row2.createCell(11).setCellValue(vY2);
        row2.createCell(13).setCellValue(z3);
        row2.createCell(14).setCellValue(vZ3);

        Row row3 = sheet.createRow(2);
        row3.createCell(0).setCellValue("Gm");
        row3.createCell(1).setCellValue(Gm);

        Row row4 = sheet.createRow(3);
        row4.createCell(0).setCellValue("Cycle counts");
        row4.createCell(1).setCellValue(cycleCount);
        saveExcel(workbook);
    }

    private void setAppearances() {
        sphere1.setRadius(LARGE_RADIUS);
        sphere2.setRadius(LARGE_RADIUS);
        sphere3.setRadius(SMALL_RADIUS);

        PhongMaterial material1 = new PhongMaterial();
        material1.setDiffuseColor(Color.ORANGE);
        material1.setSpecularColor(Color.BLACK);
        sphere1.setMaterial(material1);

        PhongMaterial material2 = new PhongMaterial();
        material2.setDiffuseColor(Color.DARKCYAN);
        material2.setSpecularColor(Color.BLACK);
        sphere2.setMaterial(material2);

        PhongMaterial material3 = new PhongMaterial();
        material3.setDiffuseColor(Color.SEAGREEN);
        material3.setSpecularColor(Color.BLACK);
        sphere3.setMaterial(material3);
    }

    private void setAnimation() {
        Timeline animation = new Timeline(new KeyFrame(Duration.millis(1), e -> calculate()));
        animation.setCycleCount(Timeline.INDEFINITE);
        animation.play();
    }

    private Scene setupScene() throws IOException {
        // Create pane to display the spheres and lines
        displayPane = new Pane();
        displayPane.getChildren().add(sphere1);
        displayPane.getChildren().add(sphere2);
        displayPane.getChildren().add(sphere3);
        displayPane.getChildren().add(new Line(SUBSCENE_WIDTH, 0, -1 * SUBSCENE_WIDTH, 0));
        displayPane.getChildren().add(new Line(0, SUBSCENE_HEIGHT, 0, -1 * SUBSCENE_HEIGHT));

        // Create subscene to hold the displayPane and attach camera
        SubScene subScene = new SubScene(displayPane, SUBSCENE_WIDTH, SUBSCENE_HEIGHT);
        subScene.setCamera(camera);

        // Create a VBox to hold all widgets
        VBox leftControl = new VBox();

        Button topViewBtn = new Button("Top View");
        Button tiltViewBtn = new Button("Tilt View");   // default view
        topViewBtn.setDisable(false);
        tiltViewBtn.setDisable(true);

        leftControl.getChildren().add(topViewBtn);
        leftControl.getChildren().add(tiltViewBtn);
        leftControl.getChildren().add(currentSphere1XLabel);
        leftControl.getChildren().add(currentSphere1YLabel);
        leftControl.getChildren().add(currentSphere2XLabel);
        leftControl.getChildren().add(currentSphere2YLabel);
        leftControl.getChildren().add(currentSphere3ZLabel);
        leftControl.getChildren().add(minSphere1XLabel);
        leftControl.getChildren().add(maxSphere1XLabel);
        leftControl.getChildren().add(maxSphere2XLabel);
        leftControl.getChildren().add(minSphere2XLabel);
        leftControl.getChildren().add(maxSphere3ZLabel);
        leftControl.getChildren().add(cycleCountLabel);

        // Create splitPane to hold both controls and the subscene with displayPane
        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftControl, subScene);

        // Create a main scene to hold the splitPane
//        Parent root = FXMLLoader.load(getClass().getResource("menu.fxml"));
        Scene scene = new Scene(splitPane, SCREEN_WIDTH, SCREEN_HEIGHT);
        scene.setFill(Color.SILVER);
        return scene;
    }

    private void setCamera() {
        // For timestep = 0.75:
        // TranslateX: 0.0, TranslateY: 935.0, TranslateZ: -655.0,
        // RotateX: 70.0, RotateY: 0.0, RotateZ: 0.0

        // MOVE CAMERA
        camera.getTransforms().addAll(
                rotateX = new Rotate(70, Rotate.X_AXIS),
                rotateY = new Rotate(0, Rotate.Y_AXIS),
                rotateZ = new Rotate(0, Rotate.Z_AXIS),
                translate = new Translate(60, 935, -1030));

        // Move camera to a good view
        camera.translateZProperty().set(-1000);

        // Set the clipping planes
        camera.setNearClip(NEAR_CLIP);
        camera.setFarClip(FAR_CLIP);
        ((PerspectiveCamera) camera).setFieldOfView(35);
    }

    private void saveExcel(XSSFWorkbook workbook) {
        try {
            workbook.write(new FileOutputStream("Result.xlsx"));
            // System.out.println("Excel created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void calculate() {

        // record current locations:
        lastX1 = x1;
        lastY1 = y1;
        lastX2 = x2;
        lastY2 = y2;
        lastZ3 = z3;


        // record current velocities:
        double lastVXOfSphere1 = vX1;
        double lastVYOfSphere1 = vy1;
        double lastVXOfSphere2 = vX2;
        double lastVYOfSphere2 = vY2;
        double lastVZOfSphere3 = vZ3;


        // Just separating the calculation steps for M1
        double powX1 = Math.pow(x1, 2);
        double powY1 = Math.pow(y1, 2);
        double powZ3 = Math.pow(z3, 2);
        double powXY = Math.pow(powX1 + powY1, 1.5);
        double powXYZ = Math.pow(powX1 + powY1 + powZ3, 1.5);


        // Calculate and update velocities and locations (M1):
        vX1 += (((2 * Gm) / powXYZ) - GM / (2 * powXY)) * x1 * TIMESTEP;
        x1 += lastVXOfSphere1 * TIMESTEP;

        vy1 += (((2 * Gm) / powXYZ) - GM / (2 * powXY)) * y1 * TIMESTEP;
        y1 += lastVYOfSphere1 * TIMESTEP;


        // Just separating the calculation steps for M2
        double powX2 = Math.pow(x2, 2);
        double powY2 = Math.pow(y2, 2);
        powXY = Math.pow(powX2 + powY2, 1.5);
        powXYZ = Math.pow(powX2 + powY2 + powZ3, 1.5);


        // Calculate and update velocities and locations (M2):
        vX2 += (((2 * Gm) / powXYZ) - GM / (2 * powXY)) * x2 * TIMESTEP;
        x2 += lastVXOfSphere2 * TIMESTEP;

        vY2 += (((2 * Gm) / powXYZ) - GM / (2 * powXY)) * y2 * TIMESTEP;
        y2 += lastVYOfSphere2 * TIMESTEP;


        // Just separating the calculation steps for M3
        double powXDiff = Math.pow(x2 - x1, 2);
        double powYDiff = Math.pow(y2 - y1, 2);
        powXY = Math.pow(powXDiff + powYDiff + powZ3, 1.5);


        // Calculate and update velocities and locations (M3):
        vZ3 += (-1) * ((4 * GM) / powXY) * z3 * TIMESTEP;
        z3 += lastVZOfSphere3 * TIMESTEP;


        countCycles();
        drawTracks(x1, y1, x2, y2, lastX1, lastY1, lastX2, lastY2);
        move(x1, y1, x2, y2, z3);
    }

    private void countCycles() {
        if (Math.round(y1) == 0 && !touchedYAxis) {

            yCount++;
            touchedYAxis = true;

            if (yCount == 2) {
                // One cycle completes
                yCount = 0;
                cycleCount++;

                // Record the results into "Result.xlsx":
                XSSFSheet sheet = workbook.getSheet("Test");
                Cell cycleCountCell = sheet.getRow(3).getCell(1);

                Row startingRow;
                if (startingRowCount <= 3) {
                    startingRow = sheet.getRow(startingRowCount);
                } else {
                    startingRow = sheet.createRow(startingRowCount);
                }
                startingRow.createCell(3).setCellValue(x1);
                startingRow.createCell(4).setCellValue(y1);
                startingRow.createCell(5).setCellValue(vX1);
                startingRow.createCell(6).setCellValue(vy1);
                startingRow.createCell(8).setCellValue(x2);
                startingRow.createCell(9).setCellValue(y2);
                startingRow.createCell(10).setCellValue(vX2);
                startingRow.createCell(11).setCellValue(vY2);
                startingRow.createCell(13).setCellValue(z3);
                startingRow.createCell(14).setCellValue(vZ3);

                cycleCountCell.setCellValue(cycleCount);
                saveExcel(workbook);

                startingRowCount++;
            }
        } else if (Math.round(y1) != 0 && touchedYAxis) {
            touchedYAxis = false;
        }
    }

    private void drawTracks(double x1, double y1, double x2, double y2,
                            double lastX1, double lastY1, double lastX2, double lastY2) {
        Line line1 = new Line(x1, y1, lastX1, lastY1);
        Line line2 = new Line(x2, y2, lastX2, lastY2);
        displayPane.getChildren().add(line1);
        displayPane.getChildren().add(line2);
    }

    private void move(double x1, double y1, double x2, double y2, double z) {

        // Move M1
        sphere1.translateXProperty().set(x1);
        sphere1.translateYProperty().set(y1);

        // Move M2
        sphere2.translateXProperty().set(x2);
        sphere2.translateYProperty().set(y2);

        // Move M3
        sphere3.translateXProperty().set(xOfSphere3);
        sphere3.translateYProperty().set(yOfSphere3);
        sphere3.translateZProperty().set(z);

        // Update the labels:
        // Multiply Y and Z component by (-1) to match our views
        currentSphere1XLabel.setText("Current X of M1: " + roundToThreeSig(this.x1));
        currentSphere1YLabel.setText("Current Y of M1: " + roundToThreeSig(this.y1) * -1);
        currentSphere2XLabel.setText("Current X of M2: " + roundToThreeSig(this.x2));
        currentSphere2YLabel.setText("Current Y of M2: " + roundToThreeSig(this.y2) * -1);
        currentSphere3ZLabel.setText("Current Z of M3: " + roundToThreeSig(z3) * -1);

        minX1 = Math.min(minX1, this.x1);
        maxX1 = Math.max(maxX1, this.x1);
        minX2 = Math.min(minX2, this.x2);
        maxX2 = Math.max(maxX2, this.x2);
        maxZ3 = Math.max(maxZ3, z3);
        minSphere1XLabel.setText("Min x of M1: " + roundToThreeSig(minX1));
        maxSphere1XLabel.setText("Max x of M1: " + roundToThreeSig(maxX1));
        minSphere2XLabel.setText("Min x of M2: " + roundToThreeSig(minX2));
        maxSphere2XLabel.setText("Max x of M2: " + roundToThreeSig(maxX2));
        maxSphere3ZLabel.setText("Max z of M3: " + roundToThreeSig(maxZ3));
        cycleCountLabel.setText("Cycle count: " + cycleCount);


//        Check camera properties:
        /*
        System.out.print("TranslateX: " + translate.getX());
        System.out.print(", TranslateY: " + translate.getY());
        System.out.print(", TranslateZ: " + translate.getZ());
        System.out.print(", RotateX: " + rotateX.getAngle());
        System.out.print(", RotateY: " + rotateY.getAngle());
        System.out.println(", RotateZ: " + rotateZ.getAngle());
        */

    }

    private double roundToThreeSig(double value) {
        BigDecimal bigDecimal = new BigDecimal(value);
        bigDecimal = bigDecimal.round(new MathContext(3));
        return bigDecimal.doubleValue();
    }

    public static void main(String[] args) {
        launch(args);
    }
}