package vip.youwe.sheller.ui.controller;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.util.converter.DefaultStringConverter;
import org.json.JSONArray;
import org.json.JSONObject;
import vip.youwe.sheller.core.ShellService;
import vip.youwe.sheller.dao.ShellManager;
import vip.youwe.sheller.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FileManagerViewController {

    private ShellManager shellManager;
    @FXML
    private TreeView dirTree;
    @FXML
    private ComboBox currentPathCombo;
    @FXML
    private TableView fileListTableView;
    @FXML
    private TableColumn fileNameCol;
    @FXML
    private StackPane fileManagerStackPane;
    @FXML
    private GridPane fileListGridPane;
    @FXML
    private GridPane fileContentGridPane;
    @FXML
    private TextField filePathText;

    public void init(ShellService shellService, List<Thread> workList, Label statusLabel, Map<String, String> basicInfoMap) {
        this.currentShellService = shellService;
        this.workList = workList;
        this.statusLabel = statusLabel;
        this.basicInfoMap = basicInfoMap;
        try {
            initFileManagerView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private Button openPathBtn;
    @FXML
    private ComboBox charsetCombo;
    @FXML
    private TextArea fileContentTextArea;
    @FXML
    private Button saveFileContentBtn;
    @FXML
    private Button cancelFileContentBtn;
    private ShellService currentShellService;
    private List<Thread> workList;
    Map<String, String> basicInfoMap;
    private Label statusLabel;

    private void initFileManagerView() throws Exception {
        initFileListTableColumns();

        initCharsetCombo();
        String driveList = this.basicInfoMap.get("driveList");
        TreeItem<String> rootItem = new TreeItem<String>("文件系统", new ImageView());
        rootItem.getGraphic().setUserData("base");
        Image icon = new Image(new ByteArrayInputStream(Utils.getResourceData("drive.png")));
        for (String drive : driveList.split(";")) {
            TreeItem<String> driveItem = new TreeItem<String>(drive, new ImageView(icon));
            driveItem.getGraphic().setUserData("root");
            driveItem.setValue(drive);
            rootItem.getChildren().add(driveItem);
            this.dirTree.setRoot(rootItem);
        }
        String currentPath = this.basicInfoMap.get("currentPath");
        ObservableList<String> pathList = FXCollections.observableArrayList(currentPath);
        this.currentPathCombo.setItems(pathList);
        this.currentPathCombo.getSelectionModel().select(0);

        loadContextMenu();
        this.dirTree.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            TreeItem<String> currentTreeItem = (TreeItem) newValue;

            String pathString = FileManagerViewController.this.getFullPath(currentTreeItem);
            try {
                FileManagerViewController.this.expandByPath(pathString);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });

        expandByPath(currentPath);

        this.charsetCombo.setItems(FXCollections.observableArrayList("自动", "GBK", "UTF-8"));

        this.cancelFileContentBtn.setOnAction(event -> {
            try {
                switchPane("list");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });

        this.saveFileContentBtn.setOnAction(event -> {
            String filePath = this.filePathText.getText();
            try {
                saveFileContent(filePath);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });
        this.fileListTableView.setEditable(true);

        this.fileNameCol.setOnEditCommit(new EventHandler<TableColumn.CellEditEvent>() {
            public void handle(TableColumn.CellEditEvent cellEditEvent) {
                String oldFileName = cellEditEvent.getOldValue().toString();
                String newFileName = cellEditEvent.getNewValue().toString();
                FileManagerViewController.this.rename(oldFileName, newFileName);
            }
        });
        this.fileNameCol.setOnEditCancel(new EventHandler<TableColumn.CellEditEvent>() {
            public void handle(TableColumn.CellEditEvent event) {
                try {
                    FileManagerViewController.this.expandByPath(FileManagerViewController.this.currentPathCombo.getValue().toString());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });
        this.openPathBtn.setOnAction(event -> {
            try {
                expandByPath(this.currentPathCombo.getValue().toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        });

        switchPane("list");
    }

    private void initCharsetCombo() {
        this.charsetCombo.getSelectionModel().selectedItemProperty().addListener((options, oldValue, newValue) -> {
            String filePath = this.filePathText.getText();
            String charset = newValue.toString().equals("自动") ? null : this.charsetCombo.getValue().toString();
            showFile(filePath, charset);
        });
    }


    private void uploadFile() throws Exception {
        String currentPath = this.currentPathCombo.getValue().toString();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("请选择需要上传的文件");

        File selectdFile = fileChooser.showOpenDialog(this.fileListGridPane.getScene().getWindow());
        if (selectdFile == null)
            return;
        String fileName = selectdFile.getName();
        byte[] fileContent = Utils.getFileData(selectdFile.getAbsolutePath());
        int bufSize = this.currentShellService.currentType.equals("aspx") ? 524288 : 46080;
        this.statusLabel.setText("正在上传……");

        Runnable runner = () -> {
            try {
                if (fileContent.length < bufSize) {
                    JSONObject resultObj = this.currentShellService.uploadFile(currentPath + fileName, fileContent);
                    String status = resultObj.getString("status");
                    String msg = resultObj.getString("msg");
                    if (status.equals("fail")) {
                        Platform.runLater(() -> this.statusLabel.setText("文件上传失败:" + msg));

                        return;
                    }
                } else {
                    List<byte[]> blocks = Utils.splitBytes(fileContent, bufSize);
                    for (int i = 0; i < blocks.size(); i++) {
                        if (i == 0) {

                            JSONObject resultObj = currentShellService.uploadFile(currentPath + fileName, blocks.get(i));

                            String status = resultObj.getString("status");
                            String msg = resultObj.getString("msg");
                            if (status.equals("fail")) {
                                Platform.runLater(() -> this.statusLabel.setText("文件上传失败:" + msg));

                                return;
                            }
                        } else {
                            JSONObject resultObj = currentShellService.appendFile(currentPath + fileName, blocks.get(i));
                            String status = resultObj.getString("status");
                            String msg = resultObj.getString("msg");
                            int currentBlockIndex = i;
                            Platform.runLater(() -> {
                                if (status.equals("fail")) {
                                    this.statusLabel.setText("文件上传失败:" + msg);
                                } else {
                                    this.statusLabel.setText(String.format("正在上传……%skb/%skb", bufSize * currentBlockIndex / 1024,
                                            fileContent.length / 1024));
                                }
                            });

                            if (status.equals("fail"))
                                return;
                        }
                    }
                }
                Platform.runLater(() -> {
                    this.statusLabel.setText("上传完成");
                    try {
                        this.expandByPath(currentPath);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> this.statusLabel.setText("操作失败:" + e.getMessage()));
            }
        };

        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }

    private void rename(String oldFileName, String newFileName) {
        String currentDir = this.currentPathCombo.getValue().toString();
        String oldFullName = currentDir + oldFileName;
        String newFullName = currentDir + newFileName;
        Runnable runner = () -> {
            try {
                JSONObject resultObj = this.currentShellService.renameFile(oldFullName, newFullName);
                String status = resultObj.getString("status");
                String msg = resultObj.getString("msg");
                Platform.runLater(() -> {
                    try {
                        this.expandByPath(this.currentPathCombo.getValue().toString());
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    if (status.equals("fail")) {
                        this.statusLabel.setText(msg);
                    } else {
                        this.statusLabel.setText(msg);
                    }
                });

            } catch (Exception e) {
                this.statusLabel.setText("操作失败:" + e.getMessage());
            }
        };
        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }

    private void saveFileContent(String pathString) throws UnsupportedEncodingException {
        String charset = null;
        if (this.charsetCombo.getSelectionModel().getSelectedIndex() > 0) {
            charset = this.charsetCombo.getValue().toString();
        }

        byte[] fileContent = (charset == null) ? this.fileContentTextArea.getText().getBytes() : this.fileContentTextArea.getText().getBytes(charset);

        this.statusLabel.setText("正在保存……");
        Runnable runner = () -> {
            try {
                JSONObject resultObj = this.currentShellService.uploadFile(pathString, fileContent, true);
                String status = resultObj.getString("status");
                String msg = resultObj.getString("msg");
                Platform.runLater(() -> {
                    if (status.equals("success")) {
                        this.statusLabel.setText("保存成功。");
                    } else {
                        this.statusLabel.setText("保存失败:" + msg);
                    }

                });

            } catch (Exception e) {
                Platform.runLater(() -> this.statusLabel.setText("操作失败:" + e.getMessage()));
            }
        };


        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }

    private void switchPane(String show) throws UnsupportedEncodingException {
        if (show.equals("list")) {

            this.fileListGridPane.setOpacity(1.0D);
            this.fileContentGridPane.setOpacity(0.0D);
            this.fileListGridPane.toFront();
        } else if (show.equals("content")) {

            this.fileListGridPane.toBack();
            this.fileListGridPane.setOpacity(0.0D);
            this.fileContentGridPane.setOpacity(1.0D);
            this.fileContentGridPane.toFront();
        }
    }


    private String getFullPath(TreeItem currentTreeItem) {
        String fileSep = "/";

        String currentPath = currentTreeItem.getValue().toString();
        TreeItem parent = currentTreeItem;
        while (!(parent = parent.getParent()).getGraphic().getUserData().equals("base")) {
            String parentText = parent.getValue().toString();
            if (parent.getGraphic().getUserData().equals("root")) {

                currentPath = parentText + currentPath;
                continue;
            }
            currentPath = parentText + fileSep + currentPath;
        }

        if (!parent.getGraphic().getUserData().equals("directory") && !currentPath.endsWith(fileSep))
            currentPath = currentPath + fileSep;
        return currentPath;
    }

    private void initFileListTableColumns() {
        ObservableList<TableColumn<List<StringProperty>, ?>> tcs = this.fileListTableView.getColumns();
        // todo (List) 转 (ObservableValue)
        tcs.get(0).setCellValueFactory(data -> (ObservableValue) data.getValue().get(0));
        tcs.get(1).setCellValueFactory(data -> (ObservableValue) data.getValue().get(1));
        tcs.get(2).setCellValueFactory(data -> (ObservableValue) data.getValue().get(2));
        // ((TableColumn) tcs.get(0)).setCellValueFactory(data -> ((List) data.getValue()).get(0));
        // ((TableColumn) tcs.get(1)).setCellValueFactory(data -> ((List) data.getValue()).get(1));
        // ((TableColumn) tcs.get(2)).setCellValueFactory(data -> ((List) data.getValue()).get(2));

        this.fileListTableView.setRowFactory(tv -> {
            TableRow<List<StringProperty>> row = new TableRow<List<StringProperty>>();
            row.setOnMouseClicked((event) -> {
                event.consume();
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    String path = this.currentPathCombo.getValue().toString();
                    // String name = ((StringProperty)((List)row.getItem()).get(0)).getValue().toString();
                    String name = row.getItem().get(0).getValue();
                    String type = row.getItem().get(3).getValue();
                    if (!path.endsWith("/")) {
                        path = path + "/";
                    }

                    if (type.equals("file")) {
                        String fileName = path + name;
                        this.filePathText.setText(fileName);
                        this.showFile(fileName, (String) null);
                        try {
                            this.switchPane("content");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    } else if (type.equals("directory")) {
                        try {
                            this.expandByPath(path + name);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }

            });
            return row;
        });

        this.fileNameCol.setCellFactory(column -> new TextFieldTableCell<StringProperty, String>(new DefaultStringConverter()) {
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (!(item == null | empty)) {


                    String type = null;
                    try {
                        type = ((StringProperty) ((List) getTableRow().getItem()).get(3)).get();
                    } catch (Exception e) {
                        return;
                    }

                    if (type.equals("directory")) {

                        try {
                            Image icon = new Image(new ByteArrayInputStream(Utils.getResourceData("folder.png")));
                            setGraphic(new ImageView(icon));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else if (type.equals("file")) {

                        try {
                            Image icon = new Image(new ByteArrayInputStream(Utils.getResourceData("file.png")));
                            setGraphic(new ImageView(icon));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    setText(item);
                }
            }
        });
    }


    private TreeItem findTreeItemByPath(Path path) {
        String osInfo = this.basicInfoMap.get("osInfo");

        TreeItem currentItem = null;
        List<String> pathParts = new ArrayList<String>();
        String pathString = path.toString();
        if (pathString.equals("/")) {

            pathParts.add("/");
        } else {

            pathParts.addAll(Arrays.asList(pathString.split("/|\\\\")));
            if (osInfo.indexOf("linux") >= 0) {
                pathParts.set(0, "/");
            } else {
                pathParts.set(0, pathParts.get(0) + "/");
            }
        }

        Image icon = null;
        try {
            icon = new Image(new ByteArrayInputStream(Utils.getResourceData("folder.png")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String childPath : pathParts) {
            if (currentItem == null) {
                TreeItem childItem = findTreeItem(this.dirTree.getRoot(), childPath);
                currentItem = childItem;
            } else {
                TreeItem childItem = findTreeItem(currentItem, childPath);
                if (childItem == null) {
                    childItem = new TreeItem(childPath, new ImageView(icon));
                    childItem.getGraphic().setUserData("directory");
                    currentItem.getChildren().add(childItem);
                }
                currentItem = childItem;
            }
            currentItem.setExpanded(true);
        }

        this.dirTree.getSelectionModel().select(currentItem);
        return currentItem;
    }


    private void insertTreeItems(JSONArray rows, TreeItem currentTreeItem) {
        currentTreeItem.getChildren().clear();
        for (int i = 0; i < rows.length(); i++) {

            try {
                JSONObject fileObj = rows.getJSONObject(i);
                String type = new String(Base64.decode(fileObj.getString("type")), "UTF-8");
                String name = new String(Base64.decode(fileObj.getString("name")), "UTF-8");
                if (!name.equals(".") && !name.equals("..")) {
                    if (type.equals("directory")) {

                        Image icon = new Image(new ByteArrayInputStream(Utils.getResourceData("folder.png")));
                        TreeItem<String> treeItem = new TreeItem<String>(name, new ImageView(icon));
                        treeItem.getGraphic().setUserData("directory");
                        currentTreeItem.getChildren().add(treeItem);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        currentTreeItem.setExpanded(true);
        this.dirTree.getSelectionModel().select(currentTreeItem);
    }

    private TreeItem findTreeItem(TreeItem treeItem, String text) {
        ObservableList<TreeItem> childItemList = treeItem.getChildren();
        for (TreeItem childItem : childItemList) {
            if (childItem.getValue().toString().equals(text)) {
                return childItem;
            }
        }
        return null;
    }

    private void insertFileRows(JSONArray jsonArray) {
        ObservableList<List<StringProperty>> data = FXCollections.observableArrayList();

        for (int i = 0; i < jsonArray.length(); i++) {

            JSONObject rowObj = jsonArray.getJSONObject(i);
            try {
                String type = new String(Base64.decode(rowObj.getString("type")), "UTF-8");
                String name = new String(Base64.decode(rowObj.getString("name")), "UTF-8");
                String size = new String(Base64.decode(rowObj.getString("size")), "UTF-8");
                String lastModified = new String(Base64.decode(rowObj.getString("lastModified")));
                List<StringProperty> row = new ArrayList<StringProperty>();
                row.add(0, new SimpleStringProperty(name));

                row.add(1, new SimpleStringProperty(size));
                row.add(2, new SimpleStringProperty(lastModified));
                row.add(3, new SimpleStringProperty(type));
                data.add(row);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.fileListTableView.setItems(data);
    }

    private void expandByPath(String pathStr) throws UnsupportedEncodingException {
        Path path = Paths.get(pathStr, new String[0]).normalize();
        String pathString = path.toString().endsWith("/") ? path.toString() : (path.toString() + "/");
        TreeItem currentTreeItem = findTreeItemByPath(path);
        this.currentPathCombo.setValue(pathString);
        this.statusLabel.setText("正在加载目录……");
        Runnable runner = () -> {
            try {
                JSONObject resultObj = this.currentShellService.listFiles(pathString);
                Platform.runLater(() -> {
                    try {
                        String status = resultObj.getString("status");
                        String msg = resultObj.getString("msg");
                        if (status.equals("fail")) {
                            this.statusLabel.setText("目录读取失败:" + msg);
                            return;
                        }

                        this.statusLabel.setText("目录加载成功");
                        msg = msg.replace("},]", "}]");
                        JSONArray objArr = new JSONArray(msg.trim());
                        this.insertFileRows(objArr);
                        this.insertTreeItems(objArr, currentTreeItem);
                    } catch (Exception e) {
                        this.statusLabel.setText("操作失败：" + e.getMessage());
                    }

                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        };
        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }


    private void showFile(String filePath, String charset) {
        Runnable runner = () -> {

            try {
                JSONObject resultObj = this.currentShellService.showFile(filePath, charset);
                String status = resultObj.getString("status");
                String msg = resultObj.getString("msg");
                Platform.runLater(() -> {
                    if (status.equals("fail")) {
                        this.statusLabel.setText("文件打开失败:" + msg);
                    } else {
                        this.fileContentTextArea.setText(msg);
                        try {
                            this.switchPane("content");
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e) {
                this.statusLabel.setText("操作失败:" + e.getMessage());
                e.printStackTrace();
            }
        };
        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }

    private void loadContextMenu() {
        ContextMenu cm = new ContextMenu();
        MenuItem refreshBtn = new MenuItem("刷新");
        cm.getItems().add(refreshBtn);
        MenuItem openBtn = new MenuItem("打开");
        cm.getItems().add(openBtn);
        MenuItem renameBtn = new MenuItem("重命名");
        cm.getItems().add(renameBtn);
        MenuItem delBtn = new MenuItem("删除");
        cm.getItems().add(delBtn);
        cm.getItems().add(new SeparatorMenuItem());
        MenuItem downloadBtn = new MenuItem("下载");
        cm.getItems().add(downloadBtn);
        MenuItem uploadBtn = new MenuItem("上传");
        cm.getItems().add(uploadBtn);

        Menu createMenu = new Menu("新建");
        MenuItem createFileBtn = new MenuItem("文件...");
        MenuItem createDirectoryBtn = new MenuItem("文件夹");
        createMenu.getItems().add(createFileBtn);
        createMenu.getItems().add(createDirectoryBtn);
        cm.getItems().add(createMenu);
        cm.getItems().add(new SeparatorMenuItem());
        MenuItem changeTimeStampBtn = new MenuItem("修改时间戳");
        cm.getItems().add(changeTimeStampBtn);
        MenuItem cloneTimeStampBtn = new MenuItem("克隆时间戳");
        cm.getItems().add(cloneTimeStampBtn);
        this.fileListTableView.setContextMenu(cm);
        openBtn.setOnAction(event -> {
            String type = ((StringProperty) ((List) this.fileListTableView.getSelectionModel().getSelectedItem()).get(3)).getValue();
            String name = ((StringProperty) ((List) this.fileListTableView.getSelectionModel().getSelectedItem()).get(0)).getValue();
            String pathString = this.currentPathCombo.getValue().toString();
            pathString = Paths.get(pathString, new String[0]).normalize().toString();
            if (!pathString.endsWith("/")) {
                pathString = pathString + "/";
            }
            pathString = pathString + name;
            if (type.equals("directory")) {

                try {
                    expandByPath(pathString);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {

                String filePathString = pathString;
                this.filePathText.setText(pathString);
                String charset = this.charsetCombo.getValue().toString().equals("����") ? null : this.charsetCombo.getValue().toString();
                showFile(filePathString, charset);
            }
        });

        refreshBtn.setOnAction(event -> {
            this.statusLabel.setText("正在刷新……");
            try {
                expandByPath(this.currentPathCombo.getValue().toString());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            this.statusLabel.setText("刷新完成。");
        });
        renameBtn.setOnAction(event -> {
            int row = this.fileListTableView.getSelectionModel().getSelectedIndex();

            this.fileListTableView.edit(row, this.fileNameCol);
        });
        delBtn.setOnAction(event -> {
            String name = ((StringProperty) ((List) this.fileListTableView.getSelectionModel().getSelectedItem()).get(0)).getValue();
            String fileFullPath = this.currentPathCombo.getValue().toString() + name;
            Runnable runner = () -> {
                try {
                    JSONObject resultObj = this.currentShellService.deleteFile(fileFullPath);
                    String status = resultObj.getString("status");
                    String msg = resultObj.getString("msg");
                    Platform.runLater(() -> {
                        if (status.equals("success")) {
                            try {
                                this.expandByPath(this.currentPathCombo.getValue().toString());
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }

                        this.statusLabel.setText(msg);
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        this.statusLabel.setText("操作失败:" + e.getMessage());
                    });
                }

            };

            Thread workThrad = new Thread(runner);
            this.workList.add(workThrad);
            workThrad.start();
        });
        uploadBtn.setOnAction(event -> {
            try {
                uploadFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        downloadBtn.setOnAction(event -> downloadFile());
    }


    private void downloadFile() {
        String fileName = ((StringProperty) ((List) this.fileListTableView.getSelectionModel().getSelectedItem()).get(0)).getValue();
        String fileFullPath = this.currentPathCombo.getValue().toString() + fileName;
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("请选择保存路径");
        fileChooser.setInitialFileName(fileName);
        File selectedFile = fileChooser.showSaveDialog(this.fileListGridPane.getScene().getWindow());
        String localFilePath = selectedFile.getAbsolutePath();
        if (selectedFile == null || selectedFile.equals(""))
            return;
        this.statusLabel.setText("正在下载" + fileFullPath + "……");

        Runnable runner = () -> {
            try {
                this.currentShellService.downloadFile(fileFullPath, localFilePath);
                String result = selectedFile.getName() + "下载完成,文件大小:" + selectedFile.length();
                Platform.runLater(() -> this.statusLabel.setText(result));

            } catch (Exception e) {
                Platform.runLater(() -> this.statusLabel.setText("操作失败:" + e.getMessage()));
                e.printStackTrace();
            }
        };
        Thread workThrad = new Thread(runner);
        this.workList.add(workThrad);
        workThrad.start();
    }
}
